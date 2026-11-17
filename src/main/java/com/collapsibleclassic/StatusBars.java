/*
 * Copyright (c) 2026, SplatterFan
 * Adapted from RuneLite's Status Bars plugin (net.runelite.client.plugins.statusbars):
 * Copyright (c) 2018-2019, Jos <Malevolentdev@gmail.com>
 * Copyright (c) 2019, Rheon <https://github.com/Rheon-D>
 * BSD 2-Clause; see LICENSE.
 */
package com.collapsibleclassic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.GameState;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.AlternateSprites;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.itemstats.Effect;
import net.runelite.client.plugins.itemstats.ItemStatChangesService;
import net.runelite.client.plugins.itemstats.StatChange;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.TextComponent;
import net.runelite.client.util.ImageUtil;

/**
 * Hitpoints and Prayer bars drawn over the classic frame's two pillars, in the same place and style RuneLite's
 * Status Bars plugin uses in Resizable Classic. They exist only while the frame is open, so they collapse with it.
 */
@Singleton
class StatusBars extends Overlay
{
	private static final Color BACKGROUND = new Color(0, 0, 0, 150);
	private static final Color OVERHEAL = new Color(216, 255, 139, 150);
	private static final Color HEALTH = new Color(225, 35, 0, 125);
	private static final Color POISONED = new Color(0, 145, 0, 150);
	private static final Color VENOMED = new Color(0, 65, 0, 150);
	private static final Color DISEASED = new Color(255, 193, 75, 181);
	private static final Color PARASITE = new Color(196, 62, 109, 181);
	private static final Color HEAL = new Color(255, 112, 6, 150);
	private static final Color PRAYER = new Color(50, 200, 200, 175);
	private static final Color ACTIVE_PRAYER = new Color(57, 255, 186, 225);
	private static final Color PRAYER_HEAL = new Color(57, 255, 186, 75);
	private static final int BORDER = 1, ICON_TOP = 4, COUNTER_BELOW_ICON = 35, COUNTER_WITHOUT_ICON = 18;
	private static final int VENOM_THRESHOLD = 1000000;

	@Inject
	private Client client;

	@Inject
	private CollapsibleClassicConfig config;

	@Inject
	private ClassicFrame frame;

	@Inject
	private SkillIconManager icons;

	@Inject
	private SpriteManager sprites;

	@Inject
	private ItemStatChangesService itemStats;

	private final Image heartDisease = ImageUtil.loadImageResource(AlternateSprites.class, AlternateSprites.DISEASE_HEART);
	private final Image heartPoison = ImageUtil.loadImageResource(AlternateSprites.class, AlternateSprites.POISON_HEART);
	private final Image heartVenom = ImageUtil.loadImageResource(AlternateSprites.class, AlternateSprites.VENOM_HEART);
	private int checkedTick = -1, lastCombatTick;
	private boolean displayed = true;

	StatusBars()
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.statusBars() || client.getGameState() != GameState.LOGGED_IN)
		{
			return null;
		}
		Rectangle left = frame.leftBar();
		Rectangle right = frame.rightBar();
		if (left == null || right == null)
		{
			return null;
		}
		if (client.getTickCount() != checkedTick)
		{
			checkedTick = client.getTickCount();
			checkCombat();
		}
		if (!displayed)
		{
			return null;
		}

		boolean lastManStanding = client.getWidget(InterfaceID.BrOverlay.CONTENT) != null;
		bar(graphics, left, client.getBoostedSkillLevel(Skill.HITPOINTS),
			lastManStanding ? Experience.MAX_REAL_LEVEL : client.getRealSkillLevel(Skill.HITPOINTS),
			restore(Skill.HITPOINTS.getName()), healthColour(), HEAL, healthIcon());
		bar(graphics, right, client.getBoostedSkillLevel(Skill.PRAYER),
			lastManStanding ? Experience.MAX_REAL_LEVEL : client.getRealSkillLevel(Skill.PRAYER),
			restore(Skill.PRAYER.getName()), prayerColour(), PRAYER_HEAL, icons.getSkillImage(Skill.PRAYER, true));
		return null;
	}

	private void bar(Graphics2D graphics, Rectangle area, int current, int maximum, int restore, Color fill,
		Color restoreFill, Image icon)
	{
		int filled = height(maximum, current, area.height);
		graphics.setColor(BACKGROUND);
		graphics.drawRect(area.x, area.y, area.width - BORDER, area.height - BORDER);
		graphics.fillRect(area.x, area.y, area.width, area.height);
		graphics.setColor(fill);
		graphics.fillRect(area.x + BORDER, area.y + BORDER + (area.height - filled),
			area.width - BORDER * 2, filled - BORDER * 2);

		if (config.restorePreview() && restore > 0)
		{
			int restored = height(maximum, restore, area.height);
			int fillY, fillHeight;
			graphics.setColor(restoreFill);
			if (restored + filled > area.height)
			{
				graphics.setColor(OVERHEAL);
				fillY = area.y + BORDER;
				fillHeight = area.height - filled - BORDER;
			}
			else
			{
				fillY = area.y + BORDER + area.height - (filled + restored);
				fillHeight = restored;
			}
			graphics.fillRect(area.x + BORDER, fillY, area.width - BORDER * 2, fillHeight);
		}

		if (icon != null)
		{
			graphics.drawImage(icon, area.x + area.width / 2 - icon.getWidth(null) / 2, area.y + ICON_TOP, null);
		}
		graphics.setFont(FontManager.getRunescapeSmallFont());
		String text = Integer.toString(current);
		TextComponent counter = new TextComponent();
		counter.setText(text);
		counter.setPosition(new Point(area.x + area.width / 2 - graphics.getFontMetrics().stringWidth(text) / 2,
			area.y + (icon != null ? COUNTER_BELOW_ICON : COUNTER_WITHOUT_ICON)));
		counter.render(graphics);
	}

	private static int height(int base, int current, int size)
	{
		if (base <= 0)
		{
			return 0;
		}
		double ratio = (double) current / base;
		return ratio >= 1 ? size : (int) Math.round(ratio * size);
	}

	private Color healthColour()
	{
		int poison = client.getVarpValue(VarPlayerID.POISON);
		if (poison >= VENOM_THRESHOLD)
		{
			return VENOMED;
		}
		if (poison > 0)
		{
			return POISONED;
		}
		if (client.getVarpValue(VarPlayerID.DISEASE) > 0)
		{
			return DISEASED;
		}
		if (client.getVarbitValue(VarbitID.PARASITE) >= 1)
		{
			return PARASITE;
		}
		return HEALTH;
	}

	private Image healthIcon()
	{
		int poison = client.getVarpValue(VarPlayerID.POISON);
		if (poison > 0 && poison < 50)
		{
			return heartPoison;
		}
		if (poison >= VENOM_THRESHOLD)
		{
			return heartVenom;
		}
		if (client.getVarpValue(VarPlayerID.DISEASE) > 0)
		{
			return heartDisease;
		}
		return sprites.getSprite(SpriteID.OrbIcon.HITPOINTS, 0);
	}

	private Color prayerColour()
	{
		for (Prayer prayer : Prayer.values())
		{
			if (client.isPrayerActive(prayer))
			{
				return ACTIVE_PRAYER;
			}
		}
		return PRAYER;
	}

	/** How much the inventory item under the mouse would restore of the named stat. */
	private int restore(String skill)
	{
		MenuEntry[] menu = client.getMenuEntries();
		if (menu.length == 0)
		{
			return 0;
		}
		Widget widget = menu[menu.length - 1].getWidget();
		if (widget == null || widget.getId() != InterfaceID.Inventory.ITEMS)
		{
			return 0;
		}
		Effect change = itemStats.getItemStatChanges(widget.getItemId());
		if (change == null)
		{
			return 0;
		}
		int value = 0;
		for (StatChange stat : change.calculate(client).getStatChanges())
		{
			if (stat.getTheoretical() != 0 && stat.getStat().getName().equals(skill))
			{
				value = stat.getTheoretical();
			}
		}
		return value;
	}

	/** With a delay set, the bars show only while fighting and for that many ticks afterwards. */
	private void checkCombat()
	{
		int delay = config.hideAfterCombatDelay();
		if (delay <= 0)
		{
			displayed = true;
			return;
		}
		Player local = client.getLocalPlayer();
		if (local == null)
		{
			return;
		}
		Actor target = local.getInteracting();
		boolean fighting = false;
		if (target instanceof NPC)
		{
			NPCComposition composition = ((NPC) target).getComposition();
			String[] actions = composition == null ? null : composition.getActions();
			if (actions != null)
			{
				for (String action : actions)
				{
					fighting |= "Attack".equals(action);
				}
			}
		}
		else if (target instanceof Player)
		{
			fighting = client.getVarbitValue(VarbitID.PVP_AREA_CLIENT) == 1;
		}
		if (fighting)
		{
			lastCombatTick = client.getTickCount();
			displayed = true;
		}
		else if (client.getTickCount() - lastCombatTick >= delay)
		{
			displayed = false;
		}
	}
}
