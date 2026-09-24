<div align="center">

  # FlixelGDX UI

  [![CI](https://github.com/flixelgdx/flixelgdx-ui/actions/workflows/ci_build.yml/badge.svg)](https://github.com/flixelgdx/flixelgdx-ui/actions/workflows/ci_build.yml)
  [![Maven Central](https://img.shields.io/maven-central/v/org.flixelgdx/flixelgdx-ui)](https://central.sonatype.com/artifact/org.flixelgdx/flixelgdx-ui)
  [![JitPack](https://jitpack.io/v/flixelgdx/flixelgdx-ui.svg)](https://jitpack.io/#flixelgdx/flixelgdx-ui)
  [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
  [![FlixelGDX 0.6.4](https://img.shields.io/badge/FlixelGDX-0.6.4-red)](https://github.com/flixelgdx/flixelgdx)
  [![Java 17+](https://img.shields.io/badge/Java-17%2B-orange)](https://adoptium.net/temurin/releases?version=17&os=any&arch=any)

  FlixelGDX UI is a flexible, comprehensive UI extension for the game framework [FlixelGDX](https://github.com/flixelgdx/flixelgdx).
  It's the perfect toolkit for anything from simple main menus to full-blown debug menus.

  Panels, modals, buttons, checkboxes, radio buttons, dropdowns, text boxes, tooltips — everything a menu or HUD
  needs, styled with your own art and driven by your own input.
</div>

> [!NOTE]
> The extension doesn't ship a default skin. Bring your own images and fonts, and it'll handle the rest.

> [!IMPORTANT]
> In browser builds, images load asynchronously. Preload your UI images (for example, in a loading state with
> `Flixel.assets.load(...)`) before building your skin.

---

## Features

FlixelGDX UI gives you a full widget toolkit without taking away control of your game — all made with developer
experience in mind.

### You Control the Input

The UI never reads the mouse, keyboard, or gamepad on its own. Every interaction is a simple method you call, so a
mouse click, a gamepad button and a touch tap can all drive the same button however you like:

```java
@Override
public void update(float elapsed) {
  super.update(elapsed);
  float mx = Flixel.mouse.getWorldX(ui.getCamera());
  float my = Flixel.mouse.getWorldY(ui.getCamera());
  FlixelUiWidget hit = ui.getWidgetAt(mx, my);

  if (hit != hovered) {
    if (hovered != null) hovered.unhover();
    if (hit != null) hit.hover();
    hovered = hit;
  }
  if (Flixel.mouse.justReleased(FlixelMouseButton.LEFT) && hit instanceof FlixelUiButton button) {
    button.click();
  }

  // The same button can be pressed from a gamepad, too.
  if (controls.accept.justPressed() && ui.getFocused() instanceof FlixelUiButton button) {
    button.click();
  }
}
```

Writing that glue for every widget type gets old fast, so there's an optional `FlixelUiPointer` that does it for
you. You still read the input, and the pointer turns it into hover, press, click, tooltip, dropdown and scroll calls:

```java
pointer = new FlixelUiPointer(ui); // In create().

// In update():
pointer.move(Flixel.mouse.getWorldX(ui.getCamera()), Flixel.mouse.getWorldY(ui.getCamera()));
if (Flixel.mouse.justPressed(FlixelMouseButton.LEFT)) pointer.down();
if (Flixel.mouse.justReleased(FlixelMouseButton.LEFT)) pointer.up();
pointer.scroll(Flixel.mouse.getScrollDeltaY());
```

Because the game feeds it, the same pointer works with a touch screen or a gamepad-driven cursor. Custom widgets can
react to it by overriding `onActivate(x, y)` and `onScroll(amount)`.

### Skins

A skin is a collection of styles, one for each widget type. Load your art once, and every widget dresses itself
automatically:

```java
FlixelUiSkin skin = new FlixelUiSkin();

FlixelUiButtonStyle button = new FlixelUiButtonStyle();
button.up = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/button.png"), 6, 6, 6, 6));
button.over = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/button_over.png"), 6, 6, 6, 6));
button.font = Flixel.files.internal("fonts/pixel.ttf");
skin.add("default", button);

// Need a second look? Add it under a different name.
FlixelUiButtonStyle danger = new FlixelUiButtonStyle();
danger.up = skin.track(new FlixelUiColorFill(0.8f, 0.1f, 0.1f, 1f));
skin.add("danger", danger);

// Put the UI on its own camera, on top of your game.
FlixelUiDisplay ui = new FlixelUiDisplay(FlixelUiDisplay.createHudCamera(), skin);
add(ui);

FlixelUiButton delete = new FlixelUiButton("Delete Save");
delete.setStyleName("danger");
```

### Simple Layouts

Layouts are built with just two tools: anchors, which pin a widget to a spot inside its parent, and stacks, which
line up their children in a row or column:

```java
// A column of centered buttons, in the middle of the screen.
FlixelUiStack menu = new FlixelUiStack(FlixelUiStack.Direction.VERTICAL);
menu.setSpacing(8);
menu.setAlign(FlixelAlign.CENTER);       // Center each child inside the column.
menu.anchor(FlixelAlign.CENTER, 0, 0);   // Center the column on the screen.
menu.add(new FlixelUiLabel("Main Menu"));
menu.add(new FlixelUiButton("Play", b -> Flixel.switchState(() -> new PlayState())));
menu.add(new FlixelUiButton("Quit", b -> Flixel.quit()));
ui.add(menu);

// A floating action button, pinned 24 pixels in from the bottom-right corner.
FlixelUiButton fab = new FlixelUiButton(Flixel.files.internal("ui/plus.png"), b -> openEditor());
fab.setStyleName("fab");
fab.anchor(FlixelAlign.BOTTOM_RIGHT, -24, -24);
ui.add(fab);
```

### Every Widget You Need

The extension provides a wide set of tools to construct and build UI with ease, from simple buttons to dropdown menus:

```java
// Checkboxes.
FlixelUiCheckbox vsync = new FlixelUiCheckbox("V-Sync", true);
vsync.onChange.add(c -> Flixel.graphics.setVSync(c.isChecked()));

// Radio buttons, where only one in the group can be picked.
FlixelUiRadioGroup difficulty = new FlixelUiRadioGroup();
FlixelUiRadioButton easy = new FlixelUiRadioButton("Easy", difficulty);
FlixelUiRadioButton hard = new FlixelUiRadioButton("Hard", difficulty);
difficulty.onChange.add(g -> setDifficulty(g.getSelectedIndex()));

// Dropdowns.
FlixelUiDropdown resolution = new FlixelUiDropdown(200);
resolution.setPlaceholder("Pick a resolution");
resolution.addItem("1280 x 720");
resolution.addItem("1920 x 1080");
resolution.onSelect.add(d -> applyResolution(d.getSelectedIndex()));

// Tooltips.
playButton.setTooltip("Start a new game.");
ui.showTooltip(playButton, 0.4f); // Show it after a short delay.
```

### Modals

Open a window above everything else, with a dimmed backdrop behind it. Modals can even stack on top of each
other:

```java
FlixelUiModal confirm = new FlixelUiModal(300, 160);
confirm.destroyOnClose = true;

FlixelUiStack content = new FlixelUiStack(FlixelUiStack.Direction.VERTICAL);
content.setSpacing(12);
content.setAlign(FlixelAlign.CENTER);
content.anchor(FlixelAlign.CENTER, 0, 0);
content.add(new FlixelUiLabel("Are you sure?"));
content.add(new FlixelUiButton("Yes", b -> confirm.close()));
confirm.add(content);

ui.openModal(confirm);

// Want Escape to close it? You decide.
if (Flixel.keys.justPressed(FlixelKey.ESCAPE)) {
  ui.closeModal();
}
```

### Text Boxes

Single-line and multi-line text boxes come with everything players expect — a caret, selection, copy and paste,
Ctrl+Backspace, IME support for languages like Japanese and Chinese, input filters, and password masks:

```java
FlixelTextBox name = new FlixelTextBox(220);
name.setPlaceholder("Player name");
name.setMaxLength(16);
name.setFilter(FlixelTextFilter.ALPHANUMERIC);
name.onSubmit.add(t -> saveName(t.getText().toString()));
ui.add(name);

FlixelTextBox pin = new FlixelTextBox(120);
pin.setPasswordChar('*');
pin.setFilter(FlixelTextFilter.DIGITS);

// Focus a text box to start typing into it. That's it!
name.focus();
```

---

## Installation

FlixelGDX UI is published to Maven Central, and its version always matches the framework's version. Make sure
Maven Central is in your repositories (projects made with the project generator already have it):

```gradle
repositories {
  mavenCentral()
}
```

Then add it with just one simple line inside your core module's build script:

```gradle
dependencies {
  implementation "org.flixelgdx:flixelgdx-ui:<flixelgdx-version>"
}
```

Using the Kotlin DSL (`build.gradle.kts`) instead? It's the same line with parentheses:

```kotlin
dependencies {
  implementation("org.flixelgdx:flixelgdx-ui:<flixelgdx-version>")
}
```

### Version catalogs

If your project uses a version catalog, add the extension next to the framework in `gradle/libs.versions.toml`,
so both always share the same version:

```toml
[versions]
flixelgdx = "<flixelgdx-version>"

[libraries]
flixelgdx-core = { module = "org.flixelgdx:flixelgdx-core", version.ref = "flixelgdx" }
flixelgdx-ui = { module = "org.flixelgdx:flixelgdx-ui", version.ref = "flixelgdx" }
```

```kotlin
dependencies {
  implementation(libs.flixelgdx.core)
  implementation(libs.flixelgdx.ui)
}
```

---

## Project navigation

- **[Compiling & Testing](COMPILING.md)**: How to build the extension and test it against a local copy of the framework.
- **[Contributing Guide](CONTRIBUTING.md)**: Coding standards, PR requirements, and how to contribute.
- **[Code of Conduct](CODE_OF_CONDUCT.md)**: Rules set in place for a stable open source community.
- **[Project Roles](GOVERNANCE.md)**: How each role for the project operates, including project leaders and maintainers.
