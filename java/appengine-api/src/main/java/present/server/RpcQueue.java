package present.server;

import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.RetryOptions;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.squareup.wire.Message;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import okio.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.Empty;
import present.proto.Platform;
import present.proto.RequestHeader;
import present.server.environment.Environment;
import present.wire.rpc.core.Request;
import present.wire.rpc.core.RpcMethod;
import present.wire.rpc.core.RpcProtocol;

/**
 * Creates RPC clients that invoke RPCs asynchronously via the App Engine task queue. Methods must
 * return {@code Empty} (as the result is ignored).
 *
 * @author Bob Lee (bob@present.co)
 */
public class RpcQueue<T> {

  private static final Logger logger = LoggerFactory.getLogger(RpcQueue.class);

  private static ThreadLocal<Map<String, List<TaskOptions>>> localBatches = new ThreadLocal<>();

  private final Class<T> service;
  private TaskOptions options = TaskOptions.Builder.withDefaults();
  private String queueName = "default";

  private RpcQueue(Class<T> service) {
    this.service = service;
  }

  /** Convenience method, equivalent to: {@code RpcQueue.to(service).create()} */
  public static <T> T create(Class<T> service) {
    return RpcQueue.to(service).create();
  }

  /** Starts building an RPC queue for the given service. */
  public static <T> RpcQueue<T> to(Class<T> service) {
    return new RpcQueue<>(service);
  }

  /** Optionally sets the task options. Use to add delays, etc. */
  public RpcQueue<T> with(TaskOptions options) {
    this.options = options;
    return this;
  }

  /** Don't retry the task if it fails. */
  public RpcQueue<T> noRetries() {
    options = options.retryOptions(RetryOptions.Builder.withTaskRetryLimit(0));
    return this;
  }

  /** Optionally sets the queue name. */
  public RpcQueue<T> in(String queueName) {
    this.queueName = queueName;
    return this;
  }

  /** Creates a client that enqueues RPCs. */
  public T create() {
    return service.cast(Proxy.newProxyInstance(service.getClassLoader(),
        new Class<?>[] { service }, new Handler(this)));
  }

  /**
   * Batches RPC tasks enqueued during {@code r}. Does not enqueue tasks if {@code r} throws.
   *
   * @throws IllegalStateException if already in a batch
   */
  public static void batch(Runnable r) {
    Preconditions.checkState(localBatches.get() == null, "Already in a batch.");
    localBatches.set(new HashMap<>());
    try {
      r.run();
      Map<String, List<TaskOptions>> batches = localBatches.get();
      if (!batches.isEmpty()) {
        logger.info("Enqueueing {} tasks.", batches.size());
        batches.forEach((queueName, batch) -> {
          // We can only enqueue 100 tasks at a time.
          Lists.partition(batch, 100).forEach(chunk -> {
            QueueFactory.getQueue(queueName).add(chunk);
          });
        });
      }
    } finally {
      localBatches.remove();
    }
  }

  private static class Handler implements InvocationHandler {

    private final String serviceName;
    private final Map<String, RpcMethod> methods;
    private final TaskOptions task;
    private final String queueName;

    private Handler(RpcQueue rpcQueue) {
      methods = RpcMethod.mapFor(rpcQueue.service);
      this.serviceName = rpcQueue.service.getSimpleName();
      this.task = new TaskOptions(rpcQueue.options);
      this.queueName = rpcQueue.queueName;
    }

    @Override public Object invoke(Object proxy, Method javaMethod, Object[] args)
        throws Throwable {
      String methodName = javaMethod.getName();
      RpcMethod method = methods.get(methodName);
      Preconditions.checkState(method.resultType().equals(Empty.class),
          "%s must return Empty.", method);
      Object argument = args[0];
      String url = "/api/" + serviceName + "/" + methodName;
      String traceId = UUID.randomUUID().toString().replace("-", "");
      RequestHeader header = RequestHeaders.current();
      if (header == null) {
        // We're outside of an RPC invocation, in a background task or somewhere else.
        header = new RequestHeader.Builder()
            .clientUuid(Uuids.NULL)
            .requestUuid(Uuids.newUuid())
            .platform(Platform.INTERNAL)
            .authorizationKey(Internal.AUTHORIZATION_KEY)
            .apiVersion(1)
            .build();
      } else {
        header = header.newBuilder().authorizationKey(Internal.AUTHORIZATION_KEY).build();
      }
      ByteString argumentBytes = ByteString.of(((Message) argument).encode());
      ByteString headerBytes = ByteString.of(header.encode());
      Request request = new Request(headerBytes, argumentBytes);
      TaskOptions task = new TaskOptions(this.task);
      task.url(url)
        .method(TaskOptions.Method.POST)
        .header("Host", Environment.current().apiHost())
        .header("Accept", "text/plain")
        .header("X-Cloud-Trace-Context", traceId + "/0;o=1")
        .payload(request.encode(), RpcProtocol.PROTO.contentType);
      Map<String, List<TaskOptions>> batches = localBatches.get();
      if (batches != null) {
        List<TaskOptions> batch = batches.computeIfAbsent(this.queueName,
            (key) -> new ArrayList<>());
        batch.add(task);
      } else {
        QueueFactory.getQueue(this.queueName).add(task);
      }
      return Protos.EMPTY;
    }
  }
}
