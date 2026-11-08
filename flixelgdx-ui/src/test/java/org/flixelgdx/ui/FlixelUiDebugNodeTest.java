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
import org.flixelgdx.debug.FlixelDebugNode;
import org.flixelgdx.debug.FlixelDebugNodes;
import org.flixelgdx.input.FlixelBaseInputDevice;
import org.flixelgdx.input.FlixelInputDevice;
import org.flixelgdx.input.keyboard.FlixelKeyInputManager;
import org.flixelgdx.input.mouse.FlixelMouseButton;
import org.flixelgdx.input.mouse.FlixelMouseInputManager;
import org.flixelgdx.ui.skin.FlixelTextBoxStyle;
import org.flixelgdx.ui.skin.FlixelUiButtonStyle;
import org.flixelgdx.ui.skin.FlixelUiModalStyle;
import org.flixelgdx.ui.skin.FlixelUiPanelStyle;
import org.flixelgdx.ui.text.FlixelTextBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies that the display and its widgets present a stable tree and boxes to the debugger. */
@ExtendWith(FlixelUiHeadlessExtension.class)
class FlixelUiDebugNodeTest {

  private final float[] box = new float[4];

  private FlixelCamera camera;
  private FlixelUiDisplay ui;

  @BeforeEach
  void setUp() {
    Flixel.cameras.clear();
    camera = new FlixelCamera(640, 360);
    Flixel.cameras.add(camera);
    ui = new FlixelUiDisplay(camera);
  }

  @AfterEach
  void tearDown() {
    Flixel.cameras.clear();
  }

  @Test
  void displayListsItsLayersInDrawOrder() {
    assertEquals(4, FlixelDebugNodes.childCount(ui));
    assertSame(ui.getRoot(), FlixelDebugNodes.child(ui, 0));
    assertSame(ui.getModalLayer(), FlixelDebugNodes.child(ui, 1));
    assertSame(ui.getPopupLayer(), FlixelDebugNodes.child(ui, 2));
    assertNull(FlixelDebugNodes.child(ui, 4));
    assertNull(FlixelDebugNodes.child(ui, -1));
  }

  @Test
  void containersListChildrenByPosition() {
    FlixelUiStack stack = new FlixelUiStack(FlixelUiStack.Direction.VERTICAL);
    TestWidget a = new TestWidget(10, 10);
    TestWidget b = new TestWidget(10, 10);
    stack.add(a);
    stack.add(b);
    ui.getRoot().add(stack);

    assertTrue(FlixelDebugNodes.isContainer(stack));
    assertEquals(2, FlixelDebugNodes.childCount(stack));
    Object root = FlixelDebugNodes.child(ui, 0);
    Object viaPath = FlixelDebugNodes.child(FlixelDebugNodes.child(root, 0), 1);
    assertSame(b, viaPath);
    assertEquals(0, FlixelDebugNodes.childCount(a));
  }

  @Test
  void openModalsListInOpenOrderAndOverlaysAreAboveTheRoot() {
    FlixelUiModal first = modal();
    FlixelUiModal second = modal();
    ui.openModal(first);
    ui.openModal(second);

    FlixelDebugNode layer = (FlixelDebugNode) FlixelDebugNodes.child(ui, 1);
    assertEquals(2, layer.getDebugChildCount());
    assertSame(first, layer.getDebugChild(0));
    assertSame(second, layer.getDebugChild(1));
  }

  private static FlixelUiModal modal() {
    FlixelUiModal m = new FlixelUiModal(100, 100);
    m.setStyle(new FlixelUiPanelStyle());
    m.setModalStyle(new FlixelUiModalStyle());
    return m;
  }

  @Test
  void widgetBoundsUseTheScreenRectOnTheDisplayCamera() {
    TestWidget w = new TestWidget(80, 30);
    w.setPosition(12, 7);
    ui.getRoot().add(w);
    ui.layout();

    assertTrue(w.getDebugBounds(camera, box));
    assertEquals(w.getScreenX(), box[0]);
    assertEquals(w.getScreenY(), box[1]);
    assertEquals(80f, box[2]);
    assertEquals(30f, box[3]);
  }

  @Test
  void widgetBoundsAreEmptyWhenNotDrawnThere() {
    TestWidget w = new TestWidget(80, 30);
    ui.getRoot().add(w);
    ui.layout();

    assertFalse(w.getDebugBounds(new FlixelCamera(640, 360), box), "other camera");

    w.setVisible(false);
    assertFalse(w.getDebugBounds(camera, box), "hidden");
    w.setVisible(true);
    w.setExists(false);
    assertFalse(w.getDebugBounds(camera, box), "does not exist");
    w.setExists(true);
    w.setSize(0, 30);
    assertFalse(w.getDebugBounds(camera, box), "no area");

    TestWidget loose = new TestWidget(10, 10);
    assertFalse(loose.getDebugBounds(camera, box), "not on a display");
  }

  @Test
  void hiddenAncestorHidesTheBoxButNotTheTree() {
    FlixelUiContainer holder = new FlixelUiContainer(200, 100);
    TestWidget w = new TestWidget(20, 20);
    holder.add(w);
    ui.getRoot().add(holder);
    ui.layout();
    assertTrue(w.getDebugBounds(camera, box));

    holder.setVisible(false);
    assertFalse(w.getDebugBounds(camera, box));
    assertEquals(1, holder.getDebugChildCount());
  }

  @Test
  void displayLayersNeverCoverTheirWidgets() {
    ui.layout();
    assertFalse(ui.getRoot().getDebugBounds(camera, box));
    assertFalse(ui.getModalLayer().getDebugBounds(camera, box));
    assertFalse(ui.getPopupLayer().getDebugBounds(camera, box));
  }

  @Test
  void injectedTypedCharactersReachTheFocusedTextBox() {
    FlixelInputDevice previous = Flixel.input;
    FlixelKeyInputManager previousKeys = Flixel.keys;
    Flixel.input = new FlixelBaseInputDevice() {
    };
    Flixel.keys = new FlixelKeyInputManager();
    try {
      FlixelTextBox field = new FlixelTextBox(200);
      field.setStyle(new FlixelTextBoxStyle());
      ui.getRoot().add(field);
      field.focus();
      Flixel.keys.injectTyped('h');
      Flixel.keys.injectTyped('i');
      assertEquals("hi", field.getText().toString());
      field.destroy();
    } finally {
      Flixel.keys = previousKeys;
      Flixel.input = previous;
    }
  }

  @Test
  void injectedMouseButtonsDriveAButtonThroughThePointer() {
    FlixelInputDevice previous = Flixel.input;
    FlixelMouseInputManager previousMouse = Flixel.mouse;
    Flixel.input = new FlixelBaseInputDevice() {
    };
    Flixel.mouse = new FlixelMouseInputManager();
    try {
      FlixelUiButton button = new FlixelUiButton("Go");
      button.setStyle(new FlixelUiButtonStyle());
      button.setAutoSize(false);
      button.setSize(80, 30);
      int[] clicks = new int[1];
      button.onClick.add(b -> clicks[0]++);
      ui.getRoot().add(button);
      ui.layout();
      FlixelUiPointer pointer = new FlixelUiPointer(ui);

      assertTrue(button.getDebugBounds(camera, box));
      float cx = box[0] + box[2] * 0.5f;
      float cy = box[1] + box[3] * 0.5f;
      pointer.move(cx, cy);
      // The UI never reads input, so the game forwards the mouse state to the pointer.
      Flixel.mouse.injectButton(FlixelMouseButton.LEFT, true);
      assertTrue(Flixel.mouse.pressed(FlixelMouseButton.LEFT));
      pointer.down();
      Flixel.mouse.injectButton(FlixelMouseButton.LEFT, false);
      assertFalse(Flixel.mouse.pressed(FlixelMouseButton.LEFT));
      pointer.up();
      assertEquals(1, clicks[0]);
    } finally {
      Flixel.mouse.releaseInjected();
      Flixel.mouse = previousMouse;
      Flixel.input = previous;
    }
  }
}
