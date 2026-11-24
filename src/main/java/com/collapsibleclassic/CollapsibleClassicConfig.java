/*
 * Copyright (c) 2026, SplatterFan
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
		name = "Collapsible side panel",
		description = "The classic frame around Resizable Modern's side panel, which still collapses when you click the open tab. Hold Alt and drag the bottom stone row to move the whole panel; Alt right-click there puts it back in the corner.",
		position = 0
	)
	String sidePanelSection = "sidePanel";

	@ConfigSection(
		name = "Orbs",
		description = "Optional orb features: moving the orbs with Alt, preset layouts and default positions. Off by default, and switched off automatically while the Compact Orbs plugin is on.",
		position = 2,
		closedByDefault = true
	)
	String orbsSection = "orbs";

	@ConfigSection(
		name = "Orb visibility",
		description = "Choose when each orb, button, the compass and the minimap is hidden: in the default layout, in your moved layout, or both. Needs Orb features on.",
		position = 3,
		closedByDefault = true
	)
	String orbVisibilitySection = "orbVisibility";

	@ConfigItem(
		keyName = "orbFeatures",
		name = "Orb features",
		description = "Turn on this plugin's orb features (everything below and in Orb visibility). While off, or while the Compact Orbs plugin is on, the minimap and orbs are left completely alone.",
		section = orbsSection,
		position = 0
	)
	default boolean orbFeatures()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideLogoutX",
		name = "Hide Modern logout X",
		description = "Hide Resizable Modern's logout X by the minimap, since the side panel has its own logout stone. Switches itself off while Compact Orbs is running: use its own Hide logout X setting then.",
		section = sidePanelSection,
		position = 1
	)
	default boolean hideLogoutX()
	{
		return true;
	}

	@ConfigItem(
		keyName = "orbLayout",
		name = "Orb layout",
		description = "Pick a ready-made compact layout (the minimap is hidden and the orbs packed into the top-right corner), or Custom to place them yourself with Move orbs.",
		section = orbsSection,
		position = 1
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
		position = 2
	)
	default boolean moveOrbs()
	{
		return false;
	}

	@ConfigItem(
		keyName = "defaultOrbPositions",
		name = "Default orb positions",
		description = "Show the orbs, compass, minimap and multi-combat icon in their normal places. Your dragged positions are kept and come back when this is switched off.",
		section = orbsSection,
		position = 3
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
		position = 4
	)
	default Keybind defaultOrbPositionsKey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "movedWhenCollapsed",
		name = "Moved layout only when collapsed",
		description = "Use the default orb layout while the side panel is open, and switch to your moved layout (dragged orbs or a preset) when you collapse it. While on, this decides by itself and Default orb positions (and its hotkey and eye button) is ignored.",
		section = orbsSection,
		position = 5
	)
	default boolean movedWhenCollapsed()
	{
		return false;
	}

	@ConfigItem(
		keyName = "onScreenToggle",
		name = "On-screen toggle",
		description = "With Move orbs on, show a small button on the game screen that switches Default orb positions on and off. Drag it with Alt like the orbs.",
		section = orbsSection,
		position = 6
	)
	default boolean onScreenToggle()
	{
		return false;
	}

	@ConfigItem(
		keyName = "preventOrbClickThrough",
		name = "Prevent orb click-through",
		description = "Stop clicks on the orbs and buttons around the minimap from also reaching the game world underneath, so clicking an orb never walks you somewhere.",
		section = orbsSection,
		position = 7
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
		position = 8
	)
	default boolean resetOrbPositions()
	{
		return false;
	}

	@ConfigItem(
		keyName = "fillCollapsedGaps",
		section = sidePanelSection,
		name = "Fill collapsed gaps",
		description = "When the side panel is closed, draw the inventory background behind the two stone rows so the game world does not show through between the stones. Uses the active resource pack's background.",
		position = 0
	)
	default boolean fillCollapsedGaps()
	{
		return true;
	}

	@ConfigSection(
		name = "Status bars",
		description = "Hitpoints and Prayer bars drawn over the two pillars. Turn off RuneLite's own Status Bars plugin to avoid two sets.",
		position = 1,
		closedByDefault = true
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
		keyName = "hitpointsOrbVisibility",
		name = "Hitpoints orb",
		description = "When to hide the Hitpoints orb beside the minimap. The default layout is the game's own placement (Default orb positions on, or nothing moved); the moved layout is your dragged orbs or a preset.",
		section = orbVisibilitySection,
		position = 0
	)
	default OrbVisibility hitpointsOrb()
	{
		return OrbVisibility.SHOW;
	}

	@ConfigItem(
		keyName = "prayerOrbVisibility",
		name = "Prayer orb",
		description = "When to hide the Prayer orb. The default layout is the game's own placement (Default orb positions on, or nothing moved); the moved layout is your dragged orbs or a preset.",
		section = orbVisibilitySection,
		position = 1
	)
	default OrbVisibility prayerOrb()
	{
		return OrbVisibility.SHOW;
	}

	@ConfigItem(
		keyName = "runOrbVisibility",
		name = "Run orb",
		description = "When to hide the Run energy orb. The default layout is the game's own placement (Default orb positions on, or nothing moved); the moved layout is your dragged orbs or a preset.",
		section = orbVisibilitySection,
		position = 2
	)
	default OrbVisibility runOrb()
	{
		return OrbVisibility.SHOW;
	}

	@ConfigItem(
		keyName = "specialOrbVisibility",
		name = "Special attack orb",
		description = "When to hide the Special attack orb. The default layout is the game's own placement (Default orb positions on, or nothing moved); the moved layout is your dragged orbs or a preset.",
		section = orbVisibilitySection,
		position = 3
	)
	default OrbVisibility specialOrb()
	{
		return OrbVisibility.SHOW;
	}

	@ConfigItem(
		keyName = "xpButtonVisibility",
		name = "XP button",
		description = "When to hide the XP drops button. The default layout is the game's own placement (Default orb positions on, or nothing moved); the moved layout is your dragged orbs or a preset.",
		section = orbVisibilitySection,
		position = 4
	)
	default OrbVisibility xpButton()
	{
		return OrbVisibility.SHOW;
	}

	@ConfigItem(
		keyName = "worldMapOrbVisibility",
		name = "World map orb",
		description = "When to hide the world map orb. The default layout is the game's own placement (Default orb positions on, or nothing moved); the moved layout is your dragged orbs or a preset.",
		section = orbVisibilitySection,
		position = 5
	)
	default OrbVisibility worldMapOrb()
	{
		return OrbVisibility.SHOW;
	}

	@ConfigItem(
		keyName = "wikiBannerVisibility",
		name = "Wiki banner",
		description = "When to hide the Wiki banner. The default layout is the game's own placement (Default orb positions on, or nothing moved); the moved layout is your dragged orbs or a preset.",
		section = orbVisibilitySection,
		position = 6
	)
	default OrbVisibility wikiBanner()
	{
		return OrbVisibility.SHOW;
	}

	@ConfigItem(
		keyName = "activityOrbVisibility",
		name = "Activity adviser",
		description = "When to hide the activity adviser orb. The default layout is the game's own placement (Default orb positions on, or nothing moved); the moved layout is your dragged orbs or a preset.",
		section = orbVisibilitySection,
		position = 7
	)
	default OrbVisibility activityOrb()
	{
		return OrbVisibility.SHOW;
	}

	@ConfigItem(
		keyName = "storeOrbVisibility",
		name = "Store orb",
		description = "When to hide the store orb. The default layout is the game's own placement (Default orb positions on, or nothing moved); the moved layout is your dragged orbs or a preset.",
		section = orbVisibilitySection,
		position = 8
	)
	default OrbVisibility storeOrb()
	{
		return OrbVisibility.SHOW;
	}

	@ConfigItem(
		keyName = "compassVisibility",
		name = "Compass",
		description = "When to hide the compass. The default layout is the game's own placement (Default orb positions on, or nothing moved); the moved layout is your dragged orbs or a preset.",
		section = orbVisibilitySection,
		position = 10
	)
	default OrbVisibility compass()
	{
		return OrbVisibility.SHOW;
	}

	@ConfigItem(
		keyName = "minimapVisibility",
		name = "Minimap",
		description = "When to hide the minimap itself (the compass and orbs stay). Right-clicking the world map orb and choosing Minimise or Restore minimap flips it for the layout you're in. The default layout is the game's own placement (Default orb positions on, or nothing moved); the moved layout is your dragged orbs or a preset.",
		section = orbVisibilitySection,
		position = 9
	)
	default OrbVisibility minimap()
	{
		return OrbVisibility.SHOW;
	}
}
