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
import org.flixelgdx.ui.skin.FlixelTextBoxStyle;
import org.flixelgdx.ui.skin.FlixelUiButtonStyle;
import org.flixelgdx.ui.skin.FlixelUiDropdownStyle;
import org.flixelgdx.ui.text.FlixelTextBox;
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

/** Verifies that {@link FlixelUiPointer} turns positions and presses into the right widget calls. */
@ExtendWith(FlixelUiHeadlessExtension.class)
class FlixelUiPointerTest {

  private FlixelUiDisplay ui;
  private FlixelUiPointer pointer;

  @BeforeEach
  void setUp() {
    Flixel.cameras.clear();
    FlixelCamera camera = new FlixelCamera(640, 360);
    Flixel.cameras.add(camera);
    ui = new FlixelUiDisplay(camera);
    pointer = new FlixelUiPointer(ui);
  }

  @AfterEach
  void tearDown() {
    Flixel.cameras.clear();
  }

  @Test
  void rejectsNullDisplay() {
    assertThrows(IllegalArgumentException.class, () -> new FlixelUiPointer(null));
  }

  @Test
  void hoverFollowsThePointer() {
    ActivatableWidget a = widgetAt(0, 0);
    ActivatableWidget b = widgetAt(100, 0);

    pointer.move(5, 5);
    assertTrue(a.isHovered());
    assertSame(a, pointer.getHovered());

    pointer.move(105, 5);
    assertFalse(a.isHovered());
    assertTrue(b.isHovered());

    pointer.move(300, 300);
    assertFalse(b.isHovered());
    assertNull(pointer.getHovered(), "empty space hovers nothing");
  }

  @Test
  void tooltipAppearsAfterTheDelayAndHidesWhenLeaving() {
    ActivatableWidget w = widgetAt(0, 0);
    w.setTooltip("Hello");
    pointer.tooltipDelay = 0.4f;

    pointer.move(5, 5);
    assertSame(w, ui.getTooltipTarget());
    assertFalse(ui.isTooltipVisible(), "the delay has not run yet");

    ui.update(0.5f);
    assertTrue(ui.isTooltipVisible());

    pointer.move(300, 300);
    assertFalse(ui.isTooltipVisible());
    assertNull(ui.getTooltipTarget());
  }

  @Test
  void pressingHidesTheTooltip() {
    ActivatableWidget w = widgetAt(0, 0);
    w.setTooltip("Hello");
    pointer.tooltipDelay = 0f;
    pointer.move(5, 5);
    assertTrue(ui.isTooltipVisible());
    pointer.down();
    assertFalse(ui.isTooltipVisible());
  }

  @Test
  void activatesOnlyWhenReleasedOverThePressedWidget() {
    ActivatableWidget w = widgetAt(0, 0);

    pointer.move(5, 5);
    pointer.down();
    assertTrue(w.isPressed());
    pointer.up();
    assertFalse(w.isPressed());
    assertEquals(1, w.activations);
    assertEquals(5f, w.lastX);

    pointer.down();
    pointer.move(300, 300);
    pointer.up();
    assertFalse(w.isPressed());
    assertEquals(1, w.activations, "dragging off before releasing must not activate");
  }

  @Test
  void clicksButtons() {
    FlixelUiButton button = new FlixelUiButton("Play");
    button.setStyle(new FlixelUiButtonStyle());
    button.setAutoSize(false);
    button.setSize(80, 30);
    int[] clicks = new int[1];
    button.onClick.add(b -> clicks[0]++);
    ui.add(button);

    pointer.move(10, 10);
    pointer.down();
    pointer.up();
    assertEquals(1, clicks[0]);
  }

  @Test
  void cancelReleasesWithoutActivating() {
    ActivatableWidget w = widgetAt(0, 0);
    pointer.move(5, 5);
    pointer.down();
    pointer.cancel();
    assertFalse(w.isPressed());
    pointer.up();
    assertEquals(0, w.activations);
  }

  @Test
  void resetForgetsHoverAndPress() {
    ActivatableWidget w = widgetAt(0, 0);
    pointer.move(5, 5);
    pointer.down();
    pointer.reset();
    assertFalse(w.isHovered());
    assertFalse(w.isPressed());
    assertNull(pointer.getHovered());
    assertNull(pointer.getPressed());
  }

  @Test
  void doesNotActivateAWidgetDisabledWhilePressed() {
    ActivatableWidget w = widgetAt(0, 0);
    pointer.move(5, 5);
    pointer.down();
    w.setEnabled(false);
    pointer.up();
    assertEquals(0, w.activations);
  }

  @Test
  void dropsWidgetsRemovedFromTheDisplay() {
    ActivatableWidget w = widgetAt(0, 0);
    pointer.move(5, 5);
    pointer.down();
    ui.remove(w);
    pointer.up();
    assertEquals(0, w.activations);
    assertNull(pointer.getHovered());
    assertNull(pointer.getPressed());
  }

  @Test
  void pressingElsewhereBlursTheFocusedWidget() {
    ActivatableWidget a = widgetAt(0, 0);
    ActivatableWidget b = widgetAt(100, 0);
    a.focus();

    pointer.move(5, 5);
    pointer.down();
    assertTrue(a.isFocused(), "pressing the focused widget keeps its focus");
    pointer.up();

    pointer.move(105, 5);
    pointer.down();
    assertFalse(a.isFocused());
    pointer.up();

    b.focus();
    pointer.move(300, 300);
    pointer.down();
    assertFalse(b.isFocused(), "pressing empty space blurs too");
  }

  @Test
  void clickingATextBoxFocusesIt() {
    FlixelTextBox box = new FlixelTextBox(200);
    FlixelTextBoxStyle style = new FlixelTextBoxStyle();
    style.fontSize = 14;
    box.setStyle(style);
    ui.add(box);
    ui.layout();

    pointer.move(10, 10);
    pointer.down();
    pointer.up();
    assertTrue(box.isFocused());
  }

  @Test
  void opensDropdownsAndSelectsTheClickedRow() {
    FlixelUiDropdown d = dropdown();
    pointer.move(10, 10);
    pointer.down();
    pointer.up();
    assertTrue(d.isOpen());
    ui.layout();

    float row1Y = listTop(d) + 30f;
    pointer.move(10, row1Y);
    assertEquals(1, d.getHighlightedIndex(), "hovering a row highlights it");

    pointer.down();
    pointer.up();
    assertEquals(1, d.getSelectedIndex());
    assertFalse(d.isOpen());
  }

  @Test
  void pressingOutsideAnOpenListClosesIt() {
    FlixelUiDropdown d = dropdown();
    d.open();
    ui.layout();
    pointer.move(600, 300);
    pointer.down();
    assertFalse(d.isOpen());
  }

  @Test
  void pressingTheFieldAgainClosesTheList() {
    FlixelUiDropdown d = dropdown();
    d.open();
    ui.layout();
    pointer.move(10, 10);
    pointer.down();
    assertTrue(d.isOpen(), "the press alone must not close the list the release is about to toggle");
    pointer.up();
    assertFalse(d.isOpen());
  }

  @Test
  void scrollsAnOpenList() {
    FlixelUiDropdown d = dropdown();
    d.setMaxVisibleItems(2);
    d.open();
    ui.layout();
    pointer.move(10, listTop(d) + 5f);
    assertTrue(pointer.scroll(1f));
    assertEquals(1f, d.getScrollOffset());
  }

  @Test
  void scrollBubblesUpToAnAncestor() {
    ScrollContainer box = new ScrollContainer();
    ActivatableWidget child = new ActivatableWidget(20, 20);
    box.add(child);
    ui.add(box);

    pointer.move(5, 5);
    assertSame(child, pointer.getHovered());
    assertTrue(pointer.scroll(2f));
    assertEquals(2f, box.scrolled);

    pointer.move(300, 300);
    assertFalse(pointer.scroll(1f), "nothing under the pointer uses the scroll");
    assertFalse(pointer.scroll(0f));
  }

  private ActivatableWidget widgetAt(float x, float y) {
    ActivatableWidget w = new ActivatableWidget(50, 50);
    w.setPosition(x, y);
    ui.add(w);
    return w;
  }

  private FlixelUiDropdown dropdown() {
    FlixelUiDropdown d = new FlixelUiDropdown(200);
    FlixelUiDropdownStyle s = new FlixelUiDropdownStyle();
    s.fontSize = 14;
    s.itemHeight = 20f;
    s.padTop = 4f;
    s.padBottom = 4f;
    s.listGap = 2f;
    d.setStyle(s);
    d.addItem("A");
    d.addItem("B");
    d.addItem("C");
    ui.add(d);
    ui.layout();
    return d;
  }

  private static float listTop(FlixelUiDropdown d) {
    return d.getScreenY() + d.getHeight() + 2f;
  }

  /** A plain widget that records the pointer hooks it receives. */
  private static final class ActivatableWidget extends TestWidget {

    float lastX = Float.NaN;
    int activations;

    ActivatableWidget(float width, float height) {
      super(width, height);
    }

    @Override
    protected void onActivate(float x, float y) {
      activations++;
      lastX = x;
    }
  }

  /** A container that consumes every scroll offered to it. */
  private static final class ScrollContainer extends FlixelUiContainer {

    float scrolled;

    ScrollContainer() {
      super(100, 100);
    }

    @Override
    protected boolean onScroll(float amount) {
      scrolled += amount;
      return true;
    }
  }
}
