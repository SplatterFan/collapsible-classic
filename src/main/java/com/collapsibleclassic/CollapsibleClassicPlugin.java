/*
 * Copyright (c) 2026, SplatterFan
 * The redraw handling is adapted from Interface Layout (https://github.com/wukuna/interface-layout),
 * Copyright (c) 2026, wukuna and contributors. BSD 2-Clause; see LICENSE.
 */
package com.collapsibleclassic;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ScriptID;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.GameStateChanged;
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
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.itemstats.ItemStatPlugin;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Collapsible Classic",
	description = "Gives Resizable Modern the Resizable Classic side panel look, and keeps Modern's ability to collapse it. Optional Hitpoints and Prayer bars on the pillars.",
	tags = {"classic", "resizable", "modern", "collapse", "layout", "stones", "status", "bars", "interface"},
	conflicts = {"Interface Layout", "Fixed Resizable Hybrid", "Menu Stones Hider", "Custom Game Tabs"}
)
@PluginDependency(ItemStatPlugin.class)
public class CollapsibleClassicPlugin extends Plugin
{
	// Client scripts that lay out the top-level interface: redraw, sub-interface changes, side-panel and resize
	// customisation, and a click on a side stone.
	private static final int TOPLEVEL_SUBCHANGE = 903, PROC_TOPLEVEL_SUBCHANGE = 908;
	private static final int TOPLEVEL_SIDEBUTTON_OP = 914, TOPLEVEL_SIDE_CUSTOMIZE = 919;

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
	private StatusBars statusBars;

	private final List<Overlay> setAside = new ArrayList<>();
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
		clientThread.invokeLater(() ->
		{
			running = true;
			scriptDepth = 0;
			refresh();
		});
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(statusBars);
		clientThread.invokeLater(() ->
		{
			running = false;
			scriptDepth = 0;
			restore();
			returnDragHandles();
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
		frame.apply();
		if (config.hideXpButton())
		{
			states.hide(client.getWidget(InterfaceID.Orbs.XP_DROPS));
		}
		if (frame.active())
		{
			overlayManager.removeIf(overlay ->
			{
				if (!DRAG_HANDLES.contains(overlay.getName()))
				{
					return false;
				}
				setAside.add(overlay);
				return true;
			});
		}
		else
		{
			returnDragHandles();
		}
	}

	private void restore()
	{
		frame.restore();
		states.restore();
	}

	private void returnDragHandles()
	{
		setAside.forEach(overlayManager::add);
		setAside.clear();
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

	private static boolean layoutScript(int id)
	{
		return id == ScriptID.TOPLEVEL_REDRAW || id == ScriptID.TOPLEVEL_RESIZE_CUSTOMISE
			|| id == TOPLEVEL_SUBCHANGE || id == PROC_TOPLEVEL_SUBCHANGE
			|| id == TOPLEVEL_SIDE_CUSTOMIZE || id == TOPLEVEL_SIDEBUTTON_OP;
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
		if (running && scriptDepth == 0 && client.getGameState() == GameState.LOGGED_IN
			&& (frame.stale() || xpButtonShowing()))
		{
			refresh();
		}
	}

	private boolean xpButtonShowing()
	{
		if (!config.hideXpButton())
		{
			return false;
		}
		Widget button = client.getWidget(InterfaceID.Orbs.XP_DROPS);
		return button != null && !button.isSelfHidden();
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

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (running && CollapsibleClassicConfig.GROUP.equals(event.getGroup()))
		{
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
