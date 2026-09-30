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
import org.flixelgdx.file.FlixelFile;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.graphics.FlixelNoopTexture;
import org.flixelgdx.graphics.FlixelTexture;
import org.flixelgdx.ui.FlixelUiPicture.ScaleMode;
import org.flixelgdx.ui.graphics.RecordingBatch;
import org.flixelgdx.util.FlixelAlign;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link FlixelUiPicture}: scaling-mode layout math, alignment, natural sizing, source
 * regions, tinting, and image ownership.
 */
@ExtendWith(FlixelUiHeadlessExtension.class)
class FlixelUiPictureTest {

  private static final float EPS = 1e-4f;

  private final FlixelTexture tex = new FlixelNoopTexture(256, 128);

  private FlixelUiDisplay ui;

  @BeforeEach
  void setUp() {
    Flixel.cameras.clear();
    FlixelCamera camera = new FlixelCamera(640, 360);
    Flixel.cameras.add(camera);
    ui = new FlixelUiDisplay(camera);
  }

  @AfterEach
  void tearDown() {
    Flixel.cameras.clear();
  }

  /** A 40x20 image in a 100x100 box. */
  private FlixelUiPicture picture(ScaleMode mode) {
    FlixelUiPicture p = new FlixelUiPicture(new FlixelFrame(tex, 0, 0, 40, 20), 100, 100);
    p.setScaleMode(mode);
    return p;
  }

  @Test
  void stretchFillsTheWholeBox() {
    FlixelUiPicture p = picture(ScaleMode.STRETCH);
    assertEquals(0f, p.getImageX(), EPS);
    assertEquals(0f, p.getImageY(), EPS);
    assertEquals(100f, p.getImageWidth(), EPS);
    assertEquals(100f, p.getImageHeight(), EPS);
  }

  @Test
  void fitKeepsTheShapeAndCentersInsideTheBox() {
    FlixelUiPicture p = picture(ScaleMode.FIT);
    assertEquals(100f, p.getImageWidth(), EPS);
    assertEquals(50f, p.getImageHeight(), EPS);
    assertEquals(0f, p.getImageX(), EPS);
    assertEquals(25f, p.getImageY(), EPS);
  }

  @Test
  void fillCoversTheBoxAndOverflowsCentered() {
    FlixelUiPicture p = picture(ScaleMode.FILL);
    assertEquals(200f, p.getImageWidth(), EPS);
    assertEquals(100f, p.getImageHeight(), EPS);
    assertEquals(-50f, p.getImageX(), EPS);
    assertEquals(0f, p.getImageY(), EPS);
  }

  @Test
  void noneDrawsTheNaturalSizeCentered() {
    FlixelUiPicture p = picture(ScaleMode.NONE);
    assertEquals(40f, p.getImageWidth(), EPS);
    assertEquals(20f, p.getImageHeight(), EPS);
    assertEquals(30f, p.getImageX(), EPS);
    assertEquals(40f, p.getImageY(), EPS);
  }

  @Test
  void alignmentSplitsTheLeftoverSpace() {
    FlixelUiPicture p = picture(ScaleMode.FIT);
    p.setAlign(FlixelAlign.TOP_LEFT);
    assertEquals(0f, p.getImageX(), EPS);
    assertEquals(0f, p.getImageY(), EPS);
    p.setAlign(FlixelAlign.BOTTOM_RIGHT);
    assertEquals(0f, p.getImageX(), EPS);
    assertEquals(50f, p.getImageY(), EPS);
    p.setScaleMode(ScaleMode.NONE);
    assertEquals(60f, p.getImageX(), EPS);
    assertEquals(80f, p.getImageY(), EPS);
  }

  @Test
  void nullScaleModeFallsBackToFit() {
    FlixelUiPicture p = picture(ScaleMode.NONE);
    p.setScaleMode(null);
    assertEquals(ScaleMode.FIT, p.getScaleMode());
  }

  @Test
  void takesTheNaturalSizeWhenNoSizeIsGiven() {
    FlixelUiPicture p = new FlixelUiPicture(new FlixelFrame(tex, 0, 0, 40, 20));
    assertTrue(p.isAutoSize());
    ui.add(p);
    ui.layout();
    assertEquals(40f, p.getWidth(), EPS);
    assertEquals(20f, p.getHeight(), EPS);
    assertEquals(40f, p.getPreferredWidth(), EPS);
    assertFalse(p.interactive, "a picture lets clicks through by default");
  }

  @Test
  void aGivenSizeIsKept() {
    FlixelUiPicture p = picture(ScaleMode.FIT);
    assertFalse(p.isAutoSize());
    ui.add(p);
    ui.layout();
    assertEquals(100f, p.getWidth(), EPS);
    assertEquals(100f, p.getHeight(), EPS);
  }

  @Test
  void sourceRegionChangesTheNaturalSizeAndIsClamped() {
    FlixelUiPicture p = new FlixelUiPicture(new FlixelFrame(tex, 10, 20, 64, 32));
    p.setRegion(16, 8, 16, 16);
    assertTrue(p.hasRegion());
    assertEquals(16, p.getNaturalWidth());
    assertEquals(16, p.getNaturalHeight());
    p.setRegion(48, 24, 100, 100);
    assertEquals(16, p.getNaturalWidth(), "clamped to the image edge");
    assertEquals(8, p.getNaturalHeight());
    p.setRegion(500, 500, 10, 10);
    assertEquals(16, p.getNaturalWidth(), "an empty clamped region is ignored");
    p.clearRegion();
    assertFalse(p.hasRegion());
    assertEquals(64, p.getNaturalWidth());
  }

  @Test
  void regionDrawsTheSubRectangleOfTheTexture() {
    FlixelUiPicture p = new FlixelUiPicture(new FlixelFrame(tex, 10, 20, 64, 32), 16, 16);
    p.setScaleMode(ScaleMode.STRETCH);
    p.setRegion(16, 8, 16, 16);
    ui.add(p);
    ui.layout();
    RecordingBatch batch = new RecordingBatch();
    p.draw(batch);
    assertEquals(1, batch.getDrawCount());
    FlixelFrame drawn = batch.getFrame(0);
    assertEquals(26, drawn.getRegionX());
    assertEquals(28, drawn.getRegionY());
    assertEquals(16, drawn.getRegionWidth());
  }

  @Test
  void drawUsesTheTintAndTheLayoutRectangle() {
    FlixelUiPicture p = picture(ScaleMode.FIT);
    p.setPosition(10, 20);
    p.getColor().r = 0.5f;
    p.setAlpha(0.5f);
    FlixelFrame frame = p.getImage().getFrame();
    ui.add(p);
    ui.layout();
    RecordingBatch batch = new RecordingBatch();
    p.draw(batch);
    assertEquals(1, batch.getDrawCount());
    assertSame(frame, batch.getFrame(0));
    assertEquals(10f, batch.getX(0), EPS);
    assertEquals(45f, batch.getY(0), EPS);
    assertEquals(100f, batch.getWidth(0), EPS);
    assertEquals(50f, batch.getHeight(0), EPS);
    assertEquals(0.5f, batch.getAlpha(0), EPS);
    assertEquals(0.5f, batch.getRed(0), EPS);
  }

  @Test
  void emptyPictureDrawsNothing() {
    FlixelUiPicture p = new FlixelUiPicture();
    p.setSize(50, 50);
    ui.add(p);
    ui.layout();
    RecordingBatch batch = new RecordingBatch();
    p.draw(batch);
    assertEquals(0, batch.getDrawCount());
    assertEquals(0f, p.getImageWidth(), EPS);
  }

  @Test
  void rejectsNullAndRotatedInput() {
    FlixelFrame rotated = new FlixelFrame(tex, 0, 0, 10, 10);
    rotated.rotated = true;
    assertThrows(IllegalArgumentException.class, () -> new FlixelUiPicture(rotated));
    assertThrows(IllegalArgumentException.class, () -> new FlixelUiPicture().setImage((FlixelFrame) null));
  }

  @Test
  void loadsFilesAndSwitchesBetweenFilesAndFrames() {
    FlixelUiPicture p = new FlixelUiPicture(Flixel.files.internal("ui/plus.png"));
    assertNotNull(p.getImage());
    FlixelFrame frame = new FlixelFrame(tex, 0, 0, 8, 8);
    p.setImage(frame);
    assertSame(frame, p.getImage().getFrame());
    p.setImage(Flixel.files.internal("ui/plus.png"));
    assertNotNull(p.getImage());
    assertFalse(p.hasRegion());
    p.destroy();
    assertEquals(0, p.getNaturalWidth(), "a destroyed picture has released its image");
    assertThrows(IllegalArgumentException.class, () -> new FlixelUiPicture((FlixelFile) null));
  }
}
