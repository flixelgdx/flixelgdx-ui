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
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelViewport;
import org.flixelgdx.input.FlixelInputDevice;
import org.flixelgdx.input.FlixelKeyboardListener;
import org.flixelgdx.input.keyboard.FlixelKey;
import org.flixelgdx.math.FlixelRect;
import org.flixelgdx.text.FlixelText;
import org.flixelgdx.ui.FlixelUiDisplay;
import org.flixelgdx.ui.FlixelUiWidget;
import org.flixelgdx.ui.graphics.FlixelUiBackground;
import org.flixelgdx.ui.skin.FlixelTextBoxStyle;
import org.flixelgdx.util.FlixelColor;
import org.flixelgdx.util.FlixelString;
import org.flixelgdx.util.signal.FlixelSignal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A single-line or multi-line text-entry widget.
 *
 * <p>Think of a text box as the classic typewriter: you focus it to start typing, and the caret
 * shows where the next character goes. The box listens to the keyboard on its own, but it only
 * acts on keys while it is focused, and only the game decides when it is focused, so the game
 * stays in charge of which box (if any) receives the player's typing.
 *
 * <h2>Basic setup</h2>
 *
 * <pre>{@code
 * // Player name box (single-line, alphanumeric, max 16 characters).
 * FlixelTextBox name = new FlixelTextBox(220);
 * name.setPlaceholder("Player name");
 * name.setMaxLength(16);
 * name.setFilter(FlixelTextFilter.ALPHANUMERIC);
 * name.onSubmit.add(t -> save.playerName = t.getText().toString());
 *
 * // Multi-line notes box.
 * FlixelTextBox notes = new FlixelTextBox(300, 120);
 * notes.setMultiLine(true);
 *
 * // PIN box with a password mask.
 * FlixelTextBox pin = new FlixelTextBox(120);
 * pin.setPasswordChar('*');
 * pin.setFilter(FlixelTextFilter.DIGITS);
 * pin.setMaxLength(6);
 * }</pre>
 *
 * <h2>Wiring input</h2>
 *
 * <p>Focus and blur control when the OS delivers typed characters. Call {@link #focus()} to start
 * text input (the OS may show a virtual keyboard) and {@link #blur()} to stop it. The game uses
 * its own pointer logic to decide which box is active:
 *
 * <pre>{@code
 * if (Flixel.mouse.justPressed(FlixelMouseButton.LEFT)) {
 *   FlixelUiWidget hit = ui.getWidgetAt(mx, my);
 *   if (hit == name) {
 *     if (!name.isFocused()) name.focus();
 *     name.setCaret(name.getIndexAt(mx, my));
 *   } else if (name.isFocused()) {
 *     name.blur();
 *   }
 * }
 * }</pre>
 *
 * <h2>Keyboard listener</h2>
 *
 * <p>{@link FlixelTextBox} implements {@link FlixelKeyboardListener} and registers itself with
 * {@link Flixel#input} when it is constructed, then removes itself in {@link #destroy()}. There is
 * nothing to register by hand. Every key event reaches every text box, but an unfocused or
 * disabled box ignores it, so only the focused box types, edits, and moves its caret.
 *
 * <p>Because of that registration, destroy a text box when you are done with it. Destroying its
 * display (which a state does when it is destroyed) or any container it is in destroys it too.
 * Create text boxes once the game is running (for example in a state's {@code create()}), since
 * the box registers with whichever input device is active at that moment.
 *
 * @see FlixelTextModel
 * @see FlixelTextFilter
 * @see FlixelTextBoxStyle
 */
public class FlixelTextBox extends FlixelUiWidget implements FlixelKeyboardListener {

  /** Dispatched whenever the text content changes. */
  public final FlixelSignal<FlixelTextBox> onChange = new FlixelSignal<>();

  /** Dispatched by {@link #submit()} or when Enter is pressed in a single-line box. */
  public final FlixelSignal<FlixelTextBox> onSubmit = new FlixelSignal<>();

  private final FlixelTextModel model = new FlixelTextModel();

  /**
   * The primary text object; always holds the model text (or its password mask).
   *
   * <p>All caret, selection, scroll, and {@link #getIndexAt} math use this object exclusively.
   * It is never overwritten with placeholder content.
   */
  private final FlixelText text = new FlixelText();

  /**
   * A separate text object used only when the placeholder is shown.
   *
   * <p>Keeping placeholder content here prevents {@link FlixelText#getCharX} and
   * {@link FlixelText#getIndexAt} from measuring the placeholder string instead of the
   * (empty) model text when the buffer is empty.
   */
  private final FlixelText placeholderText = new FlixelText();

  /**
   * One-element camera list handed to both text objects. Slot 0 is filled on
   * {@link #onAttached(FlixelUiDisplay)} and cleared on {@link #onDetached()}.
   */
  private final FlixelCamera[] textCameras = new FlixelCamera[1];

  /** Per-character password mask; only used when {@link #passwordChar} is non-zero. */
  private final FlixelString maskBuffer = new FlixelString();

  /** Scratch buffer for copy and cut; avoids allocating a {@code String} per action. */
  private final FlixelString copyBuffer = new FlixelString();

  /**
   * Scratch rectangle for text-input area calculations so
   * {@link #updateTextInputArea()} never allocates.
   */
  private final FlixelRect tiScratch = new FlixelRect();

  /**
   * Permanent subscription to the paste signal.
   *
   * <p>Stored as a field so it can be removed precisely in {@link #destroy()}.
   */
  private final FlixelSignal.SignalHandler<String> pasteHandler = this::onPasted;

  @Nullable
  private FlixelString placeholder;

  @Nullable
  private FlixelTextBoxStyle style;

  private int lastVersion = -1;

  /**
   * Last X sent to {@link FlixelInputDevice#setTextInputArea}.
   *
   * <p>Set to {@link Integer#MIN_VALUE} initially so the first real value is always sent.
   */
  private int lastTiX = Integer.MIN_VALUE;

  private int lastTiY;
  private int lastTiW;
  private int lastTiH;

  private float scrollX;
  private float scrollY;
  private float blinkTimer;

  private char passwordChar;

  private boolean customStyle;

  /**
   * {@code true} while a matching {@link FlixelInputDevice#startTextInput()}
   * is outstanding, so the paired {@code stopTextInput()} is called exactly once.
   */
  private boolean textInputStarted;

  /**
   * Creates a single-line text box whose height is computed from the style's font and padding.
   *
   * @param width The fixed width in pixels.
   */
  public FlixelTextBox(float width) {
    super(width, 0f);
    initText();
    Flixel.host.onTextPasted().add(pasteHandler);
    Flixel.input.addKeyboardListener(this);
  }

  /**
   * Creates a text box with an explicit size.
   *
   * <p>Passing a height greater than the natural single-line height enables multi-line scrolling.
   * Call {@link #setMultiLine(boolean) setMultiLine(true)} if you also want {@code Enter} to
   * insert a newline rather than submit.
   *
   * @param width The width in pixels.
   * @param height The height in pixels.
   */
  public FlixelTextBox(float width, float height) {
    super(width, height);
    initText();
    Flixel.host.onTextPasted().add(pasteHandler);
    Flixel.input.addKeyboardListener(this);
  }

  private void initText() {
    text.setScrollFactor(0f, 0f);
    text.cameras = textCameras;
    text.setAutoSize(true);
    text.setWordWrap(false);
    text.setText("");

    placeholderText.setScrollFactor(0f, 0f);
    placeholderText.cameras = textCameras;
    placeholderText.setAutoSize(true);
    placeholderText.setWordWrap(false);
    placeholderText.setText("");
  }

  @Override
  protected void onAttached(@NotNull FlixelUiDisplay attachedDisplay) {
    textCameras[0] = attachedDisplay.getCamera();
  }

  @Override
  protected void onDetached() {
    textCameras[0] = null;
    stopTextInputIfStarted();
  }

  @Override
  protected void onStyleChanged() {
    if (!customStyle) {
      style = resolveStyle(FlixelTextBoxStyle.class);
    }
    applyStyleToText();
    invalidateLayout();
  }

  @Override
  protected void onMeasure() {
    FlixelTextBoxStyle s = style;
    if (s == null || height > 0f) {
      return;
    }
    float lineH = Math.max(1, s.fontSize);
    FlixelUiBackground bg = resolveBackground(s);
    float minH = bg != null ? bg.getMinHeight() : 0f;
    height = Math.max(minH, s.padTop + lineH + s.padBottom);
  }

  @Override
  protected void onLayout() {
    FlixelTextBoxStyle s = style;
    if (s == null) {
      return;
    }
    float contentW = Math.max(0f, width - s.padLeft - s.padRight);
    if (model.isMultiLine()) {
      text.setFieldWidth(contentW);
      text.setWordWrap(true);
      placeholderText.setFieldWidth(contentW);
      placeholderText.setWordWrap(true);
    } else {
      text.setFieldWidth(0f);
      text.setWordWrap(false);
      placeholderText.setFieldWidth(0f);
      placeholderText.setWordWrap(false);
    }
    if (isFocused()) {
      updateTextInputArea();
    }
  }

  @Override
  public void update(float elapsed) {
    FlixelTextBoxStyle s = style;
    if (s != null && s.caretBlinkRate > 0f && isFocused()) {
      blinkTimer += elapsed;
    }
  }

  @Override
  protected void drawSelf(@NotNull FlixelBatch batch, float drawX, float drawY, float alpha) {
    FlixelTextBoxStyle s = style;
    FlixelColor tint = getColor();

    FlixelUiBackground bg = resolveBackground(s);
    if (bg != null) {
      batch.setColor(tint.r, tint.g, tint.b, alpha);
      bg.draw(batch, drawX, drawY, width, height);
      batch.setColor(FlixelColor.WHITE);
    }

    if (s == null) {
      return;
    }

    float padL = s.padLeft;
    float padT = s.padTop;
    float padR = s.padRight;
    float padB = s.padBottom;
    float contentW = Math.max(0f, width - padL - padR);
    float contentH = Math.max(0f, height - padT - padB);

    boolean visible = pushClip(batch, screenX + padL, screenY + padT, contentW, contentH);
    try {
      if (!visible) {
        return;
      }

      boolean showPlaceholder = model.length() == 0;

      float lineH = text.getLineHeight();
      if (lineH <= 0f) {
        lineH = Math.max(1, s.fontSize);
      }

      float textScreenX;
      float textScreenY;
      if (model.isMultiLine()) {
        textScreenX = screenX + padL;
        textScreenY = screenY + padT - scrollY;
      } else {
        textScreenX = screenX + padL - scrollX;
        textScreenY = screenY + padT + (contentH - lineH) * 0.5f;
      }

      if (!showPlaceholder && isFocused() && model.hasSelection() && s.selection != null) {
        drawSelection(batch, s.selection, tint, alpha, textScreenX, textScreenY);
      }

      if (showPlaceholder) {
        // Draw the placeholder using the separate placeholderText object so the primary
        // text object keeps its empty layout -- getIndexAt(x,y) always returns 0 correctly.
        FlixelColor phColor = s.placeholderFontColor != null ? s.placeholderFontColor
            : (s.fontColor != null ? s.fontColor : FlixelColor.WHITE);
        drawText(batch, placeholderText, textScreenX, textScreenY, phColor, tint, alpha);
      } else {
        FlixelColor nc;
        if (!isEnabled()) {
          nc = s.disabledFontColor != null ? s.disabledFontColor
              : (s.fontColor != null ? s.fontColor : FlixelColor.WHITE);
        } else {
          nc = s.fontColor != null ? s.fontColor : FlixelColor.WHITE;
        }
        drawText(batch, text, textScreenX, textScreenY, nc, tint, alpha);
      }

      if (isFocused() && s.caret != null && isCaretVisible(s)) {
        drawCaret(batch, s, tint, alpha, textScreenX, textScreenY, lineH);
      }
    } finally {
      popClip(batch);
    }
  }

  @Override
  public void destroy() {
    Flixel.host.onTextPasted().remove(pasteHandler);
    Flixel.input.removeKeyboardListener(this);
    stopTextInputIfStarted();
    text.destroy();
    placeholderText.destroy();
    onChange.clear();
    onSubmit.clear();
    super.destroy();
  }

  @Override
  public void focus() {
    boolean wasFocused = isFocused();
    super.focus();
    if (!wasFocused && isFocused() && display != null) {
      startTextInputOnce();
      invalidateTiCache();
      updateTextInputArea();
    }
  }

  @Override
  public void blur() {
    boolean wasFocused = isFocused();
    super.blur();
    if (wasFocused) {
      stopTextInputIfStarted();
    }
  }

  @Override
  public void setEnabled(boolean enabled) {
    if (!enabled && isFocused()) {
      stopTextInputIfStarted();
    }
    super.setEnabled(enabled);
  }

  /** Takes focus and moves the caret to the clicked character. */
  @Override
  protected void onActivate(float x, float y) {
    focus();
    setCaret(getIndexAt(x, y));
  }

  /** Scrolls a multi-line box by {@code amount} lines; a single-line box passes the scroll on. */
  @Override
  protected boolean onScroll(float amount) {
    if (!model.isMultiLine()) {
      return false;
    }
    scroll(amount);
    return true;
  }

  /**
   * Inserts one character at the caret, replacing the selection if one exists.
   *
   * @param c The character to type.
   * @return {@code true} when the text changed.
   */
  public boolean type(char c) {
    if (!isEnabled()) {
      return false;
    }
    boolean changed = model.type(c);
    afterEdit();
    return changed;
  }

  /**
   * Inserts a sequence of characters at the caret.
   *
   * @param insertText The text to insert; {@code null} is ignored.
   * @return {@code true} when the text changed.
   */
  public boolean insert(@Nullable CharSequence insertText) {
    if (!isEnabled()) {
      return false;
    }
    boolean changed = model.insert(insertText);
    afterEdit();
    return changed;
  }

  /**
   * Deletes the character before the caret, or the selection if one exists.
   *
   * @return {@code true} when the text changed.
   */
  public boolean backspace() {
    if (!isEnabled()) {
      return false;
    }
    boolean changed = model.backspace();
    afterEdit();
    return changed;
  }

  /**
   * Deletes the character after the caret, or the selection if one exists.
   *
   * @return {@code true} when the text changed.
   */
  public boolean deleteForward() {
    if (!isEnabled()) {
      return false;
    }
    boolean changed = model.deleteForward();
    afterEdit();
    return changed;
  }

  /**
   * Deletes from the caret to the previous or next word boundary, or the selection if one exists.
   *
   * <p>The built-in keyboard handling calls this for Ctrl+Backspace and Ctrl+Delete. A game that
   * drives the box itself (for example from a gamepad on-screen keyboard) can call it directly.
   *
   * @param direction Negative to delete toward the start, positive to delete toward the end.
   * @return {@code true} when the text changed.
   */
  public boolean deleteWord(int direction) {
    if (!isEnabled()) {
      return false;
    }
    boolean changed = model.deleteWord(direction);
    afterEdit();
    return changed;
  }

  /**
   * Moves the caret by the given number of positions, collapsing any selection.
   *
   * @param delta Negative for left, positive for right.
   */
  public void moveCaret(int delta) {
    if (!isEnabled()) {
      return;
    }
    if (model.moveCaret(delta, false)) {
      afterCaretMove();
    }
  }

  /**
   * Moves the caret by the given number of positions, optionally extending the selection.
   *
   * @param delta Negative for left, positive for right.
   * @param extend {@code true} to extend the selection, {@code false} to collapse it.
   */
  public void moveCaret(int delta, boolean extend) {
    if (!isEnabled()) {
      return;
    }
    if (model.moveCaret(delta, extend)) {
      afterCaretMove();
    }
  }

  /**
   * Jumps the caret to the next or previous word boundary.
   *
   * @param direction Negative for left, positive for right.
   * @param extend {@code true} to extend the selection, {@code false} to collapse it.
   */
  public void moveCaretWord(int direction, boolean extend) {
    if (!isEnabled()) {
      return;
    }
    if (model.moveCaretWord(direction, extend)) {
      afterCaretMove();
    }
  }

  /**
   * Moves the caret up or down by the given number of lines (multi-line only).
   *
   * <p>The caret's horizontal pixel position is preserved using
   * {@link FlixelText#getIndexAt(float, float)}.
   *
   * @param lines The number of lines to move; negative moves up, positive moves down.
   * @param extend {@code true} to extend the selection, {@code false} to collapse it.
   */
  public void moveCaretLine(int lines, boolean extend) {
    if (!isEnabled() || !model.isMultiLine()) {
      return;
    }
    int caret = model.getCaret();
    int curLine = text.getCharLine(caret);
    float caretXLocal = text.getCharX(caret);
    int targetLine = clamp(curLine + lines, 0, text.getLineCount() - 1);
    if (targetLine == curLine) {
      return;
    }
    float targetY = text.getLineTop(targetLine) + text.getLineHeight() * 0.5f;
    int targetIdx = text.getIndexAt(caretXLocal, targetY);
    if (model.setCaret(targetIdx, extend)) {
      afterCaretMove();
    }
  }

  /**
   * Moves the caret to the start of the current line.
   *
   * @param extend {@code true} to extend the selection, {@code false} to collapse it.
   */
  public void caretHome(boolean extend) {
    if (!isEnabled()) {
      return;
    }
    int line = text.getCharLine(model.getCaret());
    int target = text.getLineStart(line);
    if (model.setCaret(target, extend)) {
      afterCaretMove();
    }
  }

  /**
   * Moves the caret to the end of the current line.
   *
   * @param extend {@code true} to extend the selection, {@code false} to collapse it.
   */
  public void caretEnd(boolean extend) {
    if (!isEnabled()) {
      return;
    }
    int line = text.getCharLine(model.getCaret());
    int target = text.getLineEnd(line);
    if (model.setCaret(target, extend)) {
      afterCaretMove();
    }
  }

  /**
   * Places the caret at the given index, collapsing any selection.
   *
   * @param index The target code-unit index.
   */
  public void setCaret(int index) {
    if (model.setCaret(index, false)) {
      afterCaretMove();
    }
  }

  /**
   * Selects the range {@code [start, end)}.
   *
   * @param start The start index (inclusive).
   * @param end The end index (exclusive); becomes the caret position.
   */
  public void select(int start, int end) {
    model.select(start, end);
    afterCaretMove();
  }

  /** Selects all text. */
  public void selectAll() {
    if (model.selectAll()) {
      afterCaretMove();
    }
  }

  /**
   * Extends the selection to the given index.
   *
   * @param index The new caret position.
   */
  public void extendSelection(int index) {
    if (model.extendSelection(index)) {
      afterCaretMove();
    }
  }

  /**
   * Copies the selected text to the system clipboard.
   *
   * <p>Does nothing when there is no selection or the platform does not support the clipboard.
   */
  public void copy() {
    if (!model.hasSelection() || !Flixel.host.supportsClipboard()) {
      return;
    }
    copyBuffer.clear();
    model.copySelection(copyBuffer);
    Flixel.host.copyToClipboard(copyBuffer.toString());
  }

  /**
   * Cuts the selected text: copies it to the clipboard and then deletes it.
   *
   * <p>Does nothing when disabled, when there is no selection, or when the platform does not
   * support the clipboard.
   */
  public void cut() {
    if (!isEnabled() || !model.hasSelection()) {
      return;
    }
    copy();
    model.deleteSelection();
    afterEdit();
  }

  /**
   * Requests a paste from the system clipboard.
   *
   * <p>The paste is asynchronous. The text is inserted only when it arrives from the platform,
   * and only if this box is still focused at that point. Does nothing when the platform does not
   * support the clipboard.
   */
  public void paste() {
    if (!isEnabled() || !Flixel.host.supportsClipboard()) {
      return;
    }
    Flixel.host.pasteFromClipboard();
  }

  /**
   * Fires {@link #onSubmit}, signaling that the user has confirmed the current input.
   *
   * <p>In single-line mode this is also called when the keyboard listener sees {@code Enter}.
   */
  public void submit() {
    onSubmit.dispatch(this);
  }

  /**
   * Scrolls the content by the given number of lines.
   *
   * <p>In multi-line mode, scrolls vertically so that {@code lines} lines are skipped. In
   * single-line mode this is a no-op. The scroll is clamped so the content never scrolls past
   * its edges. Caret-follow clamping (applied on every caret move) still overrides this value
   * when the caret moves outside the visible area.
   *
   * <pre>{@code
   * // Scroll down one line with the mouse wheel.
   * notes.scroll(Flixel.mouse.wheel);
   * }</pre>
   *
   * @param lines Number of lines to scroll; negative scrolls up, positive scrolls down.
   */
  public void scroll(float lines) {
    if (!model.isMultiLine()) {
      return;
    }
    float lineH = text.getLineHeight();
    if (lineH <= 0f) {
      return;
    }
    int lineCount = text.getLineCount();
    FlixelTextBoxStyle s = style;
    float contentH = s != null ? Math.max(0f, height - s.padTop - s.padBottom) : height;
    float totalH = lineCount * lineH;
    scrollY += lines * lineH;
    float maxScroll = Math.max(0f, totalH - contentH);
    scrollY = Math.min(Math.max(0f, scrollY), maxScroll);
  }

  /**
   * Returns the zero-based code-unit index at a display-space point, accounting for padding and
   * the current scroll offset.
   *
   * <p>Uses the content text exclusively; the placeholder string never affects this result. Use
   * it to map a mouse click to a caret position:
   *
   * <pre>{@code
   * name.setCaret(name.getIndexAt(mx, my));
   * }</pre>
   *
   * @param x The X coordinate in the display camera's view space.
   * @param y The Y coordinate in the display camera's view space.
   * @return The code-unit index nearest to the point.
   */
  public int getIndexAt(float x, float y) {
    FlixelTextBoxStyle s = style;
    float padL = s != null ? s.padLeft : 0f;
    float padT = s != null ? s.padTop : 0f;
    float localX = x - screenX - padL + scrollX;
    float localY = y - screenY - padT + scrollY;
    return text.getIndexAt(localX, localY);
  }

  /**
   * Allows or disallows newlines ({@code Enter} inserts a newline versus submitting).
   *
   * @param multiLine {@code true} to enable multi-line editing, {@code false} for single-line
   */
  public void setMultiLine(boolean multiLine) {
    model.setMultiLine(multiLine);
    invalidateLayout();
  }

  /**
   * Sets the placeholder text shown when the buffer is empty.
   *
   * @param value The placeholder, or {@code null} to clear it.
   */
  public void setPlaceholder(@Nullable CharSequence value) {
    if (value == null || value.length() == 0) {
      if (placeholder != null) {
        placeholder.clear();
      }
      placeholderText.setText("");
      return;
    }
    if (placeholder == null) {
      placeholder = new FlixelString();
    }
    placeholder.set(value);
    placeholderText.setText(placeholder);
  }

  /**
   * Sets the maximum number of code units.
   *
   * @param max The limit; {@code 0} for unlimited.
   */
  public void setMaxLength(int max) {
    model.setMaxLength(max);
    afterEdit();
  }

  /**
   * Sets the character filter.
   *
   * @param filter The filter; {@code null} reverts to accepting all characters.
   */
  public void setFilter(@Nullable FlixelTextFilter filter) {
    model.setFilter(filter);
  }

  /**
   * Sets the character used as the password mask.
   *
   * <p>When non-zero, the display renders this character repeated for each real character in the
   * buffer. The model still stores the actual text. Pass {@code 0} to disable masking.
   *
   * @param c The mask character, or {@code 0} to show real text.
   */
  public void setPasswordChar(char c) {
    passwordChar = c;
    syncDisplayText();
  }

  /**
   * Replaces the text content and moves the caret to the end.
   *
   * @param newText The new text, or {@code null} to clear the buffer.
   */
  public void setText(@Nullable CharSequence newText) {
    model.setText(newText);
    afterEdit();
  }

  /**
   * Returns the current text content as a live {@link CharSequence}.
   *
   * @return The buffer; never {@code null}.
   */
  @NotNull
  public CharSequence getText() {
    return model.getText();
  }

  /**
   * Returns the current horizontal scroll offset in pixels.
   *
   * @return The horizontal scroll offset.
   */
  public float getScrollX() {
    return scrollX;
  }

  /**
   * Returns the current vertical scroll offset in pixels.
   *
   * @return The vertical scroll offset.
   */
  public float getScrollY() {
    return scrollY;
  }

  /**
   * Replaces the style used by this text box.
   *
   * @param newStyle The style, or {@code null} to revert to the skin-resolved one.
   */
  public void setStyle(@Nullable FlixelTextBoxStyle newStyle) {
    customStyle = newStyle != null;
    style = newStyle;
    applyStyleToText();
    invalidateLayout();
  }

  /**
   * Routes a typed character to {@link #type(char)}.
   *
   * <p>Only acts while this box is focused and enabled.
   *
   * @param character The typed character.
   */
  @Override
  public void keyTyped(char character) {
    if (!isFocused() || !isEnabled()) {
      return;
    }
    type(character);
  }

  /**
   * Handles navigation and editing keys on first press.
   *
   * @param keycode The key code.
   */
  @Override
  public void keyDown(int keycode) {
    handleEditKey(keycode);
  }

  /**
   * Handles navigation and editing keys on OS repeat.
   *
   * @param keycode The key code.
   */
  @Override
  public void keyRepeated(int keycode) {
    handleEditKey(keycode);
  }

  private void handleEditKey(int keycode) {
    if (!isFocused() || !isEnabled()) {
      return;
    }
    boolean shift = Flixel.input.isKeyPressed(FlixelKey.SHIFT_LEFT)
        || Flixel.input.isKeyPressed(FlixelKey.SHIFT_RIGHT);
    boolean ctrl = Flixel.input.isKeyPressed(FlixelKey.CONTROL_LEFT)
        || Flixel.input.isKeyPressed(FlixelKey.CONTROL_RIGHT);

    switch (keycode) {
      case FlixelKey.BACKSPACE -> {
        if (ctrl) {
          deleteWord(-1);
        } else {
          backspace();
        }
      }
      case FlixelKey.FORWARD_DEL -> {
        if (ctrl) {
          deleteWord(1);
        } else {
          deleteForward();
        }
      }
      case FlixelKey.LEFT -> {
        if (ctrl) {
          moveCaretWord(-1, shift);
        } else {
          moveCaret(-1, shift);
        }
      }
      case FlixelKey.RIGHT -> {
        if (ctrl) {
          moveCaretWord(1, shift);
        } else {
          moveCaret(1, shift);
        }
      }
      case FlixelKey.UP -> moveCaretLine(-1, shift);
      case FlixelKey.DOWN -> moveCaretLine(1, shift);
      case FlixelKey.HOME -> caretHome(shift);
      case FlixelKey.END -> caretEnd(shift);
      case FlixelKey.ENTER -> {
        if (model.isMultiLine()) {
          type('\n');
        } else {
          submit();
        }
      }
      case FlixelKey.A -> {
        if (ctrl) {
          selectAll();
        }
      }
      case FlixelKey.C -> {
        if (ctrl) {
          copy();
        }
      }
      case FlixelKey.X -> {
        if (ctrl) {
          cut();
        }
      }
      case FlixelKey.V -> {
        if (ctrl) {
          paste();
        }
      }
      default -> {
        /* not a handled key */ }
    }
  }

  /**
   * Synchronizes the display text and fires {@link #onChange} when the model version changed,
   * then always clamps scroll, resets the blink timer, and refreshes the IME area.
   *
   * <p>This must be called after every model mutation (type, insert, backspace, etc.) so that
   * {@link #clampScroll()} always sees current layout data from {@link FlixelText#getCharX}
   * rather than a stale layout that predates the change.
   */
  private void afterEdit() {
    int v = model.version;
    if (v != lastVersion) {
      lastVersion = v;
      syncDisplayText();
      onChange.dispatch(this);
    }
    clampScroll();
    resetBlink();
    if (isFocused()) {
      updateTextInputArea();
    }
  }

  /** Sets the {@link FlixelText} content from the model, including the password mask. */
  private void syncDisplayText() {
    if (passwordChar != 0) {
      int len = model.length();
      maskBuffer.clear();
      for (int i = 0; i < len; i++) {
        maskBuffer.insert(maskBuffer.length(), passwordChar);
      }
      text.setText(maskBuffer);
    } else {
      text.setText(model.getText());
    }
  }

  /** Applies font settings from the current style to both text objects. */
  private void applyStyleToText() {
    FlixelTextBoxStyle s = style;
    if (s == null) {
      return;
    }
    int size = Math.max(1, s.fontSize);
    if (s.font != null) {
      text.setFont(s.font);
      placeholderText.setFont(s.font);
    }
    text.setTextSize(size);
    placeholderText.setTextSize(size);
  }

  /**
   * Draws a text object at a screen-space position, converting to sprite coordinates so it aligns
   * with the batch projection at any camera zoom level.
   *
   * @param batch The sprite batch.
   * @param t The text object to draw.
   * @param textSX The screen X coordinate of the text's left edge.
   * @param textSY The screen Y coordinate of the text's top edge.
   * @param base The base color before tint and alpha are applied.
   * @param tint The widget tint color.
   * @param alpha The combined alpha value.
   */
  private void drawText(@NotNull FlixelBatch batch, @NotNull FlixelText t, float textSX,
      float textSY, @NotNull FlixelColor base, @NotNull FlixelColor tint, float alpha) {
    FlixelUiDisplay d = display;
    float spriteX = d != null ? d.toSpriteX(textSX) : textSX;
    float spriteY = d != null ? d.toSpriteY(textSY) : textSY;
    t.setPosition(spriteX, spriteY);
    t.setColor(base.r * tint.r, base.g * tint.g, base.b * tint.b, base.a * alpha);
    try {
      t.draw(batch);
    } finally {
      t.setColor(base);
    }
  }

  /**
   * Draws selection highlight rectangles for each line overlapping the selection range.
   *
   * <p>Called before drawing the text so the highlight appears behind it. Does not allocate.
   */
  private void drawSelection(
      @NotNull FlixelBatch batch,
      @NotNull FlixelUiBackground sel,
      @NotNull FlixelColor tint,
      float alpha,
      float textSX,
      float textSY) {
    int selStart = model.getSelectionStart();
    int selEnd = model.getSelectionEnd();
    int lineCount = text.getLineCount();
    batch.setColor(tint.r, tint.g, tint.b, alpha);
    for (int ln = 0; ln < lineCount; ln++) {
      int lnStart = text.getLineStart(ln);
      int lnEnd = text.getLineEnd(ln);
      if (lnEnd <= selStart || lnStart >= selEnd) {
        continue;
      }
      int drawStart = Math.max(lnStart, selStart);
      int drawEnd = Math.min(lnEnd, selEnd);
      float x1 = textSX + text.getCharX(drawStart);
      float x2 = textSX + text.getCharX(drawEnd);
      float lineY = textSY + text.getLineTop(ln);
      float h = text.getLineHeight();
      if (x2 > x1 && h > 0f) {
        sel.draw(batch, x1, lineY, x2 - x1, h);
      }
    }
    batch.setColor(FlixelColor.WHITE);
  }

  private void drawCaret(
      @NotNull FlixelBatch batch,
      @NotNull FlixelTextBoxStyle s,
      @NotNull FlixelColor tint,
      float alpha,
      float textSX,
      float textSY,
      float lineH) {
    int caret = model.getCaret();
    float caretLocalX = text.getCharX(caret);
    int caretLine = text.getCharLine(caret);
    float caretLocalY = text.getLineTop(caretLine);
    float caretW = Math.max(1f, s.caretWidth);
    float cx = textSX + caretLocalX - caretW * 0.5f;
    float cy = textSY + caretLocalY;
    batch.setColor(tint.r, tint.g, tint.b, alpha);
    s.caret.draw(batch, cx, cy, caretW, lineH);
    batch.setColor(FlixelColor.WHITE);
  }

  private boolean isCaretVisible(@NotNull FlixelTextBoxStyle s) {
    float rate = s.caretBlinkRate;
    if (rate <= 0f) {
      return true;
    }
    float cycle = rate * 2f;
    return (blinkTimer % cycle) < rate;
  }

  private void resetBlink() {
    blinkTimer = 0f;
  }

  private void afterCaretMove() {
    clampScroll();
    resetBlink();
    if (isFocused()) {
      updateTextInputArea();
    }
  }

  /**
   * Clamps scroll offsets to keep the caret visible inside the content area.
   *
   * <p>Single-line: scrolls horizontally so the caret stays within the content width.
   * Multi-line: scrolls vertically so the caret line stays within the content height.
   * Does not allocate.
   *
   * <p>The early return when {@link #display} is {@code null} is intentional: an unattached
   * widget has no measured layout, so {@link #width} and {@link #height} are zero or
   * uninitialized, making contentW and contentH meaningless. Clamping against a zero-size
   * viewport would corrupt the scroll offsets before the first layout pass.
   */
  private void clampScroll() {
    if (display == null) {
      return;
    }
    FlixelTextBoxStyle s = style;
    if (s == null) {
      return;
    }
    int caret = model.getCaret();
    float contentW = Math.max(0f, width - s.padLeft - s.padRight);
    float contentH = Math.max(0f, height - s.padTop - s.padBottom);

    if (!model.isMultiLine()) {
      float caretX = text.getCharX(caret);
      if (caretX - scrollX < 0f) {
        scrollX = caretX;
      } else if (caretX - scrollX > contentW) {
        scrollX = caretX - contentW;
      }
      if (scrollX < 0f) {
        scrollX = 0f;
      }
    } else {
      int caretLine = text.getCharLine(caret);
      float lineTop = text.getLineTop(caretLine);
      float lineH = text.getLineHeight();
      float lineBottom = lineTop + lineH;
      if (lineTop - scrollY < 0f) {
        scrollY = lineTop;
      } else if (lineBottom - scrollY > contentH) {
        scrollY = lineBottom - contentH;
      }
      if (scrollY < 0f) {
        scrollY = 0f;
      }
    }
  }

  /**
   * Sends the caret rectangle to the platform's IME so virtual keyboards position correctly.
   *
   * <p>Projects from widget screen space to framebuffer pixels via
   * {@link FlixelViewport#projectToScissor}, flips to a top-left
   * origin, and scales to logical window pixels. Only sends when the values change.
   */
  private void updateTextInputArea() {
    FlixelUiDisplay d = display;
    if (d == null) {
      return;
    }
    FlixelTextBoxStyle s = style;
    if (s == null) {
      return;
    }
    float lineH = text.getLineHeight();
    if (lineH <= 0f) {
      lineH = Math.max(1, s.fontSize);
    }
    int caret = model.getCaret();
    int caretLine = text.getCharLine(caret);
    float caretLocalX = text.getCharX(caret);
    float caretLocalTop = text.getLineTop(caretLine);

    float textSX;
    float textSY;
    if (model.isMultiLine()) {
      textSX = screenX + s.padLeft;
      textSY = screenY + s.padTop - scrollY;
    } else {
      textSX = screenX + s.padLeft - scrollX;
      float contentH = Math.max(0f, height - s.padTop - s.padBottom);
      textSY = screenY + s.padTop + (contentH - lineH) * 0.5f;
    }

    float caretW = Math.max(1f, s.caretWidth);
    float cx = textSX + caretLocalX - caretW * 0.5f;
    float cy = textSY + caretLocalTop;

    d.getCamera().getViewport().projectToScissor(cx, cy, caretW, lineH, tiScratch);
    int bbH = Flixel.graphics.getBackBufferHeight();
    int winH = Flixel.window.getHeight();
    float fbTop = bbH - tiScratch.y - tiScratch.height;
    float scale = (bbH > 0 && winH > 0) ? (float) winH / bbH : 1f;
    int tiX = Math.round(tiScratch.x * scale);
    int tiY = Math.round(fbTop * scale);
    int tiW = Math.max(1, Math.round(tiScratch.width * scale));
    int tiH = Math.max(1, Math.round(tiScratch.height * scale));

    if (tiX != lastTiX || tiY != lastTiY || tiW != lastTiW || tiH != lastTiH) {
      lastTiX = tiX;
      lastTiY = tiY;
      lastTiW = tiW;
      lastTiH = tiH;
      Flixel.input.setTextInputArea(tiX, tiY, tiW, tiH);
    }
  }

  private void invalidateTiCache() {
    lastTiX = Integer.MIN_VALUE;
  }

  private void startTextInputOnce() {
    if (!textInputStarted) {
      textInputStarted = true;
      Flixel.input.startTextInput();
    }
  }

  private void stopTextInputIfStarted() {
    if (textInputStarted) {
      textInputStarted = false;
      Flixel.input.stopTextInput();
    }
  }

  /**
   * Receives pasted text from the platform's async clipboard read.
   *
   * <p>The signal may fire on any thread. Insertion is queued to the main thread and only applied
   * when this box is still focused and enabled at that point.
   */
  private void onPasted(@Nullable String pastedText) {
    if (pastedText == null || pastedText.isEmpty()) {
      return;
    }
    FlixelTextBox self = this;
    Flixel.graphics.queueMainThread(() -> {
      if (self.isFocused() && self.isEnabled()) {
        if (self.model.insert(pastedText)) {
          self.afterEdit();
        }
      }
    });
  }

  @Nullable
  private FlixelUiBackground resolveBackground(@Nullable FlixelTextBoxStyle s) {
    if (s == null) {
      return null;
    }
    FlixelUiBackground bg;
    if (!isEnabled()) {
      bg = s.disabled;
    } else if (isFocused()) {
      bg = s.focused;
    } else if (isHovered()) {
      bg = s.over;
    } else {
      bg = s.up;
    }
    return bg != null ? bg : s.up;
  }

  private static int clamp(int val, int min, int max) {
    return val < min ? min : (Math.min(val, max));
  }
}
