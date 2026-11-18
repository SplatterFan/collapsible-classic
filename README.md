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
| Orb layout | Ready-made compact layouts borrowed from Compact Orbs: Vertical, Horizontal and Horizontal (wide). They hide the minimap and pack the orbs into the top-right corner. Custom (the default) leaves placement to you. With Move orbs on, Alt-drag moves a whole preset; each preset remembers its own spot. Default orb positions overrides a preset while it's on. |
| Move orbs | Lets the orbs, compass and minimap be dragged while Alt is held. Off by default, for people who only want the frame. |
| Default orb positions | Puts the orbs, compass and minimap back in their normal places without forgetting where you dragged them. Switch it off to get your layout back. |
| Default positions hotkey | A key that switches Default orb positions on and off. |
| On-screen toggle | A small eye button on the game screen that does the same. Drag it with Alt. |
| Move side panel | Off by default. While on, hold Alt and drag anywhere on the side panel to move the whole thing (frame, stones and inventory) as one piece; it still collapses where it is. Alt + right-click puts it back in the corner. Switch it off once it's placed: the panel stays where it is and Alt-clicks reach the inventory again. |
| Fill collapsed gaps | With the panel closed, fills the small gaps between the stacked stones with the inventory background. |
| Show status bars | Draws Hitpoints and Prayer bars over the pillars, with icon, value, status-effect colours and a preview of what the hovered food or potion restores. Turn off RuneLite's own Status Bars plugin to avoid two sets. |
| Hide after combat | Ticks to keep the status bars showing after combat ends. 0 always shows them. |
| Prevent orb click-through | On by default. Clicking an orb or minimap button never also clicks the game world behind it. |
| Minimise minimap (world map orb right-click) | Hides the minimap the same way Hide minimap does, instead of switching to the game's minimised layout. While it's hidden, the option reads Restore minimap. |
| Reset all positions | Tick it to put every dragged orb, the compass, the minimap and any moved preset back where they belong. It unticks itself. |
| Orb visibility: Hide … | Hides individual orbs, the XP button, the Wiki banner, the compass or the minimap itself. |

## Moving the orbs

Turn on **Move orbs**, then hold **Alt** and a yellow box appears around each orb, the compass and the minimap. Drag any of them anywhere. **Alt + right-click** puts one back in its normal place. With a preset layout chosen, one box covers the whole block, so it moves as a unit.

## Making RuneLite boxes follow the panel

RuneLite can attach any movable box (Boosts, for example, or one of the orbs) to the side panel:

1. Hold **Alt** and drag the box to where you want it beside the frame.
2. Release Alt, hold **Shift**, right-click the box and choose **Overlay Origin → Sidepanel**.

The box then moves down with the frame when the panel collapses and back up when it opens.

## Conflicts

Turn off other plugins that rearrange the side stones, the side panel or the orbs, such as Interface Layout, Compact Orbs, Fixed Resizable Hybrid and Menu Stones Hider. RuneLite will offer to switch them off when you turn this plugin on.

## Credits

- The widget state and redraw handling is adapted from [Interface Layout](https://github.com/wukuna/interface-layout) by wukuna and contributors.
- The orb movement follows the approach of [Compact Orbs](https://github.com/its-cue/compact-orbs) by its-cue.
- The status bars are adapted from RuneLite's own [Status Bars](https://github.com/runelite/runelite) plugin by Jos and Rheon.

Both are BSD 2-Clause licensed, as is this plugin. See `LICENSE`.
