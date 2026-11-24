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
