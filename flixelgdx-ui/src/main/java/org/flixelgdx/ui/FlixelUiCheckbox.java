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
import org.flixelgdx.util.FlixelColor;
import org.flixelgdx.util.signal.FlixelSignal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A toggleable widget that draws a box on the left and a label to its right, switching between
 * an unchecked and a checked look when toggled.
 *
 * <p>Think of a checkbox as a light switch. The game flips it by calling {@link #click()} (or
 * {@link #setChecked(boolean)} to set it directly) and the switch snaps to the new position.
 * The checkbox never reads the mouse or keyboard itself; the game decides when a toggle happens.
 *
 * <pre>{@code
 * FlixelUiCheckbox vsync = new FlixelUiCheckbox("V-Sync", settings.isVsync());
 * vsync.onChange.add(cb -> settings.setVsync(cb.isChecked()));
 * vsync.anchor(FlixelAlign.TOP_LEFT, 16, 16);
 * ui.add(vsync);
 *
 * // In the game's own input code:
 * FlixelUiWidget hit = ui.getWidgetAt(mx, my);
 * if (hit == vsync && Flixel.mouse.justReleased(FlixelMouseButton.LEFT)) {
 *   vsync.click(); // Toggles the checkbox and fires onChange.
 * }
 * }</pre>
 *
 * <h2>Checked state</h2>
 *
 * <p>{@link #click()} toggles the checked state and fires {@link #onChange}. It does nothing
 * when the checkbox is disabled. {@link #setChecked(boolean)} sets the state directly and fires
 * {@link #onChange} only when the value changes; pass {@code false} to the {@code notify}
 * overload to suppress the signal. {@link #toggle()} is an alias for {@link #click()}.
 *
 * <h2>Look</h2>
 *
 * <p>The box background is picked from the {@link FlixelUiCheckboxStyle} in the order: disabled,
 * pressed (down), hovered (over), focused, plain. Each of those states picks the {@code on} or
 * {@code off} variant based on whether the checkbox is checked; a missing variant falls back to
 * the plain {@code on} or {@code off} background.
 */
public class FlixelUiCheckbox extends FlixelUiAbstractToggle {

  /**
   * Dispatched by {@link #click()} and by {@link #setChecked(boolean)} (when the value changes),
   * while the checkbox is enabled.
   *
   * <p>The payload is the checkbox itself so one listener can serve several checkboxes without
   * allocating.
   */
  public final FlixelSignal<FlixelUiCheckbox> onChange = new FlixelSignal<>();

  /** The style in use, or {@code null} before one is resolved. */
  @Nullable
  private FlixelUiCheckboxStyle style;

  /** Whether {@link #style} was set via {@link #setStyle(FlixelUiCheckboxStyle)} directly. */
  private boolean customStyle;

  private boolean checked;

  /**
   * Creates an unchecked checkbox with the given label.
   *
   * @param label The label text; {@code null} shows no text.
   */
  public FlixelUiCheckbox(@Nullable CharSequence label) {
    this(label, false);
  }

  /**
   * Creates a checkbox with the given label and initial checked state.
   *
   * @param label The label text; {@code null} shows no text.
   * @param checked {@code true} to start in the checked state.
   */
  public FlixelUiCheckbox(@Nullable CharSequence label, boolean checked) {
    super(label);
    this.checked = checked;
  }

  /**
   * Resolves this checkbox's style from the skin, unless a style was set directly.
   *
   * @throws IllegalArgumentException If the skin has no {@link FlixelUiCheckboxStyle} with this
   *     checkbox's style name.
   */
  @Override
  protected void onStyleChanged() {
    if (!customStyle) {
      style = resolveStyle(FlixelUiCheckboxStyle.class);
    }
    applyStyle();
  }

  /** Invalidates the layout when the new state changes the minimum size of the box. */
  @Override
  protected void onStateChanged() {
    invalidateLayout();
  }

  /**
   * Toggles the checked state and fires {@link #onChange} when the checkbox is enabled.
   *
   * <p>Does nothing when the checkbox is disabled. This is equivalent to calling
   * {@link #toggle()}.
   */
  public void click() {
    if (!isEnabled()) {
      return;
    }
    setChecked(!checked);
  }

  /**
   * Toggles the checked state and fires {@link #onChange} when the checkbox is enabled.
   *
   * <p>This is an alias for {@link #click()}.
   */
  public void toggle() {
    click();
  }

  /**
   * Sets the checked state and fires {@link #onChange} when the value changes.
   *
   * @param value The new checked state.
   */
  public void setChecked(boolean value) {
    setChecked(value, true);
  }

  /**
   * Sets the checked state, optionally firing {@link #onChange}.
   *
   * @param value The new checked state.
   * @param notify {@code true} to fire {@link #onChange} when the value changes.
   */
  public void setChecked(boolean value, boolean notify) {
    if (checked == value) {
      return;
    }
    checked = value;
    onStateChanged();
    if (notify) {
      onChange.dispatch(this);
    }
  }

  /**
   * Returns the background for the current state and checked value.
   *
   * <p>Priority: disabled, pressed, hovered, focused, plain. The {@code on} or {@code off}
   * variant is chosen by {@link #isChecked()}; a missing variant falls back to the plain
   * {@code on} or {@code off} background.
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
    boolean on = checked;
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
   * Destroys this checkbox, its label, and its signals.
   */
  @Override
  public void destroy() {
    onChange.clear();
    super.destroy();
    style = null;
  }

  /**
   * Makes this checkbox use a style object directly instead of looking one up in the skin.
   *
   * <p>Passing {@code null} goes back to the skin's style named {@link #getStyleName()}. The
   * style is applied immediately either way.
   *
   * @param style The style to use, or {@code null} to use the skin again.
   * @throws IllegalArgumentException If {@code style} is {@code null}, the checkbox is on a
   *     display, and the skin has no matching {@link FlixelUiCheckboxStyle}.
   */
  public void setStyle(@Nullable FlixelUiCheckboxStyle style) {
    customStyle = style != null;
    this.style = style;
    if (style == null && display != null) {
      this.style = resolveStyle(FlixelUiCheckboxStyle.class);
    }
    applyStyle();
  }

  /**
   * Returns whether this checkbox is checked.
   *
   * @return {@code true} when checked.
   */
  public boolean isChecked() {
    return checked;
  }

  /**
   * Returns the style this checkbox uses.
   *
   * @return The style, or {@code null} while not on a display and no style was set directly.
   */
  @Nullable
  public FlixelUiCheckboxStyle getStyle() {
    return style;
  }

  @Override
  protected boolean isCheckedState() {
    return checked;
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

  /** Hands the current style's font settings to the label and re-measures. */
  private void applyStyle() {
    FlixelUiCheckboxStyle s = style;
    if (s != null) {
      part.setFormat(s.font, s.fontSize, s.fontColor);
    }
    invalidateLayout();
  }
}
