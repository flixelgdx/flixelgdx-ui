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

import org.flixelgdx.FlixelCamera;
import org.flixelgdx.file.FlixelFile;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.text.FlixelText;
import org.flixelgdx.util.FlixelColor;
import org.flixelgdx.util.FlixelString;
import org.flixelgdx.util.FlixelStringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The piece of text a widget draws as one of its parts, such as a label's words or a button's
 * caption.
 *
 * <p>It wraps one {@link FlixelText} and takes care of everything a widget would otherwise repeat:
 * applying the font, size, and color from a style; keeping the text's {@link FlixelText#cameras}
 * pointed at the display's camera (the UI camera is not a default draw target, so text that does
 * not list it is never drawn there); placing the text at a UI screen position with
 * {@link FlixelUiDisplay#toSpriteX(float)}; and fading it with the widget's combined alpha.
 *
 * <p>The text has a scroll factor of {@code 0}, like every widget. Nothing here allocates after
 * construction.
 *
 * <p>Clip rectangles on the text itself are never used: a sprite clip clears the scissor when it
 * finishes, which would drop the display's clip stack.
 */
final class FlixelUiTextPart {

  /** The text object; configured and drawn only through this part. */
  @NotNull
  private final FlixelText text = new FlixelText();

  /** The one-element camera list handed to {@link #text}; slot 0 holds the display's camera. */
  @NotNull
  private final FlixelCamera[] cameras = new FlixelCamera[1];

  /** The text color set by the owning widget, before tint and alpha. */
  @NotNull
  private final FlixelColor color = new FlixelColor(FlixelColor.WHITE);

  /** The display the owner is attached to, or {@code null} when it is not on one. */
  @Nullable
  private FlixelUiDisplay display;

  /** The font file last applied, or {@code null} for the framework's default font. */
  @Nullable
  private FlixelFile font;

  /** Whether {@link #font} has been applied to {@link #text} at least once. */
  private boolean fontApplied;

  /** Creates an empty text part with the framework's default font and white text. */
  FlixelUiTextPart() {
    text.setText("");
    text.setScrollFactor(0f, 0f);
    text.cameras = cameras;
  }

  /**
   * Replaces the text, doing nothing when the content is the same.
   *
   * @param value The new text; {@code null} counts as empty.
   * @return {@code true} if the content changed.
   */
  boolean setText(@Nullable CharSequence value) {
    CharSequence v = value != null ? value : "";
    if (FlixelStringUtil.contentEquals(v, text.getTextBuffer())) {
      return false;
    }
    text.setText(v);
    return true;
  }

  /**
   * Applies a style's font settings.
   *
   * <p>The font is only handed to the text when it differs from the last one, so restyling with
   * the same font does not rebuild the glyphs.
   *
   * @param font The font file, or {@code null} for the framework's default font.
   * @param size The font size in pixels.
   * @param color The text color, or {@code null} for white.
   */
  void setFormat(@Nullable FlixelFile font, int size, @Nullable FlixelColor color) {
    if (!fontApplied || font != this.font) {
      if (font != null) {
        text.setFont(font);
      } else {
        text.setFont((String) null);
      }
      this.font = font;
      fontApplied = true;
    }
    text.setTextSize(size);
    setColor(color);
  }

  /**
   * Sets the text color used from the next draw on.
   *
   * @param color The text color, or {@code null} for white.
   */
  void setColor(@Nullable FlixelColor color) {
    this.color.set(color != null ? color : FlixelColor.WHITE);
  }

  /**
   * Points the text at a display's camera, so it is drawn during that camera's pass.
   *
   * @param display The display the owning widget was attached to.
   */
  void attach(@NotNull FlixelUiDisplay display) {
    this.display = display;
    cameras[0] = display.getCamera();
  }

  /** Forgets the display, so the text is not drawn on any camera until attached again. */
  void detach() {
    display = null;
    cameras[0] = null;
  }

  /**
   * Draws the text with its top-left corner at a UI screen position.
   *
   * <p>The color set with {@link #setColor(FlixelColor)} is multiplied by the widget's tint and
   * combined alpha for this draw only, then restored. Does nothing when the text is empty. Does not
   * allocate.
   *
   * @param batch The batch to draw into.
   * @param screenX The left edge, in the display camera's view space.
   * @param screenY The top edge, in the display camera's view space.
   * @param tint The widget's tint; its alpha is ignored because {@code alpha} already includes it.
   * @param alpha The combined alpha of the widget and its ancestors.
   */
  void draw(@NotNull FlixelBatch batch, float screenX, float screenY, @NotNull FlixelColor tint, float alpha) {
    if (isEmpty()) {
      return;
    }
    FlixelUiDisplay d = display;
    float sx = d != null ? d.toSpriteX(screenX) : screenX;
    float sy = d != null ? d.toSpriteY(screenY) : screenY;
    text.setPosition(sx, sy);
    FlixelColor c = color;
    text.setColor(c.r * tint.r, c.g * tint.g, c.b * tint.b, c.a * alpha);
    try {
      text.draw(batch);
    } finally {
      text.setColor(c);
    }
  }

  /** Destroys the text object. The part must not be used afterwards. */
  void destroy() {
    display = null;
    cameras[0] = null;
    text.destroy();
  }

  /**
   * Returns whether there is no text to show.
   *
   * @return {@code true} when the text is empty.
   */
  boolean isEmpty() {
    return text.getTextBuffer().isEmpty();
  }

  /**
   * Returns the width of the laid-out text box, or {@code 0} when the text is empty.
   *
   * <p>With a {@linkplain FlixelText#setFieldWidth(float) field width} set, this is that width.
   * An empty text returns {@code 0} without loading a font.
   *
   * @return The width in pixels.
   */
  float getWidth() {
    return isEmpty() ? 0f : text.getWidth();
  }

  /**
   * Returns the height of the laid-out text box, or {@code 0} when the text is empty.
   *
   * @return The height in pixels.
   */
  float getHeight() {
    return isEmpty() ? 0f : text.getHeight();
  }

  /**
   * Returns the live text buffer. Do not modify it; use {@link #setText(CharSequence)}.
   *
   * @return The text currently shown.
   */
  @NotNull
  FlixelString getBuffer() {
    return text.getTextBuffer();
  }

  /**
   * Returns the wrapped text object, for widgets that set layout options such as wrapping and
   * for tests.
   *
   * @return The text object.
   */
  @NotNull
  FlixelText getText() {
    return text;
  }

  /**
   * Returns the camera list handed to the text object, for tests.
   *
   * @return The one-element camera list.
   */
  @NotNull
  FlixelCamera[] getCameras() {
    return cameras;
  }
}
