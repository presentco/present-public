package present.server;

import com.google.common.collect.ImmutableMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import present.proto.ActivityService;
import present.proto.ContentService;
import present.proto.EmailService;
import present.proto.GroupService;
import present.proto.MessagingService;
import present.proto.PhoneService;
import present.proto.PingService;
import present.proto.RequestHeader;
import present.proto.SlackService;
import present.proto.UrlResolverService;
import present.proto.UserService;
import present.server.filter.AppEngineRpcFilterChain;
import present.server.phone.PhoneServiceImpl;
import present.server.slack.SlackServiceImpl;
import present.wire.rpc.server.RpcServlet;

/**
 * @author Bob Lee (bob@present.co)
 */
@Singleton public class AppEngineRpcServlet extends RpcServlet {

  @Inject public AppEngineRpcServlet(
      AppEngineGroupService groupService,
      AppEngineUserService userService,
      AppEnginePingService pingService,
      AppEngineMessagingService messagingService,
      AppEngineContentService contentService,
      AppEngineActivityService activityService,
      AppEngineUrlResolverService urlResolverService,
      SlackServiceImpl slackService,
      EmailServiceImpl emailService,
      PhoneServiceImpl phoneService,
      AppEngineRpcFilterChain filter
  ) {
    service(RequestHeader.class, GroupService.class, groupService, filter, ImmutableMap.of(
        "saveGroup", "joinGroup",
        "unsaveGroup", "leaveGroup",
        "getHome", "getExploreHtml",
        "getNearby", "getFeedHtml",
        "getSavedGroups", "getJoinedGroups"
    ));
    service(RequestHeader.class, UserService.class, userService, filter);
    service(RequestHeader.class, PingService.class, pingService, filter);
    service(RequestHeader.class, MessagingService.class, messagingService, filter);
    service(RequestHeader.class, ContentService.class, contentService, filter);
    service(RequestHeader.class, ActivityService.class, activityService, filter);
    service(RequestHeader.class, UrlResolverService.class, urlResolverService, filter);
    service(RequestHeader.class, SlackService.class, slackService, filter);
    service(RequestHeader.class, EmailService.class, emailService, filter);
    service(RequestHeader.class, PhoneService.class, phoneService, filter);
  }
}
