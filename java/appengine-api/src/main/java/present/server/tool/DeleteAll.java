package present.server.tool;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import com.google.common.collect.Iterables;
import java.util.List;
import present.server.model.log.DatastoreOperation;

import static present.server.tool.RemoteTool.PRODUCTION_SERVER;
import static present.server.tool.RemoteTool.against;

/**
 * Deletes all entities of a type.
 *
 * @author Bob Lee (bob@present.co)
 */
public class DeleteAll {
  public static void main(String[] args) {
    against(PRODUCTION_SERVER, () -> {
      AsyncDatastoreService datastore = DatastoreServiceFactory.getAsyncDatastoreService();
      int chunkSize = 10000;
      Iterable<Key> keys = Iterables.transform(datastore.prepare(
          new Query(DatastoreOperation.class.getSimpleName()).setKeysOnly())
          .asIterable(FetchOptions.Builder.withChunkSize(chunkSize)), Entity::getKey);
      Iterable<List<Key>> parts = Iterables.partition(keys, chunkSize);
      int count = 0;
      for (List<Key> part : parts) {
        datastore.delete(part);
        System.out.print(".");
        count = (count + 1) % 80;
        if (count == 0) System.out.println();
      }
    });
  }
}
