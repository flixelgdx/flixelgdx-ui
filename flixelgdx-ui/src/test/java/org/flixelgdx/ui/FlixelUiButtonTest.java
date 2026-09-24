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
import org.flixelgdx.ui.graphics.FlixelNineSlice;
import org.flixelgdx.ui.graphics.FlixelUiImage;
import org.flixelgdx.ui.graphics.RecordingBatch;
import org.flixelgdx.ui.skin.FlixelUiButtonStyle;
import org.flixelgdx.ui.skin.FlixelUiSkin;
import org.flixelgdx.util.FlixelColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link FlixelUiButton}: which background each state draws, clicking, style fallbacks,
 * sizing, and content placement.
 */
@ExtendWith(FlixelUiHeadlessExtension.class)
class FlixelUiButtonTest {

  private static final float EPS = 1e-4f;

  private final FlixelTexture texture = new FlixelNoopTexture(256, 256);
  private final RecordingBatch batch = new RecordingBatch();

  private FlixelFrame upFrame;
  private FlixelFrame overFrame;
  private FlixelFrame downFrame;
  private FlixelFrame disabledFrame;
  private FlixelFrame focusedFrame;
  private FlixelUiButtonStyle full;

  @BeforeEach
  void setUp() {
    upFrame = new FlixelFrame(texture, 0, 0, 16, 16);
    overFrame = new FlixelFrame(texture, 16, 0, 16, 16);
    downFrame = new FlixelFrame(texture, 32, 0, 16, 16);
    disabledFrame = new FlixelFrame(texture, 48, 0, 16, 16);
    focusedFrame = new FlixelFrame(texture, 64, 0, 16, 16);
    full = new FlixelUiButtonStyle();
    full.up = new FlixelUiImage(upFrame);
    full.over = new FlixelUiImage(overFrame);
    full.down = new FlixelUiImage(downFrame);
    full.disabled = new FlixelUiImage(disabledFrame);
    full.focused = new FlixelUiImage(focusedFrame);
  }

  @Test
  void visualStatesFollowTheirPriority() {
    FlixelUiButton button = new FlixelUiButton("");
    button.setStyle(full);
    button.setSize(100, 40);

    assertSame(upFrame, drawnBackground(button));
    button.focus();
    assertSame(focusedFrame, drawnBackground(button));
    button.hover();
    assertSame(overFrame, drawnBackground(button), "hovered beats focused");
    button.press();
    assertSame(downFrame, drawnBackground(button), "pressed beats hovered and focused");
    button.setEnabled(false);
    assertSame(disabledFrame, drawnBackground(button), "disabled beats everything");
    button.setEnabled(true);
    assertSame(upFrame, drawnBackground(button), "disabling cleared the other states");
  }

  @Test
  void missingBackgroundsAndColorsFallBack() {
    FlixelUiButtonStyle minimal = new FlixelUiButtonStyle();
    minimal.up = new FlixelUiImage(upFrame);
    FlixelColor red = new FlixelColor(FlixelColor.RED);
    FlixelColor green = new FlixelColor(FlixelColor.GREEN);
    minimal.fontColor = red;
    minimal.overFontColor = green;
    FlixelUiButton button = new FlixelUiButton("");
    button.setStyle(minimal);
    button.setSize(50, 20);

    button.focus();
    assertSame(upFrame, drawnBackground(button));
    assertSame(red, button.getFontColor());
    button.hover();
    assertSame(upFrame, drawnBackground(button));
    assertSame(green, button.getFontColor());
    button.press();
    assertSame(upFrame, drawnBackground(button));
    assertSame(red, button.getFontColor(), "no down color, so the normal color");
    button.setEnabled(false);
    assertSame(upFrame, drawnBackground(button));
    assertSame(red, button.getFontColor());

    minimal.fontColor = null;
    assertSame(FlixelColor.WHITE, button.getFontColor(), "no font color at all means white");

    FlixelUiButton unstyled = new FlixelUiButton("");
    assertNull(unstyled.getBackground());
    assertSame(FlixelColor.WHITE, unstyled.getFontColor());
    batch.reset();
    unstyled.setSize(10, 10);
    unstyled.drawSelf(batch, 0, 0, 1f);
    assertEquals(0, batch.getDrawCount(), "no style, no background");
  }

  @Test
  void clickFiresOnlyWhileEnabledAndLeavesThePressedStateAlone() {
    int[] clicks = new int[1];
    FlixelUiButton button = new FlixelUiButton("Play", b -> clicks[0]++);
    button.click();
    assertEquals(1, clicks[0]);
    assertFalse(button.isPressed(), "click() does not press");

    button.press();
    button.click();
    assertEquals(2, clicks[0]);
    assertTrue(button.isPressed(), "click() does not release");

    button.setEnabled(false);
    button.click();
    assertEquals(2, clicks[0], "a disabled button ignores click()");

    button.setEnabled(true);
    FlixelUiButton[] payload = new FlixelUiButton[1];
    button.onClick.add(b -> payload[0] = b);
    button.click();
    assertSame(button, payload[0]);
    assertEquals(3, clicks[0]);
  }

  @Test
  void preferredSizeIsPaddingPlusContentButAtLeastTheBackgroundMinimum() {
    FlixelFrame iconFrame = new FlixelFrame(texture, 0, 64, 20, 10);
    FlixelUiButtonStyle style = new FlixelUiButtonStyle();
    style.up = new FlixelNineSlice(new FlixelFrame(texture, 0, 128, 64, 64), 20, 20, 20, 20);
    style.padLeft = 5;
    style.padTop = 6;
    style.padRight = 7;
    style.padBottom = 8;
    FlixelUiButton button = new FlixelUiButton(iconFrame, null);
    button.setStyle(style);

    // Content is 20 x 10, so padding plus content is 32 x 24, below the 40 x 40 minimum.
    assertEquals(40f, button.getPreferredWidth(), EPS);
    assertEquals(40f, button.getPreferredHeight(), EPS);

    style.up = new FlixelNineSlice(new FlixelFrame(texture, 0, 128, 64, 64), 2, 2, 2, 2);
    assertEquals(32f, button.getPreferredWidth(), EPS);
    assertEquals(24f, button.getPreferredHeight(), EPS);

    // The background of the current state counts, not only the up background.
    style.over = new FlixelNineSlice(new FlixelFrame(texture, 64, 128, 64, 64), 25, 25, 25, 25);
    button.hover();
    assertEquals(50f, button.getPreferredWidth(), EPS);
    assertEquals(50f, button.getPreferredHeight(), EPS);
    button.unhover();

    // On a display, an auto-sized button takes its preferred size on layout.
    FlixelUiDisplay ui = new FlixelUiDisplay(new FlixelCamera(640, 360));
    ui.add(button);
    ui.layout();
    assertEquals(32f, button.getWidth(), EPS);
    assertEquals(24f, button.getHeight(), EPS);

    button.hover(); // Shows the larger "over" background, which needs a new layout.
    ui.update(0f);
    assertEquals(50f, button.getWidth(), EPS);

    button.setSize(120, 30);
    ui.layout();
    assertFalse(button.isAutoWidth());
    assertEquals(120f, button.getPreferredWidth(), EPS);
    assertEquals(120f, button.getWidth(), EPS);
    assertEquals(30f, button.getHeight(), EPS);
  }

  @Test
  void contentIsCenteredAndSinksWhilePressed() {
    FlixelFrame iconFrame = new FlixelFrame(texture, 0, 64, 20, 10);
    FlixelUiButtonStyle style = new FlixelUiButtonStyle();
    style.up = new FlixelUiImage(upFrame);
    style.pressedOffsetY = 2;
    FlixelUiButton button = new FlixelUiButton(iconFrame, null);
    button.setStyle(style);
    button.setSize(100, 40);

    batch.reset();
    button.drawSelf(batch, 10, 20, 0.5f);
    assertEquals(2, batch.getDrawCount(), "background, then icon");
    assertSame(upFrame, batch.getFrame(0));
    assertEquals(10f, batch.getX(0), EPS);
    assertEquals(20f, batch.getY(0), EPS);
    assertEquals(100f, batch.getWidth(0), EPS);
    assertEquals(40f, batch.getHeight(0), EPS);
    assertEquals(0.5f, batch.getAlpha(0), EPS);
    assertSame(iconFrame, batch.getFrame(1));
    assertEquals(10f + 40f, batch.getX(1), EPS);
    assertEquals(20f + 15f, batch.getY(1), EPS);
    assertEquals(20f, batch.getWidth(1), EPS);
    assertEquals(10f, batch.getHeight(1), EPS);
    assertEquals(0.5f, batch.getAlpha(1), EPS);
    assertEquals(1f, batch.getColor().r, EPS, "the batch color is restored to white");
    assertEquals(1f, batch.getColor().a, EPS);

    button.press();
    batch.reset();
    button.drawSelf(batch, 10, 20, 1f);
    assertEquals(20f + 15f + 2f, batch.getY(1), EPS);
  }

  @Test
  void skinStyleIsResolvedWhenAttached() {
    FlixelUiSkin skin = new FlixelUiSkin();
    FlixelUiButtonStyle fab = new FlixelUiButtonStyle();
    skin.add("default", full);
    skin.add("fab", fab);
    FlixelUiDisplay ui = new FlixelUiDisplay(new FlixelCamera(640, 360), skin);

    FlixelUiButton button = new FlixelUiButton("");
    assertNull(button.getStyle());
    ui.add(button);
    assertSame(full, button.getStyle());
    button.setStyleName("fab");
    assertSame(fab, button.getStyle());

    FlixelUiButtonStyle custom = new FlixelUiButtonStyle();
    button.setStyle(custom);
    ui.setSkin(skin);
    assertSame(custom, button.getStyle(), "a style object set directly survives a skin refresh");
    button.setStyle((FlixelUiButtonStyle) null);
    assertSame(fab, button.getStyle(), "null goes back to the skin");
  }

  private FlixelFrame drawnBackground(FlixelUiButton button) {
    batch.reset();
    button.drawSelf(batch, 0, 0, 1f);
    assertTrue(batch.getDrawCount() >= 1, "a background was drawn");
    return batch.getFrame(0);
  }
}
