<div align="center">

# FlixelGDX UI

[![CI](https://github.com/flixelgdx/flixelgdx-ui/actions/workflows/ci_build.yml/badge.svg)](https://github.com/flixelgdx/flixelgdx-ui/actions/workflows/ci_build.yml)
[![Maven Central](https://img.shields.io/maven-central/v/org.flixelgdx/flixelgdx-ui)](https://central.sonatype.com/artifact/org.flixelgdx/flixelgdx-ui)
[![JitPack](https://jitpack.io/v/flixelgdx/flixelgdx-ui.svg)](https://jitpack.io/#flixelgdx/flixelgdx-ui)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![FlixelGDX 0.6.3](https://img.shields.io/badge/FlixelGDX-0.6.3-red)](https://github.com/flixelgdx/flixelgdx)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-orange)](https://adoptium.net/temurin/releases?version=17&os=any&arch=any)

</div>

FlixelGDX UI is the official widget toolkit extension for [FlixelGDX](https://github.com/flixelgdx/flixelgdx).
It gives you panels, modals, buttons, checkboxes, radio buttons, dropdowns, text boxes, and tooltips —
everything a typical in-game menu or HUD needs — without tying your input handling to any particular device
or control scheme.

---

## The core idea: you wire the input

**The UI never reads input itself.** No widget polls the mouse, keyboard, touches, or a gamepad. Every
interaction — hovering a button, pressing it, opening a dropdown — is a plain method *you* call. The one
exception is typing: a text box listens to the keyboard on its own so you don't have to forward every key, but
it only types while it is focused, and only you decide when it is focused.

Think of a widget as a puppet on a stage. The puppet has joints that bend (hovered, pressed, focused,
disabled) and visibly reacts when they move, but it never moves on its own. Your game code is the puppeteer:
it reads whatever input system it wants, decides what happened, and pulls a string by calling `hover()`,
`press()`, `click()`, or whichever method fits. That means you decide whether a gamepad "accept" action and a
mouse click both trigger the same button, or whether they do completely different things. The widget does not
care; it only reacts to what you tell it.

This also means the extension ships no built-in skin, no built-in fonts, and no hard-coded key mappings. You
bring your own art and your own input logic. The extension connects them.

---

## Installation

```gradle
dependencies {
    implementation "org.flixelgdx:flixelgdx-ui:<version>"
}
```

Replace `<version>` with the latest release from
[Releases](https://github.com/flixelgdx/flixelgdx-ui/releases), or use a
[JitPack](https://jitpack.io/#flixelgdx/flixelgdx-ui) coordinate to pin a specific branch or commit.

To build against a local checkout of this extension (or the framework itself), follow
**[COMPILING.md](COMPILING.md)**.

---

## Quick start

### 1. Preload images (browser backend)

On the browser backend, images are decoded asynchronously. `FlixelNineSlice.load(...)` and
`FlixelUiImage.load(...)` require the file to be fully decoded before they are called. Preload all UI images
in a loading state and build the skin only after loading completes:

```java
// In a loading state's create():
Flixel.assets.load(Flixel.files.internal("ui/button.png"));
Flixel.assets.load(Flixel.files.internal("ui/panel.png"));
// ... queue every UI image ...

// In the same state's update():
if (Flixel.assets.update()) {
    Flixel.switchState(new MenuState()); // All assets decoded; safe to build the skin.
}
```

On desktop and other synchronous backends the load call returns immediately and no special loading state is
needed, but writing the same pattern makes your game portable.

### 2. Build a skin

A `FlixelUiSkin` holds named style objects, one per widget type. Create it once (per state, or once for the
whole game if you reuse it):

```java
FlixelUiSkin skin = new FlixelUiSkin();

// Load a nine-slice image and hand ownership to the skin so it is destroyed with it.
FlixelNineSlice panelBg = FlixelNineSlice.load(Flixel.files.internal("ui/panel.png"), 8, 8, 8, 8);
skin.track(panelBg);

FlixelUiPanelStyle panelStyle = new FlixelUiPanelStyle();
panelStyle.background = panelBg;
panelStyle.padLeft = panelStyle.padTop = panelStyle.padRight = panelStyle.padBottom = 12;
skin.add("default", panelStyle);

FlixelUiButtonStyle btnStyle = new FlixelUiButtonStyle();
btnStyle.up   = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/btn.png"),    6, 6, 6, 6));
btnStyle.over = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/btn_over.png"), 6, 6, 6, 6));
btnStyle.down = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/btn_down.png"), 6, 6, 6, 6));
btnStyle.font = Flixel.files.internal("fonts/pixel.ttf");
btnStyle.fontSize = 16;
btnStyle.padLeft = btnStyle.padRight = 12;
btnStyle.padTop  = btnStyle.padBottom = 6;
skin.add("default", btnStyle);
```

### 2. Create the display

```java
FlixelUiSkin skin = buildSkin(); // As above.

// createHudCamera() adds a transparent camera on top of every existing one.
// Call it in every state's create(); switching states resets the camera list.
FlixelUiDisplay ui = new FlixelUiDisplay(FlixelUiDisplay.createHudCamera(), skin);
add(ui); // add() is FlixelState.add().
```

### 3. Add widgets

```java
FlixelUiStack column = new FlixelUiStack(FlixelUiStack.Direction.VERTICAL);
column.setSpacing(8);
column.anchor(FlixelAlign.CENTER, 0, 0); // Centered on screen.

column.add(new FlixelUiLabel("Main Menu"));
column.add(new FlixelUiButton("Play",  b -> Flixel.switchState(new PlayState())));
column.add(new FlixelUiButton("Quit",  b -> Flixel.app.exit()));

ui.add(column);
```

### 4. Wire input

The display provides `getWidgetAt(x, y)`, which returns the topmost interactive widget at a point in the HUD
camera's view space. Call it from your state's `update()`:

```java
FlixelUiWidget hovered;

@Override
public void update(float elapsed) {
    super.update(elapsed);
    float mx = Flixel.mouse.getWorldX(ui.getCamera());
    float my = Flixel.mouse.getWorldY(ui.getCamera());
    FlixelUiWidget hit = ui.getWidgetAt(mx, my);

    if (hit != hovered) {
        if (hovered != null) hovered.unhover();
        if (hit != null)     hit.hover();
        hovered = hit;
    }
    if (Flixel.mouse.justPressed(FlixelMouseButton.LEFT)  && hit != null) hit.press();
    if (Flixel.mouse.justReleased(FlixelMouseButton.LEFT) && hit != null) {
        hit.release();
        if (hit instanceof FlixelUiButton b) b.click();
        // Handle other widget types (checkbox, dropdown, ...) the same way.
    }
}
```

---

## Wiring input

### Mouse

The minimal mouse loop above covers hover, press, release, and click. A few things to keep in mind:

- **Track the hovered widget yourself.** The display does not track it. When the cursor moves off a widget,
  your code is responsible for calling `unhover()`.
- **Coordinate space.** `Flixel.mouse.getWorldX(ui.getCamera())` returns a position in the HUD camera's view
  space, which is exactly what `getWidgetAt(x, y)` expects. Do not use raw screen pixels.
- **One call fires the signal once.** Calling `hover()` when the widget is already hovered is a no-op. The
  `onHover` signal fires only when the state *changes* from not-hovered to hovered.

### Keyboard and gamepad focus

Focus is the widget that keyboard or gamepad input is directed to (for example, a focused button that can be
"pressed" with a gamepad A). The display tracks a single focused widget at a time in `getFocused()`.

```java
// Move focus with arrow keys or d-pad.
if (Flixel.keys.justPressed(Keys.DOWN) || controls.dpadDown.justPressed()) {
    FlixelUiWidget focused = ui.getFocused();
    if (focused == null) {
        firstButton.focus();
    } else if (focused == firstButton) {
        firstButton.blur();
        secondButton.focus();
    }
}

// "Accept" action triggers the focused button.
if (controls.accept.justPressed()) {
    if (ui.getFocused() instanceof FlixelUiButton b) b.click();
    if (ui.getFocused() instanceof FlixelUiCheckbox cb) cb.click();
}
```

Calling `focus()` on a widget automatically calls `blur()` on whatever was focused before, so you never need
to un-focus the old widget manually.

### Text box keyboard listener

`FlixelTextBox` implements `FlixelKeyboardListener` and registers itself with `Flixel.input` when it is
constructed, then removes itself in `destroy()`. There is nothing to register by hand. Every text box hears
every key, but only the focused (and enabled) one acts on it, and only the game decides which box is focused.

Since it registers itself, destroy a text box when you are done with it. Destroying its display, or any
container it sits in, does that for you, and a state destroys its display when the state is destroyed.

The text box handles printable characters in `keyTyped()` and editing keys (backspace, delete, arrows,
home/end, enter, ctrl+A/C/V/X) in `keyDown()`. `focus()` calls `Flixel.input.startTextInput()` for IME
support; `blur()` calls `stopTextInput()`.

---

## Widgets

### Panel, stack, and anchors

`FlixelUiPanel` is a container with a background (typically a nine-slice). `FlixelUiStack` lays out its
children one after another in a column (`VERTICAL`) or a row (`HORIZONTAL`). Most other widgets just sit where
you anchor them.

```java
// A centered 320x240 panel with a column of widgets inside.
FlixelUiPanel panel = new FlixelUiPanel(320, 240);
panel.anchor(FlixelAlign.CENTER, 0, 0);

FlixelUiStack column = new FlixelUiStack(FlixelUiStack.Direction.VERTICAL);
column.setSpacing(8);
column.setPercentSize(1f, Float.NaN); // Full panel width, auto height.
panel.add(column);

column.add(new FlixelUiLabel("Settings"));
column.add(vsyncCheckbox);
column.add(new FlixelUiButton("Back", b -> closePauseMenu()));
ui.add(panel);
```

**Anchors.** `widget.anchor(FlixelAlign.BOTTOM_RIGHT, -16, -16)` places the widget 16 pixels in from the
bottom-right corner. The first argument is any `FlixelAlign` constant (`TOP_LEFT`, `TOP`, `TOP_RIGHT`,
`LEFT`, `CENTER`, `RIGHT`, `BOTTOM_LEFT`, `BOTTOM`, `BOTTOM_RIGHT`). The offsets move the widget in from the
chosen corner; negative values move away from the edge.

**Percent sizes.** `widget.setPercentSize(1f, 0.5f)` makes the widget fill 100% of its parent's content width
and 50% of its content height. Pass `Float.NaN` on an axis to leave that axis fixed.

### Label

`FlixelUiLabel` displays a read-only piece of text. It is not interactive by default, so hit-testing passes
straight through it.

```java
FlixelUiLabel score = new FlixelUiLabel("Score: 0");
score.anchor(FlixelAlign.TOP_RIGHT, -8, 8);
ui.add(score);

// Update without allocating a new string every frame.
scoreText.set("Score: ").concat(points); // scoreText is a FlixelString field in your state.
score.setText(scoreText);

// Wrapping and alignment.
FlixelUiLabel hint = new FlixelUiLabel("Press any key to continue.");
hint.setWrapWidth(200);
hint.setAlignment(FlixelText.Alignment.CENTER);
```

The label auto-sizes to its text by default. Calling `setWidth(float)`, `setHeight(float)`, or
`setSize(float, float)` fixes one or both axes.

### Button and floating action button

```java
// Text button.
FlixelUiButton play = new FlixelUiButton("Play", b -> Flixel.switchState(new PlayState()));
play.anchor(FlixelAlign.CENTER, 0, 0);
ui.add(play);

// Icon-only button.
FlixelUiButton back = new FlixelUiButton(Flixel.files.internal("ui/arrow_back.png"), b -> goBack());
ui.add(back);

// Text + icon.
FlixelUiButton save = new FlixelUiButton("Save", Flixel.files.internal("ui/save.png"), null);
save.onClick.add(b -> saveGame());
ui.add(save);
```

A **floating action button** (a round "+" button in a corner) is just a button wearing a round style and
anchored to a corner:

```java
// Add a "fab" style to the skin once.
FlixelUiButtonStyle fabStyle = new FlixelUiButtonStyle();
fabStyle.up = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/fab.png"), 24, 24, 24, 24));
fabStyle.padLeft = fabStyle.padTop = fabStyle.padRight = fabStyle.padBottom = 16;
skin.add("fab", fabStyle);

// Use it.
FlixelUiButton fab = new FlixelUiButton(Flixel.files.internal("ui/plus.png"), b -> openCreate());
fab.setStyle("fab");
fab.anchor(FlixelAlign.BOTTOM_RIGHT, -24, -24);
ui.add(fab);
```

**Wiring a button.** Call `press()` on mouse/pointer down, `release()` on pointer up, and `click()` to
actually fire `onClick`. Separating press from click lets you cancel the click if the pointer moved away:

```java
if (Flixel.mouse.justReleased(FlixelMouseButton.LEFT)) {
    if (hovered instanceof FlixelUiButton b) {
        b.release();
        b.click(); // Only fire onClick when the pointer is still on the button.
    } else if (pressed instanceof FlixelUiButton b) {
        b.release(); // Pointer moved away; just un-press, no click.
    }
}
```

### Checkbox

`FlixelUiCheckbox` draws a box on the left and a label to its right. `click()` toggles the checked state and
fires `onChange`. `setChecked(boolean)` sets it directly.

```java
FlixelUiCheckbox vsync = new FlixelUiCheckbox("V-Sync", settings.isVsync());
vsync.onChange.add(cb -> settings.setVsync(cb.isChecked()));
vsync.anchor(FlixelAlign.TOP_LEFT, 16, 16);
ui.add(vsync);

// In input handling:
if (hit == vsync && Flixel.mouse.justReleased(FlixelMouseButton.LEFT)) vsync.click();
```

### Radio group

`FlixelUiRadioGroup` is a plain object (not a widget) that keeps at most one button selected. Selecting one
button automatically deselects the others.

```java
FlixelUiRadioGroup difficulty = new FlixelUiRadioGroup();
FlixelUiRadioButton easy   = new FlixelUiRadioButton("Easy",   difficulty);
FlixelUiRadioButton medium = new FlixelUiRadioButton("Medium", difficulty);
FlixelUiRadioButton hard   = new FlixelUiRadioButton("Hard",   difficulty);
difficulty.select(0); // "Easy" selected by default.
difficulty.onChange.add(g -> applyDifficulty(g.getSelected()));

FlixelUiStack radioRow = new FlixelUiStack(FlixelUiStack.Direction.HORIZONTAL);
radioRow.setSpacing(16);
radioRow.add(easy); radioRow.add(medium); radioRow.add(hard);
ui.add(radioRow);

// In input handling:
if (hit instanceof FlixelUiRadioButton rb && Flixel.mouse.justReleased(FlixelMouseButton.LEFT)) {
    rb.click(); // Selects this button and deselects the others.
}

// Gamepad navigation through the group:
if (controls.dpadRight.justPressed()) difficulty.selectNext();
if (controls.dpadLeft.justPressed())  difficulty.selectPrevious();
```

### Modal

`FlixelUiModal` is a panel that opens on a dedicated layer above all root widgets. The display draws each
modal's backdrop (from its `FlixelUiModalStyle`) immediately before the modal itself.

**The UI never closes a modal automatically.** There is no built-in "click the backdrop to close" or "Escape
closes". Your game drives all of that:

```java
// Build the modal.
FlixelUiModal dialog = new FlixelUiModal(300, 180);
dialog.anchor(FlixelAlign.CENTER, 0, 0);
FlixelUiButton closeBtn = new FlixelUiButton("OK", b -> dialog.close());
// ... add content ...
dialog.add(closeBtn);

// Open it.
ui.openModal(dialog);

// Close on a click outside the modal.
if (Flixel.mouse.justPressed(FlixelMouseButton.LEFT) && ui.getWidgetAt(mx, my) == null) {
    ui.closeModal(); // Closes the topmost modal.
}

// Close on a back/escape action.
if (controls.back.justPressed()) {
    ui.closeModal();
}

// Close when a specific modal is buried below others.
ui.closeModal(dialog); // Closes this particular modal.
```

`destroyOnClose` — set this to `true` if you want the modal destroyed when it is closed.

### Dropdown

`FlixelUiDropdown` shows the selected item in a fixed-width field and opens a floating list when the game
calls `open()`.

```java
FlixelUiDropdown res = new FlixelUiDropdown(200);
res.addItem("1280 x 720");
res.addItem("1920 x 1080");
res.addItem("2560 x 1440");
res.setSelectedIndex(0);
res.setPlaceholder("Choose...");
res.onSelect.add(d -> applyResolution(d.getSelectedIndex()));
res.anchor(FlixelAlign.CENTER, 0, 0);
ui.add(res);
```

**Wiring a dropdown.** The dropdown widget itself occupies the field row; when open, it places a floating list
on the display's popup layer. `getWidgetAt(x, y)` returns the dropdown widget for clicks on the field, and
returns items inside the list when the list is open. Use the dropdown's own hit-testing helpers to tell the
two apart:

```java
// Open or close the field on click.
if (hit == res && Flixel.mouse.justPressed(FlixelMouseButton.LEFT)) res.toggle();

// When open, highlight the row the cursor is on.
if (res.isOpen()) {
    res.highlight(res.getItemAt(mx, my)); // Returns -1 when not over any row.
}

// Select the highlighted row on click (or on gamepad accept).
if (res.isOpen() && Flixel.mouse.justReleased(FlixelMouseButton.LEFT)) {
    int idx = res.getHighlightedIndex();
    if (idx >= 0) res.select(idx); // Fires onSelect and closes the list.
    else res.close();
}

// Gamepad / keyboard navigation while open.
if (res.isOpen()) {
    if (controls.dpadDown.justPressed())  res.highlightNext();
    if (controls.dpadUp.justPressed())    res.highlightPrevious();
    if (controls.accept.justPressed())    res.select(res.getHighlightedIndex());
    if (controls.back.justPressed())      res.close();
}
```

### Tooltip

The display owns one shared `FlixelUiTooltip`. The game shows it by calling `showTooltip(target)` or
`showTooltip(target, delaySeconds)` and hides it with `hideTooltip()`. The tooltip is hidden automatically
when the target widget is removed, killed, or becomes invisible.

Set tooltip text on a widget:

```java
playButton.setTooltip("Start a new game.");
settingsButton.setTooltip("Open the settings menu.");
```

Show it with a hover delay:

```java
FlixelUiWidget hovered;

// In update():
FlixelUiWidget hit = ui.getWidgetAt(mx, my);
if (hit != hovered) {
    ui.hideTooltip();
    if (hit != null && hit.hasTooltip()) ui.showTooltip(hit, 0.4f); // 400 ms delay.
    hovered = hit;
}
```

The tooltip's appearance (background, font, gap, max width) comes from the `FlixelUiTooltipStyle` in the skin
under the name `"default"` (or whatever name you pass to `ui.setTooltipStyle("name")`).

### Text box

`FlixelTextBox` handles single-line and multi-line text entry with a caret, selection, placeholder text, a
character filter, and a password mask.

```java
// Single-line input (player name).
FlixelTextBox name = new FlixelTextBox(220);
name.setPlaceholder("Player name");
name.setMaxLength(16);
name.setFilter(FlixelTextFilter.ALPHANUMERIC);
name.onSubmit.add(t -> save.playerName = t.getText().toString());
ui.add(name);

// Multi-line notes field.
FlixelTextBox notes = new FlixelTextBox(300, 120);
notes.setMultiLine(true);
ui.add(notes);

// PIN entry with masking.
FlixelTextBox pin = new FlixelTextBox(120);
pin.setPasswordChar('*');
pin.setFilter(FlixelTextFilter.DIGITS);
pin.setMaxLength(6);
ui.add(pin);
```

**Keyboard input is automatic.** The text box registers itself as a keyboard listener when it is created and
removes itself when destroyed. Keys only reach the text while the box is focused, so focusing it is all the
game has to do.

**Focus on pointer down; blur on pointer down outside.**

```java
if (Flixel.mouse.justPressed(FlixelMouseButton.LEFT)) {
    if (hit == name) {
        name.focus();
        name.setCaret(name.getIndexAt(mx, my));
    } else if (name.isFocused()) {
        name.blur();
    }
}
```

**Clipboard.** Call `copy()`, `cut()`, and `paste()` from your game's own shortcut handling if you want those
operations. `FlixelTextBox.keyDown()` already handles them through the keyboard listener, so you only need to
call them manually when driving the box without the keyboard listener (for example, from a context menu).

**Word editing.** With the keyboard listener registered, Ctrl+Left and Ctrl+Right jump by word, and
Ctrl+Backspace and Ctrl+Delete delete a whole word through `deleteWord(-1)` and `deleteWord(1)`.

**Built-in filters.** `FlixelTextFilter.DIGITS`, `FlixelTextFilter.ALPHANUMERIC`,
`FlixelTextFilter.ASCII_PRINTABLE`, and the always-accept default. Implement the interface for anything else:

```java
shout.setFilter(c -> c >= 'A' && c <= 'Z'); // Uppercase letters only.
```

---

## Skins and styles

### Style classes

Each widget type has a matching style class. Create an instance, fill in its fields, and add it to the skin:

| Widget | Style class |
|---|---|
| `FlixelUiPanel` | `FlixelUiPanelStyle` |
| `FlixelUiModal` | `FlixelUiModalStyle` (extends `FlixelUiPanelStyle`) |
| `FlixelUiButton` | `FlixelUiButtonStyle` |
| `FlixelUiLabel` | `FlixelUiLabelStyle` |
| `FlixelUiCheckbox` | `FlixelUiCheckboxStyle` |
| `FlixelUiRadioButton` | `FlixelUiRadioButtonStyle` (same fields as checkbox) |
| `FlixelUiDropdown` | `FlixelUiDropdownStyle` |
| `FlixelUiTooltip` | `FlixelUiTooltipStyle` |
| `FlixelTextBox` | `FlixelTextBoxStyle` |

### Required fields

Most style classes require only one background field. Everything else falls back gracefully:

- **Button, dropdown, text box**: `up` is required. `over`, `down`, `focused`, `disabled` all fall back to
  `up` when `null`. All font colors fall back to `fontColor`.
- **Checkbox, radio button**: `off` and `on` are required. All hover/pressed/focused/disabled variants fall
  back to their plain counterparts.
- **Panel**: `background` is optional (leaving it `null` draws nothing).
- **Modal**: `backdrop` is optional. When `null`, no full-screen overlay is drawn.
- **Label**: has no backgrounds — only `font`, `fontSize`, and `color`.
- **Tooltip**: `background` is optional; if absent, the tooltip draws its text with no frame.

### Background ownership and `skin.track(...)`

Backgrounds hold GPU resources. Use `skin.track(background)` to register a background with the skin so it is
destroyed when `skin.destroy()` is called.

> **Browser backend note**: build the skin only after all image files have been decoded. See the
> [Quick start](#1-preload-images-browser-backend) section for the loading-state pattern.

```java
FlixelNineSlice panelBg = FlixelNineSlice.load(Flixel.files.internal("ui/panel.png"), 8, 8, 8, 8);
skin.track(panelBg); // skin.destroy() will release this.
style.background = panelBg;
```

Alternatively, pass the result of `track(...)` directly:

```java
style.up = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/btn.png"), 6, 6, 6, 6));
```

Backgrounds shared between styles (for example, `style.focused = style.over`) are safe: the skin tracks
objects by identity and destroys each one only once.

### `FlixelUiColorFill` for backdrops, carets, and selections

`FlixelUiColorFill` paints a plain colored rectangle. Use it anywhere a background is expected — no image
file needed:

```java
// Modal backdrop (translucent black).
modalStyle.backdrop = new FlixelUiColorFill(0f, 0f, 0f, 0.6f);

// Text selection highlight (semi-transparent blue).
textBoxStyle.selection = new FlixelUiColorFill(0.2f, 0.5f, 1f, 0.5f);

// Blinking caret (opaque white).
textBoxStyle.caret      = new FlixelUiColorFill(FlixelColor.WHITE);
textBoxStyle.caretWidth = 2f;
textBoxStyle.caretBlinkRate = 0.53f; // Half a second per half cycle.
```

`FlixelUiColorFill.destroy()` is a no-op — it does not own any image.

### Multiple styles per widget type

A skin can hold several styles of the same class, each under a different name. The style name defaults to
`"default"` and is changed with `widget.setStyle("name")`:

```java
skin.add("default", normalButtonStyle);
skin.add("danger",  redButtonStyle);
skin.add("fab",     fabStyle);

FlixelUiButton deleteBtn = new FlixelUiButton("Delete");
deleteBtn.setStyle("danger");
```

---

## Clipping

A container with `setClipChildren(true)` restricts drawing to its content area (its bounds minus padding).
Children that extend past the edges are cut off, like a window frame that hides whatever is past its glass.
Clip rectangles stack and intersect: a clipping container inside another clipping container cuts to the
overlap of both.

Widgets can also clip their own internals from inside `drawSelf()`:

```java
if (pushClip(batch, getScreenX() + padLeft, getScreenY() + padTop,
             getWidth() - padLeft - padRight, getHeight() - padTop - padBottom)) {
    drawScrolledContent(batch);
}
popClip(batch);
```

Clip rectangles are always axis-aligned in the HUD camera's view space and ignore `getAngle()`.

---

## HUD cameras and `defaultDrawTarget`

`FlixelUiDisplay.createHudCamera()` creates a transparent `FlixelCamera` with `defaultDrawTarget = false`
and adds it to `Flixel.cameras`. The `defaultDrawTarget = false` flag means world objects that do not
explicitly list a set of cameras (those whose `FlixelBasic.cameras` is `null`) will not be drawn on the HUD
camera. Only the display itself, and any object that specifically lists the HUD camera in its own `cameras`
field, will appear there.

This prevents every game-world sprite from being drawn twice when a HUD camera is active. If you have a
sprite that must appear on both the world and the HUD (for example a crosshair), add the HUD camera to its
`cameras` list.

**Call `createHudCamera()` inside `create()`**, not in the constructor. `FlixelState.create()` runs after the
framework clears the camera list; a camera created before that will be gone.

```java
@Override
public void create() {
    super.create();
    FlixelUiDisplay ui = new FlixelUiDisplay(FlixelUiDisplay.createHudCamera(), skin);
    add(ui);
    // ...
}
```

---

## Performance notes

- **No per-frame allocation.** `getWidgetAt(x, y)`, layout, drawing, and hit-testing allocate nothing.
  `FlixelUiTextPart` (the internal text renderer used by labels, buttons, and toggles) pools its rendering
  state so that updating a text label does not allocate a new object.
- **Lazy layout.** Layout only runs when the display's `update()`, `draw()`, or `getWidgetAt()` is called
  after something marked the layout dirty. If nothing changed, no work is done.
- **Clip stack is fixed-size.** The display keeps a stack of at most `FlixelUiDisplay.MAX_CLIP_DEPTH`
  (16) rectangles, all allocated up front. Pushing beyond the limit throws an `IllegalStateException`.
- **FlixelArray for children.** Every container uses `FlixelArray`, the framework's typed-array collection,
  rather than a Java standard-library list. No boxing, no `Iterator` objects.

---

## Known limitations

- **Rotation does not rotate `FlixelText`-based labels.** Setting `getAngle()` on a button, label, or text
  box rotates the background geometry around the widget's center, but the text drawn by `FlixelText` is not
  rotated. The text will appear at the unrotated screen position. Use rotation only on widgets without text.
- **`FlixelSprite` clip rects inside UI widgets reset the UI clip.** If a `FlixelSprite` is added to the
  same camera as the UI and uses its own scissor/clip, it may reset the GL scissor state that the UI clip
  stack set. Use `FlixelUiContainer.setClipChildren(true)` instead of mixing sprite-level clipping.
- **IME composition preview is not shown inline.** The text box calls `startTextInput()` for IME support,
  but it does not render the in-progress composition string inline. The committed text appears normally.
- **Missing styles throw on attach.** If a widget is added to a display whose skin has no style matching
  the widget's style name, an `IllegalArgumentException` is thrown at attach time (not at draw time). Build
  the skin before adding widgets.
- **Browser backend: images must be preloaded.** `FlixelNineSlice.load(...)` and `FlixelUiImage.load(...)`
  call the asset manager synchronously. On the browser backend, images are decoded asynchronously, so
  calling `load(...)` before the file is decoded throws. Preload all UI image files in a loading state
  with `Flixel.assets.load(path)` and wait for `Flixel.assets.update()` to return `true` before building
  the skin. See the Quick start section for a loading-state example.
- **Tooltips and hover are game-driven.** There is no automatic hover tooltip timing. The game must track
  the hovered widget and call `showTooltip(target, delay)` itself.

---

## Package overview

| Package | Contents |
|---|---|
| `org.flixelgdx.ui` | Core widgets (`FlixelUiDisplay`, `FlixelUiWidget`, `FlixelUiPanel`, etc.) |
| `org.flixelgdx.ui.graphics` | Backgrounds: `FlixelNineSlice`, `FlixelUiImage`, `FlixelUiColorFill` |
| `org.flixelgdx.ui.skin` | Style and skin classes for every widget type |
| `org.flixelgdx.ui.text` | `FlixelTextBox`, `FlixelTextModel`, `FlixelTextFilter` |

---

## Contributing and building

- **[COMPILING.md](COMPILING.md)** - how to build and test the extension locally, including the composite
  build against a local framework clone.
- **[CONTRIBUTING.md](CONTRIBUTING.md)** - contribution guidelines (shared with the main framework).
- **[AGENTS.md](AGENTS.md)** - coding style, architecture rules, and the UI-specific design rules AI
  assistants (and human contributors) follow in this repository.

---

## License

FlixelGDX UI is released under the [MIT License](LICENSE).
