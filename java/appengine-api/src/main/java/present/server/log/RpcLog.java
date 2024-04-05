package present.server.log;

import com.google.appengine.repackaged.com.google.common.collect.Iterables;
import com.google.apphosting.api.logservice.LogServicePb;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.wire.WireTypeAdapterFactory;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.RequestHeader;
import present.server.Caches;
import present.server.environment.Environment;
import present.wire.rpc.client.RpcClient;
import present.wire.rpc.core.RpcFilter;
import present.wire.rpc.core.RpcMethod;

import static present.server.log.RequestLogs.toRfc3339;

/**
 * A parsed RPC log.
 *
 * @author Bob Lee (bob@present.co)
 */
public class RpcLog {

  private static final Logger logger = LoggerFactory.getLogger(RpcLog.class);

  private static Gson gson = new GsonBuilder().registerTypeAdapterFactory(
      new WireTypeAdapterFactory()).create();

  private static final String RPC_LOG_PREFIX = "present.server.filter.HeaderFilter filter: RPC:";

  private final LogServicePb.RequestLog log;
  private final RpcMethod method;
  private final RequestHeader header;
  private final Object argument;

  public RpcLog(LogServicePb.RequestLog log, RpcMethod method, RequestHeader header,
      Object argument) {
    this.log = log;
    this.method = method;
    this.header = header;
    this.argument = argument;
  }

  /** Returns the entire request log. */
  public LogServicePb.RequestLog log() {
    return log;
  }

  /** Returns the log timestamp. */
  public long timestamp() {
    return log.getStartTime();
  }

  /** Parses the request header. */
  public RequestHeader header() {
    return this.header;
  }

  /** Returns the RPC method. */
  public RpcMethod rpcMethod() {
    return this.method;
  }

  /** Parses the RPC argument. */
  public Object argument() {
    return this.argument;
  }

  private static final ThreadLocal<RequestHeader> localHeader = new ThreadLocal<>();

  private static final RpcFilter headerFilter = invocation -> {
    invocation.setHeader(localHeader.get());
    return invocation.proceed();
  };

  private static LoadingCache<Class<?>, Object> clients = Caches.create(
      key -> RpcClient.create(Environment.current().apiUrl(), RequestHeader.class, key, headerFilter));

  /** Replays the RPC and returns the result. Be careful not to create an infinite loop!!! */
  public Object replay() {
    Object client = clients.getUnchecked(method.service());
    try {
      localHeader.set(this.header);
      return method.method().invoke(client, this.argument);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    } finally {
      localHeader.remove();
    }
  }

  /**
   * Parses RPC requests from the logs. Note: We truncate long string in requests when we log
   * them.
   */
  public static Iterable<RpcLog> search(Class<?> service, String method, long startTime,
      long endTime) {
    String filter = "resource.type=gae_app"
        + " AND logName=\"projects/" + Environment.applicationId()
        + "/logs/appengine.googleapis.com%2Frequest_log\""
        + " AND severity>=INFO"
        + " AND protoPayload.resource:\"/api/" + service.getSimpleName() + "/" + method + "\""
        + " AND timestamp>=\"" + toRfc3339(startTime) + "\""
        + " AND timestamp<=\"" + toRfc3339(endTime) + "\"";
    Iterable<LogServicePb.RequestLog> logs = RequestLogs.search(filter);
    return parse(logs);
  }

  /**
   * Parses RPC requests from the logs. Note: We truncate long string in requests when we log
   * them.
   */
  public static Iterable<RpcLog> parse(Iterable<LogServicePb.RequestLog> logs) {
    Iterable<LogServicePb.RequestLog> apiLogs =
        Iterables.filter(logs, log -> log.getResource().startsWith("/api/"));
    return Iterables.filter(Iterables.transform(apiLogs, log -> {
      if (!"POST".equals(log.getMethod())) return null;

      for (LogServicePb.LogLine line : log.lines()) {
        String message = line.getLogMessage();
        if (message.startsWith(RPC_LOG_PREFIX)) {
          try {
            return parse(log, message);
          } catch (Exception e) {
            logger.error("Error parsing: " + message, e);
            return null;
          }
        }
      }
      logger.warn("Couldn't find RPC in log: {}", log);
      return null;
    }), Objects::nonNull);
  }

  private static final LoadingCache<Class<?>, Map<String, RpcMethod>> rpcMethods
      = Caches.create(RpcMethod::mapFor);

  /** Parses an RPC log message. */
  private static RpcLog parse(LogServicePb.RequestLog log, String message) {
    // Example:
    //
    // present.server.filter.HeaderFilter filter: RPC: (HeaderFilter.java:32)
    // headers: {
    //   "clientUuid": "2d461892-8634-4034-bb1c-9f528572df82",
    //   "requestUuid": "8fe45823-a45e-4ac6-8ed5-4196a17ccd22",
    //   "authorizationKey": "unimplemented",
    //   "platform": "IOS",
    //   "apiVersion": 1,
    //   "clientVersion": "1.1b10",
    //   "platformVersion": "11.2",
    //   "location": {
    //     "latitude": 37.785834,
    //     "longitude": -122.406417,
    //     "accuracy": 0.0
    //   }
    // }
    // GroupService.getPastComments({
    //   "groupId": "8ad1d3e3-b6e2-442c-9ef3-bf0885e4dc72"
    // })

    // Parse strings.
    int headerStart = message.indexOf('{', RPC_LOG_PREFIX.length());
    int headerEnd = message.indexOf("\n}", headerStart) + 2;
    String headerString = message.substring(headerStart, headerEnd);
    int serviceStart = headerEnd + 1;
    int period = message.indexOf('.', serviceStart);
    String service = message.substring(serviceStart, period);
    int argStart = message.indexOf('{', period);
    int argEnd = message.length() - 2;
    String methodName = message.substring(period + 1, argStart - 1);
    String argString = message.substring(argStart, argEnd);

    // Parse JSON.
    RequestHeader header = gson.fromJson(headerString, RequestHeader.class);
    RpcMethod method;
    try {
      Class<?> serviceType = Class.forName("present.proto." + service);
      method = rpcMethods.getUnchecked(serviceType).get(methodName);
      if (method == null) throw new RuntimeException("RPC method not found.");
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
    Object argument = gson.fromJson(argString, method.argumentType());
    return new RpcLog(log, method, header, argument);
  }
}
