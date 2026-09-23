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
import org.flixelgdx.ui.FlixelUiTooltip;
import org.flixelgdx.ui.graphics.FlixelUiBackground;
import org.flixelgdx.util.FlixelColor;
import org.jetbrains.annotations.Nullable;

/**
 * The look of a {@link FlixelUiTooltip}: its background, font, padding around the text, the gap
 * between the target widget and the tooltip bubble, and an optional maximum width for wrapping.
 *
 * <p>A tooltip style is a plain bag of settings. Fill one in, add it to a {@link FlixelUiSkin}
 * under the name {@code "default"} (or another name passed to
 * {@code FlixelUiDisplay.setTooltipStyle(...)}), and every tooltip shown by that display wears it:
 *
 * <pre>{@code
 * FlixelUiTooltipStyle tip = new FlixelUiTooltipStyle();
 * tip.background = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/tooltip.png"),
 *     4, 4, 4, 4));
 * tip.font = Flixel.files.internal("fonts/pixel.ttf");
 * tip.fontSize = 14;
 * tip.fontColor = new FlixelColor(0xFF_FF_CC_FF);
 * tip.padLeft = tip.padRight = 6;
 * tip.padTop = tip.padBottom = 4;
 * tip.gap = 4;
 * tip.maxWidth = 200;
 * skin.add("default", tip);
 * }</pre>
 *
 * <p>Changing a field after the style is in use does not update the tooltip on its own; call
 * {@code ui.setSkin(ui.getSkin())} to restyle it.
 */
public class FlixelUiTooltipStyle implements FlixelUiStyle {

  /** The background frame drawn behind the text, or {@code null} for no background. */
  @Nullable
  public FlixelUiBackground background;

  /** The font file to draw with, or {@code null} to use the framework's default font. */
  @Nullable
  public FlixelFile font;

  /** The font size in pixels; values below {@code 1} are treated as {@code 1}. */
  public int fontSize = 16;

  /** The text color; {@code null} means white. */
  @Nullable
  public FlixelColor fontColor = new FlixelColor(FlixelColor.WHITE);

  /** The space between the tooltip's left edge and the text, in pixels. */
  public float padLeft;

  /** The space between the tooltip's top edge and the text, in pixels. */
  public float padTop;

  /** The space between the tooltip's right edge and the text, in pixels. */
  public float padRight;

  /** The space between the tooltip's bottom edge and the text, in pixels. */
  public float padBottom;

  /**
   * The distance between the target widget and the tooltip bubble, in pixels.
   *
   * <p>A positive value leaves a gap; {@code 0} places the tooltip flush against the widget.
   */
  public float gap;

  /**
   * The width at which long text wraps onto new lines, in pixels.
   *
   * <p>This is the text area width, not counting the padding. A value of {@code 0} (the default)
   * lets the text grow as wide as it needs to on one line; a positive value causes wrapping.
   */
  public float maxWidth;
}
