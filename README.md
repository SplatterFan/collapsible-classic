# Collapsible Classic

A RuneLite plugin that gives **Resizable – Modern layout** the look of **Resizable – Classic layout**, while keeping the one thing Modern does better: clicking the active tab collapses the side panel out of the way.

## What it does

- Draws the classic side panel frame around Modern's panel: the tapered stone rows, the two pillars and the backdrop.
- Uses Classic's tab highlights, including the tapered ones on the four corner stones.
- Puts a logout stone back in the bottom row and hides Modern's logout X by the minimap.
- When the panel is closed, stacks the two stone rows in the bottom-right corner.
- Keeps the frame around bank, shop and trade inventories.
- Optionally draws Hitpoints and Prayer bars over the pillars.

Everything is drawn with the game's own sprites, so resource packs apply to the frame as well.

## Using it

1. In the game's settings, set **Game client layout** to **Resizable – Modern layout**.
2. Turn the plugin on.

The plugin does nothing in Fixed or Resizable – Classic.

## Settings

| Setting | What it does |
|---|---|
| Fill collapsed gaps | With the panel closed, fills the small gaps between the stacked stones with the inventory background. |
| Hide XP button | Hides the XP button beside the minimap. |
| Show status bars | Draws Hitpoints and Prayer bars over the pillars, with icon, value and status-effect colours. Turn off RuneLite's own Status Bars plugin to avoid two sets. |
| Restore preview | Shows how much the inventory item under the mouse would restore. |
| Hide after combat | Ticks to keep the status bars showing after combat ends. 0 always shows them. |

## Making RuneLite boxes follow the panel

RuneLite can attach any movable box (Boosts, for example) to the side panel:

1. Hold **Alt** and drag the box to where you want it beside the frame.
2. Release Alt, hold **Shift**, right-click the box and choose **Overlay Origin → Sidepanel**.

The box then moves down with the frame when the panel collapses and back up when it opens.

## Conflicts

Turn off other plugins that rearrange the side stones or the side panel, such as Interface Layout, Fixed Resizable Hybrid and Menu Stones Hider.

## Credits

- The widget state and redraw handling is adapted from [Interface Layout](https://github.com/wukuna/interface-layout) by wukuna and contributors.
- The status bars are adapted from RuneLite's own [Status Bars](https://github.com/runelite/runelite) plugin by Jos and Rheon.

Both are BSD 2-Clause licensed, as is this plugin. See `LICENSE`.
