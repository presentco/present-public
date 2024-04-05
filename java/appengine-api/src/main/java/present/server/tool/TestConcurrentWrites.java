package present.server.tool;

import com.google.appengine.api.NamespaceManager;
import com.google.common.base.Stopwatch;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import java.util.concurrent.CountDownLatch;

import static com.googlecode.objectify.ObjectifyService.ofy;

public class TestConcurrentWrites {

  // 100 writes across 10 threads: ~14/s w/o errors
  // 200 writes across 20 threads: ~16/s w/o errors
  // 100 transactional updates across 10 threads: ~4/s w/o errors

  public static void main(String[] args) {
    ObjectifyService.register(MyEntity.class);
    int threads = 10;
    CountDownLatch latch = new CountDownLatch(threads);
    Stopwatch sw = Stopwatch.createStarted();
    for (int i = 0; i < threads; i++) {
      Thread t = new Thread(() -> {
        RemoteTool.against(RemoteTool.STAGING_SERVER, () -> {
          for (int a = 0; a < 10; a++) {
            NamespaceManager.set("test");
            ofy().transact(() -> {
              MyEntity entity = ofy().load().type(MyEntity.class).id("id").now();
              entity.value++;
              ofy().save().entity(entity).now();
            });
          }
        });
        latch.countDown();
      });
      t.start();
    }
    try {
      latch.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    System.out.println("Done in " + sw + ".");
  }

  @Entity static class MyEntity {
    @Id String id = "id";
    int value = 0;
  }
}