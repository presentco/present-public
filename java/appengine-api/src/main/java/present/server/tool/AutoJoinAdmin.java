package present.server.tool;

import present.server.model.user.PresentAdmins;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author Gabrielle Taylor
 */
public class AutoJoinAdmin {
  public static void main(String[] args) throws IOException {
    RemoteTool.against(RemoteTool.PRODUCTION_SERVER, () -> {
      PresentAdmins.addAutoJoinUser(PresentAdmins.ByName.chauntie, 5L, TimeUnit.MINUTES);
      PresentAdmins.addAutoJoinUser(PresentAdmins.ByName.janete, 60L, TimeUnit.MINUTES);
      PresentAdmins.addAutoJoinUser(PresentAdmins.ByName.kassia, 12L, TimeUnit.HOURS);
      PresentAdmins.addAutoJoinUser(PresentAdmins.ByName.kristina, 1L, TimeUnit.DAYS);
      PresentAdmins.addAutoJoinUser(PresentAdmins.ByName.gabrielle, 2L, TimeUnit.DAYS);
    });
  }
}
