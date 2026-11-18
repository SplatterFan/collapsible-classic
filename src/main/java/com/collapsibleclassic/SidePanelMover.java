/*
 * Copyright (c) 2026, SplatterFan
 * BSD 2-Clause; see LICENSE.
 */
package com.collapsibleclassic;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * One Alt-drag box over the whole classic side panel, so the frame, both stone rows and the panel contents move as
 * one piece. The box is always the size of the open panel, because a closed panel is shorter but sits in the same
 * place: the saved position is the open panel's top-left corner, and a closed panel keeps the same bottom edge.
 */
@Singleton
class SidePanelMover extends Overlay
{
	static final String KEY = "sidePanelPosition";

	@Inject
	private CollapsibleClassicConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ClassicFrame frame;

	private Runnable refresh;
	private boolean running;
	private Point lastApplied;
	private boolean reset;

	SidePanelMover()
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.UNDER_WIDGETS);
		setPriority(Overlay.PRIORITY_HIGHEST);
		setMovable(true);
		// Snapping would hand the position to a RuneLite corner instead of a point on the screen.
		setSnappable(false);
	}

	@Override
	public String getName()
	{
		return "COLLAPSIBLE_CLASSIC_SIDE_PANEL";
	}

	void start(Runnable refresh)
	{
		this.refresh = refresh;
		running = true;
		overlayManager.add(this);
	}

	void stop()
	{
		running = false;
		getBounds().setSize(0, 0);
		overlayManager.remove(this);
	}

	/** Puts the panel back in the bottom-right corner, forgetting RuneLite's record of the drag box too. */
	void resetPosition()
	{
		configManager.unsetConfiguration(CollapsibleClassicConfig.GROUP, KEY);
		lastApplied = null;
		overlayManager.resetOverlay(this);
	}

	// RuneLite calls this when the box is reset with Alt right-click; the panel then goes back to its corner.
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
		if (reset && !placed)
		{
			lastApplied = null;
			configManager.unsetConfiguration(CollapsibleClassicConfig.GROUP, KEY);
			refresh.run();
		}
		reset = false;

		Rectangle panel = config.moveSidePanel() ? frame.panelArea() : null;
		if (panel == null)
		{
			getBounds().setSize(0, 0);
			return null;
		}
		if (placed)
		{
			Point canvas = getBounds().getLocation();
			if (!canvas.equals(lastApplied))
			{
				lastApplied = new Point(canvas);
				configManager.setConfiguration(CollapsibleClassicConfig.GROUP, KEY, canvas);
				refresh.run();
			}
		}
		else
		{
			getBounds().setLocation(panel.getLocation());
		}
		return panel.getSize();
	}
}
