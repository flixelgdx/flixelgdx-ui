# Skins, Styles, and Backgrounds

This guide explains how your art reaches every widget: backgrounds (nine-slices, images, and color fills), style
classes, named styles in a skin, one-off styles, changing styles at runtime, and who destroys what.

## Table of contents

1. [Looks are kept apart from behavior](#looks-are-kept-apart-from-behavior)
2. [Backgrounds](#backgrounds)
3. [Styles](#styles)
4. [Skins and named styles](#skins-and-named-styles)
5. [One-off styles](#one-off-styles)
6. [Changing styles at runtime](#changing-styles-at-runtime)
7. [Who destroys what](#who-destroys-what)
8. [Loading images safely in the browser](#loading-images-safely-in-the-browser)
9. [Style reference](#style-reference)
10. [Common mistakes](#common-mistakes)
11. [Check your understanding](#check-your-understanding)

## Looks are kept apart from behavior

A button only knows how to behave: hover, press, click. Everything about how it looks lives in a separate **style**
object. That lets one game reuse the same button code with several looks, and lets an artist restyle the whole UI
without touching the code that wires buttons up.

Think of a skin as a wardrobe with one drawer per kind of widget. The button drawer holds button outfits, the checkbox
drawer holds checkbox outfits, and each outfit has a name tag such as `"default"` or `"danger"`. When a widget is added
to a display, it opens its own drawer in the display's skin and puts on the outfit whose name matches its style name.

The extension ships no built-in skin. You bring your own images and fonts.

## Backgrounds

A background is anything a widget can paint behind itself, stretched to the widget's size. All three kinds implement
`FlixelUiBackground`, so any style field that takes a background accepts any of them.

| Background | Use it for | Create it with |
|------------|-----------|----------------|
| `FlixelNineSlice` | Panels, buttons, text box frames: art with borders or rounded corners. | `FlixelNineSlice.load(file, left, top, right, bottom)` or `new FlixelNineSlice(frame, left, top, right, bottom)` |
| `FlixelUiImage` | Check marks, radio dots, slider thumbs, arrows: icons drawn at their own size. | `FlixelUiImage.load(file)` or `new FlixelUiImage(frame)` |
| `FlixelUiColorFill` | Prototypes, highlights, carets, and dimmed modal backdrops. | `new FlixelUiColorFill(r, g, b, a)` or `new FlixelUiColorFill(color)` |

**Nine-slices** cut an image into a 3 by 3 grid. Picture a window frame made of nine pieces of wood: the four corners
never change size, the top and bottom pieces stretch sideways, the left and right pieces stretch up and down, and the
glass in the middle stretches both ways. The four numbers are how thick each side of the frame is, in pixels of the
source image:

```java
// Corners are 8 pixels on every side of panel.png.
FlixelNineSlice panel = FlixelNineSlice.load(Flixel.files.internal("ui/panel.png"), 8, 8, 8, 8);
```

A frame from a texture atlas works too; the nine-slice cuts inside that frame's region. Atlas frames packed rotated or
trimmed are rejected, so export nine-slice art with rotation and trimming turned off.

Backgrounds draw with the widget's tint and fade applied for them, so one background can be shared by many widgets in
different states. Drawing never allocates.

## Styles

Every widget type that has a look has a style class: a plain object with public fields and no behavior. Only one or
two fields are usually required; state variants left `null` fall back to the plain one, so a prototype needs very
little:

```java
FlixelUiButtonStyle style = new FlixelUiButtonStyle();
style.up = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/button.png"), 6, 6, 6, 6));
style.over = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/button_over.png"), 6, 6, 6, 6));
style.down = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/button_down.png"), 6, 6, 6, 6));
style.focused = style.over;
style.font = Flixel.files.internal("fonts/pixel.ttf");
style.fontSize = 16;
style.padLeft = style.padRight = 12;
style.padTop = style.padBottom = 6;
style.pressedOffsetY = 1; // The text sinks one pixel while pressed.
```

Fonts are `FlixelFile` references, never string paths, and a `null` font uses the framework's built-in font. Font
sizes below `1` count as `1`.

## Skins and named styles

`FlixelUiSkin` stores styles by their exact class and a name:

```java
FlixelUiSkin skin = new FlixelUiSkin();
skin.add("default", buttonStyle);
skin.add("default", checkboxStyle); // Different drawer, so no clash.

FlixelUiButtonStyle danger = new FlixelUiButtonStyle();
danger.up = skin.track(new FlixelUiColorFill(0.8f, 0.1f, 0.1f, 1f));
skin.add("danger", danger);

ui = new FlixelUiDisplay(FlixelUiDisplay.createHudCamera(), skin);

FlixelUiButton delete = new FlixelUiButton("Delete Save");
delete.setStyleName("danger");
```

Every widget's style name starts as `"default"`. Adding a style under a name that already exists in that drawer
replaces it. Other skin methods:

- `skin.get(FlixelUiButtonStyle.class, "danger")` returns a style, or throws with a clear message naming the missing
  class and name.
- `skin.has(type, name)` checks without throwing.
- `skin.remove(type, name)` removes a style.

Because styles are filed by exact class, a subclass of a style lives in its own drawer. `FlixelUiModalStyle` extends
`FlixelUiPanelStyle` and `FlixelUiRadioButtonStyle` extends `FlixelUiCheckboxStyle`, yet each is looked up on its own,
so radio buttons can be round while checkboxes are square.

## One-off styles

A widget can skip the skin and wear a style object directly, for a look used exactly once:

```java
FlixelUiLabelStyle warning = new FlixelUiLabelStyle();
warning.color = new FlixelColor(FlixelColor.RED);
warningLabel.setStyle(warning);

warningLabel.setStyle(null); // Back to the skin's style named by getStyleName().
```

Every styled widget has `setStyle(...)` and `getStyle()`.

## Changing styles at runtime

- `widget.setStyleName("other")` switches a widget to another named style right away.
- `ui.setSkin(newSkin)` swaps the whole skin and restyles every widget on the display, parents before children. It
  throws if a widget needs a style the new skin lacks. The old skin is not destroyed for you.
- Editing a style's fields after widgets use it does **not** update them by itself. Call `ui.setSkin(ui.getSkin())`
  to apply the changes.

That last trick also makes a simple theme switcher: keep a light and a dark skin, and call `ui.setSkin(...)` with
the one the player picks.

## Who destroys what

Several styles often share one background, so the skin cannot know which backgrounds a style uses. Instead, hand each
background you load to `skin.track(...)`. It returns its argument, so loading and tracking fit on one line, and
`skin.destroy()` destroys each tracked background exactly once, however many styles share it:

```java
FlixelNineSlice over = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/over.png"), 6, 6, 6, 6));
normal.over = over;
fab.over = over; // Shared; still destroyed once.
```

- A background made with `load(...)` owns its image and releases it in `destroy()`. One made from a `FlixelFrame` you
  passed in leaves that frame's texture alone.
- `FlixelUiColorFill` owns nothing, so tracking it is optional but harmless.
- The display never destroys its skin, because one skin can serve several displays. Destroy it yourself when the UI is
  gone for good, typically in your state's `destroy()`. A destroyed skin is empty and can be filled again.

## Loading images safely in the browser

In browser builds, images are decoded asynchronously. Calling `FlixelNineSlice.load(...)`, `FlixelUiImage.load(...)`,
or anything else that loads a UI image before the file is decoded throws an exception. Preload your UI images in a
loading state, then build the skin:

```java
public class LoadingState extends FlixelState {
  @Override
  public void create() {
    super.create();
    Flixel.assets.load("ui/button.png");
    Flixel.assets.load("ui/panel.png");
  }

  @Override
  public void update(float elapsed) {
    super.update(elapsed);
    if (Flixel.assets.update()) {
      Flixel.switchState(MenuState::new); // MenuState builds the skin in create().
    }
  }
}
```

Desktop and Android load images on the spot, but preloading there costs nothing and keeps one code path.

## Style reference

Required fields are listed first. Unless noted, state variants fall back to the plain field and text colors fall back
to the normal font color.

| Style | Widget | Main fields |
|-------|--------|-------------|
| `FlixelUiLabelStyle` | `FlixelUiLabel` | `font`, `fontSize`, `color` |
| `FlixelUiButtonStyle` | `FlixelUiButton` | `up`; `over`, `down`, `disabled`, `focused`; `font`, `fontSize`, `fontColor`, `overFontColor`, `downFontColor`, `disabledFontColor`; padding; `iconSpacing`, `pressedOffsetY` |
| `FlixelUiCheckboxStyle` | `FlixelUiCheckbox` | `off`, `on`; `offOver`, `onOver`, `offDown`, `onDown`, `offDisabled`, `onDisabled`, `offFocused`, `onFocused`; `font`, `fontSize`, `fontColor`, `disabledFontColor`; `boxSize`, `spacing` |
| `FlixelUiRadioButtonStyle` | `FlixelUiRadioButton` | Same fields as the checkbox style. |
| `FlixelUiSliderStyle` | `FlixelUiSlider` | `track`, `thumb`; `fill`, `thumbOver`, `thumbDown`, `thumbDisabled`; `thumbWidth`, `thumbHeight`, `trackThickness` |
| `FlixelUiDropdownStyle` | `FlixelUiDropdown` | `up`; `over`, `down`, `disabled`, `focused`, `open`; `arrow`, `listBackground`, `itemHighlight`, `itemSelected`; font and colors; field and item padding; `itemHeight`, `listGap` |
| `FlixelTextBoxStyle` | `FlixelTextBox` | `up`; `over`, `focused`, `disabled`; `selection`, `caret`, `caretWidth`, `caretBlinkRate`; `font`, `fontSize`, `fontColor`, `placeholderFontColor`, `disabledFontColor`; padding |
| `FlixelUiPanelStyle` | `FlixelUiPanel` | `background`; padding |
| `FlixelUiModalStyle` | `FlixelUiModal` | Panel fields plus `backdrop` |
| `FlixelUiTooltipStyle` | `FlixelUiTooltip` | `background`, `font`, `fontSize`, `fontColor`, padding, `gap`, `maxWidth` |

Containers, stacks, and pictures have no style. The tooltip style is optional: without one, tooltips draw plain text
with no background. A modal uses a `FlixelUiModalStyle` with its name when the skin has one and falls back to the
`FlixelUiPanelStyle` of the same name otherwise.

## Common mistakes

- **"No FlixelUiCheckboxStyle named 'default' in this skin."** Every styled widget type you use needs a style in the
  skin under its style name. Add one per type.
- **Edited a style but nothing changed.** Call `ui.setSkin(ui.getSkin())` after editing.
- **Images vanish after a state switch.** The skin was destroyed but another display still used it. Destroy a skin
  only when nothing uses it.
- **Memory grows each time a menu opens.** Backgrounds loaded with `load(...)` but never tracked or destroyed. Track
  them with the skin.
- **A nine-slice throws when created.** The insets leave no center (for example `left + right` is at least the image
  width), or the atlas frame was rotated or trimmed.

## Check your understanding

1. Two button styles share one hover nine-slice. How do you make sure it is destroyed exactly once?
2. How can a checkbox style and a radio button style both be named `"default"`?
3. A settings screen lets the player switch to a high-contrast theme. What is the simplest way to apply it?

(Answers: pass it to `skin.track(...)` and destroy the skin; styles are filed by their exact class, and the radio
style is its own class; keep a second skin and call `ui.setSkin(highContrastSkin)`.)

**Next:** [Labels, Buttons, and Pictures](labels-buttons-pictures.md)
