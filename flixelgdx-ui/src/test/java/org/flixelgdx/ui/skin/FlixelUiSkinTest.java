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

import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.ui.graphics.FlixelUiBackground;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies how {@link FlixelUiSkin} stores, finds, and removes styles and destroys tracked
 * backgrounds.
 */
class FlixelUiSkinTest {

  @Test
  void addsAndFindsStylesByClassAndName() {
    FlixelUiSkin skin = new FlixelUiSkin();
    ButtonStyle normal = new ButtonStyle();
    ButtonStyle fab = new ButtonStyle();
    skin.add("default", normal);
    skin.add("fab", fab);
    assertSame(normal, skin.get(ButtonStyle.class, "default"));
    assertSame(fab, skin.get(ButtonStyle.class, "fab"));
    assertTrue(skin.has(ButtonStyle.class, "fab"));
    assertFalse(skin.has(ButtonStyle.class, "missing"));
    assertFalse(skin.has(CheckStyle.class, "default"));
    assertFalse(skin.has(null, "default"));
    assertFalse(skin.has(ButtonStyle.class, null));
  }

  @Test
  void keepsTheSameNameSeparateAcrossStyleClasses() {
    FlixelUiSkin skin = new FlixelUiSkin();
    ButtonStyle button = new ButtonStyle();
    CheckStyle check = new CheckStyle();
    skin.add("default", button);
    skin.add("default", check);
    assertSame(button, skin.get(ButtonStyle.class, "default"));
    assertSame(check, skin.get(CheckStyle.class, "default"));
  }

  @Test
  void replacesAStyleWithTheSameClassAndName() {
    FlixelUiSkin skin = new FlixelUiSkin();
    ButtonStyle first = new ButtonStyle();
    ButtonStyle second = new ButtonStyle();
    skin.add("default", first);
    skin.add("default", second);
    assertSame(second, skin.get(ButtonStyle.class, "default"));
  }

  @Test
  void looksUpStylesByTheirExactClass() {
    FlixelUiSkin skin = new FlixelUiSkin();
    FancyButtonStyle fancy = new FancyButtonStyle();
    skin.add("default", fancy);
    assertSame(fancy, skin.get(FancyButtonStyle.class, "default"));
    assertFalse(skin.has(ButtonStyle.class, "default"));
  }

  @Test
  void explainsWhichStyleIsMissing() {
    FlixelUiSkin skin = new FlixelUiSkin();
    skin.add("default", new ButtonStyle());
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> skin.get(ButtonStyle.class, "fab"));
    assertEquals("No ButtonStyle named 'fab' in this skin.", e.getMessage());
    assertThrows(IllegalArgumentException.class, () -> skin.get(CheckStyle.class, "default"));
  }

  @Test
  void removesOnlyTheMatchingStyle() {
    FlixelUiSkin skin = new FlixelUiSkin();
    skin.add("default", new ButtonStyle());
    skin.add("default", new CheckStyle());
    assertTrue(skin.remove(ButtonStyle.class, "default"));
    assertFalse(skin.remove(ButtonStyle.class, "default"));
    assertFalse(skin.has(ButtonStyle.class, "default"));
    assertTrue(skin.has(CheckStyle.class, "default"));
    assertFalse(skin.remove(null, "default"));
  }

  @Test
  void rejectsNullArguments() {
    FlixelUiSkin skin = new FlixelUiSkin();
    assertThrows(IllegalArgumentException.class, () -> skin.add(null, new ButtonStyle()));
    assertThrows(IllegalArgumentException.class, () -> skin.add("default", null));
    assertThrows(IllegalArgumentException.class, () -> skin.track(null));
  }

  @Test
  void destroysEachTrackedBackgroundOnce() {
    FlixelUiSkin skin = new FlixelUiSkin();
    CountingBackground shared = new CountingBackground();
    CountingBackground untracked = new CountingBackground();
    ButtonStyle a = new ButtonStyle();
    ButtonStyle b = new ButtonStyle();
    a.up = skin.track(shared);
    b.up = skin.track(shared);
    b.over = untracked;
    skin.add("a", a);
    skin.add("b", b);
    skin.destroy();
    assertEquals(1, shared.destroyed);
    assertEquals(0, untracked.destroyed);
    assertFalse(skin.has(ButtonStyle.class, "a"), "destroy() empties the skin");
    skin.destroy();
    assertEquals(1, shared.destroyed, "a destroyed skin no longer tracks anything");
  }

  @Test
  void trackReturnsItsArgument() {
    FlixelUiSkin skin = new FlixelUiSkin();
    CountingBackground bg = new CountingBackground();
    assertSame(bg, skin.track(bg));
  }

  static class ButtonStyle implements FlixelUiStyle {
    FlixelUiBackground up;
    FlixelUiBackground over;
  }

  static final class FancyButtonStyle extends ButtonStyle {
  }

  static final class CheckStyle implements FlixelUiStyle {
  }

  static final class CountingBackground implements FlixelUiBackground {
    int destroyed;

    @Override
    public void draw(@NotNull FlixelBatch batch, float x, float y, float width, float height) {}

    @Override
    public float getMinWidth() {
      return 0f;
    }

    @Override
    public float getMinHeight() {
      return 0f;
    }

    @Override
    public void destroy() {
      destroyed++;
    }
  }
}
