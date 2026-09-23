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
package org.flixelgdx.ui.text;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.input.keyboard.FlixelKey;
import org.flixelgdx.ui.FlixelUiDisplay;
import org.flixelgdx.ui.FlixelUiHeadlessExtension;
import org.flixelgdx.ui.skin.FlixelTextBoxStyle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link FlixelTextBox}: typing, deletion, signals, focus/blur, filter, max length,
 * keyboard listener routing, and submit behavior.
 */
@ExtendWith(FlixelUiHeadlessExtension.class)
class FlixelTextBoxTest {

  private FlixelCamera camera;
  private FlixelUiDisplay ui;

  @BeforeEach
  void setUp() {
    Flixel.cameras.clear();
    camera = new FlixelCamera(640, 360);
    Flixel.cameras.add(camera);
    ui = new FlixelUiDisplay(camera);
  }

  private FlixelTextBox textBox() {
    FlixelTextBox box = new FlixelTextBox(200);
    box.setStyle(style());
    return box;
  }

  private static FlixelTextBoxStyle style() {
    FlixelTextBoxStyle s = new FlixelTextBoxStyle();
    s.fontSize = 14;
    s.padLeft = 4f;
    s.padTop = 4f;
    s.padRight = 4f;
    s.padBottom = 4f;
    return s;
  }

  @Test
  void type_appendsText() {
    FlixelTextBox box = textBox();

    box.type('H');
    box.type('i');

    assertEquals("Hi", box.getText().toString());
  }

  @Test
  void type_returnsFalse_whenDisabled() {
    FlixelTextBox box = textBox();
    box.setEnabled(false);
    assertFalse(box.type('a'));
    assertEquals("", box.getText().toString());
  }

  @Test
  void backspace_removesLastChar() {
    FlixelTextBox box = textBox();
    box.type('a');
    box.type('b');
    assertTrue(box.backspace());
    assertEquals("a", box.getText().toString());
  }

  @Test
  void backspace_returnsFalse_whenDisabled() {
    FlixelTextBox box = textBox();
    box.type('a');
    box.setEnabled(false);
    assertFalse(box.backspace());
    assertEquals("a", box.getText().toString());
  }

  @Test
  void deleteForward_removesCharAfterCaret() {
    FlixelTextBox box = textBox();
    box.insert("ab");
    box.setCaret(0);
    assertTrue(box.deleteForward());
    assertEquals("b", box.getText().toString());
  }

  @Test
  void insert_addsText() {
    FlixelTextBox box = textBox();
    assertTrue(box.insert("hello"));
    assertEquals("hello", box.getText().toString());
  }

  @Test
  void insert_returnsFalse_whenDisabled() {
    FlixelTextBox box = textBox();
    box.setEnabled(false);
    assertFalse(box.insert("hi"));
    assertEquals("", box.getText().toString());
  }

  @Test
  void setText_replacesContent() {
    FlixelTextBox box = textBox();
    box.type('x');
    box.setText("new");
    assertEquals("new", box.getText().toString());
  }

  /**
   * Verifies that {@link FlixelTextBox#onChange} fires synchronously inside the mutating call,
   * not deferred to the next {@code update()} tick.
   */
  @Test
  void onChange_firesSynchronously_perEdit() {
    FlixelTextBox box = textBox();
    int[] count = { 0 };
    box.onChange.add(b -> count[0]++);

    box.insert("ab");
    assertEquals(1, count[0], "onChange must fire immediately after insert");

    box.backspace();
    assertEquals(2, count[0], "onChange must fire immediately after backspace");
  }

  @Test
  void onChange_doesNotFire_whenContentUnchanged() {
    FlixelTextBox box = textBox();
    box.insert("hi");
    int[] count = { 0 };
    box.onChange.add(b -> count[0]++);

    // A second insert of the same text that was already there does not change content.
    box.insert(null); // null is a no-op
    assertEquals(0, count[0]);

    box.update(0f); // update no longer dispatches onChange
    assertEquals(0, count[0]);
  }

  @Test
  void onSubmit_firesOnSubmit() {
    FlixelTextBox box = textBox();
    int[] count = { 0 };
    box.onSubmit.add(b -> count[0]++);

    box.submit();
    assertEquals(1, count[0]);
  }

  @Test
  void setFilter_blocksDisallowedChars() {
    FlixelTextBox box = textBox();
    box.setFilter(FlixelTextFilter.DIGITS);
    box.type('a');
    box.type('5');
    assertEquals("5", box.getText().toString());
  }

  @Test
  void setMaxLength_limitsInput() {
    FlixelTextBox box = textBox();
    box.setMaxLength(3);
    box.insert("abcdef");
    assertEquals("abc", box.getText().toString());
  }

  @Test
  void focus_andBlur_doNotCrash_withoutDisplay() {
    FlixelTextBox box = textBox();
    box.focus();
    assertTrue(box.isFocused());
    box.blur();
    assertFalse(box.isFocused());
  }

  @Test
  void setEnabled_false_blursAndStopsAcceptingInput() {
    FlixelTextBox box = textBox();
    box.focus();
    assertTrue(box.isFocused());
    box.setEnabled(false);
    box.type('x');
    assertEquals("", box.getText().toString());
  }

  @Test
  void selectAll_selectsEntireContent() {
    FlixelTextBox box = textBox();
    box.insert("hello");
    box.selectAll();
    // Overwrite selection with new char.
    box.type('X');
    assertEquals("X", box.getText().toString());
  }

  @Test
  void moveCaret_shiftsCaret() {
    FlixelTextBox box = textBox();
    box.insert("abc");
    box.moveCaret(-1);
    // The caret moved left; backspace should remove 'b', not 'c'.
    box.backspace();
    assertEquals("ac", box.getText().toString());
  }

  @Test
  void keyTyped_typesChar_whenFocused() {
    FlixelTextBox box = textBox();
    box.focus();
    box.keyTyped('x');
    assertEquals("x", box.getText().toString());
  }

  @Test
  void keyTyped_ignored_whenNotFocused() {
    FlixelTextBox box = textBox();
    box.keyTyped('x');
    assertEquals("", box.getText().toString());
  }

  @Test
  void keyDown_backspace_deletesChar() {
    FlixelTextBox box = textBox();
    box.focus();
    box.type('a');
    box.keyDown(FlixelKey.BACKSPACE);
    assertEquals("", box.getText().toString());
  }

  @Test
  void keyDown_enter_singleLine_firesSubmit() {
    FlixelTextBox box = textBox();
    box.focus();
    int[] count = { 0 };
    box.onSubmit.add(b -> count[0]++);
    box.keyDown(FlixelKey.ENTER);
    assertEquals(1, count[0]);
  }

  @Test
  void keyDown_enter_multiLine_insertsNewline() {
    FlixelTextBox box = textBox();
    box.setMultiLine(true);
    box.focus();
    box.type('a');
    box.keyDown(FlixelKey.ENTER);
    box.type('b');
    assertEquals("a\nb", box.getText().toString());
  }

  @Test
  void keyRepeated_backspace_deletesChar() {
    FlixelTextBox box = textBox();
    box.focus();
    box.type('x');
    box.keyRepeated(FlixelKey.BACKSPACE);
    assertEquals("", box.getText().toString());
  }

  @Test
  void passwordChar_doesNotAffectModelText() {
    FlixelTextBox box = textBox();
    box.setPasswordChar('*');
    box.insert("secret");
    assertEquals("secret", box.getText().toString());
  }

  @Test
  void setMultiLine_true_allowsNewlineViaType() {
    FlixelTextBox box = textBox();
    box.setMultiLine(true);
    box.focus();
    box.type('a');
    box.type('\n');
    box.type('b');
    assertEquals("a\nb", box.getText().toString());
  }

  @Test
  void destroy_clearsSignals() {
    FlixelTextBox box = textBox();
    int[] count = { 0 };
    box.onChange.add(b -> count[0]++);
    box.destroy();
    box.type('x');
    assertEquals(0, count[0]);
  }

  @Test
  void placeholder_doesNotAppearInGetText() {
    FlixelTextBox box = textBox();
    box.setPlaceholder("Enter name");
    assertEquals("", box.getText().toString());
  }

  /**
   * Placeholder content must not affect {@link FlixelTextBox#getIndexAt}: the primary text
   * object always holds the model text (empty string here), so the result must be 0 regardless
   * of what the placeholder says.
   */
  @Test
  void placeholder_doesNotAffectGetIndexAt() {
    FlixelTextBox box = textBox();
    box.setPlaceholder("Some long placeholder text");
    // Model is empty; getIndexAt must use the empty-content text, not the placeholder.
    assertEquals(0, box.getIndexAt(0f, 0f));
    assertEquals(0, box.getIndexAt(9999f, 0f));
  }

  @Test
  void cut_clearsSelection_whenEnabled() {
    FlixelTextBox box = textBox();
    box.insert("hello");
    box.selectAll();
    // Cut works regardless of clipboard support (model change still happens).
    box.cut();
    assertEquals("", box.getText().toString());
  }

  @Test
  void cut_doesNothing_whenDisabled() {
    FlixelTextBox box = textBox();
    box.insert("hello");
    box.selectAll();
    box.setEnabled(false);
    box.cut();
    assertEquals("hello", box.getText().toString());
  }

  @Test
  void getScrollX_defaultsToZero() {
    FlixelTextBox box = textBox();
    assertEquals(0f, box.getScrollX(), 0.001f);
  }

  @Test
  void getScrollY_defaultsToZero() {
    FlixelTextBox box = textBox();
    assertEquals(0f, box.getScrollY(), 0.001f);
  }

  @Test
  void scroll_isNoOp_inSingleLineMode() {
    FlixelTextBox box = textBox();
    box.scroll(5f);
    assertEquals(0f, box.getScrollY(), 0.001f);
  }

  @Test
  void scroll_clamps_toZero_whenScrollingUp() {
    FlixelTextBox box = textBox();
    box.setMultiLine(true);
    box.scroll(-100f);
    assertEquals(0f, box.getScrollY(), 0.001f);
  }

  /**
   * Typing many characters into an ATTACHED text box must not throw even though
   * {@link FlixelTextBox#clampScroll()} calls {@link org.flixelgdx.text.FlixelText#getCharX}
   * immediately. The text object is synced before every clamp, so the layout is always
   * consistent with the current caret position.
   */
  @Test
  void type_attached_neverThrows() {
    FlixelTextBox box = new FlixelTextBox(200, 30);
    box.setStyle(style());
    ui.add(box);

    assertDoesNotThrow(() -> {
      for (int i = 0; i < 50; i++) {
        box.type((char) ('a' + i % 26));
      }
    });
    assertEquals(50, box.getText().length());
  }
}
