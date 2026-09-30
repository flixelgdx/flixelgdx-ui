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
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.graphics.FlixelNoopTexture;
import org.flixelgdx.graphics.FlixelTexture;
import org.flixelgdx.ui.graphics.FlixelUiImage;
import org.flixelgdx.ui.graphics.RecordingBatch;
import org.flixelgdx.ui.skin.FlixelUiSliderStyle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link FlixelUiSlider}: value clamping and snapping, pointer mapping in both
 * orientations, signals, disabled handling, drawing, and {@link FlixelUiPointer} integration.
 */
@ExtendWith(FlixelUiHeadlessExtension.class)
class FlixelUiSliderTest {

  private static final float EPS = 1e-4f;

  private final FlixelTexture tex = new FlixelNoopTexture(64, 64);

  private FlixelUiDisplay ui;
  private FlixelFrame trackFrame;
  private FlixelFrame fillFrame;
  private FlixelFrame thumbFrame;
  private FlixelFrame overFrame;
  private FlixelFrame downFrame;
  private FlixelFrame disabledFrame;
  private FlixelUiSliderStyle style;

  @BeforeEach
  void setUp() {
    Flixel.cameras.clear();
    FlixelCamera camera = new FlixelCamera(640, 360);
    Flixel.cameras.add(camera);
    ui = new FlixelUiDisplay(camera);
    trackFrame = new FlixelFrame(tex, 0, 0, 8, 8);
    fillFrame = new FlixelFrame(tex, 8, 0, 8, 8);
    thumbFrame = new FlixelFrame(tex, 16, 0, 8, 8);
    overFrame = new FlixelFrame(tex, 24, 0, 8, 8);
    downFrame = new FlixelFrame(tex, 32, 0, 8, 8);
    disabledFrame = new FlixelFrame(tex, 40, 0, 8, 8);
    style = new FlixelUiSliderStyle();
    style.track = new FlixelUiImage(trackFrame);
    style.fill = new FlixelUiImage(fillFrame);
    style.thumb = new FlixelUiImage(thumbFrame);
    style.thumbOver = new FlixelUiImage(overFrame);
    style.thumbDown = new FlixelUiImage(downFrame);
    style.thumbDisabled = new FlixelUiImage(disabledFrame);
    style.thumbWidth = 20;
    style.thumbHeight = 20;
    style.trackThickness = 6;
  }

  @AfterEach
  void tearDown() {
    Flixel.cameras.clear();
  }

  private FlixelUiSlider slider(float length, float min, float max, float value, boolean vertical) {
    FlixelUiSlider s = new FlixelUiSlider(length, min, max, value, vertical);
    s.setStyle(style);
    s.setPosition(10, 20);
    ui.add(s);
    ui.layout();
    return s;
  }

  @Test
  void valueIsClampedIntoTheRange() {
    FlixelUiSlider s = slider(120, 0f, 10f, 5f, false);
    s.setValue(50f);
    assertEquals(10f, s.getValue(), EPS);
    s.setValue(-3f);
    assertEquals(0f, s.getValue(), EPS);
    assertEquals(new FlixelUiSlider(100, 0f, 10f, 99f).getValue(), 10f, EPS);
  }

  @Test
  void rejectsAReversedRange() {
    assertThrows(IllegalArgumentException.class, () -> new FlixelUiSlider(100, 5f, 1f, 2f));
    FlixelUiSlider s = slider(120, 0f, 10f, 5f, false);
    assertThrows(IllegalArgumentException.class, () -> s.setRange(3f, 2f));
  }

  @Test
  void stepSnapsTheValueToTheNearestMultiple() {
    FlixelUiSlider s = slider(120, 0f, 10f, 0f, false);
    s.setStep(2.5f);
    s.setValue(3.9f);
    assertEquals(5f, s.getValue(), EPS);
    s.setValue(3.7f);
    assertEquals(2.5f, s.getValue(), EPS);
    s.setValue(9.9f);
    assertEquals(10f, s.getValue(), EPS);
  }

  @Test
  void stepSnapsFromTheMinimumNotFromZero() {
    FlixelUiSlider s = slider(120, 1f, 6f, 1f, false);
    s.setStep(2f);
    s.setValue(3.4f);
    assertEquals(3f, s.getValue(), EPS);
    s.setValue(5.9f);
    assertEquals(5f, s.getValue(), EPS, "7 would pass the maximum and is clamped to 6, but 5 is nearer");
  }

  @Test
  void onChangeFiresOnlyWhenTheValueChanges() {
    int[] count = new int[1];
    FlixelUiSlider s = slider(120, 0f, 10f, 5f, false);
    s.onChange.add(x -> count[0]++);
    s.setValue(5f);
    assertEquals(0, count[0]);
    s.setValue(6f);
    assertEquals(1, count[0]);
    s.setValue(6f, false);
    s.setValue(7f, false);
    assertEquals(1, count[0], "silent set does not notify");
    assertEquals(7f, s.getValue(), EPS);
    s.setValue(100f);
    s.setValue(200f);
    assertEquals(2, count[0], "a second clamp to the same value does not notify");
  }

  @Test
  void setRangeReclampsTheValue() {
    int[] count = new int[1];
    FlixelUiSlider s = slider(120, 0f, 10f, 8f, false);
    s.onChange.add(x -> count[0]++);
    s.setRange(0f, 5f);
    assertEquals(5f, s.getValue(), EPS);
    assertEquals(1, count[0]);
  }

  @Test
  void stepUpAndDownMoveByOneStepOrATenthOfTheRange() {
    FlixelUiSlider s = slider(120, 0f, 10f, 5f, false);
    s.stepUp();
    assertEquals(6f, s.getValue(), EPS);
    s.stepDown();
    s.stepDown();
    assertEquals(4f, s.getValue(), EPS);
    s.setStep(2f);
    s.stepUp();
    assertEquals(6f, s.getValue(), EPS);
    s.setValue(10f);
    s.stepUp();
    assertEquals(10f, s.getValue(), EPS);
  }

  @Test
  void ratioReflectsPositionInTheRange() {
    FlixelUiSlider s = slider(120, 10f, 20f, 15f, false);
    assertEquals(0.5f, s.getRatio(), EPS);
    s.setRatio(0.25f);
    assertEquals(12.5f, s.getValue(), EPS);
    s.setRatio(9f);
    assertEquals(20f, s.getValue(), EPS);
    assertEquals(0f, new FlixelUiSlider(50, 3f, 3f, 3f).getRatio(), EPS);
  }

  @Test
  void horizontalPointerMapsThumbCenterToValue() {
    // Length 120, thumb 20: the thumb center travels from x = 20 to x = 120 (screen).
    FlixelUiSlider s = slider(120, 0f, 100f, 0f, false);
    assertEquals(0f, s.valueAt(20f, 25f), EPS);
    assertEquals(50f, s.valueAt(70f, 25f), EPS);
    assertEquals(100f, s.valueAt(120f, 25f), EPS);
    assertEquals(0f, s.valueAt(-500f, 25f), EPS);
    assertEquals(100f, s.valueAt(500f, 25f), EPS);
  }

  @Test
  void verticalSliderGrowsUpward() {
    // Length 120 tall at y = 20: the thumb center travels from y = 30 (max) to y = 130 (min).
    FlixelUiSlider s = slider(120, 0f, 100f, 0f, true);
    assertEquals(100f, s.valueAt(15f, 30f), EPS);
    assertEquals(50f, s.valueAt(15f, 80f), EPS);
    assertEquals(0f, s.valueAt(15f, 130f), EPS);
    assertEquals(120f, s.getHeight(), EPS);
    assertEquals(20f, s.getWidth(), EPS, "the cross size comes from the style");
  }

  @Test
  void pointerDownJumpsDragFollowsAndUpEnds() {
    int[] count = new int[1];
    FlixelUiSlider s = slider(120, 0f, 100f, 0f, false);
    s.onChange.add(x -> count[0]++);
    s.pointerDown(70f, 25f);
    assertTrue(s.isDragging());
    assertTrue(s.isPressed());
    assertEquals(50f, s.getValue(), EPS);
    s.pointerDrag(95f, 400f);
    assertEquals(75f, s.getValue(), EPS, "the drag works while the pointer is off the track");
    s.pointerUp();
    assertFalse(s.isDragging());
    assertFalse(s.isPressed());
    s.pointerDrag(20f, 25f);
    assertEquals(75f, s.getValue(), EPS, "dragging after release does nothing");
    assertEquals(2, count[0]);
  }

  @Test
  void disabledSliderIgnoresThePointerAndSteps() {
    FlixelUiSlider s = slider(120, 0f, 100f, 10f, false);
    s.setEnabled(false);
    s.pointerDown(120f, 25f);
    s.stepUp();
    assertFalse(s.isDragging());
    assertEquals(10f, s.getValue(), EPS);
    s.setValue(40f);
    assertEquals(40f, s.getValue(), EPS, "programmatic changes still work");
  }

  @Test
  void disablingMidDragEndsTheDrag() {
    FlixelUiSlider s = slider(120, 0f, 100f, 0f, false);
    s.pointerDown(70f, 25f);
    s.setEnabled(false);
    assertFalse(s.isDragging());
  }

  @Test
  void setVerticalSwapsTheSize() {
    FlixelUiSlider s = new FlixelUiSlider(200);
    assertEquals(200f, s.getWidth(), EPS);
    s.setVertical(true);
    assertTrue(s.isVertical());
    assertEquals(200f, s.getHeight(), EPS);
    assertEquals(0f, s.getWidth(), EPS);
  }

  @Test
  void drawsTrackFillAndThumbAtTheRightPlaces() {
    FlixelUiSlider s = slider(120, 0f, 100f, 50f, false);
    RecordingBatch batch = new RecordingBatch();
    s.draw(batch);
    assertEquals(3, batch.getDrawCount());
    // Track: full length, 6 thick, centered in the 20 pixel cross size.
    assertSame(trackFrame, batch.getFrame(0));
    assertEquals(10f, batch.getX(0), EPS);
    assertEquals(27f, batch.getY(0), EPS);
    assertEquals(120f, batch.getWidth(0), EPS);
    assertEquals(6f, batch.getHeight(0), EPS);
    // Fill: from the start to the middle of the thumb (offset 50 + 10).
    assertSame(fillFrame, batch.getFrame(1));
    assertEquals(60f, batch.getWidth(1), EPS);
    // Thumb: offset 50 along the 100 pixels of travel.
    assertSame(thumbFrame, batch.getFrame(2));
    assertEquals(60f, batch.getX(2), EPS);
    assertEquals(20f, batch.getY(2), EPS);
    assertEquals(20f, batch.getWidth(2), EPS);
  }

  @Test
  void verticalDrawPutsTheMinimumAtTheBottom() {
    FlixelUiSlider s = slider(120, 0f, 100f, 0f, true);
    RecordingBatch batch = new RecordingBatch();
    s.draw(batch);
    assertEquals(3, batch.getDrawCount());
    assertSame(thumbFrame, batch.getFrame(2));
    assertEquals(140f, batch.getY(2) + 20f, EPS, "thumb bottom sits on the slider bottom");
  }

  @Test
  void thumbVariantFollowsTheState() {
    FlixelUiSlider s = slider(120, 0f, 100f, 50f, false);
    assertSame(thumbFrame, drawnThumb(s));
    s.hover();
    assertSame(overFrame, drawnThumb(s));
    s.press();
    assertSame(downFrame, drawnThumb(s));
    s.release();
    s.unhover();
    s.setEnabled(false);
    assertSame(disabledFrame, drawnThumb(s));
  }

  @Test
  void missingThumbVariantsFallBackToThumbAndNoFillDrawsTwo() {
    style.thumbOver = null;
    style.fill = null;
    FlixelUiSlider s = slider(120, 0f, 100f, 50f, false);
    s.hover();
    RecordingBatch batch = new RecordingBatch();
    s.draw(batch);
    assertEquals(2, batch.getDrawCount());
    assertSame(thumbFrame, batch.getFrame(1));
  }

  @Test
  void pointerDrivesTheSliderThroughDownMoveAndUp() {
    FlixelUiPointer pointer = new FlixelUiPointer(ui);
    FlixelUiSlider s = slider(120, 0f, 100f, 0f, false);
    pointer.move(70f, 25f);
    pointer.down();
    assertEquals(50f, s.getValue(), EPS);
    assertSame(s, pointer.getPressed());
    pointer.move(120f, 25f);
    assertEquals(100f, s.getValue(), EPS);
    pointer.up();
    assertFalse(s.isDragging());
    assertFalse(s.isPressed());
  }

  private FlixelFrame drawnThumb(FlixelUiSlider s) {
    RecordingBatch batch = new RecordingBatch();
    s.draw(batch);
    return batch.getFrame(batch.getDrawCount() - 1);
  }
}
