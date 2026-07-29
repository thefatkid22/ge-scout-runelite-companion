package com.gescout.companion;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class GeScoutCompanionPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(GeScoutCompanionPlugin.class);
        RuneLite.main(args);
    }
}
