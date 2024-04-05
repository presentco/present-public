package present.server;

import com.github.mustachejava.Mustache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Streams;
import com.google.common.escape.Escaper;
import com.google.common.net.PercentEscaper;
import com.google.common.net.UrlEscapers;
import com.googlecode.objectify.Ref;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.CommentResponse;
import present.proto.ContentResponse;
import present.proto.GroupResponse;
import present.proto.HomeModel;
import present.proto.HtmlResponse;
import present.proto.NearbyFeedModel;
import present.server.model.Space;
import present.server.model.comment.Comment;
import present.server.model.content.Content;
import present.server.model.geocoding.Geocoding;
import present.server.model.group.Category;
import present.server.model.group.Group;
import present.server.model.group.GroupRanker;
import present.server.model.group.GroupSearch;
import present.server.model.group.RankedGroup;
import present.server.model.user.Client;
import present.server.model.user.Clients;
import present.server.model.user.Feature;
import present.server.model.user.User;
import present.server.model.user.Users;
import present.server.model.util.Address;
import present.server.model.util.Coordinates;

import static java.util.stream.Collectors.toList;

/**
 * Models and implementation related to the rendering HTML services.
 *
 * @author Pat Niemeyer (pat@present.co)
 * Date: 2/6/18
 */
public class GroupsHtml {

  private static final Logger logger = LoggerFactory.getLogger(GroupsHtml.class);

  private static final Mustache homeHtml = Mustaches.compileResource("/explore.html");
  private static final Mustache feedHtml = Mustaches.compileResource("/nearby.html");

  private static final List<String> CATEGORY_COLORS = ImmutableList.of(
      "#0069E3", // blue
      "#25D8F1", // cyan
      "#FDD61C", // yellow
      "#FF0AA6", // pink
      "#743CCE"  // purple
  );

  private static final int COVER_WIDTH = 440;
  private static final int COVER_HEIGHT = 248;
  private static final int MAX_GROUPS_PER_SECTION = 8;

  private static ContentResponse getCoverForHome(Group group) {
    if (group.hasCoverContent()) {
      Content cc = group.coverContent.get();
      String url = cc.squareUrl(COVER_WIDTH);
      return new ContentResponse(cc.uuid, cc.type, url, null);
    }
    // Return a map by default.
    return new ContentResponse(null, null,
        group.mapImageUrl(COVER_WIDTH, COVER_HEIGHT), null);
  }

  public static HomeModel getHomeModel(User user, Coordinates location, Future<Address> address,
      List<Group> nearbyGroups) {
    Client client = Clients.current();

    // Map groups by their first category.
    ImmutableListMultimap<String, Group> groupsByCategory = Multimaps.index(
        Iterables.filter(nearbyGroups, g -> !g.categories.isEmpty()),
        group -> group.categories.get(0)
    );

    // Create a section for each titled category with more than the min specified groups.
    List<HomeModel.Section> sections = new ArrayList<>();

    // Add "trending circles nearby" section
    List<GroupResponse> trendingGroups = nearbyGroups.stream()
        .limit(MAX_GROUPS_PER_SECTION)
        .map(g->g.toResponseFor(user, GroupsHtml::getCoverForHome))
        .collect(Collectors.toList());
    sections.add(new HomeModel.Section.Builder()
        .name("Trending")
        .encodedName("All")
        .groups(trendingGroups)
        .createUrl("/app/createCircle")
        .hasGroups(true)
        .build());

    addDynamicSection(user, nearbyGroups, sections, Category.Dynamic.FRIENDS,
        g -> g.involvesFriends);
    addDynamicSection(user, nearbyGroups, sections, Category.Dynamic.NEW,
        g -> g.createdTime > user.lastSessionTime());

    Set<String> addedCategories = new HashSet<>();
    Consumer<String> addSection = category -> {
      if (!addedCategories.add(category)) return;
      ImmutableList<Group> categoryGroups = groupsByCategory.get(category);
      if (categoryGroups.isEmpty()
          && (!client.supportsCircleCreateUrl() || !Category.isValid(category))) {
        // If there are no groups in this category, only show it if the client supports
        // creation URLs and this is a main category.
        return;
      }
      List<GroupResponse> groupResponses = categoryGroups.stream()
          .limit(MAX_GROUPS_PER_SECTION)
          .map(g -> g.toResponseFor(user, GroupsHtml::getCoverForHome))
          .collect(Collectors.toList());
      String pathEncoded = UrlEscapers.urlPathSegmentEscaper().escape(category);
      String parameterEncoded = parameterEncode(category);
      sections.add(new HomeModel.Section(category, pathEncoded, groupResponses,
          "/app/createCircle?category=" + parameterEncoded, !groupResponses.isEmpty()));
    };

    // Add sections for main categories.
    Category.asList().stream().map(Category::label).forEach(addSection);

    // Add sections for remaining (custom) categories.
    groupsByCategory.keySet().stream().sorted().forEach(addSection);

    addDynamicSection(user, nearbyGroups, sections, Category.Dynamic.WOMEN_ONLY,
        Group::isWomenOnly);

    // Top bar of categories, in the same order as the sections.

    List<HomeModel.Category> categories = Streams.mapWithIndex(sections.stream(),
        (section, index) -> {
          String name = section.name;
          String encoded = UrlEscapers.urlPathSegmentEscaper().escape(name);
          return new HomeModel.Category.Builder()
              .name(name)
              .encodedName(encoded)
              .color(CATEGORY_COLORS.get((int) index % CATEGORY_COLORS.size()))
              .build();
        })
        .filter(category -> !"Trending".equals(category.encodedName))
        .collect(Collectors.toList());

    // Generate static map image.
    String key = "xxx";
    String iconUrl = "https://storage.googleapis.com/present-production/app/pin@2x.png";
    StringBuilder mapImage = new StringBuilder("https://maps.googleapis.com/maps/api/staticmap"
        + "?maptype=roadmap"
        + "&key="+key);
    nearbyGroups.stream().limit(6).forEach(g -> {
      mapImage.append("&markers=scale:2")
          .append("%7Cicon:")
          .append(iconUrl)
          .append("%7C")
          .append(g.location.latitude)
          .append(",")
          .append(g.location.longitude);
    });

    logger.debug("Map: {}{}", mapImage, "&size=414x126&scale=2");

    boolean supportsExploreUrls = client.supports(Feature.EXPLORE_URLS);

    HomeModel.Builder builder = new HomeModel.Builder()
        .clientUuid(client.uuid)
        .categories(categories)
        .sections(sections)
        .showCreateButtons(client.supportsCircleCreateUrl())
        .location(location.toProto())
        .city(toCity(address))
        .canJoin(user != null)
        .canChangeLocation(client.supports(Feature.CHANGE_LOCATION_URL))
        .mapImage(mapImage.toString());

    if (user != null) {
      builder.memberName(user.firstName);
      // TODO: Enable when we move to one feed.
      //builder.showAreYouAWoman(supportsExploreUrls && user.gender() == Gender.UNKNOWN);
      builder.showAreYouAWoman(false);
      builder.canLogIn(false);
      builder.loggedIn(true);
    } else {
      builder.showAreYouAWoman(false);
      builder.canLogIn(supportsExploreUrls);
      builder.loggedIn(false);
    }

    builder.canLogIn(supportsExploreUrls);

    // Groups within 60 mi.
    long closeGroups = nearbyGroups.stream()
        .filter(g -> g.location.distanceTo(location) < 100000)
        .count();

    // TODO: Enable when it works.
    //builder.showWereNew(supportsExploreUrls && closeGroups < 50);
    builder.showWereNew(false);

    // TODO: Enable when it works.
    //builder.showUpdateAvailable(!client.supports(Feature.LATEST));
    builder.showUpdateAvailable(false);

    builder.showAddFriends(supportsExploreUrls);

    return builder.build();
  }

  private static boolean addDynamicSection(User user, List<Group> allGroups,
      List<HomeModel.Section> sections, String category, Predicate<Group> groupPicker) {
    List<GroupResponse> groups = user == null ? Collections.emptyList() : allGroups.stream()
        .filter(groupPicker)
        .limit(MAX_GROUPS_PER_SECTION)
        .map(g->g.toResponseFor(user, GroupsHtml::getCoverForHome))
        .collect(Collectors.toList());
    if (!groups.isEmpty()) {
      sections.add(new HomeModel.Section.Builder()
          .name(category)
          .encodedName(category)
          .groups(groups)
          .createUrl("/app/createCircle")
          .hasGroups(true)
          .build());
      return true;
    }
    return false;
  }

  // Copied from UrlEscapers. Don't use + for space—our iOS decoder doesn't support it.
  private static final Escaper URL_FORM_PARAMETER_ESCAPER =
      new PercentEscaper("-_.*", false);

  private static String parameterEncode(String value) {
    return URL_FORM_PARAMETER_ESCAPER.escape(value);
  }

  public static HtmlResponse getHome(Space space, Coordinates location) {
    User user = Users.current(false);
    Future<Address> address = Geocoding.reverseGeocode(location);
    List<Group> groups = GroupSearch
        .near(location)
        .space(space)
        .using(GroupRanker.EXPLORE)
        .limit(150)
        .run()
        .stream()
        .map(RankedGroup::group)
        .collect(toList());
    HomeModel home = GroupsHtml.getHomeModel(user, location, address, groups);
    return new HtmlResponse(Mustaches.toString(homeHtml, home));
  }

  /** Returns city from future or null if there was an error or timeout. */
  private static String toCity(Future<Address> futureAddress) {
    try {
      // This should have returned by now since we kicked it off before the group search.
      Address address = futureAddress.get(2, TimeUnit.SECONDS);
      return address == null ? null : address.city;
    } catch (Exception e) {
      logger.error("Reverse geocoding error", e);
      return null;
    }
  }

  private static ContentResponse getCoverForFeed(Group group) {
    if (group.hasCoverContent()) {
      Content cc = group.coverContent.get();
      String url = cc.url();
      return new ContentResponse(cc.uuid, cc.type, url, null);
    }
    return null;
  }

  public static NearbyFeedModel getFeedModel(User user, Coordinates location,
      Future<Address> address, List<Group> nearbyGroups) {
    List<NearbyFeedModel.Entry> feedEntries = nearbyGroups.stream()
      .map(group -> new NearbyFeedModel.Entry(
        group.toResponseFor(user, GroupsHtml::getCoverForFeed),
        toResponse(group.lastSignificantComment))
      )
      .collect(Collectors.toList());
    Client client = Clients.current();
    Ref<Content> profilePhotoRef = user == null ? null : user.photo;
    String profilePhoto = profilePhotoRef == null ? null : profilePhotoRef.get().url();
    return new NearbyFeedModel(client.uuid, feedEntries, location.toProto(),
        toCity(address), user != null, client.supports(Feature.CHANGE_LOCATION_URL),
        profilePhoto);
  }

  private static CommentResponse toResponse(Ref<Comment> commentRef) {
    if (commentRef == null) return null;
    return commentRef.get().toResponse();
  }

  public static HtmlResponse getFeedHtml(Space space, Coordinates location) {
    User user = Users.current(false);
    Future<Address> address = Geocoding.reverseGeocode(location);
    List<Group> groups = GroupSearch
        .near(location)
        .space(space)
        // Switch these lines to switch feed ranking algorithms.
        //.using(GroupRanking.NEARBY_FEED)
        .using(GroupRanker.NEARBY_FEED_BY_TIME)
        .limit(100)
        .run()
        .stream()
        .map(RankedGroup::group)
        .collect(toList());
    NearbyFeedModel nearby = getFeedModel(user, location, address, groups);
    return new HtmlResponse(Mustaches.toString(feedHtml, nearby));
  }
}
