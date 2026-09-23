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

import org.flixelgdx.FlixelCamera;
import org.flixelgdx.util.FlixelAlign;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies container layout: anchors, percent sizes, padding, local offsets, and nesting.
 */
@ExtendWith(FlixelUiHeadlessExtension.class)
class FlixelUiContainerLayoutTest {

  private static final float EPS = 0.0001f;

  private FlixelUiDisplay ui;
  private FlixelUiContainer box;

  @BeforeEach
  void setUp() {
    ui = new FlixelUiDisplay(new FlixelCamera(640, 360));
    // A 200 x 100 box at (50, 40) with padding 10, 5, 20, 15 has a 170 x 80 content area whose
    // top-left corner is at screen (60, 45).
    box = new FlixelUiContainer(200, 100);
    box.setPosition(50, 40);
    box.setPadding(10, 5, 20, 15);
    ui.add(box);
  }

  @Test
  void rootMatchesCameraVisibleArea() {
    ui.layout();
    assertEquals(640f, ui.getRoot().getWidth(), EPS);
    assertEquals(360f, ui.getRoot().getHeight(), EPS);
    assertEquals(0f, ui.getRoot().getScreenX(), EPS);
    assertEquals(0f, ui.getRoot().getScreenY(), EPS);
  }

  @Test
  void contentSizeSubtractsPadding() {
    assertEquals(170f, box.getContentWidth(), EPS);
    assertEquals(80f, box.getContentHeight(), EPS);
  }

  @Test
  void anchorsCoverAllNinePositionsWithPadding() {
    int[] aligns = {
        FlixelAlign.TOP_LEFT, FlixelAlign.TOP, FlixelAlign.TOP_RIGHT,
        FlixelAlign.LEFT, FlixelAlign.CENTER, FlixelAlign.RIGHT,
        FlixelAlign.BOTTOM_LEFT, FlixelAlign.BOTTOM, FlixelAlign.BOTTOM_RIGHT
    };
    // Child is 30 x 20 in a 170 x 80 content area at screen (60, 45).
    float[] expectedX = { 60, 130, 200, 60, 130, 200, 60, 130, 200 };
    float[] expectedY = { 45, 45, 45, 75, 75, 75, 105, 105, 105 };
    for (int i = 0; i < aligns.length; i++) {
      TestWidget child = new TestWidget(30, 20);
      child.anchor(aligns[i], 0, 0);
      box.add(child);
      ui.layout();
      assertEquals(expectedX[i], child.getScreenX(), EPS, "screen X for align " + aligns[i]);
      assertEquals(expectedY[i], child.getScreenY(), EPS, "screen Y for align " + aligns[i]);
      box.remove(child);
    }
  }

  @Test
  void anchorOffsetsAreAddedAfterAligning() {
    TestWidget child = new TestWidget(30, 20);
    child.anchor(FlixelAlign.BOTTOM_RIGHT, -4, -6);
    box.add(child);
    ui.layout();
    assertEquals(196f, child.getScreenX(), EPS);
    assertEquals(99f, child.getScreenY(), EPS);
    // The anchored local position is relative to the content origin.
    assertEquals(136f, child.getX(), EPS);
    assertEquals(54f, child.getY(), EPS);
  }

  @Test
  void unanchoredChildrenKeepTheirLocalOffsets() {
    TestWidget child = new TestWidget(30, 20);
    child.setPosition(7, 9);
    box.add(child);
    ui.layout();
    assertEquals(7f, child.getX(), EPS);
    assertEquals(9f, child.getY(), EPS);
    assertEquals(67f, child.getScreenX(), EPS);
    assertEquals(54f, child.getScreenY(), EPS);
  }

  @Test
  void clearingAnAnchorKeepsTheLastPosition() {
    TestWidget child = new TestWidget(30, 20);
    child.anchor(FlixelAlign.CENTER, 0, 0);
    box.add(child);
    ui.layout();
    child.clearAnchor();
    box.setSize(400, 400);
    ui.layout();
    assertEquals(70f, child.getX(), EPS);
    assertEquals(30f, child.getY(), EPS);
  }

  @Test
  void percentSizeUsesParentContentSize() {
    TestWidget child = new TestWidget(30, 20);
    child.setPercentSize(0.5f, 0.25f);
    box.add(child);
    ui.layout();
    assertEquals(85f, child.getWidth(), EPS);
    assertEquals(20f, child.getHeight(), EPS);
  }

  @Test
  void percentSizeNanKeepsThatAxisFixed() {
    TestWidget child = new TestWidget(30, 20);
    child.setPercentSize(1f, Float.NaN);
    box.add(child);
    ui.layout();
    assertEquals(170f, child.getWidth(), EPS);
    assertEquals(20f, child.getHeight(), EPS);
  }

  @Test
  void percentSizeIsAppliedBeforeAnchoring() {
    TestWidget child = new TestWidget(30, 20);
    child.setPercentSize(0.5f, 0.5f);
    child.anchor(FlixelAlign.BOTTOM_RIGHT, 0, 0);
    box.add(child);
    ui.layout();
    // 85 x 40 in the bottom-right corner of the content area.
    assertEquals(60f + 170f - 85f, child.getScreenX(), EPS);
    assertEquals(45f + 80f - 40f, child.getScreenY(), EPS);
  }

  @Test
  void nestedContainersAccumulateScreenPositions() {
    FlixelUiContainer inner = new FlixelUiContainer(100, 60);
    inner.setPosition(5, 5);
    inner.setPadding(3);
    TestWidget leaf = new TestWidget(10, 10);
    leaf.setPosition(2, 4);
    inner.add(leaf);
    box.add(inner);
    ui.layout();
    // box content origin (60, 45) + inner (5, 5) = (65, 50); + padding 3 + leaf (2, 4).
    assertEquals(65f, inner.getScreenX(), EPS);
    assertEquals(50f, inner.getScreenY(), EPS);
    assertEquals(70f, leaf.getScreenX(), EPS);
    assertEquals(57f, leaf.getScreenY(), EPS);
  }

  @Test
  void nestedPercentSizesFollowTheParent() {
    FlixelUiContainer inner = new FlixelUiContainer();
    inner.setPercentSize(1f, 1f);
    TestWidget leaf = new TestWidget(0, 0);
    leaf.setPercentSize(0.5f, 0.5f);
    leaf.anchor(FlixelAlign.CENTER, 0, 0);
    inner.add(leaf);
    box.add(inner);
    ui.layout();
    assertEquals(170f, inner.getWidth(), EPS);
    assertEquals(80f, inner.getHeight(), EPS);
    assertEquals(85f, leaf.getWidth(), EPS);
    assertEquals(40f, leaf.getHeight(), EPS);
    assertEquals(60f + 42.5f, leaf.getScreenX(), EPS);
    assertEquals(45f + 20f, leaf.getScreenY(), EPS);
  }

  @Test
  void rootAnchorsFollowCameraResize() {
    FlixelCamera camera = new FlixelCamera(640, 360);
    FlixelUiDisplay display = new FlixelUiDisplay(camera);
    TestWidget corner = new TestWidget(20, 20);
    corner.anchor(FlixelAlign.BOTTOM_RIGHT, 0, 0);
    display.add(corner);
    display.update(0f);
    assertEquals(620f, corner.getScreenX(), EPS);
    assertEquals(340f, corner.getScreenY(), EPS);

    // Zooming in shrinks the visible area; the next update notices and lays out again.
    camera.setZoom(2f);
    display.update(0f);
    assertEquals(320f, display.getRoot().getWidth(), EPS);
    assertEquals(300f, corner.getScreenX(), EPS);
    assertEquals(160f, corner.getScreenY(), EPS);
  }

  @Test
  void changesAreLaidOutLazilyBeforeHitTests() {
    TestWidget child = new TestWidget(10, 10);
    box.add(child);
    ui.layout();
    child.setPosition(20, 20);
    // No explicit layout() call: getWidgetAt() lays out first.
    assertSame(child, ui.getWidgetAt(81, 66));
  }

  @Test
  void addingReparentsAndAttaches() {
    FlixelUiContainer other = new FlixelUiContainer(50, 50);
    TestWidget child = new TestWidget(10, 10);
    other.add(child);
    assertEquals(null, child.getDisplay());
    box.add(child);
    assertSame(box, child.getParent());
    assertSame(ui, child.getDisplay());
    assertEquals(0, other.getChildCount());
    assertEquals(1, box.getChildCount());
    assertTrue(box.remove(child));
    assertEquals(null, child.getDisplay());
    assertFalse(box.remove(child));
  }

  @Test
  void attachingIsRecursive() {
    FlixelUiContainer inner = new FlixelUiContainer();
    TestWidget leaf = new TestWidget(1, 1);
    inner.add(leaf);
    box.add(inner);
    assertSame(ui, leaf.getDisplay());
    box.remove(inner);
    assertEquals(null, leaf.getDisplay());
  }

  @Test
  void cyclesAndLayersAreRejected() {
    FlixelUiContainer inner = new FlixelUiContainer();
    box.add(inner);
    assertThrows(IllegalArgumentException.class, () -> inner.add(box));
    assertThrows(IllegalArgumentException.class, () -> box.add(box));
    assertThrows(IllegalArgumentException.class, () -> box.add(ui.getRoot()));
  }

  @Test
  void clearDetachesEveryChild() {
    TestWidget a = new TestWidget(1, 1);
    TestWidget b = new TestWidget(1, 1);
    box.add(a);
    box.add(b);
    box.clear();
    assertEquals(0, box.getChildCount());
    assertEquals(null, a.getParent());
    assertEquals(null, b.getDisplay());
  }

  @Test
  void destroyDestroysChildrenAndLeavesTheParent() {
    TestWidget child = new TestWidget(1, 1);
    box.add(child);
    box.destroy();
    assertFalse(child.isExists());
    assertFalse(box.isExists());
    assertEquals(0, ui.getRoot().getChildCount());
    assertEquals(null, box.getDisplay());
  }
}
