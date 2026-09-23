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
import org.flixelgdx.ui.graphics.FlixelUiBackground;
import org.flixelgdx.ui.skin.FlixelUiCheckboxStyle;
import org.flixelgdx.util.FlixelColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shared drawing and layout logic for {@link FlixelUiCheckbox} and {@link FlixelUiRadioButton}.
 *
 * <p>Both widgets render a box on the left and a label vertically centered to its right. This
 * base class handles that drawing and measurement; the subclasses manage their own style fields,
 * checked or selected state, and signals.
 */
abstract class FlixelUiAbstractToggle extends FlixelUiWidget {

  /** The label text and its font settings. */
  @NotNull
  final FlixelUiTextPart part = new FlixelUiTextPart();

  /** Creates a toggle with the given label. */
  FlixelUiAbstractToggle(@Nullable CharSequence label) {
    part.setText(label);
  }

  @Override
  protected void onAttached(@NotNull FlixelUiDisplay display) {
    part.attach(display);
  }

  @Override
  protected void onDetached() {
    part.detach();
  }

  /**
   * Sizes the widget to its box plus spacing plus label.
   *
   * <p>Called during the measure step of every layout pass. Must not allocate.
   */
  @Override
  protected void onMeasure() {
    FlixelUiBackground bg = getBackground();
    float bs = computeBoxSize(bg);
    float bh = computeBoxHeight(bg);
    float tw = part.getWidth();
    float th = part.getHeight();
    float spacing = getSpacing();
    width = bs + (tw > 0f ? spacing + tw : 0f);
    height = Math.max(bh, th);
  }

  /**
   * Returns the preferred width: box size plus spacing plus label.
   *
   * @return The preferred width in pixels.
   */
  @Override
  public float getPreferredWidth() {
    FlixelUiBackground bg = getBackground();
    float bs = computeBoxSize(bg);
    float tw = part.getWidth();
    return bs + (tw > 0f ? getSpacing() + tw : 0f);
  }

  /**
   * Returns the preferred height: the taller of the box and the label.
   *
   * @return The preferred height in pixels.
   */
  @Override
  public float getPreferredHeight() {
    FlixelUiBackground bg = getBackground();
    float bh = computeBoxHeight(bg);
    return Math.max(bh, part.getHeight());
  }

  /**
   * Draws the box and, when the label is non-empty, the label to its right.
   *
   * <p>The box is drawn at {@code drawX, drawY} vertically centered in the widget's height. The
   * label is vertically centered to the right of the box. Colors are multiplied by
   * {@code alpha} and the widget's tint, and the batch color is restored to white afterwards.
   *
   * @param batch The batch to draw into.
   * @param drawX The left edge in draw coordinates.
   * @param drawY The top edge in draw coordinates.
   * @param alpha The combined alpha of this widget and its ancestors.
   */
  @Override
  protected void drawSelf(@NotNull FlixelBatch batch, float drawX, float drawY, float alpha) {
    FlixelUiBackground bg = getBackground();
    FlixelColor tint = getColor();
    float bw = computeBoxSize(bg);
    float bh = computeBoxHeight(bg);
    float boxTop = drawY + (height - bh) * 0.5f;
    if (bg != null) {
      batch.setColor(tint.r, tint.g, tint.b, alpha);
      bg.draw(batch, drawX, boxTop, bw, bh);
      batch.setColor(FlixelColor.WHITE);
    }
    if (!part.isEmpty()) {
      part.setColor(getFontColor());
      float labelTop = screenY + (height - part.getHeight()) * 0.5f;
      part.draw(batch, screenX + bw + getSpacing(), labelTop, tint, alpha);
    }
  }

  /**
   * Destroys the label text this widget owns; subclasses chain to this and clean up their own
   * fields.
   */
  @Override
  public void destroy() {
    super.destroy();
    part.destroy();
  }

  /**
   * Updates the label text; invalidates the layout when the content changes.
   *
   * @param text The new label; {@code null} or empty shows no text.
   */
  public void setText(@Nullable CharSequence text) {
    if (part.setText(text)) {
      invalidateLayout();
    }
  }

  /**
   * Returns the background for the current state and checked value.
   *
   * <p>Visual priority: disabled, pressed (down), hovered (over), focused, then plain. Within
   * each state, the {@code on} or {@code off} variant is chosen based on whether
   * {@link #isCheckedState()} returns {@code true}. A missing variant falls back to the plain
   * {@code on} or {@code off} background.
   *
   * @return The background to draw, or {@code null} when there is no style.
   */
  @Nullable
  public abstract FlixelUiBackground getBackground();

  /**
   * Returns the text color for the label in the current state.
   *
   * @return The color; white when the style has none.
   */
  @NotNull
  public abstract FlixelColor getFontColor();

  /**
   * Returns {@code true} when the widget is in its "on" (checked or selected) visual state.
   *
   * @return Whether the "on" backgrounds should be used.
   */
  protected abstract boolean isCheckedState();

  /**
   * Returns the spacing between the box and the label from the current style, or
   * {@link FlixelUiCheckboxStyle#spacing} default when there is none.
   *
   * @return The spacing in pixels.
   */
  protected abstract float getSpacing();

  /**
   * Returns the box width from the style's {@link FlixelUiCheckboxStyle#boxSize}, falling back
   * to {@link FlixelUiBgSize#width(FlixelUiBackground)} when {@code boxSize} is zero.
   *
   * @param bg The background to read the size from; may be {@code null}.
   * @return The box width in pixels.
   */
  final float computeBoxSize(@Nullable FlixelUiBackground bg) {
    float bs = getRawBoxSize();
    if (bs > 0f) {
      return bs;
    }
    return FlixelUiBgSize.width(bg);
  }

  /**
   * Returns the box height from the style's {@link FlixelUiCheckboxStyle#boxSize}, falling back
   * to {@link FlixelUiBgSize#height(FlixelUiBackground)} when {@code boxSize} is zero.
   *
   * @param bg The background to read the size from; may be {@code null}.
   * @return The box height in pixels.
   */
  final float computeBoxHeight(@Nullable FlixelUiBackground bg) {
    float bs = getRawBoxSize();
    if (bs > 0f) {
      return bs;
    }
    return FlixelUiBgSize.height(bg);
  }

  /**
   * Returns the raw {@link FlixelUiCheckboxStyle#boxSize} field, or {@code 0} when there is no
   * style. Used by the compute helpers to avoid recomputing the style lookup.
   *
   * @return The raw box size in pixels.
   */
  protected abstract float getRawBoxSize();

  /**
   * Returns the label text part, for tests.
   *
   * @return The text part; never {@code null}.
   */
  @NotNull
  FlixelUiTextPart getPart() {
    return part;
  }
}
