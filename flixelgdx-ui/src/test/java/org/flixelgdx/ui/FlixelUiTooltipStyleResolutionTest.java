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

import org.flixelgdx.FlixelCamera;
import org.flixelgdx.ui.skin.FlixelUiSkin;
import org.flixelgdx.ui.skin.FlixelUiTooltipStyle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies that {@link FlixelUiTooltip#onStyleChanged()} uses an explicit skin check instead of
 * swallowing all {@code IllegalArgumentException}s, so the no-style fallback works correctly while
 * genuine errors from other resolution paths are still visible.
 */
@ExtendWith(FlixelUiHeadlessExtension.class)
class FlixelUiTooltipStyleResolutionTest {

  @Test
  void tooltipStyleIsNullWhenSkinHasNoTooltipStyle() {
    // Skin with no tooltip style at all.
    FlixelUiSkin skin = new FlixelUiSkin();
    FlixelUiDisplay ui = new FlixelUiDisplay(new FlixelCamera(640, 360), skin);
    FlixelUiTooltip tip = ui.getTooltip();
    // Tooltip must be usable (plain text, no background) rather than crashing.
    assertNull(tip.getStyle(),
        "tooltip style must be null when the skin has no matching tooltip style");
  }

  @Test
  void tooltipStyleIsResolvedWhenSkinHasIt() {
    FlixelUiSkin skin = new FlixelUiSkin();
    FlixelUiTooltipStyle style = new FlixelUiTooltipStyle();
    skin.add("default", style);
    FlixelUiDisplay ui = new FlixelUiDisplay(new FlixelCamera(640, 360), skin);
    FlixelUiTooltip tip = ui.getTooltip();
    assertNotNull(tip.getStyle(), "tooltip style must be resolved when present in the skin");
  }

  @Test
  void replacingSkinWithoutTooltipStyleFallsBackGracefully() {
    FlixelUiSkin skinWithStyle = new FlixelUiSkin();
    skinWithStyle.add("default", new FlixelUiTooltipStyle());
    FlixelUiDisplay ui = new FlixelUiDisplay(new FlixelCamera(640, 360), skinWithStyle);
    assertNotNull(ui.getTooltip().getStyle());

    FlixelUiSkin skinWithout = new FlixelUiSkin();
    ui.setSkin(skinWithout);
    assertNull(ui.getTooltip().getStyle(),
        "replacing the skin with one without a tooltip style must fall back gracefully");
  }
}
