package present.server.tool;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import present.server.model.geocoding.Geocoding;
import present.server.model.user.User;
import present.server.model.user.UserState;
import present.server.model.util.Coordinates;

import static com.googlecode.objectify.ObjectifyService.ofy;

public class BackfillLocations {
  public static void main(String[] args) throws IOException {
    RemoteTool.against(RemoteTool.STAGING_SERVER, () -> {
      System.out.println("Backfilling locations from zip code for all users...");

      List<User> users = ofy().load().type(User.class).filter("state", UserState.MEMBER).list();
      System.out.println("Number of users: " + users.size());

      System.out.println("Number of users without signup location: " + users.stream()
          .filter(u -> u.signupLocation == null)
          .collect(Collectors.toList()).size());
      System.out.println("Number of users with signup location: " + users.stream()
          .filter(u -> u.signupLocation != null)
          .collect(Collectors.toList()).size());
      for (User user : users) {
        ofy().transact(() -> {
          User latest = user.reload();
          if (latest.signupLocationSource == null) {
            System.out.println("#" + users.indexOf(user) + " user = " + latest.publicName());
            // If a user has a zip code but no sign up location saved
            if (latest.zip != null && latest.signupLocation == null) {
              // Geocode zip code (captured during web signup).
              Coordinates location = Geocoding.geocodeZipCode(latest.zip);
              // Store sign up location coordinates for user
              if (location != null) {
                latest.setSignupLocation(location);
                System.out.println("\tLocation null, set location to: " + location);
                // Store source of signup location as ZIP
                latest.signupLocationSource = User.SignupLocationSource.ZIP;
                System.out.println("\tSet location source to ZIP.");
                // Save results to user
                latest.save();
              }
              // Otherwise if a user has a sign up location but no source, fill in source
            } else if (latest.zip == null
                && latest.signupLocation != null) {
              latest.signupLocationSource = User.SignupLocationSource.GPS;
              System.out.println("\tSignup location but no source and no zip, set source to GPS");
              // Save results to user
              latest.save();
            } else if (latest.zip != null
                && latest.signupLocation != null) {
              //latest.signupLocationSource = User.SignupLocationSource.GPS;
              System.out.println("\tSignup location and zip. Set source to GPS?");
              // Save results to user
              //latest.save();
            } else if (latest.zip == null
                && latest.signupLocation == null) {
              System.out.println("\tNo signup location and no zip! (UUID:" + user.uuid() + ")");
            }
          }
        });
      }

      System.out.println("\nBackfill complete.");
      List<User> endUsers = ofy().load().type(User.class).filter("state", UserState.MEMBER).list();
      System.out.println("Number of users: " + endUsers.size());
      System.out.println("Number of users without signup location: " + endUsers.stream()
          .filter(u -> u.signupLocation == null)
          .collect(Collectors.toList()).size());
      System.out.println("Number of users with signup location: " + endUsers.stream()
          .filter(u -> u.signupLocation != null)
          .collect(Collectors.toList()).size());
    });
  }
}
