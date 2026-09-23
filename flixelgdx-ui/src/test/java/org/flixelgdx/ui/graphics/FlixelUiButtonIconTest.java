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
package org.flixelgdx.ui.graphics;

import org.flixelgdx.Flixel;
import org.flixelgdx.file.FlixelFile;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.graphics.FlixelGraphic;
import org.flixelgdx.graphics.FlixelNoopTexture;
import org.flixelgdx.ui.FlixelUiButton;
import org.flixelgdx.ui.FlixelUiHeadlessExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies that a {@link FlixelUiButton} releases the icon image it loaded, and only that one.
 *
 * <p>It lives in this package to read {@link FlixelUiImage}'s owned image.
 */
@ExtendWith(FlixelUiHeadlessExtension.class)
class FlixelUiButtonIconTest {

  @Test
  void ownedIconIsReleasedOnDestroy() {
    FlixelUiButton button = new FlixelUiButton(Flixel.files.internal("ui/plus.png"), null);
    FlixelUiImage icon = button.getIcon();
    assertNotNull(icon);
    FlixelGraphic graphic = icon.getGraphic();
    assertNotNull(graphic, "an icon from a file is owned");
    int held = graphic.getRefCount();
    button.destroy();
    assertNull(button.getIcon());
    assertNull(icon.getGraphic());
    assertEquals(held - 1, graphic.getRefCount());
  }

  @Test
  void replacingAnOwnedIconReleasesIt() {
    FlixelUiButton button = new FlixelUiButton("New", Flixel.files.internal("ui/plus.png"), null);
    FlixelUiImage icon = button.getIcon();
    assertNotNull(icon);
    FlixelGraphic graphic = icon.getGraphic();
    assertNotNull(graphic);
    int held = graphic.getRefCount();

    FlixelFrame frame = new FlixelFrame(new FlixelNoopTexture(16, 16));
    button.setIcon(frame);
    assertEquals(held - 1, graphic.getRefCount());
    FlixelUiImage frameIcon = button.getIcon();
    assertNotNull(frameIcon);
    assertNull(frameIcon.getGraphic(), "a caller's frame is not owned");

    button.setIcon((FlixelFrame) null);
    assertNull(button.getIcon());
    button.destroy();
  }

  @Test
  void nullIconFileIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new FlixelUiButton((FlixelFile) null, null));
  }
}
