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
import org.flixelgdx.FlixelConfig;
import org.flixelgdx.graphics.FlixelUnsupportedBatch;
import org.flixelgdx.math.FlixelMatrix;
import org.flixelgdx.ui.skin.FlixelUiModalStyle;
import org.flixelgdx.util.FlixelColor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link FlixelUiDisplay} drawing, coordinate helpers, lifecycle, and the HUD camera.
 *
 * <p>No graphics backend is installed, so drawing goes through the no-op
 * {@link FlixelUnsupportedBatch}, which still remembers its transform. Outside a real camera pass
 * {@link Flixel#getDrawCamera()} is {@code null}, so these tests rely on the display falling back to
 * the first camera in {@link Flixel#cameras}, the same fallback sprites use.
 */
@ExtendWith(FlixelUiHeadlessExtension.class)
class FlixelUiDisplayTest {

  private static final float EPS = 0.0001f;

  private final FlixelUnsupportedBatch batch = FlixelUnsupportedBatch.INSTANCE;

  private FlixelCamera camera;
  private FlixelUiDisplay ui;

  @BeforeEach
  void setUp() {
    Flixel.cameras.clear();
    camera = new FlixelCamera(640, 360);
    Flixel.cameras.add(camera);
    ui = new FlixelUiDisplay(camera);
    batch.getTransform().idt();
  }

  @AfterEach
  void tearDown() {
    Flixel.cameras.clear();
  }

  @Test
  void drawsAtTheLaidOutPositionWithCombinedAlpha() {
    FlixelUiContainer group = new FlixelUiContainer(100, 100);
    group.setPosition(20, 30);
    group.setAlpha(0.5f);
    TestWidget w = new TestWidget(10, 10);
    w.setPosition(5, 6);
    w.setAlpha(0.5f);
    group.add(w);
    ui.add(group);
    ui.draw(batch);
    assertEquals(1, w.drawCount);
    assertEquals(25f, w.lastDrawX, EPS);
    assertEquals(36f, w.lastDrawY, EPS);
    assertEquals(0.25f, w.lastAlpha, EPS);
    assertEquals(0.25f, w.getWorldAlpha(), EPS);
  }

  @Test
  void drawsOnlyOnItsOwnCamera() {
    TestWidget w = new TestWidget(10, 10);
    ui.add(w);
    Flixel.cameras.clear();
    Flixel.cameras.add(new FlixelCamera(640, 360));
    Flixel.cameras.add(camera);
    ui.draw(batch);
    assertEquals(0, w.drawCount, "another camera is first, so this is not the display's pass");
  }

  @Test
  void skipsInvisibleKilledAndTransparentWidgets() {
    TestWidget hidden = new TestWidget(10, 10);
    TestWidget killed = new TestWidget(10, 10);
    TestWidget clear = new TestWidget(10, 10);
    hidden.setVisible(false);
    killed.kill();
    clear.setAlpha(0f);
    ui.add(hidden);
    ui.add(killed);
    ui.add(clear);
    ui.draw(batch);
    assertEquals(0, hidden.drawCount);
    assertEquals(0, killed.drawCount);
    assertEquals(0, clear.drawCount);
  }

  @Test
  void rotationUsesABatchTransformAndRestoresIt() {
    FlixelUiContainer spinner = new FlixelUiContainer(100, 50);
    spinner.setAngle(90f);
    TestWidget inside = new TestWidget(10, 10);
    spinner.add(inside);
    ui.add(spinner);
    float[] identity = new FlixelMatrix().val.clone();
    ui.draw(batch);
    assertFalse(Arrays.equals(identity, inside.drawTransform.val),
        "children draw inside the rotated transform");
    assertArrayEquals(identity, batch.getTransform().val, EPS, "the previous transform is restored");
  }

  @Test
  void unrotatedWidgetsLeaveTheTransformAlone() {
    TestWidget w = new TestWidget(10, 10);
    ui.add(w);
    ui.draw(batch);
    assertArrayEquals(new FlixelMatrix().val, w.drawTransform.val, EPS);
  }

  @Test
  void spriteHelpersUndoTheCameraZoomMargin() {
    camera.setZoom(2f);
    float sx = 37f;
    float sy = 12f;
    assertEquals(sx, ui.toDrawX(sx), EPS);
    assertEquals(sy, ui.toDrawY(sy), EPS);
    assertNotEquals(0f, camera.getViewMarginX());
    // A scroll-factor-0 sprite placed with the helpers draws exactly where the widget does.
    assertEquals(ui.toDrawX(sx), camera.worldToViewX(ui.toSpriteX(sx), 0f), EPS);
    assertEquals(ui.toDrawY(sy), camera.worldToViewY(ui.toSpriteY(sy), 0f), EPS);
  }

  @Test
  void updateOnlyReachesActiveExistingWidgets() {
    int[] updates = new int[2];
    FlixelUiWidget counting = new TestWidget(1, 1) {
      @Override
      public void update(float elapsed) {
        updates[0]++;
      }
    };
    FlixelUiWidget inactive = new TestWidget(1, 1) {
      @Override
      public void update(float elapsed) {
        updates[1]++;
      }
    };
    inactive.setActive(false);
    ui.add(counting);
    ui.add(inactive);
    ui.update(0.016f);
    assertEquals(1, updates[0]);
    assertEquals(0, updates[1]);
  }

  @Test
  void destroyTearsDownEveryLayer() {
    TestWidget w = new TestWidget(10, 10);
    TestWidget popup = new TestWidget(10, 10);
    FlixelUiModal modal = new FlixelUiModal(10, 10);
    modal.setModalStyle(new FlixelUiModalStyle());
    ui.add(w);
    ui.getPopupLayer().add(popup);
    ui.openModal(modal);
    w.focus();
    ui.destroy();
    assertFalse(ui.isExists());
    assertFalse(w.isExists());
    assertFalse(popup.isExists());
    assertFalse(modal.isExists());
    assertEquals(null, ui.getFocused());
    assertEquals(null, ui.getWidgetAt(5, 5));
  }

  @Test
  void createHudCameraIsTransparentAndRegistered() {
    FlixelConfig previous = Flixel.config;
    Flixel.config = new FlixelConfig.Builder("UI test").size(320, 240).build();
    try {
      FlixelCamera hud = FlixelUiDisplay.createHudCamera();
      assertSame(hud, Flixel.cameras.peek());
      assertEquals(320, hud.width);
      assertEquals(240, hud.height);
      assertTrue(hud.useBgAlphaBlending);
      assertEquals(FlixelColor.CLEAR.getRgba8888(), hud.bgColor.getRgba8888());
      assertFalse(hud.defaultDrawTarget, "world objects without a camera list stay off the UI camera");
    } finally {
      Flixel.config = previous;
    }
  }
}
