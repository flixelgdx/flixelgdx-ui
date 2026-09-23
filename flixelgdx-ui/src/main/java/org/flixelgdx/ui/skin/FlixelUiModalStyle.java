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

import org.flixelgdx.ui.FlixelUiModal;
import org.flixelgdx.ui.graphics.FlixelUiBackground;
import org.flixelgdx.ui.graphics.FlixelUiColorFill;
import org.jetbrains.annotations.Nullable;

/**
 * The look of a {@link FlixelUiModal}: the modal panel style plus the backdrop drawn over the
 * whole display before the modal itself.
 *
 * <p>It extends {@link FlixelUiPanelStyle} so you can still set a nine-slice background and
 * padding for the modal window, and adds one extra field for the backdrop.
 *
 * <p>The backdrop is a full-screen overlay drawn behind this modal (but above every modal below
 * it). A semi-transparent black fill is the most common choice.
 *
 * <pre>{@code
 * FlixelUiModalStyle modalStyle = new FlixelUiModalStyle();
 * modalStyle.background = skin.track(
 *     FlixelNineSlice.load(Flixel.files.internal("ui/dialog.png"), 8, 8, 8, 8));
 * modalStyle.padLeft = modalStyle.padTop = modalStyle.padRight = modalStyle.padBottom = 16;
 * // A translucent black backdrop at 60% opacity.
 * modalStyle.backdrop = new FlixelUiColorFill(0f, 0f, 0f, 0.6f);
 * skin.add("default", modalStyle);
 * }</pre>
 *
 * <p>The skin key is separate from the panel key: a skin can hold both a {@link FlixelUiPanelStyle}
 * named {@code "default"} and a {@link FlixelUiModalStyle} named {@code "default"} without
 * conflict, because they are stored in different drawers.
 *
 * @see FlixelUiModal
 * @see FlixelUiColorFill
 */
public class FlixelUiModalStyle extends FlixelUiPanelStyle {

  /**
   * The background drawn over the whole display before this modal, or {@code null} for none.
   *
   * <p>A {@link FlixelUiColorFill} with low alpha (around 0.5 to 0.7) is the standard choice.
   * When multiple modals are open, each one draws its own backdrop, so the screen progressively
   * dims with each additional layer.
   */
  @Nullable
  public FlixelUiBackground backdrop;
}
