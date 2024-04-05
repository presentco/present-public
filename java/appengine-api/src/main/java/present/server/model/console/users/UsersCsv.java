package present.server.model.console.users;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import present.server.tool.ExportUsers;

@Path("/")
public class UsersCsv {

  private static final Response FORBIDDEN = Response.status(Response.Status.FORBIDDEN).build();

  @Path("users.csv")
  @GET
  @Produces("text/csv; charset=UTF-8")
  public Response csv(@QueryParam("auth") String auth) {
    if (!valid(auth)) return FORBIDDEN;
    return Response.ok(new CsvOutput()).build();
  }

  @Path("users.xls")
  @GET
  @Produces("application/vnd.ms-excel; charset=UTF-8")
  public Response xls(@QueryParam("auth") String auth) {
    if (!valid(auth)) return FORBIDDEN;
    return Response.ok(new CsvOutput()).build();
  }

  private static boolean valid(String auth) {
    return "xx".equals(auth);
  }

  static class CsvOutput implements StreamingOutput {
    @Override
    public void write(OutputStream out) throws IOException, WebApplicationException {
      ExportUsers.exportTo(new OutputStreamWriter(out, StandardCharsets.UTF_8));
    }
  }
}
