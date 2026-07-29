package com.gescout.companion;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import org.junit.Test;

public class ServerAddressPolicyTest
{
    @Test
    public void permitsLanAndLoopbackHttp() throws Exception
    {
        assertEquals(
            new URI("http://127.0.0.1:3000"),
            ServerAddressPolicy.requireSafeBaseUrl("http://127.0.0.1:3000/"));
        assertEquals(
            new URI("http://192.168.1.20:3000"),
            ServerAddressPolicy.requireSafeBaseUrl("http://192.168.1.20:3000"));
        assertEquals(
            new URI("http://100.100.10.20:3000"),
            ServerAddressPolicy.requireSafeBaseUrl("http://100.100.10.20:3000"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsPublicHttp() throws Exception
    {
        ServerAddressPolicy.requireSafeBaseUrl("http://8.8.8.8:3000");
    }

    @Test
    public void permitsHttps() throws Exception
    {
        assertEquals(
            new URI("https://ge.example.com"),
            ServerAddressPolicy.requireSafeBaseUrl("https://ge.example.com/"));
    }
}
