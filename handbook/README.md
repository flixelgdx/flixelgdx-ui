# The FlixelGDX UI Handbook

Welcome! This handbook is the guide to FlixelGDX UI, the widget toolkit for FlixelGDX games. It explains how to put
menus, settings screens, dialogs, and HUD controls on screen, how to style them with your own art, and how to connect
them to your game's mouse, keyboard, touch, and gamepad input. Every guide uses the real API, in plain language, with
short examples you can copy.

## Table of contents

1. [How to use this handbook](#how-to-use-this-handbook)
2. [Suggested reading paths](#suggested-reading-paths)
3. [The guides](#the-guides)
4. [Other documentation](#other-documentation)

## How to use this handbook

- **Every guide stands on its own.** Each one starts with its own table of contents and links to related guides, so
  you can jump straight to what you need.
- **Guides follow the same shape:** why the feature exists, the mental model, how to use it with real code, common
  mistakes, and a short "Check your understanding" quiz with answers.
- **Code examples use the real FlixelGDX UI API.** Shorter snippets assume the usual imports and a display stored in
  a field called `ui`. Widget classes live in `org.flixelgdx.ui`, backgrounds in `org.flixelgdx.ui.graphics`, styles
  and skins in `org.flixelgdx.ui.skin`, and text boxes in `org.flixelgdx.ui.text`.
- **The Javadoc goes deeper.** Every class and package is documented in detail. This handbook is the map; the
  Javadoc is the street-level view.
- **New to FlixelGDX itself?** Read the framework's own handbook first, at least the parts on states, cameras, and
  input. This handbook assumes you know what a `FlixelState` is.

## Suggested reading paths

**Building your first menu:** read [Getting Started](getting-started.md), then [Core Concepts](core-concepts.md) and
[Wiring Input](input.md). After that, dip into the widget guides as you need each widget.

**Making it look good:** [Skins, Styles, and Backgrounds](skins-and-styles.md) explains how your art reaches every
widget.

**In a hurry:** the [Cookbook](cookbook.md) has complete recipes for a main menu, a pause menu, a settings screen, a
confirm dialog, and more.

## The guides

| # | Guide | What you will learn |
|---|-------|---------------------|
| 1 | [Getting Started: Installation and Your First Menu](getting-started.md) | Adding the dependency, building a skin, creating a display, and a working button. |
| 2 | [Core Concepts: The Display, Widgets, and Layout](core-concepts.md) | The widget tree, layers, coordinates, anchors, stacks, padding, clipping, and the widget lifecycle. |
| 3 | [Wiring Input: Pointers, Focus, and Your Own Controls](input.md) | Why the UI never reads input, the pointer, wiring by hand, keyboard and gamepad focus, scrolling, and cursors. |
| 4 | [Skins, Styles, and Backgrounds](skins-and-styles.md) | Nine-slices, images, color fills, style classes, named styles, and who destroys what. |
| 5 | [Labels, Buttons, and Pictures](labels-buttons-pictures.md) | Text, clickable buttons with text and icons, floating action buttons, and images. |
| 6 | [Checkboxes and Radio Buttons](checkboxes-and-radio-buttons.md) | On and off settings, groups where only one can be picked, and their signals. |
| 7 | [Sliders and Dropdowns](sliders-and-dropdowns.md) | Picking a number from a range and picking one item from a list. |
| 8 | [Text Boxes](text-boxes.md) | Typing, focus, filters, passwords, multi-line text, selection, and the clipboard. |
| 9 | [Panels, Modals, and Tooltips](panels-modals-tooltips.md) | Windows with backgrounds, dialogs above everything else, and hover hints. |
| 10 | [Custom Widgets and Debugging](custom-widgets.md) | Writing your own widget, its hooks, and how the UI shows up in the debugger. |
| 11 | ["How Do I...?" Cookbook](cookbook.md) | Complete recipes for common screens and quick answers to common questions. |

## Other documentation

- **The Javadoc.** Each package has a detailed overview in its `package-info.java`, starting with `org.flixelgdx.ui`.
- **[`README.md`](../README.md)**: the project overview and feature tour.
- **[`COMPILING.md`](../COMPILING.md)**: building the extension and testing it against a local copy of the framework.
- **[`CONTRIBUTING.md`](../CONTRIBUTING.md)**: how to contribute to the extension.

Have fun building great menus!
