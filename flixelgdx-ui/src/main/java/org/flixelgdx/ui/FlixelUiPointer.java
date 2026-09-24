/*
 * MIT License
 *
 * Copyright (c) 2026 stringdotjar
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.flixelgdx.ui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Turns a position and a "down" or "up" that the game reports into the hover, press, click, and
 * tooltip calls a desktop-style UI expects.
 *
 * <p>The UI never reads input, so every game has to connect its mouse (or finger, or virtual
 * cursor) to the widgets itself. Most games want the same thing: highlight what is under the
 * cursor, show its tooltip after a short pause, push a button in while the button is held, and
 * click it only if the release happens over the same button. The pointer is that glue, written
 * once. Think of it as a translator standing between the game and the UI: the game tells it
 * "the cursor is here" and "the button went down", and it tells each widget what that means for
 * it.
 *
 * <p>The pointer still reads no input. The game decides where the pointer is and what counts as
 * a press, so the same pointer works for a mouse, a touch screen, or a cursor moved with a
 * gamepad stick. Using it is optional: a game can keep calling {@link FlixelUiWidget#hover()},
 * {@link FlixelUiButton#click()}, and friends directly.
 *
 * <pre>{@code
 * // In create():
 * pointer = new FlixelUiPointer(ui);
 *
 * // In update():
 * pointer.move(Flixel.mouse.getWorldX(ui.getCamera()), Flixel.mouse.getWorldY(ui.getCamera()));
 * if (Flixel.mouse.justPressed(FlixelMouseButton.LEFT)) pointer.down();
 * if (Flixel.mouse.justReleased(FlixelMouseButton.LEFT)) pointer.up();
 * pointer.scroll(Flixel.mouse.getScrollDeltaY());
 * }</pre>
 *
 * <h2>What each call does</h2>
 *
 * <ul>
 *   <li>{@link #move(float, float)} finds the widget under the point with
 *       {@link FlixelUiDisplay#getWidgetAt(float, float)}, unhovers the old one, hovers the new
 *       one, starts its tooltip countdown, and highlights the row under the point in an open
 *       dropdown list.
 *   <li>{@link #down()} presses the hovered widget, hides the tooltip, blurs the focused widget
 *       when the press lands somewhere else, and closes any open dropdown list that was not
 *       pressed.
 *   <li>{@link #up()} releases the pressed widget and, when the pointer is still over it,
 *       activates it: buttons click, checkboxes toggle, radio buttons select, dropdowns open or
 *       close, list rows are selected, and text boxes take focus and move their caret. Custom
 *       widgets join in by overriding {@link FlixelUiWidget#onActivate(float, float)}.
 *   <li>{@link #scroll(float)} offers the scroll to the hovered widget and then to each of its
 *       ancestors until one uses it (see {@link FlixelUiWidget#onScroll(float)}).
 * </ul>
 *
 * <p>{@link #down()}, {@link #up()}, and {@link #scroll(float)} act on the widget found by the
 * most recent {@link #move(float, float)}, so call {@code move(...)} first every frame. None of
 * the methods allocate.
 *
 * <h2>Letting the UI swallow clicks</h2>
 *
 * <p>{@link #getHovered()} is {@code null} when the pointer is not over any interactive widget.
 * A game can check it before treating a click as a world click, so pressing a button does not
 * also fire the player's weapon.
 */
public final class FlixelUiPointer {

  /**
   * Seconds the pointer must rest on a widget before its tooltip appears.
   *
   * <p>Defaults to {@code 0.4}. Use {@code 0} to show tooltips immediately.
   */
  public float tooltipDelay = 0.4f;

  /** The X passed to the most recent {@link #move(float, float)}. */
  private float x;

  /** The Y passed to the most recent {@link #move(float, float)}. */
  private float y;

  /** The display this pointer drives. */
  @NotNull
  private final FlixelUiDisplay display;

  /** The widget under the pointer, or {@code null}. */
  @Nullable
  private FlixelUiWidget hovered;

  /** The widget that received the last {@link #down()} and has not been released, or {@code null}. */
  @Nullable
  private FlixelUiWidget pressed;

  /**
   * Creates a pointer that drives the widgets of one display.
   *
   * @param display The display whose widgets this pointer hovers, presses, and activates.
   * @throws IllegalArgumentException If {@code display} is {@code null}.
   */
  public FlixelUiPointer(@NotNull FlixelUiDisplay display) {
    if (display == null) {
      throw new IllegalArgumentException("A pointer's display must not be null.");
    }
    this.display = display;
  }

  /**
   * Moves the pointer, updating which widget is hovered.
   *
   * <p>When the widget under the point changes, the old one is unhovered (and its tooltip hidden)
   * before the new one is hovered. The new widget's tooltip countdown starts only while nothing is
   * pressed, so holding a button down does not pop tooltips up over other widgets. Over an open
   * dropdown list, the row under the point is highlighted.
   *
   * @param x The pointer's X in the display camera's view space (as from
   *     {@code Flixel.mouse.getWorldX(ui.getCamera())}).
   * @param y The pointer's Y in the display camera's view space.
   */
  public void move(float x, float y) {
    this.x = x;
    this.y = y;
    dropDetached();
    FlixelUiWidget hit = display.getWidgetAt(x, y);
    FlixelUiWidget old = hovered;
    if (hit != old) {
      if (old != null) {
        old.unhover();
        if (display.getTooltipTarget() == old) {
          display.hideTooltip();
        }
      }
      hovered = hit;
      if (hit != null) {
        hit.hover();
        if (pressed == null && hit.hasTooltip()) {
          display.showTooltip(hit, tooltipDelay);
        }
      }
    }
    if (hit instanceof FlixelUiDropdownList list) {
      FlixelUiDropdown dropdown = list.dropdown;
      int row = dropdown.getItemAt(x, y);
      if (row >= 0 && row != dropdown.getHighlightedIndex()) {
        dropdown.highlight(row);
      }
    }
  }

  /**
   * Presses the hovered widget, as when a mouse button goes down.
   *
   * <p>The tooltip is hidden, a focused widget other than the hovered one is blurred, and any open
   * dropdown list is closed unless the press landed on that list or its dropdown. Pressing empty
   * space does all of that and presses nothing. Does nothing more while a widget is already
   * pressed.
   */
  public void down() {
    dropDetached();
    if (pressed != null) {
      return;
    }
    FlixelUiWidget hit = hovered;
    display.hideTooltip();
    closeOtherLists(hit);
    FlixelUiWidget focused = display.getFocused();
    if (focused != null && focused != hit) {
      focused.blur();
    }
    if (hit != null) {
      pressed = hit;
      hit.press();
    }
  }

  /**
   * Releases the pressed widget, activating it when the pointer is still over it.
   *
   * <p>A press that started on one widget and ended on another only releases the first one, the
   * same way a desktop button is not clicked when the cursor is dragged off it before letting go.
   * Does nothing when nothing is pressed.
   */
  public void up() {
    dropDetached();
    FlixelUiWidget p = pressed;
    if (p == null) {
      return;
    }
    pressed = null;
    p.release();
    // A release handler may have removed or disabled the widget, so check again before activating.
    if (p == hovered && p.getDisplay() == display && p.isEnabled()) {
      p.onActivate(x, y);
    }
  }

  /**
   * Scrolls whatever is under the pointer.
   *
   * <p>The hovered widget gets the first chance through {@link FlixelUiWidget#onScroll(float)};
   * when it does not use the scroll, its parent gets a chance, and so on up the tree. An open
   * dropdown list scrolls by rows and a multi-line text box scrolls by lines.
   *
   * @param amount How far to scroll; positive values scroll toward the end. Passing {@code 0}
   *     does nothing.
   * @return {@code true} when a widget used the scroll, so the game can skip its own handling
   *     (such as zooming the world camera).
   */
  public boolean scroll(float amount) {
    if (amount == 0f) {
      return false;
    }
    dropDetached();
    for (FlixelUiWidget w = hovered; w != null; w = w.parent) {
      if (w.isEnabled() && w.onScroll(amount)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Releases the pressed widget without activating it.
   *
   * <p>Use it when a press should be abandoned, for example when the window loses focus or a
   * touch is canceled by the system. Does nothing when nothing is pressed.
   */
  public void cancel() {
    dropDetached();
    FlixelUiWidget p = pressed;
    if (p == null) {
      return;
    }
    pressed = null;
    p.release();
  }

  /**
   * Forgets everything the pointer is tracking: the pressed widget is released without being
   * activated, the hovered widget is unhovered, and its tooltip is hidden.
   *
   * <p>Call it when the pointer leaves the game window or when the UI is rebuilt.
   */
  public void reset() {
    cancel();
    FlixelUiWidget h = hovered;
    if (h == null) {
      return;
    }
    hovered = null;
    h.unhover();
    if (display.getTooltipTarget() == h) {
      display.hideTooltip();
    }
  }

  /**
   * Stops tracking widgets that left this pointer's display.
   *
   * <p>A widget that was removed or destroyed is no longer on the display, so the pointer drops it
   * without calling anything on it.
   */
  private void dropDetached() {
    FlixelUiWidget h = hovered;
    if (h != null && h.getDisplay() != display) {
      hovered = null;
    }
    FlixelUiWidget p = pressed;
    if (p != null && p.getDisplay() != display) {
      pressed = null;
    }
  }

  /**
   * Closes every open dropdown list except the one that is being pressed (or whose dropdown field
   * is being pressed, since releasing on the field toggles the list itself).
   *
   * @param hit The widget being pressed, or {@code null} for empty space.
   */
  private void closeOtherLists(@Nullable FlixelUiWidget hit) {
    FlixelUiContainer popups = display.getPopupLayer();
    // Walk backwards because closing a dropdown removes its list from the popup layer.
    for (int i = popups.getChildCount() - 1; i >= 0; i--) {
      if (popups.getChildAt(i) instanceof FlixelUiDropdownList list
          && hit != list
          && hit != list.dropdown) {
        list.dropdown.close();
      }
    }
  }

  /**
   * Returns the widget under the pointer.
   *
   * @return The hovered widget, or {@code null} when the pointer is not over any interactive
   *     widget.
   */
  @Nullable
  public FlixelUiWidget getHovered() {
    return hovered;
  }

  /**
   * Returns the widget currently held down.
   *
   * @return The pressed widget, or {@code null} when nothing is pressed.
   */
  @Nullable
  public FlixelUiWidget getPressed() {
    return pressed;
  }

  public float getX() {
    return x;
  }

  public float getY() {
    return y;
  }

  @NotNull
  public FlixelUiDisplay getDisplay() {
    return display;
  }
}
