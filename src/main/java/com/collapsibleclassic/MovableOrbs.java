/*
 * Copyright (c) 2026, SplatterFan
 * The approach (orbs re-placed every frame from their drag boxes, containers resized only temporarily, and the
 * world map and wiki treated as right-edge pieces) follows Compact Orbs (https://github.com/its-cue/compact-orbs),
 * Copyright (c) 2025, its-cue. BSD 2-Clause; see LICENSE.
 */
package com.collapsibleclassic;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.SpritePixels;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Lets each orb around the minimap, the compass and the minimap itself be dragged anywhere while RuneLite's drag
 * key (Alt) is held.
 *
 * Every orb gets an invisible RuneLite overlay the same size as the orb. RuneLite draws the yellow box, handles
 * the dragging and remembers where the box was put. When any orb has been moved, the containers around the orbs
 * are temporarily resized to cover the screen so nothing is cut off, and every orb, moved or not, is held at an
 * exact screen position. Those positions are re-applied before every frame, so the game cannot shift a piece
 * (it repositions the world map and wiki from the container's right edge, for one) for longer than it takes to
 * draw a frame. Nothing about the containers' own layout rules is changed: one revalidate puts everything back.
 */
@Slf4j
@Singleton
class MovableOrbs
{
	private static final String KEY_PREFIX = "orbPosition_";

	enum Orb
	{
		HITPOINTS(InterfaceID.Orbs.ORB_HEALTH),
		PRAYER(InterfaceID.Orbs.ORB_PRAYER),
		RUN(InterfaceID.Orbs.ORB_RUNENERGY),
		SPECIAL(InterfaceID.Orbs.ORB_SPECENERGY),
		XP(InterfaceID.Orbs.XP_DROPS),
		WORLD_MAP(InterfaceID.Orbs.ORB_WORLDMAP),
		WIKI(InterfaceID.Orbs.WIKI),
		ACTIVITY(InterfaceID.Orbs.ORB_CONTENTRECOM),
		STORE(InterfaceID.Orbs.ORB_STORE),
		// The map, its stone surround and the dead zones around it are separate pieces that move together.
		MINIMAP(InterfaceID.ToplevelPreEoc.MINIMAP,
			InterfaceID.ToplevelPreEoc.MAP_MINIMAP_GRAPHIC9,
			InterfaceID.ToplevelPreEoc.MAP_NOCLICK_0, InterfaceID.ToplevelPreEoc.MAP_NOCLICK_1,
			InterfaceID.ToplevelPreEoc.MAP_NOCLICK_2, InterfaceID.ToplevelPreEoc.MAP_NOCLICK_3,
			InterfaceID.ToplevelPreEoc.MAP_NOCLICK_4, InterfaceID.ToplevelPreEoc.MAP_NOCLICK_5),
		// The compass picture and its click area are separate pieces too.
		COMPASS(InterfaceID.ToplevelPreEoc.MAP_MINIMAP_GRAPHIC6, InterfaceID.ToplevelPreEoc.COMPASSCLICK);

		private final int component;
		private final int[] companions;

		Orb(int component, int... companions)
		{
			this.component = component;
			this.companions = companions;
		}
	}

	// Containers resized to cover the screen while any orb is moved, outermost first. The orbs sub-interface
	// inside ORBS is only resized, never moved: the game places it from its parent.
	private static final int[] CONTAINERS = {
		InterfaceID.ToplevelPreEoc.MAP_CONTAINER, InterfaceID.ToplevelPreEoc.MAP_MINIMAP, InterfaceID.ToplevelPreEoc.ORBS
	};
	private static final int ORBS_ROOT = InterfaceID.Orbs.UNIVERSE;

	// The compass's stone ring is normally part of the minimap surround. Once the two are apart, the game's
	// stand-alone ring sprite is drawn around the compass instead.
	private static final int COMPASS_RING_SIZE = 43;
	// The minimap's stone surround, whose top-left corner has the compass ring painted into it.
	private static final int SURROUND = InterfaceID.ToplevelPreEoc.MAP_MINIMAP_GRAPHIC9;
	private static final String TOGGLE = "TOGGLE";
	private static final String PRESET_PREFIX = "PRESET_";
	// The on-screen button, by default just under the minimap's bottom-right corner.
	private static final int TOGGLE_SIZE = 17, TOGGLE_DEFAULT_X = 190, TOGGLE_DEFAULT_Y = 184;

	@Inject
	private Client client;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private CollapsibleClassicConfig config;

	@Inject
	private WidgetStates states;

	private final Map<String, Handle> handles = new LinkedHashMap<>();
	// While stretched: the containers, and the exact screen position every piece inside them is held at.
	private final List<Widget> containers = new ArrayList<>();
	private final Map<Widget, Point> held = new LinkedHashMap<>();
	private final Map<Integer, Long> reported = new HashMap<>();
	private final Set<Widget> shownAtStart = Collections.newSetFromMap(new IdentityHashMap<>());
	private Widget orbsRoot;
	private boolean stretched;
	private SpritePixels erasedSurround, erasedFrom, replacedOverride;
	private boolean surroundOverridden;
	private Widget toggleButton;
	/** Where the current preset block is drawn, for its Alt-drag box; null when no preset is shown. */
	private Rectangle presetBlock;
	private Point toggleDefault;
	private Runnable refresh = () -> { };
	private boolean running;

	void start(Runnable refresh)
	{
		this.refresh = refresh;
		running = true;
	}

	void stop()
	{
		running = false;
		restore();
		restoreSurround();
		handles.values().forEach(handle ->
		{
			handle.getBounds().setSize(0, 0);
			overlayManager.remove(handle);
		});
		handles.clear();
	}

	/** True while the containers around the orbs are resized to cover the screen. */
	boolean isStretched()
	{
		return stretched;
	}

	/** Orbs can be moved at all, and default positions are not switched on. */
	private boolean moving()
	{
		return config.moveOrbs() && !config.defaultOrbPositions() && !CollapsibleClassicPlugin.presetActive(config);
	}

	/** Lays out moved orbs, the compass ring and the on-screen button. Call after {@link #restore()}. */
	void apply(boolean active)
	{
		toggleButton = null;
		presetBlock = null;
		if (!running || !active)
		{
			return;
		}

		// Measured first, while every piece is still exactly where the game put it.
		Rectangle mapContainer = bounds(InterfaceID.ToplevelPreEoc.MAP_CONTAINER);
		toggleDefault = mapContainer == null ? null
			: new Point(mapContainer.x + TOGGLE_DEFAULT_X, mapContainer.y + TOGGLE_DEFAULT_Y);
		Rectangle surroundBounds = bounds(SURROUND);
		// Measured even when hidden: Hide compass still needs to know where to erase its border from the surround.
		Widget compassWidget = client.getWidget(Orb.COMPASS.component);
		Rectangle compassBounds = compassWidget == null ? null : WidgetStates.bounds(compassWidget);
		Rectangle mapBounds = bounds(Orb.MINIMAP.component);
		Dimension canvas = client.getRealDimensions();

		Map<Orb, Point> targets = new LinkedHashMap<>();
		if (moving())
		{
			for (Orb orb : Orb.values())
			{
				Widget widget = visible(orb.component);
				Point saved = saved(orb);
				if (widget == null || saved == null)
				{
					continue;
				}
				// The whole group (the minimap with its surround, say) must stay on the screen.
				Rectangle group = groupExtent(orb, widget);
				targets.put(orb, new Point(
					Math.max(-group.x, Math.min(saved.x, canvas.width - group.x - group.width)),
					Math.max(-group.y, Math.min(saved.y, canvas.height - group.y - group.height))));
			}
		}
		else if (CollapsibleClassicPlugin.presetActive(config) && mapContainer != null)
		{
			// A compact block against the right edge of the minimap area, as in Compact Orbs.
			// With Move orbs on, the whole block can be dragged with Alt; each preset remembers its own spot.
			OrbLayout layout = config.orbLayout();
			Point savedBlock = config.moveOrbs() ? savedPreset(layout) : null;
			int blockX = savedBlock == null ? mapContainer.x + mapContainer.width - layout.getWidth()
				: Math.max(0, Math.min(savedBlock.x, canvas.width - layout.getWidth()));
			int blockY = savedBlock == null ? mapContainer.y
				: Math.max(0, Math.min(savedBlock.y, canvas.height - layout.getHeight()));
			presetBlock = new Rectangle(blockX, blockY, layout.getWidth(), layout.getHeight());
			layout.offsets().forEach((orb, offset) ->
			{
				Widget widget = visible(orb.component);
				if (widget == null)
				{
					return;
				}
				int inset = orb == Orb.COMPASS ? (COMPASS_RING_SIZE - widget.getWidth()) / 2 : 0;
				targets.put(orb, new Point(blockX + offset.x + inset, blockY + offset.y + inset));
			});
		}

		Widget compass = visible(Orb.COMPASS.component);
		Point compassAt = compassBounds == null ? null : compassBounds.getLocation();
		if (!targets.isEmpty())
		{
			// An undragged compass stays inside its ring, so it travels with a dragged minimap.
			if (compass != null && mapBounds != null && targets.containsKey(Orb.MINIMAP) && !targets.containsKey(Orb.COMPASS))
			{
				Point to = targets.get(Orb.MINIMAP);
				targets.put(Orb.COMPASS, new Point(compassAt.x + to.x - mapBounds.x, compassAt.y + to.y - mapBounds.y));
			}
			stretch(targets);
			if (targets.containsKey(Orb.COMPASS))
			{
				compassAt = targets.get(Orb.COMPASS);
			}
		}

		// The ring is needed whenever the compass itself has been dragged, wherever the minimap is, and whenever
		// the minimap (whose surround normally provides the ring) is hidden.
		boolean minimapHidden = CollapsibleClassicPlugin.minimapHidden(config);
		boolean compassHidden = config.hideCompass();
		boolean compassDetached = minimapHidden || targets.containsKey(Orb.COMPASS)
			&& (saved(Orb.COMPASS) != null || CollapsibleClassicPlugin.presetActive(config));
		if (!compassHidden && compass != null && compassAt != null && compassDetached && compass.getParent() != null)
		{
			int inset = (COMPASS_RING_SIZE - compass.getWidth()) / 2;
			Point ringAt = new Point(compassAt.x - inset, compassAt.y - inset);
			Widget ring = states.addChild(compass.getParent(), WidgetType.GRAPHIC);
			ring.setSpriteId(SpriteID.COMPASS_OUTLINE);
			ring.setOriginalWidth(COMPASS_RING_SIZE);
			ring.setOriginalHeight(COMPASS_RING_SIZE);
			if (stretched)
			{
				held.put(ring, ringAt);
				place(ring, ringAt);
			}
			else
			{
				Point parent = WidgetStates.location(compass.getParent());
				ring.setOriginalX(ringAt.x - parent.x);
				ring.setOriginalY(ringAt.y - parent.y);
				ring.revalidate();
			}
		}

		if ((compassDetached || compassHidden) && !minimapHidden && surroundBounds != null && compassBounds != null && mapBounds != null)
		{
			eraseCompassFromSurround(surroundBounds, compassBounds, mapBounds);
		}
		else
		{
			restoreSurround();
		}

		addToggleButton();
	}

	/**
	 * Resizes the containers to cover the screen and records where every piece inside them belongs: its own place
	 * for pieces nobody moved, its dragged place (and its companions with it) for the rest.
	 */
	private void stretch(Map<Orb, Point> targets)
	{
		for (int id : CONTAINERS)
		{
			Widget container = client.getWidget(id);
			if (container == null)
			{
				held.clear();
				containers.clear();
				return;
			}
			containers.add(container);
		}
		orbsRoot = client.getWidget(ORBS_ROOT);

		List<Widget> pieces = new ArrayList<>();
		for (Widget container : containers)
		{
			addPieces(container, pieces);
		}
		if (orbsRoot != null)
		{
			addPieces(orbsRoot, pieces);
		}
		for (Widget piece : pieces)
		{
			held.put(piece, WidgetStates.location(piece));
			if (!piece.isHidden())
			{
				shownAtStart.add(piece);
			}
		}

		targets.forEach((orb, target) ->
		{
			Widget widget = visible(orb.component);
			Point from = WidgetStates.location(widget);
			held.put(widget, target);
			for (int id : orb.companions)
			{
				Widget companion = client.getWidget(id);
				if (companion != null)
				{
					Point at = WidgetStates.location(companion);
					held.put(companion, new Point(target.x + at.x - from.x, target.y + at.y - from.y));
				}
			}
		});
		stretched = true;

		// The containers go to the screen's top-left corner and are sized to the whole screen. Every piece is then
		// given an ordinary left/top position at its exact target. The world map and wiki are normally laid out
		// from their container's right edge; converted like this, no later layout pass can push them anywhere.
		// WidgetStates records each original position and rule, and puts them back on restore.
		Dimension canvas = client.getRealDimensions();
		for (Widget container : containers)
		{
			Point parent = WidgetStates.location(container.getParent());
			states.position(container, -parent.x, -parent.y);
			states.resize(container, canvas.width, canvas.height);
			container.setNoClickThrough(false);
			container.revalidate();
		}
		if (orbsRoot != null)
		{
			orbsRoot.revalidate();
			orbsRoot.setWidth(canvas.width);
			orbsRoot.setHeight(canvas.height);
		}
		held.forEach(this::place);
	}

	private void place(Widget piece, Point target)
	{
		Point parent = WidgetStates.location(piece.getParent());
		states.position(piece, target.x - parent.x, target.y - parent.y);
		piece.revalidate();
	}

	/** The pieces directly inside a container, leaving out the containers that are resized themselves. */
	private void addPieces(Widget container, List<Widget> pieces)
	{
		for (Widget[] group : new Widget[][]{container.getStaticChildren(), container.getDynamicChildren()})
		{
			if (group == null)
			{
				continue;
			}
			for (Widget child : group)
			{
				if (child != null && !containers.contains(child))
				{
					pieces.add(child);
				}
			}
		}
	}

	/**
	 * Re-applies the stretched layout. Cheap enough to run before every frame, which is what keeps the game from
	 * moving a piece for longer than one frame. Setting a size directly only lasts until the game next lays the
	 * container out, so the sizes are re-applied here too.
	 */
	void enforce()
	{
		if (!stretched)
		{
			fixOrbsRootSize();
			return;
		}
		Dimension canvas = client.getRealDimensions();
		// The orbs sub-interface is sized directly, which only lasts until the game next lays it out.
		if (orbsRoot != null && (orbsRoot.getWidth() != canvas.width || orbsRoot.getHeight() != canvas.height))
		{
			orbsRoot.setWidth(canvas.width);
			orbsRoot.setHeight(canvas.height);
		}
		held.forEach((piece, target) ->
		{
			if (!WidgetStates.location(piece).equals(target))
			{
				place(piece, target);
			}
			if (piece.isHidden() && shownAtStart.contains(piece))
			{
				reportHidden(piece);
			}
		});
	}

	/**
	 * Writes one line to the log, at most every ten seconds per piece, when the game hides a piece this class is
	 * holding. Hiding is decided by the game's own scripts, so this is what to look at if an orb vanishes.
	 */
	private void reportHidden(Widget piece)
	{
		long now = System.currentTimeMillis();
		Long last = reported.get(piece.getId());
		if (last != null && now - last < 10_000)
		{
			return;
		}
		reported.put(piece.getId(), now);
		Widget parent = piece.getParent();
		log.info("[Collapsible Classic] piece {} hidden: self={} parentHidden={} at={} size={}x{} xMode={} originalX={}",
			piece.getId(), piece.isSelfHidden(), parent != null && parent.isHidden(), WidgetStates.location(piece),
			piece.getWidth(), piece.getHeight(), piece.getXPositionMode(), piece.getOriginalX());
	}

	/** A snapshot of everything relevant to where the orbs are, for the ::ccdebug command. */
	List<String> debugReport()
	{
		List<String> lines = new ArrayList<>();
		Dimension real = client.getRealDimensions();
		lines.add("canvas=" + client.getCanvasWidth() + "x" + client.getCanvasHeight() + " real=" + real.width + "x" + real.height
			+ " stretchedMode=" + client.isStretchedEnabled() + " resized=" + client.isResized()
			+ " toplevel=" + client.getTopLevelInterfaceId());
		lines.add("layout=" + config.orbLayout().name() + " hideCompass=" + config.hideCompass());
		lines.add("moveOrbs=" + config.moveOrbs() + " defaults=" + config.defaultOrbPositions() + " moving=" + moving()
			+ " stretched=" + stretched + " held=" + held.size() + " hideWorldMap=" + config.hideWorldMapOrb()
			+ " hideWiki=" + config.hideWikiBanner() + " hideMinimap=" + config.hideMinimap());
		for (Orb orb : Orb.values())
		{
			Point saved = saved(orb);
			if (saved != null)
			{
				lines.add("saved " + orb.name() + "=" + saved.x + "," + saved.y);
			}
		}
		Object[][] widgets = {
			{"GAMEFRAME", InterfaceID.ToplevelPreEoc.GAMEFRAME}, {"MAP_CONTAINER", InterfaceID.ToplevelPreEoc.MAP_CONTAINER},
			{"MAP_MINIMAP", InterfaceID.ToplevelPreEoc.MAP_MINIMAP}, {"ORBS", InterfaceID.ToplevelPreEoc.ORBS},
			{"UNIVERSE", InterfaceID.Orbs.UNIVERSE}, {"WORLD_MAP", InterfaceID.Orbs.ORB_WORLDMAP},
			{"WORLD_MAP_BACKING", InterfaceID.Orbs.WORLDMAP_BACKING}, {"WORLD_MAP_ICON", InterfaceID.Orbs.WORLDMAP},
			{"WIKI", InterfaceID.Orbs.WIKI}, {"HITPOINTS", InterfaceID.Orbs.ORB_HEALTH}, {"XP", InterfaceID.Orbs.XP_DROPS},
			{"COMPASS", InterfaceID.ToplevelPreEoc.MAP_MINIMAP_GRAPHIC6}, {"COMPASSCLICK", InterfaceID.ToplevelPreEoc.COMPASSCLICK},
			{"MINIMAP", InterfaceID.ToplevelPreEoc.MINIMAP}, {"SURROUND", SURROUND}
		};
		handles.forEach((key, handle) -> lines.add("handle " + key + ": bounds=" + handle.getBounds()
			+ " preferredLocation=" + handle.getPreferredLocation() + " preferredPosition=" + handle.getPreferredPosition()));
		for (Object[] entry : widgets)
		{
			Widget widget = client.getWidget((Integer) entry[1]);
			if (widget == null)
			{
				lines.add(entry[0] + ": missing");
				continue;
			}
			net.runelite.api.Point drawn = widget.getCanvasLocation();
			Point target = held.get(widget);
			lines.add(entry[0] + ": hidden=" + widget.isHidden() + "/self=" + widget.isSelfHidden()
				+ " rel=" + widget.getRelativeX() + "," + widget.getRelativeY()
				+ " orig=" + widget.getOriginalX() + "," + widget.getOriginalY()
				+ " mode=" + widget.getXPositionMode() + "," + widget.getYPositionMode()
				+ " size=" + widget.getWidth() + "x" + widget.getHeight()
				+ " sizeMode=" + widget.getWidthMode() + "," + widget.getHeightMode()
				+ " computed=" + WidgetStates.location(widget).x + "," + WidgetStates.location(widget).y
				+ " drawn=" + (drawn == null ? "null" : drawn.getX() + "," + drawn.getY())
				+ " opacity=" + widget.getOpacity()
				+ " target=" + (target == null ? "-" : target.x + "," + target.y));
		}
		return lines;
	}

	/** Forgets the stretched layout; WidgetStates puts each piece back. Call {@link #afterRestore()} once it has. */
	void restore()
	{
		held.clear();
		shownAtStart.clear();
		containers.clear();
		stretched = false;
	}

	/** The orbs sub-interface was sized directly, so it is laid out again once its parent is back. */
	void afterRestore()
	{
		if (orbsRoot != null)
		{
			orbsRoot.revalidate();
			Widget[] children = orbsRoot.getStaticChildren();
			if (children != null)
			{
				for (Widget child : children)
				{
					if (child != null)
					{
						child.revalidate();
					}
				}
			}
			orbsRoot = null;
		}
		fixOrbsRootSize();
	}

	/**
	 * The orbs sub-interface fills the orbs container by its own rules, but a size set directly on it (by us while
	 * stretched, or by another plugin) survives every later layout pass: revalidating does not undo it. Left
	 * canvas-wide, the world map and wiki, which sit against its right edge, end up off the screen. So whenever
	 * nothing is stretched, its size is worked out from its rules and put back, and its pieces laid out again.
	 */
	void fixOrbsRootSize()
	{
		if (stretched)
		{
			return;
		}
		Widget root = client.getWidget(ORBS_ROOT);
		Widget parent = client.getWidget(InterfaceID.ToplevelPreEoc.ORBS);
		if (root == null || parent == null)
		{
			return;
		}
		int width = ruleSize(root.getWidthMode(), root.getOriginalWidth(), parent.getWidth(), root.getWidth());
		int height = ruleSize(root.getHeightMode(), root.getOriginalHeight(), parent.getHeight(), root.getHeight());
		if (root.getWidth() == width && root.getHeight() == height)
		{
			return;
		}
		root.setWidth(width);
		root.setHeight(height);
		for (Widget[] group : new Widget[][]{root.getStaticChildren(), root.getDynamicChildren()})
		{
			if (group == null)
			{
				continue;
			}
			for (Widget child : group)
			{
				if (child != null)
				{
					child.revalidate();
				}
			}
		}
	}

	/** A widget's size from its size mode: absolute, parent minus a value, or a fraction of the parent. */
	private static int ruleSize(int mode, int original, int parentSize, int current)
	{
		switch (mode)
		{
			case 0:
				return original;
			case 1:
				return parentSize - original;
			case 2:
				return parentSize * original >> 14;
			default:
				return current;
		}
	}

	/**
	 * A small button on the game screen that switches Default orb positions on and off. It can be dragged with
	 * Alt like the orbs. It is a child of the game frame, which already covers the whole canvas.
	 */
	private void addToggleButton()
	{
		Widget frame = client.getWidget(InterfaceID.ToplevelPreEoc.GAMEFRAME);
		if (!config.moveOrbs() || !config.onScreenToggle() || frame == null || toggleDefault == null)
		{
			return;
		}
		Dimension canvas = client.getRealDimensions();
		Point saved = configManager.getConfiguration(CollapsibleClassicConfig.GROUP, KEY_PREFIX + TOGGLE, Point.class);
		Point at = saved != null ? saved : toggleDefault;
		at = new Point(Math.max(0, Math.min(at.x, canvas.width - TOGGLE_SIZE)),
			Math.max(0, Math.min(at.y, canvas.height - TOGGLE_SIZE)));
		Point origin = WidgetStates.location(frame);
		boolean defaults = config.defaultOrbPositions();

		Widget button = states.addChild(frame, WidgetType.GRAPHIC);
		button.setSpriteId(defaults ? SpriteID.GroundItemsVisibility._1 : SpriteID.GroundItemsVisibility._0);
		button.setOriginalX(at.x - origin.x);
		button.setOriginalY(at.y - origin.y);
		button.setOriginalWidth(TOGGLE_SIZE);
		button.setOriginalHeight(TOGGLE_SIZE);
		button.setAction(0, defaults ? "Use your orb layout" : "Use default orb positions");
		button.setHasListener(true);
		button.setOnOpListener((JavaScriptCallback) event ->
			configManager.setConfiguration(CollapsibleClassicConfig.GROUP, "defaultOrbPositions", !config.defaultOrbPositions()));
		button.revalidate();
		toggleButton = button;
	}

	/** Where an orb has been dragged to, or null if it has not been. */
	/**
	 * Forgets every dragged position: each orb, the compass, the minimap, every preset block and the on-screen
	 * button go back to their normal places. RuneLite's own memory of each drag box (including any attachment to
	 * the side panel) is cleared too, so nothing can drag a piece back somewhere it can't be reached.
	 */
	void resetAllPositions()
	{
		List<String> keys = new ArrayList<>();
		for (Orb orb : Orb.values())
		{
			keys.add(orb.name());
		}
		for (OrbLayout layout : OrbLayout.values())
		{
			keys.add(PRESET_PREFIX + layout.name());
		}
		keys.add(TOGGLE);
		for (String key : keys)
		{
			configManager.unsetConfiguration(CollapsibleClassicConfig.GROUP, KEY_PREFIX + key);
		}
		handles.values().forEach(handle ->
		{
			handle.lastApplied = null;
			overlayManager.resetOverlay(handle);
		});
		refresh.run();
	}

	private Point savedPreset(OrbLayout layout)
	{
		return configManager.getConfiguration(CollapsibleClassicConfig.GROUP, KEY_PREFIX + PRESET_PREFIX + layout.name(), Point.class);
	}

	private Point saved(Orb orb)
	{
		return configManager.getConfiguration(CollapsibleClassicConfig.GROUP, KEY_PREFIX + orb.name(), Point.class);
	}

	/** The area an orb and the pieces that move with it cover, relative to the orb's own top-left corner. */
	private Rectangle groupExtent(Orb orb, Widget widget)
	{
		Point origin = WidgetStates.location(widget);
		Rectangle extent = new Rectangle(0, 0, widget.getWidth(), widget.getHeight());
		for (int id : orb.companions)
		{
			Widget companion = client.getWidget(id);
			if (companion != null)
			{
				Point at = WidgetStates.location(companion);
				extent.add(new Rectangle(at.x - origin.x, at.y - origin.y, companion.getWidth(), companion.getHeight()));
			}
		}
		return extent;
	}

	private Rectangle bounds(int component)
	{
		Widget widget = visible(component);
		return widget == null ? null : WidgetStates.bounds(widget);
	}

	private void eraseCompassFromSurround(Rectangle surround, Rectangle compass, Rectangle map)
	{
		Widget widget = client.getWidget(SURROUND);
		if (widget == null)
		{
			return;
		}
		SpritePixels base = client.getSpriteOverrides().get(widget.getSpriteId());
		if (base == null)
		{
			SpritePixels[] cached = client.getSprites(client.getIndexSprites(), widget.getSpriteId(), 0);
			base = cached == null || cached.length == 0 ? null : cached[0];
		}
		if (base == null)
		{
			return;
		}
		if (base != erasedFrom || erasedSurround == null)
		{
			erasedSurround = erase(base, surround, compass, map);
			erasedFrom = base;
		}
		if (!surroundOverridden)
		{
			replacedOverride = client.getWidgetSpriteOverrides().get(SURROUND);
			surroundOverridden = true;
		}
		if (client.getWidgetSpriteOverrides().get(SURROUND) != erasedSurround)
		{
			client.getWidgetSpriteOverrides().put(SURROUND, erasedSurround);
			client.getWidgetSpriteCache().reset();
		}
	}

	private SpritePixels erase(SpritePixels base, Rectangle surround, Rectangle compass, Rectangle map)
	{
		int width = Math.max(base.getMaxWidth(), base.getWidth());
		int height = Math.max(base.getMaxHeight(), base.getHeight());
		int[] pixels = new int[width * height];
		int[] source = base.getPixels();
		for (int y = 0; y < base.getHeight(); y++)
		{
			for (int x = 0; x < base.getWidth(); x++)
			{
				int tx = x + base.getOffsetX(), ty = y + base.getOffsetY();
				if (tx >= 0 && ty >= 0 && tx < width && ty < height)
				{
					pixels[ty * width + tx] = source[y * base.getWidth() + x];
				}
			}
		}

		// Positions in the surround's own pixels. The map's frame is the ring of stone between the map's edge and
		// the surround's right and bottom edges, which it touches.
		double compassX = compass.getCenterX() - surround.x, compassY = compass.getCenterY() - surround.y;
		double mapX = map.getCenterX() - surround.x, mapY = map.getCenterY() - surround.y;
		double frameRadius = Math.min(width - mapX, height - mapY) + 0.5;
		double ringRadius = COMPASS_RING_SIZE / 2.0 + 1;
		int[] original = pixels.clone();
		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
			{
				double px = x + 0.5, py = y + 0.5;
				if (Math.hypot(px - compassX, py - compassY) > ringRadius)
				{
					continue;
				}
				if (Math.hypot(px - mapX, py - mapY) > frameRadius)
				{
					// Outside the map's frame: the ring's own bump, which goes.
					pixels[y * width + x] = 0;
				}
				else
				{
					// Where the ring overlapped the frame, the frame is redrawn from its mirror image on the
					// other side of the map, which has no compass.
					int mirrorX = (int) Math.floor(2 * mapX - px);
					pixels[y * width + x] = mirrorX >= 0 && mirrorX < width ? original[y * width + mirrorX] : 0;
				}
			}
		}
		return client.createSpritePixels(pixels, width, height);
	}

	private void restoreSurround()
	{
		if (!surroundOverridden)
		{
			return;
		}
		if (replacedOverride != null)
		{
			client.getWidgetSpriteOverrides().put(SURROUND, replacedOverride);
		}
		else
		{
			client.getWidgetSpriteOverrides().remove(SURROUND);
		}
		client.getWidgetSpriteCache().reset();
		surroundOverridden = false;
		replacedOverride = null;
	}

	/** Keeps one RuneLite drag box over each visible orb. Call after {@link #apply(boolean)}. */
	void syncHandles(boolean active)
	{
		if (!running)
		{
			return;
		}
		for (Orb orb : Orb.values())
		{
			// No drag boxes unless orbs can be moved; RuneLite keeps the boxes' places for later.
			sync(orb.name(), area(active && moving() ? visible(orb.component) : null));
		}
		for (OrbLayout layout : OrbLayout.values())
		{
			if (layout != OrbLayout.CUSTOM)
			{
				sync(PRESET_PREFIX + layout.name(), active && layout == config.orbLayout() ? presetArea() : null);
			}
		}
		sync(TOGGLE, area(active ? toggleButton : null));
	}

	private static Rectangle area(Widget widget)
	{
		return widget == null ? null : WidgetStates.bounds(widget);
	}

	/** The preset block's box, while a preset is shown and Move orbs allows dragging it. */
	private Rectangle presetArea()
	{
		return presetBlock != null && config.moveOrbs() && CollapsibleClassicPlugin.presetActive(config)
			? new Rectangle(presetBlock) : null;
	}

	private void sync(String key, Rectangle bounds)
	{
		Handle handle = handles.get(key);
		if (bounds == null)
		{
			if (handle != null)
			{
				handle.getBounds().setSize(0, 0);
			}
			return;
		}
		if (handle == null)
		{
			handle = new Handle(key);
			handle.getBounds().setBounds(bounds);
			handles.put(key, handle);
			overlayManager.add(handle);
		}
		handle.getBounds().setSize(bounds.getSize());
		if (handle.getPreferredLocation() == null && handle.getPreferredPosition() == null)
		{
			handle.getBounds().setLocation(bounds.getLocation());
		}
	}

	private Widget visible(int component)
	{
		Widget widget = client.getWidget(component);
		return widget == null || widget.isHidden() ? null : widget;
	}

	private final class Handle extends Overlay
	{
		private final String key;
		private Point lastApplied;
		private boolean reset;

		private Handle(String key)
		{
			this.key = key;
			setPosition(OverlayPosition.DYNAMIC);
			setLayer(OverlayLayer.UNDER_WIDGETS);
			setPriority(Overlay.PRIORITY_HIGHEST);
			setMovable(true);
			setSnappable(true);
		}

		@Override
		public String getName()
		{
			return "COLLAPSIBLE_CLASSIC_ORB_" + key;
		}

		// RuneLite calls this when the box is reset with Alt right-click; the orb then goes back to its own place.
		@Override
		public void revalidate()
		{
			reset = lastApplied != null && getPreferredLocation() == null && getPreferredPosition() == null;
		}

		@Override
		public Dimension render(Graphics2D graphics)
		{
			if (!running)
			{
				return null;
			}
			boolean placed = getPreferredLocation() != null || getPreferredPosition() != null;
			// Handled before anything else, so an orb that has gone missing can still be put back.
			if (reset && !placed)
			{
				lastApplied = null;
				configManager.unsetConfiguration(CollapsibleClassicConfig.GROUP, KEY_PREFIX + key);
				refresh.run();
			}
			reset = false;
			Rectangle actual = area();
			if (actual == null)
			{
				return null;
			}

			if (placed)
			{
				// RuneLite has positioned the box, by a drag or by following what it is attached to.
				Point canvas = getBounds().getLocation();
				if (!canvas.equals(lastApplied))
				{
					lastApplied = new Point(canvas);
					configManager.setConfiguration(CollapsibleClassicConfig.GROUP, KEY_PREFIX + key, canvas);
					refresh.run();
				}
			}
			else
			{
				getBounds().setLocation(actual.getLocation());
			}
			return actual.getSize();
		}

		private Rectangle area()
		{
			if (TOGGLE.equals(key))
			{
				return MovableOrbs.area(toggleButton);
			}
			if (key.startsWith(PRESET_PREFIX))
			{
				return key.equals(PRESET_PREFIX + config.orbLayout().name()) ? presetArea() : null;
			}
			if (!moving())
			{
				return null;
			}
			return MovableOrbs.area(visible(Orb.valueOf(key).component));
		}
	}
}
