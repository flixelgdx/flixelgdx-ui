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
import org.flixelgdx.input.mouse.FlixelMouseCursor;
import org.flixelgdx.ui.skin.FlixelTextBoxStyle;
import org.flixelgdx.ui.skin.FlixelUiButtonStyle;
import org.flixelgdx.ui.skin.FlixelUiSliderStyle;
import org.flixelgdx.ui.text.FlixelTextBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Verifies the cursors widgets ask for and how {@link FlixelUiPointer#getCursor()} picks one. */
@ExtendWith(FlixelUiHeadlessExtension.class)
class FlixelUiCursorsTest {

  private FlixelUiDisplay ui;
  private FlixelUiPointer pointer;

  @BeforeEach
  void setUp() {
    FlixelUiCursors.reset();
    Flixel.cameras.clear();
    FlixelCamera camera = new FlixelCamera(640, 360);
    Flixel.cameras.add(camera);
    ui = new FlixelUiDisplay(camera);
    pointer = new FlixelUiPointer(ui);
  }

  @AfterEach
  void tearDown() {
    FlixelUiCursors.reset();
    Flixel.cameras.clear();
  }

  @Test
  void eachWidgetTypeHasItsDefaultCursor() {
    assertEquals(FlixelMouseCursor.HAND, button(0, 0).getCursor());
    assertEquals(FlixelMouseCursor.IBEAM, textBox(0, 100).getCursor());
    FlixelUiSlider slider = slider(0, 200);
    assertEquals(FlixelMouseCursor.GRAB, slider.getCursor());
    assertEquals(FlixelMouseCursor.GRABBING, slider.getPressedCursor());
    assertNull(new TestWidget(10, 10).getCursor(), "plain widgets ask for no cursor");
  }

  @Test
  void pointerReportsTheHoveredWidgetsCursor() {
    button(0, 0);
    textBox(0, 100);

    assertEquals(FlixelMouseCursor.ARROW, pointer.getCursor(), "nothing hovered");
    pointer.move(10, 10);
    assertEquals(FlixelMouseCursor.HAND, pointer.getCursor());
    pointer.move(10, 110);
    assertEquals(FlixelMouseCursor.IBEAM, pointer.getCursor());
    pointer.move(500, 300);
    assertEquals(FlixelMouseCursor.ARROW, pointer.getCursor());
  }

  @Test
  void pressedCursorLastsForTheWholeDrag() {
    slider(0, 200);

    pointer.move(15, 210);
    assertEquals(FlixelMouseCursor.GRAB, pointer.getCursor());
    pointer.down();
    assertEquals(FlixelMouseCursor.GRABBING, pointer.getCursor());
    pointer.move(500, 300);
    assertEquals(FlixelMouseCursor.GRABBING, pointer.getCursor(), "dragging off keeps the grab");
    pointer.up();
    assertEquals(FlixelMouseCursor.ARROW, pointer.getCursor());
  }

  @Test
  void pressedCursorFallsBackToTheHoverCursor() {
    FlixelUiButton button = button(0, 0);
    assertEquals(FlixelMouseCursor.HAND, button.getPressedCursor());

    FlixelUiCursors.sliderDragging = null;
    assertEquals(FlixelMouseCursor.GRAB, slider(0, 200).getPressedCursor());
  }

  @Test
  void aWidgetCanPickItsOwnCursor() {
    FlixelUiButton button = button(0, 0);
    button.setCursor(FlixelMouseCursor.NOT_ALLOWED);
    button.setPressedCursor(FlixelMouseCursor.CROSSHAIR);
    pointer.move(10, 10);
    assertEquals(FlixelMouseCursor.NOT_ALLOWED, pointer.getCursor());
    pointer.down();
    assertEquals(FlixelMouseCursor.CROSSHAIR, pointer.getCursor());
    pointer.up();

    button.setCursor(null);
    button.setPressedCursor(null);
    assertEquals(FlixelMouseCursor.HAND, button.getCursor(), "null goes back to the default");
  }

  @Test
  void aWidgetTypeCanBeTurnedOff() {
    button(0, 0);
    FlixelUiCursors.button = null;

    pointer.move(10, 10);
    assertEquals(FlixelMouseCursor.ARROW, pointer.getCursor());
  }

  @Test
  void cursorsCanBeTurnedOffEverywhere() {
    button(0, 0);
    FlixelUiCursors.enabled = false;

    pointer.move(10, 10);
    assertEquals(FlixelMouseCursor.ARROW, pointer.getCursor());
  }

  @Test
  void disabledWidgetsDoNotChangeTheCursor() {
    FlixelUiButton button = button(0, 0);
    pointer.move(10, 10);
    button.setEnabled(false);

    assertEquals(FlixelMouseCursor.ARROW, pointer.getCursor());
  }

  private FlixelUiButton button(float x, float y) {
    FlixelUiButton button = new FlixelUiButton("Play");
    button.setStyle(new FlixelUiButtonStyle());
    button.setAutoSize(false);
    button.setSize(80, 30);
    button.setPosition(x, y);
    ui.add(button);
    return button;
  }

  private FlixelTextBox textBox(float x, float y) {
    FlixelTextBox box = new FlixelTextBox(200);
    FlixelTextBoxStyle style = new FlixelTextBoxStyle();
    style.fontSize = 14;
    box.setStyle(style);
    box.setPosition(x, y);
    ui.add(box);
    ui.layout();
    return box;
  }

  private FlixelUiSlider slider(float x, float y) {
    FlixelUiSlider slider = new FlixelUiSlider(200, 0, 1, 0.5f);
    FlixelUiSliderStyle style = new FlixelUiSliderStyle();
    style.thumbWidth = 20;
    style.thumbHeight = 20;
    style.trackThickness = 6;
    slider.setStyle(style);
    slider.setPosition(x, y);
    ui.add(slider);
    ui.layout();
    return slider;
  }
}
