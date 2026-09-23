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

import org.flixelgdx.ui.FlixelUiPanel;
import org.flixelgdx.ui.graphics.FlixelUiBackground;
import org.jetbrains.annotations.Nullable;

/**
 * The look of a {@link FlixelUiPanel}: the background drawn behind its children and the padding
 * that keeps them away from its edges.
 *
 * <pre>{@code
 * FlixelUiPanelStyle window = new FlixelUiPanelStyle();
 * window.background = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/panel.png"), 8, 8, 8, 8));
 * window.padLeft = window.padTop = window.padRight = window.padBottom = 12;
 * skin.add("default", window);
 * }</pre>
 *
 * <p>The padding is usually the same as the nine-slice's border, so children never overlap the
 * frame.
 */
public class FlixelUiPanelStyle implements FlixelUiStyle {

  /** The background drawn behind the panel's children, or {@code null} for no background. */
  @Nullable
  public FlixelUiBackground background;

  /** The space between the panel's left edge and its children, in pixels. */
  public float padLeft;

  /** The space between the panel's top edge and its children, in pixels. */
  public float padTop;

  /** The space between the panel's right edge and its children, in pixels. */
  public float padRight;

  /** The space between the panel's bottom edge and its children, in pixels. */
  public float padBottom;
}
