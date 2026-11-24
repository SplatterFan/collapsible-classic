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

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.api.widgets.WidgetType;

/**
 * Remembers every change made to the game's interface so it can all be undone: positions, sizes, visibility,
 * widgets turned into layers, and widgets created. The game redraws its interface from its own scripts, so the
 * plugin undoes everything before those scripts run and re-applies it afterwards.
 */
@Singleton
class WidgetStates
{
	@Inject
	private Client client;

	private final Map<Widget, State> states = new IdentityHashMap<>();
	private final Map<Widget, Boolean> expanded = new IdentityHashMap<>();
	private final Map<Widget, List<Widget>> created = new IdentityHashMap<>();

	/** Where a widget sits on the game canvas, worked out from its parents. Valid for hidden widgets too. */
	static Point location(Widget widget)
	{
		Point point = new Point();
		Set<Widget> visited = Collections.newSetFromMap(new IdentityHashMap<>());
		for (Widget current = widget; current != null && visited.add(current); current = current.getParent())
		{
			point.translate(current.getRelativeX(), current.getRelativeY());
			Widget parent = current.getParent();
			if (parent != null)
			{
				point.translate(-parent.getScrollX(), -parent.getScrollY());
			}
		}
		return point;
	}

	static Rectangle bounds(Widget widget)
	{
		if (widget == null)
		{
			return new Rectangle();
		}
		Point point = location(widget);
		return new Rectangle(point.x, point.y, widget.getWidth(), widget.getHeight());
	}

	void capture(Widget widget)
	{
		if (widget != null)
		{
			states.computeIfAbsent(widget, State::new);
		}
	}

	void hide(Widget widget)
	{
		if (widget == null)
		{
			return;
		}
		capture(widget);
		State state = states.get(widget);
		if (state.hidden == null)
		{
			state.hidden = widget.isSelfHidden();
		}
		widget.setHidden(true);
	}

	void hideAll(Widget[] widgets)
	{
		if (widgets == null)
		{
			return;
		}
		for (Widget widget : widgets)
		{
			hide(widget);
		}
	}

	/** Positions a widget relative to its parent, overriding the game's own layout until restored. */
	void position(Widget widget, int x, int y)
	{
		if (widget == null)
		{
			return;
		}
		capture(widget);
		// Set as an ordinary top-left position as well as a forced one. Some pieces are laid out from the right
		// edge of a parent that has been stretched, and a forced position below zero is ignored; either way the
		// piece would otherwise jump. Both are put back on restore.
		widget.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
		widget.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		widget.setOriginalX(x);
		widget.setOriginalY(y);
		widget.setForcedPosition(x, y);
	}

	/** Positions a widget at a point on the game canvas. */
	void place(Widget widget, int canvasX, int canvasY)
	{
		Point parent = location(widget.getParent());
		position(widget, canvasX - parent.x, canvasY - parent.y);
		widget.revalidate();
	}

	void resize(Widget widget, int width, int height)
	{
		capture(widget);
		widget.setOriginalWidth(width);
		widget.setOriginalHeight(height);
		widget.setWidthMode(WidgetSizeMode.ABSOLUTE);
		widget.setHeightMode(WidgetSizeMode.ABSOLUTE);
	}

	/**
	 * Turns a graphic into an empty layer that can hold sprites anywhere on the canvas. Its own sprite stops being
	 * drawn, and it and its parents are stretched over the canvas so their contents are not clipped.
	 */
	Widget asLayer(Widget widget)
	{
		if (widget == null)
		{
			return null;
		}
		capture(widget);
		State state = states.get(widget);
		if (state.type == null)
		{
			state.type = widget.getType();
			state.contentType = widget.getContentType();
			state.sprite = widget.getSpriteId();
			expandAcrossCanvas(widget);
			widget.setType(WidgetType.LAYER);
		}
		return widget;
	}

	/** Creates a child widget that is removed again on restore. */
	Widget addChild(Widget parent, int type)
	{
		Widget child = parent.createChild(-1, type);
		created.computeIfAbsent(parent, ignored -> new ArrayList<>()).add(child);
		return child;
	}

	/** Draws a sprite at a rectangle on the game canvas, as a child of the given layer. */
	Widget addSprite(Widget layer, int sprite, boolean tiled, Rectangle canvas)
	{
		Widget art = addChild(layer, WidgetType.GRAPHIC);
		art.setSpriteId(sprite);
		art.setSpriteTiling(tiled);
		art.setNoClickThrough(true);
		Point parent = location(layer);
		art.setOriginalX(canvas.x - parent.x);
		art.setOriginalY(canvas.y - parent.y);
		art.setOriginalWidth(canvas.width);
		art.setOriginalHeight(canvas.height);
		art.revalidate();
		return art;
	}

	/** Forgets widgets the game has thrown away and replaced, so they are never written to again. */
	void discardRetired()
	{
		states.keySet().removeIf(widget ->
		{
			Widget current = client.getWidget(widget.getId());
			if (current != null && widget.getIndex() >= 0)
			{
				current = current.getChild(widget.getIndex());
			}
			return current != widget;
		});
	}

	void restore()
	{
		created.forEach((parent, children) ->
		{
			Widget[] current = parent.getChildren();
			if (current == null)
			{
				return;
			}
			Widget[] remaining = current.clone();
			for (Widget child : children)
			{
				int index = child.getIndex();
				if (index >= 0 && index < remaining.length && remaining[index] == child)
				{
					remaining[index] = null;
				}
			}
			int length = remaining.length;
			while (length > 0 && remaining[length - 1] == null)
			{
				length--;
			}
			parent.setChildren(Arrays.copyOf(remaining, length));
		});
		created.clear();

		states.forEach((widget, state) -> state.restore(widget));
		states.keySet().forEach(widget -> widget.setForcedPosition(-1, -1));
		states.keySet().stream().sorted(Comparator.comparingInt(WidgetStates::depth)).forEach(Widget::revalidate);
		states.clear();
		expanded.clear();
	}

	/** True when the widget with this id has been stretched over the canvas. */
	boolean isExpanded(int componentId)
	{
		return expanded.keySet().stream().anyMatch(widget -> widget.getId() == componentId);
	}

	private void expandAcrossCanvas(Widget widget)
	{
		expandAncestors(widget);
		expand(widget);
	}

	/** Stretches a widget's parents over the canvas, so the widget can be placed anywhere without being cut off. */
	void expandAncestors(Widget widget)
	{
		List<Widget> ancestors = new ArrayList<>();
		for (Widget parent = widget.getParent(); parent != null && ancestors.size() < 16; parent = parent.getParent())
		{
			ancestors.add(parent);
			if (coversCanvas(parent))
			{
				break;
			}
		}
		Collections.reverse(ancestors);
		for (Widget parent : ancestors)
		{
			expand(parent);
		}
	}

	private boolean coversCanvas(Widget widget)
	{
		Rectangle bounds = bounds(widget);
		return bounds.x == 0 && bounds.y == 0 && bounds.width >= client.getCanvasWidth()
			&& bounds.height >= client.getCanvasHeight();
	}

	/** Stretches a widget over the whole canvas while keeping each of its children exactly where it was. */
	private void expand(Widget parent)
	{
		if (expanded.containsKey(parent) || coversCanvas(parent))
		{
			return;
		}
		expanded.put(parent, true);

		Map<Widget, Point> positions = new IdentityHashMap<>();
		collect(parent.getStaticChildren(), positions);
		collect(parent.getDynamicChildren(), positions);
		// A sub-interface (the orbs, for example) is left alone: the game places and sizes it from its parent,
		// and only partly honours changes made to it directly. It is stretched along with its parent instead,
		// and its own pieces are held where they were.
		Widget[] nested = parent.getNestedChildren();
		if (nested != null)
		{
			for (Widget root : nested)
			{
				if (root != null)
				{
					collect(root.getStaticChildren(), positions);
					collect(root.getDynamicChildren(), positions);
				}
			}
		}
		positions.keySet().forEach(child ->
		{
			capture(child);
			int width = child.getWidth();
			int height = child.getHeight();
			child.setOriginalWidth(width);
			child.setOriginalHeight(height);
			child.setWidthMode(WidgetSizeMode.ABSOLUTE);
			child.setHeightMode(WidgetSizeMode.ABSOLUTE);
		});

		capture(parent);
		Rectangle outer = bounds(parent.getParent());
		position(parent, -outer.x, -outer.y);
		parent.setOriginalWidth(client.getCanvasWidth());
		parent.setOriginalHeight(client.getCanvasHeight());
		parent.setWidthMode(WidgetSizeMode.ABSOLUTE);
		parent.setHeightMode(WidgetSizeMode.ABSOLUTE);
		parent.setNoClickThrough(false);
		parent.revalidate();

		if (nested != null)
		{
			for (Widget root : nested)
			{
				if (root != null)
				{
					root.revalidate();
					// Treated as already stretched, so it is never altered directly later on.
					expanded.put(root, true);
				}
			}
		}
		positions.forEach((child, canvas) ->
		{
			Point origin = location(child.getParent());
			position(child, canvas.x - origin.x, canvas.y - origin.y);
			child.revalidate();
		});
	}

	private static void collect(Widget[] children, Map<Widget, Point> positions)
	{
		if (children == null)
		{
			return;
		}
		for (Widget child : children)
		{
			if (child != null)
			{
				positions.put(child, location(child));
			}
		}
	}

	private static int depth(Widget widget)
	{
		int depth = 0;
		for (Widget parent = widget.getParent(); parent != null && depth < 32; parent = parent.getParent())
		{
			depth++;
		}
		return depth;
	}

	private static final class State
	{
		private final int x, y, xMode, yMode, width, height, widthMode, heightMode;
		private final boolean noClickThrough;
		private Integer type, contentType, sprite;
		private Boolean hidden;

		State(Widget widget)
		{
			width = widget.getOriginalWidth();
			height = widget.getOriginalHeight();
			x = widget.getOriginalX();
			y = widget.getOriginalY();
			xMode = widget.getXPositionMode();
			yMode = widget.getYPositionMode();
			widthMode = widget.getWidthMode();
			heightMode = widget.getHeightMode();
			noClickThrough = widget.getNoClickThrough();
		}

		void restore(Widget widget)
		{
			widget.setOriginalWidth(width);
			widget.setOriginalHeight(height);
			widget.setOriginalX(x);
			widget.setOriginalY(y);
			widget.setXPositionMode(xMode);
			widget.setYPositionMode(yMode);
			widget.setWidthMode(widthMode);
			widget.setHeightMode(heightMode);
			if (type != null)
			{
				widget.setType(type);
				widget.setContentType(contentType);
				widget.setSpriteId(sprite);
			}
			if (hidden != null)
			{
				widget.setHidden(hidden);
			}
			widget.setNoClickThrough(noClickThrough);
		}
	}
}
