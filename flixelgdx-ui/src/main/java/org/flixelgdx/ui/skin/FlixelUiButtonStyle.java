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
import org.flixelgdx.ui.FlixelUiButton;
import org.flixelgdx.ui.graphics.FlixelUiBackground;
import org.flixelgdx.util.FlixelColor;
import org.jetbrains.annotations.Nullable;

/**
 * The look of a {@link FlixelUiButton} in each of its states: backgrounds, font, text colors, and
 * padding.
 *
 * <p>Only {@link #up} is really needed. Every other background falls back to {@link #up} when it
 * is {@code null}, and every other text color falls back to {@link #fontColor}, so a style can
 * start small and grow a hover or pressed look later.
 *
 * <pre>{@code
 * FlixelUiButtonStyle style = new FlixelUiButtonStyle();
 * style.up = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/button.png"), 6, 6, 6, 6));
 * style.over = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/button_over.png"), 6, 6, 6, 6));
 * style.down = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/button_down.png"), 6, 6, 6, 6));
 * style.focused = style.over;
 * style.font = Flixel.files.internal("fonts/pixel.ttf");
 * style.fontSize = 16;
 * style.padLeft = style.padRight = 12;
 * style.padTop = style.padBottom = 6;
 * style.pressedOffsetY = 1; // The label sinks by one pixel while pressed.
 * skin.add("default", style);
 * }</pre>
 *
 * <p>Changing a field after the style is in use does not update buttons on its own; call
 * {@code ui.setSkin(ui.getSkin())} to restyle them.
 */
public class FlixelUiButtonStyle implements FlixelUiStyle {

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

  /** The font file for the button's text, or {@code null} to use the framework's default font. */
  @Nullable
  public FlixelFile font;

  /** The font size in pixels; values below {@code 1} are treated as {@code 1}. */
  public int fontSize = 16;

  /** The text color when no other state applies, white by default; {@code null} also means white. */
  @Nullable
  public FlixelColor fontColor = new FlixelColor(FlixelColor.WHITE);

  /** The text color while hovered; falls back to {@link #fontColor} when {@code null}. */
  @Nullable
  public FlixelColor overFontColor;

  /** The text color while pressed; falls back to {@link #fontColor} when {@code null}. */
  @Nullable
  public FlixelColor downFontColor;

  /** The text color while disabled; falls back to {@link #fontColor} when {@code null}. */
  @Nullable
  public FlixelColor disabledFontColor;

  /** The space between the button's left edge and its content, in pixels. */
  public float padLeft;

  /** The space between the button's top edge and its content, in pixels. */
  public float padTop;

  /** The space between the button's right edge and its content, in pixels. */
  public float padRight;

  /** The space between the button's bottom edge and its content, in pixels. */
  public float padBottom;

  /** The gap between the icon and the text when a button shows both, in pixels. */
  public float iconSpacing = 4f;

  /**
   * How far the content (icon and text) moves down while the button is pressed, in pixels.
   *
   * <p>A small value such as {@code 1} or {@code 2} makes the button feel pushed in when its
   * {@link #down} image is drawn lower than {@link #up}. The default is {@code 0}.
   */
  public float pressedOffsetY;
}
