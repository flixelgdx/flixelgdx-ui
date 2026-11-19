# Labels, Buttons, and Pictures

This guide covers the three widgets nearly every screen uses: labels for text, buttons for actions (including icon
buttons and floating action buttons), and pictures for images such as logos and portraits.

## Table of contents

1. [Labels](#labels)
2. [Text that changes every frame](#text-that-changes-every-frame)
3. [Buttons](#buttons)
4. [Icons and floating action buttons](#icons-and-floating-action-buttons)
5. [How a button picks its look](#how-a-button-picks-its-look)
6. [Pictures](#pictures)
7. [Common mistakes](#common-mistakes)
8. [Check your understanding](#check-your-understanding)

## Labels

`FlixelUiLabel` shows a piece of text: a heading, a hint, or a score. A label is like a name tag stuck onto the UI. It
only shows words, so it is not interactive and clicks pass straight through it.

```java
FlixelUiLabel title = new FlixelUiLabel("Settings");
title.setStyleName("title");        // A FlixelUiLabelStyle named "title" in the skin.
title.anchor(FlixelAlign.TOP, 0, 16);
ui.add(title);

FlixelUiLabel hint = new FlixelUiLabel("Press any key to continue.");
hint.setWrapWidth(200);                          // Wraps onto new lines at 200 pixels.
hint.setAlignment(FlixelText.Alignment.CENTER);  // Centers every line.
```

- The font, size, and color come from a `FlixelUiLabelStyle`.
- A label sizes itself to its text: as wide as its longest line (or exactly the wrap width when wrapping) and as tall
  as all the lines. A `'\n'` always starts a new line.
- Giving it a size with `setSize(...)`, `setWidth(...)`, or `setHeight(...)` fixes that side. The text is then placed
  inside using `getAlignment()` horizontally and centered vertically. `setAutoSize(true)` switches back.

## Text that changes every frame

Building a new string like `"Score: " + score` every frame creates garbage, which causes stutters. `setText(...)`
copies the characters it is given and does nothing when they have not changed, so pass a reused `FlixelString`:

```java
private final FlixelString scoreText = new FlixelString();

// In update():
scoreText.set("Score: ").concat(score);
scoreLabel.setText(scoreText);
```

`getText()` returns the label's own buffer. Read it, but change it only through `setText(...)`. Buttons, checkboxes,
and radio buttons have the same `setText(...)` and `getText()` pair.

## Buttons

`FlixelUiButton` shows text, an icon, or both, and changes its look as it is hovered, pressed, focused, or disabled.
Think of a doorbell: pressing it makes it look pushed in, but the bell only rings when the game calls `click()`, which
fires `onClick`.

```java
FlixelUiButton play = new FlixelUiButton("Play", b -> Flixel.switchState(PlayState::new));
play.onHover.add(b -> Flixel.sound.play(Flixel.files.internal("sfx/hover.ogg")));
ui.add(play);

FlixelUiButton options = new FlixelUiButton("Options"); // No listener yet.
options.onClick.add(b -> openOptions());

play.setEnabled(false); // Disabled look; click() is ignored until it is enabled again.
play.setText("Continue");
```

With a `FlixelUiPointer`, a button clicks when it is pressed and released under the pointer. Without one, your game
calls `press()`, `release()`, and `click()` itself (see [Wiring Input](input.md)). `click()` alone fires `onClick`
without changing the pressed look, which suits a keyboard or gamepad confirm.

The listener receives the button, so one listener can serve several buttons without allocating:

```java
SignalHandler<FlixelUiButton> pick = b -> chooseLevel(b.getText());
level1.onClick.add(pick);
level2.onClick.add(pick);
```

Like labels, buttons size themselves (padding plus content, never smaller than the background's minimum size) until
you give them a size, and the content stays centered in whatever size they end up with. Inside a column, a common
trick is `button.setPercentSize(1f, Float.NaN)` so every button is as wide as the column.

## Icons and floating action buttons

Buttons can show an icon on the left of the text, or an icon alone:

```java
FlixelUiButton save = new FlixelUiButton("Save", Flixel.files.internal("ui/disk.png"), b -> saveGame());
FlixelUiButton close = new FlixelUiButton(Flixel.files.internal("ui/x.png"), b -> closeWindow());
FlixelUiButton fromAtlas = new FlixelUiButton(atlasFrame, b -> doThing()); // A FlixelFrame.

save.setIcon(Flixel.files.internal("ui/disk_dirty.png")); // Replace the icon.
save.setIcon((FlixelFile) null);                           // Remove it.
```

The icon is drawn at its image's own size, with the style's `iconSpacing` between icon and text. An icon loaded from a
file is released when it is replaced or the button is destroyed; a `FlixelFrame` you pass in stays yours.

A **floating action button** (a round button hovering over a corner, usually with a single "+") is not a separate
class. It is an icon-only button wearing a round style and anchored to a corner:

```java
// While building the skin:
FlixelUiButtonStyle fabStyle = new FlixelUiButtonStyle();
fabStyle.up = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/fab.png"), 24, 24, 24, 24));
fabStyle.padLeft = fabStyle.padTop = fabStyle.padRight = fabStyle.padBottom = 16;
skin.add("fab", fabStyle);

// In the state:
FlixelUiButton fab = new FlixelUiButton(Flixel.files.internal("ui/plus.png"), b -> openEditor());
fab.setStyleName("fab");
fab.anchor(FlixelAlign.BOTTOM_RIGHT, -24, -24);
ui.add(fab);
```

## How a button picks its look

Exactly one look shows at a time, chosen in this order: **disabled**, then **pressed** (`down`), then **hovered**
(`over`), then **focused**, then **up**. A missing background falls back to `up`, and a missing text color falls back
to `fontColor`. While pressed, the content moves down by the style's `pressedOffsetY`, which makes a button feel pushed
in when its `down` art is drawn lower than its `up` art. `getBackground()` and `getFontColor()` report what is showing
right now.

## Pictures

`FlixelUiPicture` shows an image as a widget of its own, with a position, anchors, and scaling. It has no style.

```java
// A logo that keeps its shape while it scales to fit a 200 by 100 box.
FlixelUiPicture logo = new FlixelUiPicture(Flixel.files.internal("ui/logo.png"), 200, 100);
logo.setScaleMode(FlixelUiPicture.ScaleMode.FIT);
logo.anchor(FlixelAlign.TOP, 0, 16);
ui.add(logo);

// One 32 by 32 cell of a sprite sheet, at its natural size.
FlixelUiPicture icon = new FlixelUiPicture(Flixel.files.internal("ui/icons.png"));
icon.setRegion(64, 0, 32, 32);
```

- **Size.** A picture made without a size takes the natural size of its image (or region) and keeps following it
  through `setImage(...)` and `setRegion(...)`. A picture made with a size (the constructors that take a width and
  height) keeps that size. To resize a picture made without one, call `setAutoSize(false)` first; a plain
  `setSize(...)` is overwritten by the natural size on the next layout pass. `setAutoSize(true)` follows the image
  again.
- **Scale modes.** `FIT` (the default) scales evenly until the whole image fits, which can leave empty bars. `FILL`
  scales evenly until the widget is covered, cropping the overflow. `STRETCH` fills the widget and ignores the image's
  shape. `NONE` draws at natural size and crops.
- **Alignment.** For every mode except `STRETCH`, `setAlign(...)` decides where leftover space goes (`CENTER` by
  default), for example `FlixelAlign.TOP_LEFT`.
- **Changing the image.** `setImage(file)` or `setImage(frame)` swaps it, `setRegion(...)` and `clearRegion()` pick a
  part, and `getNaturalWidth()` and `getNaturalHeight()` report the source size.
- **Ownership.** An image loaded from a file is released when replaced or destroyed; a frame you pass in stays yours.
- **Clicks.** A picture is not interactive by default. Set `picture.interactive = true` to make it hittable, for
  example for a clickable portrait.

Tint and fade a picture like any widget, with `getColor().set(...)` and `setAlpha(...)`.

## Common mistakes

- **A label creates garbage every frame.** Pass a reused `FlixelString` to `setText(...)` instead of concatenating.
- **`setIcon(null)` does not compile.** Both overloads accept `null`, so cast it: `setIcon((FlixelFile) null)`.
- **A button never shows its focused look.** Nothing focused it. Call `focus()` from your keyboard or gamepad code.
- **Buttons in a column have different widths.** Give each `setPercentSize(1f, Float.NaN)`.
- **A picture snaps back to its image's size after `setSize(...)`.** It is still auto-sizing. Call
  `setAutoSize(false)` first, or create it with a size.
- **A clickable picture ignores clicks.** Pictures are not interactive by default; set `interactive = true`.

## Check your understanding

1. Why does `button.click()` from a gamepad not show the pushed-in look, and how do you get it?
2. A logo must fill a 300 by 300 square without being squashed, and cropping is fine. Which scale mode?
3. A score label updates every frame. What do you pass to `setText(...)`?

(Answers: `click()` only fires `onClick`; call `press()` on button down and `release()` then `click()` on button up;
`FILL`; a reused `FlixelString` field, refilled with `set(...)` and `concat(...)`.)

**Next:** [Checkboxes and Radio Buttons](checkboxes-and-radio-buttons.md)
