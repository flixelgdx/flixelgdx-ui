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
package org.flixelgdx.ui.skin;

import org.flixelgdx.file.FlixelFile;
import org.flixelgdx.ui.graphics.FlixelUiBackground;
import org.flixelgdx.ui.graphics.FlixelUiColorFill;
import org.flixelgdx.ui.text.FlixelTextBox;
import org.flixelgdx.util.FlixelColor;
import org.jetbrains.annotations.Nullable;

/**
 * The look of a {@link FlixelTextBox}: backgrounds for each widget state, the selection and
 * caret, font settings, and padding.
 *
 * <p>Only {@link #up} is truly required. Every other background falls back to {@link #up} when
 * {@code null}, and every text color falls back to {@link #fontColor}.
 *
 * <pre>{@code
 * FlixelTextBoxStyle style = new FlixelTextBoxStyle();
 * style.up = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/input.png"), 4, 4, 4, 4));
 * style.focused = skin.track(
 *     FlixelNineSlice.load(Flixel.files.internal("ui/input_focused.png"), 4, 4, 4, 4));
 * style.selection = new FlixelUiColorFill(0.2f, 0.5f, 1f, 0.5f);
 * style.caret = new FlixelUiColorFill(FlixelColor.WHITE);
 * style.caretWidth = 2f;
 * style.caretBlinkRate = 0.53f;  // Half a second per half cycle.
 * style.font = Flixel.files.internal("fonts/pixel.ttf");
 * style.fontSize = 14;
 * style.padLeft = style.padRight = 6;
 * style.padTop = style.padBottom = 4;
 * skin.add("default", style);
 * }</pre>
 *
 * @see FlixelTextBox
 */
public class FlixelTextBoxStyle implements FlixelUiStyle {

  // --- Backgrounds ---

  /** The background drawn when no other state applies. {@code null} means no background. */
  @Nullable
  public FlixelUiBackground up;

  /** The background drawn while hovered; falls back to {@link #up} when {@code null}. */
  @Nullable
  public FlixelUiBackground over;

  /** The background drawn while focused; falls back to {@link #up} when {@code null}. */
  @Nullable
  public FlixelUiBackground focused;

  /** The background drawn while disabled; falls back to {@link #up} when {@code null}. */
  @Nullable
  public FlixelUiBackground disabled;

  // --- Selection and caret ---

  /**
   * The background drawn behind selected text, one rectangle per line.
   *
   * <p>Use a {@link FlixelUiColorFill} with a semi-transparent color for the classic "blue
   * highlight" look. {@code null} means no selection visual.
   */
  @Nullable
  public FlixelUiBackground selection;

  /**
   * The thin rectangle drawn at the insertion point while the box is focused.
   *
   * <p>A {@link FlixelUiColorFill} set to a solid color works well. {@code null} means no caret
   * visual.
   */
  @Nullable
  public FlixelUiBackground caret;

  /**
   * The width of the caret rectangle in pixels.
   *
   * <p>The default {@code 2} gives a visible but non-intrusive cursor. The caret is always at
   * least {@code 1} pixel wide when drawn.
   */
  public float caretWidth = 2f;

  /**
   * The length of each blink half-cycle in seconds.
   *
   * <p>For example, {@code 0.53f} means the caret is visible for about half a second, then
   * invisible for the same duration, giving a natural blink rhythm. Set to {@code 0} to disable
   * blinking (the caret is always visible while focused).
   */
  public float caretBlinkRate;

  // --- Font ---

  /** The font file for the typed text; {@code null} uses the framework's default font. */
  @Nullable
  public FlixelFile font;

  /** The font size in pixels; values below {@code 1} are treated as {@code 1}. */
  public int fontSize = 16;

  /** The text color for normal input; {@code null} means white. */
  @Nullable
  public FlixelColor fontColor;

  /** The text color for the placeholder; falls back to {@link #fontColor} when {@code null}. */
  @Nullable
  public FlixelColor placeholderFontColor;

  /** The text color while disabled; falls back to {@link #fontColor} when {@code null}. */
  @Nullable
  public FlixelColor disabledFontColor;

  // --- Padding ---

  /** Space between the left edge and the text content, in pixels. */
  public float padLeft;

  /** Space between the top edge and the text content, in pixels. */
  public float padTop;

  /** Space between the right edge and the text content, in pixels. */
  public float padRight;

  /** Space between the bottom edge and the text content, in pixels. */
  public float padBottom;
}
