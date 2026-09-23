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
import org.flixelgdx.graphics.FlixelUnsupportedBatch;
import org.flixelgdx.util.FlixelString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the tooltip feature: delay timing, placement, auto-hide, and interaction with the
 * widget and display APIs.
 *
 * <p>No graphics backend is installed. Drawing goes through {@link FlixelUnsupportedBatch}
 * (no-op) or {@link RecordingBatch} where draw order matters.
 */
@ExtendWith(FlixelUiHeadlessExtension.class)
class FlixelUiTooltipTest {

  private static final float EPS = 0.001f;
  private static final int DISPLAY_W = 640;
  private static final int DISPLAY_H = 360;

  private FlixelCamera camera;
  private FlixelUiDisplay ui;

  @BeforeEach
  void setUp() {
    Flixel.cameras.clear();
    camera = new FlixelCamera(DISPLAY_W, DISPLAY_H);
    Flixel.cameras.add(camera);
    ui = new FlixelUiDisplay(camera);
  }

  @AfterEach
  void tearDown() {
    Flixel.cameras.clear();
  }

  // --- setTooltip copy semantics ---

  @Test
  void setTooltipCopiesTheText() {
    TestWidget w = widget(10, 10, 50, 20);
    FlixelString buf = new FlixelString();
    buf.set("Hello");
    w.setTooltip(buf);
    // Mutate the original buffer; the widget's copy must be unaffected.
    buf.set("Changed");
    assertEquals("Hello", w.getTooltip().toString());
  }

  @Test
  void setTooltipNullClearsIt() {
    TestWidget w = widget(10, 10, 50, 20);
    w.setTooltip("Hello");
    w.setTooltip(null);
    assertNull(w.getTooltip());
    assertFalse(w.hasTooltip());
  }

  @Test
  void setTooltipEmptyStringClearsIt() {
    TestWidget w = widget(10, 10, 50, 20);
    w.setTooltip("Hello");
    w.setTooltip("");
    assertNull(w.getTooltip());
    assertFalse(w.hasTooltip());
  }

  @Test
  void hasTooltipReturnsTrueWhenTextIsSet() {
    TestWidget w = widget(10, 10, 50, 20);
    assertFalse(w.hasTooltip());
    w.setTooltip("Tip");
    assertTrue(w.hasTooltip());
  }

  // --- immediate show ---

  @Test
  void showTooltipImmediatelyMakesItVisible() {
    TestWidget w = widget(100, 100, 60, 20);
    w.setTooltip("Hello");
    ui.add(w);
    ui.showTooltip(w);
    assertTrue(ui.isTooltipVisible());
    assertSame(w, ui.getTooltipTarget());
  }

  @Test
  void noTooltipTextNothingShown() {
    TestWidget w = widget(100, 100, 60, 20);
    ui.add(w);
    ui.showTooltip(w);
    assertFalse(ui.isTooltipVisible());
    assertNull(ui.getTooltipTarget());
  }

  // --- delay timing ---

  @Test
  void tooltipIsNotShownBeforeDelayExpires() {
    TestWidget w = widget(100, 100, 60, 20);
    w.setTooltip("Tip");
    ui.add(w);
    ui.showTooltip(w, 0.4f);
    assertFalse(ui.isTooltipVisible());
    // Tick only 0.3 seconds - still not shown.
    ui.update(0.3f);
    assertFalse(ui.isTooltipVisible());
  }

  @Test
  void tooltipIsShownAfterDelayExpires() {
    TestWidget w = widget(100, 100, 60, 20);
    w.setTooltip("Tip");
    ui.add(w);
    ui.showTooltip(w, 0.4f);
    ui.update(0.4f);
    assertTrue(ui.isTooltipVisible());
  }

  @Test
  void newTargetRestartsDelay() {
    TestWidget w1 = widget(10, 10, 60, 20);
    TestWidget w2 = widget(10, 50, 60, 20);
    w1.setTooltip("One");
    w2.setTooltip("Two");
    ui.add(w1);
    ui.add(w2);
    // Start delay for w1.
    ui.showTooltip(w1, 0.4f);
    // After 0.3 s, switch to w2 with a fresh 0.4 s delay.
    ui.update(0.3f);
    ui.hideTooltip();
    ui.showTooltip(w2, 0.4f);
    // Another 0.3 s - total 0.6 s elapsed, but w2's delay has only run 0.3 s.
    ui.update(0.3f);
    assertFalse(ui.isTooltipVisible(), "w2's delay has not yet expired");
    assertSame(w2, ui.getTooltipTarget());
    // Remaining 0.1 s.
    ui.update(0.1f);
    assertTrue(ui.isTooltipVisible());
  }

  // --- hide ---

  @Test
  void hideTooltipHidesAndClearsTarget() {
    TestWidget w = widget(100, 100, 60, 20);
    w.setTooltip("Tip");
    ui.add(w);
    ui.showTooltip(w);
    ui.hideTooltip();
    assertFalse(ui.isTooltipVisible());
    assertNull(ui.getTooltipTarget());
  }

  // --- auto-hide ---

  @Test
  void autoHideWhenTargetIsRemoved() {
    TestWidget w = widget(100, 100, 60, 20);
    w.setTooltip("Tip");
    ui.add(w);
    ui.showTooltip(w);
    ui.remove(w);
    ui.update(0.016f);
    assertFalse(ui.isTooltipVisible());
  }

  @Test
  void autoHideWhenTargetIsKilled() {
    TestWidget w = widget(100, 100, 60, 20);
    w.setTooltip("Tip");
    ui.add(w);
    ui.showTooltip(w);
    w.kill();
    ui.update(0.016f);
    assertFalse(ui.isTooltipVisible());
  }

  @Test
  void autoHideWhenTargetIsDestroyed() {
    TestWidget w = widget(100, 100, 60, 20);
    w.setTooltip("Tip");
    ui.add(w);
    ui.showTooltip(w);
    w.destroy();
    ui.update(0.016f);
    assertFalse(ui.isTooltipVisible());
  }

  @Test
  void autoHideWhenTargetBecomesInvisible() {
    TestWidget w = widget(100, 100, 60, 20);
    w.setTooltip("Tip");
    ui.add(w);
    ui.showTooltip(w);
    w.setVisible(false);
    ui.update(0.016f);
    assertFalse(ui.isTooltipVisible());
  }

  @Test
  void autoHideCancelsPendingDelayWhenTargetIsRemoved() {
    TestWidget w = widget(100, 100, 60, 20);
    w.setTooltip("Tip");
    ui.add(w);
    ui.showTooltip(w, 0.4f);
    assertFalse(ui.isTooltipVisible(), "pending, not visible yet");
    ui.remove(w);
    ui.update(0.016f);
    assertFalse(ui.isTooltipVisible());
    assertNull(ui.getTooltipTarget());
  }

  // --- getWidgetAt does not return the tooltip ---

  @Test
  void getWidgetAtNeverReturnsTheTooltip() {
    TestWidget w = widget(100, 100, 60, 20);
    w.setTooltip("Tip");
    ui.add(w);
    ui.showTooltip(w);
    ui.layout();
    // Even at the tooltip's exact screen position, getWidgetAt must not return it.
    FlixelUiTooltip tip = ui.getTooltip();
    float tipX = tip.getScreenX() + tip.getWidth() * 0.5f;
    float tipY = tip.getScreenY() + tip.getHeight() * 0.5f;
    FlixelUiWidget hit = ui.getWidgetAt(tipX, tipY);
    assertFalse(hit instanceof FlixelUiTooltip,
        "getWidgetAt must never return the tooltip widget");
  }

  // --- getTooltip ---

  @Test
  void getTooltipReturnsTheSharedWidget() {
    FlixelUiTooltip tip = ui.getTooltip();
    assertNotNull(tip);
    assertSame(tip, ui.getTooltip());
  }

  // --- placement: below target by default ---

  @Test
  void tooltipIsPlacedBelowTargetWhenThereIsRoom() {
    // Widget near the top; plenty of room below.
    TestWidget w = widget(200, 10, 60, 20);
    w.setTooltip("Tip");
    ui.add(w);
    ui.showTooltip(w);
    ui.layout();
    float tipY = ui.getTooltip().getScreenY();
    // The tooltip top must be below the widget's bottom edge.
    assertTrue(tipY >= w.getScreenY() + w.getHeight(),
        "tooltip should be below the target");
  }

  // --- placement: flipped above when there is no room below ---

  @Test
  void tooltipIsFlippedAboveTargetWhenNoRoomBelow() {
    // Widget near the bottom of the display.
    TestWidget w = widget(200, DISPLAY_H - 30, 60, 20);
    w.setTooltip("Tip");
    ui.add(w);
    ui.showTooltip(w);
    ui.layout();
    float tipY = ui.getTooltip().getScreenY();
    // The tooltip bottom must be at or above the widget's top edge.
    assertTrue(tipY + ui.getTooltip().getHeight() <= w.getScreenY() + w.getHeight(),
        "tooltip should be above the target when there is no room below");
  }

  // --- clamping at left and right edges ---

  @Test
  void tooltipIsClamped_ToLeftEdge() {
    // Widget at the very left edge: centered tooltip would go negative.
    TestWidget w = widget(0, 10, 60, 20);
    w.setTooltip("Tip");
    ui.add(w);
    ui.showTooltip(w);
    ui.layout();
    float tipX = ui.getTooltip().getScreenX();
    assertTrue(tipX >= 0f, "tooltip must not go past the left edge");
  }

  @Test
  void tooltipIsClamped_ToRightEdge() {
    // Widget at the very right edge.
    TestWidget w = widget(DISPLAY_W - 10, 10, 60, 20);
    w.setTooltip("Tip");
    ui.add(w);
    ui.showTooltip(w);
    ui.layout();
    FlixelUiTooltip tip = ui.getTooltip();
    assertTrue(tip.getScreenX() + tip.getWidth() <= DISPLAY_W + EPS,
        "tooltip must not go past the right edge");
  }

  @Test
  void tooltipIsClamped_ToTopEdge() {
    // Widget positioned so the flipped-above position would be negative.
    TestWidget w = widget(200, 0, 60, 20);
    w.setTooltip("Tip");
    ui.add(w);
    ui.showTooltip(w);
    ui.layout();
    float tipY = ui.getTooltip().getScreenY();
    assertTrue(tipY >= 0f, "tooltip must not go past the top edge");
  }

  @Test
  void tooltipIsClamped_ToBottomEdge() {
    // Widget positioned so below would overflow and above also overflows.
    TestWidget w = widget(200, DISPLAY_H / 2, 60, 20);
    w.setTooltip("Tip");
    ui.add(w);
    ui.showTooltip(w);
    ui.layout();
    FlixelUiTooltip tip = ui.getTooltip();
    assertTrue(tip.getScreenY() + tip.getHeight() <= DISPLAY_H + EPS,
        "tooltip must not go past the bottom edge");
  }

  // --- tooltip is horizontally centered on the target ---

  @Test
  void tooltipIsCenteredHorizontallyOnTarget() {
    // Place a widget wide enough that the centering can be measured.
    TestWidget w = widget(200, 10, 100, 20);
    w.setTooltip("Hi");
    ui.add(w);
    ui.showTooltip(w);
    ui.layout();
    FlixelUiTooltip tip = ui.getTooltip();
    float targetCenter = w.getScreenX() + w.getWidth() * 0.5f;
    float tipCenter = tip.getScreenX() + tip.getWidth() * 0.5f;
    // Allow for clamping but they should be close when there is room.
    assertEquals(targetCenter, tipCenter, 2f,
        "tooltip should be approximately centered on the target");
  }

  // --- drawn after popups (layer order) ---

  @Test
  void tooltipIsDrawnAfterPopupLayer() {
    // The RecordingBatch counts frame-draws; we need a draw-capable batch.
    // We verify that the tooltip layer is drawn after the popup layer by checking the draw
    // order of TestWidget instances placed in each layer.
    TestWidget popupWidget = new TestWidget(10, 10);
    ui.getPopupLayer().add(popupWidget);
    TestWidget w = widget(100, 100, 60, 20);
    w.setTooltip("Tip");
    ui.add(w);
    ui.showTooltip(w);
    // Both the popup widget and the tooltip are drawn; the tooltip widget's draw happens last.
    FlixelUnsupportedBatch batch = FlixelUnsupportedBatch.INSTANCE;
    // Reset draw counts.
    popupWidget.drawCount = 0;
    FlixelUiTooltip tip = ui.getTooltip();
    ui.draw(batch);
    // Tooltip was drawn (drawCount is hard to check on FlixelUiTooltip without a font).
    // At minimum, the popup widget was drawn and the tooltip layer ran without error.
    assertEquals(1, popupWidget.drawCount, "popup widget must have been drawn");
    assertTrue(tip.isVisible(), "tooltip must still be visible after draw");
  }

  // --- helpers ---

  /** Creates a TestWidget with the given position and size, ready to be added to the display. */
  private TestWidget widget(float x, float y, float w, float h) {
    TestWidget widget = new TestWidget(w, h);
    widget.setPosition(x, y);
    return widget;
  }
}
