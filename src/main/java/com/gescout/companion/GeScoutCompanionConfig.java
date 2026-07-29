package com.gescout.companion;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(GeScoutCompanionConfig.GROUP)
public interface GeScoutCompanionConfig extends Config
{
    String GROUP = "gescoutcompanion";

    @ConfigItem(
        keyName = "serverUrl",
        name = "GE Scout server",
        description = "LAN, Tailscale, or HTTPS address of your GE Scout server",
        position = 0
    )
    default String serverUrl()
    {
        return "http://127.0.0.1:3000";
    }

    @ConfigItem(
        keyName = "deviceName",
        name = "Device name",
        description = "Name shown in GE Scout's connected clients",
        position = 1
    )
    default String deviceName()
    {
        return "RuneLite";
    }

    @ConfigItem(
        keyName = "pairingCode",
        name = "Pairing code",
        description = "One-time code created in GE Scout Settings",
        position = 2
    )
    default String pairingCode()
    {
        return "";
    }

    @ConfigItem(
        keyName = "companionToken",
        name = "Paired token",
        description = "Stored automatically after pairing",
        hidden = true,
        secret = true,
        position = 3
    )
    default String companionToken()
    {
        return "";
    }

    @ConfigItem(
        keyName = "eventQueue",
        name = "Pending events",
        description = "Durable retry queue managed by the plugin",
        hidden = true,
        position = 4
    )
    default String eventQueue()
    {
        return "[]";
    }
}
