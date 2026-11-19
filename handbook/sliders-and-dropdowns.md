# Sliders and Dropdowns

This guide covers picking a number from a range with `FlixelUiSlider`, such as a volume or brightness setting, and
picking one item from a list with `FlixelUiDropdown`, such as a screen resolution or a language.

## Table of contents

1. [Sliders](#sliders)
2. [Driving a slider](#driving-a-slider)
3. [How a slider looks](#how-a-slider-looks)
4. [Dropdowns](#dropdowns)
5. [Driving a dropdown](#driving-a-dropdown)
6. [How a dropdown looks](#how-a-dropdown-looks)
7. [Common mistakes](#common-mistakes)
8. [Check your understanding](#check-your-understanding)

## Sliders

A slider is a dimmer switch: the track is the groove, the thumb is the knob, and the value is how bright the light is.

```java
// Length 200 pixels, from 0 to 1, starting at 0.8.
FlixelUiSlider volume = new FlixelUiSlider(200, 0f, 1f, 0.8f);
volume.setStep(0.05f); // Snap to multiples of 0.05.
volume.onChange.add(s -> Flixel.sound.setMasterVolume(s.getValue()));
ui.add(volume);

// A vertical fader that grows from the bottom up, 120 pixels tall.
FlixelUiSlider fader = new FlixelUiSlider(120, 0f, 100f, 50f, true);
```

`new FlixelUiSlider(length)` makes a horizontal 0 to 1 slider starting at 0. The length is the size along the
slider's axis; the other size comes from the style.

The value always stays inside the range. With a step greater than zero, it also snaps to `min + n * step`.

| Call | What it does |
|------|--------------|
| `setValue(v)` | Clamps, snaps, and fires `onChange` if the value changed. |
| `setValue(v, false)` | The same, silently. |
| `setRatio(r)` | Sets the value by position, `0` for the minimum and `1` for the maximum. |
| `stepUp()` / `stepDown()` | Nudges by one step, or by a tenth of the range when there is no step. |
| `setRange(min, max)` / `setStep(step)` | Changes the range or step; fires `onChange` if that moves the value. |
| `setVertical(...)` | Switches orientation, swapping width and height. |
| `getValue()`, `getRatio()`, `getMin()`, `getMax()`, `getStep()` | Read the current settings. |
| `isDragging()` | `true` while the thumb is being dragged. |

## Driving a slider

With a `FlixelUiPointer`, there is nothing to do: pressing the slider jumps the thumb under the pointer, moving while
held drags it (even off the track; the value just stays at the nearest end), and the thumb shows its hovered look only
while the pointer is over the thumb itself.

By hand, feed it the pointer yourself:

```java
if (Flixel.mouse.justPressed(FlixelMouseButton.LEFT) && ui.getWidgetAt(mx, my) == volume) {
  volume.pointerDown(mx, my);
}
if (Flixel.mouse.pressed(FlixelMouseButton.LEFT)) {
  volume.pointerDrag(mx, my);
}
if (Flixel.mouse.justReleased(FlixelMouseButton.LEFT)) {
  volume.pointerUp();
}
volume.pointerMove(mx, my); // Only needed for the thumb's hovered look.
```

For a keyboard or gamepad, focus the slider and call `stepUp()` and `stepDown()` from left and right.
`valueAt(x, y)` and `isThumbAt(x, y)` answer "what value is under this point?" and "is this point on the thumb?"
without changing anything.

## How a slider looks

```java
FlixelUiSliderStyle style = new FlixelUiSliderStyle();
style.track = skin.track(new FlixelUiColorFill(0.2f, 0.2f, 0.2f, 1f));
style.fill = skin.track(new FlixelUiColorFill(0.3f, 0.7f, 1f, 1f)); // Optional filled part.
style.thumb = skin.track(FlixelUiImage.load(Flixel.files.internal("ui/knob.png")));
style.thumbWidth = 16;
style.thumbHeight = 16;
style.trackThickness = 6; // 0 makes the track as thick as the slider.
skin.add("default", style);
```

`track` and `thumb` are required. The fill is drawn from the start of the slider to the middle of the thumb. The thumb
picks `thumbDisabled`, `thumbDown`, `thumbOver`, or `thumb`, in that order, each falling back to `thumb`.

## Dropdowns

A dropdown is a drawer in a filing cabinet: the front shows what is selected, and pulling it open shows the full list.
The open list floats on the display's popup layer, above everything else, including dialogs.

```java
FlixelUiDropdown resolution = new FlixelUiDropdown(200); // Field width in pixels.
resolution.setPlaceholder("Pick a resolution");          // Shown while nothing is selected.
resolution.addItem("1280 x 720");
resolution.addItem("1920 x 1080");
resolution.addItem("2560 x 1440");
resolution.setSelectedIndex(1);                          // Silent initial choice.
resolution.onSelect.add(d -> applyResolution(d.getSelectedIndex()));
ui.add(resolution);
```

| Call | What it does |
|------|--------------|
| `addItem(text)`, `insertItem(i, text)`, `removeItem(i)`, `clearItems()` | Edit the list. Text is copied, and the selection index follows its item. |
| `getItemCount()`, `getItem(i)` | Read the list. |
| `select(i)` | Selects in response to the player: closes the list and fires `onSelect` if the choice changed. |
| `setSelectedIndex(i)` | Selects silently, without closing; `-1` clears. |
| `getSelectedIndex()`, `getSelectedItem()` | Read the choice; `-1` or `null` when nothing is selected. |
| `open()`, `close()`, `toggle()`, `isOpen()` | Show or hide the list. Opening does nothing while disabled, empty, or not on a display. |
| `highlight(i)`, `highlightNext()`, `highlightPrevious()`, `selectHighlighted()` | Move a highlight through the open list and pick it, for keyboards and gamepads. |
| `scroll(rows)`, `setMaxVisibleItems(n)` | The list shows 6 rows by default and scrolls past that. |
| `getItemAt(x, y)` | The row under a point, or `-1`. |

Besides `onSelect`, a dropdown fires `onOpen` when the list opens and `onClose` whenever it closes, including after a
selection.

The list opens below the field, or above it when there is no room below.

## Driving a dropdown

With a `FlixelUiPointer`, clicking the field opens or closes the list, moving over the list highlights the row under
the pointer, clicking a row selects it, scrolling over the list scrolls it, and pressing anywhere else closes it.

By hand, the pattern is:

```java
if (Flixel.mouse.justPressed(FlixelMouseButton.LEFT)) {
  if (resolution.isOpen()) {
    int row = resolution.getItemAt(mx, my);
    if (row >= 0) {
      resolution.select(row); // Selects and closes.
    } else {
      resolution.close();     // Clicked outside the list.
    }
  } else if (ui.getWidgetAt(mx, my) == resolution) {
    resolution.open();
  }
}
if (resolution.isOpen()) {
  int row = resolution.getItemAt(mx, my);
  if (row >= 0 && row != resolution.getHighlightedIndex()) {
    resolution.highlight(row);
  }
}
```

With a keyboard or gamepad: `toggle()` to open, `highlightNext()` and `highlightPrevious()` to move,
`selectHighlighted()` to pick, and `close()` to back out.

## How a dropdown looks

```java
FlixelUiDropdownStyle style = new FlixelUiDropdownStyle();
style.up = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/dropdown.png"), 6, 6, 6, 6));
style.open = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/dropdown_open.png"), 6, 6, 6, 6));
style.arrow = skin.track(FlixelUiImage.load(Flixel.files.internal("ui/chevron.png")));
style.listBackground = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/list.png"), 4, 4, 4, 4));
style.itemHighlight = skin.track(new FlixelUiColorFill(0.2f, 0.5f, 1f, 0.5f));
style.fontSize = 14;
style.padLeft = style.padRight = 8;
style.padTop = style.padBottom = 4;
style.itemPadLeft = 8;
style.listGap = 2;
skin.add("default", style);
```

Only `up` is required. The field picks `disabled`, `open`, `down`, `over`, `focused`, or `up`, in that order, falling
back to `up`. `arrow` is drawn at the right edge of the field. In the list, `itemHighlight` marks the highlighted row and
`itemSelected` (when set) marks the selected one. `itemHeight` of `0` sizes rows from the font plus item padding. Text
colors are `fontColor`, `highlightFontColor`, `disabledFontColor`, and `placeholderFontColor`.

## Common mistakes

- **`onChange` runs while a settings screen opens.** Set saved values with `setValue(v, false)`, and use
  `setSelectedIndex(...)` for dropdowns, which never fires `onSelect`.
- **A slider stops following the mouse.** `pointerDrag(...)` only works after `pointerDown(...)`, and every drag needs
  a `pointerUp()`.
- **A dropdown does not open.** It has no items, is disabled, or has not been added to a display yet.
- **Clicks on the open list reach the widgets behind it.** Without a pointer, check `getItemAt(...)` before handling
  other clicks, as in the hand-wiring example.

## Check your understanding

1. A music volume slider should move in steps of 10 percent and show the saved value without firing `onChange`. What
   do you call?
2. Which call should a "Confirm" gamepad button make on an open dropdown?
3. Why does an open dropdown list appear above a dialog?

(Answers: `setStep(0.1f)` and `setValue(saved, false)` on a 0 to 1 slider; `selectHighlighted()`; open lists live on
the popup layer, which is drawn and hit-tested above the modal layer.)

**Next:** [Text Boxes](text-boxes.md)
