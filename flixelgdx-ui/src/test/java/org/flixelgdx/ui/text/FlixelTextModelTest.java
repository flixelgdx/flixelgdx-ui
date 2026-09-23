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

import org.flixelgdx.util.FlixelString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link FlixelTextModel}: typing, deletion, navigation, selection, filtering,
 * surrogate pairs, and the version counter.
 *
 * <p>These tests are headless and do not require any framework backend.
 */
class FlixelTextModelTest {

  private FlixelTextModel model;

  @BeforeEach
  void setUp() {
    model = new FlixelTextModel();
  }

  @Test
  void type_appendsCharactersInOrder() {
    model.type('H');
    model.type('i');
    assertEquals("Hi", model.getText().toString());
    assertEquals(2, model.getCaret());
    assertEquals(2, model.getAnchor());
  }

  @Test
  void type_returnsTrue_whenTextChanged() {
    assertTrue(model.type('a'));
  }

  @Test
  void type_returnsFalse_forControlCharBelow32() {
    assertFalse(model.type('\t'));
    assertEquals("", model.getText().toString());
  }

  @Test
  void type_returnsFalse_forSurrogateCodeUnit() {
    assertFalse(model.type('\uD83D')); // high surrogate
    assertEquals("", model.getText().toString());
  }

  @Test
  void type_withFilter_rejectsDisallowedChar() {
    model.setFilter(FlixelTextFilter.DIGITS);
    assertFalse(model.type('a'));
    assertTrue(model.type('5'));
    assertEquals("5", model.getText().toString());
  }

  @Test
  void type_atMaxLength_returnsFalse_whenNoSelection() {
    model.setMaxLength(3);
    model.type('a');
    model.type('b');
    model.type('c');
    assertFalse(model.type('d'));
    assertEquals("abc", model.getText().toString());
  }

  @Test
  void type_replacesSelection() {
    model.type('a');
    model.type('b');
    model.type('c');
    model.select(0, 2);
    assertTrue(model.type('X'));
    assertEquals("Xc", model.getText().toString());
    assertEquals(1, model.getCaret());
  }

  @Test
  void type_newline_inSingleLineMode_isRejected() {
    assertFalse(model.type('\n'));
    assertEquals("", model.getText().toString());
  }

  @Test
  void type_newline_inMultiLineMode_isAccepted() {
    model.setMultiLine(true);
    assertTrue(model.type('a'));
    assertTrue(model.type('\n'));
    assertTrue(model.type('b'));
    assertEquals("a\nb", model.getText().toString());
  }

  @Test
  void backspace_deletesCharBeforeCaret() {
    model.type('a');
    model.type('b');
    assertTrue(model.backspace());
    assertEquals("a", model.getText().toString());
    assertEquals(1, model.getCaret());
  }

  @Test
  void backspace_returnsFalse_atStartOfBuffer() {
    assertFalse(model.backspace());
  }

  @Test
  void backspace_deletesSelection() {
    model.type('a');
    model.type('b');
    model.type('c');
    model.select(0, 3);
    assertTrue(model.backspace());
    assertEquals("", model.getText().toString());
  }

  @Test
  void backspace_deletesEntireSurrogatePair() {
    // U+1F600 (GRINNING FACE) is encoded as two UTF-16 code units.
    String emoji = "😀";
    model.insert(emoji);
    assertEquals(2, model.getCaret()); // two code units
    assertTrue(model.backspace());
    assertEquals("", model.getText().toString());
    assertEquals(0, model.getCaret());
  }

  @Test
  void deleteForward_deletesCharAfterCaret() {
    model.insert("abc");
    model.setCaret(0);
    assertTrue(model.deleteForward());
    assertEquals("bc", model.getText().toString());
    assertEquals(0, model.getCaret());
  }

  @Test
  void deleteForward_returnsFalse_atEndOfBuffer() {
    model.type('a');
    assertFalse(model.deleteForward());
  }

  @Test
  void deleteForward_deletesEntireSurrogatePair() {
    String emoji = "😀";
    model.insert(emoji);
    model.setCaret(0);
    assertTrue(model.deleteForward());
    assertEquals("", model.getText().toString());
  }

  @Test
  void insert_addsMultipleCharactersAtCaret() {
    model.type('!');
    model.setCaret(0);
    model.insert("ab");
    assertEquals("ab!", model.getText().toString());
    assertEquals(2, model.getCaret());
  }

  @Test
  void insert_respectsFilter() {
    model.setFilter(FlixelTextFilter.DIGITS);
    model.insert("a1b2c3");
    assertEquals("123", model.getText().toString());
  }

  @Test
  void insert_stopsAtMaxLength() {
    model.setMaxLength(4);
    model.insert("abcdef");
    assertEquals("abcd", model.getText().toString());
    assertEquals(4, model.getCaret());
  }

  @Test
  void insert_handlesFullSurrogatePair() {
    String emoji = "😀"; // U+1F600
    assertTrue(model.insert(emoji));
    assertEquals(2, model.length());
    assertEquals(2, model.getCaret());
  }

  @Test
  void insert_null_returnsFalse() {
    assertFalse(model.insert(null));
  }

  @Test
  void insert_emptyString_returnsFalse() {
    assertFalse(model.insert(""));
  }

  @Test
  void setText_replacesBuffer_andMovesCaretToEnd() {
    model.type('x');
    model.setText("hello");
    assertEquals("hello", model.getText().toString());
    assertEquals(5, model.getCaret());
    assertEquals(5, model.getAnchor());
  }

  @Test
  void setText_null_clearsBuffer() {
    model.type('x');
    model.setText(null);
    assertEquals("", model.getText().toString());
    assertEquals(0, model.getCaret());
  }

  @Test
  void setText_doesNotFilterCharacters() {
    model.setFilter(FlixelTextFilter.DIGITS);
    model.setText("abc");
    assertEquals("abc", model.getText().toString());
  }

  @Test
  void moveCaret_shiftsByDelta() {
    model.insert("abc");
    model.setCaret(3);
    model.moveCaret(-1, false);
    assertEquals(2, model.getCaret());
    model.moveCaret(1, false);
    assertEquals(3, model.getCaret());
  }

  @Test
  void moveCaret_clampsToBounds() {
    model.insert("ab");
    model.setCaret(0);
    model.moveCaret(-5, false);
    assertEquals(0, model.getCaret());
    model.moveCaret(100, false);
    assertEquals(2, model.getCaret());
  }

  @Test
  void moveCaret_extend_buildsSelection() {
    model.insert("abc");
    model.setCaret(0);
    model.moveCaret(2, true);
    assertEquals(2, model.getCaret());
    assertEquals(0, model.getAnchor());
    assertTrue(model.hasSelection());
    assertEquals(0, model.getSelectionStart());
    assertEquals(2, model.getSelectionEnd());
  }

  @Test
  void moveCaret_collapse_preservesCaretAtNearEnd() {
    model.insert("abc");
    model.select(0, 3);
    // Collapsing with delta=+1: should move to end of selection
    model.moveCaret(1, false);
    assertEquals(3, model.getCaret());
    assertFalse(model.hasSelection());
  }

  @Test
  void moveCaretWord_jumpsToWordBoundary() {
    model.insert("hello world");
    model.setCaret(0);
    model.moveCaretWord(1, false);
    assertEquals(5, model.getCaret());
    model.moveCaretWord(1, false);
    assertEquals(11, model.getCaret());
  }

  @Test
  void moveCaretWord_backward() {
    model.insert("hello world");
    model.setCaret(11);
    model.moveCaretWord(-1, false);
    assertEquals(6, model.getCaret());
    model.moveCaretWord(-1, false);
    assertEquals(0, model.getCaret());
  }

  @Test
  void setCaret_clampsAndSnapsToSafeBoundary() {
    String emoji = "😀";
    model.insert(emoji);
    // Index 1 is the low surrogate: should snap to either 0 or 2.
    model.setCaret(1, false);
    int c = model.getCaret();
    assertTrue(c == 0 || c == 2);
  }

  @Test
  void select_setsRange() {
    model.insert("hello");
    model.select(1, 4);
    assertEquals(4, model.getCaret());
    assertEquals(1, model.getAnchor());
    assertTrue(model.hasSelection());
    assertEquals(1, model.getSelectionStart());
    assertEquals(4, model.getSelectionEnd());
  }

  @Test
  void selectAll_selectsEntireBuffer() {
    model.insert("hi");
    assertTrue(model.selectAll());
    assertEquals(0, model.getSelectionStart());
    assertEquals(2, model.getSelectionEnd());
    assertTrue(model.hasSelection());
  }

  @Test
  void selectAll_returnsFalse_whenEmpty() {
    assertFalse(model.selectAll());
  }

  @Test
  void extendSelection_movesCaretKeepingAnchor() {
    model.insert("abc");
    model.setCaret(1, false);
    model.extendSelection(3);
    assertEquals(3, model.getCaret());
    assertEquals(1, model.getAnchor());
    assertTrue(model.hasSelection());
  }

  @Test
  void deleteSelection_removesSelectedText() {
    model.insert("hello");
    model.select(1, 4);
    assertTrue(model.deleteSelection());
    assertEquals("ho", model.getText().toString());
    assertEquals(1, model.getCaret());
    assertEquals(1, model.getAnchor());
  }

  @Test
  void deleteSelection_returnsFalse_whenNoSelection() {
    model.insert("hi");
    assertFalse(model.deleteSelection());
    assertEquals("hi", model.getText().toString());
  }

  @Test
  void copySelection_writesToOutputBuffer() {
    model.insert("hello");
    model.select(1, 4);
    FlixelString out = new FlixelString();
    model.copySelection(out);
    assertEquals("ell", out.toString());
  }

  @Test
  void copySelection_appendsToExistingContent() {
    model.insert("hello");
    model.select(0, 2);
    FlixelString out = new FlixelString();
    out.insert(0, 'X');
    model.copySelection(out);
    assertEquals("Xhe", out.toString());
  }

  @Test
  void copySelection_doesNothing_whenNoSelection() {
    model.insert("hello");
    FlixelString out = new FlixelString();
    model.copySelection(out);
    assertEquals("", out.toString());
  }

  @Test
  void version_incrementsOnlyOnTextChange() {
    int v0 = model.version;
    model.setCaret(0, false);
    assertEquals(v0, model.version); // caret move does not increment
    model.type('a');
    assertEquals(v0 + 1, model.version);
    model.type('b');
    assertEquals(v0 + 2, model.version);
    model.backspace();
    assertEquals(v0 + 3, model.version);
  }

  @Test
  void version_doesNotIncrement_whenTypeIsRejected() {
    int v0 = model.version;
    model.setFilter(FlixelTextFilter.DIGITS);
    model.type('x');
    assertEquals(v0, model.version);
  }

  @Test
  void setMaxLength_truncatesExistingContent() {
    model.insert("hello");
    model.setMaxLength(3);
    assertEquals("hel", model.getText().toString());
    assertEquals(3, model.getCaret());
  }

  @Test
  void maxLength_zero_meansUnlimited() {
    model.setMaxLength(0);
    for (int i = 0; i < 100; i++) {
      model.type('a');
    }
    assertEquals(100, model.length());
  }

  @Test
  void hasSelection_falseWhenCaretEqualsAnchor() {
    model.insert("abc");
    assertFalse(model.hasSelection());
  }

  @Test
  void hasSelection_trueAfterSelect() {
    model.insert("abc");
    model.select(0, 2);
    assertTrue(model.hasSelection());
  }

  @Test
  void length_matchesBufferLength() {
    model.insert("abc");
    assertEquals(3, model.length());
  }

  @Test
  void multiLine_false_byDefault() {
    assertFalse(model.isMultiLine());
  }

  @Test
  void setMultiLine_true_allowsNewline() {
    model.setMultiLine(true);
    assertTrue(model.type('\n'));
    assertEquals("\n", model.getText().toString());
  }
}
