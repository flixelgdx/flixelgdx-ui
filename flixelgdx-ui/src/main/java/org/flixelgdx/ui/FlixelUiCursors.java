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
package org.flixelgdx.ui;

import org.flixelgdx.input.mouse.FlixelMouseCursor;
import org.flixelgdx.ui.text.FlixelTextBox;
import org.jetbrains.annotations.Nullable;

/**
 * Global settings for the mouse cursor each kind of widget asks for.
 *
 * <p>On desktop and in the browser, players expect the cursor to change over things they can use:
 * a pointing hand over a button, a text beam over a text box, a grabbing hand on a slider. The UI
 * never touches input, so it cannot change the cursor itself. Instead, each widget reports the
 * cursor it wants through {@link FlixelUiWidget#getCursor()} and
 * {@link FlixelUiWidget#getPressedCursor()}, {@link FlixelUiPointer#getCursor()} picks the one
 * that applies right now, and the game shows it:
 *
 * <pre>{@code
 * // In update(), next to the other pointer calls:
 * Flixel.mouse.icons.setCursor(pointer.getCursor());
 * }</pre>
 *
 * <p>The fields below are the defaults every widget of that kind uses. Set one to {@code null} to
 * stop that kind of widget from changing the cursor, or set {@link #enabled} to {@code false} to
 * turn cursor changes off everywhere. A single widget can still pick its own cursor with
 * {@link FlixelUiWidget#setCursor(FlixelMouseCursor)}.
 *
 * <pre>{@code
 * FlixelUiCursors.textBox = null;   // Text boxes keep the normal arrow.
 * FlixelUiCursors.enabled = false;  // No widget changes the cursor.
 * }</pre>
 *
 * <p>Like every other UI setting, these are read on the game's main thread only.
 */
public final class FlixelUiCursors {

  /**
   * Whether widgets change the cursor at all.
   *
   * <p>When {@code false}, {@link FlixelUiPointer#getCursor()} always returns
   * {@link FlixelMouseCursor#ARROW}. Defaults to {@code true}.
   */
  public static boolean enabled = true;

  /** The cursor over a {@link FlixelUiButton}, or {@code null} for none. Defaults to {@code HAND}. */
  @Nullable
  public static FlixelMouseCursor button = FlixelMouseCursor.HAND;

  /** The cursor over a {@link FlixelTextBox}, or {@code null} for none. Defaults to {@code IBEAM}. */
  @Nullable
  public static FlixelMouseCursor textBox = FlixelMouseCursor.IBEAM;

  /** The cursor over a {@link FlixelUiSlider}, or {@code null} for none. Defaults to {@code GRAB}. */
  @Nullable
  public static FlixelMouseCursor slider = FlixelMouseCursor.GRAB;

  /**
   * The cursor while a {@link FlixelUiSlider} is being dragged, or {@code null} to keep showing
   * {@link #slider}. Defaults to {@code GRABBING}.
   */
  @Nullable
  public static FlixelMouseCursor sliderDragging = FlixelMouseCursor.GRABBING;

  private FlixelUiCursors() {}

  /** Puts every setting back to its default. */
  public static void reset() {
    enabled = true;
    button = FlixelMouseCursor.HAND;
    textBox = FlixelMouseCursor.IBEAM;
    slider = FlixelMouseCursor.GRAB;
    sliderDragging = FlixelMouseCursor.GRABBING;
  }
}
