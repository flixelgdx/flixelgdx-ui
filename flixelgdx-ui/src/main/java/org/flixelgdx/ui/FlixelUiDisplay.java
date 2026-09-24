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
import org.flixelgdx.FlixelBasic;
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.FlixelSprite;
import org.flixelgdx.FlixelState;
import org.flixelgdx.functional.IFlixelBasic;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelGraphicsManager;
import org.flixelgdx.graphics.FlixelViewport;
import org.flixelgdx.input.mouse.FlixelMouseInputManager;
import org.flixelgdx.math.FlixelRect;
import org.flixelgdx.ui.skin.FlixelUiSkin;
import org.flixelgdx.util.FlixelAlign;
import org.flixelgdx.util.FlixelColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The one object a game adds to its state to show UI: it owns the widget tree, lays it out, draws
 * it on one camera, and answers "which widget is at this point?".
 *
 * <p>Think of the display as a puppet theater. The camera is the window the audience looks
 * through, and the stage has layers from back to front: the main stage ({@link #getRoot()}) where
 * most widgets live, a modal layer where dialogs appear above everything else, a popup layer for
 * things like dropdown lists that must float over even the dialogs, and a tooltip layer drawn last
 * that holds the single shared {@link FlixelUiTooltip}. The theater never moves a puppet by
 * itself: the game is the puppeteer that calls {@link FlixelUiWidget#hover()},
 * {@link FlixelUiWidget#press()}, and friends, using {@link #getWidgetAt(float, float)} to find
 * out which puppet is under its finger.
 *
 * <h2>Coordinates</h2>
 *
 * <p>Widgets are laid out in the camera's view space: {@code (0, 0)} is the top-left corner of what
 * the camera shows and the root is exactly as large as the visible area. This is the same space
 * {@link FlixelMouseInputManager#getWorldX(FlixelCamera)} reports for the same camera, so the mouse
 * position can be passed straight to {@link #getWidgetAt(float, float)}. The UI never scrolls with
 * the camera; it behaves as if every widget had a scroll factor of {@code 0}.
 *
 * <h2>Layout and resizing</h2>
 *
 * <p>Layout runs lazily: changing a widget marks the layout dirty, and the next
 * {@link #update(float)}, {@link #draw(FlixelBatch)}, or {@link #getWidgetAt(float, float)}
 * recomputes it once. When the camera's visible size changes (for example when an extend-style
 * viewport follows a window resize, or the camera zooms), the root is resized and everything
 * anchored or percent-sized follows.
 *
 * <h2>Skins</h2>
 *
 * <p>Widgets get their looks from the display's {@link FlixelUiSkin}: when a widget is added, it
 * picks the style matching its style name from that skin. Pass the skin to
 * {@link #FlixelUiDisplay(FlixelCamera, FlixelUiSkin)}, or swap it later with
 * {@link #setSkin(FlixelUiSkin)}, which restyles every widget already on the display.
 *
 * <h2>Clipping</h2>
 *
 * <p>The display keeps a small stack of clip rectangles, like a stack of stencils laid on top of
 * each other: paint only reaches the canvas where every stencil has a hole. A container with
 * {@link FlixelUiContainer#setClipChildren(boolean)} turned on pushes its content area while its
 * children draw, and widgets can clip their own parts with
 * {@link FlixelUiWidget#pushClip(FlixelBatch, float, float, float, float)}. Each new rectangle is
 * intersected with the one below it, so nested clips can only shrink. Clip rectangles are always
 * axis-aligned in the camera's view space and ignore every widget's {@link FlixelUiWidget#getAngle()}.
 *
 * <h2>Tooltips</h2>
 *
 * <p>The display owns one shared {@link FlixelUiTooltip} on a dedicated layer drawn above
 * everything else. The game decides when to show it: call
 * {@link #showTooltip(FlixelUiWidget)} to reveal it immediately, or
 * {@link #showTooltip(FlixelUiWidget, float)} to start a countdown that fires after a delay.
 * Call {@link #hideTooltip()} to dismiss it. The tooltip is hidden automatically when the target
 * widget is removed, killed, or made invisible. To make a widget show a tooltip, call
 * {@link FlixelUiWidget#setTooltip(CharSequence)} on it.
 *
 * <p>A {@link FlixelUiPointer} already shows and hides tooltips as the cursor moves, after its
 * {@link FlixelUiPointer#tooltipDelay}. Games that drive widgets by hand can get the same
 * desktop-style behavior by tracking which widget the cursor is over and showing the tooltip after
 * a short pause:
 *
 * <pre>{@code
 * FlixelUiWidget hovered;
 *
 * // In the game's update():
 * FlixelUiWidget hit = ui.getWidgetAt(mx, my);
 * if (hit != hovered) {
 *   ui.hideTooltip();
 *   if (hit != null && hit.hasTooltip()) ui.showTooltip(hit, 0.4f);
 *   hovered = hit;
 * }
 * }</pre>
 *
 * <pre>{@code
 * public class MenuState extends FlixelState {
 *   private FlixelUiDisplay ui;
 *
 *   @Override
 *   public void create() {
 *     super.create();
 *     // A transparent camera drawn above the world. Create it in every state's create(),
 *     // because switching states resets the camera list.
 *     FlixelUiSkin skin = buildSkin(); // The game's own styles; see FlixelUiSkin.
 *     ui = new FlixelUiDisplay(FlixelUiDisplay.createHudCamera(), skin);
 *     add(ui);
 *
 *     FlixelUiStack column = new FlixelUiStack(FlixelUiStack.Direction.VERTICAL);
 *     column.setSpacing(8);
 *     column.anchor(FlixelAlign.CENTER, 0, 0);
 *     column.add(playButton);
 *     column.add(quitButton);
 *     ui.add(column);
 *   }
 *
 *   @Override
 *   public void update(float elapsed) {
 *     super.update(elapsed);
 *     float mx = Flixel.mouse.getWorldX(ui.getCamera());
 *     float my = Flixel.mouse.getWorldY(ui.getCamera());
 *     FlixelUiWidget hit = ui.getWidgetAt(mx, my); // The game decides what to do with it.
 *   }
 * }
 * }</pre>
 *
 * @see FlixelUiPointer
 * @see FlixelUiWidget
 * @see FlixelUiContainer
 */
public class FlixelUiDisplay implements IFlixelBasic {

  /**
   * How many clip rectangles can be pushed at once with
   * {@link #pushClip(FlixelBatch, float, float, float, float)}.
   *
   * <p>Every clipping container and every widget clipping its own parts uses one level while it
   * draws, so this is the deepest nesting of clips a UI can have.
   */
  public static final int MAX_CLIP_DEPTH = 16;

  /** The widget holding the display's single focus, or {@code null}. Maintained by the widgets. */
  @Nullable
  FlixelUiWidget focused;

  /**
   * The widget the current tooltip is pointing at, or {@code null} when no tooltip is pending or
   * visible.
   */
  @Nullable
  private FlixelUiWidget tooltipTarget;

  /** The camera's visible width at the last layout pass. */
  private float lastWidth = -1f;

  /** The camera's visible height at the last layout pass. */
  private float lastHeight = -1f;

  /**
   * Seconds remaining before the pending tooltip becomes visible.
   *
   * <p>Only meaningful when {@link #tooltipPending} is {@code true}.
   */
  private float tooltipDelay;

  /** How many clip rectangles are on the clip stack right now. */
  private int clipDepth;

  /** The skin widgets take their styles from; never {@code null}. */
  @NotNull
  private FlixelUiSkin skin;

  @NotNull
  private final FlixelCamera camera;

  @NotNull
  private final FlixelUiContainer root;

  /** Holds open modals in the order they were opened; the last child is the top modal. */
  @NotNull
  private final FlixelUiContainer modalLayer;

  /** Holds floating widgets, such as dropdown lists, above everything else. */
  @NotNull
  private final FlixelUiContainer popupLayer;

  /** Holds the single {@link FlixelUiTooltip}, drawn last (above every other layer). */
  @NotNull
  private final FlixelUiContainer tooltipLayer;

  /** The shared tooltip widget. */
  @NotNull
  private final FlixelUiTooltip tooltip;

  /** Left edges of the clip stack, already intersected with the levels below, in view space. */
  private final float[] clipLeft = new float[MAX_CLIP_DEPTH];

  /** Top edges of the clip stack, already intersected with the levels below, in view space. */
  private final float[] clipTop = new float[MAX_CLIP_DEPTH];

  /** Right edges of the clip stack, already intersected with the levels below, in view space. */
  private final float[] clipRight = new float[MAX_CLIP_DEPTH];

  /** Bottom edges of the clip stack, already intersected with the levels below, in view space. */
  private final float[] clipBottom = new float[MAX_CLIP_DEPTH];

  /** Reused to receive the scissor rectangle from the camera's viewport. */
  private final FlixelRect scissorScratch = new FlixelRect();

  private boolean exists = true;
  private boolean active = true;
  private boolean visible = true;
  private boolean layoutDirty = true;

  /** {@code true} while counting down to show the tooltip; {@code false} once visible or cleared. */
  private boolean tooltipPending;

  /**
   * Creates a display that lays out and draws its widgets on the given camera, with an empty skin.
   *
   * <p>The display draws only during that camera's pass, so a dedicated, transparent camera from
   * {@link #createHudCamera()} keeps the UI above the world and unaffected by world camera
   * movement.
   *
   * <p>The skin starts empty, which suits widgets that draw themselves without a style. Widgets
   * that need a style throw a clear error when added until styles are added to
   * {@link #getSkin()} or a filled skin is set with {@link #setSkin(FlixelUiSkin)}.
   *
   * @param camera The camera to draw on and to measure the visible area from.
   * @throws IllegalArgumentException If {@code camera} is {@code null}.
   */
  public FlixelUiDisplay(@NotNull FlixelCamera camera) {
    this(camera, new FlixelUiSkin());
  }

  /**
   * Creates a display that lays out and draws its widgets on the given camera, styling them from
   * a skin.
   *
   * <pre>{@code
   * FlixelUiDisplay ui = new FlixelUiDisplay(FlixelUiDisplay.createHudCamera(), skin);
   * add(ui);
   * }</pre>
   *
   * @param camera The camera to draw on and to measure the visible area from.
   * @param skin The skin widgets take their styles from. The display does not destroy it.
   * @throws IllegalArgumentException If {@code camera} or {@code skin} is {@code null}.
   */
  public FlixelUiDisplay(@NotNull FlixelCamera camera, @NotNull FlixelUiSkin skin) {
    if (camera == null) {
      throw new IllegalArgumentException("A UI display needs a camera.");
    }
    if (skin == null) {
      throw new IllegalArgumentException("A UI display needs a skin.");
    }
    this.camera = camera;
    this.skin = skin;
    root = new FlixelUiContainer();
    modalLayer = new FlixelUiContainer();
    popupLayer = new FlixelUiContainer();
    tooltipLayer = new FlixelUiContainer();
    tooltip = new FlixelUiTooltip();
    root.attach(this);
    modalLayer.attach(this);
    popupLayer.attach(this);
    tooltipLayer.attach(this);
    tooltipLayer.add(tooltip);
  }

  /**
   * Creates a transparent camera sized like the game, adds it to {@link Flixel#cameras}, and
   * returns it, ready to pass to {@link #FlixelUiDisplay(FlixelCamera)}.
   *
   * <p>The camera is created at the game's design size with the same viewport type as the default
   * camera, its background is {@link FlixelColor#CLEAR}, and it blends its background, so the
   * cameras below it show through. Because it is added last, it draws on top of every existing
   * camera.
   *
   * <p>Call it in each state's {@link FlixelState#create()}: switching states resets the camera
   * list, so a camera made for an earlier state is gone.
   *
   * <p>The camera's {@link FlixelCamera#defaultDrawTarget} is {@code false}, so world objects that
   * do not list any cameras of their own (their {@link FlixelBasic#cameras} is {@code null}) stay
   * on the world cameras and are not drawn a second time on the UI. The display itself always
   * draws on its own camera; any other object that should appear there must list this camera in
   * its {@link FlixelBasic#cameras}.
   *
   * @return The new camera, already added to {@link Flixel#cameras}.
   */
  @NotNull
  public static FlixelCamera createHudCamera() {
    FlixelCamera hud = new FlixelCamera(Flixel.getDesignWidth(), Flixel.getDesignHeight());
    hud.bgColor.set(FlixelColor.CLEAR);
    hud.useBgAlphaBlending = true;
    hud.defaultDrawTarget = false;
    Flixel.cameras.add(hud);
    return hud;
  }

  /**
   * Adds a widget to the {@linkplain #getRoot() root} layer.
   *
   * @param widget The widget to add.
   * @throws IllegalArgumentException If {@code widget} is {@code null} or cannot be added.
   * @see FlixelUiContainer#add(FlixelUiWidget)
   */
  public void add(@NotNull FlixelUiWidget widget) {
    root.add(widget);
  }

  /**
   * Removes a widget from the {@linkplain #getRoot() root} layer.
   *
   * @param widget The widget to remove.
   * @return {@code true} if the widget was a direct child of the root and has been removed.
   */
  public boolean remove(@Nullable FlixelUiWidget widget) {
    return root.remove(widget);
  }

  /**
   * Returns the topmost widget at a point, or {@code null} when nothing there can be hit.
   *
   * <p>The popup layer is searched first. Then, if any modal is open, only the top modal is
   * searched: when none of its children is hit but the point is inside the modal, the modal itself
   * is returned so clicks cannot reach the widgets behind it; outside it the result is
   * {@code null}. With no modal open, the root layer is searched. Within a layer the deepest,
   * topmost widget wins, and widgets that do not exist, are invisible, are disabled, or are not
   * {@link FlixelUiWidget#interactive} are skipped (children of non-interactive containers are
   * still found).
   *
   * <p>This is purely geometric and allocation-free; it reads no input.
   *
   * @param x The X to test, in the camera's view space (as from
   *     {@code Flixel.mouse.getWorldX(getCamera())}).
   * @param y The Y to test, in the camera's view space (as from
   *     {@code Flixel.mouse.getWorldY(getCamera())}).
   * @return The widget at that point, or {@code null}.
   */
  @Nullable
  public FlixelUiWidget getWidgetAt(float x, float y) {
    if (!exists) {
      return null;
    }
    ensureLayout();
    FlixelUiWidget hit = popupLayer.hitTest(x, y);
    if (hit != null) {
      return hit;
    }
    FlixelUiWidget modal = topModalWidget();
    if (modal != null) {
      hit = modal.hitTest(x, y);
      if (hit != null) {
        return hit;
      }
      return modal.isVisible() && modal.containsPoint(x, y) ? modal : null;
    }
    return root.hitTest(x, y);
  }

  /**
   * Keeps the layout up to date, then updates every layer.
   *
   * <p>When a tooltip delay is running (from {@link #showTooltip(FlixelUiWidget, float)}), this
   * method counts it down and reveals the tooltip when it expires. It also hides the tooltip
   * automatically when the target widget is removed, killed, or made invisible.
   *
   * @param elapsed Seconds elapsed since the last frame.
   */
  @Override
  public void update(float elapsed) {
    if (!exists || !active) {
      return;
    }
    tickTooltip(elapsed);
    ensureLayout();
    updateLayer(root, elapsed);
    updateLayer(modalLayer, elapsed);
    updateLayer(popupLayer, elapsed);
    updateLayer(tooltipLayer, elapsed);
  }

  /**
   * Draws every layer, back to front, during this display's camera pass.
   *
   * <p>A state draws its members once per camera; the display only draws when the camera being
   * drawn is its own. Outside a camera pass it treats the first camera in {@link Flixel#cameras} as
   * the current one, the same fallback {@link FlixelSprite} uses.
   *
   * @param batch The batch to draw into.
   */
  @Override
  public void draw(@NotNull FlixelBatch batch) {
    if (!exists || !visible) {
      return;
    }
    FlixelCamera current = Flixel.getDrawCamera();
    if (current == null && !Flixel.cameras.isEmpty()) {
      current = Flixel.cameras.first();
    }
    if (current != camera) {
      return;
    }
    ensureLayout();
    // Start and end with an empty clip stack, so a widget that threw during an earlier frame can
    // never leave a stale scissor behind.
    resetClip(batch);
    try {
      root.drawTree(batch, 1f);
      modalLayer.drawTree(batch, 1f);
      popupLayer.drawTree(batch, 1f);
      tooltipLayer.drawTree(batch, 1f);
    } finally {
      resetClip(batch);
    }
  }

  /**
   * Recomputes the layout of every layer right away.
   *
   * <p>Layout normally runs by itself before the next update, draw, or hit-test after something
   * changes. Call this when you need up-to-date {@link FlixelUiWidget#getScreenX()} values
   * immediately, for example right after building a screen. Does not allocate.
   */
  public void layout() {
    layoutDirty = false;
    float w = getVisibleWidth();
    float h = getVisibleHeight();
    lastWidth = w;
    lastHeight = h;
    layoutLayer(root, w, h);
    layoutLayer(modalLayer, w, h);
    layoutLayer(popupLayer, w, h);
    // Place the tooltip before laying out its layer so the container can apply the final position.
    if (tooltipTarget != null && tooltip.isVisible()) {
      placeTooltip(w, h);
    }
    layoutLayer(tooltipLayer, w, h);
  }

  /**
   * Converts a UI screen X into the X to hand to the batch while this display draws.
   *
   * <p>This is the single place where UI coordinates meet draw coordinates. The UI is laid out
   * directly in the camera's view space, which is the space the batch draws in during this
   * camera's pass, so the two are the same. It matches how a {@link FlixelSprite} with a scroll
   * factor of {@code 0} draws at zoom {@code 1}; at other zoom levels a sprite is offset by the zoom
   * margin, while the UI stays aligned with the mouse (see {@link #toSpriteX(float)}).
   *
   * @param screenX An X in the camera's view space, such as {@link FlixelUiWidget#getScreenX()}.
   * @return The X to draw at.
   */
  public float toDrawX(float screenX) {
    return screenX;
  }

  /**
   * Converts a UI screen Y into the Y to hand to the batch while this display draws.
   *
   * @param screenY A Y in the camera's view space, such as {@link FlixelUiWidget#getScreenY()}.
   * @return The Y to draw at.
   * @see #toDrawX(float)
   */
  public float toDrawY(float screenY) {
    return screenY;
  }

  /**
   * Converts a UI screen X into the world X a scroll-factor-{@code 0} {@link FlixelSprite} (or text)
   * needs so that its own {@code draw()} lands exactly on that spot of this camera.
   *
   * <p>Framework objects convert their world position with
   * {@link FlixelCamera#worldToViewX(float, float)}, which subtracts the camera's zoom margin. Widgets
   * that draw such an object as one of their parts (for example a label drawing its text) place it
   * with this method so it lines up with the widget at any zoom level.
   *
   * @param screenX An X in the camera's view space.
   * @return The world X to give the object.
   */
  public float toSpriteX(float screenX) {
    return screenX + camera.getViewMarginX();
  }

  /**
   * Converts a UI screen Y into the world Y a scroll-factor-{@code 0} {@link FlixelSprite} (or text)
   * needs so that its own {@code draw()} lands exactly on that spot of this camera.
   *
   * @param screenY A Y in the camera's view space.
   * @return The world Y to give the object.
   * @see #toSpriteX(float)
   */
  public float toSpriteY(float screenY) {
    return screenY + camera.getViewMarginY();
  }

  /**
   * Restricts drawing to a rectangle until the matching {@link #popClip(FlixelBatch)}.
   *
   * <p>Think of it as laying a stencil over the canvas: only the part of the canvas under the hole
   * receives paint. The rectangle is intersected with the clip that is already active, so a clip
   * nested inside another can only make the visible area smaller. An intersection that is empty is
   * allowed: the method then returns {@code false}, and callers should skip drawing until the
   * matching pop, because the graphics backends clamp a scissor to at least one pixel.
   *
   * <p>The batch is flushed first, so everything drawn before this call keeps the old clip. The
   * rectangle is then converted to draw space and projected to framebuffer pixels through the
   * camera's {@link FlixelViewport#projectToScissor(float, float, float, float, FlixelRect)}, and
   * applied with {@link FlixelGraphicsManager#setScissor(int, int, int, int)}, the same path a
   * {@link FlixelSprite} clip rectangle takes.
   *
   * <p>Clip rectangles are axis-aligned: they ignore every widget's
   * {@link FlixelUiWidget#getAngle()}, so a rotated widget is clipped by its unrotated rectangle.
   * Widgets normally use the helpers {@link FlixelUiWidget#pushClip(FlixelBatch, float, float, float,
   * float)} and {@link FlixelUiWidget#popClip(FlixelBatch)} instead of calling this directly. Every
   * push needs exactly one pop, ideally in a {@code finally} block. Does not allocate.
   *
   * <pre>{@code
   * // Inside a widget's drawSelf(...): keep scrolling text inside the box's inner area.
   * boolean visible = getDisplay().pushClip(batch, getScreenX() + 4, getScreenY() + 4,
   *     getWidth() - 8, getHeight() - 8);
   * try {
   *   if (visible) {
   *     text.draw(batch);
   *   }
   * } finally {
   *   getDisplay().popClip(batch);
   * }
   * }</pre>
   *
   * @param batch The batch being drawn into; it is flushed before the clip changes.
   * @param x The left edge, in the camera's view space (the same space as
   *     {@link FlixelUiWidget#getScreenX()}).
   * @param y The top edge, in the camera's view space.
   * @param width The width in pixels; negative values count as {@code 0}.
   * @param height The height in pixels; negative values count as {@code 0}.
   * @return {@code true} when the clipped area is not empty, {@code false} when nothing drawn until
   *     the matching pop can be visible.
   * @throws IllegalStateException If {@link #MAX_CLIP_DEPTH} clips are already pushed.
   */
  public boolean pushClip(@NotNull FlixelBatch batch, float x, float y, float width, float height) {
    int i = clipDepth;
    if (i >= MAX_CLIP_DEPTH) {
      throw new IllegalStateException("The UI clip stack is full: at most " + MAX_CLIP_DEPTH
          + " clips can be nested. Check that every pushClip(...) has a matching popClip(...).");
    }
    float left = x;
    float top = y;
    float right = x + Math.max(0f, width);
    float bottom = y + Math.max(0f, height);
    if (i > 0) {
      left = Math.max(left, clipLeft[i - 1]);
      top = Math.max(top, clipTop[i - 1]);
      right = Math.min(right, clipRight[i - 1]);
      bottom = Math.min(bottom, clipBottom[i - 1]);
    }
    // Collapse a disjoint intersection to an empty rectangle so its size is zero, not negative.
    if (right < left) {
      right = left;
    }
    if (bottom < top) {
      bottom = top;
    }
    batch.flush();
    clipLeft[i] = left;
    clipTop[i] = top;
    clipRight[i] = right;
    clipBottom[i] = bottom;
    clipDepth = i + 1;
    applyClip();
    return right > left && bottom > top;
  }

  /**
   * Removes the clip rectangle added by the last
   * {@link #pushClip(FlixelBatch, float, float, float, float)}.
   *
   * <p>The batch is flushed first, so everything drawn inside the clip is committed with it. Then
   * the clip below becomes active again, or, when the stack is now empty, clipping is turned off
   * with {@link FlixelGraphicsManager#clearScissor()}. Does not allocate.
   *
   * @param batch The batch being drawn into; it is flushed before the clip changes.
   * @throws IllegalStateException If no clip is pushed.
   */
  public void popClip(@NotNull FlixelBatch batch) {
    if (clipDepth == 0) {
      throw new IllegalStateException("popClip() was called without a matching pushClip(...).");
    }
    batch.flush();
    clipDepth--;
    applyClip();
  }

  /**
   * Destroys every widget on every layer and stops the display from updating or drawing.
   */
  @Override
  public void destroy() {
    root.destroy();
    modalLayer.destroy();
    popupLayer.destroy();
    tooltipLayer.destroy();
    focused = null;
    tooltipTarget = null;
    tooltipPending = false;
    exists = false;
    active = false;
    visible = true;
  }

  /**
   * Calls {@link #destroy()}, matching {@link FlixelBasic#reset()}.
   */
  @Override
  public final void reset() {
    destroy();
  }

  @Override
  public void kill() {
    exists = false;
  }

  @Override
  public void revive() {
    exists = true;
  }

  @Override
  public void setKilled(boolean killed) {
    exists = !killed;
  }

  @Override
  public void toggleKilled() {
    exists = !exists;
  }

  @Override
  public void toggleVisible() {
    visible = !visible;
  }

  /** Marks the layout as out of date. */
  void invalidate() {
    layoutDirty = true;
  }

  /**
   * Opens a modal on top of the modal stack, attaches it, lays out the display, and fires
   * {@link FlixelUiModal#onOpen}.
   *
   * <p>If the modal is already open on this display, it is moved to the top of the stack without
   * firing {@link FlixelUiModal#onOpen} again. The modal is centered by default
   * ({@link FlixelUiModal} sets {@link FlixelAlign#CENTER} in its constructor);
   * the game can change the anchor before or after calling this method.
   *
   * <p>Modals are drawn in stack order: the first opened modal draws its backdrop first, then the
   * second modal's backdrop dims the screen further before its own panel appears. Hit-testing only
   * reaches the topmost modal; clicking empty space inside it is blocked; clicking outside it
   * returns {@code null}.
   *
   * <pre>{@code
   * FlixelUiModal dialog = new FlixelUiModal(300, 180);
   * dialog.add(buildDialogContent());
   * dialog.onOpen.add(m -> tweenIn(m));
   * ui.openModal(dialog);
   * }</pre>
   *
   * @param modal The modal to open.
   * @throws IllegalArgumentException If {@code modal} is {@code null}.
   */
  public void openModal(@NotNull FlixelUiModal modal) {
    if (modal == null) {
      throw new IllegalArgumentException("Cannot open a null modal.");
    }
    if (modal.open && modal.parent == modalLayer) {
      // Move to the top without re-firing onOpen.
      modalLayer.children.removeValue(modal, true);
      modalLayer.children.add(modal);
      invalidate();
      return;
    }
    modalLayer.add(modal);
    modal.open = true;
    layout();
    modal.onOpen.dispatch(modal);
  }

  /**
   * Closes the topmost open modal.
   *
   * <p>Does nothing when no modal is open. Equivalent to
   * {@link #closeModal(FlixelUiModal) closeModal(getTopModal())}.
   */
  public void closeModal() {
    FlixelUiModal top = getTopModal();
    if (top != null) {
      closeModal(top);
    }
  }

  /**
   * Closes a specific modal, wherever it is in the stack.
   *
   * <p>Before removing the modal:
   * <ol>
   *   <li>The tooltip is hidden when its target is inside this modal (see
   *       {@link #showTooltip(FlixelUiWidget)}).
   *   <li>All widgets inside the modal have their hovered and pressed states cleared silently.
   *       Specifically: both flags are set to false, {@link FlixelUiWidget#onStateChanged()} is
   *       called once when either was set, and {@link FlixelUiWidget#onUnhover} is dispatched when
   *       the widget was hovered. {@link FlixelUiWidget#onRelease} is not dispatched, because a
   *       press cleared on close is not an activation. Focus and the enabled flag are unchanged.
   * </ol>
   *
   * <p>After the modal is removed, {@link FlixelUiModal#onClose} fires. If
   * {@link FlixelUiModal#destroyOnClose} is {@code true}, {@link FlixelUiModal#destroy()} is
   * called after {@code onClose}. Does nothing when the modal is not open on this display.
   *
   * @param modal The modal to close.
   */
  public void closeModal(@Nullable FlixelUiModal modal) {
    if (modal == null || !modal.open || modal.parent != modalLayer) {
      return;
    }
    if (tooltipTarget != null && isDescendantOfOrSelf(tooltipTarget, modal)) {
      hideTooltip();
    }
    modal.clearInteractionStates();
    modal.open = false;
    modalLayer.remove(modal);
    modal.onClose.dispatch(modal);
    if (modal.destroyOnClose) {
      modal.destroy();
    }
  }

  /**
   * Returns the topmost open modal, or {@code null} when no modal is open.
   *
   * <p>Modals that do not exist (killed) are skipped.
   *
   * @return The top {@link FlixelUiModal}, or {@code null}.
   */
  @Nullable
  public FlixelUiModal getTopModal() {
    FlixelUiWidget[] items = modalLayer.children.getItems();
    for (int i = modalLayer.children.getSize() - 1; i >= 0; i--) {
      FlixelUiWidget w = items[i];
      if (w.isExists() && w instanceof FlixelUiModal) {
        return (FlixelUiModal) w;
      }
    }
    return null;
  }

  /**
   * Returns how many modals are currently in the stack (including any that are not yet
   * {@linkplain FlixelUiWidget#isExists() existing}).
   *
   * @return The number of open modals.
   */
  public int getModalCount() {
    return modalLayer.children.getSize();
  }

  /**
   * Returns whether at least one modal is currently open.
   *
   * @return {@code true} when the modal stack is not empty.
   */
  public boolean isModalOpen() {
    return !modalLayer.children.isEmpty();
  }

  /**
   * Returns the layer that floats above the root and the modals.
   *
   * @return The popup layer, which covers the whole visible area.
   */
  @NotNull
  FlixelUiContainer getPopupLayer() {
    return popupLayer;
  }

  /**
   * Returns the layer that holds open modals, top modal last.
   *
   * @return The modal layer, which covers the whole visible area.
   */
  @NotNull
  FlixelUiContainer getModalLayer() {
    return modalLayer;
  }

  /**
   * Returns the topmost widget on the modal stack, or {@code null} when no modal is open.
   *
   * <p>Used internally by hit-testing to restrict input to the top modal.
   */
  @Nullable
  private FlixelUiWidget topModalWidget() {
    FlixelUiWidget[] items = modalLayer.children.getItems();
    for (int i = modalLayer.children.getSize() - 1; i >= 0; i--) {
      if (items[i].isExists()) {
        return items[i];
      }
    }
    return null;
  }

  /**
   * Returns whether {@code target} is the same as {@code ancestor} or a descendant of it.
   *
   * @param target The widget to check.
   * @param ancestor The potential ancestor.
   * @return {@code true} when target equals ancestor or ancestor contains target.
   */
  private static boolean isDescendantOfOrSelf(
      @NotNull FlixelUiWidget target, @NotNull FlixelUiContainer ancestor) {
    FlixelUiWidget w = target;
    while (w != null) {
      if (w == ancestor) {
        return true;
      }
      w = w.parent;
    }
    return false;
  }

  /**
   * Pops clips until only {@code depth} remain, applying the new top once.
   *
   * <p>Widgets use it in a {@code finally} block to undo whatever clips they pushed, even when
   * drawing threw halfway. Does nothing when the stack is already at or below {@code depth}.
   *
   * @param batch The batch being drawn into; it is flushed before the clip changes.
   * @param depth The clip depth to return to.
   */
  void restoreClipDepth(@NotNull FlixelBatch batch, int depth) {
    if (clipDepth <= depth) {
      return;
    }
    batch.flush();
    clipDepth = Math.max(0, depth);
    applyClip();
  }

  /**
   * Empties the clip stack and turns clipping off if anything was still pushed.
   *
   * @param batch The batch being drawn into; it is flushed before the clip changes.
   */
  private void resetClip(@NotNull FlixelBatch batch) {
    restoreClipDepth(batch, 0);
  }

  /**
   * Applies the top of the clip stack as the scissor rectangle, or clears the scissor when the
   * stack is empty.
   */
  private void applyClip() {
    int i = clipDepth - 1;
    if (i < 0) {
      Flixel.graphics.clearScissor();
      return;
    }
    float left = toDrawX(clipLeft[i]);
    float top = toDrawY(clipTop[i]);
    float right = toDrawX(clipRight[i]);
    float bottom = toDrawY(clipBottom[i]);
    FlixelRect r = camera.getViewport().projectToScissor(left, top, right - left, bottom - top, scissorScratch);
    // Round the edges rather than the size, so neighboring and nested clips share pixel edges.
    int x0 = Math.round(r.x);
    int y0 = Math.round(r.y);
    int x1 = Math.round(r.x + r.width);
    int y1 = Math.round(r.y + r.height);
    Flixel.graphics.setScissor(x0, y0, x1 - x0, y1 - y0);
  }

  /**
   * Advances the tooltip delay, auto-hides the tooltip when the target becomes invalid, and
   * reveals the tooltip when the delay expires.
   *
   * @param elapsed Seconds since the last frame.
   */
  private void tickTooltip(float elapsed) {
    FlixelUiWidget target = tooltipTarget;
    if (target == null) {
      return;
    }
    if (!isTooltipTargetValid(target)) {
      hideTooltip();
      return;
    }
    if (tooltipPending) {
      tooltipDelay -= elapsed;
      if (tooltipDelay <= 0f) {
        tooltipPending = false;
        revealTooltip();
      }
    }
  }

  /**
   * Makes the tooltip visible using the current target's tooltip text, then invalidates layout so
   * it is placed before the next draw.
   */
  private void revealTooltip() {
    FlixelUiWidget target = tooltipTarget;
    if (target == null || !target.hasTooltip()) {
      return;
    }
    tooltip.setText(target.getTooltip());
    tooltip.setVisible(true);
    invalidate();
  }

  /**
   * Computes where the tooltip should go and stores the result in the tooltip widget's local
   * position so the container can apply it as the screen position.
   *
   * <p>The tooltip is placed centered below the target at {@code gap} pixels of distance. If it
   * would go past the bottom of the display, it is flipped above the target. The position is then
   * clamped inside the display bounds on both axes.
   *
   * @param displayW The visible width of the display.
   * @param displayH The visible height of the display.
   */
  private void placeTooltip(float displayW, float displayH) {
    FlixelUiWidget target = tooltipTarget;
    if (target == null) {
      return;
    }
    float tw = tooltip.getPreferredWidth();
    float th = tooltip.getPreferredHeight();
    float gap = tooltip.getGap();
    // Try to place below the target.
    float tipX = target.screenX + (target.width - tw) * 0.5f;
    float tipY = target.screenY + target.height + gap;
    // Flip above when the tooltip would go past the bottom edge.
    if (tipY + th > displayH) {
      tipY = target.screenY - th - gap;
    }
    // Clamp inside display bounds.
    tipX = Math.max(0f, Math.min(displayW - tw, tipX));
    tipY = Math.max(0f, Math.min(displayH - th, tipY));
    tooltip.x = tipX;
    tooltip.y = tipY;
    tooltip.width = tw;
    tooltip.height = th;
  }

  /**
   * Returns whether the tooltip target is still valid: it must be on this display, exist, and be
   * visible.
   *
   * @param target The target to check.
   * @return {@code true} when the tooltip should remain shown or pending.
   */
  private boolean isTooltipTargetValid(@NotNull FlixelUiWidget target) {
    return target.display == this && target.isExists() && target.isVisible();
  }

  /** Runs a layout pass when something changed or when the camera's visible size changed. */
  private void ensureLayout() {
    if (layoutDirty || getVisibleWidth() != lastWidth || getVisibleHeight() != lastHeight) {
      layout();
    }
  }

  float getVisibleWidth() {
    return camera.getWorldWidth() / camera.getZoom();
  }

  float getVisibleHeight() {
    return camera.getWorldHeight() / camera.getZoom();
  }

  private static void layoutLayer(@NotNull FlixelUiContainer layer, float width, float height) {
    layer.x = 0f;
    layer.y = 0f;
    layer.width = width;
    layer.height = height;
    layer.screenX = 0f;
    layer.screenY = 0f;
    layer.measureTree();
    layer.layoutTree();
  }

  private static void updateLayer(@NotNull FlixelUiContainer layer, float elapsed) {
    if (layer.isExists() && layer.isActive()) {
      layer.update(elapsed);
    }
  }

  @NotNull
  public FlixelCamera getCamera() {
    return camera;
  }

  /**
   * Returns the skin widgets on this display take their styles from.
   *
   * @return The skin; never {@code null}.
   */
  @NotNull
  public FlixelUiSkin getSkin() {
    return skin;
  }

  /**
   * Replaces the skin and restyles every widget already on this display.
   *
   * <p>Each widget's {@link FlixelUiWidget#onStyleChanged()} runs again, parents before children,
   * and the layout is recomputed. Passing the current skin also works, which is how to apply
   * styles that were added to or replaced in it after the widgets were attached. The display
   * never destroys a skin; destroy the old one yourself when nothing uses it.
   *
   * @param skin The new skin.
   * @throws IllegalArgumentException If {@code skin} is {@code null}, or if a widget on the display
   *     needs a style the new skin does not have.
   */
  public void setSkin(@NotNull FlixelUiSkin skin) {
    if (skin == null) {
      throw new IllegalArgumentException("A UI display needs a skin.");
    }
    this.skin = skin;
    root.refreshStyleTree();
    modalLayer.refreshStyleTree();
    popupLayer.refreshStyleTree();
    tooltipLayer.refreshStyleTree();
    invalidate();
  }

  /**
   * Returns the root layer, which is sized to the camera's visible area and holds most widgets.
   *
   * @return The root container.
   */
  @NotNull
  public FlixelUiContainer getRoot() {
    return root;
  }

  /**
   * Returns the widget that currently holds this display's focus.
   *
   * @return The focused widget, or {@code null} when nothing is focused.
   */
  @Nullable
  public FlixelUiWidget getFocused() {
    return focused;
  }

  /**
   * Shows the tooltip for a widget immediately, with no delay.
   *
   * <p>If the target has no tooltip text (see {@link FlixelUiWidget#setTooltip(CharSequence)}),
   * or the text is empty, this call does nothing. Showing a new target while one is already
   * pending or visible replaces it and cancels any running delay.
   *
   * @param target The widget whose tooltip to show.
   * @throws IllegalArgumentException If {@code target} is {@code null}.
   */
  public void showTooltip(@NotNull FlixelUiWidget target) {
    showTooltip(target, 0f);
  }

  /**
   * Shows the tooltip for a widget after a delay, or immediately when the delay is zero or less.
   *
   * <p>If the target has no tooltip text, this call does nothing. Showing a new target while one
   * is already pending or visible replaces it, canceling any running delay, and the new delay
   * starts from scratch. The display must be updated for the countdown to advance.
   *
   * @param target The widget whose tooltip to show.
   * @param delay Seconds to wait before the tooltip appears; {@code 0} or less means immediate.
   * @throws IllegalArgumentException If {@code target} is {@code null}.
   */
  public void showTooltip(@NotNull FlixelUiWidget target, float delay) {
    if (target == null) {
      throw new IllegalArgumentException("Tooltip target must not be null.");
    }
    if (!target.hasTooltip()) {
      return;
    }
    tooltipTarget = target;
    tooltip.setVisible(false);
    if (delay <= 0f) {
      tooltipPending = false;
      revealTooltip();
    } else {
      tooltipDelay = delay;
      tooltipPending = true;
      invalidate();
    }
  }

  /**
   * Hides the tooltip and cancels any pending delay.
   *
   * <p>Does nothing when the tooltip is already hidden.
   */
  public void hideTooltip() {
    tooltipTarget = null;
    tooltipPending = false;
    if (tooltip.isVisible()) {
      tooltip.setVisible(false);
      invalidate();
    }
  }

  /**
   * Returns whether the tooltip is currently visible.
   *
   * @return {@code true} when the tooltip widget is visible.
   */
  public boolean isTooltipVisible() {
    return tooltip.isVisible();
  }

  /**
   * Returns the widget whose tooltip is currently pending or visible.
   *
   * @return The target widget, or {@code null} when no tooltip is pending or visible.
   */
  @Nullable
  public FlixelUiWidget getTooltipTarget() {
    return tooltipTarget;
  }

  /**
   * Returns the shared tooltip widget so the game can tween it.
   *
   * <p>Use this to animate the tooltip in or out, for example by tweening its alpha. The display
   * manages this widget's text, position, and visibility; the game only adjusts cosmetic properties.
   *
   * @return The tooltip widget; never {@code null}.
   */
  @NotNull
  public FlixelUiTooltip getTooltip() {
    return tooltip;
  }

  /**
   * Chooses which named style from the skin the tooltip widget uses.
   *
   * <p>Defaults to {@code "default"}. Change it before widgets are added to ensure the style is
   * available in the skin when the tooltip is first shown.
   *
   * @param name The style name; must not be {@code null}.
   * @throws IllegalArgumentException If {@code name} is {@code null}.
   */
  public void setTooltipStyle(@NotNull String name) {
    tooltip.setStyle(name);
  }

  /**
   * Returns whether any clip rectangle is pushed right now.
   *
   * <p>This is only ever {@code true} while the display draws.
   *
   * @return {@code true} when drawing is currently clipped.
   */
  public boolean isClipped() {
    return clipDepth > 0;
  }

  /**
   * Returns how many clip rectangles are pushed right now.
   *
   * @return The clip stack depth, from {@code 0} to {@link #MAX_CLIP_DEPTH}.
   */
  int getClipDepth() {
    return clipDepth;
  }

  /**
   * Returns the left edge of the active clip, in view space.
   *
   * @return The left edge, or negative infinity when nothing is clipped.
   */
  float getClipLeft() {
    return clipDepth > 0 ? clipLeft[clipDepth - 1] : Float.NEGATIVE_INFINITY;
  }

  /**
   * Returns the top edge of the active clip, in view space.
   *
   * @return The top edge, or negative infinity when nothing is clipped.
   */
  float getClipTop() {
    return clipDepth > 0 ? clipTop[clipDepth - 1] : Float.NEGATIVE_INFINITY;
  }

  /**
   * Returns the right edge of the active clip, in view space.
   *
   * @return The right edge, or positive infinity when nothing is clipped.
   */
  float getClipRight() {
    return clipDepth > 0 ? clipRight[clipDepth - 1] : Float.POSITIVE_INFINITY;
  }

  /**
   * Returns the bottom edge of the active clip, in view space.
   *
   * @return The bottom edge, or positive infinity when nothing is clipped.
   */
  float getClipBottom() {
    return clipDepth > 0 ? clipBottom[clipDepth - 1] : Float.POSITIVE_INFINITY;
  }

  @Override
  public boolean isExists() {
    return exists;
  }

  @Override
  public void setExists(boolean exists) {
    this.exists = exists;
  }

  @Override
  public boolean isActive() {
    return active;
  }

  @Override
  public void setActive(boolean active) {
    this.active = active;
  }

  @Override
  public boolean isVisible() {
    return visible;
  }

  @Override
  public void setVisible(boolean visible) {
    this.visible = visible;
  }

  @Override
  public boolean isKilled() {
    return !exists;
  }
}
