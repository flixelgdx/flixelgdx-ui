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
import org.flixelgdx.file.FlixelFile;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.graphics.FlixelGraphic;
import org.flixelgdx.graphics.FlixelTexture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A background cut into a 3 by 3 grid so it can stretch to any size while its corners stay sharp.
 *
 * <p>Picture a window frame made of nine pieces of wood. The four corner pieces never change
 * size. The top and bottom pieces get longer when the window gets wider, the left and right
 * pieces get longer when it gets taller, and the glass in the middle stretches both ways. That is
 * exactly how a nine-slice draws: the four insets say how thick each side of the frame is, in
 * pixels of the source image.
 *
 * <pre>{@code
 *   left      right
 *  +---+-----+---+
 *  | 0 |  1  | 2 |  top
 *  +---+-----+---+
 *  | 3 |  4  | 5 |
 *  +---+-----+---+
 *  | 6 |  7  | 8 |  bottom
 *  +---+-----+---+
 * }</pre>
 *
 * <p>The nine pieces are cut once, when the nine-slice is created, so drawing is just nine batch
 * draws with no allocation. When the target rectangle is smaller than {@code left + right} (or
 * {@code top + bottom}), the corners shrink proportionally so the pieces never overlap.
 *
 * <pre>{@code
 * // A button image with 6 pixel borders on every side.
 * FlixelNineSlice up = FlixelNineSlice.load(Flixel.files.internal("ui/button.png"), 6, 6, 6, 6);
 *
 * // A piece of a texture atlas works too; the nine-slice cuts inside that frame's region.
 * FlixelNineSlice panel = new FlixelNineSlice(atlasFrame, 8, 8, 8, 8);
 *
 * // When no widget needs it anymore:
 * up.destroy(); // Releases the image loaded by load(...).
 * }</pre>
 *
 * <h2>Ownership</h2>
 *
 * <p>A nine-slice made with {@link #load(FlixelFile, int, int, int, int)} loaded its own image, so
 * {@link #destroy()} releases it. A nine-slice made from a {@link FlixelFrame} you passed in does
 * not own that frame's texture, and {@link #destroy()} leaves it alone.
 *
 * <h2>Limitations</h2>
 *
 * <p>Atlas frames that were packed rotated, or trimmed (packed with their transparent border cut
 * away), cannot be sliced correctly and are rejected with an {@link IllegalArgumentException}.
 * Export nine-slice images from your atlas tool with rotation and trimming turned off.
 */
public final class FlixelNineSlice implements FlixelUiBackground {

  /** Index of the top-left corner piece. */
  static final int TOP_LEFT = 0;

  /** Index of the top edge piece. */
  static final int TOP = 1;

  /** Index of the top-right corner piece. */
  static final int TOP_RIGHT = 2;

  /** Index of the left edge piece. */
  static final int LEFT = 3;

  /** Index of the center piece. */
  static final int CENTER = 4;

  /** Index of the right edge piece. */
  static final int RIGHT = 5;

  /** Index of the bottom-left corner piece. */
  static final int BOTTOM_LEFT = 6;

  /** Index of the bottom edge piece. */
  static final int BOTTOM = 7;

  /** Index of the bottom-right corner piece. */
  static final int BOTTOM_RIGHT = 8;

  private final int left;
  private final int top;
  private final int right;
  private final int bottom;

  /** The image this nine-slice loaded itself, released on destroy; {@code null} when not owned. */
  @Nullable
  private FlixelGraphic graphic;

  /** The nine pieces, in row-major order from the top-left corner. */
  @NotNull
  private final FlixelFrame[] parts = new FlixelFrame[9];

  /**
   * Creates a nine-slice from a frame, cutting it into nine pieces with the given insets.
   *
   * <p>The frame's region is respected, so a frame from a texture atlas slices only its own part
   * of the atlas. The nine-slice does not own the frame's texture.
   *
   * @param frame The frame to slice.
   * @param left The width of the left border, in source pixels.
   * @param top The height of the top border, in source pixels.
   * @param right The width of the right border, in source pixels.
   * @param bottom The height of the bottom border, in source pixels.
   * @throws IllegalArgumentException If {@code frame} is {@code null}, rotated, or trimmed, if an
   *     inset is negative, or if the insets leave no center (for example
   *     {@code left + right >= frame.getRegionWidth()}).
   */
  public FlixelNineSlice(@NotNull FlixelFrame frame, int left, int top, int right, int bottom) {
    if (frame == null) {
      throw new IllegalArgumentException("A nine-slice needs a frame.");
    }
    if (frame.rotated) {
      throw new IllegalArgumentException(
          "Cannot slice a frame that was packed rotated; export it from the atlas without rotation.");
    }
    int w = frame.getRegionWidth();
    int h = frame.getRegionHeight();
    if (frame.offsetX != 0 || frame.offsetY != 0 || frame.originalWidth != w || frame.originalHeight != h) {
      throw new IllegalArgumentException(
          "Cannot slice a trimmed frame; export it from the atlas without trimming.");
    }
    if (left < 0 || top < 0 || right < 0 || bottom < 0) {
      throw new IllegalArgumentException("Nine-slice insets must not be negative (got left=" + left
          + ", top=" + top + ", right=" + right + ", bottom=" + bottom + ").");
    }
    if (left + right >= w || top + bottom >= h) {
      throw new IllegalArgumentException("Nine-slice insets (left=" + left + ", top=" + top + ", right=" + right
          + ", bottom=" + bottom + ") leave no center in a " + w + "x" + h + " frame.");
    }
    this.left = left;
    this.top = top;
    this.right = right;
    this.bottom = bottom;

    FlixelTexture texture = frame.getTexture();
    int x0 = frame.getRegionX();
    int y0 = frame.getRegionY();
    int centerW = w - left - right;
    int centerH = h - top - bottom;
    int x1 = x0 + left;
    int x2 = x1 + centerW;
    int y1 = y0 + top;
    int y2 = y1 + centerH;

    parts[TOP_LEFT] = new FlixelFrame(texture, x0, y0, left, top);
    parts[TOP] = new FlixelFrame(texture, x1, y0, centerW, top);
    parts[TOP_RIGHT] = new FlixelFrame(texture, x2, y0, right, top);
    parts[LEFT] = new FlixelFrame(texture, x0, y1, left, centerH);
    parts[CENTER] = new FlixelFrame(texture, x1, y1, centerW, centerH);
    parts[RIGHT] = new FlixelFrame(texture, x2, y1, right, centerH);
    parts[BOTTOM_LEFT] = new FlixelFrame(texture, x0, y2, left, bottom);
    parts[BOTTOM] = new FlixelFrame(texture, x1, y2, centerW, bottom);
    parts[BOTTOM_RIGHT] = new FlixelFrame(texture, x2, y2, right, bottom);
  }

  /**
   * Loads an image file and slices the whole image with the given insets.
   *
   * <p>The image is fetched through {@link Flixel#assets} the same way a sprite's
   * {@code loadGraphic(FlixelFile)} does, so it is shared with anything else that loads the same
   * file. The returned nine-slice holds a reference to the image until {@link #destroy()}.
   *
   * <p>On the browser backend, images are decoded asynchronously. Call this method only after
   * the file has been decoded; otherwise {@link Flixel#assets} throws because the graphic is not
   * ready yet. Preload UI images in a loading state:
   *
   * <pre>{@code
   * // In a loading state's update():
   * if (Flixel.assets.update()) Flixel.switchState(() -> new MenuState());
   *
   * // In MenuState.create(), after the assets are ready:
   * FlixelNineSlice up = FlixelNineSlice.load(Flixel.files.internal("ui/button.png"), 6, 6, 6, 6);
   * }</pre>
   *
   * @param file The image file to load, such as {@code Flixel.files.internal("ui/panel.png")}.
   * @param left The width of the left border, in source pixels.
   * @param top The height of the top border, in source pixels.
   * @param right The width of the right border, in source pixels.
   * @param bottom The height of the bottom border, in source pixels.
   * @return A new nine-slice that owns its reference to the image.
   * @throws IllegalArgumentException If {@code file} is {@code null} or the insets do not fit the
   *     image (see {@link #FlixelNineSlice(FlixelFrame, int, int, int, int)}).
   */
  @NotNull
  public static FlixelNineSlice load(@NotNull FlixelFile file, int left, int top, int right, int bottom) {
    if (file == null) {
      throw new IllegalArgumentException("Cannot load a nine-slice from a null file.");
    }
    FlixelGraphic g = Flixel.assets.<FlixelGraphic>get(file.getPath()).retain().get();
    try {
      FlixelNineSlice slice = new FlixelNineSlice(g.getFrame(), left, top, right, bottom);
      slice.graphic = g;
      return slice;
    } catch (RuntimeException e) {
      // The nine-slice was never created, so nothing else will release this reference.
      g.release();
      throw e;
    }
  }

  /**
   * Draws the nine pieces into a rectangle with the batch's current color.
   *
   * <p>Corners keep their source size, edges stretch along their length, and the center stretches
   * both ways. When the rectangle is narrower than {@link #getMinWidth()} (or shorter than
   * {@link #getMinHeight()}), the corners and edges shrink proportionally and the center is not
   * drawn on that axis. Pieces with no size are skipped. Does not allocate.
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
    float lw = left;
    float rw = right;
    int horizontal = left + right;
    if (width < horizontal) {
      float scale = width / horizontal;
      lw *= scale;
      rw *= scale;
    }
    float th = top;
    float bh = bottom;
    int vertical = top + bottom;
    if (height < vertical) {
      float scale = height / vertical;
      th *= scale;
      bh *= scale;
    }
    float cw = Math.max(0f, width - lw - rw);
    float ch = Math.max(0f, height - th - bh);
    float cx = x + lw;
    float rx = x + width - rw;
    float cy = y + th;
    float by = y + height - bh;

    drawRow(batch, TOP_LEFT, x, cx, rx, y, lw, cw, rw, th);
    drawRow(batch, LEFT, x, cx, rx, cy, lw, cw, rw, ch);
    drawRow(batch, BOTTOM_LEFT, x, cx, rx, by, lw, cw, rw, bh);
  }

  /**
   * Releases the image this nine-slice loaded through {@link #load(FlixelFile, int, int, int, int)}.
   *
   * <p>Does nothing for a nine-slice built from a caller's {@link FlixelFrame}, and does nothing
   * the second time. Do not draw a nine-slice after destroying it.
   */
  @Override
  public void destroy() {
    FlixelGraphic g = graphic;
    if (g != null) {
      graphic = null;
      g.release();
    }
  }

  /**
   * Draws one row of three pieces.
   *
   * @param batch The batch to draw into.
   * @param first The index of the row's leftmost piece.
   * @param lx The left column's X.
   * @param cx The center column's X.
   * @param rx The right column's X.
   * @param ry The row's Y.
   * @param lw The left column's drawn width.
   * @param cw The center column's drawn width.
   * @param rw The right column's drawn width.
   * @param rh The row's drawn height.
   */
  private void drawRow(@NotNull FlixelBatch batch, int first, float lx, float cx, float rx, float ry,
      float lw, float cw, float rw, float rh) {
    if (rh <= 0f) {
      return;
    }
    if (lw > 0f) {
      batch.draw(parts[first], lx, ry, lw, rh);
    }
    if (cw > 0f) {
      batch.draw(parts[first + 1], cx, ry, cw, rh);
    }
    if (rw > 0f) {
      batch.draw(parts[first + 2], rx, ry, rw, rh);
    }
  }

  /**
   * Returns one of the nine pieces, for tests.
   *
   * @param index The piece index, from {@link #TOP_LEFT} to {@link #BOTTOM_RIGHT}.
   * @return The piece's frame.
   */
  @NotNull
  FlixelFrame getPart(int index) {
    return parts[index];
  }

  /**
   * Returns the image this nine-slice loaded itself, for tests.
   *
   * @return The owned image, or {@code null} when it was built from a frame or already destroyed.
   */
  @Nullable
  FlixelGraphic getGraphic() {
    return graphic;
  }

  /**
   * Returns {@code left + right}: below this width the corners have to shrink.
   *
   * @return The minimum width in pixels.
   */
  @Override
  public float getMinWidth() {
    return left + right;
  }

  /**
   * Returns {@code top + bottom}: below this height the corners have to shrink.
   *
   * @return The minimum height in pixels.
   */
  @Override
  public float getMinHeight() {
    return top + bottom;
  }

  public int getLeft() {
    return left;
  }

  public int getTop() {
    return top;
  }

  public int getRight() {
    return right;
  }

  public int getBottom() {
    return bottom;
  }
}
