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
import org.flixelgdx.ui.FlixelUiStack.Direction;
import org.flixelgdx.util.FlixelAlign;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link FlixelUiStack}: both directions, spacing, cross-axis alignment, auto-size, and
 * nesting.
 */
@ExtendWith(FlixelUiHeadlessExtension.class)
class FlixelUiStackTest {

  private static final float EPS = 0.0001f;

  private FlixelUiDisplay ui;

  @BeforeEach
  void setUp() {
    ui = new FlixelUiDisplay(new FlixelCamera(640, 360));
  }

  @Test
  void verticalStackPlacesChildrenTopToBottomWithSpacing() {
    FlixelUiStack stack = new FlixelUiStack(Direction.VERTICAL);
    stack.setPosition(10, 20);
    stack.setSpacing(5);
    TestWidget a = new TestWidget(40, 10);
    TestWidget b = new TestWidget(60, 20);
    TestWidget c = new TestWidget(20, 30);
    stack.add(a);
    stack.add(b);
    stack.add(c);
    ui.add(stack);
    ui.layout();
    assertEquals(20f, a.getScreenY(), EPS);
    assertEquals(35f, b.getScreenY(), EPS);
    assertEquals(60f, c.getScreenY(), EPS);
    assertEquals(10f, a.getScreenX(), EPS);
    assertEquals(10f, c.getScreenX(), EPS);
  }

  @Test
  void horizontalStackPlacesChildrenLeftToRightWithSpacing() {
    FlixelUiStack stack = new FlixelUiStack(Direction.HORIZONTAL);
    stack.setSpacing(4);
    TestWidget a = new TestWidget(40, 10);
    TestWidget b = new TestWidget(60, 20);
    stack.add(a);
    stack.add(b);
    ui.add(stack);
    ui.layout();
    assertEquals(0f, a.getScreenX(), EPS);
    assertEquals(44f, b.getScreenX(), EPS);
    assertEquals(0f, a.getScreenY(), EPS);
  }

  @Test
  void autoSizeIncludesChildrenSpacingAndPadding() {
    FlixelUiStack stack = new FlixelUiStack(Direction.VERTICAL);
    stack.setSpacing(5);
    stack.setPadding(2, 3, 4, 6);
    stack.add(new TestWidget(40, 10));
    stack.add(new TestWidget(60, 20));
    ui.add(stack);
    ui.layout();
    assertEquals(2f + 60f + 4f, stack.getWidth(), EPS);
    assertEquals(3f + 10f + 5f + 20f + 6f, stack.getHeight(), EPS);
    assertEquals(stack.getWidth(), stack.getPreferredWidth(), EPS);
    assertEquals(stack.getHeight(), stack.getPreferredHeight(), EPS);
  }

  @Test
  void paddingOffsetsTheFirstChild() {
    FlixelUiStack stack = new FlixelUiStack(Direction.HORIZONTAL);
    stack.setPadding(7);
    TestWidget a = new TestWidget(10, 10);
    stack.add(a);
    ui.add(stack);
    ui.layout();
    assertEquals(7f, a.getScreenX(), EPS);
    assertEquals(7f, a.getScreenY(), EPS);
  }

  @Test
  void verticalCrossAxisAlignment() {
    FlixelUiStack stack = new FlixelUiStack(Direction.VERTICAL);
    TestWidget wide = new TestWidget(100, 10);
    TestWidget narrow = new TestWidget(40, 10);
    stack.add(wide);
    stack.add(narrow);
    ui.add(stack);

    ui.layout();
    assertEquals(0f, narrow.getScreenX(), EPS, "default is left");

    stack.setAlign(FlixelAlign.CENTER);
    ui.layout();
    assertEquals(30f, narrow.getScreenX(), EPS);

    stack.setAlign(FlixelAlign.RIGHT);
    ui.layout();
    assertEquals(60f, narrow.getScreenX(), EPS);
  }

  @Test
  void horizontalCrossAxisAlignment() {
    FlixelUiStack stack = new FlixelUiStack(Direction.HORIZONTAL);
    TestWidget tall = new TestWidget(10, 100);
    TestWidget shortOne = new TestWidget(10, 40);
    stack.add(tall);
    stack.add(shortOne);
    ui.add(stack);

    ui.layout();
    assertEquals(0f, shortOne.getScreenY(), EPS, "default is top");

    stack.setAlign(FlixelAlign.CENTER);
    ui.layout();
    assertEquals(30f, shortOne.getScreenY(), EPS);

    stack.setAlign(FlixelAlign.BOTTOM);
    ui.layout();
    assertEquals(60f, shortOne.getScreenY(), EPS);
  }

  @Test
  void crossAxisPercentSizeStillApplies() {
    FlixelUiStack stack = new FlixelUiStack(Direction.VERTICAL);
    TestWidget fixed = new TestWidget(120, 10);
    TestWidget stretched = new TestWidget(5, 10);
    stretched.setPercentSize(1f, Float.NaN);
    stack.add(fixed);
    stack.add(stretched);
    ui.add(stack);
    ui.layout();
    assertEquals(120f, stack.getWidth(), EPS, "percent-sized children do not grow an auto-sized stack");
    assertEquals(120f, stretched.getWidth(), EPS);
  }

  @Test
  void anchorsAreIgnoredInsideStacks() {
    FlixelUiStack stack = new FlixelUiStack(Direction.VERTICAL);
    stack.setSize(200, 200);
    TestWidget a = new TestWidget(10, 10);
    a.anchor(FlixelAlign.BOTTOM_RIGHT, 0, 0);
    stack.add(a);
    ui.add(stack);
    ui.layout();
    assertEquals(0f, a.getScreenX(), EPS);
    assertEquals(0f, a.getScreenY(), EPS);
  }

  @Test
  void killedChildrenTakeNoSpace() {
    FlixelUiStack stack = new FlixelUiStack(Direction.VERTICAL);
    TestWidget a = new TestWidget(10, 10);
    TestWidget b = new TestWidget(10, 10);
    TestWidget c = new TestWidget(10, 10);
    stack.add(a);
    stack.add(b);
    stack.add(c);
    ui.add(stack);
    b.kill();
    ui.layout();
    assertEquals(10f, c.getScreenY(), EPS);
    assertEquals(20f, stack.getHeight(), EPS);
    b.revive();
    ui.layout();
    assertEquals(20f, c.getScreenY(), EPS);
  }

  @Test
  void settingSizeTurnsAutoSizeOffPerAxis() {
    FlixelUiStack stack = new FlixelUiStack(Direction.VERTICAL);
    stack.add(new TestWidget(10, 10));
    ui.add(stack);

    stack.setWidth(300);
    assertFalse(stack.isAutoWidth());
    assertTrue(stack.isAutoHeight());
    ui.layout();
    assertEquals(300f, stack.getWidth(), EPS);
    assertEquals(10f, stack.getHeight(), EPS);

    stack.setAutoSize(true);
    stack.setPercentSize(Float.NaN, 0.5f);
    assertTrue(stack.isAutoWidth());
    assertFalse(stack.isAutoHeight());
    ui.layout();
    assertEquals(10f, stack.getWidth(), EPS);
    assertEquals(180f, stack.getHeight(), EPS);

    stack.setSize(50, 60);
    assertFalse(stack.isAutoWidth());
    assertFalse(stack.isAutoHeight());
  }

  @Test
  void nestedStacksSizeFromTheInsideOut() {
    FlixelUiStack row = new FlixelUiStack(Direction.HORIZONTAL);
    row.setSpacing(10);
    row.add(new TestWidget(30, 20));
    row.add(new TestWidget(30, 40));

    FlixelUiStack column = new FlixelUiStack(Direction.VERTICAL);
    column.setSpacing(5);
    column.setPadding(4);
    TestWidget header = new TestWidget(50, 12);
    column.add(header);
    column.add(row);
    TestWidget footer = new TestWidget(20, 8);
    column.add(footer);
    column.anchor(FlixelAlign.CENTER, 0, 0);
    ui.add(column);
    ui.layout();

    assertEquals(70f, row.getWidth(), EPS);
    assertEquals(40f, row.getHeight(), EPS);
    assertEquals(4f + 70f + 4f, column.getWidth(), EPS);
    assertEquals(4f + 12f + 5f + 40f + 5f + 8f + 4f, column.getHeight(), EPS);

    float left = (640f - column.getWidth()) / 2f;
    float top = (360f - column.getHeight()) / 2f;
    assertEquals(left, column.getScreenX(), EPS);
    assertEquals(top, column.getScreenY(), EPS);
    assertEquals(top + 4f + 12f + 5f, row.getScreenY(), EPS);
    assertEquals(top + 4f + 12f + 5f + 40f + 5f, footer.getScreenY(), EPS);
    // The second child of the row sits after the first child and the spacing.
    assertEquals(left + 4f + 40f, row.getChildAt(1).getScreenX(), EPS);
  }
}
