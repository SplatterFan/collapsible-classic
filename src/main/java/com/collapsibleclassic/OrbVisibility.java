package com.collapsibleclassic;

/**
 * When an orb (or the compass or minimap) is hidden: never, only in the default layout, only in your moved layout,
 * or in both. The default layout is the game's own placement: Default orb positions on, or nothing moved at all.
 */
public enum OrbVisibility
{
	SHOW("Show", false, false),
	HIDE_DEFAULT("Hide in default layout", true, false),
	HIDE_MOVED("Hide in moved layout", false, true),
	HIDE_BOTH("Hide in both", true, true);

	private final String label;
	private final boolean inDefault;
	private final boolean inMoved;

	OrbVisibility(String label, boolean inDefault, boolean inMoved)
	{
		this.label = label;
		this.inDefault = inDefault;
		this.inMoved = inMoved;
	}

	boolean hiddenIn(boolean defaultLayout)
	{
		return defaultLayout ? inDefault : inMoved;
	}

	/** The same setting with the current layout flipped between shown and hidden; the other layout is kept. */
	OrbVisibility toggledIn(boolean defaultLayout)
	{
		boolean d = defaultLayout ? !inDefault : inDefault;
		boolean m = defaultLayout ? inMoved : !inMoved;
		return d ? (m ? HIDE_BOTH : HIDE_DEFAULT) : (m ? HIDE_MOVED : SHOW);
	}

	@Override
	public String toString()
	{
		return label;
	}
}
