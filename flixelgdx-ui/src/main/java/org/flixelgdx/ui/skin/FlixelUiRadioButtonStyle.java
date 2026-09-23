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

import org.flixelgdx.ui.FlixelUiRadioButton;

/**
 * The look of a {@link FlixelUiRadioButton}.
 *
 * <p>This is a separate skin entry from {@link FlixelUiCheckboxStyle} so a game can give radio
 * buttons a round box look while checkboxes wear a square one, all under the same style name.
 * Both classes share the same fields: see {@link FlixelUiCheckboxStyle} for field documentation
 * and a usage example.
 *
 * <pre>{@code
 * FlixelUiRadioButtonStyle style = new FlixelUiRadioButtonStyle();
 * style.off = skin.track(new FlixelUiImage(Flixel.files.internal("ui/radio_off.png")));
 * style.on  = skin.track(new FlixelUiImage(Flixel.files.internal("ui/radio_on.png")));
 * style.font = Flixel.files.internal("fonts/pixel.ttf");
 * style.fontSize = 16;
 * skin.add("default", style);
 *
 * FlixelUiRadioGroup difficulty = new FlixelUiRadioGroup();
 * FlixelUiRadioButton easy = new FlixelUiRadioButton("Easy", difficulty);
 * FlixelUiRadioButton hard = new FlixelUiRadioButton("Hard", difficulty);
 * difficulty.select(0); // Select "Easy" by default.
 * }</pre>
 */
public class FlixelUiRadioButtonStyle extends FlixelUiCheckboxStyle {
  // Inherits all fields from FlixelUiCheckboxStyle; no new fields are needed.
  // The separate class is the skin key, so radio buttons and checkboxes draw differently.
}
