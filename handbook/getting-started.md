# Getting Started: Installation and Your First Menu

This guide adds FlixelGDX UI to a game, builds the smallest possible skin, puts a display on screen, and wires a
button to the mouse. By the end you will have a working menu, and you will know what each piece is for.

## Table of contents

1. [What you need](#what-you-need)
2. [Adding the dependency](#adding-the-dependency)
3. [The four pieces of every UI](#the-four-pieces-of-every-ui)
4. [Your first menu](#your-first-menu)
5. [What just happened](#what-just-happened)
6. [Using your own art](#using-your-own-art)
7. [Common mistakes](#common-mistakes)
8. [Check your understanding](#check-your-understanding)

## What you need

- A FlixelGDX game project. The project generator on the FlixelGDX website creates one for you.
- The same version of FlixelGDX UI as your FlixelGDX version. The two are always released together with matching
  version numbers.

## Adding the dependency

FlixelGDX UI is published to Maven Central. Add it to your **core** module's build script, next to the framework:

```gradle
repositories {
  mavenCentral()
}

dependencies {
  implementation "org.flixelgdx:flixelgdx-ui:<flixelgdx-version>"
}
```

With the Kotlin DSL (`build.gradle.kts`), the line takes parentheses:

```kotlin
dependencies {
  implementation("org.flixelgdx:flixelgdx-ui:<flixelgdx-version>")
}
```

If your project uses a version catalog, point both libraries at the same version so they can never drift apart:

```toml
[versions]
flixelgdx = "<flixelgdx-version>"

[libraries]
flixelgdx-core = { module = "org.flixelgdx:flixelgdx-core", version.ref = "flixelgdx" }
flixelgdx-ui = { module = "org.flixelgdx:flixelgdx-ui", version.ref = "flixelgdx" }
```

The extension is pure Java and depends only on `flixelgdx-core`, so it works the same on desktop, in the browser, and
on Android with no extra setup per platform.

## The four pieces of every UI

Every screen built with FlixelGDX UI has the same four pieces. Think of a puppet theater:

| Piece | Class | In the theater |
|-------|-------|----------------|
| The display | `FlixelUiDisplay` | The theater itself: the stage, its layers, and the window the audience looks through. |
| Widgets | `FlixelUiButton`, `FlixelUiLabel`, ... | The puppets. They react when a string is pulled, but never move on their own. |
| The skin | `FlixelUiSkin` | The costume wardrobe. Each puppet picks its outfit from it by name. |
| Your input code | `FlixelUiPointer` or your own calls | The puppeteer. It decides which string to pull, and when. |

The last row is the one that surprises people: **the UI never reads the mouse, keyboard, touch screen, or gamepad by
itself.** Your game reads input as it always does and tells the widgets what happened. That sounds like extra work,
but it means the same button can be clicked with a mouse, tapped with a finger, or chosen with a gamepad, and your
game always decides which. The optional `FlixelUiPointer` does almost all of that wiring for you in five lines.
[Wiring Input](input.md) explains it in full.

## Your first menu

This state shows a title and two buttons in the middle of the screen. It uses plain color fills instead of images,
so it runs with no assets at all.

```java
import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelState;
import org.flixelgdx.input.mouse.FlixelMouseButton;
import org.flixelgdx.ui.FlixelUiButton;
import org.flixelgdx.ui.FlixelUiDisplay;
import org.flixelgdx.ui.FlixelUiLabel;
import org.flixelgdx.ui.FlixelUiPointer;
import org.flixelgdx.ui.FlixelUiStack;
import org.flixelgdx.ui.graphics.FlixelUiColorFill;
import org.flixelgdx.ui.skin.FlixelUiButtonStyle;
import org.flixelgdx.ui.skin.FlixelUiLabelStyle;
import org.flixelgdx.ui.skin.FlixelUiSkin;
import org.flixelgdx.util.FlixelAlign;

public class MenuState extends FlixelState {

  private FlixelUiSkin skin;
  private FlixelUiDisplay ui;
  private FlixelUiPointer pointer;

  @Override
  public void create() {
    super.create();

    // 1. The skin: one style per kind of widget, filed under a name.
    skin = new FlixelUiSkin();

    FlixelUiButtonStyle button = new FlixelUiButtonStyle();
    button.up = skin.track(new FlixelUiColorFill(0.2f, 0.2f, 0.3f, 1f));
    button.over = skin.track(new FlixelUiColorFill(0.3f, 0.3f, 0.5f, 1f));
    button.down = skin.track(new FlixelUiColorFill(0.1f, 0.1f, 0.2f, 1f));
    button.padLeft = button.padRight = 16;
    button.padTop = button.padBottom = 8;
    skin.add("default", button);

    FlixelUiLabelStyle label = new FlixelUiLabelStyle();
    label.fontSize = 32;
    skin.add("default", label);

    // 2. The display, on its own transparent camera above the game.
    ui = new FlixelUiDisplay(FlixelUiDisplay.createHudCamera(), skin);
    add(ui);

    // 3. The widgets: a centered column.
    FlixelUiStack menu = new FlixelUiStack(FlixelUiStack.Direction.VERTICAL);
    menu.setSpacing(8);
    menu.setAlign(FlixelAlign.CENTER);
    menu.anchor(FlixelAlign.CENTER, 0, 0);
    menu.add(new FlixelUiLabel("My Game"));
    menu.add(new FlixelUiButton("Play", b -> Flixel.switchState(PlayState::new)));
    menu.add(new FlixelUiButton("Quit", b -> Flixel.quit()));
    ui.add(menu);

    // 4. The puppeteer: a pointer that turns mouse input into widget calls.
    pointer = new FlixelUiPointer(ui);
  }

  @Override
  public void update(float elapsed) {
    super.update(elapsed);
    pointer.move(Flixel.mouse.getWorldX(ui.getCamera()), Flixel.mouse.getWorldY(ui.getCamera()));
    if (Flixel.mouse.justPressed(FlixelMouseButton.LEFT)) {
      pointer.down();
    }
    if (Flixel.mouse.justReleased(FlixelMouseButton.LEFT)) {
      pointer.up();
    }
    pointer.scroll(Flixel.mouse.getScrollDeltaY());
    Flixel.mouse.icons.setCursor(pointer.getCursor());
  }

  @Override
  public void destroy() {
    super.destroy();
    skin.destroy();
  }
}
```

Run it, move the mouse over a button, and it lights up. Press it and it darkens. Release over it and its click
listener runs. The cursor turns into a pointing hand over the buttons.

## What just happened

- **`FlixelUiDisplay.createHudCamera()`** makes a transparent camera the size of the game, adds it on top of every
  other camera, and returns it. The display draws only on that camera, so the UI stays put while your world camera
  scrolls, shakes, or zooms. Call it in every state's `create()`, because switching states resets the camera list.
- **`add(ui)`** puts the display into the state like any other object, so the state updates and draws it.
- **The skin** holds a `FlixelUiButtonStyle` and a `FlixelUiLabelStyle`, both named `"default"`. Every widget starts
  with the style name `"default"`, so each one found its outfit without being told. A skin can hold many named styles
  per widget type; see [Skins, Styles, and Backgrounds](skins-and-styles.md).
- **`skin.track(...)`** tells the skin to destroy that background when the skin is destroyed. The display never
  destroys the skin, so the state does it in `destroy()`.
- **The stack** placed its three children in a column, centered each one across the column, and was itself anchored
  to the center of the screen. [Core Concepts](core-concepts.md) covers layout.
- **The pointer** found the widget under the mouse each frame and called `hover()`, `press()`, and finally a click on
  it. [Wiring Input](input.md) covers the pointer and how to wire widgets by hand.
- **The font** was left out of both styles, so the framework's built-in font was used. Set a style's `font` field to a
  `FlixelFile`, such as `Flixel.files.internal("fonts/pixel.ttf")`, to use your own.

## Using your own art

Real games use images. Swap the color fills for nine-slices (images whose corners stay sharp when stretched):

```java
button.up = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/button.png"), 6, 6, 6, 6));
button.over = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/button_over.png"), 6, 6, 6, 6));
button.font = Flixel.files.internal("fonts/pixel.ttf");
```

> **Browser builds:** images are decoded asynchronously in the browser, so loading one before it is ready throws an
> exception. Queue your UI images with `Flixel.assets.load(path)` in a loading state, wait until
> `Flixel.assets.update()` returns `true`, and only then build the skin. See
> [Skins, Styles, and Backgrounds](skins-and-styles.md#loading-images-safely-in-the-browser).

## Common mistakes

- **"No FlixelUiButtonStyle named 'default' in this skin."** A widget that needs a style was added before its style
  was in the skin. Add every style first, or pass the filled skin to the display's constructor.
- **The UI does not react to the mouse.** Nothing calls the widgets. Add a `FlixelUiPointer` (or your own calls) in
  `update(...)`.
- **The UI is drawn at the wrong place or not at all after switching states.** The HUD camera from an earlier state
  is gone. Call `FlixelUiDisplay.createHudCamera()` again in each state's `create()`.
- **Clicks land a little off.** The mouse position was not converted for the UI camera. Always use
  `Flixel.mouse.getWorldX(ui.getCamera())` and `getWorldY(ui.getCamera())`.

## Check your understanding

1. Why does pressing a button do nothing until you add a `FlixelUiPointer` or your own input code?
2. Where should the skin be destroyed, and why not by the display?
3. You add a second state with UI. What must its `create()` do again?

(Answers: the UI never reads input, so something must tell widgets what happened; in the state's `destroy()`, because
the display does not own the skin and one skin can be shared by several displays; create a new HUD camera with
`FlixelUiDisplay.createHudCamera()` and a new display on it.)

**Next:** [Core Concepts](core-concepts.md)
