package present.server.tool;

import java.io.IOException;
import present.server.model.user.PresentAdmins;

/**
 * @author Bob Lee (bob@present.co)
 */
public class ChangeWelcomer {
  public static void main(String[] args) throws IOException {
    RemoteTool.against(RemoteTool.PRODUCTION_SERVER, () -> {
      PresentAdmins.addWelcomer(PresentAdmins.ByName.chauntie);
      // PresentAdmins.addWelcomer(PresentAdmins.ByName.Janete);
    });
  }
}
