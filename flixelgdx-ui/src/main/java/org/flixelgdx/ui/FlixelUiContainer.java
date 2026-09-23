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

import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.util.FlixelAlign;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A widget that holds other widgets and lays them out inside its content area.
 *
 * <p>The content area is the container's rectangle minus its padding. On every layout pass each
 * child first gets its {@linkplain FlixelUiWidget#setPercentSize(float, float) percent size}
 * applied, then, if it is {@linkplain FlixelUiWidget#anchor(int, float, float) anchored}, it is
 * placed at its {@link FlixelAlign} position plus offsets. Children without an anchor keep their
 * own {@link #getX()} and {@link #getY()} as offsets from the content area's top-left corner.
 *
 * <p>Children are drawn in the order they were added, after the container itself, so the last
 * child added is on top. Hit-testing walks them in the opposite order for the same reason.
 * Containers are not {@link #interactive} by default, so clicking empty space inside one falls
 * through to whatever is underneath, while their children are still found.
 *
 * <pre>{@code
 * FlixelUiContainer bar = new FlixelUiContainer(0, 48);
 * bar.setPercentSize(1f, Float.NaN);            // Full width, fixed 48 pixel height.
 * bar.anchor(FlixelAlign.BOTTOM, 0, 0);         // Glued to the bottom of the screen.
 * bar.setPadding(8);
 * bar.add(menuButton);                          // Unanchored: sits at the padding corner.
 * settingsButton.anchor(FlixelAlign.RIGHT, 0, 0);
 * bar.add(settingsButton);                      // Vertically centered on the right.
 * ui.add(bar);
 * }</pre>
 *
 * <h2>Clipping</h2>
 *
 * <p>By default a child that sticks out of its container is drawn in full. Turn on
 * {@link #setClipChildren(boolean)} to cut children off at the edges of the content area, like a
 * window frame that hides whatever is past its borders. Clipping also applies to
 * {@link FlixelUiDisplay#getWidgetAt(float, float)}: the part of a child outside the content area
 * cannot be hit. The container's own drawing (such as a panel's background) is never clipped by
 * this setting.
 *
 * <pre>{@code
 * FlixelUiContainer list = new FlixelUiContainer(200, 120);
 * list.setPadding(4);
 * list.setClipChildren(true);     // Rows scrolled past the 4 pixel border are cut off.
 * for (int i = 0; i < rows.length; i++) {
 *   rows[i].setPosition(0, i * 24 - scrollOffset);
 *   list.add(rows[i]);
 * }
 * }</pre>
 *
 * <p>Clip rectangles are axis-aligned: a rotated container still clips to its unrotated content
 * area, and clips nest by intersecting, so a child of two clipping containers only shows where
 * both content areas overlap. At most {@link FlixelUiDisplay#MAX_CLIP_DEPTH} clips can nest.
 */
public class FlixelUiContainer extends FlixelUiWidget {

  /** The children in draw order; the last one is drawn on top. */
  final FlixelArray<FlixelUiWidget> children = new FlixelArray<>(FlixelUiWidget[]::new);

  private float padLeft;
  private float padTop;
  private float padRight;
  private float padBottom;

  private boolean clipChildren;

  /** Creates an empty container with a size of zero. */
  public FlixelUiContainer() {
    this(0f, 0f);
  }

  /**
   * Creates an empty container with the given size.
   *
   * @param width The starting width in pixels.
   * @param height The starting height in pixels.
   */
  public FlixelUiContainer(float width, float height) {
    super(width, height);
    interactive = false;
  }

  /**
   * Adds a widget as the last (topmost) child.
   *
   * <p>A widget that already has a parent is moved here from it. When this container is on a
   * display, the widget and all of its descendants are attached to that display. Adding a widget
   * that is already a child of this container does nothing.
   *
   * @param widget The widget to add.
   * @throws IllegalArgumentException If {@code widget} is {@code null}, is this container or one of
   *     its ancestors, or is one of a display's own layers.
   */
  public void add(@NotNull FlixelUiWidget widget) {
    if (widget == null) {
      throw new IllegalArgumentException("Cannot add a null widget.");
    }
    if (widget.parent == this) {
      return;
    }
    for (FlixelUiWidget p = this; p != null; p = p.parent) {
      if (p == widget) {
        throw new IllegalArgumentException("A container cannot be added to itself or its own descendant.");
      }
    }
    if (widget.parent == null && widget.display != null) {
      throw new IllegalArgumentException("A display layer cannot be added to a container.");
    }
    FlixelUiContainer old = widget.parent;
    if (old != null) {
      old.unlink(widget);
      old.invalidateLayout();
    }
    children.add(widget);
    widget.parent = this;
    if (widget.display != display) {
      widget.detach();
      if (display != null) {
        widget.attach(display);
      }
    }
    invalidateLayout();
  }

  /**
   * Removes a child, detaching it (and its descendants) from the display.
   *
   * <p>A removed widget that held the display's focus is blurred. The widget is not destroyed, so it
   * can be added somewhere else later.
   *
   * @param widget The child to remove.
   * @return {@code true} if the widget was a child of this container and has been removed.
   */
  public boolean remove(@Nullable FlixelUiWidget widget) {
    if (widget == null || widget.parent != this) {
      return false;
    }
    unlink(widget);
    widget.detach();
    invalidateLayout();
    return true;
  }

  /** Removes every child, detaching them from the display without destroying them. */
  public void clear() {
    while (!children.isEmpty()) {
      FlixelUiWidget child = children.pop();
      child.parent = null;
      child.detach();
    }
    invalidateLayout();
  }

  /**
   * Returns the child at an index in draw order.
   *
   * @param index The index, from {@code 0} (drawn first) to {@code getChildCount() - 1} (on top).
   * @return The child at that index.
   * @throws IndexOutOfBoundsException If the index is out of range.
   */
  @NotNull
  public FlixelUiWidget getChildAt(int index) {
    return children.get(index);
  }

  /**
   * Sets the same padding on all four sides.
   *
   * @param all The padding in pixels.
   */
  public void setPadding(float all) {
    setPadding(all, all, all, all);
  }

  /**
   * Sets the space between the container's edges and its content area.
   *
   * @param left The left padding in pixels.
   * @param top The top padding in pixels.
   * @param right The right padding in pixels.
   * @param bottom The bottom padding in pixels.
   */
  public void setPadding(float left, float top, float right, float bottom) {
    padLeft = left;
    padTop = top;
    padRight = right;
    padBottom = bottom;
    invalidateLayout();
  }

  /**
   * Updates every child that exists and is active, in draw order.
   *
   * <p>Subclasses that override this must call {@code super.update(elapsed)} so children keep
   * updating.
   *
   * @param elapsed Seconds elapsed since the last frame.
   */
  @Override
  public void update(float elapsed) {
    FlixelUiWidget[] items = children.begin();
    for (int i = 0, n = children.getSize(); i < n; i++) {
      FlixelUiWidget child = items[i];
      if (child.isExists() && child.isActive()) {
        child.update(elapsed);
      }
    }
    children.end();
  }

  /**
   * Destroys this container and every child in it.
   */
  @Override
  public void destroy() {
    FlixelUiWidget[] items = children.begin();
    for (int i = 0, n = children.getSize(); i < n; i++) {
      FlixelUiWidget child = items[i];
      child.parent = null;
      child.destroy();
    }
    children.end();
    children.clear();
    super.destroy();
  }

  /**
   * Draws nothing for the container itself; subclasses such as panels draw a background here.
   *
   * @param batch The batch to draw into.
   * @param drawX The left edge in draw coordinates.
   * @param drawY The top edge in draw coordinates.
   * @param alpha The combined alpha of this container and its ancestors.
   */
  @Override
  protected void drawSelf(@NotNull FlixelBatch batch, float drawX, float drawY, float alpha) {}

  /**
   * Draws every child in order, inside the content area's clip when
   * {@link #isClipChildren()} is on.
   *
   * @param batch The batch to draw into.
   * @param alpha This container's combined alpha, passed down to the children.
   */
  @Override
  void drawChildren(@NotNull FlixelBatch batch, float alpha) {
    if (!clipChildren || display == null) {
      drawEachChild(batch, alpha);
      return;
    }
    // The push happens before the try, so a push that throws never triggers an unmatched pop.
    boolean visible = pushClip(batch, screenX + padLeft, screenY + padTop, getContentWidth(), getContentHeight());
    try {
      if (visible) {
        drawEachChild(batch, alpha);
      }
    } finally {
      popClip(batch);
    }
  }

  /**
   * Returns the deepest, topmost widget under a point: children are searched in reverse draw order
   * before this container itself, so whatever is drawn on top wins.
   *
   * <p>Widgets that do not exist or are invisible are skipped along with their children. Disabled
   * or non-interactive widgets are never returned themselves, but their children are still
   * searched. When {@link #isClipChildren()} is on and the point is outside the content area, no
   * child is searched, because that part of every child is clipped away. Checking each clipping
   * ancestor on the way down is the same as testing the point against the intersection of all
   * their content areas, without keeping any extra state.
   *
   * @param px The X to test, in camera view space.
   * @param py The Y to test, in camera view space.
   * @return The widget that was hit, or {@code null}.
   */
  @Nullable
  @Override
  FlixelUiWidget hitTest(float px, float py) {
    if (!isExists() || !isVisible()) {
      return null;
    }
    if (!clipChildren || contentContains(px, py)) {
      FlixelUiWidget[] items = children.getItems();
      for (int i = children.getSize() - 1; i >= 0; i--) {
        FlixelUiWidget hit = items[i].hitTest(px, py);
        if (hit != null) {
          return hit;
        }
      }
    }
    return canBeHit() && containsPoint(px, py) ? this : null;
  }

  /**
   * Attaches this container first, then every child, so a child's
   * {@link #onAttached(FlixelUiDisplay)} can rely on its parent already being attached.
   *
   * @param display The display this container now belongs to.
   */
  @Override
  void attach(@NotNull FlixelUiDisplay display) {
    super.attach(display);
    FlixelUiWidget[] items = children.begin();
    for (int i = 0, n = children.getSize(); i < n; i++) {
      items[i].attach(display);
    }
    children.end();
  }

  /** Refreshes this container's style first, then every child's, in the same order as attaching. */
  @Override
  void refreshStyleTree() {
    super.refreshStyleTree();
    FlixelUiWidget[] items = children.begin();
    for (int i = 0, n = children.getSize(); i < n; i++) {
      items[i].refreshStyleTree();
    }
    children.end();
  }

  /**
   * Clears the hovered and pressed states of this container and every descendant.
   *
   * @see FlixelUiWidget#clearInteractionStates()
   */
  @Override
  void clearInteractionStates() {
    super.clearInteractionStates();
    FlixelUiWidget[] items = children.getItems();
    for (int i = 0, n = children.getSize(); i < n; i++) {
      items[i].clearInteractionStates();
    }
  }

  /** Detaches every child first, then this container, which is the reverse of attaching. */
  @Override
  void detach() {
    if (display == null) {
      return;
    }
    FlixelUiWidget[] items = children.begin();
    for (int i = 0, n = children.getSize(); i < n; i++) {
      items[i].detach();
    }
    children.end();
    super.detach();
  }

  @Override
  void measureTree() {
    FlixelUiWidget[] items = children.begin();
    for (int i = 0, n = children.getSize(); i < n; i++) {
      FlixelUiWidget child = items[i];
      if (child.isExists()) {
        child.measureTree();
      }
    }
    children.end();
    onMeasure();
  }

  @Override
  void layoutTree() {
    layoutChildren();
    onLayout();
    float originX = screenX + padLeft;
    float originY = screenY + padTop;
    FlixelUiWidget[] items = children.begin();
    for (int i = 0, n = children.getSize(); i < n; i++) {
      FlixelUiWidget child = items[i];
      if (child.isExists()) {
        child.screenX = originX + child.x;
        child.screenY = originY + child.y;
        child.layoutTree();
      }
    }
    children.end();
  }

  /**
   * Sets each child's local position and size inside the content area: percent sizes first, then
   * anchors. Unanchored children keep their own position.
   */
  void layoutChildren() {
    float cw = getContentWidth();
    float ch = getContentHeight();
    FlixelUiWidget[] items = children.getItems();
    for (int i = 0, n = children.getSize(); i < n; i++) {
      FlixelUiWidget child = items[i];
      if (!child.isExists()) {
        continue;
      }
      if (!Float.isNaN(child.percentWidth)) {
        child.width = child.percentWidth * cw;
      }
      if (!Float.isNaN(child.percentHeight)) {
        child.height = child.percentHeight * ch;
      }
      int align = child.anchor;
      if (align != 0) {
        child.x = alignX(align, cw, child.width) + child.anchorOffsetX;
        child.y = alignY(align, ch, child.height) + child.anchorOffsetY;
      }
    }
  }

  /**
   * Returns where a box of the given width starts when aligned horizontally inside a space.
   *
   * @param align The {@link FlixelAlign} flags.
   * @param space The width of the space.
   * @param size The width of the box.
   * @return The box's left edge, relative to the space's left edge.
   */
  static float alignX(int align, float space, float size) {
    if (FlixelAlign.isLeft(align)) {
      return 0f;
    }
    if (FlixelAlign.isRight(align)) {
      return space - size;
    }
    return (space - size) * 0.5f;
  }

  /**
   * Returns where a box of the given height starts when aligned vertically inside a space.
   *
   * @param align The {@link FlixelAlign} flags.
   * @param space The height of the space.
   * @param size The height of the box.
   * @return The box's top edge, relative to the space's top edge.
   */
  static float alignY(int align, float space, float size) {
    if (FlixelAlign.isTop(align)) {
      return 0f;
    }
    if (FlixelAlign.isBottom(align)) {
      return space - size;
    }
    return (space - size) * 0.5f;
  }

  /**
   * Checks whether a point in view space lies inside the content area, using the same half-open
   * edges as {@link #containsPoint(float, float)}.
   *
   * @param px The X to test, in camera view space.
   * @param py The Y to test, in camera view space.
   * @return {@code true} if the point is inside the content area.
   */
  boolean contentContains(float px, float py) {
    float left = screenX + padLeft;
    float top = screenY + padTop;
    return px >= left && px < left + getContentWidth() && py >= top && py < top + getContentHeight();
  }

  /**
   * Draws every child in draw order, without touching the clip.
   *
   * @param batch The batch to draw into.
   * @param alpha This container's combined alpha, passed down to the children.
   */
  private void drawEachChild(@NotNull FlixelBatch batch, float alpha) {
    FlixelUiWidget[] items = children.begin();
    try {
      for (int i = 0, n = children.getSize(); i < n; i++) {
        items[i].drawTree(batch, alpha);
      }
    } finally {
      children.end();
    }
  }

  /**
   * Removes a child from the list without detaching it.
   *
   * @param widget The child to unlink.
   */
  private void unlink(@NotNull FlixelUiWidget widget) {
    children.removeValue(widget, true);
    widget.parent = null;
  }

  /**
   * Returns the width of the content area: the width minus the left and right padding, never
   * below zero.
   *
   * @return The content width in pixels.
   */
  public float getContentWidth() {
    return Math.max(0f, width - padLeft - padRight);
  }

  /**
   * Returns the height of the content area: the height minus the top and bottom padding, never
   * below zero.
   *
   * @return The content height in pixels.
   */
  public float getContentHeight() {
    return Math.max(0f, height - padTop - padBottom);
  }

  public int getChildCount() {
    return children.getSize();
  }

  public float getPaddingLeft() {
    return padLeft;
  }

  public float getPaddingTop() {
    return padTop;
  }

  public float getPaddingRight() {
    return padRight;
  }

  public float getPaddingBottom() {
    return padBottom;
  }

  /**
   * Returns whether children are clipped to the content area.
   *
   * @return {@code true} when children are clipped, {@code false} (the default) otherwise.
   * @see #setClipChildren(boolean)
   */
  public boolean isClipChildren() {
    return clipChildren;
  }

  /**
   * Chooses whether children are cut off at the edges of the content area, both when drawing and
   * when hit-testing.
   *
   * <p>The content area is the container's rectangle minus its padding, in the camera's view
   * space. The clip is axis-aligned and ignores {@link #getAngle()}.
   *
   * @param clipChildren {@code true} to clip children, {@code false} to let them overflow.
   */
  public void setClipChildren(boolean clipChildren) {
    this.clipChildren = clipChildren;
  }
}
