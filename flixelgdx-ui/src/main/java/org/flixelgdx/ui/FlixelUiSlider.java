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

import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.math.FlixelMath;
import org.flixelgdx.signal.FlixelSignal;
import org.flixelgdx.ui.graphics.FlixelUiBackground;
import org.flixelgdx.ui.skin.FlixelUiSliderStyle;
import org.flixelgdx.util.FlixelColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A draggable slider that picks a number between a minimum and a maximum, such as a volume or
 * brightness control.
 *
 * <p>Think of a slider as a dimmer switch on a wall. The track is the groove, the thumb is the
 * knob that slides along it, and the value is how bright the light is. The game moves the knob by
 * telling the slider where the pointer is; the slider never reads the mouse, a touch screen, or
 * the keyboard itself.
 *
 * <pre>{@code
 * FlixelUiSlider volume = new FlixelUiSlider(200, 0f, 1f, 0.8f);
 * volume.setStep(0.05f); // Optional: snap to multiples of 0.05.
 * volume.onChange.add(s -> audio.setVolume(s.getValue()));
 * volume.anchor(FlixelAlign.TOP_LEFT, 16, 16);
 * ui.add(volume);
 *
 * // In the game's own input code (or let a FlixelUiPointer do it for you):
 * if (Flixel.mouse.justPressed(FlixelMouseButton.LEFT) && ui.getWidgetAt(mx, my) == volume) {
 *   volume.pointerDown(mx, my);
 * }
 * if (Flixel.mouse.pressed(FlixelMouseButton.LEFT)) volume.pointerDrag(mx, my);
 * if (Flixel.mouse.justReleased(FlixelMouseButton.LEFT)) volume.pointerUp();
 * }</pre>
 *
 * <h2>Value</h2>
 *
 * <p>The value always stays between {@link #getMin()} and {@link #getMax()}. When
 * {@link #setStep(float)} is greater than zero the value also snaps to {@code min + n * step}.
 * {@link #setValue(float)} fires {@link #onChange} only when the (clamped, snapped) value actually
 * changes; pass {@code false} to {@link #setValue(float, boolean)} to suppress the signal.
 * {@link #stepUp()} and {@link #stepDown()} nudge the value by one step (or a tenth of the range
 * when there is no step), which suits keyboard and gamepad navigation that the game handles.
 *
 * <h2>Orientation</h2>
 *
 * <p>A horizontal slider (the default) grows from left to right. A vertical slider grows from the
 * bottom up, like a mixing-desk fader. The constructor length is the size along the slider's
 * axis; the other size comes from the style unless the game sets it.
 *
 * <h2>Look</h2>
 *
 * <p>The track, optional fill, and thumb come from the {@link FlixelUiSliderStyle} in the
 * display's skin whose name matches {@link #getStyleName()}, or from a style passed to
 * {@link #setStyle(FlixelUiSliderStyle)}. The thumb shows its disabled, pressed, hovered, or plain
 * variant, in that priority.
 */
public class FlixelUiSlider extends FlixelUiWidget {

  /**
   * Dispatched when the value changes through {@link #setValue(float)}, {@link #stepUp()},
   * {@link #stepDown()}, or the pointer methods.
   *
   * <p>The payload is the slider itself so one listener can serve several sliders without
   * allocating.
   */
  public final FlixelSignal<FlixelUiSlider> onChange = new FlixelSignal<>();

  private float min;
  private float max;
  private float value;
  private float step;

  /** The style in use, or {@code null} before one is resolved. */
  @Nullable
  private FlixelUiSliderStyle style;

  /** Whether {@link #style} was set via {@link #setStyle(FlixelUiSliderStyle)} directly. */
  private boolean customStyle;

  private boolean vertical;
  private boolean dragging;

  /**
   * Creates a horizontal slider from {@code 0} to {@code 1} with a value of {@code 0}.
   *
   * @param length The width in pixels.
   */
  public FlixelUiSlider(float length) {
    this(length, 0f, 1f, 0f);
  }

  /**
   * Creates a horizontal slider.
   *
   * @param length The width in pixels.
   * @param min The smallest value.
   * @param max The largest value.
   * @param value The starting value; clamped into the range.
   * @throws IllegalArgumentException If {@code min} is greater than {@code max}.
   */
  public FlixelUiSlider(float length, float min, float max, float value) {
    this(length, min, max, value, false);
  }

  /**
   * Creates a slider.
   *
   * @param length The size along the slider's axis in pixels (width when horizontal, height when
   *     vertical).
   * @param min The smallest value.
   * @param max The largest value.
   * @param value The starting value; clamped into the range.
   * @param vertical {@code true} for a slider that grows from the bottom up.
   * @throws IllegalArgumentException If {@code min} is greater than {@code max}.
   */
  public FlixelUiSlider(float length, float min, float max, float value, boolean vertical) {
    super(vertical ? 0f : length, vertical ? length : 0f);
    if (min > max) {
      throw new IllegalArgumentException("A slider's minimum must not be greater than its maximum.");
    }
    this.min = min;
    this.max = max;
    this.value = FlixelMath.clamp(value, min, max);
    this.vertical = vertical;
  }

  /**
   * Resolves this slider's style from the skin, unless a style was set directly.
   *
   * @throws IllegalArgumentException If the skin has no {@link FlixelUiSliderStyle} with this
   *     slider's style name.
   */
  @Override
  protected void onStyleChanged() {
    if (!customStyle) {
      style = resolveStyle(FlixelUiSliderStyle.class);
    }
    invalidateLayout();
  }

  /** Fills in the cross size from the style when the game did not set one. */
  @Override
  protected void onMeasure() {
    FlixelUiSliderStyle s = style;
    if (s == null) {
      return;
    }
    float cross = Math.max(vertical ? s.thumbWidth : s.thumbHeight, s.trackThickness);
    if (vertical) {
      if (width <= 0f) {
        width = cross;
      }
    } else if (height <= 0f) {
      height = cross;
    }
  }

  /**
   * Draws the track, the filled portion, and the thumb.
   *
   * @param batch The batch to draw into.
   * @param drawX The left edge in draw coordinates.
   * @param drawY The top edge in draw coordinates.
   * @param alpha The combined alpha of this slider and its ancestors.
   */
  @Override
  protected void drawSelf(@NotNull FlixelBatch batch, float drawX, float drawY, float alpha) {
    FlixelUiSliderStyle s = style;
    if (s == null) {
      return;
    }
    FlixelColor tint = getColor();
    batch.setColor(tint.r, tint.g, tint.b, alpha);
    float thickness = s.trackThickness > 0f ? s.trackThickness : (vertical ? width : height);
    float thumbLen = vertical ? s.thumbHeight : s.thumbWidth;
    float offset = getThumbOffset();
    if (vertical) {
      float trackX = drawX + (width - thickness) * 0.5f;
      if (s.track != null) {
        s.track.draw(batch, trackX, drawY, thickness, height);
      }
      if (s.fill != null) {
        float top = offset + thumbLen * 0.5f;
        s.fill.draw(batch, trackX, drawY + top, thickness, height - top);
      }
    } else {
      float trackY = drawY + (height - thickness) * 0.5f;
      if (s.track != null) {
        s.track.draw(batch, drawX, trackY, width, thickness);
      }
      if (s.fill != null) {
        s.fill.draw(batch, drawX, trackY, offset + thumbLen * 0.5f, thickness);
      }
    }
    FlixelUiBackground thumb = getThumb();
    if (thumb != null) {
      if (vertical) {
        thumb.draw(batch, drawX + (width - s.thumbWidth) * 0.5f, drawY + offset, s.thumbWidth, s.thumbHeight);
      } else {
        thumb.draw(batch, drawX + offset, drawY + (height - s.thumbHeight) * 0.5f, s.thumbWidth, s.thumbHeight);
      }
    }
    batch.setColor(FlixelColor.WHITE);
  }

  /**
   * Starts a drag at a point and moves the thumb there, as when the pointer goes down on the
   * slider.
   *
   * <p>The slider becomes pressed and the value jumps so the thumb is centered under the point
   * (clamped to the ends). Follow up with {@link #pointerDrag(float, float)} while the pointer
   * stays down and {@link #pointerUp()} on release. Does nothing when the slider is disabled.
   *
   * @param x The X coordinate in the display camera's view space.
   * @param y The Y coordinate in the display camera's view space.
   */
  public void pointerDown(float x, float y) {
    if (!isEnabled()) {
      return;
    }
    dragging = true;
    press();
    setValue(valueAt(x, y));
  }

  /**
   * Moves the thumb to a point while the pointer is held down.
   *
   * <p>The point may be outside the slider, so a drag keeps working when the pointer strays off
   * the track; the value simply stays at the nearest end. Does nothing unless a drag started
   * with {@link #pointerDown(float, float)}.
   *
   * @param x The X coordinate in the display camera's view space.
   * @param y The Y coordinate in the display camera's view space.
   */
  public void pointerDrag(float x, float y) {
    if (!dragging || !isEnabled()) {
      return;
    }
    setValue(valueAt(x, y));
  }

  /** Ends a drag and releases the slider; the value stays where it is. */
  public void pointerUp() {
    if (!dragging) {
      return;
    }
    dragging = false;
    release();
  }

  /**
   * Returns the value that a point maps to, without changing the slider.
   *
   * <p>The result is measured so the thumb's center sits under the point, then clamped and
   * snapped exactly like {@link #setValue(float)} would. It uses {@link #getScreenX()} and
   * {@link #getScreenY()}, so the slider must have been laid out.
   *
   * @param x The X coordinate in the display camera's view space.
   * @param y The Y coordinate in the display camera's view space.
   * @return The value under the point.
   */
  public float valueAt(float x, float y) {
    float thumbLen = getThumbLength();
    float travel = (vertical ? height : width) - thumbLen;
    float t = 0f;
    if (travel > 0f) {
      float pos = (vertical ? y - screenY : x - screenX) - thumbLen * 0.5f;
      t = FlixelMath.clamp(pos / travel, 0f, 1f);
    }
    float ratio = vertical ? 1f - t : t;
    return conform(min + ratio * (max - min));
  }

  /**
   * Moves the value up by one step when the slider is enabled.
   *
   * <p>One step is {@link #getStep()}, or a tenth of the range when no step is set.
   */
  public void stepUp() {
    if (isEnabled()) {
      setValue(value + getIncrement());
    }
  }

  /**
   * Moves the value down by one step when the slider is enabled.
   *
   * @see #stepUp()
   */
  public void stepDown() {
    if (isEnabled()) {
      setValue(value - getIncrement());
    }
  }

  /**
   * Stops any drag in progress before the slider is disabled.
   *
   * @param enabled {@code true} to enable, {@code false} to disable.
   */
  @Override
  public void setEnabled(boolean enabled) {
    if (!enabled) {
      dragging = false;
    }
    super.setEnabled(enabled);
  }

  /**
   * Destroys this slider and its signals.
   */
  @Override
  public void destroy() {
    onChange.clear();
    super.destroy();
    style = null;
  }

  /**
   * Sets the value and fires {@link #onChange} when it changes.
   *
   * <p>The value is snapped to the step (when there is one) and clamped into the range first.
   *
   * @param value The new value.
   */
  public void setValue(float value) {
    setValue(value, true);
  }

  /**
   * Sets the value, optionally firing {@link #onChange}.
   *
   * @param value The new value.
   * @param notify {@code true} to fire {@link #onChange} when the value changes.
   */
  public void setValue(float value, boolean notify) {
    float next = conform(value);
    if (next == this.value) {
      return;
    }
    this.value = next;
    if (notify) {
      onChange.dispatch(this);
    }
  }

  /**
   * Sets the value from a position along the slider, where {@code 0} is the minimum and {@code 1}
   * is the maximum.
   *
   * @param ratio The position, clamped to {@code [0, 1]}.
   */
  public void setRatio(float ratio) {
    setValue(min + FlixelMath.clamp(ratio, 0f, 1f) * (max - min));
  }

  /**
   * Sets the smallest and largest values, clamping the current value into the new range.
   *
   * <p>{@link #onChange} fires when the clamp changes the value.
   *
   * @param min The smallest value.
   * @param max The largest value.
   * @throws IllegalArgumentException If {@code min} is greater than {@code max}.
   */
  public void setRange(float min, float max) {
    if (min > max) {
      throw new IllegalArgumentException("A slider's minimum must not be greater than its maximum.");
    }
    this.min = min;
    this.max = max;
    setValue(value);
  }

  /**
   * Sets the step the value snaps to.
   *
   * <p>The value snaps to {@code min + n * step}. A step of {@code 0} or less turns snapping off.
   * The current value is snapped right away and {@link #onChange} fires if that changes it.
   *
   * @param step The step size.
   */
  public void setStep(float step) {
    this.step = Math.max(0f, step);
    setValue(value);
  }

  /**
   * Sets the slider's orientation, swapping its width and height.
   *
   * @param vertical {@code true} to grow from the bottom up, {@code false} for left to right.
   */
  public void setVertical(boolean vertical) {
    if (this.vertical == vertical) {
      return;
    }
    this.vertical = vertical;
    float w = width;
    width = height;
    height = w;
    invalidateLayout();
  }

  /**
   * Makes this slider use a style object directly instead of looking one up in the skin.
   *
   * <p>Passing {@code null} goes back to the skin's style named {@link #getStyleName()}.
   *
   * @param style The style to use, or {@code null} to use the skin again.
   * @throws IllegalArgumentException If {@code style} is {@code null}, the slider is on a
   *     display, and the skin has no matching {@link FlixelUiSliderStyle}.
   */
  public void setStyle(@Nullable FlixelUiSliderStyle style) {
    customStyle = style != null;
    this.style = style;
    if (style == null && display != null) {
      this.style = resolveStyle(FlixelUiSliderStyle.class);
    }
    invalidateLayout();
  }

  /**
   * Returns the current value.
   *
   * @return A value in {@code [getMin(), getMax()]}.
   */
  public float getValue() {
    return value;
  }

  /**
   * Returns the current value as a position along the slider.
   *
   * @return {@code 0} at the minimum and {@code 1} at the maximum; {@code 0} when the range is
   *     empty.
   */
  public float getRatio() {
    return max > min ? (value - min) / (max - min) : 0f;
  }

  public float getMin() {
    return min;
  }

  public float getMax() {
    return max;
  }

  public float getStep() {
    return step;
  }

  public boolean isVertical() {
    return vertical;
  }

  /**
   * Returns whether a pointer drag is in progress.
   *
   * @return {@code true} between {@link #pointerDown(float, float)} and {@link #pointerUp()}.
   */
  public boolean isDragging() {
    return dragging;
  }

  /**
   * Returns the style this slider uses.
   *
   * @return The style, or {@code null} while not on a display and no style was set directly.
   */
  @Nullable
  public FlixelUiSliderStyle getStyle() {
    return style;
  }

  /**
   * Returns the distance from the slider's start edge to the thumb's start edge, in pixels.
   *
   * <p>For a horizontal slider that is measured from the left; for a vertical one it is measured
   * from the top.
   */
  private float getThumbOffset() {
    float travel = (vertical ? height : width) - getThumbLength();
    if (travel <= 0f) {
      return 0f;
    }
    float ratio = getRatio();
    return travel * (vertical ? 1f - ratio : ratio);
  }

  private float getThumbLength() {
    FlixelUiSliderStyle s = style;
    if (s == null) {
      return 0f;
    }
    return vertical ? s.thumbHeight : s.thumbWidth;
  }

  private float getIncrement() {
    return step > 0f ? step : (max - min) * 0.1f;
  }

  @Nullable
  private FlixelUiBackground getThumb() {
    FlixelUiSliderStyle s = style;
    if (s == null) {
      return null;
    }
    FlixelUiBackground bg;
    if (!isEnabled()) {
      bg = s.thumbDisabled;
    } else if (isPressed()) {
      bg = s.thumbDown;
    } else if (isHovered()) {
      bg = s.thumbOver;
    } else {
      bg = null;
    }
    return bg != null ? bg : s.thumb;
  }

  /** Snaps a value to the step and clamps it into the range. */
  private float conform(float v) {
    float result = v;
    if (step > 0f) {
      result = min + Math.round((v - min) / step) * step;
    }
    return FlixelMath.clamp(result, min, max);
  }
}
