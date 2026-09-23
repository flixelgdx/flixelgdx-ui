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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A background that stretches a single image to fill the rectangle it is given.
 *
 * <p>Where a {@link FlixelNineSlice} is a picture frame with sturdy corners, this is a photo
 * printed on stretchy fabric: the whole picture scales to whatever size the widget has. It suits
 * backgrounds that look fine at any size (a soft gradient, a blurred texture) and small pictures
 * drawn at a fixed size, such as the icon on a button or the tick inside a checkbox. For those,
 * {@link #getNaturalWidth()} and {@link #getNaturalHeight()} give the image's own size.
 *
 * <pre>{@code
 * FlixelUiImage icon = FlixelUiImage.load(Flixel.files.internal("ui/plus.png"));
 * icon.draw(batch, drawX, drawY, icon.getNaturalWidth(), icon.getNaturalHeight());
 *
 * // A frame from a texture atlas works too.
 * FlixelUiImage tick = new FlixelUiImage(atlasFrame);
 * }</pre>
 *
 * <p>Trimmed atlas frames (packed with their transparent border cut away) are drawn where the
 * artist placed them inside their original box, so the natural size is the untrimmed size. Frames
 * packed rotated are rejected, because they would draw sideways.
 *
 * <h2>Ownership</h2>
 *
 * <p>An image made with {@link #load(FlixelFile)} loaded its own file, so {@link #destroy()}
 * releases it. An image made from a {@link FlixelFrame} you passed in does not own that frame's
 * texture, and {@link #destroy()} leaves it alone.
 */
public final class FlixelUiImage implements FlixelUiBackground {

  @NotNull
  private final FlixelFrame frame;

  /** The image this object loaded itself, released on destroy; {@code null} when not owned. */
  @Nullable
  private FlixelGraphic graphic;

  /**
   * Creates an image background that draws a frame.
   *
   * <p>The frame's texture is not owned; {@link #destroy()} does nothing to it.
   *
   * @param frame The frame to draw.
   * @throws IllegalArgumentException If {@code frame} is {@code null} or was packed rotated.
   */
  public FlixelUiImage(@NotNull FlixelFrame frame) {
    if (frame == null) {
      throw new IllegalArgumentException("A UI image needs a frame.");
    }
    if (frame.rotated) {
      throw new IllegalArgumentException(
          "Cannot draw a frame that was packed rotated; export it from the atlas without rotation.");
    }
    this.frame = frame;
  }

  /**
   * Loads an image file into a new image background.
   *
   * <p>The file is fetched through {@link Flixel#assets} the same way a sprite's
   * {@code loadGraphic(FlixelFile)} does, so it is shared with anything else that loads the same
   * file. The returned object holds a reference to the image until {@link #destroy()}.
   *
   * <p>On the browser backend, images are decoded asynchronously. Call this method only after
   * the file has been decoded; otherwise {@link Flixel#assets} throws because the graphic is not
   * ready yet. Preload UI images in a loading state before building the skin.
   *
   * @param file The image file to load, such as {@code Flixel.files.internal("ui/plus.png")}.
   * @return A new image background that owns its reference to the image.
   * @throws IllegalArgumentException If {@code file} is {@code null}.
   */
  @NotNull
  public static FlixelUiImage load(@NotNull FlixelFile file) {
    if (file == null) {
      throw new IllegalArgumentException("Cannot load a UI image from a null file.");
    }
    FlixelGraphic g = Flixel.assets.<FlixelGraphic>get(file.getPath()).retain().get();
    try {
      FlixelUiImage image = new FlixelUiImage(g.getFrame());
      image.graphic = g;
      return image;
    } catch (RuntimeException e) {
      // The image was never created, so nothing else will release this reference.
      g.release();
      throw e;
    }
  }

  /**
   * Stretches the image over a rectangle with the batch's current color.
   *
   * <p>The rectangle stands for the image's natural (untrimmed) box. A trimmed frame is scaled by
   * the same factor and drawn at its offset inside that box. Does not allocate.
   *
   * @param batch The batch to draw into.
   * @param x The left edge in draw coordinates.
   * @param y The top edge in draw coordinates.
   * @param width The width to fill, in pixels.
   * @param height The height to fill, in pixels.
   */
  @Override
  public void draw(@NotNull FlixelBatch batch, float x, float y, float width, float height) {
    int naturalW = frame.originalWidth;
    int naturalH = frame.originalHeight;
    if (width <= 0f || height <= 0f || naturalW <= 0 || naturalH <= 0) {
      return;
    }
    float sx = width / naturalW;
    float sy = height / naturalH;
    batch.draw(frame, x + frame.offsetX * sx, y + frame.offsetY * sy,
        frame.getRegionWidth() * sx, frame.getRegionHeight() * sy);
  }

  /**
   * Releases the image this object loaded through {@link #load(FlixelFile)}.
   *
   * <p>Does nothing for an image built from a caller's {@link FlixelFrame}, and does nothing the
   * second time. Do not draw an image after destroying it.
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
   * Returns the image this object loaded itself, for tests.
   *
   * @return The owned image, or {@code null} when it was built from a frame or already destroyed.
   */
  @Nullable
  FlixelGraphic getGraphic() {
    return graphic;
  }

  /**
   * Returns {@code 0}: an image can be stretched or squashed to any size.
   *
   * @return Always {@code 0}.
   */
  @Override
  public float getMinWidth() {
    return 0f;
  }

  /**
   * Returns {@code 0}: an image can be stretched or squashed to any size.
   *
   * @return Always {@code 0}.
   */
  @Override
  public float getMinHeight() {
    return 0f;
  }

  @NotNull
  public FlixelFrame getFrame() {
    return frame;
  }

  /**
   * Returns the image's own width in pixels, before any stretching.
   *
   * <p>For a trimmed atlas frame this is the untrimmed width.
   *
   * @return The natural width in pixels.
   */
  public int getNaturalWidth() {
    return frame.originalWidth;
  }

  /**
   * Returns the image's own height in pixels, before any stretching.
   *
   * <p>For a trimmed atlas frame this is the untrimmed height.
   *
   * @return The natural height in pixels.
   */
  public int getNaturalHeight() {
    return frame.originalHeight;
  }
}
