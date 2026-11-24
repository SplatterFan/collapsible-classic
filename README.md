# Collapsible Classic

A RuneLite plugin that gives **Resizable – Modern layout** the look of **Resizable – Classic layout**, while keeping the one thing Modern does better: clicking the active tab collapses the side panel out of the way.

## The idea

Collapsible Classic is about a **minimal** screen. Collapse the side panel, tuck the orbs away, and keep the game world clear, then **return to the original layout at any time** with one click, a hotkey, or just by opening the panel again.

The orb customisation is built for **looks**: moving, hiding and laying out the orbs so the screen stays tidy. It doesn't try to match the depth of dedicated orb plugins. If you want more from your orbs, use [Compact Orbs](https://github.com/its-cue/compact-orbs); this plugin works alongside it and leaves the orbs to it automatically.

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

### Collapsible side panel

| Setting | What it does |
|---|---|
| Fill collapsed gaps | With the panel closed, fills the small gaps between the stacked stones with the inventory background. |
| Hide Modern logout X | On by default, since the panel has its own logout stone. Switches itself off while Compact Orbs is running; use its own Hide logout X setting then. |

To move the whole panel (frame, stones and inventory) as one piece, hold **Alt** and drag the **bottom stone row**. It still collapses where you put it. **Alt + right-click** on that row puts it back in the corner.

### Status bars

| Setting | What it does |
|---|---|
| Show status bars | Off by default. Draws Hitpoints and Prayer bars over the pillars, with icon, value, status-effect colours and a preview of what the hovered food or potion restores. Turn off RuneLite's own Status Bars plugin to avoid two sets. |
| Hide after combat | Ticks to keep the status bars showing after combat ends. 0 always shows them. |

### Orbs

| Setting | What it does |
|---|---|
| Orb features | Off by default. Turns on everything below and in Orb visibility. While it's off, or while Compact Orbs is running, the minimap and orbs are left completely alone. |
| Orb layout | Ready-made compact layouts borrowed from Compact Orbs: Vertical, Horizontal and Horizontal (wide). They hide the minimap and pack the orbs into the top-right corner. Custom (the default) leaves placement to you. With Move orbs on, Alt-drag moves a whole preset; each preset remembers its own spot. |
| Move orbs | Lets the orbs, compass and minimap be dragged while Alt is held. |
| Default orb positions | Puts the orbs, compass, minimap and multi-combat icon back in their normal places without forgetting where you dragged them. Switch it off to get your layout back. (Move the multi-combat icon with RuneLite's own Alt-drag.) |
| Default positions hotkey | A key that switches Default orb positions on and off. |
| Moved layout only when collapsed | Uses the default orb layout while the side panel is open and switches to your moved layout (dragged orbs or a preset) when you collapse it. While it's on, it decides by itself, and Default orb positions (with its hotkey and eye button) is ignored. |
| On-screen toggle | A small eye button on the game screen that switches Default orb positions. Drag it with Alt. |
| Prevent orb click-through | Clicking an orb or minimap button never also clicks the game world behind it. |
| Reset all positions | Tick it to put every dragged orb, the compass, the minimap, any moved preset and the side panel back where they belong. It unticks itself. |

With Orb features on, right-clicking the world map orb and choosing **Minimise minimap** hides the minimap in the layout you're in (it changes the Minimap dropdown for you). While it's hidden, the option reads **Restore minimap**.

### Orb visibility

Each orb, the XP button, the Wiki banner, the compass and the minimap has a dropdown: **Show**, **Hide in default layout**, **Hide in moved layout** or **Hide in both**. The default layout is the game's own placement (Default orb positions on, or nothing moved); the moved layout is your dragged orbs or a preset. So, for example, set the minimap to *Hide in moved layout* to have it disappear in your cut-down layout and come back with the defaults. Needs Orb features on.

## Moving the orbs

Turn on **Orb features** and **Move orbs**, then hold **Alt** and a yellow box appears around each orb, the compass and the minimap. Drag any of them anywhere. **Alt + right-click** puts one back in its normal place. With a preset layout chosen, one box covers the whole block, so it moves as a unit.

## Making RuneLite boxes follow the panel

RuneLite can attach any movable box (Boosts, for example, or one of the orbs) to the side panel:

1. Hold **Alt** and drag the box to where you want it beside the frame.
2. Release Alt, hold **Shift**, right-click the box and choose **Overlay Origin → Sidepanel**.

The box then moves down with the frame when the panel collapses and back up when it opens.

## Works with Compact Orbs

With **Orb features** off (the default), this plugin only touches the side panel, so it runs alongside [Compact Orbs](https://github.com/its-cue/compact-orbs). If Compact Orbs is running, the orb features and Hide Modern logout X stand down automatically, even if they're switched on.

## Conflicts

Turn off other plugins that rearrange the side stones or the side panel. RuneLite will offer to switch off Menu Stones Hider and Custom Game Tabs for you. **Interface Layout** and **Fixed Resizable Hybrid** also clash, but they can't be listed as conflicts without blocking Compact Orbs too, so turn those off yourself.

## Credits

- The widget state and redraw handling is adapted from [Interface Layout](https://github.com/wukuna/interface-layout) by wukuna and contributors.
- The orb movement follows the approach of [Compact Orbs](https://github.com/its-cue/compact-orbs) by its-cue.
- The status bars are adapted from RuneLite's own [Status Bars](https://github.com/runelite/runelite) plugin by Jos and Rheon.

All three are BSD 2-Clause licensed, as is this plugin (see `LICENSE`). Full credits are in `NOTICE`, and each source file with adapted code keeps its original copyright header.
