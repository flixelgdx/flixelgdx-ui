# Custom Widgets and Debugging

This guide shows how to write a widget of your own, which hooks to override for drawing, styling, layout, pointer
clicks, scrolling, and cursors, and how FlixelGDX UI shows up in the FlixelGDX debugger.

## Table of contents

1. [When to write a custom widget](#when-to-write-a-custom-widget)
2. [The smallest widget](#the-smallest-widget)
3. [Giving it a style](#giving-it-a-style)
4. [The hooks](#the-hooks)
5. [Reacting to the pointer](#reacting-to-the-pointer)
6. [Clipping your own parts](#clipping-your-own-parts)
7. [The debugger](#the-debugger)
8. [Common mistakes](#common-mistakes)
9. [Check your understanding](#check-your-understanding)

## When to write a custom widget

Most screens only need the built-in widgets inside panels and stacks. Write your own when you need something they do
not draw, such as a progress bar, a color swatch, a minimap frame, or a key-binding button. A custom widget gets
layout, anchors, alpha, tinting, hit-testing, focus, signals, and debugger support for free; it only has to draw
itself.

## The smallest widget

Extend `FlixelUiWidget` and implement `drawSelf(...)`. The coordinates you receive are the widget's top-left corner,
ready to hand to the batch, and `alpha` already includes every parent's alpha:

```java
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.ui.FlixelUiWidget;
import org.flixelgdx.ui.graphics.FlixelUiColorFill;
import org.flixelgdx.util.FlixelColor;
import org.jetbrains.annotations.NotNull;

public class ColorSwatch extends FlixelUiWidget {

  private final FlixelUiColorFill fill = new FlixelUiColorFill(FlixelColor.WHITE);

  public ColorSwatch(float size) {
    super(size, size);
  }

  public void setSwatchColor(FlixelColor color) {
    fill.setColor(color);
  }

  @Override
  protected void drawSelf(@NotNull FlixelBatch batch, float drawX, float drawY, float alpha) {
    FlixelColor tint = getColor();
    batch.setColor(tint.getRed(), tint.getGreen(), tint.getBlue(), alpha);
    fill.draw(batch, drawX, drawY, width, height);
    batch.setColor(FlixelColor.WHITE);
  }
}
```

Draw as if the widget were not rotated (rotation is applied around your drawing for you), multiply your colors by
`alpha`, and set the batch color back to white afterward. `drawSelf(...)` runs every frame, so it must not allocate.

## Giving it a style

To make your widget skinnable like the built-in ones, write a style class that implements `FlixelUiStyle` and fetch
it in `onStyleChanged()` with `resolveStyle(...)`:

```java
public class ProgressBarStyle implements FlixelUiStyle {
  public FlixelUiBackground track;
  public FlixelUiBackground fill;
}

public class ProgressBar extends FlixelUiWidget {

  private ProgressBarStyle style;
  private float progress;

  public ProgressBar(float width, float height) {
    super(width, height);
    interactive = false; // Decoration: clicks pass through.
  }

  public void setProgress(float progress) {
    this.progress = progress;
  }

  @Override
  protected void onStyleChanged() {
    style = resolveStyle(ProgressBarStyle.class); // Uses getStyleName(), "default" unless changed.
  }

  @Override
  protected void drawSelf(@NotNull FlixelBatch batch, float drawX, float drawY, float alpha) {
    ProgressBarStyle s = style;
    if (s == null) {
      return;
    }
    FlixelColor tint = getColor();
    batch.setColor(tint.getRed(), tint.getGreen(), tint.getBlue(), alpha);
    if (s.track != null) {
      s.track.draw(batch, drawX, drawY, width, height);
    }
    if (s.fill != null && progress > 0f) {
      s.fill.draw(batch, drawX, drawY, width * progress, height);
    }
    batch.setColor(FlixelColor.WHITE);
  }
}
```

Then add a `ProgressBarStyle` to the skin like any other: `skin.add("default", barStyle)`. `onStyleChanged()` runs when
the widget joins a display, when its style name changes, and when the display's skin is replaced, so the widget always
wears the current style. `resolveStyle(...)` throws a clear error when the skin lacks the style.

## The hooks

Override only what you need. Every hook that runs during layout or drawing must not allocate.

| Hook | When it runs | Typical use |
|------|--------------|-------------|
| `drawSelf(batch, x, y, alpha)` | Every draw, before children. | Draw the widget. |
| `update(elapsed)` | Every frame. | Animations, blinking, timers. |
| `onStateChanged()` | Hover, press, focus, or enabled changed, before the signal fires. | Pick a different background. |
| `onStyleChanged()` | Joined a display, style name changed, or skin replaced. | `resolveStyle(...)` and apply it. |
| `onAttached(display)` / `onDetached()` | Joined or left a display. | Grab or release anything the display provides. |
| `getPreferredWidth()` / `getPreferredHeight()` | Layout. | Report a size based on content. Call `invalidateLayout()` when it changes. |
| `onMeasure()` | Layout, after children are measured. | Recompute a content-based size. |
| `onLayout()` | Layout, after this widget is placed. | Position internal parts using `getScreenX()` and `getScreenY()`. |
| `onActivate(x, y)` | A `FlixelUiPointer` pressed and released on it. | The widget's "do your thing" action. |
| `onScroll(amount)` | A `FlixelUiPointer` scrolled over it or a child. | Scroll content; return `true` to consume it. |
| `getDefaultCursor()` / `getDefaultPressedCursor()` | The pointer asks for a cursor. | A cursor for this kind of widget. |

To hold children, extend `FlixelUiContainer` (or `FlixelUiPanel` for a background) instead; children are then laid out
and drawn for you.

## Reacting to the pointer

A custom widget joins in with `FlixelUiPointer` by overriding `onActivate(...)`. Keep the real action in a public
method too, so games that wire input by hand (or use a gamepad) can call it:

```java
public class KeyBindButton extends FlixelUiWidget {

  public final FlixelSignal<KeyBindButton> onListen = new FlixelSignal<>();

  public KeyBindButton(float width, float height) {
    super(width, height);
  }

  /** Starts listening for a new key; the game's own code reads the key. */
  public void listen() {
    if (isEnabled()) {
      onListen.dispatch(this);
    }
  }

  @Override
  protected void onActivate(float x, float y) {
    listen();
  }

  @Override
  protected FlixelMouseCursor getDefaultCursor() {
    return FlixelUiCursors.button; // A pointing hand, like buttons.
  }

  @Override
  protected void drawSelf(@NotNull FlixelBatch batch, float drawX, float drawY, float alpha) {
    // Draw the current binding here.
  }
}
```

Remember the extension's golden rule when writing widgets: the widget must not read input itself. Expose a method and
let the game (or the pointer) call it.

## Clipping your own parts

A widget that scrolls its own content can restrict drawing to a rectangle with `pushClip(...)` and `popClip(...)`. The
rectangle is in the camera's view space (like `getScreenX()`), and it is combined with any clip already active:

```java
@Override
protected void drawSelf(@NotNull FlixelBatch batch, float drawX, float drawY, float alpha) {
  frame.draw(batch, drawX, drawY, width, height);
  if (pushClip(batch, getScreenX() + 4, getScreenY() + 4, width - 8, height - 8)) {
    drawScrolledContent(batch);
  }
  popClip(batch);
}
```

`pushClip(...)` returns `false` when nothing would be visible, so you can skip the drawing. To clip children instead,
use a container with `setClipChildren(true)`.

## The debugger

The display and every widget implement the framework's `FlixelDebugNode`, so the FlixelGDX debugger shows the whole UI
in its scene tree, draws a box around each widget in the viewport, and can pick or click widgets there. A display
lists its four layers, and each container lists its children in draw order.

The inspector shows every widget's state and lets you edit it: visibility, enabled state, position, size, alpha, and
tooltip, plus each widget's own rows, such as a checkbox's checked state, a slider's value and range, a dropdown's
selection, and a text box's text and caret. Edits go through the normal setters, so `onChange` and `onSelect` fire
exactly as they would for a player. These rows are registered automatically the first time a `FlixelUiDisplay` is
created in debug mode, through `FlixelUiDebugSections.register()`, so release builds pay nothing.

Your custom widgets get the common widget rows automatically. To add rows for their own fields, register an inspector
section for your class with the framework's debugger API, the same way `FlixelUiDebugSections` does; the framework's
handbook guide on extending the debugger explains how.

Debugger clicks and typed text arrive as ordinary input, so your pointer forwarding and focused text boxes react to
them like a real mouse and keyboard.

## Common mistakes

- **Allocating in `drawSelf(...)`.** It runs every frame. Create backgrounds, colors, and strings once, in the
  constructor or `onStyleChanged()`.
- **Forgetting to reset the batch color.** Later widgets draw tinted. Call `batch.setColor(FlixelColor.WHITE)`.
- **The widget's size never updates.** It reports a new preferred size but never calls `invalidateLayout()`.
- **Reading `Flixel.mouse` inside the widget.** That breaks the rule that the game drives the UI. Expose a method and
  override `onActivate(...)`.
- **Decorative widgets swallow clicks.** Set `interactive = false` in the constructor.

## Check your understanding

1. Which one method must every custom widget implement?
2. Where should a custom widget fetch its style, and why there?
3. A custom widget should do something when a `FlixelUiPointer` clicks it. What do you override, and what else should
   you provide?

(Answers: `drawSelf(...)`; in `onStyleChanged()`, because it runs whenever the style could have changed, including
skin swaps; override `onActivate(x, y)`, and also offer a public method with the same action for games that wire input
by hand.)

**Next:** ["How Do I...?" Cookbook](cookbook.md)
