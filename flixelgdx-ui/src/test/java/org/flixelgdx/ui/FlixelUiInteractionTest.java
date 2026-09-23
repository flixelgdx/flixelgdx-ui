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
import org.flixelgdx.util.signal.FlixelSignal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the interaction methods the game calls on widgets and the signals they dispatch.
 */
@ExtendWith(FlixelUiHeadlessExtension.class)
class FlixelUiInteractionTest {

  private FlixelUiDisplay ui;
  private TestWidget a;
  private TestWidget b;

  @BeforeEach
  void setUp() {
    ui = new FlixelUiDisplay(new FlixelCamera(640, 360));
    a = new TestWidget(10, 10);
    b = new TestWidget(10, 10);
    ui.add(a);
    ui.add(b);
  }

  @Test
  void hoverFiresOncePerChange() {
    int[] count = counter(a.onHover);
    int[] out = counter(a.onUnhover);
    a.hover();
    a.hover();
    assertTrue(a.isHovered());
    assertEquals(1, count[0]);
    a.unhover();
    a.unhover();
    assertFalse(a.isHovered());
    assertEquals(1, out[0]);
    assertEquals(2, a.stateChanges);
  }

  @Test
  void pressAndReleaseFireOncePerChange() {
    int[] pressed = counter(a.onPress);
    int[] released = counter(a.onRelease);
    a.release();
    assertEquals(0, released[0], "release without press changes nothing");
    a.press();
    a.press();
    assertTrue(a.isPressed());
    a.release();
    a.release();
    assertFalse(a.isPressed());
    assertEquals(1, pressed[0]);
    assertEquals(1, released[0]);
  }

  @Test
  void signalsCarryTheWidget() {
    FlixelUiWidget[] received = new FlixelUiWidget[1];
    a.onPress.add(w -> received[0] = w);
    a.press();
    assertSame(a, received[0]);
  }

  @Test
  void disabledWidgetsIgnoreInteraction() {
    int[] hovered = counter(a.onHover);
    int[] pressed = counter(a.onPress);
    int[] focused = counter(a.onFocus);
    a.setEnabled(false);
    a.hover();
    a.press();
    a.focus();
    assertFalse(a.isHovered());
    assertFalse(a.isPressed());
    assertFalse(a.isFocused());
    assertEquals(0, hovered[0]);
    assertEquals(0, pressed[0]);
    assertEquals(0, focused[0]);
    a.setEnabled(true);
    a.hover();
    assertEquals(1, hovered[0]);
  }

  @Test
  void disablingClearsStates() {
    int[] unhovered = counter(a.onUnhover);
    int[] released = counter(a.onRelease);
    int[] blurred = counter(a.onBlur);
    a.hover();
    a.press();
    a.focus();
    a.setEnabled(false);
    assertFalse(a.isEnabled());
    assertFalse(a.isHovered());
    assertFalse(a.isPressed());
    assertFalse(a.isFocused());
    assertNull(ui.getFocused());
    assertEquals(1, unhovered[0]);
    assertEquals(1, blurred[0]);
    assertEquals(0, released[0], "disabling must never look like an activation");
  }

  @Test
  void focusIsSinglePerDisplay() {
    int[] aBlurred = counter(a.onBlur);
    int[] bFocused = counter(b.onFocus);
    a.focus();
    assertSame(a, ui.getFocused());
    b.focus();
    assertSame(b, ui.getFocused());
    assertFalse(a.isFocused());
    assertTrue(b.isFocused());
    assertEquals(1, aBlurred[0]);
    assertEquals(1, bFocused[0]);
    b.blur();
    assertNull(ui.getFocused());
  }

  @Test
  void focusIsSeparatePerDisplay() {
    FlixelUiDisplay other = new FlixelUiDisplay(new FlixelCamera(640, 360));
    TestWidget c = new TestWidget(10, 10);
    other.add(c);
    a.focus();
    c.focus();
    assertTrue(a.isFocused());
    assertTrue(c.isFocused());
    assertSame(a, ui.getFocused());
    assertSame(c, other.getFocused());
  }

  @Test
  void widgetsWithoutADisplayHoldFocusLocally() {
    TestWidget loose = new TestWidget(10, 10);
    loose.focus();
    assertTrue(loose.isFocused());
    a.focus();
    assertTrue(loose.isFocused(), "focus on a display does not affect widgets outside it");

    // Adding the focused widget moves the display's focus to it.
    ui.add(loose);
    assertSame(loose, ui.getFocused());
    assertFalse(a.isFocused());
  }

  @Test
  void removingTheFocusedWidgetBlursIt() {
    int[] blurred = counter(a.onBlur);
    a.focus();
    ui.remove(a);
    assertFalse(a.isFocused());
    assertNull(ui.getFocused());
    assertEquals(1, blurred[0]);
  }

  @Test
  void destroyClearsFocusWithoutFiringListeners() {
    int[] blurred = counter(a.onBlur);
    a.focus();
    a.destroy();
    assertNull(ui.getFocused());
    assertFalse(a.isFocused());
    assertEquals(0, blurred[0]);
    assertNull(a.getParent());
  }

  @Test
  void focusSurvivesMovingBetweenContainersOfTheSameDisplay() {
    FlixelUiContainer group = new FlixelUiContainer();
    ui.add(group);
    a.focus();
    group.add(a);
    assertTrue(a.isFocused());
    assertSame(a, ui.getFocused());
  }

  private static int[] counter(FlixelSignal<FlixelUiWidget> signal) {
    int[] count = new int[1];
    signal.add(w -> count[0]++);
    return count;
  }
}
