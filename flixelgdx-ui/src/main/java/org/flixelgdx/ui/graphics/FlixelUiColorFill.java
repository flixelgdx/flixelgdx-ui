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

import org.flixelgdx.Flixel;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.util.FlixelColor;
import org.flixelgdx.util.FlixelSpriteUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A background that paints a plain rectangle of color, no image needed.
 *
 * <p>Think of it as a sheet of tinted glass cut to size. It is handy for things that are just a
 * color: the dark, see-through backdrop behind a modal, a text caret, or the highlight behind
 * selected text. The fill color is multiplied with the batch's current color, so a widget can
 * still fade it out through its alpha.
 *
 * <pre>{@code
 * // A black backdrop at 60% opacity.
 * FlixelUiColorFill backdrop = new FlixelUiColorFill(0f, 0f, 0f, 0.6f);
 * backdrop.draw(batch, 0, 0, screenWidth, screenHeight);
 *
 * // Change the color later without making a new object.
 * backdrop.getColor().a = 0.8f;
 * }</pre>
 *
 * <p>It draws with the framework's shared 1x1 white pixel, which is obtained the first time the
 * fill is drawn and belongs to the asset manager, so {@link #destroy()} has nothing to release.
 */
public final class FlixelUiColorFill implements FlixelUiBackground {

  /** The fill color, owned by this object. */
  @NotNull
  private final FlixelColor color;

  /** Holds the batch color while drawing so it can be restored without allocating. */
  @NotNull
  private final FlixelColor saved = new FlixelColor();

  /** The shared white pixel, obtained on the first draw and never destroyed here. */
  @Nullable
  private FlixelFrame pixel;

  /**
   * Creates a fill with a copy of the given color.
   *
   * @param color The fill color; copied, so later changes to it do not affect this fill.
   * @throws IllegalArgumentException If {@code color} is {@code null}.
   */
  public FlixelUiColorFill(@NotNull FlixelColor color) {
    if (color == null) {
      throw new IllegalArgumentException("A color fill needs a color.");
    }
    this.color = new FlixelColor(color);
  }

  /**
   * Creates a fill from color components.
   *
   * @param r The red component, from {@code 0} to {@code 1}.
   * @param g The green component, from {@code 0} to {@code 1}.
   * @param b The blue component, from {@code 0} to {@code 1}.
   * @param a The alpha component, from {@code 0} (invisible) to {@code 1} (opaque).
   */
  public FlixelUiColorFill(float r, float g, float b, float a) {
    this.color = new FlixelColor(r, g, b, a);
  }

  /**
   * Fills a rectangle with this color multiplied by the batch's current color.
   *
   * <p>The batch color is restored afterwards. Does not allocate.
   *
   * @param batch The batch to draw into.
   * @param x The left edge in draw coordinates.
   * @param y The top edge in draw coordinates.
   * @param width The width to fill, in pixels.
   * @param height The height to fill, in pixels.
   */
  @Override
  public void draw(@NotNull FlixelBatch batch, float x, float y, float width, float height) {
    if (width <= 0f || height <= 0f) {
      return;
    }
    FlixelFrame px = pixel;
    if (px == null) {
      px = FlixelSpriteUtil.obtainWhitePixel(Flixel.assets);
      pixel = px;
    }
    FlixelColor s = saved.set(batch.getColor());
    batch.setColor(s.r * color.r, s.g * color.g, s.b * color.b, s.a * color.a);
    batch.draw(px, x, y, width, height);
    batch.setColor(s);
  }

  /**
   * Does nothing: the white pixel belongs to the asset manager, not to this fill.
   */
  @Override
  public void destroy() {}

  /**
   * Returns {@code 0}: a fill can be any size.
   *
   * @return Always {@code 0}.
   */
  @Override
  public float getMinWidth() {
    return 0f;
  }

  /**
   * Returns {@code 0}: a fill can be any size.
   *
   * @return Always {@code 0}.
   */
  @Override
  public float getMinHeight() {
    return 0f;
  }

  /**
   * Returns the live fill color; changing its components changes the next draw.
   *
   * @return The backing color; never {@code null}.
   */
  @NotNull
  public FlixelColor getColor() {
    return color;
  }

  /**
   * Copies a color into this fill.
   *
   * @param color The new fill color.
   */
  public void setColor(@NotNull FlixelColor color) {
    this.color.set(color);
  }
}
