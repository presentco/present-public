package present.server;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.server.model.comment.Comment;
import present.server.model.comment.GroupView;
import present.server.model.content.Content;
import present.server.model.group.Group;
import present.server.model.group.JoinedGroups;
import present.server.model.user.Client;
import present.server.model.user.User;
import present.server.notification.Notifications;

import static present.server.model.PresentEntities.loadRandomSubset;

/**
 * @author pat@pat.net
 * Date: 8/30/16
 */
public class CronServlet extends HttpServlet {
  private static final Logger logger = LoggerFactory.getLogger(CronServlet.class);

  @Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    if (checkAuth(request)) {
      response.sendError(403);
      return;
    }

    if (request.getServletPath().equalsIgnoreCase("/hourly")) {
      logger.info("CronServlet: Hourly");
      Notifications.clearFailedTokens();
    }

    if (request.getServletPath().equalsIgnoreCase("/nightly")) {
      logger.info("CronServlet: Nightly");
    }

    if (request.getServletPath().equalsIgnoreCase("/warmdb")) {
      logger.info("CronServlet: Warm DB");
      loadRandomSubset(Client.class, 5);
      loadRandomSubset(User.class, 5);
      loadRandomSubset(Group.class, 5);
      loadRandomSubset(JoinedGroups.class, 5);
      loadRandomSubset(Comment.class, 5);
      loadRandomSubset(GroupView.class, 5);
      loadRandomSubset(Content.class, 5);
    }

    if (request.getServletPath().equalsIgnoreCase("/test")) {
      logger.info("Testing...");
      response.getWriter().println("Test...");
    }
  }

  private boolean checkAuth(HttpServletRequest request)
      throws IOException {
  /*
    Allow calls from Cron or with a temporary auth key for testing.
    "The X-Appengine-Cron header is set internally by Google App Engine. If your request handler
    finds this header it can trust that the request is a cron request. If the header is present in an
    external user request to your app, it is stripped..."
  */
    final String auth = "";
    if (!"true".equalsIgnoreCase(request.getHeader("X-Appengine-Cron")) &&
        !auth.equalsIgnoreCase(request.getParameter("auth"))) {
      return true;
    }
    return false;
  }
}
