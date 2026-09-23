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

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.text.FlixelFont;
import org.flixelgdx.text.FlixelText;
import org.flixelgdx.ui.graphics.RecordingBatch;
import org.flixelgdx.ui.skin.FlixelUiButtonStyle;
import org.flixelgdx.ui.skin.FlixelUiLabelStyle;
import org.flixelgdx.ui.skin.FlixelUiSkin;
import org.flixelgdx.util.FlixelColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link FlixelUiLabel} sizing and re-layout with a hand-built font, and that the shared
 * text part follows the display's camera.
 */
@ExtendWith(FlixelUiHeadlessExtension.class)
class FlixelUiLabelTest {

  private static final float EPS = 1e-4f;

  /** Horizontal advance of every letter glyph in the test font. */
  private static final float LETTER = 10f;

  /** Line height of the test font. */
  private static final float LINE = 12f;

  private FlixelUiSkin skin;
  private FlixelUiDisplay ui;

  @BeforeEach
  void setUp() {
    skin = new FlixelUiSkin();
    skin.add("default", new FlixelUiLabelStyle());
    skin.add("default", new FlixelUiButtonStyle());
    ui = new FlixelUiDisplay(new FlixelCamera(640, 360), skin);
  }

  @Test
  void preferredSizeIsTheMeasuredText() {
    FlixelUiLabel label = addLabel("ab");
    assertEquals(2 * LETTER, label.getPreferredWidth(), EPS);
    assertEquals(LINE, label.getPreferredHeight(), EPS);
    ui.layout();
    assertEquals(2 * LETTER, label.getWidth(), EPS);
    assertEquals(LINE, label.getHeight(), EPS);
  }

  @Test
  void relayoutHappensOnlyWhenTheSizeChanges() {
    FlixelUiLabel label = addLabel("ab");
    TestWidget sibling = new TestWidget(10, 10);
    ui.add(sibling);
    ui.layout();
    assertEquals(0f, sibling.getScreenX(), EPS);

    // Moving the sibling through its field skips invalidateLayout(), so its screen position only
    // changes when something else triggers a layout pass.
    sibling.x = 50;
    label.setText("ba");
    ui.update(0f);
    assertEquals(0f, sibling.getScreenX(), EPS, "same size, so no layout pass");

    label.setText("ab");
    label.setText("ab");
    ui.update(0f);
    assertEquals(0f, sibling.getScreenX(), EPS);

    label.setText("abcd");
    ui.update(0f);
    assertEquals(50f, sibling.getScreenX(), EPS, "a wider text lays the display out again");
    assertEquals(4 * LETTER, label.getWidth(), EPS);
    assertEquals("abcd", label.getText().toString());

    label.setText(null);
    assertEquals(0, label.getText().length());
    assertEquals(0f, label.getPreferredWidth(), EPS);
  }

  @Test
  void wrappingUsesTheWrapWidth() {
    FlixelUiLabel label = addLabel("ab cd");
    assertEquals(4 * LETTER + 6f, label.getPreferredWidth(), EPS); // Four letters and a space.
    label.setWrapWidth(30);
    assertEquals(30f, label.getPreferredWidth(), EPS);
    assertEquals(2 * LINE, label.getPreferredHeight(), EPS);
    label.setWrapWidth(0);
    assertEquals(LINE, label.getPreferredHeight(), EPS);
  }

  @Test
  void fixedSizeStopsFollowingTheText() {
    FlixelUiLabel label = addLabel("ab");
    label.setSize(100, 40);
    label.setAlignment(FlixelText.Alignment.CENTER);
    ui.layout();
    assertEquals(100f, label.getWidth(), EPS);
    assertEquals(100f, label.getPreferredWidth(), EPS);
    assertSame(FlixelText.Alignment.CENTER, label.getAlignment());
    label.setAutoSize(true);
    ui.layout();
    assertEquals(2 * LETTER, label.getWidth(), EPS);
  }

  @Test
  void labelsAreNotHitAndStyleComesFromTheSkin() {
    FlixelUiLabel label = addLabel("ab");
    ui.layout();
    assertNull(ui.getWidgetAt(5, 5));
    assertSame(skin.get(FlixelUiLabelStyle.class, "default"), label.getStyle());

    FlixelUiLabelStyle custom = new FlixelUiLabelStyle();
    custom.fontSize = 24;
    label.setStyle(custom);
    assertSame(custom, label.getStyle());
    assertEquals(24, label.getPart().getText().getTextSize());
  }

  @Test
  void textPartFollowsTheDisplayCamera() {
    FlixelUiLabel label = new FlixelUiLabel("ab");
    FlixelUiButton button = new FlixelUiButton("Go");
    FlixelUiTextPart part = label.getPart();
    assertSame(part.getCameras(), part.getText().cameras, "the list is handed over once");
    assertNull(part.getCameras()[0]);

    ui.add(label);
    ui.add(button);
    assertSame(ui.getCamera(), part.getCameras()[0]);
    assertSame(ui.getCamera(), button.getPart().getText().cameras[0]);
    assertEquals(0f, part.getText().getScrollX(), EPS);
    assertEquals(0f, part.getText().getScrollY(), EPS);

    ui.remove(label);
    assertNull(part.getCameras()[0], "a detached label is drawn on no camera");
    assertSame(part.getCameras(), part.getText().cameras);

    button.destroy();
    assertTrue(button.getPart().getText().getTextBuffer().isEmpty(), "the text object is destroyed");
  }

  @Test
  void drawsTheTextAtItsScreenPositionWithTheCombinedAlpha() {
    Flixel.cameras.clear();
    Flixel.cameras.add(ui.getCamera());
    try {
      FlixelUiLabelStyle red = new FlixelUiLabelStyle();
      red.color = new FlixelColor(FlixelColor.RED);
      FlixelUiLabel label = addLabel("ab");
      label.setStyle(red);
      label.setPosition(30, 40);
      ui.layout();

      RecordingBatch batch = new RecordingBatch();
      label.drawSelf(batch, 30, 40, 0.5f);
      assertEquals(2, batch.getDrawCount(), "one quad per glyph");
      assertEquals(30f, batch.getX(0), EPS);
      assertEquals(30f + LETTER, batch.getX(1), EPS);
      assertEquals(1f, batch.getRed(0), EPS);
      assertEquals(0.5f, batch.getAlpha(0), EPS);
      assertEquals(1f, label.getPart().getText().getAlpha(), EPS, "the text's own alpha is restored");
      assertEquals(1f, batch.getColor().a, EPS, "the batch color is back to white");
    } finally {
      Flixel.cameras.clear();
    }
  }

  /**
   * Adds a label to the display and gives its text the hand-built test font.
   */
  private FlixelUiLabel addLabel(String text) {
    FlixelUiLabel label = new FlixelUiLabel(text);
    ui.add(label);
    // Set after attaching, so the style (with no font of its own) does not replace it.
    label.getPart().getText().setFont(buildFont());
    return label;
  }

  /**
   * Builds a small font by hand: letters a to d advance 10 pixels, the space advances 6, and a
   * line is 12 pixels tall. No image is needed to measure text.
   */
  private static FlixelFont buildFont() {
    String fnt = "common lineHeight=" + (int) LINE + " base=10 scaleW=64 scaleH=64\n"
        + "char id=32 x=0 y=0 width=0 height=0 xoffset=0 yoffset=0 xadvance=6 page=0\n"
        + "char id=97 x=0 y=0 width=8 height=8 xoffset=0 yoffset=0 xadvance=10 page=0\n"
        + "char id=98 x=8 y=0 width=8 height=8 xoffset=0 yoffset=0 xadvance=10 page=0\n"
        + "char id=99 x=16 y=0 width=8 height=8 xoffset=0 yoffset=0 xadvance=10 page=0\n"
        + "char id=100 x=24 y=0 width=8 height=8 xoffset=0 yoffset=0 xadvance=10 page=0\n";
    return FlixelFont.fromFnt(fnt, null);
  }
}
