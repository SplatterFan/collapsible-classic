/*
 * Copyright (c) 2026, SplatterFan
 * BSD 2-Clause; see LICENSE.
 */
package com.collapsibleclassic;

import java.awt.Point;
import java.awt.Rectangle;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.config.ConfigManager;

/**
 * Draws the Resizable Classic side panel frame (tapered stone rows, pillars and backdrop) around the Resizable
 * Modern side panel. Modern keeps its own behaviour of closing the panel when the active tab is clicked; when it
 * is closed, the two stone rows stack in the bottom-right corner.
 *
 * Every measurement and sprite below was read from the game's own Resizable Classic interface
 * (ToplevelOsrsStretch.SIDE_MENU), so the frame also follows resource packs.
 */
@Singleton
class ClassicFrame
{
	private static final int FRAME_WIDTH = 241, ROW_HEIGHT = 37, PANEL_HEIGHT = 261;
	private static final int OPEN_HEIGHT = ROW_HEIGHT * 2 + PANEL_HEIGHT;
	private static final int TOP_ROW_SPRITE = 1173, BOTTOM_ROW_SPRITE = 1174;
	private static final int LEFT_PILLAR_SPRITE = 1175, RIGHT_PILLAR_SPRITE = 1176;
	private static final int BACKDROP_SPRITE = 897;
	private static final int PILLAR_WIDTH = 26, LEFT_PILLAR_X = 2, RIGHT_PILLAR_X = 212;
	// The backdrop is inset from the frame and runs 10px under each stone row.
	private static final int BACKDROP_X = 20, BACKDROP_WIDTH = 200, BACKDROP_OVERLAP = 10;

	private static final int SLOT_WIDTH = 33, CORNER_WIDTH = 38, CORNER_ICON_INSET = 4, STONE_HEIGHT = 36;
	private static final int SELECTED_TOP_LEFT = 1026, SELECTED_TOP_RIGHT = 1027;
	private static final int SELECTED_BOTTOM_LEFT = 1028, SELECTED_BOTTOM_RIGHT = 1029, SELECTED_MIDDLE = 1030;

	// Modern's panel is the 190x261 contents plus a 7px border on every side.
	private static final int CONTENTS_X = 25, CONTENTS_Y = 37, CONTENTS_WIDTH = 190, MODERN_BORDER = 7;
	// RuneLite's Status Bars placement in Resizable Classic: 20px bars flush against each side of the contents.
	private static final int BAR_WIDTH = 20, BAR_HEIGHT = 252, BAR_INSET_Y = 4;

	private static final int LOGOUT_ICON_SPRITE = 907, LOGOUT_PANEL = 10, LOGOUT_SLOT = 3;
	// Read from Modern's own logout button (ToplevelPreEoc.STONE10): [914, event_op, 1131, 10].
	private static final int SIDEBUTTON_OP_SCRIPT = 914, EVENT_OP = -2147483644, LOGOUT_OP_ARGUMENT = 1131;
	private static final int[] NO_COMPONENTS = {};
	private static final int[] MODERN_LOGOUT = {InterfaceID.ToplevelPreEoc.STONE10, InterfaceID.ToplevelPreEoc.ICON10};

	/** Modern's stones and their icons, in the game's own order. Index 10 is logout, which Modern keeps elsewhere. */
	private static final int[][] STONES = {
		{InterfaceID.ToplevelPreEoc.STONE0, InterfaceID.ToplevelPreEoc.ICON0},
		{InterfaceID.ToplevelPreEoc.STONE1, InterfaceID.ToplevelPreEoc.ICON1},
		{InterfaceID.ToplevelPreEoc.STONE2, InterfaceID.ToplevelPreEoc.ICON2},
		{InterfaceID.ToplevelPreEoc.STONE3, InterfaceID.ToplevelPreEoc.ICON3},
		{InterfaceID.ToplevelPreEoc.STONE4, InterfaceID.ToplevelPreEoc.ICON4},
		{InterfaceID.ToplevelPreEoc.STONE5, InterfaceID.ToplevelPreEoc.ICON5},
		{InterfaceID.ToplevelPreEoc.STONE6, InterfaceID.ToplevelPreEoc.ICON6},
		{InterfaceID.ToplevelPreEoc.STONE7, InterfaceID.ToplevelPreEoc.ICON7},
		{InterfaceID.ToplevelPreEoc.STONE8, InterfaceID.ToplevelPreEoc.ICON8},
		{InterfaceID.ToplevelPreEoc.STONE9, InterfaceID.ToplevelPreEoc.ICON9},
		{InterfaceID.ToplevelPreEoc.STONE10, InterfaceID.ToplevelPreEoc.ICON10},
		{InterfaceID.ToplevelPreEoc.STONE11, InterfaceID.ToplevelPreEoc.ICON11},
		{InterfaceID.ToplevelPreEoc.STONE12, InterfaceID.ToplevelPreEoc.ICON12},
		{InterfaceID.ToplevelPreEoc.STONE13, InterfaceID.ToplevelPreEoc.ICON13}
	};

	@Inject
	private Client client;

	@Inject
	private CollapsibleClassicConfig config;

	@Inject
	private WidgetStates states;

	@Inject
	private ConfigManager configManager;

	private boolean applied, framed, lastOpen;
	private Rectangle lastGame;
	private Widget shownWhileClosed;
	private Rectangle leftBar, rightBar;
	// Where the whole panel would sit when open, for the Alt-drag box; null while the frame is absent.
	private Rectangle panelArea;
	// Set by the plugin: Hide Modern logout X is on and Compact Orbs is not handling the X.
	private boolean hideLogoutX = true;
	// Modern's own highlight, learned the first time it is seen, so it can be put back when the frame is removed.
	private int modernSelected = -1;
	private boolean modernTiling;

	/** The frame only exists in Resizable Modern. */
	boolean active()
	{
		return client.getGameState() == GameState.LOGGED_IN && client.isResized()
			&& client.getTopLevelInterfaceId() == InterfaceID.TOPLEVEL_PRE_EOC;
	}

	/** Where a status bar sits over each pillar, or null while the frame is closed or absent. */
	Rectangle leftBar()
	{
		return leftBar;
	}

	Rectangle rightBar()
	{
		return rightBar;
	}

	/** The open panel's rectangle (the same whether open or closed), or null while the frame is absent. */
	Rectangle panelArea()
	{
		return panelArea;
	}

	/** Whether the side panel was open when the frame was last drawn. */
	boolean isOpen()
	{
		return lastOpen;
	}

	void setHideLogoutX(boolean hide)
	{
		hideLogoutX = hide;
	}

	/** The bottom stone row, which sits in the same place open or closed; null while the frame is absent. */
	Rectangle bottomRowArea()
	{
		return panelArea == null ? null
			: new Rectangle(panelArea.x, panelArea.y + OPEN_HEIGHT - ROW_HEIGHT, FRAME_WIDTH, ROW_HEIGHT);
	}

	/** Where the open panel's top-left corner goes for a bottom stone row at this point. */
	static Point panelOriginForBottomRow(Point row)
	{
		return new Point(row.x, row.y - OPEN_HEIGHT + ROW_HEIGHT);
	}

	/** Where the open panel's top-left corner was dragged to, or null for its normal place. */
	private Point savedPosition()
	{
		return configManager.getConfiguration(CollapsibleClassicConfig.GROUP, SidePanelMover.KEY, Point.class);
	}

	void apply()
	{
		applied = false;
		framed = false;
		if (!active())
		{
			return;
		}

		Widget container = client.getWidget(InterfaceID.ToplevelPreEoc.SIDE_CONTAINER);
		Widget topStones = client.getWidget(InterfaceID.ToplevelPreEoc.SIDE_MOVABLE);
		Widget bottomStones = client.getWidget(InterfaceID.ToplevelPreEoc.SIDE_STATIC);
		Widget topRow = client.getWidget(InterfaceID.ToplevelPreEoc.SIDE_MOVABLE_BACKGROUND);
		Widget bottomRow = client.getWidget(InterfaceID.ToplevelPreEoc.SIDE_STATIC_BACKGROUND);
		if (container == null || container.getParent() == null || topStones == null || bottomStones == null
			|| topRow == null || bottomRow == null || topRow.isHidden() || bottomRow.isHidden())
		{
			return;
		}

		Rectangle game = WidgetStates.bounds(container.getParent());
		boolean open = open(container);
		boolean modal = modal();
		int selected = selected(open, modal);
		lastOpen = open;
		lastGame = game;

		// Normally against the bottom-right corner; moved, the open panel's top-left goes where it was dragged,
		// kept on the screen. A closed panel keeps the open panel's bottom edge.
		Point saved = savedPosition();
		int x = game.x + game.width - FRAME_WIDTH;
		int openTop = game.y + game.height - OPEN_HEIGHT;
		if (saved != null)
		{
			x = Math.max(game.x, Math.min(saved.x, x));
			openTop = Math.max(game.y, Math.min(saved.y, openTop));
		}
		panelArea = new Rectangle(x, openTop, FRAME_WIDTH, OPEN_HEIGHT);
		int bottomY = openTop + OPEN_HEIGHT - ROW_HEIGHT;
		int topY = bottomY - ROW_HEIGHT - (open ? PANEL_HEIGHT : 0);

		// The bottom row is drawn first, so the backdrop and pillars go into it to sit under everything else.
		Widget lower = states.asLayer(bottomRow);
		Widget upper = states.asLayer(topRow);
		if (open || config.fillCollapsedGaps())
		{
			int backdropY = topY + ROW_HEIGHT - BACKDROP_OVERLAP;
			states.addSprite(lower, BACKDROP_SPRITE, true,
				new Rectangle(x + BACKDROP_X, backdropY, BACKDROP_WIDTH, bottomY + BACKDROP_OVERLAP - backdropY));
		}
		if (open)
		{
			states.addSprite(lower, LEFT_PILLAR_SPRITE, false,
				new Rectangle(x + LEFT_PILLAR_X, topY + ROW_HEIGHT, PILLAR_WIDTH, PANEL_HEIGHT));
			states.addSprite(lower, RIGHT_PILLAR_SPRITE, false,
				new Rectangle(x + RIGHT_PILLAR_X, topY + ROW_HEIGHT, PILLAR_WIDTH, PANEL_HEIGHT));
		}
		states.addSprite(lower, BOTTOM_ROW_SPRITE, false, new Rectangle(x, bottomY, FRAME_WIDTH, ROW_HEIGHT));
		states.addSprite(upper, TOP_ROW_SPRITE, false, new Rectangle(x, topY, FRAME_WIDTH, ROW_HEIGHT));

		// Moved away from the corner, the stone rows' own parents must not cut them off.
		if (saved != null)
		{
			states.expandAncestors(topStones);
			states.expandAncestors(bottomStones);
		}
		// Classic's stone containers are the full 241x37 row, with the stones at the top edge.
		states.resize(topStones, FRAME_WIDTH, ROW_HEIGHT);
		states.resize(bottomStones, FRAME_WIDTH, ROW_HEIGHT);
		states.place(topStones, x, topY);
		states.place(bottomStones, x, bottomY);

		for (int index = 0; index < STONES.length; index++)
		{
			if (index == LOGOUT_PANEL)
			{
				continue;
			}
			Widget stone = client.getWidget(STONES[index][0]);
			Widget icon = client.getWidget(STONES[index][1]);
			if (stone == null || stone.getParent() != (index < 7 ? topStones : bottomStones))
			{
				continue;
			}
			int slot = slot(index);
			boolean corner = slot == 0 || slot == 6;
			int iconY = icon == null ? 0 : icon.getRelativeY() - stone.getRelativeY();

			states.capture(stone);
			stone.setOriginalWidth(corner ? CORNER_WIDTH : SLOT_WIDTH);
			stone.setWidthMode(WidgetSizeMode.ABSOLUTE);
			// The highlight follows the open panel itself, not whatever sprite the stone last carried, so tabs
			// opened by hotkey are highlighted and nothing stale is ever restored.
			int current = stone.getSpriteId();
			if (current != -1 && !classicHighlight(current))
			{
				modernSelected = current;
				modernTiling = stone.getSpriteTiling();
			}
			if (index == selected)
			{
				// Classic tiles the middle highlight across the stone and draws the corner ones once.
				stone.setSpriteId(selectedSprite(index < 7, slot));
				stone.setSpriteTiling(!corner);
			}
			else
			{
				stone.setSpriteId(-1);
			}
			states.position(stone, slotX(slot), 0);
			stone.revalidate();

			if (icon == null || icon.getParent() != stone.getParent())
			{
				continue;
			}
			int iconX = slot == 0 ? CORNER_ICON_INSET
				: slot == 6 ? FRAME_WIDTH - CORNER_ICON_INSET - SLOT_WIDTH : slotX(slot);
			states.position(icon, iconX + (SLOT_WIDTH - icon.getWidth()) / 2, iconY);
			icon.revalidate();
		}

		// Modern keeps logout as an X by the minimap. Classic has a door stone in the fourth bottom slot, so one is
		// drawn there and wired to the same client script, with the same arguments, as Modern's own logout button.
		if (selected == LOGOUT_PANEL)
		{
			Widget highlight = states.addChild(bottomStones, WidgetType.GRAPHIC);
			highlight.setSpriteId(SELECTED_MIDDLE);
			highlight.setSpriteTiling(true);
			highlight.setOriginalX(slotX(LOGOUT_SLOT));
			highlight.setOriginalY(0);
			highlight.setOriginalWidth(SLOT_WIDTH);
			highlight.setOriginalHeight(STONE_HEIGHT);
			highlight.revalidate();
		}
		// The game blanks the stones while a bank, shop or trade inventory replaces the side panel; so does the door.
		if (!modal)
		{
			Widget door = states.addChild(bottomStones, WidgetType.GRAPHIC);
			door.setSpriteId(LOGOUT_ICON_SPRITE);
			door.setOriginalX(slotX(LOGOUT_SLOT));
			door.setOriginalY(0);
			door.setOriginalWidth(SLOT_WIDTH);
			door.setOriginalHeight(STONE_HEIGHT);
			door.setAction(0, "Logout");
			door.setHasListener(true);
			door.setOnOpListener(SIDEBUTTON_OP_SCRIPT, EVENT_OP, LOGOUT_OP_ARGUMENT, LOGOUT_PANEL);
			door.revalidate();
		}
		if (hideLogoutX)
		{
			for (int component : MODERN_LOGOUT)
			{
				states.hide(client.getWidget(component));
			}
		}
		applied = true;

		// The panel is moved even while closed. RuneLite overlays attached to the side panel are positioned
		// relative to it, so this is what makes them drop down with the frame when it collapses.
		states.place(container, x + CONTENTS_X - MODERN_BORDER, topY + CONTENTS_Y - MODERN_BORDER);
		if (!open)
		{
			keepTracked(container);
			return;
		}

		Widget[] border = container.getDynamicChildren();
		if (border != null)
		{
			for (Widget piece : border)
			{
				if (piece != null && piece.getType() == WidgetType.GRAPHIC)
				{
					states.hide(piece);
				}
			}
		}
		states.hide(client.getWidget(InterfaceID.ToplevelPreEoc.SIDE_BACKGROUND));
		framed = true;

		int contentsX = x + CONTENTS_X;
		int barY = topY + CONTENTS_Y + BAR_INSET_Y;
		leftBar = new Rectangle(contentsX - BAR_WIDTH, barY, BAR_WIDTH, BAR_HEIGHT);
		rightBar = new Rectangle(contentsX + CONTENTS_WIDTH, barY, BAR_WIDTH, BAR_HEIGHT);
	}

	/** Undoes the two things {@link WidgetStates} does not track. Call before {@link WidgetStates#restore()}. */
	void restore()
	{
		leftBar = null;
		rightBar = null;
		panelArea = null;
		// Hand the panel back hidden, unless the game has opened it in the meantime.
		if (shownWhileClosed != null)
		{
			if (client.getVarcIntValue(VarClientID.TOPLEVEL_PANEL) == -1 && !modal())
			{
				shownWhileClosed.setHidden(true);
			}
			shownWhileClosed = null;
		}
		// Hand any stone carrying a Classic highlight back to Modern's own highlight.
		for (int index = 0; index < STONES.length; index++)
		{
			Widget stone = index == LOGOUT_PANEL ? null : client.getWidget(STONES[index][0]);
			if (stone == null || !classicHighlight(stone.getSpriteId()))
			{
				continue;
			}
			stone.setSpriteId(modernSelected);
			if (modernSelected != -1)
			{
				stone.setSpriteTiling(modernTiling);
			}
		}
	}

	/**
	 * True when what is on screen no longer matches what was last applied: the panel opened or closed, the window
	 * changed size, a different tab was selected, or the game redrew something this plugin had hidden. Checked
	 * before every frame, because the game changes these through routes that raise no event.
	 */
	boolean stale()
	{
		if (!applied)
		{
			return false;
		}
		Widget container = client.getWidget(InterfaceID.ToplevelPreEoc.SIDE_CONTAINER);
		if (container == null || container.getParent() == null)
		{
			return false;
		}
		boolean open = open(container);
		if (open != lastOpen || !WidgetStates.bounds(container.getParent()).equals(lastGame))
		{
			return true;
		}

		for (int component : hideLogoutX ? MODERN_LOGOUT : NO_COMPONENTS)
		{
			Widget logout = client.getWidget(component);
			if (logout != null && !logout.isSelfHidden())
			{
				return true;
			}
		}
		int selected = selected(open, modal());
		for (int index = 0; index < STONES.length; index++)
		{
			Widget stone = index == LOGOUT_PANEL ? null : client.getWidget(STONES[index][0]);
			if (stone != null
				&& stone.getSpriteId() != (index == selected ? selectedSprite(index < 7, slot(index)) : -1))
			{
				return true;
			}
		}
		if (!framed || !open)
		{
			return false;
		}
		Widget backdrop = client.getWidget(InterfaceID.ToplevelPreEoc.SIDE_BACKGROUND);
		if (backdrop != null && !backdrop.isSelfHidden())
		{
			return true;
		}
		Widget[] border = container.getDynamicChildren();
		if (border == null)
		{
			return false;
		}
		for (Widget piece : border)
		{
			if (piece != null && piece.getType() == WidgetType.GRAPHIC && !piece.isSelfHidden())
			{
				return true;
			}
		}
		return false;
	}

	/** Open when a tab is selected, or when a bank, shop or trade inventory has taken over the side panel. */
	private boolean open(Widget container)
	{
		return !container.isHidden() && (client.getVarcIntValue(VarClientID.TOPLEVEL_PANEL) != -1 || modal());
	}

	private boolean modal()
	{
		Widget modal = client.getWidget(InterfaceID.ToplevelPreEoc.SIDEMODAL);
		if (modal == null)
		{
			return false;
		}
		Widget[] contents = modal.getNestedChildren();
		return contents != null && contents.length > 0;
	}

	private int selected(boolean open, boolean modal)
	{
		return open && !modal ? client.getVarcIntValue(VarClientID.TOPLEVEL_PANEL) : -1;
	}

	/**
	 * RuneLite only records where a widget is on screen while that widget is drawn, and overlays attached to the
	 * side panel read that record. A closed panel is hidden, so its record would stay at the open position. Here
	 * the closed panel is drawn empty instead: its contents are hidden, it stops intercepting clicks, and it is
	 * unhidden.
	 */
	private void keepTracked(Widget container)
	{
		if (!container.isSelfHidden())
		{
			return;
		}
		states.hideAll(container.getStaticChildren());
		states.hideAll(container.getDynamicChildren());
		states.capture(container);
		container.setNoClickThrough(false);
		container.setHidden(false);
		shownWhileClosed = container;
	}

	/** Classic orders the bottom row Clan, Friends, Account; Modern has Account before Friends. */
	private static int slot(int index)
	{
		return index == 8 ? 2 : index == 9 ? 1 : index % 7;
	}

	/** Each row is a 38px corner stone, five 33px stones, and another 38px corner stone. */
	private static int slotX(int slot)
	{
		return slot == 0 ? 0 : CORNER_WIDTH + SLOT_WIDTH * (slot - 1);
	}

	private static int selectedSprite(boolean top, int slot)
	{
		if (slot == 0)
		{
			return top ? SELECTED_TOP_LEFT : SELECTED_BOTTOM_LEFT;
		}
		if (slot == 6)
		{
			return top ? SELECTED_TOP_RIGHT : SELECTED_BOTTOM_RIGHT;
		}
		return SELECTED_MIDDLE;
	}

	private static boolean classicHighlight(int sprite)
	{
		return sprite >= SELECTED_TOP_LEFT && sprite <= SELECTED_MIDDLE;
	}
}
