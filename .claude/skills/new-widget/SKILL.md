---
name: new-widget
description: Scaffold a new FlixelGDX UI widget that follows the extension's design rules - no input reading, functional interfaces instead of FlixelObject, FlixelFile asset parameters, anchor and stack layout, and no built-in skin. Use when asked to add or create a new widget.
argument-hint: "<WidgetName> [short description]"
---

# New UI widget

Create the widget described by `$ARGUMENTS`.

## Before writing code

1. Read `AGENTS.md`, especially "UI design rules". They win over anything here.
2. Read the closest existing widget in `flixelgdx-ui/src/main/java/org/flixelgdx/ui/` (for example `FlixelUiButton` for something clickable, `FlixelUiSlider` for something with a value, `FlixelUiDropdown` for something that opens) and copy its structure, not its code.
3. If the widget needs framework APIs, read their source in `../flixelgdx/flixelgdx-core` instead of guessing.

## Rules the widget must follow

- **It never reads input.** Every interaction is a public method the game calls: `hover()`, `press()`, `release()`, `click()`, `open()`, `close()`, `select(int)`, `scroll(float)`, and so on. No `Flixel.mouse`, `Flixel.keys`, touches, gamepads, action sets, or input listeners.
- It extends `FlixelUiWidget` or implements the `org.flixelgdx.functional` interfaces it needs. It never extends `FlixelObject` or `FlixelSprite`.
- Image and font parameters are `FlixelFile`, never `String` paths.
- It is positioned with anchors and can live in a stack container. Do not add another layout system.
- It ships no default skin. Look comes from the game's assets and the skin classes in `skin/`.
- It does not allocate in `update(...)`, `draw(...)`, layout, or hit-testing.
- Name it `FlixelUi<Name>` and give it the standard copyright header.

## Also add

- Javadoc on the class with a short usage example showing how a game wires its input to the widget's methods, since that is the part new users get wrong.
- Headless JUnit 5 tests in `flixelgdx-ui/src/test/java` that drive the widget through its public methods and check its state.
- A mention in the README or guides if they list the available widgets.

Then run the finishing checks from `AGENTS.md` and report what you created.
