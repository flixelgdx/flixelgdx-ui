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
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.graphics.FlixelGraphic;
import org.flixelgdx.graphics.FlixelNoopTexture;
import org.flixelgdx.graphics.FlixelTexture;
import org.flixelgdx.ui.FlixelUiHeadlessExtension;
import org.flixelgdx.util.FlixelColor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies how {@link FlixelUiImage} and {@link FlixelUiColorFill} draw.
 */
@ExtendWith(FlixelUiHeadlessExtension.class)
class FlixelUiImageAndFillTest {

  private static final float EPS = 0.0001f;

  private final FlixelTexture texture = new FlixelNoopTexture(256, 128);

  @Test
  void imageStretchesItsFrameOverTheRectangle() {
    FlixelFrame frame = new FlixelFrame(texture, 10, 20, 40, 30);
    FlixelUiImage image = new FlixelUiImage(frame);
    RecordingBatch batch = new RecordingBatch();
    image.draw(batch, 5, 6, 80, 60);
    assertEquals(1, batch.count);
    assertSame(frame, batch.frames[0]);
    assertEquals(5f, batch.xs[0], EPS);
    assertEquals(6f, batch.ys[0], EPS);
    assertEquals(80f, batch.widths[0], EPS);
    assertEquals(60f, batch.heights[0], EPS);
    assertSame(frame, image.getFrame());
    assertEquals(40, image.getNaturalWidth());
    assertEquals(30, image.getNaturalHeight());
    assertEquals(0f, image.getMinWidth(), EPS);
    assertEquals(0f, image.getMinHeight(), EPS);

    image.draw(batch, 0, 0, 0, 10);
    assertEquals(1, batch.count, "an empty rectangle draws nothing");
  }

  @Test
  void imagePlacesATrimmedFrameInsideItsOriginalBox() {
    FlixelFrame frame = new FlixelFrame(texture, 10, 20, 40, 30);
    frame.originalWidth = 50;
    frame.originalHeight = 40;
    frame.offsetX = 5;
    frame.offsetY = 4;
    FlixelUiImage image = new FlixelUiImage(frame);
    assertEquals(50, image.getNaturalWidth());
    assertEquals(40, image.getNaturalHeight());
    RecordingBatch batch = new RecordingBatch();
    image.draw(batch, 0, 0, 100, 80); // Twice the natural size.
    assertEquals(10f, batch.xs[0], EPS);
    assertEquals(8f, batch.ys[0], EPS);
    assertEquals(80f, batch.widths[0], EPS);
    assertEquals(60f, batch.heights[0], EPS);
  }

  @Test
  void imageRejectsRotatedFramesAndReleasesOnlyWhatItLoaded() {
    FlixelFrame rotated = new FlixelFrame(texture, 0, 0, 10, 10);
    rotated.rotated = true;
    assertThrows(IllegalArgumentException.class, () -> new FlixelUiImage(rotated));
    assertThrows(IllegalArgumentException.class, () -> new FlixelUiImage(null));
    assertThrows(IllegalArgumentException.class, () -> FlixelUiImage.load(null));

    FlixelUiImage loaded = FlixelUiImage.load(Flixel.files.internal("ui/plus.png"));
    FlixelGraphic graphic = loaded.getGraphic();
    assertNotNull(graphic);
    assertEquals(1, graphic.getRefCount());
    loaded.destroy();
    loaded.destroy();
    assertEquals(0, graphic.getRefCount());
    assertNull(loaded.getGraphic());

    assertNull(new FlixelUiImage(new FlixelFrame(texture)).getGraphic());
  }

  @Test
  void colorFillMultipliesTheBatchColorAndRestoresIt() {
    FlixelUiColorFill fill = new FlixelUiColorFill(1f, 0f, 0.5f, 0.5f);
    RecordingBatch batch = new RecordingBatch();
    batch.setColor(0.5f, 1f, 1f, 0.5f);
    fill.draw(batch, 3, 4, 50, 20);
    assertEquals(1, batch.count);
    assertEquals(0.5f, batch.colors[0], EPS);
    assertEquals(0f, batch.colors[1], EPS);
    assertEquals(0.5f, batch.colors[2], EPS);
    assertEquals(0.25f, batch.colors[3], EPS);
    assertEquals(3f, batch.xs[0], EPS);
    assertEquals(4f, batch.ys[0], EPS);
    assertEquals(50f, batch.widths[0], EPS);
    assertEquals(20f, batch.heights[0], EPS);
    assertEquals(1, batch.frames[0].getRegionWidth(), "draws the 1x1 white pixel");
    assertEquals(1, batch.frames[0].getRegionHeight());

    FlixelColor after = batch.getColor();
    assertEquals(0.5f, after.r, EPS);
    assertEquals(1f, after.g, EPS);
    assertEquals(1f, after.b, EPS);
    assertEquals(0.5f, after.a, EPS);

    fill.draw(batch, 0, 0, 1, 1);
    assertSame(batch.frames[0], batch.frames[1], "the white pixel is obtained once and reused");
  }

  @Test
  void colorFillOwnsACopyOfItsColor() {
    FlixelColor source = new FlixelColor(0f, 0f, 0f, 0.6f);
    FlixelUiColorFill fill = new FlixelUiColorFill(source);
    source.a = 1f;
    assertEquals(0.6f, fill.getColor().a, EPS);
    fill.setColor(FlixelColor.RED);
    assertEquals(1f, fill.getColor().r, EPS);
    assertEquals(0f, fill.getMinWidth(), EPS);
    assertEquals(0f, fill.getMinHeight(), EPS);
    assertThrows(IllegalArgumentException.class, () -> new FlixelUiColorFill(null));

    RecordingBatch batch = new RecordingBatch();
    fill.draw(batch, 0, 0, 0, 5);
    assertEquals(0, batch.count, "an empty rectangle draws nothing");
  }
}
