# "How Do I...?" Cookbook

Complete recipes for common screens, followed by short answers to common questions. Each answer links to the guide
that explains it fully. The recipes assume a state with a `ui` display, a `pointer`, and a skin that has a `"default"`
style for every widget type used, as built in [Getting Started](getting-started.md).

## Table of contents

1. [Recipe: a main menu](#recipe-a-main-menu)
2. [Recipe: a pause menu](#recipe-a-pause-menu)
3. [Recipe: a settings screen](#recipe-a-settings-screen)
4. [Recipe: a reusable confirm dialog](#recipe-a-reusable-confirm-dialog)
5. [Recipe: a name entry box](#recipe-a-name-entry-box)
6. [Recipe: a scrolling list](#recipe-a-scrolling-list)
7. [Quick answers](#quick-answers)

## Recipe: a main menu

```java
FlixelUiStack menu = new FlixelUiStack(FlixelUiStack.Direction.VERTICAL);
menu.setWidth(200);  // A fixed width for the buttons to stretch across; the height still follows the content.
menu.setSpacing(8);
menu.setAlign(FlixelAlign.CENTER);
menu.anchor(FlixelAlign.CENTER, 0, 40);

FlixelUiLabel title = new FlixelUiLabel("My Game");
title.setStyleName("title");
menu.add(title);

FlixelUiButton play = new FlixelUiButton("Play", b -> Flixel.switchState(PlayState::new));
FlixelUiButton options = new FlixelUiButton("Options", b -> Flixel.switchState(OptionsState::new));
FlixelUiButton quit = new FlixelUiButton("Quit", b -> Flixel.quit());
play.setPercentSize(1f, Float.NaN);    // Every button as wide as the column.
options.setPercentSize(1f, Float.NaN);
quit.setPercentSize(1f, Float.NaN);
menu.add(play);
menu.add(options);
menu.add(quit);
ui.add(menu);
```

The column needs its fixed width because an auto-sized stack ignores percent-sized children on that axis, so a column
whose buttons all stretch would have nothing to size itself from. Add keyboard or gamepad focus with the pattern in
[Wiring Input](input.md#keyboard-and-gamepad-navigation).

## Recipe: a pause menu

The pause menu is a modal on the gameplay state's own display. While it is open, the state updates only the UI, so
the world freezes behind the dimmed backdrop.

```java
private FlixelUiModal pauseMenu;

// In create(), after the display and pointer exist:
pauseMenu = new FlixelUiModal(240, 200);
FlixelUiStack column = new FlixelUiStack(FlixelUiStack.Direction.VERTICAL);
column.setSpacing(8);
column.setAlign(FlixelAlign.CENTER);
column.anchor(FlixelAlign.CENTER, 0, 0);
column.add(new FlixelUiLabel("Paused"));
column.add(new FlixelUiButton("Resume", b -> pauseMenu.close()));
column.add(new FlixelUiButton("Restart", b -> Flixel.resetState()));
column.add(new FlixelUiButton("Quit to Menu", b -> Flixel.switchState(MenuState::new)));
pauseMenu.add(column);

@Override
public void update(float elapsed) {
  if (Flixel.keys.justPressed(FlixelKey.ESCAPE)) {
    if (pauseMenu.isOpen()) {
      pauseMenu.close();
    } else {
      ui.openModal(pauseMenu);
    }
  }

  if (pauseMenu.isOpen()) {
    ui.update(elapsed); // Only the UI runs; the world stays frozen.
  } else {
    super.update(elapsed);
  }

  pointer.move(Flixel.mouse.getWorldX(ui.getCamera()), Flixel.mouse.getWorldY(ui.getCamera()));
  if (Flixel.mouse.justPressed(FlixelMouseButton.LEFT)) {
    pointer.down();
  }
  if (Flixel.mouse.justReleased(FlixelMouseButton.LEFT)) {
    pointer.up();
  }
  Flixel.mouse.icons.setCursor(pointer.getCursor());
}

@Override
public void onFocusLost() {
  super.onFocusLost();
  if (!pauseMenu.isOpen()) {
    ui.openModal(pauseMenu); // Pause when the player tabs out.
  }
}
```

The modal is not destroyed on close, so it is built once and reused. Check `pointer.getHovered() == null` before
treating clicks as gameplay clicks.

## Recipe: a settings screen

A panel holds a column of rows. Saved values are loaded silently first, and the listeners added afterward save every
change.

```java
FlixelUiPanel window = new FlixelUiPanel(360, 300);
window.anchor(FlixelAlign.CENTER, 0, 0);

FlixelUiStack column = new FlixelUiStack(FlixelUiStack.Direction.VERTICAL);
column.setSpacing(12);
column.add(new FlixelUiLabel("Settings"));

// Volume: a label and a slider side by side.
FlixelUiStack volumeRow = new FlixelUiStack(FlixelUiStack.Direction.HORIZONTAL);
volumeRow.setSpacing(8);
volumeRow.setAlign(FlixelAlign.CENTER);
FlixelUiSlider volume = new FlixelUiSlider(160, 0f, 1f, 1f);
volume.setStep(0.1f);
volumeRow.add(new FlixelUiLabel("Volume"));
volumeRow.add(volume);
column.add(volumeRow);

FlixelUiCheckbox fullscreen = new FlixelUiCheckbox("Fullscreen");
column.add(fullscreen);

FlixelUiDropdown language = new FlixelUiDropdown(160);
language.addItem("English");
language.addItem("Espanol");
language.addItem("Deutsch");
column.add(language);

FlixelUiRadioGroup difficulty = new FlixelUiRadioGroup();
FlixelUiStack difficultyRow = new FlixelUiStack(FlixelUiStack.Direction.HORIZONTAL);
difficultyRow.setSpacing(12);
difficultyRow.add(new FlixelUiRadioButton("Easy", difficulty));
difficultyRow.add(new FlixelUiRadioButton("Normal", difficulty));
difficultyRow.add(new FlixelUiRadioButton("Hard", difficulty));
column.add(difficultyRow);

column.add(new FlixelUiButton("Back", b -> Flixel.switchState(MenuState::new)));
window.add(column);
ui.add(window);

// 1. Show the saved values without firing any listener.
volume.setValue(settings.volume, false);
fullscreen.setChecked(settings.fullscreen, false);
language.setSelectedIndex(settings.language);
difficulty.setSelectedIndex(settings.difficulty);

// 2. Save every change from now on.
volume.onChange.add(s -> settings.volume = s.getValue());
fullscreen.onChange.add(c -> settings.fullscreen = c.isChecked());
language.onSelect.add(d -> settings.language = d.getSelectedIndex());
difficulty.onChange.add(g -> settings.difficulty = g.getSelectedIndex());
```

Here `settings` stands for your game's own settings object. Write it to `Flixel.save` when the player leaves the
screen.

## Recipe: a reusable confirm dialog

A helper that builds a one-shot dialog and runs an action only on "Yes":

```java
private void confirm(CharSequence question, Runnable onYes) {
  FlixelUiModal dialog = new FlixelUiModal(300, 140);
  dialog.destroyOnClose = true; // Built per question, gone after the answer.

  FlixelUiStack content = new FlixelUiStack(FlixelUiStack.Direction.VERTICAL);
  content.setSpacing(16);
  content.setAlign(FlixelAlign.CENTER);
  content.anchor(FlixelAlign.CENTER, 0, 0);

  FlixelUiLabel text = new FlixelUiLabel(question);
  text.setWrapWidth(260);
  text.setAlignment(FlixelText.Alignment.CENTER);
  content.add(text);

  FlixelUiStack buttons = new FlixelUiStack(FlixelUiStack.Direction.HORIZONTAL);
  buttons.setSpacing(8);
  buttons.add(new FlixelUiButton("Yes", b -> {
    dialog.close();
    onYes.run();
  }));
  buttons.add(new FlixelUiButton("No", b -> dialog.close()));
  content.add(buttons);

  dialog.add(content);
  ui.openModal(dialog);
}

// Usage:
confirm("Delete this save? This cannot be undone.", () -> deleteSave(slot));
```

The dialog is created when the player asks, not every frame, so the allocation is fine.

## Recipe: a name entry box

```java
FlixelUiStack form = new FlixelUiStack(FlixelUiStack.Direction.VERTICAL);
form.setSpacing(8);
form.setAlign(FlixelAlign.CENTER);
form.anchor(FlixelAlign.CENTER, 0, 0);

FlixelTextBox name = new FlixelTextBox(220);
name.setPlaceholder("Your name");
name.setMaxLength(16);
name.setFilter(FlixelTextFilter.ALPHANUMERIC);
name.setText(save.playerName);
name.onSubmit.add(t -> {
  save.playerName = t.getText().toString();
  Flixel.switchState(PlayState::new);
});

FlixelUiButton ok = new FlixelUiButton("OK", b -> name.submit());

form.add(new FlixelUiLabel("Enter your name"));
form.add(name);
form.add(ok);
ui.add(form);

name.focus(); // Ready to type immediately.
```

Enter and the OK button both go through `onSubmit`, so the save logic lives in one place.

## Recipe: a scrolling list

A container that clips its children and moves a stack of rows when scrolled. Overriding `onScroll(...)` lets a
`FlixelUiPointer` scroll it when the wheel moves over any row:

```java
public class ScrollList extends FlixelUiContainer {

  private final FlixelUiStack rows = new FlixelUiStack(FlixelUiStack.Direction.VERTICAL);
  private final float rowHeight;
  private float offset;

  public ScrollList(float width, float height, float rowHeight) {
    super(width, height);
    this.rowHeight = rowHeight;
    interactive = true; // Scrolling over the gaps between rows works too.
    setClipChildren(true);
    rows.setPercentSize(1f, Float.NaN);
    add(rows);
  }

  public FlixelUiStack getRows() {
    return rows;
  }

  @Override
  protected boolean onScroll(float amount) {
    float max = Math.max(0f, rows.getHeight() - getContentHeight());
    offset = Math.min(Math.max(offset + amount * rowHeight, 0f), max);
    rows.setPosition(0f, -offset);
    return true;
  }
}

// Usage:
ScrollList levels = new ScrollList(220, 200, 32);
levels.anchor(FlixelAlign.CENTER, 0, 0);
for (int i = 0; i < levelNames.length; i++) {
  FlixelUiButton row = new FlixelUiButton(levelNames[i], b -> startLevel(b.getText()));
  row.setPercentSize(1f, Float.NaN);
  levels.getRows().add(row);
}
ui.add(levels);
```

The loop builds the screen once in `create()`, so its allocations are fine. Rows scrolled out of view are hidden and
cannot be clicked, because clipping also applies to hit-testing.

## Quick answers

**...show UI above the game world?**
`new FlixelUiDisplay(FlixelUiDisplay.createHudCamera(), skin)` and `add(...)` it in every state's `create()`.
([Getting Started](getting-started.md))

**...pin something to a corner?**
`widget.anchor(FlixelAlign.TOP_RIGHT, -8, 8);` ([Core Concepts](core-concepts.md#anchors))

**...line widgets up in a row or column?**
A `FlixelUiStack` with `Direction.HORIZONTAL` or `Direction.VERTICAL`. ([Core Concepts](core-concepts.md#stacks))

**...make the mouse work with my UI?**
A `FlixelUiPointer`: `move(...)`, `down()`, and `up()` every frame. ([Wiring Input](input.md))

**...stop UI clicks from also hitting the game?**
Only handle a world click when `pointer.getHovered() == null`. ([Wiring Input](input.md#keeping-clicks-from-reaching-the-game-world))

**...navigate a menu with a keyboard or gamepad?**
Move focus with `focus()` and activate `ui.getFocused()`. ([Wiring Input](input.md#keyboard-and-gamepad-navigation))

**...change the cursor over buttons?**
`Flixel.mouse.icons.setCursor(pointer.getCursor());` every frame. ([Wiring Input](input.md#mouse-cursors))

**...give one button a different look?**
Add another style under a new name and call `button.setStyleName("danger")`.
([Skins, Styles, and Backgrounds](skins-and-styles.md#skins-and-named-styles))

**...switch themes while the game runs?**
`ui.setSkin(otherSkin);` ([Skins, Styles, and Backgrounds](skins-and-styles.md#changing-styles-at-runtime))

**...update a label every frame without garbage?**
Pass a reused `FlixelString` to `setText(...)`. ([Labels, Buttons, and Pictures](labels-buttons-pictures.md#text-that-changes-every-frame))

**...disable a button?**
`button.setEnabled(false);`

**...hide a widget without leaving a gap in its stack?**
`widget.kill();`, and `revive()` to bring it back. ([Core Concepts](core-concepts.md#showing-hiding-fading-and-destroying))

**...fade in a whole window?**
Tween the panel's alpha; children fade with it. ([Panels, Modals, and Tooltips](panels-modals-tooltips.md#opening-closing-and-stacking-modals))

**...close a dialog with Escape or a backdrop click?**
Check for it yourself and call `ui.closeModal()`. ([Panels, Modals, and Tooltips](panels-modals-tooltips.md#closing-on-a-click-outside-or-a-back-button))

**...add a hover hint?**
`widget.setTooltip("...")`; a pointer shows it automatically. ([Panels, Modals, and Tooltips](panels-modals-tooltips.md#tooltips))

**...only allow numbers in a text box?**
`box.setFilter(FlixelTextFilter.DIGITS);` ([Text Boxes](text-boxes.md#filters-limits-and-passwords))

**...make a password field?**
`box.setPasswordChar('*');` ([Text Boxes](text-boxes.md#filters-limits-and-passwords))

**...load UI images in a browser build without errors?**
Preload them with `Flixel.assets.load(...)` in a loading state first.
([Skins, Styles, and Backgrounds](skins-and-styles.md#loading-images-safely-in-the-browser))

**...draw something no built-in widget draws?**
Extend `FlixelUiWidget` and implement `drawSelf(...)`. ([Custom Widgets and Debugging](custom-widgets.md))
