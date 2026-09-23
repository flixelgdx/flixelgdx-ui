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
import org.flixelgdx.math.FlixelMatrix;
import org.jetbrains.annotations.NotNull;

/**
 * A minimal concrete widget for tests that records what its last {@link #drawSelf} call received.
 */
class TestWidget extends FlixelUiWidget {

  float lastDrawX = Float.NaN;
  float lastDrawY = Float.NaN;
  float lastAlpha = Float.NaN;
  int drawCount;
  int stateChanges;

  /** A copy of the batch transform in effect during the last draw. */
  final FlixelMatrix drawTransform = new FlixelMatrix();

  TestWidget(float width, float height) {
    super(width, height);
  }

  @Override
  protected void drawSelf(@NotNull FlixelBatch batch, float drawX, float drawY, float alpha) {
    lastDrawX = drawX;
    lastDrawY = drawY;
    lastAlpha = alpha;
    drawCount++;
    drawTransform.set(batch.getTransform());
  }

  @Override
  protected void onStateChanged() {
    stateChanges++;
  }
}
