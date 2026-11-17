package com.collapsibleclassic;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(CollapsibleClassicConfig.GROUP)
public interface CollapsibleClassicConfig extends Config
{
	String GROUP = "collapsibleclassic";

	@ConfigItem(
		keyName = "fillCollapsedGaps",
		name = "Fill collapsed gaps",
		description = "When the side panel is closed, draw the inventory background behind the two stone rows so the game world does not show through between the stones. Uses the active resource pack's background.",
		position = 0
	)
	default boolean fillCollapsedGaps()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hideXpButton",
		name = "Hide XP button",
		description = "Hide the XP button beside the minimap.",
		position = 1
	)
	default boolean hideXpButton()
	{
		return false;
	}

	@ConfigSection(
		name = "Status bars",
		description = "Hitpoints and Prayer bars drawn over the two pillars. Turn off RuneLite's own Status Bars plugin to avoid two sets.",
		position = 2
	)
	String statusBarsSection = "statusBars";

	@ConfigItem(
		keyName = "statusBars",
		name = "Show status bars",
		description = "Draw Hitpoints and Prayer bars over the two pillars, with the icon, current value and status-effect colours. They hide when the side panel is closed.",
		section = statusBarsSection,
		position = 0
	)
	default boolean statusBars()
	{
		return false;
	}

	@ConfigItem(
		keyName = "restorePreview",
		name = "Restore preview",
		description = "Show how much Hitpoints or Prayer the inventory item under the mouse would restore.",
		section = statusBarsSection,
		position = 1
	)
	default boolean restorePreview()
	{
		return true;
	}

	@Range(max = 500)
	@Units(Units.TICKS)
	@ConfigItem(
		keyName = "hideAfterCombatDelay",
		name = "Hide after combat",
		description = "Ticks to keep the status bars showing after combat ends. 0 always shows them.",
		section = statusBarsSection,
		position = 2
	)
	default int hideAfterCombatDelay()
	{
		return 0;
	}
}
