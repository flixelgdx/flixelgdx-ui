# Core Concepts: The Display, Widgets, and Layout

This guide explains how a FlixelGDX UI screen is put together: the display and its layers, the widget tree, the
coordinate space, how widgets are placed with anchors, percent sizes, and stacks, and how widgets are hidden, shown,
and destroyed.

## Table of contents

1. [The display](#the-display)
2. [Layers: why dialogs and dropdowns float on top](#layers-why-dialogs-and-dropdowns-float-on-top)
3. [The widget tree](#the-widget-tree)
4. [Coordinates](#coordinates)
5. [Layout: anchors, percent sizes, and stacks](#layout-anchors-percent-sizes-and-stacks)
6. [Padding and clipping](#padding-and-clipping)
7. [Preferred size and auto-size](#preferred-size-and-auto-size)
8. [Showing, hiding, fading, and destroying](#showing-hiding-fading-and-destroying)
9. [Interaction states and signals](#interaction-states-and-signals)
10. [Common mistakes](#common-mistakes)
11. [Check your understanding](#check-your-understanding)

## The display

`FlixelUiDisplay` is the one object your state adds to show UI. It owns every widget, lays them out, draws them on one
camera, and answers the question "which widget is at this point?".

```java
ui = new FlixelUiDisplay(FlixelUiDisplay.createHudCamera(), skin);
add(ui);
```

The display draws only during its own camera's pass, so a dedicated transparent camera from `createHudCamera()` keeps
the UI on top of the world and unaffected by world camera movement. That camera is not a default draw target, so your
world objects are not drawn on it a second time. There is also a constructor that takes only a camera and starts with
an empty skin; widgets that need a style then throw until you add styles to `ui.getSkin()` or call `setSkin(...)`.

`ui.add(widget)` and `ui.remove(widget)` add to and remove from the main layer. Destroying the display (which a state
does for you when the state is destroyed) destroys every widget on it.

## Layers: why dialogs and dropdowns float on top

The display has four layers, drawn back to front:

| Layer | What lives there | How things get there |
|-------|------------------|----------------------|
| Root | Almost every widget. | `ui.add(...)`. Read it with `ui.getRoot()`. |
| Modal | Dialogs, each with a dimmed backdrop. | `ui.openModal(...)`. See [Panels, Modals, and Tooltips](panels-modals-tooltips.md). |
| Popup | Open dropdown lists. | A dropdown puts its list there when it opens. |
| Tooltip | The one shared tooltip bubble. | `ui.showTooltip(...)` or the pointer. |

Hit-testing follows the same order from the top: an open dropdown list wins first; then, while a modal is open, only
the top modal can be hit, so the screen behind it is blocked; otherwise the root layer is searched.

## The widget tree

Every piece of UI is a `FlixelUiWidget`. Widgets that hold other widgets are `FlixelUiContainer`s, and containers can
hold containers, so a screen is a tree:

```text
root
+-- FlixelUiPanel (a window)
    +-- FlixelUiStack (a column)
        +-- FlixelUiLabel
        +-- FlixelUiButton
        +-- FlixelUiButton
```

- `container.add(widget)` adds a child on top of the others. A widget that already has a parent moves to the new one.
- `container.remove(widget)` takes a child out without destroying it, so it can be added somewhere else later.
- `container.clear()` removes every child, again without destroying them.
- `getChildAt(i)`, `getChildCount()`, `getParent()`, and `getDisplay()` let you walk the tree.

Children draw in the order they were added, so the last child is on top, and hit-testing checks them in the opposite
order. Widgets do not extend `FlixelObject` or `FlixelSprite`. They implement the framework's small interfaces
(`IFlixelBasic`, `FlixelPositional`, and `FlixelColorable`), so tools that work on those, such as tweens, work on
widgets too.

## Coordinates

The UI is laid out in its camera's view space: `(0, 0)` is the top-left corner of what the camera shows, x grows to the
right, and y grows downward. The root layer is exactly as big as the visible area. This is the same space that
`Flixel.mouse.getWorldX(ui.getCamera())` and `getWorldY(ui.getCamera())` report, so a mouse position can be compared
with widgets directly.

Each widget has two positions:

- **Local:** `getX()` and `getY()` are measured from the parent's content corner (its top-left plus its padding).
- **Screen:** `getScreenX()` and `getScreenY()` are absolute positions in the camera's view space, cached after each
  layout pass.

Layout runs lazily. Changing a widget marks the layout as out of date, and the next update, draw, or
`getWidgetAt(...)` call recomputes it once. Call `ui.layout()` to force it right away, for example before reading
`getScreenX()` on a widget you just added. When the camera's visible size changes (a window resize or a zoom), the
root resizes and everything anchored or percent-sized follows.

## Layout: anchors, percent sizes, and stacks

There are exactly two layout tools, and they cover almost every menu: **anchors** pin a widget to a spot in its parent,
and **stacks** line widgets up in a row or column.

### Anchors

An anchor is one of the nine `FlixelAlign` positions of the parent (`TOP_LEFT`, `TOP`, `TOP_RIGHT`, `LEFT`,
`CENTER`, `RIGHT`, `BOTTOM_LEFT`, `BOTTOM`, `BOTTOM_RIGHT`) plus an offset in pixels:

```java
score.anchor(FlixelAlign.TOP_RIGHT, -8, 8);   // 8 pixels in from the top-right corner.
pauseButton.anchor(FlixelAlign.TOP_LEFT, 8, 8);
title.anchor(FlixelAlign.TOP, 0, 24);          // Centered horizontally, 24 pixels down.
```

Because the anchor is relative to the parent, the widget stays in its corner no matter how big the parent or window
gets. While a widget is anchored, the layout owns its `x` and `y`; move it with `setAnchorOffsetX(...)` and
`setAnchorOffsetY(...)` instead (these are also what to tween to slide an anchored widget). `clearAnchor()` stops
anchoring and keeps the current position. A widget with no anchor uses its own `setPosition(x, y)` as an offset from
the parent's content corner.

### Percent sizes

`setPercentSize(width, height)` sizes a widget as a fraction of its parent's content area, on every layout pass. Pass
`Float.NaN` for an axis to keep it fixed:

```java
FlixelUiContainer bottomBar = new FlixelUiContainer(0, 48);
bottomBar.setPercentSize(1f, Float.NaN);       // Full width, fixed 48 pixel height.
bottomBar.anchor(FlixelAlign.BOTTOM, 0, 0);
ui.add(bottomBar);
```

### Stacks

A `FlixelUiStack` places its children one after another, in a column (`Direction.VERTICAL`, top to bottom) or a row
(`Direction.HORIZONTAL`, left to right):

```java
FlixelUiStack column = new FlixelUiStack(FlixelUiStack.Direction.VERTICAL);
column.setSpacing(8);                    // Gap between neighbors.
column.setPadding(16);                   // Space around the content.
column.setAlign(FlixelAlign.CENTER);     // Line children up across the column.
column.add(title);
column.add(playButton);
column.add(quitButton);
column.anchor(FlixelAlign.CENTER, 0, 0); // The whole column sits in the middle.
ui.add(column);
```

- `setAlign(...)` only reads the cross axis: `LEFT`, `CENTER`, or `RIGHT` for a column, `TOP`, `CENTER`, or `BOTTOM`
  for a row. A column starts left-aligned and a row starts top-aligned.
- Each child gets its preferred size. Children's anchors are ignored inside a stack, and so is a percent size along
  the stacking direction. A percent size across it still works, so `child.setPercentSize(1f, Float.NaN)` stretches a
  child across a column.
- A new stack **auto-sizes**: it is always exactly as big as its children plus spacing and padding, which is what lets
  stacks nest inside stacks. Setting a size yourself (`setWidth`, `setHeight`, `setSize`, or a percent size) turns
  auto-size off for that axis; `setAutoSize(true)` turns it back on.

Rows inside a column make most forms:

```java
FlixelUiStack row = new FlixelUiStack(FlixelUiStack.Direction.HORIZONTAL);
row.setSpacing(8);
row.setAlign(FlixelAlign.CENTER);
row.add(new FlixelUiLabel("Volume"));
row.add(volumeSlider);
column.add(row);
```

## Padding and clipping

`setPadding(all)` or `setPadding(left, top, right, bottom)` keeps a container's children away from its edges. The
rectangle inside the padding is the **content area**, and that is what anchors and percent sizes are measured against.
Panels take their padding from their style until you set your own.

By default, a child that sticks out of its container is drawn in full. `setClipChildren(true)` cuts children off at
the content area, both when drawing and when hit-testing, like a window frame hiding whatever is past its edges.
That is the building block for a scrolling list: move the rows, and the container hides the ones outside it. Clips
are always axis-aligned and nest by overlapping, up to `FlixelUiDisplay.MAX_CLIP_DEPTH` deep.

## Preferred size and auto-size

Labels, buttons, checkboxes, radio buttons, and pictures size themselves to their content by default (a button is its
padding plus its text and icon, never smaller than its background's minimum size). Giving one a size with
`setSize(...)`, `setWidth(...)`, or `setHeight(...)` fixes that side, and the content stays aligned inside it. Each of
these widgets has `setAutoSize(...)` to switch back. `getPreferredWidth()` and `getPreferredHeight()` report what a
widget would like to be, which is what stacks use.

## Showing, hiding, fading, and destroying

| Call | Effect |
|------|--------|
| `setVisible(false)` | Not drawn and cannot be hit, but keeps its space in a stack. |
| `kill()` / `revive()` | Stops existing: not drawn, not hit, and takes no space in a stack. |
| `setEnabled(false)` | Drawn with its disabled look, ignores every interaction call, and cannot be hit. |
| `setAlpha(a)` | Fades the widget and everything inside it, since alpha multiplies down the tree. |
| `getColor().set(r, g, b, a)` | Tints the widget. |
| `setAngle(degrees)` | Rotates the visuals only. Hit-testing and clipping still use the unrotated rectangle. |
| `destroy()` | Removes all signal listeners, leaves the parent, and is gone for good. Containers destroy their children. |

Use `kill()` or `setVisible(false)` for things that come back, and `destroy()` only when you are done. Destroy text
boxes in particular when you remove them for good, since they listen to the keyboard (see
[Text Boxes](text-boxes.md)).

## Interaction states and signals

Every widget has four interaction states, each set by a method your game (or the pointer) calls:

| State | Set with | Cleared with | Signals |
|-------|----------|--------------|---------|
| Hovered | `hover()` | `unhover()` | `onHover`, `onUnhover` |
| Pressed | `press()` | `release()` | `onPress`, `onRelease` |
| Focused | `focus()` | `blur()` | `onFocus`, `onBlur` |
| Enabled | `setEnabled(true)` | `setEnabled(false)` | none |

Each method fires its signal only when the state actually changes, so calling `hover()` every frame fires `onHover`
once. Calls on a disabled widget are ignored. Only one widget per display holds focus; focusing another one blurs the
first, and `ui.getFocused()` tells you which one has it. Signals pass the widget itself, so one listener can serve many
widgets:

```java
button.onHover.add(w -> Flixel.sound.play(Flixel.files.internal("sfx/hover.ogg")));
```

Widget-specific signals such as a button's `onClick` or a slider's `onChange` are covered in each widget's guide.

## Common mistakes

- **Moving an anchored widget with `setPosition(...)`.** The layout overwrites it. Change the anchor offsets instead.
- **Anchoring a child inside a stack.** Stacks ignore anchors. Use `setAlign(...)` on the stack.
- **A stack refuses to grow after `setWidth(...)`.** Setting a size turns auto-size off. Call `setAutoSize(true)`.
- **Reading `getScreenX()` right after `add(...)` and getting `0`.** Layout has not run yet. Call `ui.layout()` first.
- **Rotated buttons are hard to click.** Rotation is visual only; hit-testing uses the unrotated rectangle.

## Check your understanding

1. A pause button should always sit 12 pixels from the top-right corner, even when the window resizes. What do you
   call?
2. What is the difference between `kill()` and `setVisible(false)` inside a vertical stack?
3. Why does a click inside an open dialog never reach the button behind it?

(Answers: `pauseButton.anchor(FlixelAlign.TOP_RIGHT, -12, 12)`; a killed widget takes no space, while an invisible
one keeps its gap; while a modal is open, hit-testing only searches the top modal, and empty space inside it returns
the modal itself.)

**Next:** [Wiring Input](input.md)
