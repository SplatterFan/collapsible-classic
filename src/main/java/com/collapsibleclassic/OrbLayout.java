package com.collapsibleclassic;

import java.awt.Point;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import lombok.Getter;

/**
 * Ready-made orb layouts. The coordinates come from the Compact Orbs plugin by its-cue (BSD-2-Clause): each preset
 * is a compact block in the top-right corner of the screen, with the minimap hidden and every orb placed at a fixed
 * spot inside the block. CUSTOM means no preset: orbs sit where you dragged them, or in their normal places.
 */
@Getter
public enum OrbLayout
{
	CUSTOM("Custom (drag with Alt)", 0, 0, null),
	VERTICAL("Vertical", 96, 212, positions(
		7, 42,     // XP
		39, 39,    // Hitpoints
		39, 73,    // Prayer
		39, 107,   // Run
		39, 141,   // Special
		3, 101,    // Store
		3, 136,    // Activity
		5, 70,     // World map
		38, 178,   // Wiki
		20, 1)),   // Compass ring
	HORIZONTAL("Horizontal", 209, 102, positions(
		179, 25,
		35, 33,
		35, 68,
		93, 33,
		93, 68,
		0, 34,
		0, 68,
		146, 16,
		151, 88,
		149, 46)),
	HORIZONTAL_WIDE("Horizontal (wide)", 228, 79, positions(
		75, 12,
		0, 45,
		57, 45,
		114, 45,
		171, 45,
		37, 10,
		0, 10,
		167, 0,
		162, 31,
		110, 1));

	/** Order of the coordinate pairs given to each preset. */
	private static final MovableOrbs.Orb[] ORDER = {
		MovableOrbs.Orb.XP, MovableOrbs.Orb.HITPOINTS, MovableOrbs.Orb.PRAYER, MovableOrbs.Orb.RUN,
		MovableOrbs.Orb.SPECIAL, MovableOrbs.Orb.STORE, MovableOrbs.Orb.ACTIVITY, MovableOrbs.Orb.WORLD_MAP,
		MovableOrbs.Orb.WIKI, MovableOrbs.Orb.COMPASS
	};

	private final String label;
	/** Width of the block, which sits against the right edge of the minimap area. */
	private final int width;
	/** Height of the block, used for the Alt-drag box. */
	private final int height;
	private final int[] coordinates;

	OrbLayout(String label, int width, int height, int[] coordinates)
	{
		this.label = label;
		this.width = width;
		this.height = height;
		this.coordinates = coordinates;
	}

	private static int[] positions(int... xy)
	{
		return xy;
	}

	/** Where each piece goes inside the block (for the compass, where its 43px ring goes). Empty for CUSTOM. */
	Map<MovableOrbs.Orb, Point> offsets()
	{
		if (coordinates == null)
		{
			return Collections.emptyMap();
		}
		Map<MovableOrbs.Orb, Point> map = new EnumMap<>(MovableOrbs.Orb.class);
		for (int i = 0; i < ORDER.length; i++)
		{
			map.put(ORDER[i], new Point(coordinates[i * 2], coordinates[i * 2 + 1]));
		}
		return map;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
