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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The editing logic for a text box: the text buffer, caret, selection, and mutation operations,
 * with no rendering code.
 *
 * <p>Think of it as the notepad in a typewriter. It knows what characters are on the page and
 * where the print head sits, but it knows nothing about ink, rollers, or display. A
 * {@link FlixelTextBox} owns one model, but the model can also be used and tested entirely
 * standalone.
 *
 * <h2>Selection model</h2>
 *
 * <p>{@link #getCaret()} is the active cursor end. {@link #getAnchor()} is the fixed opposite
 * end set when a selection starts. When {@code caret == anchor} there is no selection. The
 * selection covers the code-unit range {@code [min(caret, anchor), max(caret, anchor))}.
 *
 * <h2>Surrogate pairs</h2>
 *
 * <p>Caret positions are UTF-16 code-unit indices. Surrogate pairs (characters outside the Basic
 * Multilingual Plane) are always treated as single units: every movement and deletion operation
 * steps over an entire pair rather than splitting it.
 *
 * <h2>Version counter</h2>
 *
 * <p>{@link #version} starts at {@code 0} and increments every time the text content changes. A
 * caret-only move does not increment it. Widgets compare the version to avoid rebuilding the
 * layout when nothing changed.
 *
 * <h2>Usage example</h2>
 *
 * <pre>{@code
 * FlixelTextModel model = new FlixelTextModel();
 * model.setMaxLength(16);
 * model.setFilter(FlixelTextFilter.ALPHANUMERIC);
 *
 * model.type('H'); // Inserts at the caret, increments version.
 * model.type('i');
 * model.selectAll();
 * model.type('!'); // Replaces the selection.
 * System.out.println(model.getText()); // "!"
 * }</pre>
 *
 * @see FlixelTextBox
 * @see FlixelTextFilter
 */
public final class FlixelTextModel {

  /** The text content; never {@code null}. Grows on demand; see {@link FlixelString}. */
  @NotNull
  private final FlixelString buffer = new FlixelString();

  /**
   * The active end of the selection (the moving end that the user places by pressing keys or
   * clicking).
   *
   * <p>Always in {@code [0, length()]}, and always at a code-unit boundary that is not the
   * low half of a surrogate pair.
   */
  private int caret;

  /**
   * The fixed (anchor) end of the selection.
   *
   * <p>Equal to {@link #caret} when there is no selection. Together with the caret it defines
   * the selected range {@code [min(caret, anchor), max(caret, anchor))}.
   */
  private int anchor;

  /**
   * The maximum number of code units the buffer may hold.
   *
   * <p>{@code 0} means unlimited. Reducing it below the current length truncates both the text
   * and the caret.
   */
  private int maxLength;

  /** Determines which characters {@link #type(char)} and {@link #insert(CharSequence)} accept. */
  @NotNull
  private FlixelTextFilter filter = FlixelTextFilter.ANY;

  /**
   * Whether the newline character ({@code '\n'}) is allowed.
   *
   * <p>When {@code false}, typing Enter or inserting a newline is silently ignored.
   */
  private boolean multiLine;

  /**
   * Incremented every time the text content changes.
   *
   * <p>A caret-only move does not increment this. Compare it between frames to detect edits
   * without inspecting the full buffer.
   */
  public int version;

  /** Creates an empty model with no limits and the default (accept-all) filter. */
  public FlixelTextModel() {}

  // -------------------------------------------------------------------------
  // Text mutation
  // -------------------------------------------------------------------------

  /**
   * Inserts one character at the caret, replacing any selection first.
   *
   * <p>The character is silently dropped when:
   * <ul>
   *   <li>it is a control character below code point 32 (except {@code '\n'} in multi-line
   *       mode);</li>
   *   <li>it is a surrogate code unit;</li>
   *   <li>the active {@link #getFilter() filter} rejects it;</li>
   *   <li>the {@link #getMaxLength() maximum length} has been reached and there is no
   *       selection to replace.</li>
   * </ul>
   *
   * @param c The character to type.
   * @return {@code true} when the text changed.
   */
  public boolean type(char c) {
    if (!isAllowed(c)) {
      return false;
    }
    deleteSelection();
    if (maxLength > 0 && buffer.length() >= maxLength) {
      return false;
    }
    buffer.insert(caret, c);
    caret++;
    anchor = caret;
    version++;
    return true;
  }

  /**
   * Inserts a sequence of characters at the caret, replacing any selection first, filtering
   * each character individually and stopping early when the maximum length is reached.
   *
   * <p>Characters that fail the filter or that are control characters (below 32, except
   * {@code '\n'} in multi-line mode) or surrogate code units are silently skipped.
   *
   * @param text The text to insert. Does nothing when {@code null} or empty.
   * @return {@code true} when the text changed.
   */
  public boolean insert(@Nullable CharSequence text) {
    if (text == null || text.length() == 0) {
      return false;
    }
    boolean changed = deleteSelection();
    int len = text.length();
    for (int i = 0; i < len; i++) {
      char c = text.charAt(i);
      if (Character.isHighSurrogate(c) && i + 1 < len && Character.isLowSurrogate(text.charAt(i + 1))) {
        // Surrogate pair: insert both code units together. isAllowed() does not apply here because
        // surrogates are not BMP characters and would otherwise be rejected.
        if (maxLength > 0 && buffer.length() + 1 >= maxLength) {
          break;
        }
        buffer.insert(caret, c);
        caret++;
        buffer.insert(caret, text.charAt(i + 1));
        caret++;
        i++; // Skip the low surrogate on the next iteration.
        changed = true;
      } else if (Character.isLowSurrogate(c)) {
        // Lone low surrogate with no preceding high surrogate: discard.
        continue;
      } else {
        if (!isAllowed(c)) {
          continue;
        }
        if (maxLength > 0 && buffer.length() >= maxLength) {
          break;
        }
        buffer.insert(caret, c);
        caret++;
        changed = true;
      }
    }
    anchor = caret;
    if (changed) {
      version++;
    }
    return changed;
  }

  /**
   * Deletes the character immediately before the caret, or the whole selection if one exists.
   *
   * <p>Treats a surrogate pair as one unit: when the caret is just after a low surrogate whose
   * predecessor is a high surrogate, both code units are deleted together.
   *
   * @return {@code true} when the text changed.
   */
  public boolean backspace() {
    if (hasSelection()) {
      return deleteSelection();
    }
    if (caret == 0) {
      return false;
    }
    int del = deleteUnitBefore(caret);
    buffer.delete(caret - del, caret);
    caret -= del;
    anchor = caret;
    version++;
    return true;
  }

  /**
   * Deletes the character immediately after the caret, or the whole selection if one exists.
   *
   * <p>Treats a surrogate pair as one unit: when the caret is just before a high surrogate
   * whose successor is a low surrogate, both code units are deleted together.
   *
   * @return {@code true} when the text changed.
   */
  public boolean deleteForward() {
    if (hasSelection()) {
      return deleteSelection();
    }
    int len = buffer.length();
    if (caret >= len) {
      return false;
    }
    int del = deleteUnitAfter(caret);
    buffer.delete(caret, caret + del);
    version++;
    return true;
  }

  /**
   * Deletes the selected range and moves the caret to the selection start.
   *
   * <p>Does nothing when there is no selection.
   *
   * @return {@code true} when text was deleted.
   */
  public boolean deleteSelection() {
    if (!hasSelection()) {
      return false;
    }
    int start = getSelectionStart();
    int end = getSelectionEnd();
    buffer.delete(start, end);
    caret = start;
    anchor = start;
    version++;
    return true;
  }

  /**
   * Sets the complete text content, resets the caret to the end, and clears any selection.
   *
   * <p>If the text exceeds the maximum length it is truncated at the limit.
   * Control characters other than {@code '\n'} (in multi-line mode) and surrogate code units
   * are accepted as-is because this is a programmatic setter, not user input.
   *
   * @param text The text to set. {@code null} counts as empty.
   */
  public void setText(@Nullable CharSequence text) {
    buffer.clear();
    if (text != null && text.length() > 0) {
      int limit = (maxLength > 0) ? Math.min(text.length(), maxLength) : text.length();
      buffer.set(text.length() > limit ? text.subSequence(0, limit) : text);
    }
    caret = buffer.length();
    anchor = caret;
    version++;
  }

  // -------------------------------------------------------------------------
  // Caret and selection
  // -------------------------------------------------------------------------

  /**
   * Moves the caret by {@code delta} code units, stepping over surrogate pairs, and optionally
   * extends the selection.
   *
   * <p>When there is a selection and {@code extend} is {@code false}, the caret collapses to the
   * near end of the selection (left for negative delta, right for positive) without moving
   * further. When {@code extend} is {@code true} the anchor stays fixed and the caret alone
   * moves.
   *
   * @param delta The number of positions to move; negative moves left, positive moves right.
   * @param extend {@code true} to extend the selection, {@code false} to collapse it first.
   * @return {@code true} when the caret position changed.
   */
  public boolean moveCaret(int delta, boolean extend) {
    if (!extend && hasSelection()) {
      int newCaret = delta < 0 ? getSelectionStart() : getSelectionEnd();
      if (newCaret != caret || anchor != caret) {
        caret = newCaret;
        anchor = newCaret;
        return true;
      }
    }
    int target = caret;
    if (delta > 0) {
      for (int i = 0; i < delta; i++) {
        int step = moveUnitForward(target);
        if (step == 0) {
          break;
        }
        target += step;
      }
    } else {
      for (int i = 0; i > delta; i--) {
        int step = moveUnitBackward(target);
        if (step == 0) {
          break;
        }
        target -= step;
      }
    }
    if (!extend) {
      anchor = target;
    }
    boolean changed = (caret != target) || (!extend && anchor != caret);
    caret = target;
    if (!extend) {
      anchor = caret;
    }
    return changed;
  }

  /**
   * Moves the caret by one position, collapsing any selection to the near end.
   *
   * @param delta The direction; negative for left, positive for right.
   * @return {@code true} when the caret position changed.
   */
  public boolean moveCaret(int delta) {
    return moveCaret(delta, false);
  }

  /**
   * Jumps the caret by one word in the given direction, optionally extending the selection.
   *
   * <p>A word consists of consecutive characters that are letters, digits, or underscores.
   * Moving right: skip whitespace and punctuation to reach a word start, then consume the whole
   * word. Moving left: skip the word the caret is at the end of, then skip any preceding
   * whitespace.
   *
   * @param direction Negative to jump left, positive to jump right.
   * @param extend {@code true} to extend the selection rather than collapsing it.
   * @return {@code true} when the caret moved.
   */
  public boolean moveCaretWord(int direction, boolean extend) {
    int target;
    if (direction >= 0) {
      target = wordRight(caret);
    } else {
      target = wordLeft(caret);
    }
    if (!extend) {
      anchor = target;
    }
    boolean changed = caret != target;
    caret = target;
    return changed;
  }

  /**
   * Places the caret at a specific index, optionally extending the selection.
   *
   * <p>The index is clamped to {@code [0, length()]} and then snapped to a code-unit boundary
   * that is not the low half of a surrogate pair.
   *
   * @param index The target code-unit index.
   * @param extend {@code true} to keep the anchor and only move the caret.
   * @return {@code true} when the caret or anchor changed.
   */
  public boolean setCaret(int index, boolean extend) {
    int safe = safeIndex(clamp(index, 0, buffer.length()));
    boolean changed = caret != safe || (!extend && anchor != safe);
    caret = safe;
    if (!extend) {
      anchor = safe;
    }
    return changed;
  }

  /**
   * Places the caret at a specific index without extending the selection.
   *
   * @param index The target index.
   * @return {@code true} when the caret position changed.
   */
  public boolean setCaret(int index) {
    return setCaret(index, false);
  }

  /**
   * Sets both caret and anchor to define a selection range.
   *
   * <p>Both bounds are clamped and snapped to safe code-unit boundaries. After the call the
   * selection covers {@code [start, end)}, with the caret at {@code end}.
   *
   * @param start The start of the selection (inclusive).
   * @param end The end of the selection (exclusive); becomes the new caret position.
   */
  public void select(int start, int end) {
    anchor = safeIndex(clamp(start, 0, buffer.length()));
    caret = safeIndex(clamp(end, 0, buffer.length()));
  }

  /**
   * Selects all text: places the anchor at {@code 0} and the caret at {@link #length()}.
   *
   * @return {@code true} when the selection changed.
   */
  public boolean selectAll() {
    int len = buffer.length();
    boolean changed = caret != len || anchor != 0;
    anchor = 0;
    caret = len;
    return changed;
  }

  /**
   * Extends the selection by moving the caret to the given index without changing the anchor.
   *
   * @param index The new caret position.
   * @return {@code true} when the caret moved.
   */
  public boolean extendSelection(int index) {
    return setCaret(index, true);
  }

  /**
   * Copies the selected text into {@code out} without allocating.
   *
   * <p>If there is no selection, {@code out} is cleared.
   *
   * @param out The buffer to fill with the selection.
   */
  public void copySelection(@NotNull FlixelString out) {
    if (!hasSelection()) {
      out.clear();
      return;
    }
    int start = getSelectionStart();
    int end = getSelectionEnd();
    for (int i = start; i < end; i++) {
      // Append to existing content: insert at end is O(1) amortized, no allocation.
      out.insert(out.length(), buffer.charAt(i));
    }
  }

  // -------------------------------------------------------------------------
  // Configuration
  // -------------------------------------------------------------------------

  /**
   * Sets the maximum number of code units the buffer may hold.
   *
   * <p>{@code 0} means unlimited. When the new limit is smaller than the current text length,
   * the text is truncated and the caret is clamped inside the new limit.
   *
   * @param max The new maximum, or {@code 0} for no limit.
   */
  public void setMaxLength(int max) {
    maxLength = max < 0 ? 0 : max;
    if (maxLength > 0 && buffer.length() > maxLength) {
      buffer.setLength(maxLength);
      caret = safeIndex(clamp(caret, 0, maxLength));
      anchor = safeIndex(clamp(anchor, 0, maxLength));
      version++;
    }
  }

  /**
   * Sets the filter that gates each character typed or pasted into this model.
   *
   * @param filter The new filter; {@code null} reverts to accepting all characters.
   */
  public void setFilter(@Nullable FlixelTextFilter filter) {
    this.filter = filter != null ? filter : FlixelTextFilter.ANY;
  }

  /**
   * Allows or disallows newline characters in the text.
   *
   * <p>When {@code false} (the default), attempting to type or insert a newline is silently
   * ignored. When {@code true}, the newline passes all the other checks and is inserted normally.
   *
   * @param multiLine {@code true} to allow newlines.
   */
  public void setMultiLine(boolean multiLine) {
    this.multiLine = multiLine;
  }

  // -------------------------------------------------------------------------
  // Queries
  // -------------------------------------------------------------------------

  /**
   * Returns the live text buffer as a {@link CharSequence}.
   *
   * <p>The returned sequence reflects every subsequent edit. Do not hold a reference across
   * frames if the model might be mutated by another path.
   *
   * @return The live buffer; never {@code null}.
   */
  @NotNull
  public CharSequence getText() {
    return buffer;
  }

  /**
   * Returns the number of code units currently in the buffer.
   *
   * @return The text length.
   */
  public int length() {
    return buffer.length();
  }

  /**
   * Returns the current caret position (active end of the selection).
   *
   * @return A code-unit index in {@code [0, length()]}.
   */
  public int getCaret() {
    return caret;
  }

  /**
   * Returns the anchor end of the selection.
   *
   * <p>Equal to the caret when there is no selection.
   *
   * @return A code-unit index in {@code [0, length()]}.
   */
  public int getAnchor() {
    return anchor;
  }

  /**
   * Returns the start (lower index) of the selected range.
   *
   * @return The lower of the caret and anchor.
   */
  public int getSelectionStart() {
    return Math.min(caret, anchor);
  }

  /**
   * Returns the end (higher index, exclusive) of the selected range.
   *
   * @return The higher of the caret and anchor.
   */
  public int getSelectionEnd() {
    return Math.max(caret, anchor);
  }

  /**
   * Returns whether there is a non-empty selection.
   *
   * @return {@code true} when the caret and anchor differ.
   */
  public boolean hasSelection() {
    return caret != anchor;
  }

  /**
   * Returns the maximum number of code units allowed, or {@code 0} for no limit.
   *
   * @return The max length.
   */
  public int getMaxLength() {
    return maxLength;
  }

  /**
   * Returns the active filter.
   *
   * @return The filter; never {@code null}.
   */
  @NotNull
  public FlixelTextFilter getFilter() {
    return filter;
  }

  /**
   * Returns whether newlines are allowed.
   *
   * @return {@code true} if multi-line mode is enabled.
   */
  public boolean isMultiLine() {
    return multiLine;
  }

  // -------------------------------------------------------------------------
  // Internal helpers
  // -------------------------------------------------------------------------

  /**
   * Returns whether {@code c} passes all checks for user input: not a bare control character
   * (unless it is {@code '\n'} and multi-line is on), not a surrogate code unit, and accepted
   * by the filter.
   */
  private boolean isAllowed(char c) {
    if (Character.isSurrogate(c)) {
      return false;
    }
    if (c < 32) {
      return c == '\n' && multiLine;
    }
    return filter.accept(c);
  }

  /**
   * Returns the number of code units to delete when backspacing before {@code pos}. Returns
   * {@code 2} when code units at {@code pos - 2} and {@code pos - 1} form a surrogate pair,
   * otherwise {@code 1}. Returns {@code 0} when {@code pos == 0}.
   */
  private int deleteUnitBefore(int pos) {
    if (pos < 2) {
      return pos;
    }
    char lo = buffer.charAt(pos - 1);
    char hi = buffer.charAt(pos - 2);
    return Character.isSurrogatePair(hi, lo) ? 2 : 1;
  }

  /**
   * Returns the number of code units to delete when deleting forward from {@code pos}. Returns
   * {@code 2} when the code units at {@code pos} and {@code pos + 1} form a surrogate pair,
   * otherwise {@code 1}. Returns {@code 0} when {@code pos >= length}.
   */
  private int deleteUnitAfter(int pos) {
    int len = buffer.length();
    if (pos >= len) {
      return 0;
    }
    if (pos + 1 < len) {
      char hi = buffer.charAt(pos);
      char lo = buffer.charAt(pos + 1);
      if (Character.isSurrogatePair(hi, lo)) {
        return 2;
      }
    }
    return 1;
  }

  /**
   * Returns the number of code units to step when moving right from {@code pos}, accounting for
   * surrogate pairs. Returns {@code 0} when at the end.
   */
  private int moveUnitForward(int pos) {
    int len = buffer.length();
    if (pos >= len) {
      return 0;
    }
    if (pos + 1 < len) {
      char hi = buffer.charAt(pos);
      char lo = buffer.charAt(pos + 1);
      if (Character.isSurrogatePair(hi, lo)) {
        return 2;
      }
    }
    return 1;
  }

  /**
   * Returns the number of code units to step when moving left from {@code pos}, accounting for
   * surrogate pairs. Returns {@code 0} when at the start.
   */
  private int moveUnitBackward(int pos) {
    if (pos < 2) {
      return pos;
    }
    char lo = buffer.charAt(pos - 1);
    char hi = buffer.charAt(pos - 2);
    return Character.isSurrogatePair(hi, lo) ? 2 : 1;
  }

  /**
   * Returns the rightmost word-jump target from {@code from}: skips whitespace/punctuation to
   * find a word start, then consumes the whole word.
   */
  private int wordRight(int from) {
    int len = buffer.length();
    int pos = from;
    // Skip non-word characters.
    while (pos < len && !isWordChar(buffer.charAt(pos))) {
      pos += moveUnitForward(pos);
    }
    // Consume the word.
    while (pos < len && isWordChar(buffer.charAt(pos))) {
      pos += moveUnitForward(pos);
    }
    return pos;
  }

  /**
   * Returns the leftmost word-jump target from {@code from}: steps back through the current
   * word (if any), then through any preceding whitespace/punctuation.
   */
  private int wordLeft(int from) {
    int pos = from;
    if (pos == 0) {
      return 0;
    }
    int back = moveUnitBackward(pos);
    char first = buffer.charAt(pos - back);
    if (isWordChar(first)) {
      // Cursor is inside or just after a word: back up to the word's start.
      while (pos > 0) {
        back = moveUnitBackward(pos);
        if (!isWordChar(buffer.charAt(pos - back))) {
          break;
        }
        pos -= back;
      }
    } else {
      // Cursor is after non-word characters: skip them, then back up through the preceding word.
      while (pos > 0) {
        back = moveUnitBackward(pos);
        if (isWordChar(buffer.charAt(pos - back))) {
          break;
        }
        pos -= back;
      }
      while (pos > 0) {
        back = moveUnitBackward(pos);
        if (!isWordChar(buffer.charAt(pos - back))) {
          break;
        }
        pos -= back;
      }
    }
    return pos;
  }

  /** Returns {@code true} for characters that form a word: letters, digits, or underscore. */
  private static boolean isWordChar(char c) {
    return Character.isLetterOrDigit(c) || c == '_';
  }

  /**
   * Snaps {@code idx} forward if it lands inside the low half of a surrogate pair: the returned
   * index is always the high half or a non-surrogate.
   */
  private int safeIndex(int idx) {
    int len = buffer.length();
    if (idx > 0 && idx < len) {
      char c = buffer.charAt(idx);
      if (Character.isLowSurrogate(c)) {
        char prev = buffer.charAt(idx - 1);
        if (Character.isHighSurrogate(prev)) {
          return idx + 1;
        }
      }
    }
    return idx;
  }

  private static int clamp(int val, int min, int max) {
    return val < min ? min : (val > max ? max : val);
  }
}
