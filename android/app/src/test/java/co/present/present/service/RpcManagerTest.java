package co.present.present.service;

import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.UUID;

import co.present.present.config.FeatureDataProvider;
import co.present.present.location.LocationDataProvider;
import present.proto.Platform;
import present.proto.RequestHeader;
import present.wire.rpc.core.RpcFilter;
import present.wire.rpc.core.RpcInvocation;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RpcManager}
 */
@RunWith(MockitoJUnitRunner.class)
public class RpcManagerTest {
    private static final String CLIENT_UUID_STRING = "00000029-0036-0001-0202-000000000556";
    private RpcManager underTest;
    @Mock
    private RpcInvocation rpcInvocation;
    @Mock
    LocationDataProvider locationDataProvider;
    @Mock
    SharedPreferences sharedPreferences;

    @Mock
    FeatureDataProvider featureDataProvider;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(sharedPreferences.getString(anyString(), anyString())).thenReturn(CLIENT_UUID_STRING);
        underTest = new RpcManager(sharedPreferences, locationDataProvider, featureDataProvider);
    }

    @Test
    public void newRpcFilter() throws Exception {
        RpcFilter result = underTest.newRpcFilter(UUID.fromString("29-36-1-202-555"),
                UUID.fromString("1-202-555-0193-30"));
        result.filter(rpcInvocation);

        ArgumentCaptor<RequestHeader> headerCaptor = ArgumentCaptor.forClass(RequestHeader.class);
        verify(rpcInvocation).setHeader(headerCaptor.capture());
        RequestHeader header = headerCaptor.getValue();
        assertEquals("00000029-0036-0001-0202-000000000555", header.clientUuid);
        assertEquals("00000001-0202-0555-0193-000000000030", header.requestUuid);
        verify(rpcInvocation).proceed();
    }

    @Test
    public void newHeader() throws Exception {
        RequestHeader result = underTest.generateHeader(
                UUID.fromString("35-38-6-41-45"),
                UUID.fromString("4-9-7-66633-49"));
        assertEquals("00000035-0038-0006-0041-000000000045", result.clientUuid);
        assertEquals("00000004-0009-0007-6633-000000000049", result.requestUuid);
        assertEquals(Platform.ANDROID, result.platform);
        assertEquals("not implemented", result.authorizationKey);
    }
}