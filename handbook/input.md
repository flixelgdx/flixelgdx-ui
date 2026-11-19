# Wiring Input: Pointers, Focus, and Your Own Controls

This guide explains why the UI never reads input itself, how `FlixelUiPointer` connects a mouse, finger, or virtual
cursor to every widget in a few lines, how to wire widgets by hand when you need full control, how to drive menus
with a keyboard or gamepad, and how mouse cursors and scrolling work.

## Table of contents

1. [Why the UI never reads input](#why-the-ui-never-reads-input)
2. [The pointer: wiring for most games](#the-pointer-wiring-for-most-games)
3. [Double and triple clicks](#double-and-triple-clicks)
4. [Keeping clicks from reaching the game world](#keeping-clicks-from-reaching-the-game-world)
5. [Scrolling](#scrolling)
6. [Mouse cursors](#mouse-cursors)
7. [Keyboard and gamepad navigation](#keyboard-and-gamepad-navigation)
8. [Wiring widgets by hand](#wiring-widgets-by-hand)
9. [Common mistakes](#common-mistakes)
10. [Check your understanding](#check-your-understanding)

## Why the UI never reads input

No widget, display, or pointer ever looks at the mouse, keyboard, touch screen, or gamepad. Every interaction is a
plain method your game calls: `hover()`, `press()`, `click()`, `open()`, `select(i)`, `scroll(n)`, and so on.

That keeps your game in charge. A button can be clicked by a mouse, tapped by a finger, or chosen with a gamepad, and
your game decides which of those it supports, how they are bound, and when the UI should ignore input entirely (during
a cutscene, for example). It also means nothing in the UI fights your game's own input code.

There is one small exception: a focused text box receives typed characters and editing keys from the keyboard, since
typing would be impractical otherwise. It still only types while focused, and only your game focuses it. See
[Text Boxes](text-boxes.md).

## The pointer: wiring for most games

Calling each widget's methods by hand gets repetitive: buttons want hover, press, and a click only when the release
lands on them, sliders want to follow the cursor while held, text boxes want caret placement and drag selection, and
dropdowns and tooltips need their own care. `FlixelUiPointer` does all of it. Think of it as a universal remote: your
game reports where the cursor is and when the button goes down or up, and the pointer turns that into the right call
for whatever widget is underneath.

```java
// In create(), after the display exists:
pointer = new FlixelUiPointer(ui);

// In update():
pointer.move(Flixel.mouse.getWorldX(ui.getCamera()), Flixel.mouse.getWorldY(ui.getCamera()));
if (Flixel.mouse.justPressed(FlixelMouseButton.LEFT)) {
  pointer.down();
}
if (Flixel.mouse.justReleased(FlixelMouseButton.LEFT)) {
  pointer.up();
}
pointer.scroll(Flixel.mouse.getScrollDeltaY());
Flixel.mouse.icons.setCursor(pointer.getCursor());
```

What each call does:

| Call | What happens |
|------|--------------|
| `move(x, y)` | Finds the widget under the point, unhovers the old one, hovers the new one, and starts its tooltip countdown. Highlights the row under the point in an open dropdown list. While a slider or text box is pressed, drags it. |
| `down()` | Presses the hovered widget, hides the tooltip, blurs the focused widget if the press landed elsewhere, and closes any open dropdown list that was not pressed. A slider jumps to the point; a text box takes focus and places its caret. |
| `up()` | Releases the pressed widget and, if the pointer is still over it, activates it: buttons click, checkboxes toggle, radio buttons select, and dropdowns open or close (or select a row). |
| `scroll(amount)` | Offers the scroll to the hovered widget, then to each parent until one uses it. |
| `cancel()` | Releases the pressed widget without activating it, for a press the system took away. |
| `reset()` | Cancels the press, unhovers, and hides the tooltip. Use it when the cursor leaves the window or the UI is rebuilt. |

`down()`, `up()`, and `scroll(...)` act on the widget found by the most recent `move(...)`, so call `move(...)` first
every frame. A press that starts on one button and is released on another clicks neither, just like on a desktop.
None of the pointer's methods allocate, so calling them every frame is fine.

**Touch screens and virtual cursors.** The pointer does not care where the position comes from. Pass a finger's world
position and call `down()` and `up()` when it touches and lifts, or move a cursor sprite with a gamepad stick, pass
its position, and call `down()` and `up()` on a face button. For a touch screen, skip the cursor icon line and call
`pointer.reset()` when the finger lifts, so the last button does not stay highlighted.

**Tooltip delay.** The pointer shows a hovered widget's tooltip after `pointer.tooltipDelay` seconds (`0.4` by
default; `0` shows it at once). Tooltips do not pop up while something is held down.

## Double and triple clicks

Text boxes select a word on a double click and a line on a triple click. The pointer has no clock, so it cannot tell
those from single clicks; your game counts them and passes the count to `down(clicks)`:

```java
private float time;
private float lastPress = -1f;
private int clicks;

// In update():
time += elapsed;
if (Flixel.mouse.justPressed(FlixelMouseButton.LEFT)) {
  clicks = (lastPress >= 0f && time - lastPress < 0.3f) ? clicks + 1 : 1;
  lastPress = time;
  pointer.down(clicks);
}
```

## Keeping clicks from reaching the game world

When the UI is drawn over gameplay, a click on a button should not also fire the player's weapon. `getHovered()` is
`null` when the pointer is not over any interactive widget, so check it before treating a click as a world click:

```java
if (Flixel.mouse.justPressed(FlixelMouseButton.LEFT)) {
  pointer.down();
  if (pointer.getHovered() == null) {
    player.shoot();
  }
}
```

Labels, pictures, and plain containers are not interactive, so clicks over them still count as world clicks. Panels
and modals are interactive, so clicking the empty part of a window is swallowed by the window.

## Scrolling

`pointer.scroll(amount)` returns `true` when a widget used the scroll, so you can skip your own handling:

```java
if (!pointer.scroll(Flixel.mouse.getScrollDeltaY())) {
  zoomWorldCamera(Flixel.mouse.getScrollDeltaY());
}
```

Positive amounts scroll toward the end. An open dropdown list scrolls by rows and a multi-line text box by lines.
The sign and size of raw wheel input differ between devices, so flip or scale the value if scrolling feels backward.

## Mouse cursors

Players expect the cursor to change over things they can use. Since the UI cannot change the cursor itself, each
widget reports the cursor it wants and `pointer.getCursor()` picks the right one; your game applies it with
`Flixel.mouse.icons.setCursor(...)`. The defaults are a pointing hand over buttons, a text beam over text boxes, and an
open hand over sliders that closes while dragging. Change them in `FlixelUiCursors`:

```java
FlixelUiCursors.textBox = null;   // Text boxes keep the normal arrow.
FlixelUiCursors.enabled = false;  // No widget changes the cursor.
FlixelUiCursors.reset();          // Back to the defaults.
```

One widget can override its own cursor with `setCursor(...)` (and `setPressedCursor(...)` while held), and `null` goes
back to the default:

```java
deleteButton.setCursor(FlixelMouseCursor.NOT_ALLOWED);
```

## Keyboard and gamepad navigation

Keyboard and gamepad menus are built on focus. Your game moves focus between widgets with `focus()` and activates the
focused one with its own method. A focused button shows the style's `focused` background, so players can see where
they are.

```java
private FlixelUiWidget[] items;
private int index;

// In create(), after building the menu:
items = new FlixelUiWidget[] {playButton, optionsButton, quitButton};
items[0].focus();

// In update():
if (Flixel.keys.justPressed(FlixelKey.DOWN)) {
  index = (index + 1) % items.length;
  items[index].focus();
}
if (Flixel.keys.justPressed(FlixelKey.UP)) {
  index = (index + items.length - 1) % items.length;
  items[index].focus();
}
if (Flixel.keys.justPressed(FlixelKey.ENTER) && ui.getFocused() instanceof FlixelUiButton button) {
  button.click();
}
```

For a gamepad, read your own input actions in place of the keys. Each widget has the methods a controller needs:

| Widget | Activate | Change value |
|--------|----------|--------------|
| `FlixelUiButton` | `click()` | |
| `FlixelUiCheckbox` | `click()` or `toggle()` | `setChecked(...)` |
| `FlixelUiRadioGroup` | | `selectNext()`, `selectPrevious()` |
| `FlixelUiSlider` | | `stepUp()`, `stepDown()` |
| `FlixelUiDropdown` | `toggle()`, then `selectHighlighted()` | `highlightNext()`, `highlightPrevious()` |
| `FlixelTextBox` | `focus()` to type, `submit()` | `type(c)`, `backspace()`, ... |

`click()` alone does not show the pushed-in look. For that, call `press()` when the confirm button goes down and
`release()` followed by `click()` when it comes up.

## Wiring widgets by hand

You can skip the pointer entirely (or mix it with direct calls) when you need something it does not do. A minimal
mouse wiring looks like this:

```java
private FlixelUiWidget hovered;

@Override
public void update(float elapsed) {
  super.update(elapsed);
  float mx = Flixel.mouse.getWorldX(ui.getCamera());
  float my = Flixel.mouse.getWorldY(ui.getCamera());
  FlixelUiWidget hit = ui.getWidgetAt(mx, my);

  if (hit != hovered) {
    if (hovered != null) {
      hovered.unhover();
    }
    ui.hideTooltip();
    if (hit != null) {
      hit.hover();
      ui.showTooltip(hit, 0.4f); // Does nothing when the widget has no tooltip.
    }
    hovered = hit;
  }
  if (hit != null && Flixel.mouse.justPressed(FlixelMouseButton.LEFT)) {
    hit.press();
  }
  if (Flixel.mouse.justReleased(FlixelMouseButton.LEFT) && hit instanceof FlixelUiButton button) {
    button.release();
    button.click();
  }
}
```

`ui.getWidgetAt(x, y)` is purely geometric: it returns the topmost interactive, enabled, visible widget at the point,
or `null`. Widgets with `interactive = false` are skipped, but their children are still found, so set it on
decoration to let clicks fall through. Each widget guide shows the extra calls that widget needs by hand, such as
`pointerDown(...)` for sliders and text boxes.

## Common mistakes

- **Calling `down()` before `move(...)`.** The pointer presses whatever was under the previous position. Call
  `move(...)` first every frame.
- **Passing screen coordinates.** Use `Flixel.mouse.getWorldX(ui.getCamera())`, not the raw window position.
- **Buttons stay highlighted after a touch.** There is no hover on a touch screen; call `pointer.reset()` when the
  finger lifts.
- **Word selection never happens.** Pass a click count to `pointer.down(clicks)`; plain `down()` always counts as one.
- **The player shoots when clicking a menu button.** Check `pointer.getHovered() == null` before world clicks.

## Check your understanding

1. Why can the same `FlixelUiPointer` code serve a mouse, a touch screen, and a gamepad-driven cursor?
2. A player presses on "Delete", drags off it, and lets go over empty space. Is the button clicked?
3. How does a gamepad user activate a dropdown and choose its third item?

(Answers: the pointer only needs a position and "down" or "up" from your game, and it never reads a device itself; no,
a click needs the release to land on the pressed widget; `toggle()` to open it, `highlightNext()` until the third row
is highlighted (or `highlight(2)`), then `selectHighlighted()`.)

**Next:** [Skins, Styles, and Backgrounds](skins-and-styles.md)
