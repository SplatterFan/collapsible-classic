package com.collapsibleclassic;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(CollapsibleClassicConfig.GROUP)
public interface CollapsibleClassicConfig extends Config
{
	String GROUP = "collapsibleclassic";

	@ConfigSection(
		name = "Orbs",
		description = "Orb layout, moving the orbs with Alt, and hiding individual orbs and buttons around the minimap.",
		position = 7
	)
	String orbsSection = "orbs";

	@ConfigSection(
		name = "Orb visibility",
		description = "Hide individual orbs, buttons, the compass or the minimap in Resizable Modern.",
		position = 8,
		closedByDefault = true
	)
	String orbVisibilitySection = "orbVisibility";

	@ConfigItem(
		keyName = "orbLayout",
		name = "Orb layout",
		description = "Pick a ready-made compact layout (the minimap is hidden and the orbs packed into the top-right corner), or Custom to place them yourself with Move orbs.",
		section = orbsSection,
		position = 0
	)
	default OrbLayout orbLayout()
	{
		return OrbLayout.CUSTOM;
	}

	@ConfigItem(
		keyName = "moveOrbs",
		name = "Move orbs",
		description = "Let the orbs, compass and minimap (or a whole preset layout) be dragged anywhere while Alt is held. Alt right-click puts it back.",
		section = orbsSection,
		position = 1
	)
	default boolean moveOrbs()
	{
		return false;
	}

	@ConfigItem(
		keyName = "defaultOrbPositions",
		name = "Default orb positions",
		description = "Show the orbs, compass and minimap in their normal places. Your dragged positions are kept and come back when this is switched off.",
		section = orbsSection,
		position = 2
	)
	default boolean defaultOrbPositions()
	{
		return false;
	}

	@ConfigItem(
		keyName = "defaultOrbPositionsKey",
		name = "Default positions hotkey",
		description = "A key that switches Default orb positions on and off.",
		section = orbsSection,
		position = 3
	)
	default Keybind defaultOrbPositionsKey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "onScreenToggle",
		name = "On-screen toggle",
		description = "With Move orbs on, show a small button on the game screen that switches Default orb positions on and off. Drag it with Alt like the orbs.",
		section = orbsSection,
		position = 5
	)
	default boolean onScreenToggle()
	{
		return false;
	}

	@ConfigItem(
		keyName = "defaultPositionsShowMinimap",
		name = "Show minimap with defaults",
		description = "While Default orb positions is on, show the minimap even if Hide minimap is ticked.",
		section = orbsSection,
		position = 4
	)
	default boolean defaultPositionsShowMinimap()
	{
		return true;
	}

	@ConfigItem(
		keyName = "preventOrbClickThrough",
		name = "Prevent orb click-through",
		description = "Stop clicks on the orbs and buttons around the minimap from also reaching the game world underneath, so clicking an orb never walks you somewhere.",
		section = orbsSection,
		position = 6
	)
	default boolean preventOrbClickThrough()
	{
		return true;
	}

	@ConfigItem(
		keyName = "resetOrbPositions",
		name = "Reset all positions",
		description = "Tick to put every dragged orb, the compass, the minimap, any moved preset and the side panel back in its normal place. It unticks itself.",
		section = orbsSection,
		position = 7
	)
	default boolean resetOrbPositions()
	{
		return false;
	}

	@ConfigItem(
		keyName = "moveSidePanel",
		name = "Move side panel",
		description = "Let the whole side panel (frame, stones and inventory) be dragged anywhere as one piece while Alt is held. Alt right-click puts it back in the corner. Switch it off once placed: the panel stays put and Alt-clicks reach the inventory again.",
		position = 4
	)
	default boolean moveSidePanel()
	{
		return false;
	}

	@ConfigItem(
		keyName = "fillCollapsedGaps",
		name = "Fill collapsed gaps",
		description = "When the side panel is closed, draw the inventory background behind the two stone rows so the game world does not show through between the stones. Uses the active resource pack's background.",
		position = 5
	)
	default boolean fillCollapsedGaps()
	{
		return true;
	}

	@ConfigSection(
		name = "Status bars",
		description = "Hitpoints and Prayer bars drawn over the two pillars. Turn off RuneLite's own Status Bars plugin to avoid two sets.",
		position = 6
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
		return true;
	}

	@Range(max = 500)
	@Units(Units.TICKS)
	@ConfigItem(
		keyName = "hideAfterCombatDelay",
		name = "Hide after combat",
		description = "Ticks to keep the status bars showing after combat ends. 0 always shows them.",
		section = statusBarsSection,
		position = 1
	)
	default int hideAfterCombatDelay()
	{
		return 50;
	}

	@ConfigItem(
		keyName = "hideHitpointsOrb",
		name = "Hide Hitpoints orb",
		description = "Hide the Hitpoints orb beside the minimap.",
		section = orbVisibilitySection,
		position = 0
	)
	default boolean hideHitpointsOrb()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hidePrayerOrb",
		name = "Hide Prayer orb",
		description = "Hide the Prayer orb beside the minimap. Quick-prayers can then only be toggled by hotkey.",
		section = orbVisibilitySection,
		position = 1
	)
	default boolean hidePrayerOrb()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideRunOrb",
		name = "Hide Run orb",
		description = "Hide the Run energy orb beside the minimap.",
		section = orbVisibilitySection,
		position = 2
	)
	default boolean hideRunOrb()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideSpecialOrb",
		name = "Hide Special attack orb",
		description = "Hide the Special attack orb beside the minimap.",
		section = orbVisibilitySection,
		position = 3
	)
	default boolean hideSpecialOrb()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideXpButton",
		name = "Hide XP button",
		description = "Hide the XP button beside the minimap.",
		section = orbVisibilitySection,
		position = 4
	)
	default boolean hideXpButton()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideWorldMapOrb",
		name = "Hide World map orb",
		description = "Hide the World map orb beside the minimap.",
		section = orbVisibilitySection,
		position = 5
	)
	default boolean hideWorldMapOrb()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideWikiBanner",
		name = "Hide Wiki banner",
		description = "Hide the Wiki banner under the minimap.",
		section = orbVisibilitySection,
		position = 6
	)
	default boolean hideWikiBanner()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideActivityOrb",
		name = "Hide Activity Adviser orb",
		description = "Hide the Activity Adviser orb beside the minimap.",
		section = orbVisibilitySection,
		position = 7
	)
	default boolean hideActivityOrb()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideStoreOrb",
		name = "Hide Store orb",
		description = "Hide the Store orb beside the minimap.",
		section = orbVisibilitySection,
		position = 8
	)
	default boolean hideStoreOrb()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideCompass",
		name = "Hide compass",
		description = "Hide the compass in Resizable Modern.",
		section = orbVisibilitySection,
		position = 10
	)
	default boolean hideCompass()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideMinimap",
		name = "Hide minimap",
		description = "Hide the minimap itself in Resizable Modern. The compass and orbs stay, and can be dragged elsewhere while Alt is held. Right-clicking the world map orb and choosing Minimise minimap (or Restore minimap) flips this too.",
		section = orbVisibilitySection,
		position = 9
	)
	default boolean hideMinimap()
	{
		return false;
	}
}
