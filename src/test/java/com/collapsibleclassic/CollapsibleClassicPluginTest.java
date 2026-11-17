package com.collapsibleclassic;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CollapsibleClassicPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(CollapsibleClassicPlugin.class);
        RuneLite.main(args);
    }
}
