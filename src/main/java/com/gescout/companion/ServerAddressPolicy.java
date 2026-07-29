package com.gescout.companion;

import java.net.InetAddress;
import java.net.URI;

final class ServerAddressPolicy
{
    private ServerAddressPolicy()
    {
    }

    static URI requireSafeBaseUrl(String value) throws Exception
    {
        URI uri = new URI(value == null ? "" : value.trim());
        if (uri.getHost() == null)
        {
            throw new IllegalArgumentException("Enter a valid GE Scout URL.");
        }
        if ("https".equalsIgnoreCase(uri.getScheme()))
        {
            return stripTrailingSlash(uri);
        }
        if (!"http".equalsIgnoreCase(uri.getScheme()))
        {
            throw new IllegalArgumentException("Use an HTTP or HTTPS URL.");
        }
        InetAddress address = InetAddress.getByName(uri.getHost());
        byte[] bytes = address.getAddress();
        boolean tailscaleV4 =
            bytes.length == 4
                && (bytes[0] & 0xff) == 100
                && (bytes[1] & 0xff) >= 64
                && (bytes[1] & 0xff) <= 127;
        if (!address.isLoopbackAddress()
            && !address.isSiteLocalAddress()
            && !address.isAnyLocalAddress()
            && !tailscaleV4)
        {
            throw new IllegalArgumentException(
                "Plain HTTP is limited to loopback, LAN, or Tailscale addresses.");
        }
        return stripTrailingSlash(uri);
    }

    private static URI stripTrailingSlash(URI uri) throws Exception
    {
        String text = uri.toString();
        while (text.endsWith("/"))
        {
            text = text.substring(0, text.length() - 1);
        }
        return new URI(text);
    }
}
