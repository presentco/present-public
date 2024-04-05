package present.server.tool;

import com.google.appengine.tools.remoteapi.RemoteApiInstaller;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.Closeable;
import java.io.IOException;

@SuppressWarnings("ALL") public class ExportInvitationRequests extends RemoteTool {

  public static void main(String[] args) throws IOException
  {
    //RemoteApiInstaller installer = installRemoteAPI(DEV_SERVER);
    //RemoteApiInstaller installer = installRemoteAPI(STAGING_SERVER);
    RemoteApiInstaller installer = installRemoteAPI(PRODUCTION_SERVER);
    try (Closeable closeable = ObjectifyService.begin())
    {
      //NamespaceManager.set("test");

      // TODO: InvitationRequest is in the web app which is not in our dependencies.
      /*
      PresentEntities.registerAll();
      ObjectifyService.register(InvitationRequest.class);

      String rowFormat = "\n%s, %s, %s, %s";
      System.out.printf(rowFormat, "First", "Last", "Email", "Zip");

      List<InvitationRequest> list = ofy().load().type(InvitationRequest.class).list();
      list.stream().forEach(request->
      {
        System.out.printf(rowFormat,
          request.firstName, request.lastName, request.email, request.zip
        );
      });
      */

    } finally {
      installer.uninstall();
    }
  }
}
