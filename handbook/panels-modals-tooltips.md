# Panels, Modals, and Tooltips

This guide covers windows with backgrounds (`FlixelUiPanel`), dialogs that open above everything else
(`FlixelUiModal`), and the hover hints that explain what a widget does (tooltips).

## Table of contents

1. [Panels](#panels)
2. [Modals](#modals)
3. [Opening, closing, and stacking modals](#opening-closing-and-stacking-modals)
4. [Closing on a click outside or a back button](#closing-on-a-click-outside-or-a-back-button)
5. [How panels and modals look](#how-panels-and-modals-look)
6. [Tooltips](#tooltips)
7. [Common mistakes](#common-mistakes)
8. [Check your understanding](#check-your-understanding)

## Panels

A panel is a tray: the tray itself is what you see (its background), and whatever you put on it moves with it. It is a
container that draws its style's background behind its children, which are laid out like in any container: anchored,
percent-sized, or inside a stack.

```java
FlixelUiPanel window = new FlixelUiPanel(320, 240);
window.anchor(FlixelAlign.CENTER, 0, 0);

FlixelUiStack column = new FlixelUiStack(FlixelUiStack.Direction.VERTICAL);
column.setSpacing(8);
column.add(new FlixelUiLabel("Settings"));
column.add(new FlixelUiButton("Back", b -> Flixel.switchState(MenuState::new)));

window.add(column);
ui.add(window);
```

- **Padding** comes from the style until you call `setPadding(...)` yourself; from then on your padding wins, even if
  the style changes. `isCustomPadding()` tells you which applies.
- **Clicks.** Unlike a plain container, a panel is interactive: clicking its empty part hits the panel, so clicks never
  reach the buttons hidden behind a window. Set `window.interactive = false` to let them through.
- **No background.** A panel style with no background draws nothing, which makes an invisible click blocker.

## Modals

A modal is a dialog that opens above every root-layer widget, with a backdrop that dims the screen behind it. Think of
a spotlight on stage: everything outside it goes dark, and the audience cannot reach the shadowed parts. While any
modal is open, only the topmost one can be hit, so nothing behind it can be clicked.

```java
FlixelUiModal confirm = new FlixelUiModal(320, 160); // Centered by default.

FlixelUiStack content = new FlixelUiStack(FlixelUiStack.Direction.VERTICAL);
content.setSpacing(16);
content.setAlign(FlixelAlign.CENTER);
content.anchor(FlixelAlign.CENTER, 0, 0);
content.add(new FlixelUiLabel("Quit to the menu?"));

FlixelUiStack buttons = new FlixelUiStack(FlixelUiStack.Direction.HORIZONTAL);
buttons.setSpacing(8);
buttons.add(new FlixelUiButton("Yes", b -> Flixel.switchState(MenuState::new)));
buttons.add(new FlixelUiButton("No", b -> confirm.close()));
content.add(buttons);

confirm.add(content);
ui.openModal(confirm);
```

`FlixelUiModal` is a panel, so everything about panels applies. It is anchored to the center when created; change the
anchor any time.

## Opening, closing, and stacking modals

| Call | What it does |
|------|--------------|
| `ui.openModal(modal)` | Opens it on top of any other open modal and fires its `onOpen`. Opening an already open modal just moves it to the top. |
| `ui.closeModal()` | Closes the top modal. |
| `ui.closeModal(modal)` or `modal.close()` | Closes that modal, wherever it is in the stack. |
| `ui.getTopModal()`, `ui.getModalCount()`, `ui.isModalOpen()` | Ask what is open. |
| `modal.isOpen()` | Whether that modal is open. |

Closing a modal hides a tooltip that belongs to it, clears the hovered and pressed state of everything in it (without
firing `onRelease`, since closing is not a click), removes it, and fires `onClose`.

Modals stack. Opening a "Are you sure?" dialog from inside a settings dialog dims the screen a little more and blocks
the settings dialog until the new one closes.

**Reusing or discarding.** A closed modal is kept so it can be opened again. For one-shot dialogs, set
`modal.destroyOnClose = true`, and it destroys itself (and everything in it) right after `onClose`.

**Animating.** `onOpen` fires after the modal is attached and laid out, which is the right moment to start a tween:

```java
confirm.onOpen.add(m -> {
  m.setAlpha(0f);
  FlixelTween.tween(m, new FlixelTweenSettings()
      .addGoal(m::getAlpha, 1f, m::setAlpha)
      .setDuration(0.15f));
});
```

## Closing on a click outside or a back button

The UI never closes a modal by itself: there is no built-in "click the backdrop to close" or "Escape closes". Your game
decides. While a modal is open, `ui.getWidgetAt(...)` returns `null` for a point outside the top modal, so:

```java
if (ui.isModalOpen()) {
  if (Flixel.mouse.justPressed(FlixelMouseButton.LEFT) && ui.getWidgetAt(mx, my) == null) {
    ui.closeModal(); // Clicked the backdrop.
  }
  if (Flixel.keys.justPressed(FlixelKey.ESCAPE)) {
    ui.closeModal(); // Back button.
  }
}
```

`ui.isModalOpen()` is also handy for pausing gameplay input while a dialog is up.

## How panels and modals look

```java
FlixelUiPanelStyle window = new FlixelUiPanelStyle();
window.background = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/panel.png"), 8, 8, 8, 8));
window.padLeft = window.padTop = window.padRight = window.padBottom = 12;
skin.add("default", window);

FlixelUiModalStyle dialog = new FlixelUiModalStyle();
dialog.background = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/dialog.png"), 8, 8, 8, 8));
dialog.padLeft = dialog.padTop = dialog.padRight = dialog.padBottom = 16;
dialog.backdrop = skin.track(new FlixelUiColorFill(0f, 0f, 0f, 0.6f)); // 60 percent black.
skin.add("default", dialog);
```

Match the padding to the nine-slice's border so children never overlap the frame. A modal uses the
`FlixelUiModalStyle` with its style name when the skin has one; otherwise it falls back to the `FlixelUiPanelStyle` of
that name and draws no backdrop. `setStyle(...)` and `setModalStyle(...)` give one modal its own look.

## Tooltips

A tooltip is a small bubble explaining a widget. Any widget can have one, and the display owns a single shared bubble
on its own layer above everything else.

```java
playButton.setTooltip("Start a new game.");
volumeSlider.setTooltip("Music and sound effects volume.");
```

With a `FlixelUiPointer`, that is all: hovering shows the tooltip after `pointer.tooltipDelay` seconds, and moving off
or pressing hides it. By hand:

| Call | What it does |
|------|--------------|
| `ui.showTooltip(widget)` | Shows the widget's tooltip now. |
| `ui.showTooltip(widget, delay)` | Shows it after `delay` seconds, counted down by the display's update. |
| `ui.hideTooltip()` | Hides it and cancels any countdown. |
| `ui.isTooltipVisible()`, `ui.getTooltipTarget()` | Ask what is showing. |

Showing does nothing for a widget without tooltip text, and the tooltip hides itself when its widget is removed,
killed, or hidden. `setTooltip(null)` clears a widget's text, and `hasTooltip()` and `getTooltip()` read it. The text
is copied, so passing a reused `FlixelString` is safe.

The bubble appears centered below its widget, flips above it when there is no room below, and always stays on screen.
Its look comes from a `FlixelUiTooltipStyle` named by `ui.setTooltipStyle(name)` (`"default"` unless changed). The
style is optional; without one, the tooltip is plain text with no background.

```java
FlixelUiTooltipStyle tip = new FlixelUiTooltipStyle();
tip.background = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/tooltip.png"), 4, 4, 4, 4));
tip.fontSize = 14;
tip.padLeft = tip.padRight = 6;
tip.padTop = tip.padBottom = 4;
tip.gap = 4;        // Distance from the widget.
tip.maxWidth = 200; // Wrap long text; 0 keeps it on one line.
skin.add("default", tip);
```

`ui.getTooltip()` returns the bubble widget itself, for fading it in with a tween. The display manages its text,
position, and visibility, so only change its looks.

## Common mistakes

- **Buttons behind a window still react.** The window is a plain container. Use a `FlixelUiPanel`, which blocks clicks.
- **The modal never closes on Escape or a backdrop click.** The UI never does that by itself; add the check shown
  above.
- **Closed dialogs pile up in memory.** Set `destroyOnClose = true` on one-shot modals.
- **The modal has no dimmed backdrop.** The skin has only a `FlixelUiPanelStyle` under that name. Add a
  `FlixelUiModalStyle` with a `backdrop`.
- **Tooltips never appear without a pointer.** The display only counts down while it updates, and something has to
  call `showTooltip(...)`.

## Check your understanding

1. What makes a click on the empty part of a dialog stop at the dialog?
2. How do you make a "Discard changes?" dialog disappear and free its memory after it is answered?
3. A tooltip should wrap at 180 pixels. Where do you set that?

(Answers: hit-testing returns the modal itself for empty space inside it, and nothing outside the top modal is
searched; set `destroyOnClose = true` and close it from its buttons; `maxWidth = 180` on the tooltip style.)

**Next:** [Custom Widgets and Debugging](custom-widgets.md)
