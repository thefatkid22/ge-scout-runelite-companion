package com.gescout.companion;

import java.net.URI;
import okhttp3.Request;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GeScoutSyncClientTest
{
    @Test
    public void requestsReportTheCompanionReleaseVersion() throws Exception
    {
        Request request = GeScoutSyncClient.requestBuilder(
            new URI("https://ge-scout.example.test/api/sync/companion"))
            .build();

        assertEquals(
            "GE-Scout-RuneLite-Companion/0.2.0",
            request.header("User-Agent"));
    }
}
