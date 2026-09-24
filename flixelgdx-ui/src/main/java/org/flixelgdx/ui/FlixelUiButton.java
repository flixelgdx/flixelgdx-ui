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
import org.flixelgdx.file.FlixelFile;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.ui.graphics.FlixelUiBackground;
import org.flixelgdx.ui.graphics.FlixelUiImage;
import org.flixelgdx.ui.skin.FlixelUiButtonStyle;
import org.flixelgdx.ui.skin.FlixelUiSkin;
import org.flixelgdx.util.FlixelColor;
import org.flixelgdx.util.FlixelString;
import org.flixelgdx.util.signal.FlixelSignal;
import org.flixelgdx.util.signal.FlixelSignal.SignalHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A clickable widget that shows text, an icon, or both, and changes its look as the game hovers,
 * presses, focuses, or disables it.
 *
 * <p>Think of a button as a doorbell. Pressing it (with {@link #press()}) makes it look pushed in,
 * but the bell only rings when the game calls {@link #click()}, which dispatches {@link #onClick}.
 * The button never reads the mouse, keyboard, or gamepad itself: the game decides when each of
 * those things happens and calls the matching method.
 *
 * <pre>{@code
 * FlixelUiButton play = new FlixelUiButton("Play", b -> Flixel.switchState(new PlayState()));
 * play.anchor(FlixelAlign.CENTER, 0, 0);
 * play.onHover.add(b -> Flixel.sound.play(Flixel.files.internal("sfx/hover.ogg")));
 * ui.add(play);
 *
 * // In the game's own input code (see the package documentation for full mouse wiring):
 * if (Flixel.mouse.justReleased(FlixelMouseButton.LEFT) && ui.getWidgetAt(mx, my) == play) {
 *   play.release(); // Back to the hovered look.
 *   play.click();   // Rings the bell: dispatches onClick.
 * }
 *
 * // From a gamepad or keyboard, through the game's own action set:
 * if (controls.accept.justPressed() && ui.getFocused() instanceof FlixelUiButton b) {
 *   b.click();
 * }
 *
 * play.setEnabled(false); // Disabled look; click() is ignored until it is enabled again.
 * }</pre>
 *
 * <h2>Looks</h2>
 *
 * <p>The button's backgrounds, font, text colors, and padding come from the
 * {@link FlixelUiButtonStyle} in the display's {@link FlixelUiSkin} whose name matches
 * {@link #getStyleName()}, or from a style object passed to {@link #setStyle(FlixelUiButtonStyle)}.
 * Exactly one look is shown at a time, picked in this order: disabled, then pressed (the style's
 * {@code down}), then hovered ({@code over}), then {@code focused}, then {@code up}. A state whose
 * background or text color is missing from the style uses the {@code up} background or the normal
 * font color.
 *
 * <p>The content (icon and text) is centered inside the padding, with the icon on the left and the
 * style's {@code iconSpacing} between the two. While pressed, it moves down by the style's
 * {@code pressedOffsetY}.
 *
 * <h2>Size</h2>
 *
 * <p>By default, the button sizes itself on every layout pass: the padding plus its content, but
 * never smaller than the minimum size of the background it is currently showing (so a nine-slice's
 * corners never overlap). Giving it a size with {@link #setSize(float, float)},
 * {@link #setWidth(float)}, or {@link #setHeight(float)} fixes that side instead, and a
 * {@linkplain #setPercentSize(float, float) percent size} or a {@link FlixelUiStack} can size it
 * too. The content stays centered in whatever size it ends up with.
 *
 * <h2>Floating action buttons</h2>
 *
 * <p>A floating action button (a round button hovering over a corner of the screen, usually with a
 * single "+" icon) is not a separate class. It is an icon-only button that wears a round style,
 * usually named {@code "fab"}, and is {@linkplain #anchor(int, float, float) anchored} to a corner:
 *
 * <pre>{@code
 * // Once, while building the skin:
 * FlixelUiButtonStyle fabStyle = new FlixelUiButtonStyle();
 * fabStyle.up = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/fab.png"), 24, 24, 24, 24));
 * fabStyle.padLeft = fabStyle.padTop = fabStyle.padRight = fabStyle.padBottom = 16;
 * skin.add("fab", fabStyle);
 *
 * // In the state:
 * FlixelUiButton fab = new FlixelUiButton(Flixel.files.internal("ui/plus.png"), b -> openCreateDialog());
 * fab.setStyle("fab");
 * fab.anchor(FlixelAlign.BOTTOM_RIGHT, -24, -24); // 24 pixels in from the bottom-right corner.
 * ui.add(fab);
 * }</pre>
 *
 * <h2>Icons</h2>
 *
 * <p>An icon is drawn at its image's own size. An icon given as a {@link FlixelFile} is loaded by
 * the button through {@link Flixel#assets} and released when the icon is replaced or the button is
 * destroyed. An icon given as a {@link FlixelFrame} (for example from a texture atlas) belongs to
 * the caller and is never released by the button.
 */
public class FlixelUiButton extends FlixelUiWidget {

  /**
   * Dispatched by {@link #click()} while the button is enabled.
   *
   * <p>The payload is the button itself, so one listener can serve several buttons without
   * allocating anything.
   */
  public final FlixelSignal<FlixelUiButton> onClick = new FlixelSignal<>();

  /** The caption and its font settings. */
  @NotNull
  private final FlixelUiTextPart part = new FlixelUiTextPart();

  /** The icon, or {@code null} for none. Destroyed when replaced; only releases what it loaded. */
  @Nullable
  private FlixelUiImage icon;

  /** The style in use, or {@code null} before one is resolved. */
  @Nullable
  private FlixelUiButtonStyle style;

  /** The background the last layout pass measured against, used to spot min size changes. */
  @Nullable
  private FlixelUiBackground measuredBackground;

  /** Whether {@link #style} was set with {@link #setStyle(FlixelUiButtonStyle)} instead of the skin. */
  private boolean customStyle;

  private boolean autoWidth = true;
  private boolean autoHeight = true;

  /**
   * Creates a text button with no click listener yet.
   *
   * @param text The caption; copied. {@code null} shows no text.
   */
  public FlixelUiButton(@Nullable CharSequence text) {
    this(text, (FlixelUiImage) null, null);
  }

  /**
   * Creates a text button.
   *
   * @param text The caption; copied. {@code null} shows no text.
   * @param onClick Added to {@link #onClick} when not {@code null}.
   */
  public FlixelUiButton(@Nullable CharSequence text, @Nullable SignalHandler<FlixelUiButton> onClick) {
    this(text, (FlixelUiImage) null, onClick);
  }

  /**
   * Creates an icon-only button, loading the icon from a file.
   *
   * <p>This is the usual way to make a floating action button; see the class documentation.
   *
   * @param icon The icon image, such as {@code Flixel.files.internal("ui/plus.png")}. The button
   *     holds a reference to it until the icon is replaced or the button is destroyed.
   * @param onClick Added to {@link #onClick} when not {@code null}.
   * @throws IllegalArgumentException If {@code icon} is {@code null}.
   */
  public FlixelUiButton(@NotNull FlixelFile icon, @Nullable SignalHandler<FlixelUiButton> onClick) {
    this(null, requireIcon(icon), onClick);
  }

  /**
   * Creates an icon-only button from a frame, such as one from a texture atlas.
   *
   * @param icon The icon frame. It is not owned: the button never releases its texture.
   * @param onClick Added to {@link #onClick} when not {@code null}.
   * @throws IllegalArgumentException If {@code icon} is {@code null} or was packed rotated.
   */
  public FlixelUiButton(@NotNull FlixelFrame icon, @Nullable SignalHandler<FlixelUiButton> onClick) {
    this((CharSequence) null, new FlixelUiImage(icon), onClick);
  }

  /**
   * Creates a button with an icon on the left and text on the right.
   *
   * @param text The caption; copied. {@code null} shows no text.
   * @param icon The icon image. The button holds a reference to it until the icon is replaced or
   *     the button is destroyed.
   * @param onClick Added to {@link #onClick} when not {@code null}.
   * @throws IllegalArgumentException If {@code icon} is {@code null}.
   */
  public FlixelUiButton(@Nullable CharSequence text, @NotNull FlixelFile icon,
      @Nullable SignalHandler<FlixelUiButton> onClick) {
    this(text, requireIcon(icon), onClick);
  }

  /**
   * Creates a button from an already built icon.
   *
   * @param text The caption, or {@code null} for none.
   * @param icon The icon, or {@code null} for none.
   * @param onClick Added to {@link #onClick} when not {@code null}.
   */
  private FlixelUiButton(@Nullable CharSequence text, @Nullable FlixelUiImage icon,
      @Nullable SignalHandler<FlixelUiButton> onClick) {
    part.setText(text);
    this.icon = icon;
    if (onClick != null) {
      this.onClick.add(onClick);
    }
  }

  /**
   * Activates the button, dispatching {@link #onClick}.
   *
   * <p>Does nothing while the button is disabled. It does not change the pressed state: a game
   * that wants the pushed-in look calls {@link #press()} and {@link #release()} itself, for example
   * when a mouse button goes down and comes back up, and calls {@code click()} on the release.
   */
  public void click() {
    if (!isEnabled()) {
      return;
    }
    onClick.dispatch(this);
  }

  /**
   * Resolves this button's style from the skin, unless a style object was set directly, and
   * applies its font settings.
   *
   * @throws IllegalArgumentException If the skin has no {@link FlixelUiButtonStyle} with this
   *     button's style name.
   */
  @Override
  protected void onStyleChanged() {
    if (!customStyle) {
      style = resolveStyle(FlixelUiButtonStyle.class);
    }
    applyStyle();
  }

  /**
   * Recomputes the layout when the new state shows a background with a different minimum size,
   * since the preferred size depends on it.
   */
  @Override
  protected void onStateChanged() {
    FlixelUiBackground bg = getBackground();
    FlixelUiBackground old = measuredBackground;
    if (bg == old) {
      return;
    }
    float oldMinW = old != null ? old.getMinWidth() : 0f;
    float oldMinH = old != null ? old.getMinHeight() : 0f;
    float minW = bg != null ? bg.getMinWidth() : 0f;
    float minH = bg != null ? bg.getMinHeight() : 0f;
    if (minW != oldMinW || minH != oldMinH) {
      invalidateLayout();
    }
  }

  @Override
  protected void onAttached(@NotNull FlixelUiDisplay display) {
    part.attach(display);
  }

  @Override
  protected void onDetached() {
    part.detach();
  }

  /** Sizes an auto-sized button to its content. */
  @Override
  protected void onMeasure() {
    measuredBackground = getBackground();
    if (autoWidth) {
      width = measureWidth();
    }
    if (autoHeight) {
      height = measureHeight();
    }
  }

  /**
   * Draws the background for the current state, then the icon and the text centered inside the
   * padding.
   *
   * @param batch The batch to draw into.
   * @param drawX The left edge in draw coordinates.
   * @param drawY The top edge in draw coordinates.
   * @param alpha The combined alpha of this button and its ancestors.
   */
  @Override
  protected void drawSelf(@NotNull FlixelBatch batch, float drawX, float drawY, float alpha) {
    FlixelUiButtonStyle s = style;
    FlixelColor tint = getColor();
    FlixelUiBackground bg = getBackground();
    if (bg != null) {
      batch.setColor(tint.r, tint.g, tint.b, alpha);
      bg.draw(batch, drawX, drawY, width, height);
      batch.setColor(FlixelColor.WHITE);
    }
    float padL = s != null ? s.padLeft : 0f;
    float padT = s != null ? s.padTop : 0f;
    float padR = s != null ? s.padRight : 0f;
    float padB = s != null ? s.padBottom : 0f;
    float contentW = getContentWidth();
    float contentH = getContentHeight();
    // Offsets of the content box from the button's top-left corner.
    float left = padL + (width - padL - padR - contentW) * 0.5f;
    float top = padT + (height - padT - padB - contentH) * 0.5f;
    if (s != null && isPressed()) {
      top += s.pressedOffsetY;
    }
    FlixelUiImage img = icon;
    float textLeft = left;
    if (img != null) {
      float iw = img.getNaturalWidth();
      float ih = img.getNaturalHeight();
      batch.setColor(tint.r, tint.g, tint.b, alpha);
      img.draw(batch, drawX + left, drawY + top + (contentH - ih) * 0.5f, iw, ih);
      batch.setColor(FlixelColor.WHITE);
      textLeft += iw + (part.isEmpty() ? 0f : getIconSpacing());
    }
    if (!part.isEmpty()) {
      part.setColor(getFontColor());
      part.draw(batch, screenX + textLeft, screenY + top + (contentH - part.getHeight()) * 0.5f, tint, alpha);
    }
  }

  /**
   * Destroys this button, its caption, and its icon.
   *
   * <p>Every {@link #onClick} listener is removed. An icon loaded from a {@link FlixelFile} is
   * released; an icon frame passed in by the caller is left alone.
   */
  @Override
  public void destroy() {
    onClick.clear();
    super.destroy();
    part.destroy();
    FlixelUiImage img = icon;
    if (img != null) {
      icon = null;
      img.destroy();
    }
    style = null;
  }

  /**
   * Returns the width this button would like to have.
   *
   * @return The padding plus the content width, but at least the current background's minimum
   *     width, when the width is automatic; otherwise the fixed width.
   */
  @Override
  public float getPreferredWidth() {
    return autoWidth ? measureWidth() : width;
  }

  /**
   * Returns the height this button would like to have.
   *
   * @return The padding plus the content height, but at least the current background's minimum
   *     height, when the height is automatic; otherwise the fixed height.
   */
  @Override
  public float getPreferredHeight() {
    return autoHeight ? measureHeight() : height;
  }

  /**
   * Fixes the button's size, turning off automatic sizing on both sides.
   *
   * @param width The new width in pixels.
   * @param height The new height in pixels.
   */
  @Override
  public void setSize(float width, float height) {
    autoWidth = false;
    autoHeight = false;
    super.setSize(width, height);
  }

  /**
   * Fixes the button's width, turning off automatic sizing for the width.
   *
   * @param width The new width in pixels.
   */
  @Override
  public void setWidth(float width) {
    autoWidth = false;
    super.setWidth(width);
  }

  /**
   * Fixes the button's height, turning off automatic sizing for the height.
   *
   * @param height The new height in pixels.
   */
  @Override
  public void setHeight(float height) {
    autoHeight = false;
    super.setHeight(height);
  }

  /**
   * Turns automatic sizing to the content on or off for both sides.
   *
   * @param autoSize {@code true} to size the button to its content on every layout pass.
   */
  public void setAutoSize(boolean autoSize) {
    autoWidth = autoSize;
    autoHeight = autoSize;
    invalidateLayout();
  }

  /**
   * Returns the background for the current state, applying the style's fallbacks.
   *
   * <p>The first state that applies wins: disabled, pressed, hovered, focused, and finally up.
   *
   * @return The background to draw, or {@code null} when there is no style or no background.
   */
  @Nullable
  public FlixelUiBackground getBackground() {
    FlixelUiButtonStyle s = style;
    if (s == null) {
      return null;
    }
    FlixelUiBackground bg;
    if (!isEnabled()) {
      bg = s.disabled;
    } else if (isPressed()) {
      bg = s.down;
    } else if (isHovered()) {
      bg = s.over;
    } else if (isFocused()) {
      bg = s.focused;
    } else {
      bg = s.up;
    }
    return bg != null ? bg : s.up;
  }

  /**
   * Returns the text color for the current state, applying the style's fallbacks.
   *
   * <p>Disabled, pressed, and hovered buttons use their own color when the style has one; every
   * other case uses the style's normal font color.
   *
   * @return The text color; white when there is no style or the style has no font color.
   */
  @NotNull
  public FlixelColor getFontColor() {
    FlixelUiButtonStyle s = style;
    if (s == null) {
      return FlixelColor.WHITE;
    }
    FlixelColor c;
    if (!isEnabled()) {
      c = s.disabledFontColor;
    } else if (isPressed()) {
      c = s.downFontColor;
    } else if (isHovered()) {
      c = s.overFontColor;
    } else {
      c = null;
    }
    if (c == null) {
      c = s.fontColor;
    }
    return c != null ? c : FlixelColor.WHITE;
  }

  /**
   * Returns the preferred width from the content: padding plus content, at least the current
   * background's minimum width.
   *
   * @return The width in pixels.
   */
  private float measureWidth() {
    FlixelUiButtonStyle s = style;
    float w = getContentWidth();
    if (s == null) {
      return w;
    }
    w += s.padLeft + s.padRight;
    FlixelUiBackground bg = getBackground();
    return bg != null ? Math.max(w, bg.getMinWidth()) : w;
  }

  /**
   * Returns the preferred height from the content: padding plus content, at least the current
   * background's minimum height.
   *
   * @return The height in pixels.
   */
  private float measureHeight() {
    FlixelUiButtonStyle s = style;
    float h = getContentHeight();
    if (s == null) {
      return h;
    }
    h += s.padTop + s.padBottom;
    FlixelUiBackground bg = getBackground();
    return bg != null ? Math.max(h, bg.getMinHeight()) : h;
  }

  /**
   * Returns the width of the icon, the spacing, and the text together.
   *
   * @return The content width in pixels.
   */
  private float getContentWidth() {
    FlixelUiImage img = icon;
    float w = part.getWidth();
    if (img != null) {
      w += img.getNaturalWidth();
      if (!part.isEmpty()) {
        w += getIconSpacing();
      }
    }
    return w;
  }

  /**
   * Returns the height of the taller of the icon and the text.
   *
   * @return The content height in pixels.
   */
  private float getContentHeight() {
    FlixelUiImage img = icon;
    float h = part.getHeight();
    return img != null ? Math.max(h, img.getNaturalHeight()) : h;
  }

  /**
   * Returns the style's icon spacing, or {@code 0} without a style.
   *
   * @return The spacing in pixels.
   */
  private float getIconSpacing() {
    FlixelUiButtonStyle s = style;
    return s != null ? s.iconSpacing : 0f;
  }

  /** Hands the current style's font settings to the caption and re-measures. */
  private void applyStyle() {
    FlixelUiButtonStyle s = style;
    if (s != null) {
      part.setFormat(s.font, s.fontSize, s.fontColor);
    }
    invalidateLayout();
  }

  /**
   * Destroys the current icon and stores a new one.
   *
   * @param next The new icon, or {@code null} for none.
   */
  private void replaceIcon(@Nullable FlixelUiImage next) {
    FlixelUiImage old = icon;
    icon = next;
    if (old != null && old != next) {
      old.destroy();
    }
    invalidateLayout();
  }

  /**
   * Loads an icon file, rejecting {@code null}.
   *
   * @param file The icon file.
   * @return The loaded icon, owned by the caller.
   * @throws IllegalArgumentException If {@code file} is {@code null}.
   */
  @NotNull
  private static FlixelUiImage requireIcon(@Nullable FlixelFile file) {
    if (file == null) {
      throw new IllegalArgumentException("A button icon file must not be null.");
    }
    return FlixelUiImage.load(file);
  }

  /**
   * Returns the caption.
   *
   * <p>This is the button's own copy. Read it, but change it only through
   * {@link #setText(CharSequence)}.
   *
   * @return The live text buffer; empty when the button has no text.
   */
  @NotNull
  public CharSequence getText() {
    return part.getBuffer();
  }

  /**
   * Changes the caption.
   *
   * <p>The characters are copied, so a reused {@link FlixelString} can be passed every frame
   * without allocating, and nothing happens when the content is the same as before.
   *
   * @param text The new caption; {@code null} or empty shows no text.
   */
  public void setText(@Nullable CharSequence text) {
    if (part.setText(text)) {
      invalidateLayout();
    }
  }

  /**
   * Returns the icon.
   *
   * @return The icon, or {@code null} when the button has none.
   */
  @Nullable
  public FlixelUiImage getIcon() {
    return icon;
  }

  /**
   * Replaces the icon with one loaded from a file, or removes it.
   *
   * <p>The previous icon is destroyed, which releases it if the button loaded it. The new file is
   * loaded through {@link Flixel#assets} and held until it is replaced or the button is destroyed.
   *
   * @param file The icon image, or {@code null} to remove the icon.
   */
  public void setIcon(@Nullable FlixelFile file) {
    replaceIcon(file != null ? FlixelUiImage.load(file) : null);
  }

  /**
   * Replaces the icon with a frame, such as one from a texture atlas, or removes it.
   *
   * <p>The previous icon is destroyed, which releases it if the button loaded it. The frame is not
   * owned: the button never releases its texture.
   *
   * @param frame The icon frame, or {@code null} to remove the icon.
   * @throws IllegalArgumentException If {@code frame} was packed rotated.
   */
  public void setIcon(@Nullable FlixelFrame frame) {
    replaceIcon(frame != null ? new FlixelUiImage(frame) : null);
  }

  /**
   * Returns the style this button uses.
   *
   * @return The style, or {@code null} while the button is not on a display and no style object
   *     was set.
   */
  @Nullable
  public FlixelUiButtonStyle getStyle() {
    return style;
  }

  /**
   * Makes this button use a style object directly instead of looking one up in the skin.
   *
   * <p>Handy for a one-off look that does not belong in the skin. Passing {@code null} goes back to
   * the skin's style named {@link #getStyleName()} (right away while on a display, otherwise when
   * added to one). The style is applied immediately either way.
   *
   * @param style The style to use, or {@code null} to use the skin again.
   * @throws IllegalArgumentException If {@code style} is {@code null}, the button is on a display,
   *     and the skin has no matching {@link FlixelUiButtonStyle}.
   */
  public void setStyle(@Nullable FlixelUiButtonStyle style) {
    customStyle = style != null;
    this.style = style;
    if (style == null && display != null) {
      this.style = resolveStyle(FlixelUiButtonStyle.class);
    }
    applyStyle();
  }

  /**
   * Returns whether the width follows the content.
   *
   * @return {@code true} unless a fixed width was set.
   */
  public boolean isAutoWidth() {
    return autoWidth;
  }

  /**
   * Returns whether the height follows the content.
   *
   * @return {@code true} unless a fixed height was set.
   */
  public boolean isAutoHeight() {
    return autoHeight;
  }

  /**
   * Returns the caption part, for tests.
   *
   * @return The caption part.
   */
  @NotNull
  FlixelUiTextPart getPart() {
    return part;
  }
}
