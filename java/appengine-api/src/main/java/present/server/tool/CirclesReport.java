package present.server.tool;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import present.server.model.group.Group;
import present.server.model.user.PresentAdmins;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static present.server.tool.RemoteTool.PRODUCTION_SERVER;
import static present.server.tool.RemoteTool.against;

/**
 * @author Gabrielle Taylor {gabrielle@present.co}
 */
public class CirclesReport {

  public static void main(String[] args) {
    against(PRODUCTION_SERVER, () -> {
      writeFile(userGeneratedCircles(), "user-circles");
      //writeFile(newNationwideCircles(), "nationwide-circles");
      //writeFile(allNationwideCircles(), "all-nationwide-circles");
    });
  }

  public static List<Group> userGeneratedCircles() {
    long createdSince = ZonedDateTime.of(2017, 10, 1, 0, 0, 0, 0, ZoneId.systemDefault()).toInstant().toEpochMilli();
    System.out.println("Finding circles created since " + Instant.ofEpochMilli(createdSince).atZone(ZoneId.systemDefault()));
    List<Group> groups = ofy().load().type(Group.class)
        .filter("owner !=", PresentAdmins.ByName.janete.getRef())
        .filter("owner !=", PresentAdmins.ByName.kassia.getRef())
        .filter("owner !=", PresentAdmins.ByName.kristina.getRef())
        .filter("owner !=", PresentAdmins.ByName.pegah.getRef())
        .filter("owner !=", PresentAdmins.ByName.gabrielle.getRef())
        .filter("owner !=", PresentAdmins.ByName.kayla.getRef())
        .filter("owner !=", PresentAdmins.ByName.emma.getRef())
        .list().stream().filter(g -> g.createdTime >= createdSince)
        .collect(Collectors.toList());
    return groups;
  }

  public static String csvPrint(Group g) {
    DateTimeFormatter format = DateTimeFormatter.ofPattern("MMM dd yyyy HH:mm a");
    return String.join(", ",
        "\"" + g.title + "\"",
        g.shortLink(),
        Instant.ofEpochMilli(g.createdTime).atZone(ZoneId.systemDefault()).format(format),
        g.owner.get().fullName(),
        g.owner.get().shortLink(),
        g.owner.get().facebookLink(),
        g.owner.get().email(),
        "\"" + g.description.replace("\n", " ").replace("\"", "\"\"") + "\"",
        "\"" + g.locationName + "\"",
        g.activeComments + "",
        String.valueOf(g.memberCount),
        "\"" + g.categories.toString() + "\"");
  }

  private static void writeFile(List<Group> groups, String name) throws IOException, InterruptedException {
    // TODO: replace with CSVWriter library

    String path = "out/" + name + ".csv";
    String header = String.join(", ",
        "Circle Name",
        "Circle Link",
        "Created Time",
        "Circle Owner",
        "Present Link",
        "Facebook Link",
        "Email",
        "Circle Description",
        "Circle Location",
        "Comment Count",
        "Member Count",
        "Categories");
    new File(path).delete();

    try (BufferedWriter out = new BufferedWriter(
        Files.newBufferedWriter(Paths.get(path), StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
      out.write(header);
      out.newLine();
      for (Group g : groups) {
        out.write(csvPrint(g));
        out.newLine();
        System.out.print(".");
      }
    } catch (IOException x) {
      System.err.println(x);
    }
    //new ProcessBuilder("open", path).inheritIO().start().waitFor();
  }
}
