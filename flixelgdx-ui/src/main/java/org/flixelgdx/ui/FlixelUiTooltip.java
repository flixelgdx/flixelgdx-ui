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
import org.flixelgdx.ui.skin.FlixelUiSkin;
import org.flixelgdx.ui.skin.FlixelUiTooltipStyle;
import org.flixelgdx.util.FlixelColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The shared tooltip bubble that {@link FlixelUiDisplay} shows when the game calls
 * {@link FlixelUiDisplay#showTooltip(FlixelUiWidget)} or
 * {@link FlixelUiDisplay#showTooltip(FlixelUiWidget, float)}.
 *
 * <p>Think of it as a name card that follows the currently pointed-at widget: the display owns
 * exactly one, places it automatically near the target, and keeps it there until
 * {@link FlixelUiDisplay#hideTooltip()} is called or the target leaves the screen. The game never
 * creates or adds this widget itself; it receives it from
 * {@link FlixelUiDisplay#getTooltip()} when it wants to tween the tooltip in or out.
 *
 * <p>The tooltip is not {@link #interactive}: {@link FlixelUiDisplay#getWidgetAt(float, float)}
 * skips it entirely. Its appearance comes from the {@link FlixelUiTooltipStyle} in the display's
 * skin whose name matches the display's tooltip style name (see
 * {@link FlixelUiDisplay#setTooltipStyle(String)}). When the skin has no such style, the tooltip
 * draws its text with no background.
 *
 * @see FlixelUiDisplay#showTooltip(FlixelUiWidget)
 * @see FlixelUiDisplay#showTooltip(FlixelUiWidget, float)
 * @see FlixelUiDisplay#hideTooltip()
 */
public class FlixelUiTooltip extends FlixelUiWidget {

  /** The text and its font settings. */
  @NotNull
  private final FlixelUiTextPart part = new FlixelUiTextPart();

  /** The style in use, or {@code null} when the skin has no matching tooltip style. */
  @Nullable
  private FlixelUiTooltipStyle style;

  /** Package-private: the display creates this. */
  FlixelUiTooltip() {
    interactive = false;
    setVisible(false);
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
   * Resolves the tooltip style from the skin, silently falling back to {@code null} when the skin
   * has no matching tooltip style, giving the tooltip a plain-text, no-background look.
   *
   * <p>The check uses {@link FlixelUiSkin#has(Class, String)} so genuine configuration errors
   * (for example, a missing style of a different class) are still reported by the skin's other
   * resolution paths rather than being swallowed here.
   */
  @Override
  protected void onStyleChanged() {
    FlixelUiDisplay d = display;
    if (d != null && d.getSkin().has(FlixelUiTooltipStyle.class, getStyleName())) {
      style = resolveStyle(FlixelUiTooltipStyle.class);
    } else {
      style = null;
    }
    applyStyle();
  }

  /**
   * Returns the preferred width: padding plus the text width.
   *
   * <p>Called by {@link FlixelUiDisplay} to place the tooltip before the layout pass measures it,
   * so it computes the current size directly from the text object without relying on a cached
   * field.
   *
   * @return The preferred width in pixels.
   */
  @Override
  public float getPreferredWidth() {
    FlixelUiTooltipStyle s = style;
    float padH = s != null ? s.padLeft + s.padRight : 0f;
    return padH + (part.isEmpty() ? 0f : part.getWidth());
  }

  /**
   * Returns the preferred height: padding plus the text height.
   *
   * @return The preferred height in pixels.
   */
  @Override
  public float getPreferredHeight() {
    FlixelUiTooltipStyle s = style;
    float padV = s != null ? s.padTop + s.padBottom : 0f;
    return padV + (part.isEmpty() ? 0f : part.getHeight());
  }

  /**
   * Sizes the tooltip to its text plus padding.
   */
  @Override
  protected void onMeasure() {
    width = getPreferredWidth();
    height = getPreferredHeight();
  }

  /**
   * Draws the background, then the text inside the padding.
   *
   * @param batch The batch to draw into.
   * @param drawX The left edge in draw coordinates.
   * @param drawY The top edge in draw coordinates.
   * @param alpha The combined alpha of this tooltip and its ancestors.
   */
  @Override
  protected void drawSelf(@NotNull FlixelBatch batch, float drawX, float drawY, float alpha) {
    FlixelUiTooltipStyle s = style;
    FlixelUiBackground bg = s != null ? s.background : null;
    FlixelColor tint = getColor();
    if (bg != null) {
      batch.setColor(tint.r, tint.g, tint.b, alpha);
      bg.draw(batch, drawX, drawY, width, height);
      batch.setColor(FlixelColor.WHITE);
    }
    if (!part.isEmpty()) {
      float padL = s != null ? s.padLeft : 0f;
      float padT = s != null ? s.padTop : 0f;
      part.draw(batch, screenX + padL, screenY + padT, tint, alpha);
    }
  }

  /**
   * Destroys this tooltip and the text object it owns.
   */
  @Override
  public void destroy() {
    super.destroy();
    part.destroy();
    style = null;
  }

  /**
   * Updates the text shown by this tooltip and applies wrapping from the current style.
   *
   * @param text The text to show; {@code null} shows nothing.
   */
  void setText(@Nullable CharSequence text) {
    FlixelUiTooltipStyle s = style;
    if (s != null && s.maxWidth > 0f) {
      part.getText().setFieldWidth(s.maxWidth);
      part.getText().setWordWrap(true);
    } else {
      part.getText().setWordWrap(false);
    }
    part.setText(text);
    invalidateLayout();
  }

  /** Applies the current style's font and wrapping settings to the text part. */
  private void applyStyle() {
    FlixelUiTooltipStyle s = style;
    if (s != null) {
      part.setFormat(s.font, s.fontSize, s.fontColor);
      if (s.maxWidth > 0f) {
        part.getText().setFieldWidth(s.maxWidth);
        part.getText().setWordWrap(true);
      } else {
        part.getText().setWordWrap(false);
      }
    }
    invalidateLayout();
  }

  /**
   * Returns the gap between the target widget and the tooltip bubble from the current style, or
   * {@code 0} when there is no style.
   *
   * @return The gap in pixels.
   */
  float getGap() {
    FlixelUiTooltipStyle s = style;
    return s != null ? s.gap : 0f;
  }

  /**
   * Returns the style this tooltip is currently using, or {@code null} when the skin has no
   * matching tooltip style (plain-text, no-background mode).
   *
   * @return The style, or {@code null}.
   */
  @Nullable
  public FlixelUiTooltipStyle getStyle() {
    return style;
  }

  /**
   * Returns the text part, for tests.
   *
   * @return The text part; never {@code null}.
   */
  @NotNull
  FlixelUiTextPart getPart() {
    return part;
  }
}
