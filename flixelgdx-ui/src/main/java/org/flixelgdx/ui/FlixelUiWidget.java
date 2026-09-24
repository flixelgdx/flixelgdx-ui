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

import org.flixelgdx.FlixelBasic;
import org.flixelgdx.FlixelObject;
import org.flixelgdx.functional.FlixelColorable;
import org.flixelgdx.functional.FlixelPositional;
import org.flixelgdx.functional.IFlixelBasic;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.math.FlixelMatrix;
import org.flixelgdx.ui.skin.FlixelUiSkin;
import org.flixelgdx.ui.skin.FlixelUiStyle;
import org.flixelgdx.util.FlixelAlign;
import org.flixelgdx.util.FlixelColor;
import org.flixelgdx.util.FlixelString;
import org.flixelgdx.util.signal.FlixelSignal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The base class of every piece of UI: a rectangle that can be laid out, drawn, and put into
 * interaction states by the game.
 *
 * <p>Think of a widget as a puppet on a stage. It has joints that can bend (hovered, pressed,
 * focused, enabled) and it visibly reacts when they do, but it never moves on its own. The game is
 * the puppeteer: it decides, from its own mouse, keyboard, or gamepad handling, when to pull a
 * string by calling {@link #hover()}, {@link #press()}, {@link #focus()}, and the rest. The widget
 * never reads input itself.
 *
 * <p>Widgets implement the framework's functional interfaces ({@link IFlixelBasic},
 * {@link FlixelPositional}, and {@link FlixelColorable}) instead of extending a scene object, so
 * tools that work on those interfaces, such as tweens, work on widgets unchanged. The lifecycle
 * flags ({@link #isExists()}, {@link #isActive()}, {@link #isVisible()}, {@link #kill()},
 * {@link #revive()}, {@link #destroy()}, and {@link #reset()}) behave like {@link FlixelBasic}.
 *
 * <h2>Coordinates</h2>
 *
 * <p>{@link #getX()} and {@link #getY()} are <em>local</em>: they are measured from the parent
 * container's content origin (its top-left corner plus its padding). After each layout pass the
 * widget also caches its absolute position in the display camera's view space, available from
 * {@link #getScreenX()} and {@link #getScreenY()}. That space is the same one
 * {@code Flixel.mouse.getWorldX(camera)} reports, so a game can compare the two directly.
 *
 * <h2>Layout</h2>
 *
 * <p>A widget can be {@linkplain #anchor(int, float, float) anchored} to one of the nine
 * {@link FlixelAlign} positions of its parent, and it can be sized as a fraction of its parent
 * with {@link #setPercentSize(float, float)}. Inside a {@link FlixelUiStack} the stack places its
 * children one after another instead, so anchors are ignored there.
 *
 * <h2>Interaction</h2>
 *
 * <p>Each interaction method changes a state flag and dispatches the matching signal only when the
 * state actually changes, so calling {@link #hover()} every frame fires {@link #onHover} once. Calls
 * on a disabled widget are ignored. Only one widget per display holds focus at a time.
 *
 * <pre>{@code
 * // A custom widget only has to draw itself.
 * public class ColorBox extends FlixelUiWidget {
 *   public ColorBox(float width, float height) {
 *     super(width, height);
 *   }
 *
 *   @Override
 *   protected void drawSelf(FlixelBatch batch, float drawX, float drawY, float alpha) {
 *     // Draw a filled rectangle at (drawX, drawY) with getWidth() x getHeight() pixels.
 *   }
 * }
 *
 * ColorBox box = new ColorBox(64, 64);
 * box.anchor(FlixelAlign.TOP_RIGHT, -8, 8);   // 8 pixels in from the top-right corner.
 * box.onPress.add(w -> Flixel.info("Pressed!"));
 * ui.add(box);
 *
 * // Later, in the game's own input code:
 * if (ui.getWidgetAt(mx, my) == box && Flixel.mouse.justPressed(FlixelMouseButton.LEFT)) {
 *   box.press(); // Fires onPress once.
 * }
 * }</pre>
 *
 * @see FlixelUiContainer
 * @see FlixelUiDisplay
 */
public abstract class FlixelUiWidget implements IFlixelBasic, FlixelPositional, FlixelColorable {

  /** Dispatched when {@link #hover()} changes this widget from not hovered to hovered. */
  public final FlixelSignal<FlixelUiWidget> onHover = new FlixelSignal<>();

  /** Dispatched when {@link #unhover()} or {@link #setEnabled(boolean)} clears the hovered state. */
  public final FlixelSignal<FlixelUiWidget> onUnhover = new FlixelSignal<>();

  /** Dispatched when {@link #press()} changes this widget from not pressed to pressed. */
  public final FlixelSignal<FlixelUiWidget> onPress = new FlixelSignal<>();

  /** Dispatched when {@link #release()} changes this widget from pressed to not pressed. */
  public final FlixelSignal<FlixelUiWidget> onRelease = new FlixelSignal<>();

  /** Dispatched when {@link #focus()} gives this widget focus. */
  public final FlixelSignal<FlixelUiWidget> onFocus = new FlixelSignal<>();

  /** Dispatched when this widget loses focus, from {@link #blur()} or from another widget's {@link #focus()}. */
  public final FlixelSignal<FlixelUiWidget> onBlur = new FlixelSignal<>();

  /** The container this widget belongs to, or {@code null} when it has no parent. */
  @Nullable
  FlixelUiContainer parent;

  /**
   * The display this widget is attached to, or {@code null} when it is not on a display.
   *
   * <p>Read-only for subclasses. Use the lifecycle hooks ({@link #onAttached} and
   * {@link #onDetached}) rather than reading this field directly where possible.
   */
  @Nullable
  protected FlixelUiDisplay display;

  /** Local X, measured from the parent's content origin. */
  float x;

  /** Local Y, measured from the parent's content origin. */
  float y;

  /** The logical width of this widget, in the display camera's view space. */
  protected float width;

  /** The logical height of this widget, in the display camera's view space. */
  protected float height;

  /**
   * Absolute X in the display camera's view space, cached by the last layout pass.
   *
   * <p>Equal to what {@link #getScreenX()} returns.
   */
  protected float screenX;

  /**
   * Absolute Y in the display camera's view space, cached by the last layout pass.
   *
   * <p>Equal to what {@link #getScreenY()} returns.
   */
  protected float screenY;

  /** Fraction of the parent's content width, or {@code NaN} to keep {@link #width} fixed. */
  float percentWidth = Float.NaN;

  /** Fraction of the parent's content height, or {@code NaN} to keep {@link #height} fixed. */
  float percentHeight = Float.NaN;

  /** The {@link FlixelAlign} flags this widget is anchored to, or {@code 0} for no anchor. */
  int anchor;

  float anchorOffsetX;
  float anchorOffsetY;

  private float angle;
  private float scrollX;
  private float scrollY;

  @NotNull
  private String styleName = "default";

  /** Batch transform saved while a rotated widget draws; created the first time it is needed. */
  @Nullable
  private FlixelMatrix savedTransform;

  /** Rotation transform applied while a rotated widget draws; created the first time it is needed. */
  @Nullable
  private FlixelMatrix rotatedTransform;

  @NotNull
  private final FlixelColor color = new FlixelColor(FlixelColor.WHITE);

  /**
   * The tooltip text for this widget, or {@code null} when none has been set.
   *
   * <p>Allocated lazily on the first non-empty call to {@link #setTooltip(CharSequence)} and
   * reused afterwards.
   */
  @Nullable
  private FlixelString tooltipText;

  /**
   * Whether {@link FlixelUiDisplay#getWidgetAt(float, float)} can return this widget.
   *
   * <p>Turn it off for purely decorative widgets (such as a label inside a button) so hit-testing
   * falls through to whatever is underneath. Containers default to {@code false}; their children
   * are still searched either way.
   */
  public boolean interactive = true;

  private boolean exists = true;
  private boolean active = true;
  private boolean visible = true;
  private boolean enabled = true;
  private boolean hovered;
  private boolean pressed;
  private boolean focused;

  /** Creates a widget with a size of zero. */
  protected FlixelUiWidget() {
    this(0f, 0f);
  }

  /**
   * Creates a widget with the given size.
   *
   * @param width The starting width in pixels.
   * @param height The starting height in pixels.
   */
  protected FlixelUiWidget(float width, float height) {
    this.width = width;
    this.height = height;
  }

  /**
   * Updates this widget once per frame.
   *
   * <p>The default does nothing. Override it for time-based visuals such as a blinking caret.
   * The UI never reads input here; interaction always comes from the game calling methods such as
   * {@link #hover()}.
   *
   * @param elapsed Seconds elapsed since the last frame.
   */
  @Override
  public void update(float elapsed) {}

  /**
   * Draws this widget (and, for containers, its children) into the batch.
   *
   * <p>Nothing is drawn when the widget does not exist, is invisible, or its combined alpha is
   * zero. When {@link #getAngle()} is not zero, the batch transform is rotated around the widget's
   * center for the duration of the call, so the widget and all of its children turn together, and
   * the previous transform is restored afterwards.
   *
   * <p>A {@link FlixelUiDisplay} calls this for you on its own camera; game code rarely needs to.
   *
   * @param batch The batch to draw into.
   */
  @Override
  public void draw(@NotNull FlixelBatch batch) {
    drawTree(batch, parent != null ? parent.getWorldAlpha() : 1f);
  }

  /**
   * Draws this widget with an already combined parent alpha, then its children.
   *
   * @param batch The batch to draw into.
   * @param parentAlpha The product of every ancestor's alpha.
   */
  void drawTree(@NotNull FlixelBatch batch, float parentAlpha) {
    if (!exists || !visible) {
      return;
    }
    float alpha = parentAlpha * color.a;
    if (alpha <= 0f) {
      return;
    }
    FlixelUiDisplay d = display;
    float drawX = d != null ? d.toDrawX(screenX) : screenX;
    float drawY = d != null ? d.toDrawY(screenY) : screenY;
    int clipDepth = d != null ? d.getClipDepth() : 0;
    boolean rotated = angle != 0f;
    if (rotated) {
      if (savedTransform == null) {
        savedTransform = new FlixelMatrix();
        rotatedTransform = new FlixelMatrix();
      }
      float cx = drawX + width * 0.5f;
      float cy = drawY + height * 0.5f;
      savedTransform.set(batch.getTransform());
      rotatedTransform.set(savedTransform);
      rotatedTransform.translate(cx, cy, 0f);
      rotatedTransform.rotateZ(angle);
      rotatedTransform.translate(-cx, -cy, 0f);
      batch.setTransform(rotatedTransform);
    }
    try {
      drawSelf(batch, drawX, drawY, alpha);
      drawChildren(batch, alpha);
    } finally {
      // Undo anything this widget left behind, even when drawing threw, so the clip stack and the
      // batch transform stay balanced for its siblings.
      if (d != null) {
        d.restoreClipDepth(batch, clipDepth);
      }
      if (rotated) {
        batch.setTransform(savedTransform);
      }
    }
  }

  /**
   * Draws this widget's children; widgets without children draw nothing here.
   *
   * @param batch The batch to draw into.
   * @param alpha This widget's combined alpha, passed down to the children.
   */
  void drawChildren(@NotNull FlixelBatch batch, float alpha) {}

  /**
   * Draws this widget's own visuals, before any of its children.
   *
   * <p>The coordinates are ready to hand to the batch: they are this widget's top-left corner in
   * the camera's draw space. Multiply any color's alpha by {@code alpha}, which already includes
   * every ancestor's alpha. Rotation is handled by the caller through the batch transform, so draw
   * as if the widget were not rotated. Restore the batch color to white when you change it.
   *
   * @param batch The batch to draw into.
   * @param drawX The left edge in draw coordinates.
   * @param drawY The top edge in draw coordinates.
   * @param alpha The combined alpha of this widget and all of its ancestors.
   */
  protected abstract void drawSelf(@NotNull FlixelBatch batch, float drawX, float drawY, float alpha);

  /**
   * Restricts drawing to a rectangle until the matching {@link #popClip(FlixelBatch)}, for widgets
   * that clip their own parts, such as a text box hiding text that scrolled out of view.
   *
   * <p>The rectangle is in the camera's view space, the same space as {@link #getScreenX()} and
   * {@link #getScreenY()}, and it is intersected with any clip that is already active, such as the
   * content area of a clipping {@link FlixelUiContainer} around this widget. Clip rectangles are
   * axis-aligned and ignore {@link #getAngle()}. When this widget is not on a display, nothing is
   * clipped and the method returns {@code true}. Any clip still pushed when {@link #drawSelf(FlixelBatch,
   * float, float, float)} returns (or throws) is popped for you, but popping it yourself keeps the
   * intent clear.
   *
   * <pre>{@code
   * @Override
   * protected void drawSelf(FlixelBatch batch, float drawX, float drawY, float alpha) {
   *   background.draw(batch, drawX, drawY, getWidth(), getHeight());
   *   if (pushClip(batch, getScreenX() + 4, getScreenY() + 4, getWidth() - 8, getHeight() - 8)) {
   *     drawScrolledText(batch); // Only the part inside the 4 pixel border shows.
   *   }
   *   popClip(batch);
   * }
   * }</pre>
   *
   * @param batch The batch being drawn into; it is flushed before the clip changes.
   * @param x The left edge, in the camera's view space.
   * @param y The top edge, in the camera's view space.
   * @param width The width in pixels.
   * @param height The height in pixels.
   * @return {@code false} when the clipped area is empty, so drawing until the pop can be skipped;
   *     otherwise {@code true}.
   * @throws IllegalStateException If {@link FlixelUiDisplay#MAX_CLIP_DEPTH} clips are already
   *     pushed.
   * @see FlixelUiDisplay#pushClip(FlixelBatch, float, float, float, float)
   */
  protected boolean pushClip(@NotNull FlixelBatch batch, float x, float y, float width, float height) {
    FlixelUiDisplay d = display;
    return d == null || d.pushClip(batch, x, y, width, height);
  }

  /**
   * Removes the clip added by the last {@link #pushClip(FlixelBatch, float, float, float, float)}.
   *
   * <p>Does nothing when this widget is not on a display.
   *
   * @param batch The batch being drawn into; it is flushed before the clip changes.
   * @throws IllegalStateException If no clip is pushed on the display.
   * @see FlixelUiDisplay#popClip(FlixelBatch)
   */
  protected void popClip(@NotNull FlixelBatch batch) {
    FlixelUiDisplay d = display;
    if (d != null) {
      d.popClip(batch);
    }
  }

  /**
   * Checks whether a point in the display camera's view space lies inside this widget.
   *
   * <p>The test uses {@link #getScreenX()}, {@link #getScreenY()}, {@link #getWidth()}, and
   * {@link #getHeight()}. Like the angle of a {@link FlixelObject}, {@link #getAngle()} only rotates
   * the visuals and does not affect this test. The left and top edges are inside; the right and
   * bottom edges are not, so two widgets that touch never both contain the same point.
   *
   * @param px The X to test, in the same space as {@code Flixel.mouse.getWorldX(camera)}.
   * @param py The Y to test, in the same space as {@code Flixel.mouse.getWorldY(camera)}.
   * @return {@code true} if the point is inside this widget's rectangle.
   */
  public boolean containsPoint(float px, float py) {
    return px >= screenX && px < screenX + width && py >= screenY && py < screenY + height;
  }

  /**
   * Returns the deepest widget at a point, starting from this widget.
   *
   * @param px The X to test, in camera view space.
   * @param py The Y to test, in camera view space.
   * @return This widget when it can be hit and contains the point, otherwise {@code null}.
   */
  @Nullable
  FlixelUiWidget hitTest(float px, float py) {
    if (!exists || !visible) {
      return null;
    }
    return canBeHit() && containsPoint(px, py) ? this : null;
  }

  /**
   * Returns whether hit-testing may return this widget itself.
   *
   * @return {@code true} when this widget is both interactive and enabled.
   */
  boolean canBeHit() {
    return interactive && enabled;
  }

  /**
   * Marks this widget as hovered, for example because the game found the mouse over it.
   *
   * <p>Does nothing when the widget is disabled or already hovered; otherwise it calls
   * {@link #onStateChanged()} and dispatches {@link #onHover}.
   */
  public void hover() {
    if (!enabled || hovered) {
      return;
    }
    hovered = true;
    onStateChanged();
    onHover.dispatch(this);
  }

  /**
   * Clears the hovered state, for example because the game found the mouse left this widget.
   *
   * <p>Does nothing when the widget is disabled or not hovered; otherwise it calls
   * {@link #onStateChanged()} and dispatches {@link #onUnhover}.
   */
  public void unhover() {
    if (!enabled || !hovered) {
      return;
    }
    hovered = false;
    onStateChanged();
    onUnhover.dispatch(this);
  }

  /**
   * Marks this widget as pressed, for example because the game saw a mouse button go down over it.
   *
   * <p>Does nothing when the widget is disabled or already pressed; otherwise it calls
   * {@link #onStateChanged()} and dispatches {@link #onPress}.
   */
  public void press() {
    if (!enabled || pressed) {
      return;
    }
    pressed = true;
    onStateChanged();
    onPress.dispatch(this);
  }

  /**
   * Clears the pressed state, for example because the game saw the mouse button come back up.
   *
   * <p>Does nothing when the widget is disabled or not pressed; otherwise it calls
   * {@link #onStateChanged()} and dispatches {@link #onRelease}.
   */
  public void release() {
    if (!enabled || !pressed) {
      return;
    }
    pressed = false;
    onStateChanged();
    onRelease.dispatch(this);
  }

  /**
   * Gives this widget focus, for example because a gamepad moved the selection onto it.
   *
   * <p>Only one widget per display holds focus, so the display's previously focused widget is
   * blurred first (and dispatches its own {@link #onBlur}). A widget that is not on a display can
   * still hold focus locally, and takes the display's focus when it is added to one. Does nothing
   * when the widget is disabled or already focused.
   */
  public void focus() {
    if (!enabled || focused) {
      return;
    }
    FlixelUiDisplay d = display;
    if (d != null) {
      FlixelUiWidget previous = d.focused;
      if (previous != null && previous != this) {
        previous.blur();
      }
      d.focused = this;
    }
    focused = true;
    onStateChanged();
    onFocus.dispatch(this);
  }

  /**
   * Removes focus from this widget.
   *
   * <p>Does nothing when the widget is not focused; otherwise it calls {@link #onStateChanged()}
   * and dispatches {@link #onBlur}.
   */
  public void blur() {
    if (!focused) {
      return;
    }
    FlixelUiDisplay d = display;
    if (d != null && d.focused == this) {
      d.focused = null;
    }
    focused = false;
    onStateChanged();
    onBlur.dispatch(this);
  }

  /**
   * Enables or disables this widget.
   *
   * <p>A disabled widget ignores every interaction method and is skipped by
   * {@link FlixelUiDisplay#getWidgetAt(float, float)}. Disabling clears the hovered and pressed
   * states and blurs the widget: {@link #onUnhover} and {@link #onBlur} are dispatched when those
   * states were set, but {@link #onRelease} is not, because a release usually means "activate" and
   * a widget that is being disabled must never activate.
   *
   * @param enabled {@code true} to enable the widget, {@code false} to disable it.
   */
  public void setEnabled(boolean enabled) {
    if (this.enabled == enabled) {
      return;
    }
    if (enabled) {
      this.enabled = true;
      onStateChanged();
      return;
    }
    boolean wasHovered = hovered;
    hovered = false;
    pressed = false;
    blur();
    this.enabled = false;
    onStateChanged();
    if (wasHovered) {
      onUnhover.dispatch(this);
    }
  }

  /**
   * Clears the hovered and pressed states without firing {@link #onRelease}, for cleanup when a
   * modal that contains this widget is closed.
   *
   * <p>The behavior mirrors the first half of {@link #setEnabled(boolean) setEnabled(false)}:
   * both states are cleared silently, {@link #onStateChanged()} is called once when either was set,
   * and {@link #onUnhover} is dispatched when the widget was hovered. {@link #onRelease} is
   * intentionally not dispatched because a press cleared this way does not represent an activation.
   * Focus and the enabled flag are not changed.
   */
  void clearInteractionStates() {
    boolean wasHovered = hovered;
    boolean wasPressed = pressed;
    hovered = false;
    pressed = false;
    if (wasHovered || wasPressed) {
      onStateChanged();
    }
    if (wasHovered) {
      onUnhover.dispatch(this);
    }
  }

  /**
   * Called whenever an interaction state changes, before the matching signal is dispatched.
   *
   * <p>Override it to refresh visuals that depend on {@link #isHovered()}, {@link #isPressed()},
   * {@link #isFocused()}, or {@link #isEnabled()}, such as picking a different background.
   */
  protected void onStateChanged() {}

  /**
   * Assigns tooltip text to this widget so the game can show it via
   * {@link FlixelUiDisplay#showTooltip(FlixelUiWidget)}.
   *
   * <p>The characters are copied into a {@link FlixelString} owned by this widget, so a
   * reused buffer can be passed without risk of the tooltip changing later. The string is
   * allocated the first time a non-empty text is set and then reused. Passing {@code null} or an
   * empty sequence clears the tooltip.
   *
   * <pre>{@code
   * playButton.setTooltip("Start the game");
   * // Later, in the game's input handling:
   * FlixelUiWidget hit = ui.getWidgetAt(mx, my);
   * if (hit != hovered) {
   *   ui.hideTooltip();
   *   if (hit != null && hit.hasTooltip()) ui.showTooltip(hit, 0.4f);
   *   hovered = hit;
   * }
   * }</pre>
   *
   * @param text The tooltip text to store, or {@code null} to clear it.
   */
  public void setTooltip(@Nullable CharSequence text) {
    if (text == null || text.isEmpty()) {
      if (tooltipText != null) {
        tooltipText.clear();
      }
      return;
    }
    if (tooltipText == null) {
      tooltipText = new FlixelString();
    }
    tooltipText.set(text);
  }

  /**
   * Returns the tooltip text for this widget.
   *
   * @return The tooltip text, or {@code null} when none has been set.
   */
  @Nullable
  public CharSequence getTooltip() {
    return (tooltipText != null && !tooltipText.isEmpty()) ? tooltipText : null;
  }

  /**
   * Returns whether this widget has non-empty tooltip text.
   *
   * @return {@code true} when there is text to show in a tooltip.
   */
  public boolean hasTooltip() {
    return tooltipText != null && !tooltipText.isEmpty();
  }

  /**
   * Called whenever the style this widget should wear may have changed.
   *
   * <p>That happens when the widget is attached to a display, when {@link #setStyle(String)} is
   * called while it is attached, and when the display's skin is replaced with
   * {@link FlixelUiDisplay#setSkin(FlixelUiSkin)}. Widgets with a style override it, fetch their
   * style with {@link #resolveStyle(Class)}, and refresh whatever depends on it:
   *
   * <pre>{@code
   * @Override
   * protected void onStyleChanged() {
   *   if (!customStyle) {
   *     style = resolveStyle(FlixelUiButtonStyle.class);
   *   }
   *   invalidateLayout(); // Padding or font size may have changed.
   * }
   * }</pre>
   *
   * <p>It runs after {@link #onAttached(FlixelUiDisplay)}, and a container runs it before its
   * children, so a child can rely on its parent's style being up to date.
   */
  protected void onStyleChanged() {}

  /**
   * Returns the style named {@link #getStyleName()} of the given class from the display's skin.
   *
   * <p>Call it from {@link #onStyleChanged()}. For example, a button calls
   * {@code resolveStyle(FlixelUiButtonStyle.class)} to get the button style its style name refers
   * to.
   *
   * @param type The exact style class this widget uses.
   * @param <T> The style's type.
   * @return The style; never {@code null}.
   * @throws IllegalStateException If this widget is not attached to a display.
   * @throws IllegalArgumentException If the display's skin has no style of that class with this
   *     widget's style name (see {@link FlixelUiSkin#get(Class, String)}).
   */
  @NotNull
  protected <T extends FlixelUiStyle> T resolveStyle(@NotNull Class<T> type) {
    FlixelUiDisplay d = display;
    if (d == null) {
      throw new IllegalStateException("Cannot resolve a style for a widget that is not on a display.");
    }
    return d.getSkin().get(type, styleName);
  }

  /**
   * Anchors this widget to a position inside its parent's content area.
   *
   * <p>The parent places the widget at the chosen {@link FlixelAlign} position (for example
   * {@link FlixelAlign#BOTTOM_RIGHT}) and then adds the offsets, so
   * {@code anchor(BOTTOM_RIGHT, -8, -8)} keeps the widget 8 pixels in from that corner no matter
   * how large the parent is. While
   * anchored, the layout owns {@link #getX()} and {@link #getY()}; move the widget with the offsets
   * instead. Pass {@code 0} (or call {@link #clearAnchor()}) to stop anchoring, which keeps the
   * current position as a plain local offset. Children of a {@link FlixelUiStack} ignore anchors.
   *
   * @param align The {@link FlixelAlign} flags to anchor to, or {@code 0} for no anchor.
   * @param offsetX Pixels added to the anchored X.
   * @param offsetY Pixels added to the anchored Y.
   */
  public void anchor(int align, float offsetX, float offsetY) {
    anchor = align;
    anchorOffsetX = offsetX;
    anchorOffsetY = offsetY;
    invalidateLayout();
  }

  /** Removes the anchor, keeping the widget's current local position. */
  public void clearAnchor() {
    anchor(0, 0f, 0f);
  }

  /**
   * Sizes this widget as a fraction of its parent's content size.
   *
   * <p>For example, {@code setPercentSize(1f, Float.NaN)} makes the widget exactly as wide as its
   * parent's content area while keeping its current height. The size is applied on every layout
   * pass, so it follows the parent when the parent resizes.
   *
   * @param width The fraction of the parent's content width, or {@link Float#NaN} to keep the
   *     width fixed.
   * @param height The fraction of the parent's content height, or {@link Float#NaN} to keep the
   *     height fixed.
   */
  public void setPercentSize(float width, float height) {
    percentWidth = width;
    percentHeight = height;
    invalidateLayout();
  }

  /**
   * Returns the width this widget would like to have.
   *
   * <p>Stacks use it to place their children. The default is the current width; widgets whose
   * natural size comes from their content, such as a label, override it.
   *
   * @return The preferred width in pixels.
   */
  public float getPreferredWidth() {
    return width;
  }

  /**
   * Returns the height this widget would like to have.
   *
   * @return The preferred height in pixels.
   * @see #getPreferredWidth()
   */
  public float getPreferredHeight() {
    return height;
  }

  /**
   * Marks the display's layout as out of date so it is recomputed before the next update, draw,
   * or hit-test.
   *
   * <p>Setters that affect layout call this for you. Call it yourself only when something your
   * {@link #getPreferredWidth()} depends on changes. Does nothing when the widget is not on a
   * display.
   */
  public void invalidateLayout() {
    if (display != null) {
      display.invalidate();
    }
  }

  /**
   * Measures this widget and, for containers, its whole subtree, children first.
   */
  void measureTree() {
    onMeasure();
  }

  /**
   * Lays out this widget's children (if any) now that its own screen position is known.
   */
  void layoutTree() {
    onLayout();
  }

  /**
   * Called during the measure step of a layout pass, after every child has been measured and
   * before any positions are computed.
   *
   * <p>Override it when the preferred size depends on content that is cheap to recompute, such as
   * a stack adding up its children. Must not allocate.
   */
  protected void onMeasure() {}

  /**
   * Called after the parent has positioned this widget and {@link #getScreenX()} and
   * {@link #getScreenY()} are up to date.
   *
   * <p>Override it to position the widget's own parts, such as a label inside a button. It runs
   * before the widget's children are laid out. Must not allocate.
   */
  protected void onLayout() {}

  /**
   * Attaches this widget to a display.
   *
   * @param display The display this widget now belongs to.
   */
  void attach(@NotNull FlixelUiDisplay display) {
    this.display = display;
    if (focused) {
      FlixelUiWidget previous = display.focused;
      if (previous != null && previous != this) {
        previous.blur();
      }
      display.focused = this;
    }
    onAttached(display);
    onStyleChanged();
  }

  /**
   * Calls {@link #onStyleChanged()} on this widget and, for containers, on every descendant, parent
   * first.
   */
  void refreshStyleTree() {
    onStyleChanged();
  }

  /** Detaches this widget from its display, blurring it first if it held the display's focus. */
  void detach() {
    FlixelUiDisplay d = display;
    if (d == null) {
      return;
    }
    if (d.focused == this) {
      blur();
    }
    onDetached();
    display = null;
  }

  /**
   * Called when this widget is attached to a display, either directly or because an ancestor was.
   *
   * <p>Override it to pick up anything that comes from the display. Styles have their own hook,
   * {@link #onStyleChanged()}, which runs right after this one.
   *
   * @param display The display this widget now belongs to.
   */
  protected void onAttached(@NotNull FlixelUiDisplay display) {}

  /**
   * Called when this widget is detached from its display. {@link #getDisplay()} still returns the
   * old display during this call.
   */
  protected void onDetached() {}

  /**
   * Multiplies this widget's alpha by the alpha of every ancestor.
   *
   * <p>This is the alpha the widget is actually drawn with, so fading a panel fades everything in
   * it. Walks up the parent chain without allocating.
   *
   * @return The combined alpha, from {@code 0} to {@code 1} for alphas in that range.
   */
  public float getWorldAlpha() {
    float a = color.a;
    for (FlixelUiContainer p = parent; p != null; p = p.parent) {
      a *= p.getColor().a;
    }
    return a;
  }

  @Override
  public void kill() {
    exists = false;
    invalidateLayout();
  }

  @Override
  public void revive() {
    exists = true;
    invalidateLayout();
  }

  @Override
  public void setKilled(boolean killed) {
    if (killed) {
      kill();
    } else {
      revive();
    }
  }

  @Override
  public void toggleKilled() {
    setKilled(exists);
  }

  @Override
  public void toggleVisible() {
    visible = !visible;
  }

  /**
   * Destroys this widget for good.
   *
   * <p>Every signal listener is removed, the widget leaves its parent (and its display), its
   * interaction states are cleared, and it stops existing. A destroyed widget should not be used
   * again; use {@link #kill()} to hide it temporarily instead. Containers also destroy all of their
   * children.
   */
  @Override
  public void destroy() {
    onHover.clear();
    onUnhover.clear();
    onPress.clear();
    onRelease.clear();
    onFocus.clear();
    onBlur.clear();
    if (parent != null) {
      parent.remove(this);
    } else if (display != null) {
      detach();
    }
    hovered = false;
    pressed = false;
    focused = false;
    active = false;
    exists = false;
    visible = true;
  }

  /**
   * Calls {@link #destroy()}, matching {@link FlixelBasic#reset()}. Marked final so subclasses
   * override {@link #destroy()} instead.
   */
  @Override
  public final void reset() {
    destroy();
  }

  @Override
  public void setSize(float width, float height) {
    this.width = width;
    this.height = height;
    invalidateLayout();
  }

  /**
   * Sets the local position.
   *
   * <p>Overridden only so the layout is invalidated once instead of twice.
   *
   * @param x The new local X.
   * @param y The new local Y.
   */
  @Override
  public void setPosition(float x, float y) {
    this.x = x;
    this.y = y;
    invalidateLayout();
  }

  @Override
  public void changeX(float dx) {
    setX(x + dx);
  }

  @Override
  public void changeY(float dy) {
    setY(y + dy);
  }

  @Override
  public void changeAngle(float deltaDegrees) {
    angle += deltaDegrees;
  }

  /**
   * Returns the horizontal center in local coordinates, {@code getX() + getWidth() / 2}.
   *
   * @return The local center X.
   */
  @Override
  public float getMidpointX() {
    return x + width * 0.5f;
  }

  /**
   * Returns the vertical center in local coordinates, {@code getY() + getHeight() / 2}.
   *
   * @return The local center Y.
   */
  @Override
  public float getMidpointY() {
    return y + height * 0.5f;
  }

  /**
   * Returns the local X; widgets do not move by themselves, so there is no separate last position.
   *
   * @return The same value as {@link #getX()}.
   */
  @Override
  public float getLastX() {
    return x;
  }

  /**
   * Returns the local Y; widgets do not move by themselves, so there is no separate last position.
   *
   * @return The same value as {@link #getY()}.
   */
  @Override
  public float getLastY() {
    return y;
  }

  /**
   * Returns the X position relative to the parent's content origin.
   *
   * @return The local X.
   */
  @Override
  public float getX() {
    return x;
  }

  @Override
  public void setX(float x) {
    this.x = x;
    invalidateLayout();
  }

  /**
   * Returns the Y position relative to the parent's content origin.
   *
   * @return The local Y.
   */
  @Override
  public float getY() {
    return y;
  }

  @Override
  public void setY(float y) {
    this.y = y;
    invalidateLayout();
  }

  @Override
  public float getWidth() {
    return width;
  }

  @Override
  public void setWidth(float width) {
    this.width = width;
    invalidateLayout();
  }

  @Override
  public float getHeight() {
    return height;
  }

  @Override
  public void setHeight(float height) {
    this.height = height;
    invalidateLayout();
  }

  /**
   * Returns the absolute left edge in the display camera's view space, as of the last layout pass.
   *
   * <p>This is the same space as {@code Flixel.mouse.getWorldX(camera)}, so the cursor is over this
   * widget's left edge exactly when the two are equal.
   *
   * @return The cached screen X.
   */
  public float getScreenX() {
    return screenX;
  }

  /**
   * Returns the absolute top edge in the display camera's view space, as of the last layout pass.
   *
   * @return The cached screen Y.
   * @see #getScreenX()
   */
  public float getScreenY() {
    return screenY;
  }

  /**
   * Returns the visual rotation in degrees.
   *
   * <p>The widget and its children are drawn rotated around the widget's center. Like the angle of
   * a {@link FlixelObject}, it does not affect layout, {@link #containsPoint(float, float)}, or
   * hit-testing.
   *
   * @return The angle in degrees.
   */
  @Override
  public float getAngle() {
    return angle;
  }

  @Override
  public void setAngle(float degrees) {
    angle = degrees;
  }

  /**
   * Returns the stored horizontal scroll factor.
   *
   * <p>The value is kept so tools written against {@link FlixelPositional} keep working, but a
   * {@link FlixelUiDisplay} always draws UI as if the scroll factor were {@code 0}: widgets stay put
   * on screen no matter where the camera scrolls.
   *
   * @return The stored scroll factor on X, {@code 0} by default.
   */
  @Override
  public float getScrollX() {
    return scrollX;
  }

  /**
   * Returns the stored vertical scroll factor.
   *
   * @return The stored scroll factor on Y, {@code 0} by default.
   * @see #getScrollX()
   */
  @Override
  public float getScrollY() {
    return scrollY;
  }

  /**
   * Stores new scroll factors.
   *
   * @param scrollX The horizontal scroll factor to store.
   * @param scrollY The vertical scroll factor to store.
   * @see #getScrollX()
   */
  @Override
  public void setScrollFactor(float scrollX, float scrollY) {
    this.scrollX = scrollX;
    this.scrollY = scrollY;
  }

  /**
   * Returns the live tint. Its alpha is this widget's own alpha; see {@link #getWorldAlpha()}.
   *
   * @return The backing color; never {@code null}.
   */
  @NotNull
  @Override
  public FlixelColor getColor() {
    return color;
  }

  @Override
  public void setColor(@NotNull FlixelColor color) {
    this.color.set(color);
  }

  public float getAlpha() {
    return color.a;
  }

  public void setAlpha(float alpha) {
    color.a = alpha;
  }

  /**
   * Returns the name of the style this widget uses from the display's skin.
   *
   * @return The style name, {@code "default"} unless changed.
   */
  @NotNull
  public String getStyleName() {
    return styleName;
  }

  /**
   * Chooses which named style this widget uses from the display's skin, such as {@code "fab"} for a
   * floating action button.
   *
   * <p>While the widget is on a display, the new style is applied right away through
   * {@link #onStyleChanged()}, and the layout is marked out of date. Otherwise it is applied when
   * the widget is attached.
   *
   * @param name The style name.
   * @throws IllegalArgumentException If {@code name} is {@code null}, or if the widget is on a
   *     display whose skin has no style with that name for this widget.
   */
  public void setStyle(@NotNull String name) {
    if (name == null) {
      throw new IllegalArgumentException("Style name must not be null.");
    }
    styleName = name;
    if (display != null) {
      onStyleChanged();
      invalidateLayout();
    }
  }

  /**
   * Returns the anchor flags, or {@code 0} when the widget is not anchored.
   *
   * @return The {@link FlixelAlign} flags passed to {@link #anchor(int, float, float)}.
   */
  public int getAnchor() {
    return anchor;
  }

  public float getAnchorOffsetX() {
    return anchorOffsetX;
  }

  public float getAnchorOffsetY() {
    return anchorOffsetY;
  }

  /**
   * Returns the fraction of the parent's content width, or {@code NaN} when the width is fixed.
   *
   * @return The width fraction.
   */
  public float getPercentWidth() {
    return percentWidth;
  }

  /**
   * Returns the fraction of the parent's content height, or {@code NaN} when the height is fixed.
   *
   * @return The height fraction.
   */
  public float getPercentHeight() {
    return percentHeight;
  }

  @Nullable
  public FlixelUiContainer getParent() {
    return parent;
  }

  @Nullable
  public FlixelUiDisplay getDisplay() {
    return display;
  }

  @Override
  public boolean isExists() {
    return exists;
  }

  @Override
  public void setExists(boolean exists) {
    this.exists = exists;
    invalidateLayout();
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

  public boolean isEnabled() {
    return enabled;
  }

  public boolean isHovered() {
    return hovered;
  }

  public boolean isPressed() {
    return pressed;
  }

  public boolean isFocused() {
    return focused;
  }
}
