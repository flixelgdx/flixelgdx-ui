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
import org.flixelgdx.ui.FlixelUiLabel;
import org.flixelgdx.util.FlixelColor;
import org.jetbrains.annotations.Nullable;

/**
 * The look of a {@link FlixelUiLabel}: which font it uses, how big, and in which color.
 *
 * <p>A label style is a plain bag of settings. Fill one in, add it to a {@link FlixelUiSkin}, and
 * every label whose style name matches wears it:
 *
 * <pre>{@code
 * FlixelUiLabelStyle title = new FlixelUiLabelStyle();
 * title.font = Flixel.files.internal("fonts/pixel.ttf");
 * title.fontSize = 24;
 * title.color.set(FlixelColor.YELLOW);
 * skin.add("title", title);
 *
 * FlixelUiLabel heading = new FlixelUiLabel("Settings");
 * heading.setStyleName("title");
 * }</pre>
 *
 * <p>Changing a field after the style is in use does not update labels on its own; call
 * {@code ui.setSkin(ui.getSkin())} to restyle them.
 */
public class FlixelUiLabelStyle implements FlixelUiStyle {

  /** The font file to draw with, or {@code null} to use the framework's default font. */
  @Nullable
  public FlixelFile font;

  /** The font size in pixels; values below {@code 1} are treated as {@code 1}. */
  public int fontSize = 16;

  /** The text color, white by default; {@code null} also means white. */
  @Nullable
  public FlixelColor color = new FlixelColor(FlixelColor.WHITE);
}
