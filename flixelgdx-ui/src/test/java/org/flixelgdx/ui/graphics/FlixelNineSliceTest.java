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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies how {@link FlixelNineSlice} cuts its frame, validates its insets, and draws.
 */
@ExtendWith(FlixelUiHeadlessExtension.class)
class FlixelNineSliceTest {

  private static final float EPS = 0.0001f;

  private final FlixelTexture texture = new FlixelNoopTexture(256, 128);

  @Test
  void cutsAPlainFrameIntoNinePieces() {
    FlixelTexture small = new FlixelNoopTexture(30, 20);
    FlixelNineSlice slice = new FlixelNineSlice(new FlixelFrame(small), 4, 3, 5, 2);
    assertRegion(slice, FlixelNineSlice.TOP_LEFT, 0, 0, 4, 3);
    assertRegion(slice, FlixelNineSlice.TOP, 4, 0, 21, 3);
    assertRegion(slice, FlixelNineSlice.TOP_RIGHT, 25, 0, 5, 3);
    assertRegion(slice, FlixelNineSlice.LEFT, 0, 3, 4, 15);
    assertRegion(slice, FlixelNineSlice.CENTER, 4, 3, 21, 15);
    assertRegion(slice, FlixelNineSlice.RIGHT, 25, 3, 5, 15);
    assertRegion(slice, FlixelNineSlice.BOTTOM_LEFT, 0, 18, 4, 2);
    assertRegion(slice, FlixelNineSlice.BOTTOM, 4, 18, 21, 2);
    assertRegion(slice, FlixelNineSlice.BOTTOM_RIGHT, 25, 18, 5, 2);
    for (int i = 0; i < 9; i++) {
      assertSame(small, slice.getPart(i).getTexture());
    }
  }

  @Test
  void cutsInsideAnAtlasFramesRegion() {
    FlixelNineSlice slice = new FlixelNineSlice(new FlixelFrame(texture, 100, 50, 30, 20), 4, 3, 5, 2);
    assertRegion(slice, FlixelNineSlice.TOP_LEFT, 100, 50, 4, 3);
    assertRegion(slice, FlixelNineSlice.CENTER, 104, 53, 21, 15);
    assertRegion(slice, FlixelNineSlice.BOTTOM_RIGHT, 125, 68, 5, 2);
    FlixelFrame center = slice.getPart(FlixelNineSlice.CENTER);
    assertEquals(104f / 256f, center.getU(), EPS);
    assertEquals(53f / 128f, center.getV(), EPS);
    assertEquals(125f / 256f, center.getU2(), EPS);
    assertEquals(68f / 128f, center.getV2(), EPS);
  }

  @Test
  void rejectsBadInsetsAndUnsupportedFrames() {
    FlixelFrame frame = new FlixelFrame(texture, 0, 0, 30, 20);
    assertThrows(IllegalArgumentException.class, () -> new FlixelNineSlice(null, 1, 1, 1, 1));
    assertThrows(IllegalArgumentException.class, () -> new FlixelNineSlice(frame, -1, 0, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> new FlixelNineSlice(frame, 0, 0, 0, -1));
    assertThrows(IllegalArgumentException.class, () -> new FlixelNineSlice(frame, 15, 1, 15, 1),
        "left + right equal to the width leaves no center");
    assertThrows(IllegalArgumentException.class, () -> new FlixelNineSlice(frame, 1, 12, 1, 9));

    FlixelFrame rotated = new FlixelFrame(texture, 0, 0, 30, 20);
    rotated.rotated = true;
    assertThrows(IllegalArgumentException.class, () -> new FlixelNineSlice(rotated, 1, 1, 1, 1));

    FlixelFrame trimmed = new FlixelFrame(texture, 0, 0, 30, 20);
    trimmed.offsetX = 2;
    trimmed.originalWidth = 34;
    assertThrows(IllegalArgumentException.class, () -> new FlixelNineSlice(trimmed, 1, 1, 1, 1));
  }

  @Test
  void drawsNinePiecesWithFixedCornersAndStretchedMiddle() {
    FlixelNineSlice slice = new FlixelNineSlice(new FlixelFrame(texture, 0, 0, 30, 20), 4, 3, 5, 2);
    RecordingBatch batch = new RecordingBatch();
    slice.draw(batch, 10, 20, 100, 60);
    assertEquals(9, batch.count);
    assertDraw(batch, 0, slice, FlixelNineSlice.TOP_LEFT, 10, 20, 4, 3);
    assertDraw(batch, 1, slice, FlixelNineSlice.TOP, 14, 20, 91, 3);
    assertDraw(batch, 2, slice, FlixelNineSlice.TOP_RIGHT, 105, 20, 5, 3);
    assertDraw(batch, 3, slice, FlixelNineSlice.LEFT, 10, 23, 4, 55);
    assertDraw(batch, 4, slice, FlixelNineSlice.CENTER, 14, 23, 91, 55);
    assertDraw(batch, 5, slice, FlixelNineSlice.RIGHT, 105, 23, 5, 55);
    assertDraw(batch, 6, slice, FlixelNineSlice.BOTTOM_LEFT, 10, 78, 4, 2);
    assertDraw(batch, 7, slice, FlixelNineSlice.BOTTOM, 14, 78, 91, 2);
    assertDraw(batch, 8, slice, FlixelNineSlice.BOTTOM_RIGHT, 105, 78, 5, 2);
  }

  @Test
  void shrinksCornersProportionallyWhenSmallerThanTheMinimum() {
    FlixelNineSlice slice = new FlixelNineSlice(new FlixelFrame(texture, 0, 0, 30, 20), 4, 3, 5, 2);
    RecordingBatch batch = new RecordingBatch();
    slice.draw(batch, 0, 0, 6, 4);
    // 6 / 9 of the horizontal borders and 4 / 5 of the vertical ones; no middle row or column.
    assertEquals(4, batch.count);
    float lw = 4f * 6f / 9f;
    float rw = 5f * 6f / 9f;
    float th = 3f * 4f / 5f;
    float bh = 2f * 4f / 5f;
    assertDraw(batch, 0, slice, FlixelNineSlice.TOP_LEFT, 0, 0, lw, th);
    assertDraw(batch, 1, slice, FlixelNineSlice.TOP_RIGHT, lw, 0, rw, th);
    assertDraw(batch, 2, slice, FlixelNineSlice.BOTTOM_LEFT, 0, th, lw, bh);
    assertDraw(batch, 3, slice, FlixelNineSlice.BOTTOM_RIGHT, lw, th, rw, bh);
  }

  @Test
  void shrinksOnlyTheAxisThatIsTooSmall() {
    FlixelNineSlice slice = new FlixelNineSlice(new FlixelFrame(texture, 0, 0, 30, 20), 4, 3, 5, 2);
    RecordingBatch batch = new RecordingBatch();
    slice.draw(batch, 0, 0, 6, 60);
    assertEquals(6, batch.count, "three rows of two pieces; the center column is skipped");
    assertDraw(batch, 2, slice, FlixelNineSlice.LEFT, 0, 3, 4f * 6f / 9f, 55);
    assertDraw(batch, 3, slice, FlixelNineSlice.RIGHT, 4f * 6f / 9f, 3, 5f * 6f / 9f, 55);
  }

  @Test
  void skipsZeroSizedPieces() {
    FlixelNineSlice slice = new FlixelNineSlice(new FlixelFrame(texture, 0, 0, 30, 20), 0, 3, 0, 0);
    RecordingBatch batch = new RecordingBatch();
    slice.draw(batch, 0, 0, 50, 50);
    assertEquals(2, batch.count, "only the top edge and the center have any size");
    assertDraw(batch, 0, slice, FlixelNineSlice.TOP, 0, 0, 50, 3);
    assertDraw(batch, 1, slice, FlixelNineSlice.CENTER, 0, 3, 50, 47);

    batch.count = 0;
    slice.draw(batch, 0, 0, 0, 50);
    slice.draw(batch, 0, 0, 50, -1);
    assertEquals(0, batch.count);
  }

  @Test
  void reportsMinimumSizeAndInsets() {
    FlixelNineSlice slice = new FlixelNineSlice(new FlixelFrame(texture, 0, 0, 30, 20), 4, 3, 5, 2);
    assertEquals(9f, slice.getMinWidth(), EPS);
    assertEquals(5f, slice.getMinHeight(), EPS);
    assertEquals(4, slice.getLeft());
    assertEquals(3, slice.getTop());
    assertEquals(5, slice.getRight());
    assertEquals(2, slice.getBottom());
  }

  @Test
  void releasesOnlyAnImageItLoadedItself() {
    FlixelNineSlice loaded = FlixelNineSlice.load(Flixel.files.internal("ui/button.png"), 0, 0, 0, 0);
    FlixelGraphic graphic = loaded.getGraphic();
    assertNotNull(graphic);
    assertEquals(1, graphic.getRefCount());
    loaded.destroy();
    assertEquals(0, graphic.getRefCount());
    assertNull(loaded.getGraphic());
    loaded.destroy(); // A second destroy does nothing.
    assertEquals(0, graphic.getRefCount());

    FlixelNineSlice fromFrame = new FlixelNineSlice(new FlixelFrame(texture), 1, 1, 1, 1);
    assertNull(fromFrame.getGraphic());
    fromFrame.destroy();

    assertThrows(IllegalArgumentException.class, () -> FlixelNineSlice.load(null, 0, 0, 0, 0));
  }

  private static void assertRegion(FlixelNineSlice slice, int part, int x, int y, int w, int h) {
    FlixelFrame f = slice.getPart(part);
    assertEquals(x, f.getRegionX(), "region x of part " + part);
    assertEquals(y, f.getRegionY(), "region y of part " + part);
    assertEquals(w, f.getRegionWidth(), "region width of part " + part);
    assertEquals(h, f.getRegionHeight(), "region height of part " + part);
  }

  private static void assertDraw(RecordingBatch batch, int call, FlixelNineSlice slice, int part,
      float x, float y, float w, float h) {
    assertSame(slice.getPart(part), batch.frames[call], "frame of draw " + call);
    assertEquals(x, batch.xs[call], EPS, "x of draw " + call);
    assertEquals(y, batch.ys[call], EPS, "y of draw " + call);
    assertEquals(w, batch.widths[call], EPS, "width of draw " + call);
    assertEquals(h, batch.heights[call], EPS, "height of draw " + call);
  }
}
