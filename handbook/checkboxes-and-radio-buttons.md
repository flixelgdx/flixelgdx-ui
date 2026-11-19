# Checkboxes and Radio Buttons

This guide covers on and off settings with `FlixelUiCheckbox`, and choices where only one option can be picked with
`FlixelUiRadioButton` and `FlixelUiRadioGroup`.

## Table of contents

1. [Checkboxes](#checkboxes)
2. [Radio buttons and groups](#radio-buttons-and-groups)
3. [Signal order](#signal-order)
4. [Loading saved values without firing signals](#loading-saved-values-without-firing-signals)
5. [How they look](#how-they-look)
6. [Common mistakes](#common-mistakes)
7. [Check your understanding](#check-your-understanding)

## Checkboxes

A checkbox is a light switch: a box on the left, a label on the right, and two positions.

```java
FlixelUiCheckbox vsync = new FlixelUiCheckbox("V-Sync", settings.vsync); // Label and starting state.
vsync.onChange.add(c -> settings.vsync = c.isChecked());
ui.add(vsync);

FlixelUiCheckbox subtitles = new FlixelUiCheckbox("Subtitles"); // Starts unchecked.
```

| Call | What it does |
|------|--------------|
| `click()` or `toggle()` | Flips the state and fires `onChange`. Ignored while disabled. |
| `setChecked(value)` | Sets the state and fires `onChange` only if it changed. |
| `setChecked(value, false)` | Sets the state silently. |
| `isChecked()` | Reads the state. |

With a `FlixelUiPointer`, a checkbox toggles when it is pressed and released under the pointer, and clicking its label
counts too, since the label is part of the widget. By hand, call `click()` when your game sees a click on it.

## Radio buttons and groups

Radio buttons work like the preset buttons on a car radio: pressing one lights it up and turns the others off, and
pressing a lit one does nothing. The group is a plain object (not a widget) that tracks which one is picked. Each
button joins its group in its constructor:

```java
FlixelUiRadioGroup difficulty = new FlixelUiRadioGroup();
FlixelUiRadioButton easy = new FlixelUiRadioButton("Easy", difficulty);
FlixelUiRadioButton normal = new FlixelUiRadioButton("Normal", difficulty);
FlixelUiRadioButton hard = new FlixelUiRadioButton("Hard", difficulty);
difficulty.setSelectedIndex(1); // "Normal" starts selected, silently.
difficulty.onChange.add(g -> settings.difficulty = g.getSelectedIndex());

FlixelUiStack column = new FlixelUiStack(FlixelUiStack.Direction.VERTICAL);
column.setSpacing(4);
column.add(easy);
column.add(normal);
column.add(hard);
ui.add(column);
```

The group does not place the buttons; put them in a stack or anywhere you like.

**On a button:** `click()` or `select()` selects it (ignored when it is disabled or already selected), `isSelected()`
reads it, and `getGroup()` returns its group.

**On the group:**

| Call | What it does |
|------|--------------|
| `select(button)` or `select(index)` | Selects a button and fires the signals. |
| `select(button, false)` | Selects silently. |
| `setSelectedIndex(index)` | Selects by index silently; `-1` clears the selection. |
| `selectNext()` / `selectPrevious()` | Moves the selection, wrapping around and skipping disabled or invisible buttons. Ideal for left and right on a gamepad. |
| `clearSelection()` | Selects nothing, silently. |
| `getSelected()` / `getSelectedIndex()` | Reads the selection; `null` or `-1` when nothing is picked. |
| `getCount()` / `getRadioAt(index)` | Walks the buttons in the order they joined. |
| `add(button)` / `remove(button)` | Moves a button into this group (out of its old one) or takes it out. |

## Signal order

When the selection changes, signals fire in this order:

1. The previously selected button's `onChange`.
2. The newly selected button's `onChange`.
3. The group's `onChange`, once.

Listen on the group when you care about the choice, and on a button when that one option needs to react (for example
to show a hint while it is selected).

## Loading saved values without firing signals

When a settings screen opens, its widgets should show the saved values, but your `onChange` listeners should not run
(they might save again, play a sound, or restart audio). Use the silent setters, then add the listeners:

```java
vsync.setChecked(save.vsync, false);
difficulty.setSelectedIndex(save.difficulty);

vsync.onChange.add(c -> save.vsync = c.isChecked());
difficulty.onChange.add(g -> save.difficulty = g.getSelectedIndex());
```

## How they look

Checkboxes use `FlixelUiCheckboxStyle` and radio buttons use `FlixelUiRadioButtonStyle`. They have the same fields
but separate skin drawers, so radio buttons can be round while checkboxes are square, both named `"default"`.

```java
FlixelUiCheckboxStyle box = new FlixelUiCheckboxStyle();
box.off = skin.track(FlixelUiImage.load(Flixel.files.internal("ui/check_off.png")));
box.on = skin.track(FlixelUiImage.load(Flixel.files.internal("ui/check_on.png")));
box.font = Flixel.files.internal("fonts/pixel.ttf");
box.fontSize = 16;
box.spacing = 8; // Gap between the box and the label.
skin.add("default", box);

FlixelUiRadioButtonStyle dot = new FlixelUiRadioButtonStyle();
dot.off = skin.track(FlixelUiImage.load(Flixel.files.internal("ui/radio_off.png")));
dot.on = skin.track(FlixelUiImage.load(Flixel.files.internal("ui/radio_on.png")));
skin.add("default", dot);
```

Only `off` and `on` are required. The box picks its look in this order: disabled, pressed, hovered, focused, plain,
each in its `on` or `off` variant (such as `onOver` or `offDisabled`), falling back to plain `on` or `off`. The box is
drawn at `boxSize` pixels, or at the image's natural size when `boxSize` is `0`. The label uses `font`, `fontSize`,
`fontColor`, and `disabledFontColor`.

## Common mistakes

- **Saving or playing sounds while a screen loads.** Use `setChecked(value, false)` and `setSelectedIndex(...)`
  before adding listeners.
- **A radio button cannot be unchecked by clicking it.** That is by design. Use `clearSelection()` if the game needs
  "nothing selected", or use a checkbox.
- **Radio buttons appear but are never exclusive.** Each button was given a different group. Share one group object.
- **"No FlixelUiRadioButtonStyle named 'default'."** A checkbox style does not cover radio buttons; add both.

## Check your understanding

1. You want a "Fullscreen" setting that is either on or off. Checkbox or radio buttons?
2. In what order do signals fire when the player switches from "Easy" to "Hard"?
3. How do you show the saved difficulty without running the group's `onChange` listener?

(Answers: a checkbox; Easy's `onChange`, then Hard's `onChange`, then the group's `onChange`; call
`difficulty.setSelectedIndex(saved)`, which is silent, or add the listener afterward.)

**Next:** [Sliders and Dropdowns](sliders-and-dropdowns.md)
