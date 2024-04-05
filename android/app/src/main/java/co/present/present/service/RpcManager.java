package co.present.present.service;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.location.Location;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.UUID;

import javax.inject.Inject;

import co.present.present.BuildConfig;
import co.present.present.config.FeatureDataProvider;
import co.present.present.location.LocationDataProvider;
import co.present.present.location.LocationDataProviderKt;
import present.proto.Coordinates;
import present.proto.Platform;
import present.proto.RequestHeader;
import present.wire.rpc.client.RpcClient;
import present.wire.rpc.core.RpcFilter;
import present.wire.rpc.core.RpcProtocol;

/**
 * Wrapper for accessing Bubble servers with the same library as the server side.
 */
public class RpcManager {
    private static final String PREF_CLIENT_UUID = "clientUuid";
    private SharedPreferences preferences;
    private FeatureDataProvider featureDataProvider;
    private UUID clientUuid;
    private LocationDataProvider locationDataProvider;

    @SuppressLint("ApplySharedPref")
    public void clear() {
        preferences.edit()
                .putString(PREF_CLIENT_UUID, "")
                .commit();
    }

    @Inject
    public RpcManager(SharedPreferences preferences, LocationDataProvider locationDataProvider, FeatureDataProvider featureDataProvider) {
        if (getClientUuid() == null) {
            String uuid = preferences.getString(PREF_CLIENT_UUID, "");
            if (uuid.isEmpty()) {
                setClientUuid(UUID.randomUUID());
                preferences.edit()
                        .putString(PREF_CLIENT_UUID, clientUuid.toString())
                        .apply();
            }
            else {
                clientUuid = UUID.fromString(uuid);
            }
        }
        this.preferences = preferences;
        this.locationDataProvider = locationDataProvider;
        this.featureDataProvider = featureDataProvider;
    }

    @VisibleForTesting
    public RpcManager(UUID clientUuid) {
        setClientUuid(clientUuid);
    }

    public <T> T getService(Class<T> serviceType) {
        return rpcClient(newRpcFilter(getClientUuid(), UUID.randomUUID()), serviceType);
    }

    @NonNull
    @VisibleForTesting
    RpcFilter newRpcFilter(final UUID clientUuid, final UUID requestUuid) {
        return rpcInvocation ->
        {
            rpcInvocation.setHeader(generateHeader(clientUuid, requestUuid));
            return rpcInvocation.proceed();
        };
    }

    @NonNull
    public RequestHeader generateHeader() {
        return generateHeader(clientUuid, UUID.randomUUID());
    }

    @NonNull
    @VisibleForTesting
    RequestHeader generateHeader(UUID clientUuid, UUID requestUuid) {
        RequestHeader.Builder builder = new RequestHeader.Builder()
                .clientUuid(clientUuid.toString())
                .requestUuid(requestUuid.toString())
                .authorizationKey("not implemented") //wtf why is it so important to pass a dummy string?! server throws error w/o it
                .platform(Platform.ANDROID)
                .apiVersion(1)
                .clientVersion(String.valueOf(BuildConfig.VERSION_CODE));

        Location location = locationDataProvider.getCachedLocation();
        if (location != null) {
            builder.location(LocationDataProviderKt.toCoordinates(location));
        }
        if (featureDataProvider.getOverrideLocation() != null) {
            builder.selectedLocation(new Coordinates(featureDataProvider.getOverrideLatitude(), featureDataProvider.getOverrideLongitude(), 0.0));
        }

        return builder.build();
    }

    private <T> T rpcClient(RpcFilter filter, Class<T> serviceType) {
        return RpcClient.create(
                RpcProtocol.PROTO, getHost(), RequestHeader.class, serviceType, filter);
    }

    @NonNull
    private String getHost() {
        return featureDataProvider.getServerUrl();
    }

    public UUID getClientUuid() {
        return clientUuid;
    }

    private void setClientUuid(UUID clientUuid) {
        this.clientUuid = clientUuid;
    }
}
