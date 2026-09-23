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
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.ui.skin.FlixelUiSkin;
import org.flixelgdx.ui.skin.FlixelUiStyle;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies when {@link FlixelUiWidget#onStyleChanged()} runs and what
 * {@link FlixelUiWidget#resolveStyle(Class)} returns.
 */
@ExtendWith(FlixelUiHeadlessExtension.class)
class FlixelUiStyleResolutionTest {

  private FlixelUiSkin skin;
  private BoxStyle normal;
  private BoxStyle fab;
  private FlixelUiDisplay ui;

  @BeforeEach
  void setUp() {
    skin = new FlixelUiSkin();
    normal = new BoxStyle();
    fab = new BoxStyle();
    skin.add("default", normal);
    skin.add("fab", fab);
    ui = new FlixelUiDisplay(new FlixelCamera(640, 360), skin);
  }

  @Test
  void resolvesTheDefaultStyleWhenAttached() {
    StyledWidget w = new StyledWidget();
    assertEquals(0, w.styleChanges, "nothing to resolve before the widget is on a display");
    ui.add(w);
    assertEquals(1, w.styleChanges);
    assertSame(normal, w.style);
  }

  @Test
  void resolvesAChildWhenItsContainerIsAttached() {
    FlixelUiContainer group = new FlixelUiContainer();
    StyledWidget w = new StyledWidget();
    w.setStyle("fab");
    group.add(w);
    assertEquals(0, w.styleChanges);
    ui.add(group);
    assertEquals(1, w.styleChanges);
    assertSame(fab, w.style);
  }

  @Test
  void setStyleWhileAttachedResolvesRightAway() {
    StyledWidget w = new StyledWidget();
    ui.add(w);
    w.setStyle("fab");
    assertEquals(2, w.styleChanges);
    assertSame(fab, w.style);
    assertEquals("fab", w.getStyleName());
  }

  @Test
  void setStyleWhileDetachedWaitsForAttach() {
    StyledWidget w = new StyledWidget();
    w.setStyle("fab");
    assertEquals(0, w.styleChanges);
    ui.add(w);
    assertEquals(1, w.styleChanges);
    assertSame(fab, w.style);

    ui.remove(w);
    w.setStyle("default");
    assertEquals(1, w.styleChanges, "a removed widget is no longer on a display");
  }

  @Test
  void changingTheSkinRestylesEveryAttachedWidget() {
    FlixelUiContainer group = new FlixelUiContainer();
    StyledWidget nested = new StyledWidget();
    StyledWidget top = new StyledWidget();
    group.add(nested);
    ui.add(group);
    ui.add(top);

    FlixelUiSkin other = new FlixelUiSkin();
    BoxStyle otherDefault = new BoxStyle();
    other.add("default", otherDefault);
    ui.setSkin(other);

    assertSame(other, ui.getSkin());
    assertEquals(2, nested.styleChanges);
    assertEquals(2, top.styleChanges);
    assertSame(otherDefault, nested.style);
    assertSame(otherDefault, top.style);
  }

  @Test
  void settingTheSameSkinAgainPicksUpReplacedStyles() {
    StyledWidget w = new StyledWidget();
    ui.add(w);
    BoxStyle replacement = new BoxStyle();
    skin.add("default", replacement);
    assertSame(normal, w.style, "a replaced style is not picked up by itself");
    ui.setSkin(skin);
    assertSame(replacement, w.style);
  }

  @Test
  void missingStylesFailWithTheSkinsMessage() {
    StyledWidget w = new StyledWidget();
    w.setStyle("nope");
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> ui.add(w));
    assertEquals("No BoxStyle named 'nope' in this skin.", e.getMessage());
  }

  @Test
  void resolvingWithoutADisplayFails() {
    StyledWidget w = new StyledWidget();
    assertThrows(IllegalStateException.class, () -> w.resolveStyle(BoxStyle.class));
  }

  @Test
  void theOneArgumentDisplayStartsWithAnEmptySkin() {
    FlixelUiDisplay plain = new FlixelUiDisplay(new FlixelCamera(640, 360));
    assertNotNull(plain.getSkin());
    TestWidget unstyled = new TestWidget(10, 10);
    plain.add(unstyled); // Widgets that never resolve a style work without one.
    assertThrows(IllegalArgumentException.class, () -> plain.add(new StyledWidget()));
  }

  @Test
  void rejectsANullSkin() {
    FlixelCamera camera = new FlixelCamera(640, 360);
    assertThrows(IllegalArgumentException.class, () -> new FlixelUiDisplay(camera, null));
    assertThrows(IllegalArgumentException.class, () -> ui.setSkin(null));
  }

  static final class BoxStyle implements FlixelUiStyle {
  }

  /** A widget that resolves a {@link BoxStyle} whenever its style may have changed. */
  static final class StyledWidget extends FlixelUiWidget {
    BoxStyle style;
    int styleChanges;

    @Override
    protected void onStyleChanged() {
      styleChanges++;
      style = resolveStyle(BoxStyle.class);
    }

    @Override
    protected void drawSelf(@NotNull FlixelBatch batch, float drawX, float drawY, float alpha) {}
  }
}
