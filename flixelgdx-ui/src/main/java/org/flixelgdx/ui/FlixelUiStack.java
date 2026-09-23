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

import org.flixelgdx.util.FlixelAlign;
import org.jetbrains.annotations.NotNull;

/**
 * A container that places its children one after another, in a column or a row.
 *
 * <p>A vertical stack puts each child below the previous one; a horizontal stack puts each child
 * to the right of the previous one. {@link #setSpacing(float)} adds a gap between neighbors, and
 * {@link #setAlign(int)} lines the children up on the other axis: {@link FlixelAlign#LEFT},
 * {@link FlixelAlign#CENTER}, or {@link FlixelAlign#RIGHT} in a vertical stack, and
 * {@link FlixelAlign#TOP}, {@link FlixelAlign#CENTER}, or {@link FlixelAlign#BOTTOM} in a
 * horizontal one.
 *
 * <p>Each child is given its {@linkplain FlixelUiWidget#getPreferredWidth() preferred size}. A
 * child's percent size still applies on the cross axis (for example
 * {@code setPercentSize(1f, Float.NaN)} stretches a child across a vertical stack), but percent
 * sizes along the stacking direction and anchors are ignored. Children that do not exist
 * ({@linkplain FlixelUiWidget#kill() killed}) take up no space; invisible children keep their
 * space.
 *
 * <h2>Auto-size</h2>
 *
 * <p>A new stack auto-sizes: its size is always exactly its content (children, spacing, and
 * padding), and that is also its preferred size, so stacks nest inside other stacks naturally.
 * Choosing a size yourself turns auto-size off for the axes you chose: {@link #setWidth(float)}
 * for the width, {@link #setHeight(float)} for the height, {@link #setSize(float, float)} for
 * both, and {@link #setPercentSize(float, float)} for each axis that is not {@code NaN}. Call
 * {@link #setAutoSize(boolean)} to turn it back on. Children with a percent size on an auto-sized
 * axis do not count toward the stack's size on that axis, since their size depends on it.
 *
 * <pre>{@code
 * FlixelUiStack column = new FlixelUiStack(FlixelUiStack.Direction.VERTICAL);
 * column.setSpacing(8);
 * column.setPadding(16);
 * column.setAlign(FlixelAlign.CENTER);
 * column.add(title);
 * column.add(playButton);
 * column.add(quitButton);
 * column.anchor(FlixelAlign.CENTER, 0, 0); // The whole column sits in the middle of the screen.
 * ui.add(column);
 * }</pre>
 */
public class FlixelUiStack extends FlixelUiContainer {

  private float spacing;
  private int align;

  @NotNull
  private final Direction direction;

  private boolean autoWidth = true;
  private boolean autoHeight = true;

  /**
   * Creates an empty, auto-sized stack.
   *
   * <p>Children are aligned to the left of a vertical stack and to the top of a horizontal one
   * until {@link #setAlign(int)} is called.
   *
   * @param direction Whether children are placed in a column or a row.
   * @throws IllegalArgumentException If {@code direction} is {@code null}.
   */
  public FlixelUiStack(@NotNull Direction direction) {
    if (direction == null) {
      throw new IllegalArgumentException("Stack direction must not be null.");
    }
    this.direction = direction;
    this.align = direction == Direction.VERTICAL ? FlixelAlign.LEFT : FlixelAlign.TOP;
  }

  /**
   * Turns auto-size on or off for both axes.
   *
   * @param autoSize {@code true} to size the stack to its content on both axes.
   */
  public void setAutoSize(boolean autoSize) {
    autoWidth = autoSize;
    autoHeight = autoSize;
    invalidateLayout();
  }

  /**
   * Sets a fixed width and height, turning auto-size off on both axes.
   *
   * @param width The new width.
   * @param height The new height.
   */
  @Override
  public void setSize(float width, float height) {
    autoWidth = false;
    autoHeight = false;
    super.setSize(width, height);
  }

  /**
   * Sets a fixed width, turning auto-size off for the width.
   *
   * @param width The new width.
   */
  @Override
  public void setWidth(float width) {
    autoWidth = false;
    super.setWidth(width);
  }

  /**
   * Sets a fixed height, turning auto-size off for the height.
   *
   * @param height The new height.
   */
  @Override
  public void setHeight(float height) {
    autoHeight = false;
    super.setHeight(height);
  }

  /**
   * Sizes the stack as a fraction of its parent, turning auto-size off for each axis that is not
   * {@code NaN}.
   *
   * @param width The fraction of the parent's content width, or {@link Float#NaN} to leave the
   *     width alone.
   * @param height The fraction of the parent's content height, or {@link Float#NaN} to leave the
   *     height alone.
   */
  @Override
  public void setPercentSize(float width, float height) {
    if (!Float.isNaN(width)) {
      autoWidth = false;
    }
    if (!Float.isNaN(height)) {
      autoHeight = false;
    }
    super.setPercentSize(width, height);
  }

  /** Resizes an auto-sized stack to fit its already measured children. */
  @Override
  protected void onMeasure() {
    if (!autoWidth && !autoHeight) {
      return;
    }
    boolean vertical = direction == Direction.VERTICAL;
    float main = 0f;
    float cross = 0f;
    int count = 0;
    FlixelUiWidget[] items = children.getItems();
    for (int i = 0, n = children.getSize(); i < n; i++) {
      FlixelUiWidget child = items[i];
      if (!child.isExists()) {
        continue;
      }
      count++;
      if (vertical) {
        main += child.getPreferredHeight();
        if (Float.isNaN(child.percentWidth)) {
          cross = Math.max(cross, child.getPreferredWidth());
        }
      } else {
        main += child.getPreferredWidth();
        if (Float.isNaN(child.percentHeight)) {
          cross = Math.max(cross, child.getPreferredHeight());
        }
      }
    }
    if (count > 1) {
      main += spacing * (count - 1);
    }
    float contentW = vertical ? cross : main;
    float contentH = vertical ? main : cross;
    if (autoWidth) {
      width = contentW + getPaddingLeft() + getPaddingRight();
    }
    if (autoHeight) {
      height = contentH + getPaddingTop() + getPaddingBottom();
    }
  }

  @Override
  void layoutChildren() {
    boolean vertical = direction == Direction.VERTICAL;
    float cw = getContentWidth();
    float ch = getContentHeight();
    float cursor = 0f;
    FlixelUiWidget[] items = children.getItems();
    for (int i = 0, n = children.getSize(); i < n; i++) {
      FlixelUiWidget child = items[i];
      if (!child.isExists()) {
        continue;
      }
      float w = child.getPreferredWidth();
      float h = child.getPreferredHeight();
      if (vertical) {
        if (!Float.isNaN(child.percentWidth)) {
          w = child.percentWidth * cw;
        }
        child.width = w;
        child.height = h;
        child.x = alignX(align, cw, w);
        child.y = cursor;
        cursor += h + spacing;
      } else {
        if (!Float.isNaN(child.percentHeight)) {
          h = child.percentHeight * ch;
        }
        child.width = w;
        child.height = h;
        child.x = cursor;
        child.y = alignY(align, ch, h);
        cursor += w + spacing;
      }
    }
  }

  @NotNull
  public Direction getDirection() {
    return direction;
  }

  public float getSpacing() {
    return spacing;
  }

  /**
   * Sets the gap between neighboring children.
   *
   * @param spacing The gap in pixels.
   */
  public void setSpacing(float spacing) {
    this.spacing = spacing;
    invalidateLayout();
  }

  /**
   * Returns the cross-axis alignment flags.
   *
   * @return The {@link FlixelAlign} flags used to line up children across the stacking direction.
   */
  public int getAlign() {
    return align;
  }

  /**
   * Sets how children line up across the stacking direction.
   *
   * <p>Only the cross axis is read: a vertical stack looks at the horizontal part
   * ({@link FlixelAlign#LEFT}, {@link FlixelAlign#RIGHT}, or neither for centered) and a
   * horizontal stack looks at the vertical part ({@link FlixelAlign#TOP},
   * {@link FlixelAlign#BOTTOM}, or neither for centered), so {@link FlixelAlign#CENTER} centers
   * children in both.
   *
   * @param align The {@link FlixelAlign} flags.
   */
  public void setAlign(int align) {
    this.align = align;
    invalidateLayout();
  }

  public boolean isAutoWidth() {
    return autoWidth;
  }

  public boolean isAutoHeight() {
    return autoHeight;
  }

  /**
   * The direction a {@link FlixelUiStack} places its children in.
   */
  public enum Direction {

    /** Children are placed top to bottom, in a column. */
    VERTICAL,

    /** Children are placed left to right, in a row. */
    HORIZONTAL
  }
}
