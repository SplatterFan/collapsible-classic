/*
 * Copyright (c) 2026, SplatterFan
 * Copyright (c) 2018, Tomas Slusny <slusnucky@gmail.com>
 * Copyright (c) 2025, cue <https://github.com/its-cue>
 * Copyright (c) 2025, Richardant
 * Copyright (c) 2016-2017, Adam <Adam@sigterm.info>
 * Copyright (c) 2026, Frederick Campbell
 * Copyright (c) 2026, wukuna
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

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuEntry;
import net.runelite.api.ScriptID;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.VarClientIntChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.itemstats.ItemStatPlugin;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;

@PluginDescriptor(
	name = "Collapsible Classic",
	description = "Gives Resizable Modern the Resizable Classic side panel look, and keeps Modern's ability to collapse it. Optional Hitpoints and Prayer bars on the pillars.",
	tags = {"classic", "resizable", "modern", "collapse", "layout", "stones", "status", "bars", "interface"},
	// RuneLite treats two plugins as conflicting when their lists share any name. Compact Orbs lists Interface Layout
	// and Fixed Resizable Hybrid, so naming them here would stop the two plugins running together; the README warns
	// about them instead. Compact Orbs itself is welcome: the orb features stand down while it is on.
	conflicts = {"Menu Stones Hider", "Custom Game Tabs"}
)
@Slf4j
@PluginDependency(ItemStatPlugin.class)
public class CollapsibleClassicPlugin extends Plugin
{
	// Client scripts that lay out the top-level interface: redraw, sub-interface changes, side-panel and resize
	// customisation, and a click on a side stone.
	private static final int TOPLEVEL_SUBCHANGE = 903, PROC_TOPLEVEL_SUBCHANGE = 908;
	private static final int TOPLEVEL_SIDEBUTTON_OP = 914, TOPLEVEL_SIDE_CUSTOMIZE = 919;
	// Scripts that lay out the orbs around the minimap.
	private static final int ORBS_UPDATE_STORE = 2396, ORBS_UPDATE_ACTIVITY = 2480;
	private static final int WIKI_ICON_INIT = 3304, WORLD_MAP_UPDATE = 1700;

	/**
	 * RuneLite lets these pieces of Modern's interface be dragged while Alt is held. The frame stretches the stone
	 * rows' containers over the whole canvas, so their drag handles would cover the game view and swallow every
	 * Alt-click; and the panel must not be dragged out of its frame. The handles are set aside while it is drawn.
	 */
	private static final Set<String> DRAG_HANDLES = Set.of(
		"RESIZABLE_VIEWPORT_BOTTOM_LINE_TABS1",
		"RESIZABLE_VIEWPORT_BOTTOM_LINE_TABS2",
		"RESIZABLE_VIEWPORT_BOTTOM_LINE_INVENTORY_PARENT"
	);
	// Moving an orb out of the minimap area stretches the minimap's container in the same way.
	private static final String MINIMAP_DRAG_HANDLE = "RESIZABLE_MINIMAP_WIDGET";

	private static final int[] MINIMAP_PIECES = {
		InterfaceID.ToplevelPreEoc.MAP_NOCLICK_0, InterfaceID.ToplevelPreEoc.MAP_NOCLICK_1,
		InterfaceID.ToplevelPreEoc.MAP_NOCLICK_2, InterfaceID.ToplevelPreEoc.MAP_NOCLICK_3,
		InterfaceID.ToplevelPreEoc.MAP_NOCLICK_4, InterfaceID.ToplevelPreEoc.MAP_NOCLICK_5,
		InterfaceID.ToplevelPreEoc.MINIMAP, InterfaceID.ToplevelPreEoc.MAP_MINIMAP_GRAPHIC9
	};

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private CollapsibleClassicConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private WidgetStates states;

	@Inject
	private ClassicFrame frame;

	@Inject
	private SidePanelMover sidePanelMover;

	@Inject
	private StatusBars statusBars;

	@Inject
	private MovableOrbs movableOrbs;

	@Inject
	private ConfigManager configManager;

	@Inject
	private KeyManager keyManager;

	@Inject
	private PluginManager pluginManager;

	private static final String COMPACT_ORBS = "Compact Orbs";
	/** Compact Orbs is running: the orb features and the logout X are left to it. */
	private boolean compactOrbsActive;

	private final HotkeyListener defaultPositionsHotkey = new HotkeyListener(() -> config.defaultOrbPositionsKey())
	{
		@Override
		public void hotkeyPressed()
		{
			configManager.setConfiguration(CollapsibleClassicConfig.GROUP, "defaultOrbPositions", !config.defaultOrbPositions());
		}
	};

	private final List<Overlay> setAside = new ArrayList<>();

	/**
	 * RuneLite's own Alt-drag box for the multi-combat icon. While Default orb positions is on it is set aside and the
	 * icon goes back to its normal place, so it switches between default and moved along with the orbs.
	 */
	private static final String MULTICOMBAT_HANDLE = "MULTICOMBAT_RESIZABLE_MODERN";
	private final List<Overlay> multicombatAside = new ArrayList<>();
	private int[] hiddenOrbs = {};
	/** Orb widgets we marked as not click-through, with the value each had before. */
	private final Map<Widget, Boolean> clickBlocked = new IdentityHashMap<>();

	/**
	 * The orbs and buttons around the minimap, in both copies of the orbs interface. Marking them stops a click on
	 * an orb from also reaching the game world underneath (walking there), as Compact Orbs does.
	 */
	private static final int[] CLICK_BLOCKED_ORBS = {
		InterfaceID.Orbs.ORB_HEALTH, InterfaceID.Orbs.ORB_PRAYER, InterfaceID.Orbs.ORB_RUNENERGY,
		InterfaceID.Orbs.ORB_SPECENERGY, InterfaceID.Orbs.XP_DROPS, InterfaceID.Orbs.ORB_WORLDMAP,
		InterfaceID.Orbs.WIKI, InterfaceID.Orbs.ORB_CONTENTRECOM, InterfaceID.Orbs.ORB_STORE,
		InterfaceID.OrbsNomap.ORB_HEALTH, InterfaceID.OrbsNomap.ORB_PRAYER, InterfaceID.OrbsNomap.ORB_RUNENERGY,
		InterfaceID.OrbsNomap.ORB_SPECENERGY, InterfaceID.OrbsNomap.XP_DROPS, InterfaceID.OrbsNomap.ORB_WORLDMAP,
		InterfaceID.OrbsNomap.WIKI, InterfaceID.OrbsNomap.ORB_CONTENTRECOM, InterfaceID.OrbsNomap.ORB_STORE
	};
	private boolean minimapHandleSetAside;
	private boolean running;
	private boolean refreshPending;
	private int scriptDepth;

	@Provides
	CollapsibleClassicConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CollapsibleClassicConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(statusBars);
		keyManager.registerKeyListener(defaultPositionsHotkey);
		clientThread.invokeLater(() ->
		{
			running = true;
			scriptDepth = 0;
			compactOrbsActive = compactOrbsRunning();
			movableOrbs.start(this::refresh);
			sidePanelMover.start(this::refresh);
			refresh();
		});
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(statusBars);
		keyManager.unregisterKeyListener(defaultPositionsHotkey);
		clientThread.invokeLater(() ->
		{
			running = false;
			scriptDepth = 0;
			movableOrbs.stop();
			sidePanelMover.stop();
			restore();
			returnDragHandles();
			syncMulticombat(false);
		});
	}

	/** Undoes everything, then draws the frame again from the game's current state. */
	private void refresh()
	{
		if (!running || scriptDepth > 0)
		{
			return;
		}
		restore();
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			returnDragHandles();
			return;
		}
		boolean orbs = orbsOn();
		movableOrbs.setStandDown(compactOrbsActive);
		frame.setHideLogoutX(config.hideLogoutX() && !compactOrbsActive);
		frame.apply();
		panelCollapsed = frame.active() && !frame.isOpen();
		hiddenOrbs = orbs ? orbsToHide() : new int[0];
		for (int orb : hiddenOrbs)
		{
			states.hide(client.getWidget(orb));
		}
		if (orbs && config.preventOrbClickThrough())
		{
			for (int id : CLICK_BLOCKED_ORBS)
			{
				Widget orb = client.getWidget(id);
				if (orb != null && !clickBlocked.containsKey(orb))
				{
					clickBlocked.put(orb, orb.getNoClickThrough());
					orb.setNoClickThrough(true);
				}
			}
		}
		boolean active = frame.active();
		movableOrbs.apply(active && orbs);
		movableOrbs.syncHandles(active && orbs);
		syncMulticombat(active && orbs && defaultsOn(config));

		boolean minimapStretched = movableOrbs.isStretched();
		if (!active || minimapStretched != minimapHandleSetAside)
		{
			returnDragHandles();
		}
		if (active)
		{
			minimapHandleSetAside = minimapStretched;
			overlayManager.removeIf(overlay ->
			{
				String name = overlay.getName();
				if (!DRAG_HANDLES.contains(name) && !(minimapStretched && MINIMAP_DRAG_HANDLE.equals(name)))
				{
					return false;
				}
				setAside.add(overlay);
				return true;
			});
		}
	}

	private void restore()
	{
		clickBlocked.forEach(Widget::setNoClickThrough);
		clickBlocked.clear();
		frame.restore();
		movableOrbs.restore();
		states.restore();
		movableOrbs.afterRestore();
	}

	/** Puts the multi-combat icon in its normal place while default positions are on, and hands it back after. */
	private void syncMulticombat(boolean useDefault)
	{
		if (useDefault && multicombatAside.isEmpty())
		{
			overlayManager.removeIf(overlay ->
			{
				if (!MULTICOMBAT_HANDLE.equals(overlay.getName()))
				{
					return false;
				}
				multicombatAside.add(overlay);
				return true;
			});
			Widget icon = client.getWidget(InterfaceID.ToplevelPreEoc.MULTIWAY_ICON);
			if (icon != null)
			{
				icon.setForcedPosition(-1, -1);
				icon.revalidate();
			}
		}
		else if (!useDefault && !multicombatAside.isEmpty())
		{
			multicombatAside.forEach(overlayManager::add);
			multicombatAside.clear();
		}
	}

	private void returnDragHandles()
	{
		setAside.forEach(overlayManager::add);
		setAside.clear();
		minimapHandleSetAside = false;
	}

	private synchronized void scheduleRefresh()
	{
		if (refreshPending)
		{
			return;
		}
		refreshPending = true;
		clientThread.invokeLater(() ->
		{
			refreshPending = false;
			refresh();
		});
	}

	private boolean layoutScript(int id)
	{
		if (id == ScriptID.TOPLEVEL_REDRAW || id == ScriptID.TOPLEVEL_RESIZE_CUSTOMISE
			|| id == TOPLEVEL_SUBCHANGE || id == PROC_TOPLEVEL_SUBCHANGE
			|| id == TOPLEVEL_SIDE_CUSTOMIZE || id == TOPLEVEL_SIDEBUTTON_OP)
		{
			return true;
		}
		// The orbs' own scripts only matter while this plugin is changing the orbs.
		return orbsOn() && (id == ORBS_UPDATE_STORE || id == ORBS_UPDATE_ACTIVITY || id == WORLD_MAP_UPDATE
			|| id == WIKI_ICON_INIT || id == ScriptID.WIKI_ICON_UPDATE);
	}

	/** Orb features are switched on and Compact Orbs is not running. */
	private boolean orbsOn()
	{
		return config.orbFeatures() && !compactOrbsActive;
	}

	private boolean compactOrbsRunning()
	{
		for (Plugin plugin : pluginManager.getPlugins())
		{
			PluginDescriptor descriptor = plugin.getClass().getAnnotation(PluginDescriptor.class);
			if (descriptor != null && COMPACT_ORBS.equals(descriptor.name()) && pluginManager.isPluginActive(plugin))
			{
				return true;
			}
		}
		return false;
	}

	@Subscribe
	public void onPluginChanged(PluginChanged event)
	{
		if (!running)
		{
			return;
		}
		boolean compact = compactOrbsRunning();
		if (compact != compactOrbsActive)
		{
			// Hand the orbs over cleanly: undo everything with the old setting, then redraw with the new one.
			clientThread.invokeLater(() ->
			{
				if (compact)
				{
					// Undoing must not resize anything Compact Orbs now owns.
					movableOrbs.setStandDown(true);
				}
				restore();
				compactOrbsActive = compact;
				refresh();
			});
		}
	}

	// The game's layout scripts must see its own, untouched interface. Everything is undone before the outermost
	// one runs and re-applied after it finishes.
	@Subscribe(priority = 2)
	public void onScriptPreFired(ScriptPreFired event)
	{
		if (running && layoutScript(event.getScriptId()) && scriptDepth++ == 0)
		{
			restore();
		}
	}

	@Subscribe(priority = -2)
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (running && layoutScript(event.getScriptId()))
		{
			scriptDepth = Math.max(0, scriptDepth - 1);
			refresh();
		}
	}

	@Subscribe
	public void onVarClientIntChanged(VarClientIntChanged event)
	{
		if (running && event.getIndex() == VarClientID.TOPLEVEL_PANEL)
		{
			scheduleRefresh();
		}
	}

	@Subscribe(priority = -2)
	public void onBeforeRender(BeforeRender event)
	{
		if (!running || scriptDepth > 0 || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		if (frame.stale() || hiddenOrbShowing())
		{
			refresh();
		}
		movableOrbs.enforce();
	}

	/** The game re-shows orbs through its own scripts, so this is checked before every frame. */
	private boolean hiddenOrbShowing()
	{
		for (int orb : hiddenOrbs)
		{
			Widget widget = client.getWidget(orb);
			if (widget != null && !widget.isSelfHidden())
			{
				return true;
			}
		}
		return false;
	}

	private int[] orbsToHide()
	{
		List<Integer> orbs = new ArrayList<>();
		// With the minimap minimised the game swaps in a second copy of the orbs (OrbsNomap), so both are hidden.
		addIf(orbs, hidden(config.hitpointsOrb()), InterfaceID.Orbs.ORB_HEALTH, InterfaceID.OrbsNomap.ORB_HEALTH);
		addIf(orbs, hidden(config.prayerOrb()), InterfaceID.Orbs.ORB_PRAYER, InterfaceID.OrbsNomap.ORB_PRAYER);
		addIf(orbs, hidden(config.runOrb()), InterfaceID.Orbs.ORB_RUNENERGY, InterfaceID.OrbsNomap.ORB_RUNENERGY);
		addIf(orbs, hidden(config.specialOrb()), InterfaceID.Orbs.ORB_SPECENERGY, InterfaceID.OrbsNomap.ORB_SPECENERGY);
		addIf(orbs, hidden(config.xpButton()), InterfaceID.Orbs.XP_DROPS, InterfaceID.OrbsNomap.XP_DROPS);
		addIf(orbs, hidden(config.worldMapOrb()), InterfaceID.Orbs.ORB_WORLDMAP, InterfaceID.OrbsNomap.ORB_WORLDMAP);
		addIf(orbs, hidden(config.wikiBanner()), InterfaceID.Orbs.WIKI, InterfaceID.OrbsNomap.WIKI);
		addIf(orbs, hidden(config.activityOrb()), InterfaceID.Orbs.ORB_CONTENTRECOM, InterfaceID.OrbsNomap.ORB_CONTENTRECOM);
		addIf(orbs, hidden(config.storeOrb()), InterfaceID.Orbs.ORB_STORE, InterfaceID.OrbsNomap.ORB_STORE);
		addIf(orbs, hidden(config.compass()),
			InterfaceID.ToplevelPreEoc.MAP_MINIMAP_GRAPHIC6, InterfaceID.ToplevelPreEoc.COMPASSCLICK);
		if (minimapHidden(config))
		{
			for (int piece : MINIMAP_PIECES)
			{
				orbs.add(piece);
			}
		}
		return orbs.stream().mapToInt(Integer::intValue).toArray();
	}

	/** The side panel is collapsed, as of the last redraw. */
	private static volatile boolean panelCollapsed;

	/**
	 * Default positions are in effect. With Moved layout only when collapsed on, the side panel decides by itself
	 * (open: default, collapsed: moved), whatever Default orb positions is set to; otherwise that setting (or its
	 * hotkey and eye button) decides.
	 */
	static boolean defaultsOn(CollapsibleClassicConfig config)
	{
		return config.movedWhenCollapsed() ? !panelCollapsed : config.defaultOrbPositions();
	}

	/** A preset always hides the minimap; otherwise its Orb visibility setting decides, for the layout in use. */
	static boolean minimapHidden(CollapsibleClassicConfig config)
	{
		return presetActive(config) || config.minimap().hiddenIn(defaultLayout(config));
	}

	/**
	 * The game's own placement is showing: Default orb positions is on, or nothing is moved (Move orbs off and no
	 * preset). Otherwise your moved layout is showing.
	 */
	static boolean defaultLayout(CollapsibleClassicConfig config)
	{
		return defaultsOn(config) || !config.moveOrbs() && config.orbLayout() == OrbLayout.CUSTOM;
	}

	private boolean hidden(OrbVisibility visibility)
	{
		return visibility.hiddenIn(defaultLayout(config));
	}

	/** A ready-made layout is chosen, and Default orb positions is not overriding it. */
	static boolean presetActive(CollapsibleClassicConfig config)
	{
		return config.orbLayout() != OrbLayout.CUSTOM && !defaultsOn(config);
	}

	private static void addIf(List<Integer> orbs, boolean hide, int... components)
	{
		if (hide)
		{
			for (int component : components)
			{
				orbs.add(component);
			}
		}
	}

	@Subscribe(priority = -2)
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (running)
		{
			states.discardRetired();
			refresh();
		}
	}

	@Subscribe(priority = -2)
	public void onGameStateChanged(GameStateChanged event)
	{
		if (!running)
		{
			return;
		}
		if (event.getGameState() != GameState.LOGGED_IN)
		{
			restore();
			scriptDepth = 0;
		}
		else
		{
			refresh();
		}
	}

	/** Typing ::ccdebug in the chat writes a report of the orbs' state to the log and the chat box. */
	@Subscribe
	public void onCommandExecuted(CommandExecuted event)
	{
		if (!"ccdebug".equalsIgnoreCase(event.getCommand()))
		{
			return;
		}
		List<String> lines = movableOrbs.debugReport();
		for (String line : lines)
		{
			log.info("[CC debug] {}", line);
		}
		for (String line : lines)
		{
			if (line.startsWith("moveOrbs") || line.startsWith("WORLD_MAP:") || line.startsWith("WIKI:"))
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "CC debug " + line, null);
			}
		}
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "CC debug: full report written to the log ("
			+ lines.size() + " lines, search for CC debug)", null);
	}

	/** The interface group of the orbs beside the full minimap (not the minimised-minimap copy). */
	private static final int ORBS_GROUP = InterfaceID.Orbs.UNIVERSE >>> 16;

	/**
	 * The world map orb's right-click option to minimise the minimap. Instead of switching the game to its
	 * minimised layout, it flips Hide minimap, so the minimap goes and comes back exactly as with that setting.
	 * Left alone when a preset hides the minimap anyway, and in the game's own minimised layout, so that layout's
	 * own option can still restore it.
	 */
	private boolean minimapToggle(String option, String target, int widgetId)
	{
		if (!running || !orbsOn() || !frame.active() || presetActive(config) || widgetId == -1
			|| widgetId >>> 16 != ORBS_GROUP)
		{
			return false;
		}
		String text = ((option == null ? "" : option) + " " + (target == null ? "" : target)).toLowerCase();
		return text.contains("minimap") || text.contains("minimise") || text.contains("minimize");
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		// Taking damage counts as combat for the status bars, even when you aren't attacking anything.
		if (running && event.getActor() == client.getLocalPlayer())
		{
			statusBars.tookHit();
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		MenuEntry entry = event.getMenuEntry();
		if (minimapToggle(entry.getOption(), entry.getTarget(), entry.getParam1()) && minimapHidden(config))
		{
			entry.setOption("Restore minimap");
			entry.setTarget("");
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (minimapToggle(event.getMenuOption(), event.getMenuTarget(), event.getParam1()))
		{
			event.consume();
			// Flips the minimap for the layout in use; the setting for the other layout is kept.
			configManager.setConfiguration(CollapsibleClassicConfig.GROUP, "minimapVisibility",
				config.minimap().toggledIn(defaultLayout(config)));
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (running && CollapsibleClassicConfig.GROUP.equals(event.getGroup()))
		{
			if ("resetOrbPositions".equals(event.getKey()) && "true".equals(event.getNewValue()))
			{
				// A one-shot switch: forget every dragged position, then untick itself.
				clientThread.invokeLater(() ->
				{
					movableOrbs.resetAllPositions();
					sidePanelMover.resetPosition();
					configManager.setConfiguration(CollapsibleClassicConfig.GROUP, "resetOrbPositions", false);
				});
			}
			scheduleRefresh();
		}
	}

	@Subscribe
	public void onProfileChanged(ProfileChanged event)
	{
		if (running)
		{
			scheduleRefresh();
		}
	}
}
