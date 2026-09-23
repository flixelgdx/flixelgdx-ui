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

import org.flixelgdx.collections.FlixelList;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelDisplayMode;
import org.flixelgdx.graphics.FlixelGraphicsManager;
import org.flixelgdx.graphics.FlixelNoopGraphicsManager;
import org.flixelgdx.ui.graphics.RecordingBatch;
import org.jetbrains.annotations.NotNull;

/**
 * A graphics manager test double that records every scissor call, along with how many times the
 * batch had been flushed when the call happened.
 *
 * <p>Every other method keeps the interface's no-op default.
 */
final class RecordingGraphics implements FlixelGraphicsManager {

  static final int SET = 1;
  static final int CLEAR = 2;
  static final int CAPACITY = 64;

  /** The kind of each call, {@link #SET} or {@link #CLEAR}. */
  final int[] kinds = new int[CAPACITY];
  final int[] xs = new int[CAPACITY];
  final int[] ys = new int[CAPACITY];
  final int[] widths = new int[CAPACITY];
  final int[] heights = new int[CAPACITY];

  /** The batch's flush count at the time of each call. */
  final int[] flushes = new int[CAPACITY];

  int count;

  private final RecordingBatch batch;

  RecordingGraphics(@NotNull RecordingBatch batch) {
    this.batch = batch;
  }

  @Override
  public void setScissor(int x, int y, int width, int height) {
    record(SET, x, y, width, height);
  }

  @Override
  public void clearScissor() {
    record(CLEAR, 0, 0, 0, 0);
  }

  @NotNull
  @Override
  public FlixelBatch getBatch() {
    return batch;
  }

  @NotNull
  @Override
  public FlixelList<FlixelDisplayMode> getDisplayModes() {
    return FlixelNoopGraphicsManager.INSTANCE.getDisplayModes();
  }

  /** Returns the kind of the last call, or {@code 0} when nothing was recorded. */
  int lastKind() {
    return count == 0 ? 0 : kinds[count - 1];
  }

  private void record(int kind, int x, int y, int width, int height) {
    kinds[count] = kind;
    xs[count] = x;
    ys[count] = y;
    widths[count] = width;
    heights[count] = height;
    flushes[count] = batch.flushCount;
    count++;
  }
}
