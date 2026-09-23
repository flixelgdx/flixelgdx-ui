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
import org.flixelgdx.ui.FlixelUiDropdown;
import org.flixelgdx.ui.graphics.FlixelUiBackground;
import org.flixelgdx.ui.graphics.FlixelUiImage;
import org.flixelgdx.util.FlixelColor;
import org.jetbrains.annotations.Nullable;

/**
 * The look of a {@link FlixelUiDropdown}: backgrounds for each field state, the arrow icon, the
 * floating list panel, item row highlighting and selection, and all text styling.
 *
 * <p>Only {@link #up} is required. Every other field background falls back to {@link #up} when
 * {@code null}, and every font color falls back to {@link #fontColor}.
 *
 * <pre>{@code
 * FlixelUiDropdownStyle style = new FlixelUiDropdownStyle();
 * style.up = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/dropdown.png"), 6, 6, 6, 6));
 * style.open = skin.track(
 *     FlixelNineSlice.load(Flixel.files.internal("ui/dropdown_open.png"), 6, 6, 6, 6));
 * style.listBackground = skin.track(
 *     FlixelNineSlice.load(Flixel.files.internal("ui/list_bg.png"), 4, 4, 4, 4));
 * style.itemHighlight = new FlixelUiColorFill(0.2f, 0.5f, 1f, 0.5f);
 * style.font = Flixel.files.internal("fonts/pixel.ttf");
 * style.fontSize = 14;
 * style.fontColor = new FlixelColor(FlixelColor.WHITE);
 * style.padLeft = style.padRight = 8;
 * style.padTop = style.padBottom = 4;
 * style.itemHeight = 24;
 * style.listGap = 2;
 * skin.add("default", style);
 * }</pre>
 *
 * @see FlixelUiDropdown
 */
public class FlixelUiDropdownStyle implements FlixelUiStyle {

  /** The background drawn when no other state applies, or {@code null} for no background. */
  @Nullable
  public FlixelUiBackground up;

  /** The background drawn while hovered; falls back to {@link #up} when {@code null}. */
  @Nullable
  public FlixelUiBackground over;

  /** The background drawn while pressed; falls back to {@link #up} when {@code null}. */
  @Nullable
  public FlixelUiBackground down;

  /** The background drawn while disabled; falls back to {@link #up} when {@code null}. */
  @Nullable
  public FlixelUiBackground disabled;

  /** The background drawn while focused; falls back to {@link #up} when {@code null}. */
  @Nullable
  public FlixelUiBackground focused;

  /**
   * The background drawn while the list is open; falls back to {@link #up} when {@code null}.
   *
   * <p>A distinct graphic (such as a depressed border) helps the player see the dropdown is
   * currently expanded.
   */
  @Nullable
  public FlixelUiBackground open;

  /**
   * An icon drawn at the right edge of the field, vertically centered at its natural size,
   * to indicate the dropdown can be opened; {@code null} for no arrow.
   *
   * <p>A {@link FlixelUiImage} backed by a chevron or triangle works well here. For a
   * {@link FlixelUiImage} the draw size comes from its natural pixel dimensions; for any other
   * background type it comes from {@link FlixelUiBackground#getMinWidth()} and
   * {@link FlixelUiBackground#getMinHeight()}.
   */
  @Nullable
  public FlixelUiBackground arrow;

  /** The background drawn behind the entire list panel; {@code null} for no background. */
  @Nullable
  public FlixelUiBackground listBackground;

  /**
   * The background drawn behind the highlighted row; {@code null} for no highlight visual.
   *
   * <p>Only one row is highlighted at a time. The background is drawn before the row's text.
   */
  @Nullable
  public FlixelUiBackground itemHighlight;

  /**
   * The background drawn behind the selected row; {@code null} to show no separate selected look.
   *
   * <p>When a row is both selected and highlighted, {@code itemSelected} takes priority over
   * {@link #itemHighlight}. When this is {@code null}, the highlighted row uses
   * {@link #itemHighlight} regardless of whether it is also selected.
   */
  @Nullable
  public FlixelUiBackground itemSelected;

  /** The font file for the field and list text; {@code null} for the framework's default font. */
  @Nullable
  public FlixelFile font;

  /** The font size in pixels; values below {@code 1} are treated as {@code 1}. */
  public int fontSize = 16;

  /** The text color for the selected item text and normal list rows; {@code null} means white. */
  @Nullable
  public FlixelColor fontColor;

  /** The text color for the highlighted row; falls back to {@link #fontColor} when {@code null}. */
  @Nullable
  public FlixelColor highlightFontColor;

  /** The text color while disabled; falls back to {@link #fontColor} when {@code null}. */
  @Nullable
  public FlixelColor disabledFontColor;

  /**
   * The text color for the placeholder shown when nothing is selected; falls back to
   * {@link #fontColor} when {@code null}.
   */
  @Nullable
  public FlixelColor placeholderFontColor;

  /** The space between the field's left edge and its text, in pixels. */
  public float padLeft;

  /** The space between the field's top edge and its text, in pixels. */
  public float padTop;

  /** The space between the field's right edge and its text (and the arrow), in pixels. */
  public float padRight;

  /** The space between the field's bottom edge and its text, in pixels. */
  public float padBottom;

  /** The space between the left edge of each list row and the row text, in pixels. */
  public float itemPadLeft;

  /** The space between the top edge of each list row and the row text, in pixels. */
  public float itemPadTop;

  /** The space between the right edge of each list row and the row text, in pixels. */
  public float itemPadRight;

  /** The space between the bottom edge of each list row and the row text, in pixels. */
  public float itemPadBottom;

  /**
   * The height of each list row in pixels.
   *
   * <p>When {@code 0}, the row height is computed as the text line height (from the font and
   * {@link #fontSize}) plus {@link #itemPadTop} and {@link #itemPadBottom}.
   */
  public float itemHeight;

  /**
   * The gap between the dropdown field and the list panel, in pixels.
   *
   * <p>A small positive value (such as {@code 2}) creates a visible separation between the field
   * and the open list. The gap applies both below the field (normal placement) and above it
   * (when the list is flipped because there is no room below).
   */
  public float listGap;
}
