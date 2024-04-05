package present.server.tool;

import com.google.appengine.tools.remoteapi.RemoteApiInstaller;
import com.googlecode.objectify.ObjectifyService;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileReader;
import java.io.IOException;
import present.server.model.PresentEntities;
import present.server.model.user.User;
import present.server.model.util.Coordinates;

import static present.server.tool.RemoteTool.PRODUCTION_SERVER;
import static present.server.tool.RemoteTool.installRemoteAPI;

/**
 * Parse the logs for location.
 * (This is horrible, fragile code for one-off project.  Don't look at it!)
 * @author Pat Niemeyer (pat@present.co)
 */
public class ParseLogs {

  public static void main(String[] args) throws IOException {
    //RemoteApiInstaller installer = installRemoteAPI(DEV_SERVER);
    //RemoteApiInstaller installer = installRemoteAPI(STAGING_SERVER);
    RemoteApiInstaller installer = installRemoteAPI(PRODUCTION_SERVER);
    try (Closeable closeable = ObjectifyService.begin()) {
      //NamespaceManager.set("test");
      PresentEntities.registerAll();

      new ParseLogs().run();
    } finally {
      installer.uninstall();
    }
  }


  public void run() throws IOException {

    String fileName = "api_2017_11_14.log";
    BufferedReader br = new BufferedReader(new FileReader(fileName));
    String line;
    while ((line = br.readLine()) != null) {
      if (Character.isDigit(line.charAt(0))) {
        startLine(line);
      } else {
        bodyLine(line);
      }
    }
    System.out.println("count = " + count);
  }

  private void startLine(String line)
  {
    reportAndClear();
    boolean post = line.contains("POST");
    boolean get = line.contains("GET");
    boolean options = line.contains("OPTIONS");
    boolean internal = line.startsWith("0.1.0.2");
    if (!post && !get && !options) {
      throw new RuntimeException();
    }
    if (post && !internal && line.contains("completeSignup")) {
      completeSignup = true;
    }
  }

  private void bodyLine(String line) {
    if(!completeSignup) { return; }

    if (line.startsWith("\t: client:")) {
      int ui = line.indexOf("User #");
      userId = line.substring(ui+6, ui + 35+7);
    }
    if (line.startsWith("\t:     \"latitude\": ")) {
      latitude = Double.parseDouble(line.substring(19, 27));
    }
    if (line.startsWith("\t:     \"longitude\": ")) {
      longitude = Double.parseDouble(line.substring(20, 28));
    }
  }

  boolean completeSignup;
  String userId;
  Double latitude, longitude;

  int count = 0;
  private void reportAndClear() {
    if (completeSignup && userId != null && latitude != null && longitude != null) {
      User user = User.get(userId);
      if (user == null) {
        throw new RuntimeException("can't find: "+userId);
      }
      if(user.signupLocation == null) {
        user.signupLocation = new Coordinates(latitude, longitude);
        user.savePreservingUpdateTime().now();
        System.out.printf("%s, %s, %s\n", userId, latitude, longitude);
        count++;
      } else {
        System.out.println("skipping: "+userId);
      }
    }
    completeSignup = false;
    userId = null; latitude = null; longitude = null;
  }
}

