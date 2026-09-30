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

import org.flixelgdx.file.FlixelFile;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.ui.graphics.FlixelUiImage;
import org.flixelgdx.util.FlixelAlign;
import org.flixelgdx.util.FlixelColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A widget that shows a picture, such as a logo, a portrait, or an icon, inside the UI.
 *
 * <p>Think of it as a picture frame hung on the wall. The frame is the widget (it has a position,
 * a size, and anchors like any other widget) and the photo inside it is the image. The
 * {@link ScaleMode} decides how the photo is fitted into the frame: stretched to the edges, shrunk
 * to fit without cropping, grown to cover it, or left at its own size.
 *
 * <pre>{@code
 * // A logo that keeps its shape while it scales to fit a 200x100 box.
 * FlixelUiPicture logo = new FlixelUiPicture(Flixel.files.internal("ui/logo.png"), 200, 100);
 * logo.setScaleMode(FlixelUiPicture.ScaleMode.FIT);
 * logo.anchor(FlixelAlign.TOP, 0, 16);
 * ui.add(logo);
 *
 * // Only show one cell (the 32x32 square at 64, 0) of a sprite sheet.
 * FlixelUiPicture icon = new FlixelUiPicture(Flixel.files.internal("ui/icons.png"));
 * icon.setRegion(64, 0, 32, 32);
 *
 * // Tint and fade like any widget.
 * icon.getColor().set(1f, 0.5f, 0.5f, 1f);
 * icon.setAlpha(0.8f);
 * }</pre>
 *
 * <h2>Size</h2>
 *
 * <p>A picture made without a size, or with {@link #setAutoSize(boolean)} turned on, takes the
 * natural pixel size of its image (or of its source region) every time the layout is measured.
 * Give it a size, and it stays that size no matter what image it shows.
 *
 * <h2>Scale modes</h2>
 *
 * <ul>
 *   <li>{@link ScaleMode#STRETCH} fills the whole widget and ignores the image's shape.
 *   <li>{@link ScaleMode#FIT} scales the image evenly until it fits fully inside the widget, which
 *       can leave empty bars along one side. This is the default.
 *   <li>{@link ScaleMode#FILL} scales the image evenly until it covers the widget, cropping what
 *       overflows.
 *   <li>{@link ScaleMode#NONE} draws the image at its natural size, cropping what overflows.
 * </ul>
 *
 * <p>For every mode except {@link ScaleMode#STRETCH}, the leftover space is split using
 * {@link #getAlign()}, which defaults to {@link FlixelAlign#CENTER}.
 *
 * <h2>Ownership</h2>
 *
 * <p>An image loaded from a {@link FlixelFile} is owned by the picture and released by
 * {@link #destroy()} or when it is replaced. An image built from a {@link FlixelFrame} is not
 * owned, so whoever created its texture stays responsible for it. A picture ignores the mouse by
 * default ({@link #interactive} is {@code false}), so clicks pass through to whatever is behind.
 */
public class FlixelUiPicture extends FlixelUiWidget {

  private float imageX;
  private float imageY;
  private float imageW;
  private float imageH;
  private int align = FlixelAlign.CENTER;

  /** The image shown, or {@code null} when the picture is empty. */
  @Nullable
  private FlixelUiImage image;

  /** The image for the source region, or {@code null} when the whole image is shown. */
  @Nullable
  private FlixelUiImage regionImage;

  @NotNull
  private ScaleMode scaleMode = ScaleMode.FIT;

  /** Whether {@link #image} was loaded by this picture, so this picture releases it. */
  private boolean ownsImage;

  private boolean autoSize;

  /** Creates an empty picture of size zero; give it an image with {@code setImage(...)}. */
  public FlixelUiPicture() {
    super(0f, 0f);
    interactive = false;
    autoSize = true;
  }

  /**
   * Creates a picture that loads an image file and takes its natural size.
   *
   * @param file The image file, such as {@code Flixel.files.internal("ui/logo.png")}.
   * @throws IllegalArgumentException If {@code file} is {@code null}.
   */
  public FlixelUiPicture(@NotNull FlixelFile file) {
    this();
    setImage(file);
  }

  /**
   * Creates a picture that shows an already loaded frame and takes its natural size.
   *
   * <p>The picture does not own the frame's texture.
   *
   * @param frame The frame to show, such as a region of a texture atlas.
   * @throws IllegalArgumentException If {@code frame} is {@code null} or was packed rotated.
   */
  public FlixelUiPicture(@NotNull FlixelFrame frame) {
    this();
    setImage(frame);
  }

  /**
   * Creates a picture of a fixed size that loads an image file.
   *
   * @param file The image file.
   * @param width The width in pixels.
   * @param height The height in pixels.
   * @throws IllegalArgumentException If {@code file} is {@code null}.
   */
  public FlixelUiPicture(@NotNull FlixelFile file, float width, float height) {
    this(file);
    setSize(width, height);
    autoSize = false;
  }

  /**
   * Creates a picture of a fixed size that shows an already loaded frame.
   *
   * @param frame The frame to show.
   * @param width The width in pixels.
   * @param height The height in pixels.
   * @throws IllegalArgumentException If {@code frame} is {@code null} or was packed rotated.
   */
  public FlixelUiPicture(@NotNull FlixelFrame frame, float width, float height) {
    this(frame);
    setSize(width, height);
    autoSize = false;
  }

  /** Takes the natural size of the shown image when auto size is on. */
  @Override
  protected void onMeasure() {
    if (autoSize) {
      width = getNaturalWidth();
      height = getNaturalHeight();
    }
  }

  /**
   * Returns the natural width when auto size is on, otherwise the current width.
   *
   * @return The preferred width in pixels.
   */
  @Override
  public float getPreferredWidth() {
    return autoSize ? getNaturalWidth() : width;
  }

  /**
   * Returns the natural height when auto size is on, otherwise the current height.
   *
   * @return The preferred height in pixels.
   */
  @Override
  public float getPreferredHeight() {
    return autoSize ? getNaturalHeight() : height;
  }

  /**
   * Draws the image inside the widget according to the scale mode, tinted by the widget's color
   * and combined alpha.
   *
   * @param batch The batch to draw into.
   * @param drawX The left edge in draw coordinates.
   * @param drawY The top edge in draw coordinates.
   * @param alpha The combined alpha of this picture and its ancestors.
   */
  @Override
  protected void drawSelf(@NotNull FlixelBatch batch, float drawX, float drawY, float alpha) {
    FlixelUiImage shown = getShownImage();
    if (shown == null || width <= 0f || height <= 0f) {
      return;
    }
    computeRect();
    if (imageW <= 0f || imageH <= 0f) {
      return;
    }
    boolean overflow = imageW > width || imageH > height;
    if (overflow && !pushClip(batch, screenX, screenY, width, height)) {
      popClip(batch);
      return;
    }
    FlixelColor tint = getColor();
    batch.setColor(tint.r, tint.g, tint.b, alpha);
    shown.draw(batch, drawX + imageX, drawY + imageY, imageW, imageH);
    batch.setColor(FlixelColor.WHITE);
    if (overflow) {
      popClip(batch);
    }
  }

  /**
   * Releases the image this picture loaded, if any, and clears the picture.
   */
  @Override
  public void destroy() {
    releaseImage();
    regionImage = null;
    super.destroy();
  }

  /**
   * Loads an image file and shows it, releasing the image this picture loaded before.
   *
   * <p>The source region is cleared. The picture owns the loaded image. See
   * {@link FlixelUiImage#load(FlixelFile)} for the browser-backend note about decoding.
   *
   * @param file The image file to load.
   * @throws IllegalArgumentException If {@code file} is {@code null}.
   */
  public void setImage(@NotNull FlixelFile file) {
    FlixelUiImage loaded = FlixelUiImage.load(file);
    releaseImage();
    image = loaded;
    ownsImage = true;
    regionImage = null;
    invalidateLayout();
  }

  /**
   * Shows an already loaded frame, releasing the image this picture loaded before.
   *
   * <p>The source region is cleared. The picture does not own the frame's texture.
   *
   * @param frame The frame to show.
   * @throws IllegalArgumentException If {@code frame} is {@code null} or was packed rotated.
   */
  public void setImage(@NotNull FlixelFrame frame) {
    FlixelUiImage wrapped = new FlixelUiImage(frame);
    releaseImage();
    image = wrapped;
    ownsImage = false;
    regionImage = null;
    invalidateLayout();
  }

  /**
   * Shows only a rectangle of the image, such as one cell of a sprite sheet.
   *
   * <p>The rectangle is measured in pixels from the top-left corner of the image (or of the atlas
   * frame it was built from) and is clamped to the image's bounds. Does nothing when the picture
   * has no image or the clamped rectangle is empty.
   *
   * @param x The left edge of the region.
   * @param y The top edge of the region.
   * @param regionWidth The width of the region.
   * @param regionHeight The height of the region.
   */
  public void setRegion(int x, int y, int regionWidth, int regionHeight) {
    FlixelUiImage base = image;
    if (base == null) {
      return;
    }
    FlixelFrame f = base.getFrame();
    int left = Math.max(0, Math.min(x, f.getRegionWidth()));
    int top = Math.max(0, Math.min(y, f.getRegionHeight()));
    int w = Math.min(regionWidth, f.getRegionWidth() - left);
    int h = Math.min(regionHeight, f.getRegionHeight() - top);
    if (w <= 0 || h <= 0) {
      return;
    }
    regionImage = new FlixelUiImage(new FlixelFrame(f.getTexture(), f.getRegionX() + left, f.getRegionY() + top, w, h));
    invalidateLayout();
  }

  /** Goes back to showing the whole image. */
  public void clearRegion() {
    if (regionImage != null) {
      regionImage = null;
      invalidateLayout();
    }
  }

  /**
   * Sets how the image is fitted into the widget.
   *
   * @param mode The scale mode; {@code null} counts as {@link ScaleMode#FIT}.
   */
  public void setScaleMode(@Nullable ScaleMode mode) {
    scaleMode = mode != null ? mode : ScaleMode.FIT;
  }

  /**
   * Sets where the image sits when it does not fill the widget.
   *
   * @param align A {@link FlixelAlign} constant such as {@link FlixelAlign#TOP_LEFT}.
   */
  public void setAlign(int align) {
    this.align = align;
  }

  /**
   * Sets whether the picture takes the natural size of its image on every layout pass.
   *
   * @param autoSize {@code true} to follow the image's size, {@code false} to keep the current size.
   */
  public void setAutoSize(boolean autoSize) {
    this.autoSize = autoSize;
    invalidateLayout();
  }

  /**
   * Returns the left edge of the drawn image, relative to the widget's left edge.
   *
   * <p>Negative when the image overflows and is cropped.
   *
   * @return The offset in pixels.
   */
  public float getImageX() {
    computeRect();
    return imageX;
  }

  /**
   * Returns the top edge of the drawn image, relative to the widget's top edge.
   *
   * @return The offset in pixels.
   */
  public float getImageY() {
    computeRect();
    return imageY;
  }

  /**
   * Returns the width the image is drawn at after scaling.
   *
   * @return The drawn width in pixels; {@code 0} when there is no image.
   */
  public float getImageWidth() {
    computeRect();
    return imageW;
  }

  /**
   * Returns the height the image is drawn at after scaling.
   *
   * @return The drawn height in pixels; {@code 0} when there is no image.
   */
  public float getImageHeight() {
    computeRect();
    return imageH;
  }

  /**
   * Returns the natural width of what is shown: the source region if one is set, otherwise the
   * whole image.
   *
   * @return The width in pixels; {@code 0} when there is no image.
   */
  public int getNaturalWidth() {
    FlixelUiImage shown = getShownImage();
    return shown != null ? shown.getNaturalWidth() : 0;
  }

  /**
   * Returns the natural height of what is shown.
   *
   * @return The height in pixels; {@code 0} when there is no image.
   * @see #getNaturalWidth()
   */
  public int getNaturalHeight() {
    FlixelUiImage shown = getShownImage();
    return shown != null ? shown.getNaturalHeight() : 0;
  }

  @NotNull
  public ScaleMode getScaleMode() {
    return scaleMode;
  }

  public int getAlign() {
    return align;
  }

  public boolean isAutoSize() {
    return autoSize;
  }

  /**
   * Returns the image this picture shows.
   *
   * @return The image, or {@code null} when the picture is empty.
   */
  @Nullable
  public FlixelUiImage getImage() {
    return image;
  }

  /**
   * Returns whether a source region is set.
   *
   * @return {@code true} after {@link #setRegion(int, int, int, int)} succeeded.
   */
  public boolean hasRegion() {
    return regionImage != null;
  }

  @Nullable
  private FlixelUiImage getShownImage() {
    FlixelUiImage region = regionImage;
    return region != null ? region : image;
  }

  private void releaseImage() {
    FlixelUiImage old = image;
    if (old != null && ownsImage) {
      old.destroy();
    }
    image = null;
    ownsImage = false;
  }

  /** Works out the drawn rectangle of the image from the widget size, scale mode, and alignment. */
  private void computeRect() {
    int nw = getNaturalWidth();
    int nh = getNaturalHeight();
    if (nw <= 0 || nh <= 0) {
      imageW = 0f;
      imageH = 0f;
      imageX = 0f;
      imageY = 0f;
      return;
    }
    float w;
    float h;
    switch (scaleMode) {
      case STRETCH -> {
        w = width;
        h = height;
      }
      case FIT -> {
        float k = Math.min(width / nw, height / nh);
        w = nw * k;
        h = nh * k;
      }
      case FILL -> {
        float k = Math.max(width / nw, height / nh);
        w = nw * k;
        h = nh * k;
      }
      default -> {
        w = nw;
        h = nh;
      }
    }
    imageW = w;
    imageH = h;
    if ((align & FlixelAlign.LEFT) != 0) {
      imageX = 0f;
    } else if ((align & FlixelAlign.RIGHT) != 0) {
      imageX = width - w;
    } else {
      imageX = (width - w) * 0.5f;
    }
    if ((align & FlixelAlign.TOP) != 0) {
      imageY = 0f;
    } else if ((align & FlixelAlign.BOTTOM) != 0) {
      imageY = height - h;
    } else {
      imageY = (height - h) * 0.5f;
    }
  }

  /** How a {@link FlixelUiPicture} fits its image into the widget's rectangle. */
  public enum ScaleMode {

    /** Draws the image at its natural size and crops what overflows. */
    NONE,

    /** Stretches the image to fill the whole widget, ignoring its shape. */
    STRETCH,

    /** Scales the image evenly until all of it fits inside the widget. */
    FIT,

    /** Scales the image evenly until it covers the whole widget, cropping the overflow. */
    FILL
  }
}
