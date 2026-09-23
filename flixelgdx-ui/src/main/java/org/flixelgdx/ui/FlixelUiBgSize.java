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
import org.flixelgdx.ui.graphics.FlixelUiImage;
import org.jetbrains.annotations.Nullable;

/**
 * Shared helpers for reading the display size of a background, used where the natural pixel
 * dimensions of a {@link FlixelUiImage} should take priority over its (always zero) minimum size.
 *
 * <p>A nine-slice or color fill advertises its minimum size via
 * {@link FlixelUiBackground#getMinWidth()} and {@link FlixelUiBackground#getMinHeight()}, but a
 * {@link FlixelUiImage} is designed to stretch to any size, so those methods return zero. Use
 * {@link #width(FlixelUiBackground)} and {@link #height(FlixelUiBackground)} instead of calling
 * the min-size methods directly, so images are drawn at their natural dimensions while every other
 * background type still uses its declared minimum.
 */
final class FlixelUiBgSize {

  private FlixelUiBgSize() {}

  /**
   * Returns the natural width of a {@link FlixelUiImage}, or the minimum width for any other
   * background type.
   *
   * @param bg The background to measure; {@code null} returns {@code 0}.
   * @return The display width in pixels.
   */
  static float width(@Nullable FlixelUiBackground bg) {
    if (bg instanceof FlixelUiImage img) {
      return img.getNaturalWidth();
    }
    return bg != null ? bg.getMinWidth() : 0f;
  }

  /**
   * Returns the natural height of a {@link FlixelUiImage}, or the minimum height for any other
   * background type.
   *
   * @param bg The background to measure; {@code null} returns {@code 0}.
   * @return The display height in pixels.
   */
  static float height(@Nullable FlixelUiBackground bg) {
    if (bg instanceof FlixelUiImage img) {
      return img.getNaturalHeight();
    }
    return bg != null ? bg.getMinHeight() : 0f;
  }
}
