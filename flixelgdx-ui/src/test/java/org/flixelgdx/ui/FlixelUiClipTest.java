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
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelGraphicsManager;
import org.flixelgdx.ui.graphics.RecordingBatch;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the display's clip stack, container clipping in drawing and hit-testing, and the
 * widget clip helpers.
 *
 * <p>A {@link RecordingGraphics} is installed on {@link Flixel#graphics} for each test so scissor
 * calls can be checked, and the camera's viewport is given explicit screen bounds so the projected
 * scissor rectangles are predictable: with a 640 x 360 camera on a 640 x 360 screen, a view-space
 * rectangle maps to the same pixels, flipped so that Y counts up from the bottom.
 */
@ExtendWith(FlixelUiHeadlessExtension.class)
class FlixelUiClipTest {

  private static final float EPS = 1e-4f;

  private final RecordingBatch batch = new RecordingBatch();
  private final RecordingGraphics graphics = new RecordingGraphics(batch);

  private FlixelGraphicsManager originalGraphics;
  private FlixelCamera camera;
  private FlixelUiDisplay ui;

  @BeforeEach
  void setUp() {
    originalGraphics = Flixel.graphics;
    Flixel.graphics = graphics;
    Flixel.cameras.clear();
    camera = new FlixelCamera(640, 360);
    camera.getViewport().setCameraPosition(320f, 180f);
    camera.getViewport().setScreenBounds(0, 0, 640, 360);
    Flixel.cameras.add(camera);
    ui = new FlixelUiDisplay(camera);
  }

  @AfterEach
  void tearDown() {
    Flixel.graphics = originalGraphics;
    Flixel.cameras.clear();
  }

  @Test
  void nestedClipsIntersect() {
    assertTrue(ui.pushClip(batch, 10, 20, 100, 50));
    assertBounds(10, 20, 110, 70);
    assertTrue(ui.pushClip(batch, 50, 0, 200, 40));
    assertBounds(50, 20, 110, 40);
    assertTrue(ui.isClipped());
    assertEquals(2, ui.getClipDepth());
    ui.popClip(batch);
    assertBounds(10, 20, 110, 70);
    ui.popClip(batch);
    assertFalse(ui.isClipped());
    assertEquals(Float.NEGATIVE_INFINITY, ui.getClipLeft());
    assertEquals(Float.POSITIVE_INFINITY, ui.getClipBottom());
  }

  @Test
  void disjointClipsProduceAnEmptyClip() {
    ui.pushClip(batch, 0, 0, 10, 10);
    assertFalse(ui.pushClip(batch, 20, 30, 5, 5), "nothing can be visible");
    assertEquals(0f, ui.getClipRight() - ui.getClipLeft(), EPS);
    assertEquals(0f, ui.getClipBottom() - ui.getClipTop(), EPS);
    assertTrue(ui.isClipped(), "an empty clip still counts as a clip");
    // Anything nested inside an empty clip stays empty.
    assertFalse(ui.pushClip(batch, 0, 0, 640, 360));
    ui.popClip(batch);
    ui.popClip(batch);
    ui.popClip(batch);
    assertFalse(ui.isClipped());
  }

  @Test
  void identicalClipsKeepTheSameBounds() {
    assertTrue(ui.pushClip(batch, 5, 6, 7, 8));
    assertTrue(ui.pushClip(batch, 5, 6, 7, 8));
    assertBounds(5, 6, 12, 14);
    ui.popClip(batch);
    ui.popClip(batch);
  }

  @Test
  void negativeSizesCountAsEmpty() {
    assertFalse(ui.pushClip(batch, 10, 10, -5, 20));
    assertEquals(10f, ui.getClipRight(), EPS);
    ui.popClip(batch);
  }

  @Test
  void pushAndPopApplyProjectedScissorsAfterFlushing() {
    ui.pushClip(batch, 10, 20, 100, 50);
    ui.pushClip(batch, 50, 0, 200, 40);
    ui.popClip(batch);
    ui.popClip(batch);

    assertEquals(4, graphics.count);
    // Scissors are in framebuffer pixels measured from the bottom-left corner.
    assertScissor(0, 10, 360 - 70, 100, 50);
    assertScissor(1, 50, 360 - 40, 60, 20);
    assertScissor(2, 10, 360 - 70, 100, 50);
    assertEquals(RecordingGraphics.CLEAR, graphics.kinds[3]);
    for (int i = 0; i < graphics.count; i++) {
      assertEquals(i + 1, graphics.flushes[i], "the batch is flushed before every scissor change");
    }
  }

  @Test
  void scissorFollowsTheViewportScale() {
    camera.getViewport().setScreenBounds(0, 0, 1280, 720);
    ui.pushClip(batch, 10, 20, 100, 50);
    assertScissor(0, 20, 720 - 140, 200, 100);
    ui.popClip(batch);
  }

  @Test
  void popWithoutPushThrows() {
    assertThrows(IllegalStateException.class, () -> ui.popClip(batch));
    assertEquals(0, graphics.count);
  }

  @Test
  void pushingPastTheMaximumDepthThrows() {
    for (int i = 0; i < FlixelUiDisplay.MAX_CLIP_DEPTH; i++) {
      ui.pushClip(batch, 0, 0, 100, 100);
    }
    IllegalStateException e = assertThrows(IllegalStateException.class,
        () -> ui.pushClip(batch, 0, 0, 100, 100));
    assertTrue(e.getMessage().contains(String.valueOf(FlixelUiDisplay.MAX_CLIP_DEPTH)));
    assertEquals(FlixelUiDisplay.MAX_CLIP_DEPTH, ui.getClipDepth(), "a failed push changes nothing");
  }

  @Test
  void drawResetsALeftoverClipStack() {
    ui.pushClip(batch, 0, 0, 10, 10);
    ui.pushClip(batch, 0, 0, 10, 10);
    ClipProbe probe = new ClipProbe();
    ui.add(probe);
    ui.draw(batch);
    assertFalse(probe.clippedDuringDraw, "the stack is emptied before drawing starts");
    assertFalse(ui.isClipped());
    assertEquals(RecordingGraphics.CLEAR, graphics.lastKind());
  }

  @Test
  void drawLeavesNoScissorCallsWhenNothingClips() {
    ui.add(new TestWidget(10, 10));
    ui.draw(batch);
    assertEquals(0, graphics.count);
  }

  @Test
  void aWidgetThatThrowsCannotLeaveTheStackUnbalanced() {
    FlixelUiContainer box = clippingBox(0, 0, 100, 100, 0);
    box.add(new ThrowingWidget());
    ui.add(box);
    assertThrows(IllegalStateException.class, () -> ui.draw(batch));
    assertFalse(ui.isClipped());
    assertEquals(RecordingGraphics.CLEAR, graphics.lastKind());
  }

  @Test
  void aClipAWidgetForgetsToPopIsPoppedAfterItDraws() {
    LeakyWidget leaky = new LeakyWidget();
    ClipProbe after = new ClipProbe();
    ui.add(leaky);
    ui.add(after);
    ui.draw(batch);
    assertTrue(leaky.pushed);
    assertFalse(after.clippedDuringDraw, "the sibling drawn next is not clipped");
    assertFalse(ui.isClipped());
  }

  @Test
  void widgetHelpersDoNothingWhenNotAttached() {
    LeakyWidget loose = new LeakyWidget();
    assertTrue(loose.pushClip(batch, 0, 0, 0, 0), "nothing is clipped off a display");
    loose.popClip(batch);
    assertEquals(0, graphics.count);
    assertEquals(0, batch.flushCount);
  }

  @Test
  void clippingContainersClipTheirChildrenToTheContentArea() {
    FlixelUiContainer box = clippingBox(100, 50, 200, 120, 10);
    ClipProbe child = new ClipProbe();
    box.add(child);
    ui.add(box);
    ui.draw(batch);

    assertTrue(child.clippedDuringDraw);
    assertEquals(110f, child.left, EPS);
    assertEquals(60f, child.top, EPS);
    assertEquals(290f, child.right, EPS);
    assertEquals(160f, child.bottom, EPS);
    assertScissor(0, 110, 360 - 160, 180, 100);
    assertEquals(RecordingGraphics.CLEAR, graphics.lastKind());
    assertFalse(ui.isClipped());
  }

  @Test
  void containersDoNotClipByDefault() {
    FlixelUiContainer box = new FlixelUiContainer(100, 100);
    assertFalse(box.isClipChildren());
    ClipProbe child = new ClipProbe();
    box.add(child);
    ui.add(box);
    ui.draw(batch);
    assertFalse(child.clippedDuringDraw);
    assertEquals(0, graphics.count);
  }

  @Test
  void nestedClippingContainersIntersect() {
    FlixelUiContainer outer = clippingBox(0, 0, 100, 100, 0);
    FlixelUiContainer inner = clippingBox(50, 50, 100, 100, 0);
    ClipProbe child = new ClipProbe();
    inner.add(child);
    outer.add(inner);
    ui.add(outer);
    ui.draw(batch);
    assertEquals(50f, child.left, EPS);
    assertEquals(50f, child.top, EPS);
    assertEquals(100f, child.right, EPS);
    assertEquals(100f, child.bottom, EPS);
    assertEquals(2, child.depth);
  }

  @Test
  void childrenOfAnEmptyClipAreNotDrawn() {
    FlixelUiContainer box = clippingBox(0, 0, 20, 20, 10);
    TestWidget child = new TestWidget(10, 10);
    box.add(child);
    ui.add(box);
    ui.draw(batch);
    assertEquals(0, child.drawCount);
    assertFalse(ui.isClipped());
  }

  @Test
  void hitTestingIgnoresTheClippedPartOfAChild() {
    FlixelUiContainer box = clippingBox(100, 100, 100, 100, 10);
    TestWidget child = new TestWidget(100, 100);
    child.setPosition(50, 50); // Screen (160, 160) to (260, 260); content ends at 190.
    box.add(child);
    ui.add(box);
    assertSame(child, ui.getWidgetAt(170, 170));
    assertSame(child, ui.getWidgetAt(189, 189));
    assertNull(ui.getWidgetAt(195, 170), "inside the padding");
    assertNull(ui.getWidgetAt(230, 230), "outside the container");

    box.setClipChildren(false);
    assertSame(child, ui.getWidgetAt(230, 230), "overflow is hit when clipping is off");
  }

  @Test
  void hitTestingIntersectsNestedClippingContainers() {
    FlixelUiContainer outer = clippingBox(0, 0, 100, 100, 0);
    FlixelUiContainer inner = clippingBox(50, 50, 100, 100, 0);
    TestWidget child = new TestWidget(100, 100);
    inner.add(child);
    outer.add(inner);
    ui.add(outer);
    assertSame(child, ui.getWidgetAt(75, 75));
    assertNull(ui.getWidgetAt(120, 75), "inside the inner clip but outside the outer one");
    assertNull(ui.getWidgetAt(25, 25), "inside the outer clip but outside the inner one");
  }

  @Test
  void aClippingContainerCanStillBeHitItself() {
    FlixelUiContainer box = clippingBox(0, 0, 100, 100, 10);
    box.interactive = true;
    ui.add(box);
    assertSame(box, ui.getWidgetAt(5, 5), "the padding belongs to the container");
  }

  private FlixelUiContainer clippingBox(float x, float y, float width, float height, float padding) {
    FlixelUiContainer box = new FlixelUiContainer(width, height);
    box.setPosition(x, y);
    box.setPadding(padding);
    box.setClipChildren(true);
    return box;
  }

  private void assertBounds(float left, float top, float right, float bottom) {
    assertEquals(left, ui.getClipLeft(), EPS);
    assertEquals(top, ui.getClipTop(), EPS);
    assertEquals(right, ui.getClipRight(), EPS);
    assertEquals(bottom, ui.getClipBottom(), EPS);
  }

  private void assertScissor(int call, int x, int y, int width, int height) {
    assertEquals(RecordingGraphics.SET, graphics.kinds[call], "call " + call + " sets a scissor");
    assertEquals(x, graphics.xs[call], "x of call " + call);
    assertEquals(y, graphics.ys[call], "y of call " + call);
    assertEquals(width, graphics.widths[call], "width of call " + call);
    assertEquals(height, graphics.heights[call], "height of call " + call);
  }

  /** Records the display's clip while it draws. */
  private static final class ClipProbe extends FlixelUiWidget {

    float left;
    float top;
    float right;
    float bottom;
    int depth;

    boolean clippedDuringDraw;

    ClipProbe() {
      super(10, 10);
    }

    @Override
    protected void drawSelf(@NotNull FlixelBatch batch, float drawX, float drawY, float alpha) {
      FlixelUiDisplay display = getDisplay();
      clippedDuringDraw = display.isClipped();
      left = display.getClipLeft();
      top = display.getClipTop();
      right = display.getClipRight();
      bottom = display.getClipBottom();
      depth = display.getClipDepth();
    }
  }

  /** Pushes a clip of its own and never pops it. */
  private static final class LeakyWidget extends FlixelUiWidget {

    boolean pushed;

    LeakyWidget() {
      super(10, 10);
    }

    @Override
    protected void drawSelf(@NotNull FlixelBatch batch, float drawX, float drawY, float alpha) {
      pushed = pushClip(batch, 0, 0, 5, 5);
    }
  }

  /** Pushes a clip of its own, then throws while drawing. */
  private static final class ThrowingWidget extends FlixelUiWidget {

    ThrowingWidget() {
      super(10, 10);
    }

    @Override
    protected void drawSelf(@NotNull FlixelBatch batch, float drawX, float drawY, float alpha) {
      pushClip(batch, 0, 0, 5, 5);
      throw new IllegalStateException("Drawing failed on purpose.");
    }
  }
}
