package present.server.notification;

/**
 * Fields used in the APNS notification messages.
 *
 * Note: These names are not referenced in the notifications module directly but are placed
 * here to support acceptance testing without introducing a dependency on the api server module.
 *
 * @author Pat Niemeyer (pat@present.co)
 */
public class PayloadNames {

  public static String USER_ID = "userId";
  public static String USER = "user"; // encoded proto payload

  public static String GROUP_ID = "groupId";

  public static String COMMENT_ID = "commentId";
}

