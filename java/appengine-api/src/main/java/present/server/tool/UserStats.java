package present.server.tool;

import com.google.appengine.tools.remoteapi.RemoteApiInstaller;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import present.server.Time;
import present.server.model.PresentEntities;
import present.server.model.comment.Comment;
import present.server.model.comment.Comments;
import present.server.model.group.Group;
import present.server.model.group.JoinedGroups;
import present.server.model.user.User;
import present.server.model.user.UserState;
import present.server.model.user.Users;

@SuppressWarnings("ALL") public class UserStats extends RemoteTool {

  public static void main(String[] args) throws IOException
  {
    //RemoteApiInstaller installer = installRemoteAPI(DEV_SERVER);
    //RemoteApiInstaller installer = installRemoteAPI(STAGING_SERVER);
    RemoteApiInstaller installer = installRemoteAPI(PRODUCTION_SERVER);
    try (Closeable closeable = ObjectifyService.begin())
    {
      //NamespaceManager.set("test");
      PresentEntities.registerAll();

      List<Comment> allComments = Comments.stream()
          .filter(comment -> comment.group != null
              && comment.group.get() != null
              && comment.author != null
              && comment.author.get() != null)
          .collect(Collectors.toList());

      long allCommentsCount = allComments.size();
      System.out.println("allCommentsCount = " + allCommentsCount);

      System.out.println("indexing group comments");
      List<Comment> groupComments = allComments.stream()
          .filter(comment -> comment.group.get() instanceof Group)
          .map(comment -> (Comment) comment)
          .collect(Collectors.toList());
      ImmutableListMultimap<User, Comment> groupCommentMap =
          Multimaps.index(groupComments, comment -> comment.author.get());
      long allGroupCommentsCount = groupCommentMap.values().stream().count();
      System.out.println("groupCommentsCount = " + allGroupCommentsCount);

      System.out.printf("\n%s, %s, %s, %s, %s, %s, %s, %s, %s, %s",
          "Name", "Email", "User State", "Notifications Enabled", "Last Profile Update",
          "Signup Steps Completed", "Signup Date",
          "Group Comments", "Saved Groups");

      Users.stream().forEach(user->
      {
        String lastInfoUpdate = Time.format_yyyy_MM_dd(user.updatedTime);
        String signupDateString = Time.format_yyyy_MM_dd(user.signupTime);

        // in order of (current) signup
        List<String> signupStepsCompletedList = new ArrayList<>();
        boolean hasName = user.name() != null && !user.name().equals("");
        if (hasName) { signupStepsCompletedList.add("name"); }
        boolean hasPhoto = !user.profilePhotoUrl().equals(User.MISSING_PHOTO_URL);
        if (hasPhoto) { signupStepsCompletedList.add("photo"); }
        boolean hasDescription = user.bio != null && !user.bio.equals("");
        if (hasDescription) { signupStepsCompletedList.add("description"); }
        boolean hasInterests = !user.interests.isEmpty();
        if (hasInterests) { signupStepsCompletedList.add("interests"); }
        String signupStepsCompleted = String.join(":", signupStepsCompletedList);


        // number of group comments posted
        int groupCommentsCount = groupCommentMap.get(user).size();

        // number of joined groups
        JoinedGroups joinedGroups = JoinedGroups.getOrCreate(user);
        int savedGroupsCount = joinedGroups == null ? 0 : joinedGroups.groups.size();

        String email = user.email() != null ? user.email() : "";

        Boolean notifsEnabledBool = new ListUsers.UserAndClients(user).notificationsEnabled();
        String notifsEnabled = notifsEnabledBool != null ? String.valueOf(notifsEnabledBool) : "";

        UserState state = user.state;

        System.out.printf("\n%s, %s, %s, %s, %s, %s, %s, %s, %s, %s",
            user.fullName(),
            email,
            state.toString(),
            notifsEnabled,
            lastInfoUpdate,
            signupStepsCompleted,
            signupDateString,
            groupCommentsCount,
            savedGroupsCount
        );
      });

    } finally {
      installer.uninstall();
    }
  }
}
