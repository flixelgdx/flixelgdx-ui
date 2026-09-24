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
import org.flixelgdx.ui.skin.FlixelUiModalStyle;
import org.flixelgdx.ui.skin.FlixelUiPanelStyle;
import org.flixelgdx.ui.skin.FlixelUiSkin;
import org.flixelgdx.util.FlixelAlign;
import org.flixelgdx.util.FlixelColor;
import org.flixelgdx.util.signal.FlixelSignal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A floating dialog that sits above all root-layer widgets and below popups, controlled
 * entirely by the game.
 *
 * <p>Think of a modal as a spotlight on stage: while it is open everything behind it dims and
 * the audience cannot interact with the shadowed parts. The display draws each modal's
 * backdrop (a full-screen overlay from its {@link FlixelUiModalStyle}) immediately before the
 * modal itself, so a stack of modals produces progressive dimming.
 *
 * <h2>Opening and closing</h2>
 *
 * <p>Pass the modal to {@link FlixelUiDisplay#openModal(FlixelUiModal)} to show it. The modal is
 * centered by default ({@link FlixelAlign#CENTER}) and fires {@link #onOpen} once it is attached
 * and laid out. The game closes it by calling {@link FlixelUiDisplay#closeModal()} (closes the
 * top one) or {@link FlixelUiDisplay#closeModal(FlixelUiModal)} (closes a specific one); the
 * modal also provides a convenience {@link #close()} that delegates to its display.
 *
 * <p>The UI never closes a modal automatically. There is no built-in "click the backdrop to close"
 * or "Escape closes". To close on a click outside the modal, check whether
 * {@link FlixelUiDisplay#getWidgetAt(float, float)} returns {@code null} when the pointer is
 * outside the modal. To close on a back action, call {@link #close()} from the game's own input
 * code:
 *
 * <pre>{@code
 * // In the game's update(), after a pointer event:
 * FlixelUiWidget hit = ui.getWidgetAt(mx, my);
 * if (hit == null && Flixel.mouse.justPressed(FlixelMouseButton.LEFT)) {
 *   ui.closeModal(); // Clicked outside - close the top modal.
 * }
 * }</pre>
 *
 * <h2>Typical usage: confirm dialog</h2>
 *
 * <pre>{@code
 * FlixelUiModal confirm = new FlixelUiModal(320, 160);
 *
 * FlixelUiLabel msg = new FlixelUiLabel("Quit to menu?");
 * FlixelUiButton yes = new FlixelUiButton("Yes", b -> {
 *   confirm.close();
 *   Flixel.switchState(new MenuState());
 * });
 * FlixelUiButton no = new FlixelUiButton("No", b -> confirm.close());
 *
 * FlixelUiStack row = new FlixelUiStack(FlixelUiStack.Direction.HORIZONTAL);
 * row.setSpacing(8);
 * row.add(yes);
 * row.add(no);
 *
 * FlixelUiStack col = new FlixelUiStack(FlixelUiStack.Direction.VERTICAL);
 * col.setSpacing(16);
 * col.add(msg);
 * col.add(row);
 * confirm.add(col);
 *
 * // Tween the modal in when it opens:
 * confirm.onOpen.add(m -> {
 *   m.setAlpha(0f);
 *   Flixel.tweens.tween(m, FlixelTween.TWEEN_ALPHA_FIELD, 1f, 0.15f);
 * });
 *
 * // When the player presses a "quit" button elsewhere:
 * ui.openModal(confirm);
 * }</pre>
 *
 * @see FlixelUiDisplay#openModal(FlixelUiModal)
 * @see FlixelUiModalStyle
 */
public class FlixelUiModal extends FlixelUiPanel {

  /**
   * Dispatched after this modal is attached and laid out by
   * {@link FlixelUiDisplay#openModal(FlixelUiModal)}.
   *
   * <p>Opening an already-open modal (which moves it to the top) does not fire this signal again.
   */
  public final FlixelSignal<FlixelUiModal> onOpen = new FlixelSignal<>();

  /**
   * Dispatched after this modal is removed from the display by
   * {@link FlixelUiDisplay#closeModal(FlixelUiModal)}.
   *
   * <p>If {@link #destroyOnClose} is {@code true}, {@link #destroy()} is called after this signal
   * fires.
   */
  public final FlixelSignal<FlixelUiModal> onClose = new FlixelSignal<>();

  /** The modal-specific style, or {@code null} before one is resolved. */
  @Nullable
  private FlixelUiModalStyle modalStyle;

  /** Whether {@link #modalStyle} was set with {@link #setModalStyle(FlixelUiModalStyle)} directly. */
  private boolean customModalStyle;

  /**
   * When {@code true} the modal calls {@link #destroy()} after {@link #onClose} fires, so the game
   * never has to clean it up manually for one-shot dialogs.
   *
   * <p>Defaults to {@code false} so a modal can be reopened after it closes.
   */
  public boolean destroyOnClose;

  /** {@code true} while this modal is open on a display. Set only by {@link FlixelUiDisplay}. */
  boolean open;

  /** Creates an empty modal with a size of zero, centered by default. */
  public FlixelUiModal() {
    this(0f, 0f);
  }

  /**
   * Creates an empty modal with the given size, centered by default.
   *
   * @param width The starting width in pixels.
   * @param height The starting height in pixels.
   */
  public FlixelUiModal(float width, float height) {
    super(width, height);
    anchor(FlixelAlign.CENTER, 0f, 0f);
  }

  /**
   * Draws the style's full-display backdrop, then the panel's own background.
   *
   * <p>The backdrop covers the entire display at coordinate {@code (0, 0)}, so stacked modals
   * dim the screen progressively: each modal in the stack contributes one backdrop layer before
   * drawing its own panel content.
   *
   * @param batch The batch to draw into.
   * @param drawX The modal panel's left edge in draw coordinates.
   * @param drawY The modal panel's top edge in draw coordinates.
   * @param alpha The combined alpha of this modal and its ancestors.
   */
  @Override
  protected void drawSelf(@NotNull FlixelBatch batch, float drawX, float drawY, float alpha) {
    FlixelUiModalStyle ms = modalStyle;
    FlixelUiBackground bd = ms != null ? ms.backdrop : null;
    if (bd != null) {
      FlixelUiDisplay d = display;
      float dispW = d != null ? d.getVisibleWidth() : width;
      float dispH = d != null ? d.getVisibleHeight() : height;
      FlixelColor tint = getColor();
      float bdX = d != null ? d.toDrawX(0f) : 0f;
      float bdY = d != null ? d.toDrawY(0f) : 0f;
      batch.setColor(tint.r, tint.g, tint.b, alpha);
      bd.draw(batch, bdX, bdY, dispW, dispH);
      batch.setColor(FlixelColor.WHITE);
    }
    super.drawSelf(batch, drawX, drawY, alpha);
  }

  /**
   * Resolves the modal's style: looks up a {@link FlixelUiModalStyle} in the skin first; falls
   * back to the panel's {@link FlixelUiPanelStyle} lookup when none exists.
   */
  @Override
  protected void onStyleChanged() {
    if (!customModalStyle) {
      FlixelUiDisplay d = display;
      FlixelUiSkin sk = d != null ? d.getSkin() : null;
      FlixelUiModalStyle resolved = null;
      if (sk != null && sk.has(FlixelUiModalStyle.class, getStyleName())) {
        resolved = sk.get(FlixelUiModalStyle.class, getStyleName());
      }
      modalStyle = resolved;
    }
    // Reset the parent's customStyle flag so that when no modal style was found the parent can
    // re-resolve its own FlixelUiPanelStyle from the skin.
    customStyle = false;
    if (modalStyle != null) {
      // Apply the modal style as the panel style so its background and padding take effect.
      super.setStyle(modalStyle);
    } else {
      super.onStyleChanged();
    }
  }

  /**
   * Closes this modal by asking its display to close it; does nothing when not open.
   *
   * <p>Equivalent to calling {@link FlixelUiDisplay#closeModal(FlixelUiModal)} with this modal.
   * When this modal is not the top one, only it is closed; modals above it are unaffected.
   */
  public void close() {
    FlixelUiDisplay d = display;
    if (d != null && open) {
      d.closeModal(this);
    }
  }

  /**
   * Returns whether this modal is currently open on a display.
   *
   * @return {@code true} after {@link FlixelUiDisplay#openModal(FlixelUiModal)} and before
   *     {@link FlixelUiDisplay#closeModal(FlixelUiModal)}.
   */
  public boolean isOpen() {
    return open;
  }

  /**
   * Makes this modal use a style object directly, sending a {@link FlixelUiModalStyle} to
   * {@link #setModalStyle(FlixelUiModalStyle)} so its backdrop applies.
   *
   * <p>A plain {@link FlixelUiPanelStyle} is used for the panel only, as on any panel. Passing
   * {@code null} goes back to the skin's styles.
   *
   * @param style The style to use, or {@code null} to use the skin again.
   */
  @Override
  public void setStyle(@Nullable FlixelUiPanelStyle style) {
    if (style == null || style instanceof FlixelUiModalStyle) {
      setModalStyle((FlixelUiModalStyle) style);
      return;
    }
    super.setStyle(style);
  }

  /**
   * Makes this modal use a style object directly instead of looking one up in the skin.
   *
   * <p>Passing {@code null} goes back to the skin's style. The style's backdrop and panel
   * background are applied immediately when on a display; otherwise the style is staged for when
   * the modal is opened.
   *
   * @param style The style to use, or {@code null} to use the skin again.
   */
  public void setModalStyle(@Nullable FlixelUiModalStyle style) {
    customModalStyle = style != null;
    modalStyle = style;
    onStyleChanged();
  }

  /**
   * Returns the modal style this modal uses.
   *
   * @return The modal style, or {@code null} before one is resolved.
   */
  @Nullable
  public FlixelUiModalStyle getModalStyle() {
    return modalStyle;
  }

  /**
   * Destroys this modal, its children, and clears its signal listeners.
   */
  @Override
  public void destroy() {
    super.destroy();
    open = false;
    modalStyle = null;
    onOpen.clear();
    onClose.clear();
  }
}
