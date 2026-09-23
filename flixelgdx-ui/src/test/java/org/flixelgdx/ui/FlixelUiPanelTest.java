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
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.graphics.FlixelNoopTexture;
import org.flixelgdx.graphics.FlixelTexture;
import org.flixelgdx.ui.graphics.FlixelUiImage;
import org.flixelgdx.ui.graphics.RecordingBatch;
import org.flixelgdx.ui.skin.FlixelUiPanelStyle;
import org.flixelgdx.ui.skin.FlixelUiSkin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link FlixelUiPanel}: its background, the style padding rule, and click blocking.
 */
@ExtendWith(FlixelUiHeadlessExtension.class)
class FlixelUiPanelTest {

  private static final float EPS = 1e-4f;

  private final FlixelTexture texture = new FlixelNoopTexture(64, 64);

  private FlixelFrame frame;
  private FlixelUiPanelStyle style;
  private FlixelUiSkin skin;
  private FlixelUiDisplay ui;

  @BeforeEach
  void setUp() {
    frame = new FlixelFrame(texture, 0, 0, 32, 32);
    style = new FlixelUiPanelStyle();
    style.background = new FlixelUiImage(frame);
    style.padLeft = 1;
    style.padTop = 2;
    style.padRight = 3;
    style.padBottom = 4;
    skin = new FlixelUiSkin();
    skin.add("default", style);
    ui = new FlixelUiDisplay(new FlixelCamera(640, 360), skin);
  }

  @Test
  void drawsTheStyleBackgroundOverItsRectangle() {
    FlixelUiPanel panel = new FlixelUiPanel(100, 50);
    panel.setStyle(style);
    RecordingBatch batch = new RecordingBatch();
    panel.drawSelf(batch, 3, 4, 0.5f);
    assertEquals(1, batch.getDrawCount());
    assertSame(frame, batch.getFrame(0));
    assertEquals(3f, batch.getX(0), EPS);
    assertEquals(4f, batch.getY(0), EPS);
    assertEquals(100f, batch.getWidth(0), EPS);
    assertEquals(50f, batch.getHeight(0), EPS);
    assertEquals(0.5f, batch.getAlpha(0), EPS);
    assertEquals(1f, batch.getColor().a, EPS, "the batch color is restored");

    style.background = null;
    batch.reset();
    panel.drawSelf(batch, 0, 0, 1f);
    assertEquals(0, batch.getDrawCount(), "no background draws nothing");
  }

  @Test
  void usesTheStylePaddingUntilTheGameSetsItsOwn() {
    FlixelUiPanel panel = new FlixelUiPanel(100, 50);
    ui.add(panel);
    assertSame(style, panel.getStyle());
    assertEquals(1f, panel.getPaddingLeft(), EPS);
    assertEquals(2f, panel.getPaddingTop(), EPS);
    assertEquals(3f, panel.getPaddingRight(), EPS);
    assertEquals(4f, panel.getPaddingBottom(), EPS);
    assertFalse(panel.isCustomPadding());

    panel.setPadding(10);
    assertTrue(panel.isCustomPadding());
    ui.setSkin(skin); // Restyles every widget.
    assertEquals(10f, panel.getPaddingLeft(), EPS, "explicit padding wins over the style");
    assertEquals(10f, panel.getPaddingBottom(), EPS);

    FlixelUiPanelStyle other = new FlixelUiPanelStyle();
    other.padLeft = 20;
    panel.setStyle(other);
    assertEquals(10f, panel.getPaddingLeft(), EPS);
  }

  @Test
  void blocksHitsOnItsEmptyArea() {
    FlixelUiPanel panel = new FlixelUiPanel(200, 100);
    panel.setPosition(10, 10);
    TestWidget child = new TestWidget(20, 20);
    panel.add(child);
    ui.add(panel);
    ui.layout();

    // The child sits at the content origin: the panel's corner plus the style padding (1, 2).
    assertSame(child, ui.getWidgetAt(12, 13));
    assertSame(panel, ui.getWidgetAt(150, 80), "the panel itself blocks the click");
    assertNull(ui.getWidgetAt(5, 5));

    panel.interactive = false;
    assertNull(ui.getWidgetAt(150, 80), "a non-interactive panel lets clicks through");
  }

  @Test
  void missingSkinStyleThrows() {
    FlixelUiDisplay empty = new FlixelUiDisplay(new FlixelCamera(640, 360));
    assertThrows(IllegalArgumentException.class, () -> empty.add(new FlixelUiPanel()));
  }
}
