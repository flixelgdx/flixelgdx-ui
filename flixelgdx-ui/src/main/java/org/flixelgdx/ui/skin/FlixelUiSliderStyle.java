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

import org.flixelgdx.ui.FlixelUiSlider;
import org.flixelgdx.ui.graphics.FlixelUiBackground;
import org.jetbrains.annotations.Nullable;

/**
 * The look of a {@link FlixelUiSlider}: a track, an optional filled portion, and a thumb with
 * hovered, pressed, and disabled variants.
 *
 * <p>Only {@link #track} and {@link #thumb} are required. Every optional thumb variant falls back
 * to {@link #thumb}, and a {@code null} {@link #fill} simply draws no filled portion:
 *
 * <pre>{@code
 * FlixelUiSliderStyle style = new FlixelUiSliderStyle();
 * style.track = new FlixelUiColorFill(0.2f, 0.2f, 0.2f, 1f);
 * style.fill = new FlixelUiColorFill(0.3f, 0.7f, 1f, 1f);
 * style.thumb = skin.track(FlixelUiImage.load(Flixel.files.internal("ui/knob.png")));
 * style.thumbWidth = 16;
 * style.thumbHeight = 16;
 * style.trackThickness = 6;
 * skin.add("default", style);
 *
 * FlixelUiSlider volume = new FlixelUiSlider(200, 0f, 1f, 0.8f);
 * volume.onChange.add(s -> audio.setVolume(s.getValue()));
 * ui.add(volume);
 * }</pre>
 *
 * <p>The thumb variant is picked in the order: disabled, pressed (down), hovered (over), plain.
 * Changing a field after the style is in use does not update sliders on its own; call
 * {@code ui.setSkin(ui.getSkin())} to restyle them.
 */
public class FlixelUiSliderStyle implements FlixelUiStyle {

  /** The track, stretched along the slider's whole length. Required. */
  @Nullable
  public FlixelUiBackground track;

  /**
   * The filled portion, drawn on top of the track from the start of the slider to the middle of
   * the thumb; {@code null} draws no fill.
   */
  @Nullable
  public FlixelUiBackground fill;

  /** The thumb when no other state applies. Required. */
  @Nullable
  public FlixelUiBackground thumb;

  /** The thumb while hovered; falls back to {@link #thumb}. */
  @Nullable
  public FlixelUiBackground thumbOver;

  /** The thumb while pressed or being dragged; falls back to {@link #thumb}. */
  @Nullable
  public FlixelUiBackground thumbDown;

  /** The thumb while the slider is disabled; falls back to {@link #thumb}. */
  @Nullable
  public FlixelUiBackground thumbDisabled;

  /** The thumb width in pixels. The default is {@code 16}. */
  public float thumbWidth = 16f;

  /** The thumb height in pixels. The default is {@code 16}. */
  public float thumbHeight = 16f;

  /**
   * The track thickness in pixels (its height for a horizontal slider, its width for a vertical
   * one).
   *
   * <p>When set to {@code 0}, the track fills the slider's whole cross size. The default is
   * {@code 6}.
   */
  public float trackThickness = 6f;
}
