package present.server;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.common.collect.ImmutableSet;
import com.googlecode.objectify.ObjectifyFactory;
import java.util.Set;
import present.server.model.user.Friendship;
import present.server.model.group.GroupMembership;
import present.server.model.log.LoggingDatastoreService;
import present.server.model.user.User;

/**
 * Customized Objectify factory
 *
 * @author Bob Lee (bob@present.co)
 */
public class PresentObjectifyFactory extends ObjectifyFactory {

  private static Set<String> LOGGED_KINDS = ImmutableSet.of(
      //User.class.getSimpleName(),
      GroupMembership.class.getSimpleName(),
      Friendship.class.getSimpleName()
  );

  public PresentObjectifyFactory() {
    // Translate Wire protos to byte[].
    getTranslators().addEarly(new WireTranslatorFactory());
  }

  @Override
  protected AsyncDatastoreService createRawAsyncDatastoreService(DatastoreServiceConfig cfg) {
    return new LoggingDatastoreService(super.createRawAsyncDatastoreService(cfg), LOGGED_KINDS);
  }
}
