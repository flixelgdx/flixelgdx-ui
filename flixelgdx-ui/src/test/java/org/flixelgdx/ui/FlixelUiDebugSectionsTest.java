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
import org.flixelgdx.debug.FlixelDebugObjectIds;
import org.flixelgdx.debug.feeds.FlixelDebugInspector;
import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.ui.skin.FlixelTextBoxStyle;
import org.flixelgdx.ui.text.FlixelTextBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies the inspector rows of the UI widgets and that edits go through the normal setters. */
@ExtendWith(FlixelUiHeadlessExtension.class)
class FlixelUiDebugSectionsTest {

  private FlixelDebugInspector inspector;

  @BeforeEach
  void setUp() {
    Flixel.cameras.clear();
    Flixel.cameras.add(new FlixelCamera(640, 360));
    inspector = new FlixelDebugInspector(new FlixelDebugObjectIds());
    FlixelUiDebugSections.register(inspector);
  }

  @AfterEach
  void tearDown() {
    Flixel.cameras.clear();
  }

  @Test
  void registrationHappensOnceAndCoversTheToolkit() {
    assertTrue(inspector.isRegistered(FlixelUiWidget.class));
    assertTrue(inspector.isRegistered(FlixelUiCheckbox.class));
    assertTrue(inspector.isRegistered(FlixelTextBox.class));
    assertTrue(inspector.isRegistered(FlixelUiDisplay.class));
    FlixelUiDebugSections.register(inspector);
    assertTrue(inspector.isRegistered(FlixelUiSlider.class));
  }

  @Test
  void checkboxShowsItsOwnAndCommonRows() {
    inspector.select(new FlixelUiCheckbox("Sound", true));
    String rows = rows();
    assertTrue(rows.contains("Checked"));
    assertTrue(rows.contains("Text"));
    assertTrue(rows.contains("Screen X"));
    assertTrue(rows.contains("Tooltip"));
  }

  @Test
  void editingCheckedFiresTheChangeSignal() {
    FlixelUiCheckbox box = new FlixelUiCheckbox("Sound", false);
    int[] changes = new int[1];
    box.onChange.add(c -> changes[0]++);
    inspector.select(box);
    rows();

    set(0, 0, w -> w.writeValue(true));

    assertTrue(box.isChecked());
    assertEquals(1, changes[0]);
  }

  @Test
  void sliderValueAndRangeAreEditable() {
    FlixelUiSlider slider = new FlixelUiSlider(100, 0, 10, 2);
    int[] changes = new int[1];
    slider.onChange.add(s -> changes[0]++);
    inspector.select(slider);
    assertTrue(rows().contains("Value"));

    set(0, 0, w -> w.writeValue(7f));
    set(0, 2, w -> w.writeValue(20f));

    assertEquals(7f, slider.getValue());
    assertEquals(20f, slider.getMax());
    assertEquals(1, changes[0]);
  }

  @Test
  void dropdownSelectionFiresOnSelect() {
    FlixelUiDropdown dropdown = new FlixelUiDropdown(100);
    dropdown.addItem("a");
    dropdown.addItem("b");
    int[] selects = new int[1];
    dropdown.onSelect.add(d -> selects[0]++);
    inspector.select(dropdown);
    rows();

    set(0, 0, w -> w.writeValue(1));

    assertEquals(1, dropdown.getSelectedIndex());
    assertEquals(1, selects[0]);
  }

  @Test
  void textBoxTextAndWidgetRowsAreEditable() {
    FlixelTextBox field = new FlixelTextBox(200);
    field.setStyle(new FlixelTextBoxStyle());
    inspector.select(field);
    assertTrue(rows().contains("Caret"));

    set(0, 0, w -> w.writeValue("hello"));
    assertEquals("hello", field.getText().toString());

    // TextBox, then the widget section, whose first row is Visible.
    set(1, 0, w -> w.writeValue(false));
    assertFalse(field.isVisible());
  }

  @Test
  void labelTextIsEditable() {
    FlixelUiLabel label = new FlixelUiLabel("old");
    inspector.select(label);
    rows();
    set(0, 0, w -> w.writeValue("new"));
    assertEquals("new", label.getText().toString());
  }

  private String rows() {
    FlixelDebugMessageWriter w = new FlixelDebugMessageWriter(64);
    w.beginMessage(FlixelDebugProtocol.MSG_FEED_DATA);
    w.writeU16(7);
    inspector.write(w);
    ByteBuffer data = w.finish();
    byte[] bytes = new byte[data.remaining()];
    data.get(bytes);
    return new String(bytes, StandardCharsets.ISO_8859_1);
  }

  private void set(int section, int property, Consumer<FlixelDebugMessageWriter> value) {
    FlixelDebugMessageWriter w = new FlixelDebugMessageWriter(64);
    w.beginMessage(FlixelDebugProtocol.MSG_FEED_ACTION);
    w.writeU16(7);
    w.writeU8(FlixelDebugProtocol.INSPECTOR_SET_PROPERTY);
    w.writeI32(inspector.getSelectedId()).writeU16(section).writeU16(property);
    value.accept(w);
    FlixelDebugMessageReader r = new FlixelDebugMessageReader(w.finish());
    r.readU8();
    r.readU16();
    int action = r.readU8();
    inspector.handleAction(action, r);
  }
}
