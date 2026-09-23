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

import org.flixelgdx.ui.graphics.FlixelUiBackground;
import org.flixelgdx.ui.skin.FlixelUiCheckboxStyle;
import org.flixelgdx.ui.skin.FlixelUiRadioButtonStyle;
import org.flixelgdx.util.FlixelColor;
import org.flixelgdx.util.signal.FlixelSignal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A mutually exclusive toggle that belongs to a {@link FlixelUiRadioGroup}: selecting one button
 * in the group deselects the others.
 *
 * <p>Think of a car radio. Pressing preset 3 makes preset 3 light up and all the others go dark.
 * Pressing preset 3 again does nothing; it is already on. The radio button never reads the mouse
 * or keyboard itself; the game calls {@link #click()} or {@link #select()}, or the game drives
 * the group directly with {@link FlixelUiRadioGroup#select(int)}.
 *
 * <pre>{@code
 * FlixelUiRadioGroup difficulty = new FlixelUiRadioGroup();
 * FlixelUiRadioButton easy   = new FlixelUiRadioButton("Easy",   difficulty);
 * FlixelUiRadioButton medium = new FlixelUiRadioButton("Medium", difficulty);
 * FlixelUiRadioButton hard   = new FlixelUiRadioButton("Hard",   difficulty);
 * difficulty.select(0);
 * difficulty.onChange.add(g -> applyDifficulty(g.getSelected()));
 * ui.add(easy);
 * ui.add(medium);
 * ui.add(hard);
 *
 * // In the game's input code:
 * FlixelUiWidget hit = ui.getWidgetAt(mx, my);
 * if (hit instanceof FlixelUiRadioButton rb && Flixel.mouse.justReleased(FlixelMouseButton.LEFT)) {
 *   rb.click(); // Selects this button; ignored if it is already selected or disabled.
 * }
 * }</pre>
 *
 * <h2>Signals</h2>
 *
 * <p>{@link #onChange} is dispatched on both the deselected and the newly selected button when
 * the selection changes. The deselected button fires first; the group's
 * {@link FlixelUiRadioGroup#onChange} fires last, after both.
 *
 * <h2>Look</h2>
 *
 * <p>The box background is picked from a {@link FlixelUiRadioButtonStyle} using the same
 * priority as {@link FlixelUiCheckbox}: disabled, pressed, hovered, focused, plain; with
 * {@code on} or {@code off} variants based on selection.
 */
public class FlixelUiRadioButton extends FlixelUiAbstractToggle {

  /**
   * Dispatched when this button's selected state changes: once when it is deselected, and once
   * when it is selected, before the group's {@link FlixelUiRadioGroup#onChange}.
   *
   * <p>The payload is this button so one listener can serve several buttons without allocating.
   */
  public final FlixelSignal<FlixelUiRadioButton> onChange = new FlixelSignal<>();

  /** The style in use, or {@code null} before one is resolved. */
  @Nullable
  private FlixelUiRadioButtonStyle style;

  /** The group this button belongs to, or {@code null} when it has none. */
  @Nullable
  private FlixelUiRadioGroup group;

  /** Whether {@link #style} was set via {@link #setStyle(FlixelUiRadioButtonStyle)} directly. */
  private boolean customStyle;

  private boolean selected;

  /**
   * Creates an unselected radio button and adds it to a group.
   *
   * @param label The label text; {@code null} shows no text.
   * @param group The group to join.
   * @throws IllegalArgumentException If {@code group} is {@code null}.
   */
  public FlixelUiRadioButton(@Nullable CharSequence label, @NotNull FlixelUiRadioGroup group) {
    super(label);
    if (group == null) {
      throw new IllegalArgumentException("A radio button's group must not be null.");
    }
    group.add(this);
  }

  /**
   * Resolves this button's style from the skin, unless a style was set directly.
   *
   * @throws IllegalArgumentException If the skin has no {@link FlixelUiRadioButtonStyle} with
   *     this button's style name.
   */
  @Override
  protected void onStyleChanged() {
    if (!customStyle) {
      style = resolveStyle(FlixelUiRadioButtonStyle.class);
    }
    applyStyle();
  }

  /** Invalidates the layout when the new state changes the minimum size of the box. */
  @Override
  protected void onStateChanged() {
    invalidateLayout();
  }

  /**
   * Selects this button in its group, doing nothing when it is already selected or disabled.
   *
   * <p>Clicking a radio button that is already selected has no effect: a selected radio cannot
   * be deselected by clicking it again.
   */
  public void click() {
    if (!isEnabled() || selected) {
      return;
    }
    select();
  }

  /**
   * Selects this button in its group, doing nothing when it is already selected or disabled.
   *
   * <p>This is the explicit form of {@link #click()}.
   */
  public void select() {
    if (!isEnabled() || selected) {
      return;
    }
    FlixelUiRadioGroup g = group;
    if (g != null) {
      g.select(this, true);
    } else {
      setSelectedInternal(true, true);
    }
  }

  /**
   * Returns the background for the current state and selected value.
   *
   * <p>Priority: disabled, pressed, hovered, focused, plain; {@code on} or {@code off} by
   * {@link #isSelected()}. Missing variants fall back to {@link FlixelUiCheckboxStyle#on} or
   * {@link FlixelUiCheckboxStyle#off}.
   *
   * @return The background to draw, or {@code null} when there is no style.
   */
  @Override
  @Nullable
  public FlixelUiBackground getBackground() {
    FlixelUiCheckboxStyle s = style;
    if (s == null) {
      return null;
    }
    boolean on = selected;
    FlixelUiBackground bg;
    if (!isEnabled()) {
      bg = on ? s.onDisabled : s.offDisabled;
    } else if (isPressed()) {
      bg = on ? s.onDown : s.offDown;
    } else if (isHovered()) {
      bg = on ? s.onOver : s.offOver;
    } else if (isFocused()) {
      bg = on ? s.onFocused : s.offFocused;
    } else {
      bg = on ? s.on : s.off;
    }
    if (bg == null) {
      bg = on ? s.on : s.off;
    }
    return bg;
  }

  /**
   * Returns the label text color for the current state.
   *
   * @return The color; white when the style has none.
   */
  @Override
  @NotNull
  public FlixelColor getFontColor() {
    FlixelUiCheckboxStyle s = style;
    if (s == null) {
      return FlixelColor.WHITE;
    }
    if (!isEnabled() && s.disabledFontColor != null) {
      return s.disabledFontColor;
    }
    FlixelColor c = s.fontColor;
    return c != null ? c : FlixelColor.WHITE;
  }

  /**
   * Destroys this button, its label, and its signals.
   *
   * <p>The button is removed from its group silently; the group's {@link #onChange} does not
   * fire.
   */
  @Override
  public void destroy() {
    onChange.clear();
    FlixelUiRadioGroup g = group;
    if (g != null) {
      group = null;
      g.remove(this);
    }
    super.destroy();
    style = null;
  }

  /**
   * Makes this button use a style object directly instead of looking one up in the skin.
   *
   * <p>Passing {@code null} goes back to the skin's style named {@link #getStyleName()}. The
   * style is applied immediately either way.
   *
   * @param style The style to use, or {@code null} to use the skin again.
   * @throws IllegalArgumentException If {@code style} is {@code null}, the button is on a
   *     display, and the skin has no matching {@link FlixelUiRadioButtonStyle}.
   */
  public void setStyle(@Nullable FlixelUiRadioButtonStyle style) {
    customStyle = style != null;
    this.style = style;
    if (style == null && display != null) {
      this.style = resolveStyle(FlixelUiRadioButtonStyle.class);
    }
    applyStyle();
  }

  /**
   * Returns whether this button is currently selected.
   *
   * @return {@code true} when selected.
   */
  public boolean isSelected() {
    return selected;
  }

  /**
   * Returns the group this button belongs to.
   *
   * @return The group, or {@code null} after the button is removed from its group.
   */
  @Nullable
  public FlixelUiRadioGroup getGroup() {
    return group;
  }

  /**
   * Returns the style this button uses.
   *
   * @return The style, or {@code null} while not on a display and no style was set directly.
   */
  @Nullable
  public FlixelUiRadioButtonStyle getStyle() {
    return style;
  }

  @Override
  protected boolean isCheckedState() {
    return selected;
  }

  @Override
  protected float getSpacing() {
    FlixelUiCheckboxStyle s = style;
    return s != null ? s.spacing : 8f;
  }

  @Override
  protected float getRawBoxSize() {
    FlixelUiCheckboxStyle s = style;
    return s != null ? s.boxSize : 0f;
  }

  /** Sets the selected state directly, optionally firing {@link #onChange}. */
  void setSelectedInternal(boolean value, boolean notify) {
    if (selected == value) {
      return;
    }
    selected = value;
    onStateChanged();
    if (notify) {
      onChange.dispatch(this);
    }
  }

  /** Called by {@link FlixelUiRadioGroup#add(FlixelUiRadioButton)} to set the group reference. */
  void setGroup(@Nullable FlixelUiRadioGroup g) {
    group = g;
  }

  /** Hands the current style's font settings to the label and re-measures. */
  private void applyStyle() {
    FlixelUiCheckboxStyle s = style;
    if (s != null) {
      part.setFormat(s.font, s.fontSize, s.fontColor);
    }
    invalidateLayout();
  }
}
