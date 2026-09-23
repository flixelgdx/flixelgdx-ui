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
package org.flixelgdx.ui.graphics;

import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.graphics.FlixelTexture;
import org.flixelgdx.math.FlixelAffine;
import org.flixelgdx.math.FlixelMatrix;
import org.flixelgdx.util.FlixelBlendMode;
import org.flixelgdx.util.FlixelColor;
import org.flixelgdx.util.FlixelShader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A batch test double that records every {@code draw(frame, x, y, width, height)} call, along with
 * the batch color at the time of the call.
 *
 * <p>Any other draw method fails the test, since backgrounds are only expected to use that one.
 */
public final class RecordingBatch implements FlixelBatch {

  static final int CAPACITY = 32;

  final FlixelFrame[] frames = new FlixelFrame[CAPACITY];
  final float[] xs = new float[CAPACITY];
  final float[] ys = new float[CAPACITY];
  final float[] widths = new float[CAPACITY];
  final float[] heights = new float[CAPACITY];

  /** The batch color at each draw, as {@code r, g, b, a} quadruples. */
  final float[] colors = new float[CAPACITY * 4];

  int count;

  /** How many times {@link #flush()} has been called. */
  public int flushCount;

  private final FlixelColor color = new FlixelColor(FlixelColor.WHITE);
  private final FlixelMatrix projection = new FlixelMatrix();
  private final FlixelMatrix transform = new FlixelMatrix();

  @Override
  public void draw(@NotNull FlixelFrame frame, float x, float y, float width, float height) {
    frames[count] = frame;
    xs[count] = x;
    ys[count] = y;
    widths[count] = width;
    heights[count] = height;
    colors[count * 4] = color.r;
    colors[count * 4 + 1] = color.g;
    colors[count * 4 + 2] = color.b;
    colors[count * 4 + 3] = color.a;
    count++;
  }

  /** Forgets every recorded draw call. */
  public void reset() {
    count = 0;
  }

  /** Returns how many draw calls were recorded, for tests in other packages. */
  public int getDrawCount() {
    return count;
  }

  /** Returns the frame of a recorded draw call. */
  public FlixelFrame getFrame(int index) {
    return frames[index];
  }

  /** Returns the X of a recorded draw call. */
  public float getX(int index) {
    return xs[index];
  }

  /** Returns the Y of a recorded draw call. */
  public float getY(int index) {
    return ys[index];
  }

  /** Returns the width of a recorded draw call. */
  public float getWidth(int index) {
    return widths[index];
  }

  /** Returns the height of a recorded draw call. */
  public float getHeight(int index) {
    return heights[index];
  }

  /** Returns the batch alpha at the time of a recorded draw call. */
  public float getAlpha(int index) {
    return colors[index * 4 + 3];
  }

  /** Returns the batch red component at the time of a recorded draw call. */
  public float getRed(int index) {
    return colors[index * 4];
  }

  @Override
  public void begin() {}

  @Override
  public void end() {}

  @Override
  public void flush() {
    flushCount++;
  }

  @Override
  public void draw(@NotNull FlixelTexture texture, float x, float y, float width, float height) {
    throw new AssertionError("Unexpected draw(texture, ...) call.");
  }

  @Override
  public void draw(@NotNull FlixelTexture texture, float x, float y, float width, float height,
      float u, float v, float u2, float v2) {
    throw new AssertionError("Unexpected draw(texture, ..., uvs) call.");
  }

  @Override
  public void draw(@NotNull FlixelFrame frame, float x, float y, float originX, float originY,
      float width, float height, float scaleX, float scaleY, float rotation,
      boolean flipX, boolean flipY) {
    throw new AssertionError("Unexpected transformed draw call.");
  }

  @Override
  public void draw(@NotNull FlixelFrame frame, float width, float height, @NotNull FlixelAffine transform) {
    throw new AssertionError("Unexpected affine draw call.");
  }

  @Override
  public void draw(@NotNull FlixelTexture texture, float @NotNull [] vertices, int offset, int count) {
    throw new AssertionError("Unexpected vertex draw call.");
  }

  @Override
  public int getRenderCalls() {
    return count;
  }

  @Override
  public int getTotalRenderCalls() {
    return count;
  }

  @NotNull
  @Override
  public FlixelColor getColor() {
    return color;
  }

  @Override
  public void setColor(@NotNull FlixelColor color) {
    this.color.set(color);
  }

  @Override
  public void setColor(float r, float g, float b, float a) {
    color.set(r, g, b, a);
  }

  @NotNull
  @Override
  public FlixelBlendMode getBlendMode() {
    return FlixelBlendMode.NORMAL;
  }

  @Override
  public void setBlendMode(@Nullable FlixelBlendMode mode) {}

  @Nullable
  @Override
  public FlixelShader getShader() {
    return null;
  }

  @Override
  public void setShader(@Nullable FlixelShader shader) {}

  @NotNull
  @Override
  public FlixelMatrix getProjection() {
    return projection;
  }

  @Override
  public void setProjection(@NotNull FlixelMatrix projection) {
    this.projection.set(projection);
  }

  @NotNull
  @Override
  public FlixelMatrix getTransform() {
    return transform;
  }

  @Override
  public void setTransform(@NotNull FlixelMatrix transform) {
    this.transform.set(transform);
  }

  @Override
  public void destroy() {}
}
