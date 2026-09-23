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

import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.graphics.FlixelNoopTexture;
import org.flixelgdx.graphics.FlixelTexture;
import org.flixelgdx.ui.graphics.FlixelUiImage;
import org.flixelgdx.ui.graphics.RecordingBatch;
import org.flixelgdx.ui.skin.FlixelUiCheckboxStyle;
import org.flixelgdx.ui.skin.FlixelUiSkin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link FlixelUiCheckbox}: background selection for every state and checked value,
 * click and setChecked behavior, signal counts, disabled handling, and preferred size.
 */
@ExtendWith(FlixelUiHeadlessExtension.class)
class FlixelUiCheckboxTest {

  private static final float EPS = 1e-4f;

  private final FlixelTexture tex = new FlixelNoopTexture(256, 256);
  private final RecordingBatch batch = new RecordingBatch();

  private FlixelFrame offFrame;
  private FlixelFrame onFrame;
  private FlixelFrame offOverFrame;
  private FlixelFrame onOverFrame;
  private FlixelFrame offDownFrame;
  private FlixelFrame onDownFrame;
  private FlixelFrame offDisabledFrame;
  private FlixelFrame onDisabledFrame;
  private FlixelFrame offFocusedFrame;
  private FlixelFrame onFocusedFrame;
  private FlixelUiCheckboxStyle full;

  @BeforeEach
  void setUp() {
    offFrame = new FlixelFrame(tex, 0, 0, 16, 16);
    onFrame = new FlixelFrame(tex, 16, 0, 16, 16);
    offOverFrame = new FlixelFrame(tex, 32, 0, 16, 16);
    onOverFrame = new FlixelFrame(tex, 48, 0, 16, 16);
    offDownFrame = new FlixelFrame(tex, 64, 0, 16, 16);
    onDownFrame = new FlixelFrame(tex, 80, 0, 16, 16);
    offDisabledFrame = new FlixelFrame(tex, 96, 0, 16, 16);
    onDisabledFrame = new FlixelFrame(tex, 112, 0, 16, 16);
    offFocusedFrame = new FlixelFrame(tex, 128, 0, 16, 16);
    onFocusedFrame = new FlixelFrame(tex, 144, 0, 16, 16);
    full = new FlixelUiCheckboxStyle();
    full.off = new FlixelUiImage(offFrame);
    full.on = new FlixelUiImage(onFrame);
    full.offOver = new FlixelUiImage(offOverFrame);
    full.onOver = new FlixelUiImage(onOverFrame);
    full.offDown = new FlixelUiImage(offDownFrame);
    full.onDown = new FlixelUiImage(onDownFrame);
    full.offDisabled = new FlixelUiImage(offDisabledFrame);
    full.onDisabled = new FlixelUiImage(onDisabledFrame);
    full.offFocused = new FlixelUiImage(offFocusedFrame);
    full.onFocused = new FlixelUiImage(onFocusedFrame);
    full.boxSize = 16;
    full.spacing = 4;
  }

  // --- background priority: unchecked states ---

  @Test
  void boxUsesOffForPlainUnchecked() {
    FlixelUiCheckbox cb = unchecked();
    assertSame(offFrame, drawnBackground(cb));
  }

  @Test
  void boxUsesOffFocusedWhenFocused() {
    FlixelUiCheckbox cb = unchecked();
    cb.focus();
    assertSame(offFocusedFrame, drawnBackground(cb));
  }

  @Test
  void boxUsesOffOverWhenHovered() {
    FlixelUiCheckbox cb = unchecked();
    cb.hover();
    assertSame(offOverFrame, drawnBackground(cb), "hovered beats focused");
  }

  @Test
  void boxUsesOffDownWhenPressed() {
    FlixelUiCheckbox cb = unchecked();
    cb.press();
    assertSame(offDownFrame, drawnBackground(cb), "pressed beats hovered");
  }

  @Test
  void boxUsesOffDisabledWhenDisabled() {
    FlixelUiCheckbox cb = unchecked();
    cb.press();
    cb.hover();
    cb.setEnabled(false);
    assertSame(offDisabledFrame, drawnBackground(cb), "disabled beats everything");
  }

  // --- background priority: checked states ---

  @Test
  void boxUsesOnForPlainChecked() {
    FlixelUiCheckbox cb = checked();
    assertSame(onFrame, drawnBackground(cb));
  }

  @Test
  void boxUsesOnFocusedWhenFocused() {
    FlixelUiCheckbox cb = checked();
    cb.focus();
    assertSame(onFocusedFrame, drawnBackground(cb));
  }

  @Test
  void boxUsesOnOverWhenHovered() {
    FlixelUiCheckbox cb = checked();
    cb.hover();
    assertSame(onOverFrame, drawnBackground(cb));
  }

  @Test
  void boxUsesOnDownWhenPressed() {
    FlixelUiCheckbox cb = checked();
    cb.press();
    assertSame(onDownFrame, drawnBackground(cb));
  }

  @Test
  void boxUsesOnDisabledWhenDisabled() {
    FlixelUiCheckbox cb = checked();
    cb.setEnabled(false);
    assertSame(onDisabledFrame, drawnBackground(cb));
  }

  // --- fallback to plain background when variant is absent ---

  @Test
  void missingVariantsFallBackToPlainBackground() {
    FlixelUiCheckboxStyle minimal = new FlixelUiCheckboxStyle();
    minimal.off = new FlixelUiImage(offFrame);
    minimal.on = new FlixelUiImage(onFrame);
    minimal.boxSize = 16;
    // Null label: avoids text drawing in drawSelf, which needs a camera not available in this test.
    FlixelUiCheckbox cb = new FlixelUiCheckbox(null);
    cb.setStyle(minimal);
    cb.setSize(100, 20);

    cb.focus();
    assertSame(offFrame, drawnBackground(cb), "no offFocused -> falls back to off");
    cb.hover();
    assertSame(offFrame, drawnBackground(cb), "no offOver -> falls back to off");
    cb.press();
    assertSame(offFrame, drawnBackground(cb), "no offDown -> falls back to off");
    cb.setEnabled(false);
    assertSame(offFrame, drawnBackground(cb), "no offDisabled -> falls back to off");

    cb.setEnabled(true);
    cb.setChecked(true, false);
    cb.focus();
    assertSame(onFrame, drawnBackground(cb), "no onFocused -> falls back to on");
  }

  // --- click / toggle / setChecked ---

  @Test
  void clickTogglesAndFiresOnChange() {
    int[] count = new int[1];
    FlixelUiCheckbox cb = new FlixelUiCheckbox("x");
    cb.onChange.add(c -> count[0]++);
    cb.setStyle(full);

    assertFalse(cb.isChecked());
    cb.click();
    assertTrue(cb.isChecked());
    assertEquals(1, count[0]);
    cb.click();
    assertFalse(cb.isChecked());
    assertEquals(2, count[0]);
  }

  @Test
  void toggleIsAliasForClick() {
    int[] count = new int[1];
    FlixelUiCheckbox cb = new FlixelUiCheckbox("x");
    cb.onChange.add(c -> count[0]++);
    cb.toggle();
    assertTrue(cb.isChecked());
    assertEquals(1, count[0]);
  }

  @Test
  void setCheckedFiresOnChangeOnlyWhenValueChanges() {
    int[] count = new int[1];
    FlixelUiCheckbox cb = new FlixelUiCheckbox("x");
    cb.onChange.add(c -> count[0]++);
    cb.setChecked(false); // No change.
    assertEquals(0, count[0]);
    cb.setChecked(true);
    assertEquals(1, count[0]);
    cb.setChecked(true); // No change.
    assertEquals(1, count[0]);
  }

  @Test
  void setCheckedSilentDoesNotFireOnChange() {
    int[] count = new int[1];
    FlixelUiCheckbox cb = new FlixelUiCheckbox("x");
    cb.onChange.add(c -> count[0]++);
    cb.setChecked(true, false);
    assertTrue(cb.isChecked());
    assertEquals(0, count[0]);
  }

  // --- disabled ignores click ---

  @Test
  void disabledCheckboxIgnoresClick() {
    int[] count = new int[1];
    FlixelUiCheckbox cb = new FlixelUiCheckbox("x");
    cb.onChange.add(c -> count[0]++);
    cb.setEnabled(false);
    cb.click();
    assertFalse(cb.isChecked());
    assertEquals(0, count[0]);
  }

  // --- preferred size ---

  @Test
  void preferredSizeIsBoxPlusSpacingPlusLabel() {
    FlixelUiCheckbox cb = new FlixelUiCheckbox(null);
    cb.setStyle(full);
    // No label: width = boxSize only.
    assertEquals(16f, cb.getPreferredWidth(), EPS);
    assertEquals(16f, cb.getPreferredHeight(), EPS);
  }

  @Test
  void preferredSizeUsesMinSizeWhenBoxSizeIsZero() {
    FlixelUiCheckboxStyle style = new FlixelUiCheckboxStyle();
    style.off = new FlixelUiImage(new FlixelFrame(tex, 0, 0, 24, 20));
    style.on = new FlixelUiImage(new FlixelFrame(tex, 24, 0, 24, 20));
    style.boxSize = 0;
    FlixelUiCheckbox cb = new FlixelUiCheckbox(null);
    cb.setStyle(style);
    assertEquals(24f, cb.getPreferredWidth(), EPS);
    assertEquals(20f, cb.getPreferredHeight(), EPS);
  }

  // --- skin style resolution ---

  @Test
  void skinStyleIsResolvedWhenAttached() {
    FlixelUiSkin skin = new FlixelUiSkin();
    skin.add("default", full);
    FlixelUiDisplay ui = new FlixelUiDisplay(
        new org.flixelgdx.FlixelCamera(640, 360), skin);
    FlixelUiCheckbox cb = new FlixelUiCheckbox("x");
    assertNull(cb.getStyle());
    ui.add(cb);
    assertSame(full, cb.getStyle());
  }

  // --- batch color restored to white ---

  @Test
  void batchColorRestoredAfterDraw() {
    FlixelUiCheckbox cb = unchecked();
    cb.setSize(100, 20);
    batch.reset();
    cb.drawSelf(batch, 0, 0, 1f);
    assertEquals(1f, batch.getColor().r, EPS);
    assertEquals(1f, batch.getColor().a, EPS);
  }

  // --- alpha multiplied ---

  @Test
  void alphaMultipliedOnBoxDraw() {
    FlixelUiCheckbox cb = unchecked();
    cb.setSize(100, 20);
    batch.reset();
    cb.drawSelf(batch, 0, 0, 0.5f);
    assertTrue(batch.getDrawCount() >= 1);
    assertEquals(0.5f, batch.getAlpha(0), EPS);
  }

  // --- helpers ---

  private FlixelUiCheckbox unchecked() {
    // Null label avoids text drawing in drawSelf, which needs a camera not available in this test.
    FlixelUiCheckbox cb = new FlixelUiCheckbox(null);
    cb.setStyle(full);
    cb.setSize(100, 20);
    return cb;
  }

  private FlixelUiCheckbox checked() {
    FlixelUiCheckbox cb = new FlixelUiCheckbox(null, true);
    cb.setStyle(full);
    cb.setSize(100, 20);
    return cb;
  }

  private FlixelFrame drawnBackground(FlixelUiCheckbox cb) {
    batch.reset();
    cb.drawSelf(batch, 0, 0, 1f);
    assertTrue(batch.getDrawCount() >= 1, "a background was drawn");
    return batch.getFrame(0);
  }
}
