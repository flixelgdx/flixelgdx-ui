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

import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelIdentityMap;
import org.flixelgdx.collections.FlixelMap;
import org.flixelgdx.functional.FlixelDestroyable;
import org.flixelgdx.ui.FlixelUiDisplay;
import org.flixelgdx.ui.FlixelUiWidget;
import org.flixelgdx.ui.graphics.FlixelNineSlice;
import org.flixelgdx.ui.graphics.FlixelUiBackground;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A collection of named widget styles, sorted by the kind of widget they dress.
 *
 * <p>Think of a skin as a wardrobe with one drawer per kind of widget. The button drawer holds
 * button outfits, the checkbox drawer holds checkbox outfits, and each outfit has a name tag such
 * as {@code "default"} or {@code "fab"}. When a widget is added to a {@link FlixelUiDisplay}, it
 * opens its own drawer in the display's skin and puts on the outfit whose name matches
 * {@link FlixelUiWidget#getStyleName()}. Two drawers can each hold an outfit called
 * {@code "default"} without any clash.
 *
 * <h2>Bring your own assets</h2>
 *
 * <p>The extension ships no built-in skin and no images. A game loads its own nine-slices, images,
 * and font files, fills in style objects, and adds them here. The skin itself is plain data:
 * building one does not touch the GPU until something is drawn.
 *
 * <pre>{@code
 * FlixelUiSkin skin = new FlixelUiSkin();
 *
 * // Load each image once. track(...) makes the skin destroy it in skin.destroy().
 * FlixelNineSlice up = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/button.png"), 6, 6, 6, 6));
 * FlixelNineSlice over = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/button_over.png"), 6, 6, 6, 6));
 * FlixelNineSlice round = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/fab.png"), 24, 24, 24, 24));
 *
 * FlixelUiButtonStyle normal = new FlixelUiButtonStyle();
 * normal.up = up;
 * normal.over = over;
 * normal.font = Flixel.files.internal("fonts/pixel.ttf");
 * normal.fontSize = 16;
 * skin.add("default", normal);
 *
 * FlixelUiButtonStyle fab = new FlixelUiButtonStyle();
 * fab.up = round;
 * fab.over = over; // Shared with "default"; tracked once, destroyed once.
 * fab.font = normal.font;
 * fab.fontSize = 24;
 * skin.add("fab", fab);
 *
 * FlixelUiDisplay ui = new FlixelUiDisplay(FlixelUiDisplay.createHudCamera(), skin);
 * add(ui);
 *
 * FlixelUiButton plus = new FlixelUiButton(...);
 * plus.setStyle("fab"); // Wears the "fab" button style.
 * ui.add(plus);
 * }</pre>
 *
 * <h2>Who destroys the backgrounds</h2>
 *
 * <p>Styles are plain objects, and several styles often share the same background, so the skin
 * cannot see which backgrounds a style uses. Instead, hand each background you load to
 * {@link #track(FlixelUiBackground)}: {@link #destroy()} destroys every tracked background exactly
 * once, however many styles share it. Backgrounds you do not track are yours to destroy.
 *
 * <p>Lookups are allocation-free. Styles are keyed by their exact class, so a subclass of a style
 * lives in its own drawer.
 *
 * @see FlixelUiStyle
 */
public class FlixelUiSkin implements FlixelDestroyable {

  /** One map of named styles per style class. */
  @NotNull
  private final FlixelIdentityMap<Class<?>, FlixelMap<String, FlixelUiStyle>> styles = new FlixelIdentityMap<>();

  /** Backgrounds destroyed by {@link #destroy()}, each stored once. */
  @NotNull
  private final FlixelArray<FlixelUiBackground> tracked = new FlixelArray<>(FlixelUiBackground[]::new);

  /**
   * Adds a style under a name, replacing any style of the same class that already has that name.
   *
   * <p>The style is filed under its own class, so {@code add("default", buttonStyle)} and
   * {@code add("default", checkboxStyle)} do not replace each other. Widgets already on a display
   * pick up a replaced style the next time their style is resolved, for example after
   * {@link FlixelUiDisplay#setSkin(FlixelUiSkin)}.
   *
   * @param name The style name, such as {@code "default"}.
   * @param style The style to add.
   * @param <T> The style's type.
   * @throws IllegalArgumentException If {@code name} or {@code style} is {@code null}.
   */
  public <T extends FlixelUiStyle> void add(@NotNull String name, @NotNull T style) {
    if (name == null) {
      throw new IllegalArgumentException("Style name must not be null.");
    }
    if (style == null) {
      throw new IllegalArgumentException("Cannot add a null style.");
    }
    Class<?> type = style.getClass();
    FlixelMap<String, FlixelUiStyle> named = styles.get(type);
    if (named == null) {
      named = new FlixelMap<>();
      styles.put(type, named);
    }
    named.put(name, style);
  }

  /**
   * Returns the style of a class with a name.
   *
   * <pre>{@code
   * FlixelUiButtonStyle fab = skin.get(FlixelUiButtonStyle.class, "fab");
   * }</pre>
   *
   * @param type The exact style class.
   * @param name The style name.
   * @param <T> The style's type.
   * @return The style; never {@code null}.
   * @throws IllegalArgumentException If no style of that class has that name, with a message such
   *     as {@code No FlixelUiButtonStyle named 'fab' in this skin}.
   */
  @NotNull
  public <T extends FlixelUiStyle> T get(@NotNull Class<T> type, @NotNull String name) {
    FlixelUiStyle style = find(type, name);
    if (style == null) {
      throw new IllegalArgumentException(
          "No " + (type != null ? type.getSimpleName() : "null") + " named '" + name + "' in this skin.");
    }
    // Safe: add(...) files every style under its own runtime class, so anything stored under the
    // key "type" is an instance of exactly that class.
    @SuppressWarnings("unchecked")
    T typed = (T) style;
    return typed;
  }

  /**
   * Checks whether a style of a class with a name exists.
   *
   * @param type The exact style class.
   * @param name The style name.
   * @return {@code true} if {@link #get(Class, String)} would find it.
   */
  public boolean has(@Nullable Class<?> type, @Nullable String name) {
    return find(type, name) != null;
  }

  /**
   * Removes the style of a class with a name.
   *
   * <p>Backgrounds the style used are not destroyed; tracked ones are still destroyed by
   * {@link #destroy()}.
   *
   * @param type The exact style class.
   * @param name The style name.
   * @return {@code true} if a style was found and removed.
   */
  public boolean remove(@Nullable Class<?> type, @Nullable String name) {
    if (type == null || name == null) {
      return false;
    }
    FlixelMap<String, FlixelUiStyle> named = styles.get(type);
    return named != null && named.remove(name) != null;
  }

  /**
   * Hands a background to this skin so {@link #destroy()} destroys it.
   *
   * <p>Tracking the same background twice stores it once, so a background shared by many styles
   * is destroyed exactly once. Returns its argument so loading and tracking fit on one line:
   *
   * <pre>{@code
   * style.up = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/button.png"), 6, 6, 6, 6));
   * }</pre>
   *
   * @param background The background to track, such as a {@link FlixelNineSlice}.
   * @param <T> The background's type.
   * @return The same background.
   * @throws IllegalArgumentException If {@code background} is {@code null}.
   */
  @NotNull
  public <T extends FlixelUiBackground> T track(@NotNull T background) {
    if (background == null) {
      throw new IllegalArgumentException("Cannot track a null background.");
    }
    if (!tracked.contains(background, true)) {
      tracked.add(background);
    }
    return background;
  }

  /**
   * Destroys every {@linkplain #track(FlixelUiBackground) tracked} background once and removes
   * every style.
   *
   * <p>Call it when the UI that uses this skin is gone for good, for example when leaving a state.
   * Untracked backgrounds are left alone. The skin is empty afterwards and can be filled again.
   */
  @Override
  public void destroy() {
    FlixelUiBackground[] items = tracked.getItems();
    for (int i = 0, n = tracked.getSize(); i < n; i++) {
      items[i].destroy();
    }
    tracked.clear();
    styles.clear();
  }

  /**
   * Looks up a style without throwing.
   *
   * @param type The exact style class.
   * @param name The style name.
   * @return The style, or {@code null} when there is none.
   */
  @Nullable
  private FlixelUiStyle find(@Nullable Class<?> type, @Nullable String name) {
    if (type == null || name == null) {
      return null;
    }
    FlixelMap<String, FlixelUiStyle> named = styles.get(type);
    return named != null ? named.get(name) : null;
  }
}
