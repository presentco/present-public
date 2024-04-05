package present.server.tool;

import java.time.MonthDay;
import java.util.concurrent.TimeUnit;
import present.server.email.SummaryEmail;
import present.server.model.user.User;

public class GetSummary {
  public static void main(String[] args) {
    RemoteTool.against(RemoteTool.PRODUCTION_SERVER, () -> {
      TimeUnit timeUnit = TimeUnit.DAYS;
      long duration = 1;
      SummaryEmail email = new SummaryEmail(ProductionUsers.kassia(),
          System.currentTimeMillis() - timeUnit.toMillis(duration));
    });
  }
}
