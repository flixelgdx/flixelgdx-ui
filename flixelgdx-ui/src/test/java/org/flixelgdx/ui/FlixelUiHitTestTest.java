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
import org.flixelgdx.ui.skin.FlixelUiModalStyle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link FlixelUiDisplay#getWidgetAt(float, float)} and
 * {@link FlixelUiWidget#containsPoint(float, float)}.
 */
@ExtendWith(FlixelUiHeadlessExtension.class)
class FlixelUiHitTestTest {

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

  private static FlixelUiModal modal(float w, float h) {
    FlixelUiModal m = new FlixelUiModal(w, h);
    m.setModalStyle(new FlixelUiModalStyle());
    return m;
  }

  @Test
  void emptySpaceReturnsNull() {
    assertNull(ui.getWidgetAt(100, 100));
  }

  @Test
  void containsPointUsesHalfOpenEdges() {
    TestWidget w = at(10, 20, 30, 40);
    ui.add(w);
    ui.layout();
    assertTrue(w.containsPoint(10, 20));
    assertTrue(w.containsPoint(39.9f, 59.9f));
    assertFalse(w.containsPoint(40, 30));
    assertFalse(w.containsPoint(20, 60));
    assertFalse(w.containsPoint(9.9f, 30));
  }

  @Test
  void rotationDoesNotAffectHitTesting() {
    TestWidget w = at(10, 10, 100, 10);
    w.setAngle(90);
    ui.add(w);
    assertSame(w, ui.getWidgetAt(100, 15));
    assertNull(ui.getWidgetAt(60, 50));
  }

  @Test
  void laterSiblingsWinBecauseTheyAreDrawnOnTop() {
    TestWidget bottom = at(0, 0, 100, 100);
    TestWidget top = at(50, 50, 100, 100);
    ui.add(bottom);
    ui.add(top);
    assertSame(top, ui.getWidgetAt(75, 75));
    assertSame(bottom, ui.getWidgetAt(25, 25));
    assertSame(top, ui.getWidgetAt(125, 125));
  }

  @Test
  void deepestInteractiveWidgetWins() {
    FlixelUiContainer panel = new FlixelUiContainer(200, 200);
    panel.interactive = true;
    TestWidget child = at(10, 10, 20, 20);
    panel.add(child);
    ui.add(panel);
    assertSame(child, ui.getWidgetAt(15, 15));
    assertSame(panel, ui.getWidgetAt(100, 100));
  }

  @Test
  void nonInteractiveContainersAreSearchedButNotReturned() {
    FlixelUiContainer group = new FlixelUiContainer(200, 200);
    TestWidget child = at(10, 10, 20, 20);
    group.add(child);
    ui.add(group);
    assertSame(child, ui.getWidgetAt(15, 15));
    assertNull(ui.getWidgetAt(100, 100));
  }

  @Test
  void skippedWidgetsFallThroughToWhatIsBelow() {
    TestWidget below = at(0, 0, 50, 50);
    TestWidget above = at(0, 0, 50, 50);
    ui.add(below);
    ui.add(above);

    above.setVisible(false);
    assertSame(below, ui.getWidgetAt(10, 10), "invisible");
    above.setVisible(true);

    above.setEnabled(false);
    assertSame(below, ui.getWidgetAt(10, 10), "disabled");
    above.setEnabled(true);

    above.interactive = false;
    assertSame(below, ui.getWidgetAt(10, 10), "not interactive");
    above.interactive = true;

    above.kill();
    assertSame(below, ui.getWidgetAt(10, 10), "killed");
    above.revive();

    assertSame(above, ui.getWidgetAt(10, 10));
  }

  @Test
  void invisibleContainersHideTheirChildren() {
    FlixelUiContainer group = new FlixelUiContainer(200, 200);
    TestWidget child = at(10, 10, 20, 20);
    group.add(child);
    ui.add(group);
    group.setVisible(false);
    assertNull(ui.getWidgetAt(15, 15));
  }

  @Test
  void disabledContainersStillExposeTheirChildren() {
    FlixelUiContainer panel = new FlixelUiContainer(200, 200);
    panel.interactive = true;
    TestWidget child = at(10, 10, 20, 20);
    panel.add(child);
    ui.add(panel);
    panel.setEnabled(false);
    assertSame(child, ui.getWidgetAt(15, 15));
    assertNull(ui.getWidgetAt(100, 100));
  }

  @Test
  void popupLayerIsAboveRoot() {
    TestWidget rootWidget = at(0, 0, 100, 100);
    TestWidget popup = at(50, 50, 100, 100);
    ui.add(rootWidget);
    ui.getPopupLayer().add(popup);
    assertSame(popup, ui.getWidgetAt(75, 75));
    assertSame(rootWidget, ui.getWidgetAt(25, 25));
  }

  @Test
  void openModalRestrictsTheSearchToTheTopModal() {
    TestWidget behind = at(0, 0, 640, 360);
    ui.add(behind);

    FlixelUiModal lower = modal(300, 300);
    lower.clearAnchor();
    FlixelUiModal top = modal(200, 100);
    top.clearAnchor();
    top.setPosition(100, 100);
    TestWidget ok = at(10, 10, 50, 20);
    ok.interactive = true;
    top.add(ok);
    ui.openModal(lower);
    ui.openModal(top);

    assertSame(top, ui.getTopModal());
    assertSame(ok, ui.getWidgetAt(115, 115), "a child of the top modal");
    assertSame(top, ui.getWidgetAt(250, 150), "empty space inside the modal blocks the click");
    assertNull(ui.getWidgetAt(20, 20), "nothing behind the modal can be hit");

    ui.closeModal(top);
    assertSame(lower, ui.getTopModal());
    assertSame(lower, ui.getWidgetAt(20, 20));
    ui.closeModal(lower);
    assertNull(ui.getTopModal());
    assertSame(behind, ui.getWidgetAt(20, 20));
  }

  @Test
  void popupsStayAboveAnOpenModal() {
    FlixelUiModal m = modal(640, 360);
    m.clearAnchor();
    ui.openModal(m);
    TestWidget popup = at(10, 10, 20, 20);
    ui.getPopupLayer().add(popup);
    assertSame(popup, ui.getWidgetAt(15, 15));
    assertSame(m, ui.getWidgetAt(100, 100));
  }

  @Test
  void reopeningAModalMovesItToTheTop() {
    FlixelUiModal a = modal(10, 10);
    FlixelUiModal b = modal(10, 10);
    ui.openModal(a);
    ui.openModal(b);
    ui.openModal(a);
    assertSame(a, ui.getTopModal());
  }

  private static TestWidget at(float x, float y, float width, float height) {
    TestWidget w = new TestWidget(width, height);
    w.setPosition(x, y);
    return w;
  }
}
