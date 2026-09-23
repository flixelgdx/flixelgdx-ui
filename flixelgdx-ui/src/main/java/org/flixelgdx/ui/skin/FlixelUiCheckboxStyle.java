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
import org.flixelgdx.ui.FlixelUiCheckbox;
import org.flixelgdx.ui.graphics.FlixelUiBackground;
import org.flixelgdx.ui.graphics.FlixelUiImage;
import org.flixelgdx.util.FlixelColor;
import org.jetbrains.annotations.Nullable;

/**
 * The look of a {@link FlixelUiCheckbox}: box backgrounds for every combination of checked and
 * interaction state, a font, and the spacing between the box and the label.
 *
 * <p>Only {@link #off} and {@link #on} are required. Every optional variant falls back to its
 * plain counterpart ({@link #offOver} falls back to {@link #off}, {@link #onDown} falls back to
 * {@link #on}, and so on), so a minimal style only needs two backgrounds:
 *
 * <pre>{@code
 * FlixelUiCheckboxStyle style = new FlixelUiCheckboxStyle();
 * style.off = skin.track(new FlixelUiImage(Flixel.files.internal("ui/check_off.png")));
 * style.on  = skin.track(new FlixelUiImage(Flixel.files.internal("ui/check_on.png")));
 * style.font = Flixel.files.internal("fonts/pixel.ttf");
 * style.fontSize = 16;
 * style.fontColor = new FlixelColor(FlixelColor.WHITE);
 * style.spacing = 8;
 * skin.add("default", style);
 *
 * FlixelUiCheckbox vsync = new FlixelUiCheckbox("V-Sync", true);
 * vsync.onChange.add(cb -> applySettings(cb.isChecked()));
 * ui.add(vsync);
 * }</pre>
 *
 * <p>The visual priority for state selection is: disabled, then pressed (down), then hovered
 * (over), then focused, and finally the plain state. Each state picks the matching variant for
 * the current checked state ({@code on} or {@code off}), falling back to the plain {@code on} or
 * {@code off} background when the variant is {@code null}.
 *
 * <p>Changing a field after the style is in use does not update checkboxes on its own; call
 * {@code ui.setSkin(ui.getSkin())} to restyle them.
 */
public class FlixelUiCheckboxStyle implements FlixelUiStyle {

  /** The box background when unchecked and no other state applies. Required. */
  @Nullable
  public FlixelUiBackground off;

  /** The box background when checked and no other state applies. Required. */
  @Nullable
  public FlixelUiBackground on;

  /** The box background when unchecked and hovered; falls back to {@link #off}. */
  @Nullable
  public FlixelUiBackground offOver;

  /** The box background when checked and hovered; falls back to {@link #on}. */
  @Nullable
  public FlixelUiBackground onOver;

  /** The box background when unchecked and pressed; falls back to {@link #off}. */
  @Nullable
  public FlixelUiBackground offDown;

  /** The box background when checked and pressed; falls back to {@link #on}. */
  @Nullable
  public FlixelUiBackground onDown;

  /** The box background when unchecked and disabled; falls back to {@link #off}. */
  @Nullable
  public FlixelUiBackground offDisabled;

  /** The box background when checked and disabled; falls back to {@link #on}. */
  @Nullable
  public FlixelUiBackground onDisabled;

  /** The box background when unchecked and focused; falls back to {@link #off}. */
  @Nullable
  public FlixelUiBackground offFocused;

  /** The box background when checked and focused; falls back to {@link #on}. */
  @Nullable
  public FlixelUiBackground onFocused;

  /** The font file for the label, or {@code null} to use the framework's default font. */
  @Nullable
  public FlixelFile font;

  /** The label text color; {@code null} is treated as white. */
  @Nullable
  public FlixelColor fontColor = new FlixelColor(FlixelColor.WHITE);

  /** The label text color while disabled; falls back to {@link #fontColor} when {@code null}. */
  @Nullable
  public FlixelColor disabledFontColor;

  /**
   * The font size in pixels; values below {@code 1} are treated as {@code 1}.
   *
   * <p>The default is {@code 16}.
   */
  public int fontSize = 16;

  /**
   * The box width and height in pixels.
   *
   * <p>When set to {@code 0} (the default), the box is drawn at the current background's minimum
   * size; for a {@link FlixelUiImage} that is its natural pixel size.
   */
  public float boxSize;

  /** The gap between the right edge of the box and the left edge of the label, in pixels. */
  public float spacing = 8f;
}
