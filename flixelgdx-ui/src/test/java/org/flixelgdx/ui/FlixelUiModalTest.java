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
import org.flixelgdx.ui.graphics.FlixelUiColorFill;
import org.flixelgdx.ui.graphics.RecordingBatch;
import org.flixelgdx.ui.skin.FlixelUiModalStyle;
import org.flixelgdx.ui.skin.FlixelUiSkin;
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
 * Verifies {@link FlixelUiModal}: open/close lifecycle, signals, hit-testing restrictions,
 * stale-state cleanup, tooltip hiding, destroyOnClose, and backdrop drawing order.
 */
@ExtendWith(FlixelUiHeadlessExtension.class)
class FlixelUiModalTest {

  private static final float EPS = 1e-4f;

  private FlixelCamera camera;
  private FlixelUiDisplay ui;

  @BeforeEach
  void setUp() {
    Flixel.cameras.clear();
    camera = new FlixelCamera(640, 360);
    Flixel.cameras.add(camera);
    ui = new FlixelUiDisplay(camera);
  }

  // Returns a modal ready for use in a skin-less display (no skin entry needed).
  private static FlixelUiModal modal(float w, float h) {
    FlixelUiModal m = new FlixelUiModal(w, h);
    m.setModalStyle(new FlixelUiModalStyle());
    return m;
  }

  @Test
  void openAndCloseIsOpen() {
    FlixelUiModal m = modal(100, 80);
    assertFalse(m.isOpen());
    ui.openModal(m);
    assertTrue(m.isOpen());
    ui.closeModal(m);
    assertFalse(m.isOpen());
  }

  @Test
  void isModalOpenAndCount() {
    FlixelUiModal m = modal(100, 80);
    assertFalse(ui.isModalOpen());
    assertEquals(0, ui.getModalCount());
    ui.openModal(m);
    assertTrue(ui.isModalOpen());
    assertEquals(1, ui.getModalCount());
    ui.closeModal(m);
    assertFalse(ui.isModalOpen());
    assertEquals(0, ui.getModalCount());
  }

  @Test
  void onOpenFiredOnceAfterAttach() {
    FlixelUiModal m = modal(100, 80);
    int[] opens = { 0 };
    m.onOpen.add(modal -> opens[0]++);
    ui.openModal(m);
    assertEquals(1, opens[0]);
  }

  @Test
  void onCloseFiredAfterRemoval() {
    FlixelUiModal m = modal(100, 80);
    int[] closes = { 0 };
    m.onClose.add(modal -> closes[0]++);
    ui.openModal(m);
    assertEquals(0, closes[0]);
    ui.closeModal(m);
    assertEquals(1, closes[0]);
  }

  @Test
  void signalOrderIsOpenThenClose() {
    FlixelUiModal m = modal(50, 50);
    StringBuilder order = new StringBuilder();
    m.onOpen.add(modal -> order.append("open"));
    m.onClose.add(modal -> order.append("close"));
    ui.openModal(m);
    ui.closeModal(m);
    assertEquals("openclose", order.toString());
  }

  @Test
  void reopenMovesToTopWithoutFiringOnOpen() {
    FlixelUiModal first = modal(100, 80);
    FlixelUiModal second = modal(100, 80);
    int[] opens = { 0 };
    first.onOpen.add(modal -> opens[0]++);
    ui.openModal(first);
    ui.openModal(second);
    assertEquals(1, opens[0]);
    assertSame(second, ui.getTopModal());
    ui.openModal(first); // Moves to top.
    assertEquals(1, opens[0], "onOpen must not fire again");
    assertSame(first, ui.getTopModal());
  }

  @Test
  void stackedModalsAndCloseSpecificFromMiddle() {
    FlixelUiModal a = modal(100, 80);
    FlixelUiModal b = modal(100, 80);
    FlixelUiModal c = modal(100, 80);
    ui.openModal(a);
    ui.openModal(b);
    ui.openModal(c);
    assertEquals(3, ui.getModalCount());
    assertSame(c, ui.getTopModal());
    ui.closeModal(b); // Close from the middle.
    assertEquals(2, ui.getModalCount());
    assertSame(c, ui.getTopModal()); // c stays on top.
    assertFalse(b.isOpen());
    assertTrue(a.isOpen());
    assertTrue(c.isOpen());
  }

  @Test
  void closeModalClosesTopOne() {
    FlixelUiModal a = modal(100, 80);
    FlixelUiModal b = modal(100, 80);
    ui.openModal(a);
    ui.openModal(b);
    ui.closeModal();
    assertFalse(b.isOpen());
    assertTrue(a.isOpen());
    assertSame(a, ui.getTopModal());
  }

  @Test
  void getTopModalReturnsNullWhenNoneOpen() {
    assertNull(ui.getTopModal());
  }

  @Test
  void getWidgetAtRestrictedToTopModal() {
    TestWidget behind = new TestWidget(640, 360);
    ui.add(behind);
    FlixelUiModal m = modal(200, 150);
    ui.openModal(m);
    ui.layout();
    // Center of the display (0..640, 0..360): modal centered at (220, 105) with size 200x150.
    float mx = camera.getWorldWidth() / 2f;
    float my = camera.getWorldHeight() / 2f;
    // Inside the modal - should return the modal itself (empty space inside it).
    FlixelUiWidget hit = ui.getWidgetAt(mx, my);
    assertSame(m, hit);
    // Outside the modal - should return null, not the widget behind it.
    assertNull(ui.getWidgetAt(5f, 5f));
    // The widget behind is not reachable while the modal is open.
    assertNull(ui.getWidgetAt(5f, 5f));
  }

  @Test
  void getWidgetAtReturnsModalForEmptySpaceInsideIt() {
    FlixelUiModal m = modal(200, 150);
    ui.openModal(m);
    ui.layout();
    float mx = camera.getWorldWidth() / 2f;
    float my = camera.getWorldHeight() / 2f;
    assertSame(m, ui.getWidgetAt(mx, my));
  }

  @Test
  void getWidgetAtReturnsNullOutsideTopModal() {
    FlixelUiModal m = modal(200, 150);
    ui.openModal(m);
    ui.layout();
    // Top-left corner of the display is outside the centered 200x150 modal.
    assertNull(ui.getWidgetAt(5f, 5f));
  }

  @Test
  void destroyOnCloseDestroysAfterOnClose() {
    FlixelUiModal m = modal(100, 80);
    m.destroyOnClose = true;
    boolean[] closeFiredBeforeDestroy = { false };
    m.onClose.add(modal -> closeFiredBeforeDestroy[0] = modal.isExists());
    ui.openModal(m);
    ui.closeModal(m);
    // onClose fired while the modal was still alive.
    assertTrue(closeFiredBeforeDestroy[0]);
    // After onClose, destroy() was called.
    assertFalse(m.isExists());
  }

  @Test
  void tooltipHiddenWhenTargetModalCloses() {
    FlixelUiModal m = modal(200, 150);
    TestWidget inside = new TestWidget(50, 50);
    m.add(inside);
    inside.setTooltip("hint");
    ui.openModal(m);
    ui.layout();
    ui.showTooltip(inside);
    assertTrue(ui.isTooltipVisible());
    ui.closeModal(m);
    assertFalse(ui.isTooltipVisible());
    assertNull(ui.getTooltipTarget());
  }

  @Test
  void staleHoverClearedOnClose() {
    FlixelUiModal m = modal(200, 150);
    TestWidget w = new TestWidget(50, 50);
    m.add(w);
    ui.openModal(m);
    w.hover();
    assertTrue(w.isHovered());
    int[] unhoverCount = { 0 };
    w.onUnhover.add(widget -> unhoverCount[0]++);
    ui.closeModal(m);
    assertFalse(w.isHovered());
    assertEquals(1, unhoverCount[0], "onUnhover must fire once");
  }

  @Test
  void stalePressedClearedWithoutOnRelease() {
    FlixelUiModal m = modal(200, 150);
    TestWidget w = new TestWidget(50, 50);
    m.add(w);
    ui.openModal(m);
    w.press();
    assertTrue(w.isPressed());
    int[] releaseCount = { 0 };
    w.onRelease.add(widget -> releaseCount[0]++);
    ui.closeModal(m);
    assertFalse(w.isPressed());
    assertEquals(0, releaseCount[0], "onRelease must not fire");
  }

  @Test
  void backdropDrawnOncePerModalBeforeItsPanel() {
    FlixelTexture tex = new FlixelNoopTexture(4, 4);
    FlixelFrame panelFrame = new FlixelFrame(tex, 0, 0, 4, 4);

    FlixelUiSkin skin = new FlixelUiSkin();
    FlixelUiColorFill backdrop = new FlixelUiColorFill(0f, 0f, 0f, 0.5f);
    FlixelUiModalStyle ms = new FlixelUiModalStyle();
    ms.backdrop = backdrop;
    ms.background = new org.flixelgdx.ui.graphics.FlixelUiImage(panelFrame);
    skin.add("default", ms);

    FlixelUiDisplay ui2 = new FlixelUiDisplay(camera, skin);
    FlixelUiModal m = new FlixelUiModal(200, 150);
    ui2.openModal(m);
    ui2.layout();

    RecordingBatch batch = new RecordingBatch();
    // Draw the root layer first (nothing), then the modal layer.
    // Each modal draws: backdrop (full-screen fill) then panel background.
    ui2.getModalLayer().drawTree(batch, 1f);
    // 2 draws: backdrop fill + panel background.
    assertEquals(2, batch.getDrawCount());
    // First draw is the backdrop - it covers the full display (640x360).
    assertEquals(0f, batch.getX(0), EPS);
    assertEquals(0f, batch.getY(0), EPS);
    assertEquals(640f, batch.getWidth(0), EPS);
    assertEquals(360f, batch.getHeight(0), EPS);
    // Second draw is the panel background at the modal's position.
    assertEquals(200f, batch.getWidth(1), EPS);
    assertEquals(150f, batch.getHeight(1), EPS);
  }

  @Test
  void twoStackedModalsBackdropOrderAfterRoot() {
    FlixelTexture tex = new FlixelNoopTexture(4, 4);
    FlixelUiSkin skin = new FlixelUiSkin();
    FlixelUiColorFill backdrop = new FlixelUiColorFill(0f, 0f, 0f, 0.5f);
    FlixelUiModalStyle ms = new FlixelUiModalStyle();
    ms.backdrop = backdrop;
    ms.background = new org.flixelgdx.ui.graphics.FlixelUiImage(new FlixelFrame(tex, 0, 0, 4, 4));
    skin.add("default", ms);

    FlixelUiDisplay ui2 = new FlixelUiDisplay(camera, skin);
    FlixelUiModal first = new FlixelUiModal(200, 150);
    FlixelUiModal second = new FlixelUiModal(100, 80);
    ui2.openModal(first);
    ui2.openModal(second);
    ui2.layout();

    RecordingBatch batch = new RecordingBatch();
    ui2.getModalLayer().drawTree(batch, 1f);
    // 4 draws: backdrop1, panel1, backdrop2, panel2.
    assertEquals(4, batch.getDrawCount());
    // Backdrops are full-display.
    assertEquals(640f, batch.getWidth(0), EPS);
    assertEquals(640f, batch.getWidth(2), EPS);
    // Panels at their own sizes.
    assertEquals(200f, batch.getWidth(1), EPS);
    assertEquals(100f, batch.getWidth(3), EPS);
  }

  @Test
  void modalSelfClosesViaCloseMethod() {
    FlixelUiModal m = modal(100, 80);
    ui.openModal(m);
    assertTrue(m.isOpen());
    m.close();
    assertFalse(m.isOpen());
  }

  @Test
  void closeNoOpWhenNotOpen() {
    FlixelUiModal m = modal(100, 80);
    m.close(); // Should not throw.
    assertFalse(m.isOpen());
  }

  @Test
  void closeModalNoOpWhenNoModalOpen() {
    ui.closeModal(); // Should not throw when no modal is open.
    assertNull(ui.getTopModal());
  }

  @Test
  void destroyDisplayDestroysOpenModals() {
    FlixelUiModal m = modal(100, 80);
    ui.openModal(m);
    assertTrue(m.isOpen());
    ui.destroy();
    assertFalse(m.isExists());
  }

  @Test
  void modalStyle_backdropNullNoExtraDrawCall() {
    FlixelUiSkin skin = new FlixelUiSkin();
    FlixelUiModalStyle ms = new FlixelUiModalStyle();
    // No backdrop, no panel background.
    skin.add("default", ms);

    FlixelUiDisplay ui2 = new FlixelUiDisplay(camera, skin);
    FlixelUiModal m = new FlixelUiModal(100, 80);
    ui2.openModal(m);
    ui2.layout();

    RecordingBatch batch = new RecordingBatch();
    ui2.getModalLayer().drawTree(batch, 1f);
    // No backdrop draw, no panel background draw.
    assertEquals(0, batch.getDrawCount());
  }

  @Test
  void tooltipTargetOutsideModalNotHiddenOnClose() {
    FlixelUiModal m = modal(200, 150);
    TestWidget outsideWidget = new TestWidget(50, 50);
    outsideWidget.setTooltip("outside");
    ui.add(outsideWidget);
    ui.openModal(m);
    ui.showTooltip(outsideWidget);
    // The tooltip target is outside the modal; closing the modal should not hide it.
    // (Current behavior: tooltip target check is only for descendants of the closed modal.)
    ui.closeModal(m);
    // Tooltip should still be visible - target is outside.
    assertNotNull(ui.getTooltipTarget());
  }
}
