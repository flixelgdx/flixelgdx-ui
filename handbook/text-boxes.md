# Text Boxes

This guide covers `FlixelTextBox`: single-line and multi-line text entry, focus, filters, length limits, password
masks, keyboard shortcuts, mouse selection, the clipboard, and the editing model underneath.

## Table of contents

1. [Creating a text box](#creating-a-text-box)
2. [Focus decides where typing goes](#focus-decides-where-typing-goes)
3. [Reading and changing the text](#reading-and-changing-the-text)
4. [Filters, limits, and passwords](#filters-limits-and-passwords)
5. [Multi-line text](#multi-line-text)
6. [Keyboard shortcuts](#keyboard-shortcuts)
7. [Mouse and touch selection](#mouse-and-touch-selection)
8. [Editing from code](#editing-from-code)
9. [How a text box looks](#how-a-text-box-looks)
10. [The editing model](#the-editing-model)
11. [Common mistakes](#common-mistakes)
12. [Check your understanding](#check-your-understanding)

## Creating a text box

```java
FlixelTextBox name = new FlixelTextBox(220); // Width; height comes from the style's font and padding.
name.setPlaceholder("Player name");          // Shown in its own color while the box is empty.
name.setMaxLength(16);
name.setFilter(FlixelTextFilter.ALPHANUMERIC);
name.onSubmit.add(t -> save.playerName = t.getText().toString());
ui.add(name);
```

Text boxes are in the `org.flixelgdx.ui.text` package. Create them once the game is running (for example in a state's
`create()`), because each box connects to the active keyboard when it is constructed.

## Focus decides where typing goes

Think of a text box as a typewriter: you roll paper into it to start typing, and the caret shows where the next
letter goes. Typing is the one place the UI listens on its own: every text box listens to the keyboard, but only the
**focused, enabled** box reacts. Your game decides which box is focused, so it stays in charge of where typing goes.

```java
name.focus(); // Starts text input. Phones may show their on-screen keyboard.
name.blur();  // Stops it.
```

Focusing a text box asks the platform to start text input, which is also what lets input methods for languages such
as Japanese and Chinese work; the box tells the platform where its caret is so their candidate windows appear next to
it. Blurring stops text input. Only one widget per display is focused, so focusing a button blurs the text box.

With a `FlixelUiPointer`, pressing a text box focuses it and places the caret, and pressing anywhere else blurs it.
By hand:

```java
if (Flixel.mouse.justPressed(FlixelMouseButton.LEFT)) {
  if (ui.getWidgetAt(mx, my) == name) {
    name.focus();
    name.setCaret(name.getIndexAt(mx, my));
  } else if (name.isFocused()) {
    name.blur();
  }
}
```

Because every text box listens to the keyboard until it is destroyed, destroy a text box you remove for good.
Destroying its display or any container it is in destroys it too, and a state destroys its display when the state
ends.

## Reading and changing the text

- `getText()` returns the live text. Call `toString()` only when you need a `String`, such as when saving.
- `setText(text)` replaces it, moves the caret to the end, and fires `onChange`.
- `onChange` fires whenever the content changes, by typing or by code.
- `onSubmit` fires when the player presses Enter in a single-line box, or when your game calls `submit()` (for example
  from an "OK" button or a gamepad confirm).

## Filters, limits, and passwords

A filter is a bouncer at the door: every typed or pasted character must pass it.

| Filter | Lets in |
|--------|---------|
| `FlixelTextFilter.ANY` | Everything (the same as no filter). |
| `FlixelTextFilter.DIGITS` | `0` to `9`. |
| `FlixelTextFilter.DECIMAL` | Digits, `.`, and `-`. It does not check their placement, so validate the full string yourself. |
| `FlixelTextFilter.ALPHANUMERIC` | Letters and digits in any language, plus spaces. |
| `FlixelTextFilter.NO_SPACES` | Everything except spaces. |

A filter is a one-method interface, so a lambda covers anything else, and filters combine with `and(...)`,
`or(...)`, and `negate()`:

```java
FlixelTextBox hex = new FlixelTextBox(120);
hex.setFilter(FlixelTextFilter.DIGITS.or(c -> (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')));

FlixelTextBox pin = new FlixelTextBox(120);
pin.setFilter(FlixelTextFilter.DIGITS);
pin.setMaxLength(6);
pin.setPasswordChar('*'); // Shows one * per character; the real text is still stored.
```

Control characters never reach a filter; the box drops them first. `setMaxLength(0)` means no limit, and
`setPasswordChar((char) 0)` turns the mask off. `setFilter(null)` accepts everything again.

## Multi-line text

```java
FlixelTextBox notes = new FlixelTextBox(300, 120);
notes.setMultiLine(true); // Enter inserts a new line instead of submitting.
```

A height taller than one line gives the box room to scroll vertically, and the view follows the caret. The pointer
scrolls a multi-line box by lines; by hand, call `notes.scroll(lines)`. A single-line box scrolls sideways to follow
the caret instead and passes wheel scrolling on to its parent.

## Keyboard shortcuts

While focused, a text box handles these keys by itself, with key repeat:

| Keys | Action |
|------|--------|
| Typing | Inserts characters, replacing any selection. |
| Backspace / Delete | Deletes the character before or after the caret, or the selection. |
| Ctrl+Backspace / Ctrl+Delete | Deletes to the previous or next word boundary. |
| Left / Right | Moves by one character. With Ctrl, by one word. |
| Up / Down | Moves by one line in a multi-line box. |
| Home / End | Jumps to the start or end of the line. |
| Shift with any movement key | Extends the selection instead of moving. |
| Enter | Submits a single-line box; inserts a new line in a multi-line box. |
| Ctrl+A / Ctrl+C / Ctrl+X / Ctrl+V | Select all, copy, cut, paste. |

The box does not handle Escape or Tab, so those are free for your game, for example to blur the box or move focus to
the next field.

## Mouse and touch selection

A press places the caret, dragging extends the selection, two quick presses select a word, and three select the line
(or all the text in a single-line box). With a `FlixelUiPointer`, this works out of the box once you pass a click count
to `pointer.down(clicks)` (see [Wiring Input](input.md#double-and-triple-clicks)). By hand:

```java
if (Flixel.mouse.justPressed(FlixelMouseButton.LEFT) && ui.getWidgetAt(mx, my) == name) {
  name.pointerDown(mx, my, clicks); // Focuses the box, then places the caret or selects.
}
if (Flixel.mouse.pressed(FlixelMouseButton.LEFT)) {
  name.pointerDrag(mx, my);
}
if (Flixel.mouse.justReleased(FlixelMouseButton.LEFT)) {
  name.pointerUp();
}
```

## Editing from code

Every edit the keyboard makes is also a public method, so an on-screen keyboard for gamepads or a cheat console can
drive a box directly:

- **Typing:** `type(c)`, `insert(text)`, `backspace()`, `deleteForward()`, `deleteWord(direction)`. Each returns
  `true` when the text changed.
- **Caret:** `setCaret(index)`, `moveCaret(delta)`, `moveCaret(delta, extend)`, `moveCaretWord(direction, extend)`,
  `moveCaretLine(lines, extend)`, `caretHome(extend)`, `caretEnd(extend)`.
- **Selection:** `select(start, end)`, `selectAll()`, `selectLineAt(index)`, `extendSelection(index)`,
  `hasSelection()`, `getSelectionStart()`, `getSelectionEnd()`, `getCaret()`.
- **Clipboard:** `copy()`, `cut()`, and `paste()`. Paste arrives asynchronously and is only inserted if the box is
  still focused when it arrives. On platforms without a clipboard these do nothing.
- **Hit-testing:** `getIndexAt(x, y)` maps a point to a character index.

Indices count UTF-16 code units, and the caret always steps over emoji and other surrogate pairs as one character.

## How a text box looks

```java
FlixelTextBoxStyle style = new FlixelTextBoxStyle();
style.up = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/input.png"), 4, 4, 4, 4));
style.focused = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/input_focused.png"), 4, 4, 4, 4));
style.selection = skin.track(new FlixelUiColorFill(0.2f, 0.5f, 1f, 0.5f));
style.caret = skin.track(new FlixelUiColorFill(FlixelColor.WHITE));
style.caretWidth = 2f;
style.caretBlinkRate = 0.53f; // Seconds per half blink; 0 keeps the caret solid.
style.fontSize = 14;
style.placeholderFontColor = new FlixelColor(0.6f, 0.6f, 0.6f, 1f);
style.padLeft = style.padRight = 6;
style.padTop = style.padBottom = 4;
skin.add("default", style);
```

Only `up` is required. `over`, `focused`, and `disabled` fall back to `up`, and the placeholder and disabled colors
fall back to `fontColor`. `selection` is drawn behind selected text, one rectangle per line, and `caret` at the
insertion point while focused.

## The editing model

The text, caret, and selection live in a `FlixelTextModel`, which draws nothing. It knows what characters are on the
page and where the print head sits, but nothing about ink or paper. You can use it on its own to build a custom text
widget, or to test editing logic without a display:

```java
FlixelTextModel model = new FlixelTextModel();
model.setMaxLength(16);
model.setFilter(FlixelTextFilter.ALPHANUMERIC);
model.type('H');
model.type('i');
model.selectAll();
model.type('!'); // Replaces the selection; the text is now "!".
```

Its `version` field goes up every time the text changes (but not when only the caret moves), so a widget can tell
cheaply whether it needs to redraw.

## Common mistakes

- **Typing goes nowhere.** No text box is focused. Call `focus()`, or press the box with a pointer.
- **Two boxes type at once.** They are on different displays, each with its own focus. Blur one when focusing the
  other.
- **A removed text box still reacts to keys.** It was removed but never destroyed. Call `destroy()` on it.
- **`getText().toString()` every frame.** That creates a new string each time. Read `getText()` directly, or convert
  only when saving.
- **Enter does not submit a notes box.** Multi-line boxes insert a new line on Enter. Give the screen an OK button that
  calls `submit()`.

## Check your understanding

1. Why does a text box listen to the keyboard on its own, and what stops every box from typing at once?
2. A code field should accept only uppercase letters, at most 8 of them. What do you set?
3. How does a gamepad on-screen keyboard put letters into a box?

(Answers: typing would be impractical otherwise, and only the focused, enabled box reacts; `setFilter(c -> c >= 'A'
&& c <= 'Z')` and `setMaxLength(8)`; it calls `type(c)`, `backspace()`, and `submit()` on the box.)

**Next:** [Panels, Modals, and Tooltips](panels-modals-tooltips.md)
