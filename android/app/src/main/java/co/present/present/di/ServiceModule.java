package co.present.present.di;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Named;
import javax.inject.Singleton;

import androidx.room.Room;
import co.present.present.config.FeatureDataProvider;
import co.present.present.db.BlockedDao;
import co.present.present.db.CircleDao;
import co.present.present.db.CityDao;
import co.present.present.db.CurrentUserDao;
import co.present.present.db.Database;
import co.present.present.db.FriendRelationshipDao;
import co.present.present.db.SpacesDao;
import co.present.present.db.UserDao;
import co.present.present.feature.ContactsImpl;
import co.present.present.feature.GetContacts;
import co.present.present.feature.GetFriendRequests;
import co.present.present.feature.GetFriendRequestsImpl;
import co.present.present.feature.GetJoined;
import co.present.present.feature.GetJoinedImpl;
import co.present.present.feature.common.FriendUser;
import co.present.present.feature.common.FriendUserImpl;
import co.present.present.feature.common.GetCircle;
import co.present.present.feature.common.GetCircleImpl;
import co.present.present.feature.common.GetFriends;
import co.present.present.feature.common.GetFriendsImpl;
import co.present.present.feature.common.GetMembers;
import co.present.present.feature.common.GetMembersImpl;
import co.present.present.feature.common.ResolveCategoryUrl;
import co.present.present.feature.common.ResolveCircleUrl;
import co.present.present.feature.common.ResolveUrl;
import co.present.present.feature.common.ResolveUserUrl;
import co.present.present.feature.common.viewmodel.GetCurrentUser;
import co.present.present.feature.common.viewmodel.GetCurrentUserImpl;
import co.present.present.feature.create.CircleUploadPhotoImpl;
import co.present.present.feature.detail.GetMemberRequests;
import co.present.present.feature.detail.GetMembershipRequestsImpl;
import co.present.present.feature.detail.chat.GetComments;
import co.present.present.feature.detail.chat.GetCommentsImpl;
import co.present.present.feature.detail.info.JoinCircle;
import co.present.present.feature.detail.info.JoinCircleImpl;
import co.present.present.feature.discovery.RefreshCircles;
import co.present.present.feature.discovery.RefreshCirclesImpl;
import co.present.present.feature.discovery.Searchable;
import co.present.present.feature.discovery.SearchableImpl;
import co.present.present.feature.profile.GetBlocked;
import co.present.present.feature.profile.GetBlockedImpl;
import co.present.present.feature.profile.ProfileUploadPhotoImpl;
import co.present.present.location.ContactPermissions;
import co.present.present.location.LocationDataProvider;
import co.present.present.location.LocationPermissions;
import co.present.present.model.Category;
import co.present.present.model.Circle;
import co.present.present.model.User;
import co.present.present.service.Filesystem;
import co.present.present.service.RpcManager;
import co.present.present.user.UserDataProvider;
import dagger.Module;
import dagger.Provides;
import present.proto.ActivityService;
import present.proto.ContentService;
import present.proto.GroupService;
import present.proto.UrlResolverService;
import present.proto.UserService;

/**
 * Dagger injection module for RPC related classes.
 */
@Module
public class ServiceModule {
    public static final String LEGACY_DB_FILE_NAME = "present_db";
    private static final String DB_FILE_NAME = "present_db_2";

    @Provides @Singleton
    protected RpcManager provideRpcManager(SharedPreferences preferences,
                                           LocationDataProvider locationDataProvider,
                                           FeatureDataProvider featureDataProvider) {
        return new RpcManager(preferences, locationDataProvider, featureDataProvider);
    }

    @Provides
    @Singleton
    @Named("feedSearch")
    Searchable provideFeedSearchable() {
        return new SearchableImpl();
    }

    @Provides
    Searchable provideSearchable() {
        return new SearchableImpl();
    }

    @Provides @Singleton
    protected UserService provideUserService(RpcManager rpcManager) {
        return rpcManager.getService(UserService.class);
    }

    @Provides @Singleton
    protected ActivityService provideActivityService(RpcManager rpcManager) {
        return rpcManager.getService(ActivityService.class);
    }

    @Provides @Singleton
    protected ContentService provideContentService(RpcManager rpcManager) {
        return rpcManager.getService(ContentService.class);
    }

    @Provides @Singleton
    protected GroupService provideGroupService(RpcManager rpcManager) {
        return rpcManager.getService(GroupService.class);
    }

    @Provides @Singleton
    protected GetBlocked provideGetBlocked(UserService userService, BlockedDao blockedDao) {
        return new GetBlockedImpl(blockedDao, userService);
    }

    @Provides @Singleton
    protected UrlResolverService provideUrlResolverService(RpcManager rpcManager) {
        return rpcManager.getService(UrlResolverService.class);
    }

    @Provides @Singleton GetFriendRequests provideGetFriendRequests(
            Database database, UserService userService,
            FriendRelationshipDao friendRelationshipDao,
            UserDao userDao, GetCurrentUser getCurrentUser) {
        return new GetFriendRequestsImpl(database, userService, friendRelationshipDao, userDao, getCurrentUser);
    }

    @Provides @Singleton
    UserDataProvider provideUserDataProvider(SharedPreferences preferences, Database database, RpcManager rpcManager) {
        return new UserDataProvider(database, rpcManager, preferences);
    }

    @Provides @Singleton
    Database providesPresentDatabase(Context context) {
        return Room.databaseBuilder(context.getApplicationContext(), Database.class, DB_FILE_NAME)
                .fallbackToDestructiveMigration()
                .build();
    }

    @Provides @Singleton
    UserDao provideUserDao(Database database) {
        return database.userDao();
    }

    @Provides @Singleton
    FriendRelationshipDao provideFriendRelationshipDao(Database database) {
        return database.friendRelationshipDao();
    }

    @Provides @Singleton
    GetMemberRequests provideGetMemberRequests(GroupService groupService, GetCurrentUser getCurrentUser) {
        return new GetMembershipRequestsImpl(groupService, getCurrentUser);
    }

    @Provides @Singleton
    CityDao provideCityDao(Database database) {
        return database.cityDao();
    }

    @Provides @Singleton
    SpacesDao provideSpacesDao() {
        return new SpacesDao();
    }

    @Provides @Singleton
    CurrentUserDao provideCurrentUserDao(Database database) {
        return database.currentUserDao();
    }

    @Provides @Singleton
    GetFriends provideGetFriends(UserService userService, Database database, UserDao userDao, FriendRelationshipDao friendRequestDao) {
        return new GetFriendsImpl(userService, database, userDao, friendRequestDao);
    }

    @Provides @Singleton
    CircleDao provideCircleDao(Database database) {
        return database.circleDao();
    }

    @Provides @Singleton
    BlockedDao provideBlockedDao(Database database) {
        return database.blockedDao();
    }

    @Provides @Singleton
    JoinCircle provideJoinCircle(GroupService groupService, CircleDao circleDao, UserDao userDao, GetMembers getMembers) {
        return new JoinCircleImpl(groupService, circleDao, userDao, getMembers);
    }

    @Provides @Singleton GetContacts provideGetContacts(UserService userService, Application application) {
        return new ContactsImpl(userService, application);
    }

    @Provides @Singleton
    FriendUser provideFriendUser(UserService userService, UserDao userDao, FriendRelationshipDao friendRelationshipDao) {
        return new FriendUserImpl(friendRelationshipDao, userDao, userService);
    }

    @Provides
    GetCurrentUser provideGetCurrentUser(CurrentUserDao currentUserDao) {
        return new GetCurrentUserImpl(currentUserDao);
    }

    @Provides
    GetJoined provideGetJoined(GetCurrentUser getCurrentUser, GroupService circleService, CircleDao circleDao) {
        return new GetJoinedImpl(getCurrentUser, circleService, circleDao);
    }

    @Provides
    GetCircle provideGetCircle(CircleDao circleDao, GroupService groupService) {
        return new GetCircleImpl(circleDao, groupService);
    }

    @Provides @Singleton
    GetMembers provideGetMembers(GetCircle getCircle, GroupService groupService, GetCurrentUser getCurrentUser) {
        return new GetMembersImpl(getCircle, groupService, getCurrentUser);
    }

    @Provides RefreshCircles provideRefreshCircles(CircleDao circleDao,
                                                   Database database,
                                                   GroupService groupService,
                                                   FeatureDataProvider featureDataProvider,
                                                   LocationDataProvider locationDataProvider,
                                                   SpacesDao spacesDao,
                                                   GetCurrentUser getCurrentUser,
                                                   Application application) {
        return new RefreshCirclesImpl(circleDao, database, groupService, featureDataProvider, locationDataProvider, spacesDao, getCurrentUser, application);
    }

    @Provides
    GetComments provideGetComments(GroupService groupService, GetCurrentUser getCurrentUser, RpcManager rpcManager) {
        return new GetCommentsImpl(groupService, getCurrentUser, rpcManager);
    }

    @Provides @Singleton ContactPermissions provideContactPermissions() {
        return ContactPermissions.INSTANCE;
    }

    @Provides @Singleton
    LocationPermissions provideLocationPermissions() {
        return new LocationPermissions();
    }

    @Provides
    CircleUploadPhotoImpl provideCircleUploadPhoto(ContentService contentService, Filesystem filesystem) {
        return new CircleUploadPhotoImpl(contentService, filesystem);
    }

    @Provides
    ProfileUploadPhotoImpl provideProfileUploadPhoto(ContentService contentService, Filesystem filesystem, UserDataProvider userDataProvider) {
        return new ProfileUploadPhotoImpl(contentService, filesystem, userDataProvider);
    }

    @Provides @Singleton
    ResolveUrl<Circle> provideResolveCircleUrl(UrlResolverService urlResolverService, CircleDao circleDao, UserDao userDao) {
        return new ResolveCircleUrl(urlResolverService, circleDao, userDao);
    }

    @Provides @Singleton
    ResolveUrl<User> provideResolveUserUrl(UrlResolverService urlResolverService, CircleDao circleDao, UserDao userDao) {
        return new ResolveUserUrl(urlResolverService, circleDao, userDao);
    }

    @Provides @Singleton
    ResolveUrl<Category> provideResolveCategoryUrl(UrlResolverService urlResolverService, FeatureDataProvider featureDataProvider, CircleDao circleDao, UserDao userDao) {
        return new ResolveCategoryUrl(urlResolverService, featureDataProvider, circleDao, userDao);
    }

    @Provides @Singleton
    protected FeatureDataProvider provideFeatureDataProvider(SharedPreferences sharedPreferences) {
        return new FeatureDataProvider(sharedPreferences);
    }

    @Provides @Singleton
    protected LocationDataProvider provideLocationDataProvider(LocationPermissions locationPermissions) {
        return new LocationDataProvider(locationPermissions);
    }

}
