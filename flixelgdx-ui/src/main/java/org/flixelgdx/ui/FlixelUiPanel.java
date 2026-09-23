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

import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.ui.graphics.FlixelUiBackground;
import org.flixelgdx.ui.skin.FlixelUiPanelStyle;
import org.flixelgdx.ui.skin.FlixelUiSkin;
import org.flixelgdx.util.FlixelColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A container that draws a background behind its children, such as a window, a card, or a
 * sidebar.
 *
 * <p>Think of a panel as a tray: the tray itself is what you see (its background), and whatever is
 * placed on it moves with it. Children are laid out exactly like in any {@link FlixelUiContainer}
 * (anchors, percent sizes, or a {@link FlixelUiStack} inside), inside the panel's padding.
 *
 * <pre>{@code
 * FlixelUiPanel panel = new FlixelUiPanel(320, 240);
 * panel.anchor(FlixelAlign.CENTER, 0, 0);
 *
 * FlixelUiStack column = new FlixelUiStack(FlixelUiStack.Direction.VERTICAL);
 * column.setSpacing(8);
 * column.setPercentSize(1f, 1f);
 * column.add(new FlixelUiLabel("Settings"));
 * column.add(new FlixelUiButton("Back", b -> Flixel.switchState(new MenuState())));
 *
 * panel.add(column);
 * ui.add(panel);
 * }</pre>
 *
 * <h2>Style and padding</h2>
 *
 * <p>The background and padding come from the {@link FlixelUiPanelStyle} in the display's
 * {@link FlixelUiSkin} whose name matches {@link #getStyleName()}, or from a style object passed
 * to {@link #setStyle(FlixelUiPanelStyle)}. A style with no background draws nothing, which is
 * handy for an invisible click blocker.
 *
 * <p>The style's padding is used until the game sets its own: once {@link #setPadding(float)} or
 * {@link #setPadding(float, float, float, float)} is called, that padding wins and later style
 * changes no longer touch it. This lets a skin give every panel sensible padding while one special
 * panel keeps a hand-picked value.
 *
 * <h2>Blocking clicks</h2>
 *
 * <p>Unlike a plain container, a panel is {@link #interactive}: when
 * {@link FlixelUiDisplay#getWidgetAt(float, float)} finds no child under a point inside the panel,
 * it returns the panel itself. Clicking the empty part of a window therefore never reaches the
 * buttons hidden behind it. Set {@link #interactive} to {@code false} to let clicks through.
 */
public class FlixelUiPanel extends FlixelUiContainer {

  /** The style in use, or {@code null} before one is resolved. */
  @Nullable
  private FlixelUiPanelStyle style;

  /** Whether {@link #style} was set with {@link #setStyle(FlixelUiPanelStyle)} instead of the skin. */
  boolean customStyle;

  /** Whether the game set the padding itself, so the style's padding no longer applies. */
  private boolean customPadding;

  /** Creates an empty panel with a size of zero. */
  public FlixelUiPanel() {
    this(0f, 0f);
  }

  /**
   * Creates an empty panel with the given size.
   *
   * @param width The starting width in pixels.
   * @param height The starting height in pixels.
   */
  public FlixelUiPanel(float width, float height) {
    super(width, height);
    interactive = true;
  }

  /**
   * Sets the padding on all four sides, overriding the style's padding from now on.
   *
   * @param left The left padding in pixels.
   * @param top The top padding in pixels.
   * @param right The right padding in pixels.
   * @param bottom The bottom padding in pixels.
   */
  @Override
  public void setPadding(float left, float top, float right, float bottom) {
    customPadding = true;
    super.setPadding(left, top, right, bottom);
  }

  /**
   * Resolves this panel's style from the skin, unless a style object was set directly, and applies
   * its padding unless the game set its own.
   *
   * @throws IllegalArgumentException If the skin has no {@link FlixelUiPanelStyle} with this panel's
   *     style name.
   */
  @Override
  protected void onStyleChanged() {
    if (!customStyle) {
      style = resolveStyle(FlixelUiPanelStyle.class);
    }
    applyStyle();
  }

  /**
   * Draws the style's background over the panel's whole rectangle.
   *
   * @param batch The batch to draw into.
   * @param drawX The left edge in draw coordinates.
   * @param drawY The top edge in draw coordinates.
   * @param alpha The combined alpha of this panel and its ancestors.
   */
  @Override
  protected void drawSelf(@NotNull FlixelBatch batch, float drawX, float drawY, float alpha) {
    FlixelUiPanelStyle s = style;
    FlixelUiBackground bg = s != null ? s.background : null;
    if (bg == null) {
      return;
    }
    FlixelColor tint = getColor();
    batch.setColor(tint.r, tint.g, tint.b, alpha);
    bg.draw(batch, drawX, drawY, width, height);
    batch.setColor(FlixelColor.WHITE);
  }

  /**
   * Destroys this panel and every child in it.
   */
  @Override
  public void destroy() {
    super.destroy();
    style = null;
  }

  /** Copies the style's padding into the container, unless the game set its own padding. */
  private void applyStyle() {
    FlixelUiPanelStyle s = style;
    if (s != null && !customPadding) {
      super.setPadding(s.padLeft, s.padTop, s.padRight, s.padBottom);
    }
    invalidateLayout();
  }

  /**
   * Returns the style this panel uses.
   *
   * @return The style, or {@code null} while the panel is not on a display and no style object was
   *     set.
   */
  @Nullable
  public FlixelUiPanelStyle getStyle() {
    return style;
  }

  /**
   * Makes this panel use a style object directly instead of looking one up in the skin.
   *
   * <p>Passing {@code null} goes back to the skin's style named {@link #getStyleName()} (right away
   * while on a display, otherwise when added to one). The style's padding is applied unless the
   * game already set its own.
   *
   * @param style The style to use, or {@code null} to use the skin again.
   * @throws IllegalArgumentException If {@code style} is {@code null}, the panel is on a display, and
   *     the skin has no matching {@link FlixelUiPanelStyle}.
   */
  public void setStyle(@Nullable FlixelUiPanelStyle style) {
    customStyle = style != null;
    this.style = style;
    if (style == null && display != null) {
      this.style = resolveStyle(FlixelUiPanelStyle.class);
    }
    applyStyle();
  }

  /**
   * Returns whether the game set the padding itself, so the style's padding is ignored.
   *
   * @return {@code true} after {@link #setPadding(float, float, float, float)} was called.
   */
  public boolean isCustomPadding() {
    return customPadding;
  }
}
