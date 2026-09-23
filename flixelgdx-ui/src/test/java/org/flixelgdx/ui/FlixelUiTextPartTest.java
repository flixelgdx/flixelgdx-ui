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

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.text.FlixelFont;
import org.flixelgdx.ui.graphics.RecordingBatch;
import org.flixelgdx.util.FlixelColor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Verifies the camera-array lifecycle of {@link FlixelUiTextPart}: allocated once at construction,
 * never reallocated on attach, draw, or detach.
 */
@ExtendWith(FlixelUiHeadlessExtension.class)
class FlixelUiTextPartTest {

  private static final float EPS = 1e-4f;

  @Test
  void camerasArrayIsAllocatedOnceAndHandedToTheText() {
    FlixelUiTextPart part = new FlixelUiTextPart();
    FlixelCamera[] cameras = part.getCameras();

    assertNull(cameras[0], "no camera before attach");
    assertSame(cameras, part.getText().cameras, "the same array is handed to the text at construction");
  }

  @Test
  void attachFillsSlotZeroWithTheDisplayCamera() {
    FlixelUiTextPart part = new FlixelUiTextPart();
    FlixelCamera[] cameras = part.getCameras();

    FlixelCamera camera = new FlixelCamera(640, 360);
    FlixelUiDisplay ui = new FlixelUiDisplay(camera);
    part.attach(ui);

    assertSame(cameras, part.getCameras(), "attach does not reallocate the array");
    assertSame(cameras, part.getText().cameras, "text still uses the same array after attach");
    assertSame(camera, cameras[0], "slot 0 holds the display camera");
  }

  @Test
  void detachClearsSlotZeroWithoutReallocating() {
    FlixelUiTextPart part = new FlixelUiTextPart();
    FlixelCamera[] cameras = part.getCameras();

    FlixelUiDisplay ui = new FlixelUiDisplay(new FlixelCamera(640, 360));
    part.attach(ui);
    part.detach();

    assertSame(cameras, part.getCameras(), "detach does not reallocate the array");
    assertSame(cameras, part.getText().cameras, "text still uses the same array after detach");
    assertNull(cameras[0], "slot 0 is cleared after detach");
  }

  @Test
  void drawDoesNotReallocateTheCamerasArray() {
    Flixel.cameras.clear();
    FlixelCamera camera = new FlixelCamera(640, 360);
    Flixel.cameras.add(camera);
    try {
      FlixelUiTextPart part = new FlixelUiTextPart();
      FlixelUiDisplay ui = new FlixelUiDisplay(camera);
      part.attach(ui);
      part.setText("hi");
      part.getText().setFont(buildFont());

      FlixelCamera[] before = part.getCameras();
      RecordingBatch batch = new RecordingBatch();
      part.draw(batch, 0, 0, FlixelColor.WHITE, 1f);

      assertSame(before, part.getCameras(), "draw does not reallocate the cameras array");
      assertSame(before, part.getText().cameras, "text still uses the same array after draw");
      assertEquals(camera, before[0], "slot 0 still holds the display camera after draw");
      assertEquals(1f, part.getText().getAlpha(), EPS, "the text's own alpha is restored after draw");

      part.destroy();
    } finally {
      Flixel.cameras.clear();
    }
  }

  /**
   * Builds a minimal font with only the glyphs needed for the test text.
   */
  private static FlixelFont buildFont() {
    String fnt = "common lineHeight=12 base=10 scaleW=64 scaleH=64\n"
        + "char id=104 x=0 y=0 width=8 height=8 xoffset=0 yoffset=0 xadvance=10 page=0\n"
        + "char id=105 x=8 y=0 width=8 height=8 xoffset=0 yoffset=0 xadvance=10 page=0\n";
    return FlixelFont.fromFnt(fnt, null);
  }
}
