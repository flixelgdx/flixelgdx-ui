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
package org.flixelgdx.ui.graphics;

import org.flixelgdx.functional.FlixelDestroyable;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.ui.FlixelUiWidget;
import org.flixelgdx.ui.skin.FlixelUiStyle;
import org.jetbrains.annotations.NotNull;

/**
 * Something a widget can paint behind itself, stretched to whatever size the widget has.
 *
 * <p>Think of a background as a picture frame that can stretch. A widget hands it a rectangle
 * ("be this big, right here") and the background fills it: a {@link FlixelNineSlice} keeps its
 * corners crisp while its edges and middle stretch, a {@link FlixelUiImage} stretches one picture,
 * and a {@link FlixelUiColorFill} paints a plain color. Because they all share this interface, a
 * {@link FlixelUiStyle} can hold any of them in the same field.
 *
 * <p>A background draws with the batch's <em>current</em> color. The widget sets that color (its
 * tint, with the combined alpha of the widget and its ancestors) before calling
 * {@link #draw(FlixelBatch, float, float, float, float)}, so a background never needs to know
 * about fading or tinting. Drawing never allocates.
 *
 * <pre>{@code
 * // Inside a widget's drawSelf(...):
 * FlixelColor tint = getColor();
 * batch.setColor(tint.r, tint.g, tint.b, alpha);
 * style.background.draw(batch, drawX, drawY, getWidth(), getHeight());
 * batch.setColor(FlixelColor.WHITE);
 * }</pre>
 *
 * <p>Backgrounds that load their own image, such as {@link FlixelNineSlice#load}, hold a
 * reference to it until {@link #destroy()} is called. Destroy each background once, when nothing
 * uses it anymore.
 *
 * @see FlixelUiWidget
 */
public interface FlixelUiBackground extends FlixelDestroyable {

  /**
   * Draws this background into a rectangle, using the batch's current color.
   *
   * <p>A rectangle with a width or height of zero or less draws nothing.
   *
   * @param batch The batch to draw into.
   * @param x The left edge in draw coordinates.
   * @param y The top edge in draw coordinates.
   * @param width The width to fill, in pixels.
   * @param height The height to fill, in pixels.
   */
  void draw(@NotNull FlixelBatch batch, float x, float y, float width, float height);

  /**
   * Returns the narrowest width this background can be drawn at without its parts overlapping.
   *
   * <p>A widget using this background should not be laid out narrower than this. Drawing smaller
   * still works, but may look squashed.
   *
   * @return The minimum width in pixels.
   */
  float getMinWidth();

  /**
   * Returns the shortest height this background can be drawn at without its parts overlapping.
   *
   * @return The minimum height in pixels.
   * @see #getMinWidth()
   */
  float getMinHeight();
}
