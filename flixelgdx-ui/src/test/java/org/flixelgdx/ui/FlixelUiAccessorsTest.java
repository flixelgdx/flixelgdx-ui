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
import org.flixelgdx.ui.FlixelUiStack.Direction;
import org.flixelgdx.ui.skin.FlixelTextBoxStyle;
import org.flixelgdx.ui.skin.FlixelUiDropdownStyle;
import org.flixelgdx.ui.skin.FlixelUiModalStyle;
import org.flixelgdx.ui.text.FlixelTextBox;
import org.flixelgdx.ui.text.FlixelTextFilter;
import org.flixelgdx.util.FlixelAlign;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the single-value getters and setters that pair up into properties, so each setter's
 * value reads back and the setters that affect layout behave like their multi-value forms.
 */
@ExtendWith(FlixelUiHeadlessExtension.class)
class FlixelUiAccessorsTest {

  private static final float EPS = 0.0001f;

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

  @Test
  void anchorSettersLayOutLikeAnchor() {
    TestWidget byParts = new TestWidget(10, 10);
    byParts.setAnchor(FlixelAlign.BOTTOM_RIGHT);
    byParts.setAnchorOffsetX(-8);
    byParts.setAnchorOffsetY(-4);
    TestWidget atOnce = new TestWidget(10, 10);
    atOnce.anchor(FlixelAlign.BOTTOM_RIGHT, -8, -4);
    ui.add(byParts);
    ui.add(atOnce);
    ui.layout();

    assertEquals(FlixelAlign.BOTTOM_RIGHT, byParts.getAnchor());
    assertEquals(-8f, byParts.getAnchorOffsetX(), EPS);
    assertEquals(-4f, byParts.getAnchorOffsetY(), EPS);
    assertEquals(atOnce.getX(), byParts.getX(), EPS);
    assertEquals(atOnce.getY(), byParts.getY(), EPS);
  }

  @Test
  void percentSettersSizeEachAxis() {
    TestWidget w = new TestWidget(10, 10);
    w.setPercentWidth(0.5f);
    ui.add(w);
    ui.layout();
    assertEquals(0.5f, w.getPercentWidth(), EPS);
    assertTrue(Float.isNaN(w.getPercentHeight()));
    assertEquals(320f, w.getWidth(), EPS);
    assertEquals(10f, w.getHeight(), EPS);

    w.setPercentHeight(1f);
    ui.layout();
    assertEquals(360f, w.getHeight(), EPS);
  }

  @Test
  void stackPercentSettersTurnOffAutoSizePerAxis() {
    FlixelUiStack stack = new FlixelUiStack(Direction.VERTICAL);
    assertTrue(stack.isAutoSize());
    stack.setPercentWidth(1f);
    assertFalse(stack.isAutoWidth());
    assertTrue(stack.isAutoHeight());
    assertFalse(stack.isAutoSize());
    stack.setPercentHeight(Float.NaN);
    assertTrue(stack.isAutoHeight(), "NaN leaves the height alone");
  }

  @Test
  void styleNameIsAProperty() {
    TestWidget w = new TestWidget(10, 10);
    w.setStyleName("fab");
    assertEquals("fab", w.getStyleName());
  }

  @Test
  void radioGroupSelectedIndexIsSilent() {
    FlixelUiRadioGroup group = new FlixelUiRadioGroup();
    new FlixelUiRadioButton("A", group);
    new FlixelUiRadioButton("B", group);
    int[] changes = new int[1];
    group.onChange.add(g -> changes[0]++);

    group.setSelectedIndex(1);
    assertEquals(1, group.getSelectedIndex());
    group.setSelectedIndex(-1);
    assertEquals(-1, group.getSelectedIndex());
    assertNull(group.getSelected());
    assertEquals(0, changes[0], "setSelectedIndex() never fires onChange");
  }

  @Test
  void textBoxSettersReadBack() {
    FlixelTextBox box = new FlixelTextBox(200);
    FlixelTextBoxStyle style = new FlixelTextBoxStyle();
    box.setStyle(style);
    box.setPlaceholder("PIN");
    box.setPasswordChar('*');
    box.setMaxLength(6);
    box.setMultiLine(true);

    assertSame(style, box.getStyle());
    assertEquals("PIN", box.getPlaceholder().toString());
    assertEquals('*', box.getPasswordChar());
    assertEquals(6, box.getMaxLength());
    assertTrue(box.isMultiLine());

    box.setPlaceholder(null);
    assertNull(box.getPlaceholder());
  }

  @Test
  void textBoxFilterMirrorsItsSetter() {
    FlixelTextBox box = new FlixelTextBox(200);
    assertNull(box.getFilter(), "no filter reads as null");
    box.setFilter(FlixelTextFilter.DIGITS);
    assertSame(FlixelTextFilter.DIGITS, box.getFilter());
    box.setFilter(null);
    assertNull(box.getFilter());
  }

  @Test
  void textBoxCaretAndSelectionReadBack() {
    FlixelTextBox box = new FlixelTextBox(200);
    box.setText("hello");
    box.setCaret(2);
    assertEquals(2, box.getCaret());
    box.select(1, 4);
    assertEquals(1, box.getSelectionStart());
    assertEquals(4, box.getSelectionEnd());
  }

  @Test
  void dropdownPlaceholderAndStyleReadBack() {
    FlixelUiDropdown d = new FlixelUiDropdown(100);
    assertNull(d.getPlaceholder());
    d.setPlaceholder("Pick one");
    assertEquals("Pick one", d.getPlaceholder().toString());
    FlixelUiDropdownStyle style = new FlixelUiDropdownStyle();
    d.setStyle(style);
    assertSame(style, d.getStyle());
  }

  @Test
  void toggleTextReadsBack() {
    FlixelUiCheckbox box = new FlixelUiCheckbox("Sound");
    assertEquals("Sound", box.getText().toString());
    box.setText("Music");
    assertEquals("Music", box.getText().toString());
  }

  @Test
  void autoSizeCoversBothAxes() {
    FlixelUiButton button = new FlixelUiButton("Go");
    assertTrue(button.isAutoSize());
    button.setWidth(50);
    assertFalse(button.isAutoSize());

    FlixelUiLabel label = new FlixelUiLabel("Hi");
    label.setAutoSize(false);
    assertFalse(label.isAutoSize());
  }

  @Test
  void modalStyleAndTooltipStyleReadBack() {
    FlixelUiModal modal = new FlixelUiModal(100, 100);
    FlixelUiModalStyle style = new FlixelUiModalStyle();
    modal.setModalStyle(style);
    assertSame(style, modal.getModalStyle());

    assertEquals("default", ui.getTooltipStyle());
    ui.setTooltipStyle("big");
    assertEquals("big", ui.getTooltipStyle());
  }
}
