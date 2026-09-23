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
import org.flixelgdx.text.FlixelText;
import org.flixelgdx.ui.skin.FlixelUiLabelStyle;
import org.flixelgdx.ui.skin.FlixelUiSkin;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A widget that shows a piece of text, such as a heading, a hint, or a score.
 *
 * <p>A label is like a name tag stuck onto the UI: it only shows words and never reacts to the
 * game's input, so it is not {@link #interactive} and hit-testing passes straight through it to
 * whatever is underneath. Its font, size, and color come from the {@link FlixelUiLabelStyle} in
 * the display's {@link FlixelUiSkin} whose name matches {@link #getStyleName()}, or from a style
 * object passed to {@link #setStyle(FlixelUiLabelStyle)}.
 *
 * <pre>{@code
 * FlixelUiLabel score = new FlixelUiLabel("Score: 0");
 * score.anchor(FlixelAlign.TOP_RIGHT, -8, 8);
 * ui.add(score);
 *
 * // Later, without allocating a new string every frame:
 * scoreText.set("Score: ").concat(points); // scoreText is a FlixelString field of the game.
 * score.setText(scoreText);                 // Copied; does nothing when the text is the same.
 *
 * FlixelUiLabel hint = new FlixelUiLabel("Press any key to continue.");
 * hint.setWrapWidth(200);                          // Wraps onto new lines at 200 pixels.
 * hint.setAlignment(FlixelText.Alignment.CENTER);  // Centers every line.
 * }</pre>
 *
 * <h2>Size</h2>
 *
 * <p>By default a label sizes itself to its text on every layout pass: as wide as the longest line
 * (or exactly {@link #getWrapWidth()} when wrapping) and as tall as all the lines together. Giving
 * it a size with {@link #setSize(float, float)}, {@link #setWidth(float)}, or
 * {@link #setHeight(float)} fixes that side instead; the text is then placed inside the label's
 * box using {@link #getAlignment()} horizontally and centered vertically. A
 * {@linkplain #setPercentSize(float, float) percent size} or a {@link FlixelUiStack} can also set
 * the size, and the text is placed the same way.
 */
public class FlixelUiLabel extends FlixelUiWidget {

  /** The text and its font settings. */
  @NotNull
  private final FlixelUiTextPart part = new FlixelUiTextPart();

  /** The width lines wrap at, or {@code 0} for no wrapping. */
  private float wrapWidth;

  /** The style in use, or {@code null} before one is resolved. */
  @Nullable
  private FlixelUiLabelStyle style;

  @NotNull
  private FlixelText.Alignment alignment = FlixelText.Alignment.LEFT;

  /** Whether {@link #style} was set with {@link #setStyle(FlixelUiLabelStyle)} instead of the skin. */
  private boolean customStyle;

  private boolean autoWidth = true;
  private boolean autoHeight = true;

  /**
   * Creates a label showing some text.
   *
   * <p>Labels are not {@link #interactive}.
   *
   * @param text The text to show; copied, so later changes to it do not affect the label.
   *     {@code null} shows nothing.
   */
  public FlixelUiLabel(@Nullable CharSequence text) {
    interactive = false;
    part.setText(text);
  }

  /**
   * Resolves this label's style from the skin, unless a style object was set directly, and
   * applies its font settings.
   *
   * @throws IllegalArgumentException If the skin has no {@link FlixelUiLabelStyle} with this
   *     label's style name.
   */
  @Override
  protected void onStyleChanged() {
    if (!customStyle) {
      style = resolveStyle(FlixelUiLabelStyle.class);
    }
    applyStyle();
  }

  @Override
  protected void onAttached(@NotNull FlixelUiDisplay display) {
    part.attach(display);
  }

  @Override
  protected void onDetached() {
    part.detach();
  }

  /** Sizes an auto-sized label to its text. */
  @Override
  protected void onMeasure() {
    if (autoWidth) {
      width = measureWidth();
    }
    if (autoHeight) {
      height = part.getHeight();
    }
  }

  /**
   * Draws the text inside the label's box, placed by {@link #getAlignment()} horizontally and
   * centered vertically.
   *
   * @param batch The batch to draw into.
   * @param drawX The left edge in draw coordinates.
   * @param drawY The top edge in draw coordinates.
   * @param alpha The combined alpha of this label and its ancestors.
   */
  @Override
  protected void drawSelf(@NotNull FlixelBatch batch, float drawX, float drawY, float alpha) {
    if (part.isEmpty()) {
      return;
    }
    float textW = part.getWidth();
    float textH = part.getHeight();
    float offsetX = switch (alignment) {
      case LEFT -> 0f;
      case CENTER -> (width - textW) * 0.5f;
      case RIGHT -> width - textW;
    };
    float offsetY = (height - textH) * 0.5f;
    part.draw(batch, screenX + offsetX, screenY + offsetY, getColor(), alpha);
  }

  /**
   * Destroys this label and the text object it owns.
   */
  @Override
  public void destroy() {
    super.destroy();
    part.destroy();
    style = null;
  }

  /**
   * Returns the width this label would like to have.
   *
   * @return The text width (or {@link #getWrapWidth()} while wrapping) when the width is automatic,
   *     otherwise the fixed width.
   */
  @Override
  public float getPreferredWidth() {
    return autoWidth ? measureWidth() : width;
  }

  /**
   * Returns the height this label would like to have.
   *
   * @return The height of all text lines when the height is automatic, otherwise the fixed height.
   */
  @Override
  public float getPreferredHeight() {
    return autoHeight ? part.getHeight() : height;
  }

  /**
   * Fixes the label's size, turning off automatic sizing on both sides.
   *
   * @param width The new width in pixels.
   * @param height The new height in pixels.
   */
  @Override
  public void setSize(float width, float height) {
    autoWidth = false;
    autoHeight = false;
    super.setSize(width, height);
  }

  /**
   * Fixes the label's width, turning off automatic sizing for the width.
   *
   * @param width The new width in pixels.
   */
  @Override
  public void setWidth(float width) {
    autoWidth = false;
    super.setWidth(width);
  }

  /**
   * Fixes the label's height, turning off automatic sizing for the height.
   *
   * @param height The new height in pixels.
   */
  @Override
  public void setHeight(float height) {
    autoHeight = false;
    super.setHeight(height);
  }

  /**
   * Turns automatic sizing to the text on or off for both sides.
   *
   * @param autoSize {@code true} to size the label to its text on every layout pass.
   */
  public void setAutoSize(boolean autoSize) {
    autoWidth = autoSize;
    autoHeight = autoSize;
    invalidateLayout();
  }

  /**
   * Returns the width of the text box: the wrap width while wrapping, otherwise the widest line.
   *
   * @return The width in pixels.
   */
  private float measureWidth() {
    return wrapWidth > 0f ? wrapWidth : part.getWidth();
  }

  /** Hands the current style's font settings to the text. */
  private void applyStyle() {
    FlixelUiLabelStyle s = style;
    if (s != null) {
      part.setFormat(s.font, s.fontSize, s.color);
    }
    invalidateLayout();
  }

  /**
   * Returns the text this label shows.
   *
   * <p>This is the label's own copy. Read it, but change it only through
   * {@link #setText(CharSequence)}.
   *
   * @return The live text buffer; never {@code null}.
   */
  @NotNull
  public CharSequence getText() {
    return part.getBuffer();
  }

  /**
   * Changes the text this label shows.
   *
   * <p>The characters are copied, so a reused {@link FlixelString} can be passed every frame
   * without allocating. Nothing happens when the content is the same as before, and the layout is
   * only recomputed when the new text changes the label's preferred size.
   *
   * @param text The new text; {@code null} shows nothing.
   */
  public void setText(@Nullable CharSequence text) {
    if (display == null) {
      part.setText(text);
      return;
    }
    float oldW = getPreferredWidth();
    float oldH = getPreferredHeight();
    if (part.setText(text) && (getPreferredWidth() != oldW || getPreferredHeight() != oldH)) {
      invalidateLayout();
    }
  }

  /**
   * Returns the width lines wrap at.
   *
   * @return The wrap width in pixels, or {@code 0} when the text does not wrap.
   */
  public float getWrapWidth() {
    return wrapWidth;
  }

  /**
   * Makes long text wrap onto new lines at a width, or turns wrapping off.
   *
   * <p>While wrapping, the label's automatic width is exactly this width, and words that do not fit
   * on a line move to the next one. Explicit line breaks ({@code '\n'}) always start a new line.
   *
   * @param wrapWidth The width in pixels to wrap at, or {@code 0} (or less) for no wrapping.
   */
  public void setWrapWidth(float wrapWidth) {
    float w = Math.max(0f, wrapWidth);
    if (this.wrapWidth == w) {
      return;
    }
    this.wrapWidth = w;
    FlixelText t = part.getText();
    t.setFieldWidth(w);
    t.setWordWrap(w > 0f);
    invalidateLayout();
  }

  @NotNull
  public FlixelText.Alignment getAlignment() {
    return alignment;
  }

  /**
   * Chooses how the text lines up.
   *
   * <p>Each line is aligned within the text box (which matters for wrapped or multi-line text),
   * and the text box is aligned within the label when the label is wider than its text.
   *
   * @param alignment The alignment.
   * @throws IllegalArgumentException If {@code alignment} is {@code null}.
   */
  public void setAlignment(@NotNull FlixelText.Alignment alignment) {
    if (alignment == null) {
      throw new IllegalArgumentException("Label alignment must not be null.");
    }
    this.alignment = alignment;
    part.getText().setAlignment(alignment);
  }

  /**
   * Returns the style this label uses.
   *
   * @return The style, or {@code null} while the label is not on a display and no style object was
   *     set.
   */
  @Nullable
  public FlixelUiLabelStyle getStyle() {
    return style;
  }

  /**
   * Makes this label use a style object directly instead of looking one up in the skin.
   *
   * <p>Handy for a one-off look that does not belong in the skin. Passing {@code null} goes back to
   * the skin's style named {@link #getStyleName()} (right away while on a display, otherwise when
   * added to one). The style is applied immediately either way.
   *
   * @param style The style to use, or {@code null} to use the skin again.
   * @throws IllegalArgumentException If {@code style} is {@code null}, the label is on a display,
   *     and the skin has no matching {@link FlixelUiLabelStyle}.
   */
  public void setStyle(@Nullable FlixelUiLabelStyle style) {
    customStyle = style != null;
    this.style = style;
    if (style == null && display != null) {
      this.style = resolveStyle(FlixelUiLabelStyle.class);
    }
    applyStyle();
  }

  /**
   * Returns whether the width follows the text.
   *
   * @return {@code true} unless a fixed width was set.
   */
  public boolean isAutoWidth() {
    return autoWidth;
  }

  /**
   * Returns whether the height follows the text.
   *
   * @return {@code true} unless a fixed height was set.
   */
  public boolean isAutoHeight() {
    return autoHeight;
  }

  /**
   * Returns the text part, for tests.
   *
   * @return The text part.
   */
  @NotNull
  FlixelUiTextPart getPart() {
    return part;
  }
}
