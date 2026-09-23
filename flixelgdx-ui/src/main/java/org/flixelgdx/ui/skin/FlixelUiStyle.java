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

import org.flixelgdx.ui.FlixelUiWidget;
import org.flixelgdx.ui.graphics.FlixelUiBackground;

/**
 * Marks a class as the look of one kind of widget, so it can be stored in a {@link FlixelUiSkin}.
 *
 * <p>A style is a plain bag of settings with public fields and no behavior: which
 * {@link FlixelUiBackground} to draw in each state, which font file and size to use, which
 * colors, how much padding. Every widget type has its own style class (a button has a button
 * style, a checkbox has a checkbox style), and a skin can hold several named styles of each class,
 * such as a {@code "default"} button and a {@code "fab"} (floating action) button.
 *
 * <p>A widget's style class follows this pattern:
 *
 * <pre>{@code
 * public class FlixelUiButtonStyle implements FlixelUiStyle {
 *   public FlixelUiBackground up;           // Drawn normally.
 *   public FlixelUiBackground over;         // Drawn while hovered; falls back to up.
 *   public FlixelUiBackground down;         // Drawn while pressed; falls back to up.
 *   public FlixelFile font;                 // A font file, never a string path.
 *   public int fontSize = 16;
 *   public FlixelColor fontColor = new FlixelColor(FlixelColor.WHITE);
 *   public float padLeft, padTop, padRight, padBottom;
 * }
 * }</pre>
 *
 * <p>Styles are looked up by their exact class, so a subclass of a style is stored separately from
 * its parent class. Several styles may share the same background object; see
 * {@link FlixelUiSkin#track(FlixelUiBackground)} for who destroys it.
 *
 * @see FlixelUiSkin
 * @see FlixelUiWidget
 */
public interface FlixelUiStyle {
}
