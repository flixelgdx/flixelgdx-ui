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
import org.flixelgdx.ui.skin.FlixelUiDropdownStyle;
import org.flixelgdx.util.FlixelColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The floating list panel shown when a {@link FlixelUiDropdown} is open.
 *
 * <p>This widget lives on the popup layer of the owning display so it appears above every root-layer
 * widget and participates in hit-testing. It is added to the popup layer when the dropdown opens and
 * removed when it closes, so the pool of text parts is created in {@link #onAttached} and destroyed
 * in {@link #onDetached}.
 */
final class FlixelUiDropdownList extends FlixelUiWidget {

  /** The dropdown this list belongs to. */
  final FlixelUiDropdown dropdown;

  /**
   * The line height measured from the pool part after format is applied, in pixels.
   *
   * <p>Cached when the style changes via {@link #applyFormat()} and used by
   * {@link #getRowHeight(FlixelUiDropdownStyle)} so the row is tall enough to show a full line of
   * text. Zero until a pool part has a font applied and its layout has been computed.
   */
  private float cachedLineHeight;

  /** How many text parts are live in {@link #pool}. */
  private int poolSize;

  /**
   * Text parts, one per potentially visible row (size = {@code maxVisibleItems + 1}).
   *
   * <p>Allocated in {@link #onAttached} and released in {@link #onDetached}/{@link #destroy}.
   */
  @Nullable
  private FlixelUiTextPart[] pool;

  FlixelUiDropdownList(@NotNull FlixelUiDropdown dropdown) {
    this.dropdown = dropdown;
    interactive = true;
  }

  @Override
  protected void onAttached(@NotNull FlixelUiDisplay display) {
    rebuildPool(display);
  }

  @Override
  protected void onDetached() {
    detachPool();
  }

  /** Selects the row under the pointer, which also closes the list. */
  @Override
  protected void onActivate(float x, float y) {
    int index = dropdown.getItemAt(x, y);
    if (index >= 0) {
      dropdown.select(index);
    }
  }

  /** Scrolls the list by {@code amount} rows. */
  @Override
  protected boolean onScroll(float amount) {
    dropdown.scroll(amount);
    return true;
  }

  /**
   * Positions and sizes the list relative to the dropdown field now that the root layer has been
   * fully laid out and {@link FlixelUiDropdown#getScreenX()} is up to date.
   *
   * <p>The list is placed below the field by default. When there is not enough room below, it is
   * flipped above. The result is clamped so it stays inside the display's visible area.
   */
  @Override
  protected void onMeasure() {
    FlixelUiDropdownStyle s = dropdown.style;
    FlixelUiDisplay d = display;
    if (d == null) {
      return;
    }

    float fieldX = dropdown.screenX;
    float fieldY = dropdown.screenY;
    float fieldH = dropdown.height;
    float gap = s != null ? s.listGap : 0f;
    float rowH = getRowHeight(s);

    int itemCount = dropdown.items.getSize();
    int visRows = Math.min(itemCount, dropdown.maxVisibleItems);
    float listH = visRows * rowH;

    float dispH = d.getVisibleHeight();

    float belowY = fieldY + fieldH + gap;
    float listY;
    if (belowY + listH <= dispH) {
      listY = belowY;
    } else {
      listY = fieldY - gap - listH;
    }

    if (listY < 0f) {
      listY = 0f;
    }
    if (listY + listH > dispH) {
      listY = Math.max(0f, dispH - listH);
    }

    this.x = fieldX;
    this.y = listY;
    this.width = dropdown.width;
    this.height = listH;
  }

  @Override
  protected void drawSelf(@NotNull FlixelBatch batch, float drawX, float drawY, float alpha) {
    FlixelUiDropdownStyle s = dropdown.style;
    FlixelColor tint = getColor();

    FlixelUiBackground listBg = s != null ? s.listBackground : null;
    if (listBg != null) {
      batch.setColor(tint.r, tint.g, tint.b, alpha);
      listBg.draw(batch, drawX, drawY, width, height);
      batch.setColor(FlixelColor.WHITE);
    }

    int itemCount = dropdown.items.getSize();
    if (itemCount == 0 || pool == null) {
      return;
    }

    float rowH = getRowHeight(s);
    if (rowH <= 0f) {
      return;
    }

    int selectedIdx = dropdown.selectedIndex;
    int highlightIdx = dropdown.highlightedIndex;
    int firstRow = (int) dropdown.scrollOffset;
    int lastRow = Math.min(firstRow + dropdown.maxVisibleItems - 1, itemCount - 1);

    boolean clipped = pushClip(batch, screenX, screenY, width, height);
    if (clipped) {
      for (int i = firstRow; i <= lastRow; i++) {
        int slot = i - firstRow;
        float rowDrawY = drawY + slot * rowH;
        float rowScreenY = screenY + slot * rowH;

        // Row background: selected takes priority over highlighted.
        FlixelUiBackground rowBg = null;
        if (s != null && s.itemSelected != null && i == selectedIdx) {
          rowBg = s.itemSelected;
        } else if (s != null && i == highlightIdx) {
          rowBg = s.itemHighlight;
        }
        if (rowBg != null) {
          batch.setColor(tint.r, tint.g, tint.b, alpha);
          rowBg.draw(batch, drawX, rowDrawY, width, rowH);
          batch.setColor(FlixelColor.WHITE);
        }

        // Row text.
        if (slot < poolSize && pool[slot] != null) {
          FlixelUiTextPart part = pool[slot];
          FlixelColor textColor = resolveRowTextColor(s, i, highlightIdx);
          part.setColor(textColor);
          part.setText(dropdown.items.get(i));
          float padL = s != null ? s.itemPadLeft : 0f;
          float padT = s != null ? s.itemPadTop : 0f;
          part.draw(batch, screenX + padL, rowScreenY + padT, tint, alpha);
        }
      }
    }
    popClip(batch);
  }

  @Override
  public void destroy() {
    super.destroy();
    destroyPool();
  }

  /**
   * Destroys and rebuilds the text-part pool for the current {@code maxVisibleItems} value.
   *
   * <p>Call this when the display is attached or when {@link FlixelUiDropdown#setMaxVisibleItems}
   * changes while the list is live.
   *
   * @param display The display to attach the new parts to.
   */
  void rebuildPool(@NotNull FlixelUiDisplay display) {
    destroyPool();
    int size = dropdown.maxVisibleItems + 1;
    pool = new FlixelUiTextPart[size];
    poolSize = size;
    FlixelUiDropdownStyle s = dropdown.style;
    for (int i = 0; i < size; i++) {
      FlixelUiTextPart part = new FlixelUiTextPart();
      part.attach(display);
      if (s != null) {
        part.setFormat(s.font, s.fontSize, s.fontColor);
      }
      pool[i] = part;
    }
    // Cache the line height now that pool parts have their format applied and the font is
    // available (rebuildPool is called at game runtime when open() fires, so fonts are loaded).
    refreshLineHeight();
  }

  /** Applies the current style's font settings to every part in the pool. */
  void applyFormat() {
    FlixelUiTextPart[] p = pool;
    if (p == null) {
      return;
    }
    FlixelUiDropdownStyle s = dropdown.style;
    for (int i = 0; i < poolSize; i++) {
      if (p[i] != null && s != null) {
        p[i].setFormat(s.font, s.fontSize, s.fontColor);
      }
    }
    refreshLineHeight();
  }

  /**
   * Re-reads the line height from the first pool part and caches it for use by
   * {@link #getRowHeight(FlixelUiDropdownStyle)}.
   *
   * <p>Call this after setting a font directly on a pool part in tests, to bring the cache in
   * line with the part's actual layout.
   */
  void refreshLineHeight() {
    FlixelUiTextPart[] p = pool;
    if (p == null || poolSize == 0 || p[0] == null) {
      return;
    }
    // Temporarily set a one-line probe string so the font layout is computed even
    // if the part has no assigned text yet. "Ag" exercises both ascender and
    // descender metrics for a representative line-height reading.
    p[0].getText().setText("Ag");
    float lh = p[0].getText().getLineHeight();
    // Restore to empty so the next setText(...) call in drawSelf() detects the change.
    p[0].getText().setText("");
    if (lh > 0f) {
      cachedLineHeight = lh;
    }
  }

  /**
   * Returns the height of one list row in pixels.
   *
   * <p>When {@link FlixelUiDropdownStyle#itemHeight} is positive it is used directly. Otherwise the
   * row height is the actual text line height ({@link #cachedLineHeight}) plus the item top and
   * bottom padding. The cached line height is set by {@link #applyFormat()} and falls back to the
   * nominal font size when no font layout has been computed yet.
   *
   * @param s The dropdown style, or {@code null} for a fallback value.
   * @return The row height.
   */
  float getRowHeight(@Nullable FlixelUiDropdownStyle s) {
    if (s == null) {
      return 20f;
    }
    if (s.itemHeight > 0f) {
      return s.itemHeight;
    }
    float lh = cachedLineHeight > 0f ? cachedLineHeight : Math.max(1, s.fontSize);
    return lh + s.itemPadTop + s.itemPadBottom;
  }

  /**
   * Returns the pool text part at slot {@code i}, or {@code null} when the pool is not built or
   * the index is out of range; for tests.
   *
   * @param i The zero-based slot index.
   * @return The text part, or {@code null}.
   */
  @Nullable
  FlixelUiTextPart getPoolPart(int i) {
    FlixelUiTextPart[] p = pool;
    return (p != null && i >= 0 && i < poolSize) ? p[i] : null;
  }

  /**
   * Returns the number of live pool parts; for tests.
   *
   * @return The pool size.
   */
  int getPoolSize() {
    return poolSize;
  }

  /**
   * Returns the cached line height, for tests.
   *
   * @return The cached line height in pixels.
   */
  float getCachedLineHeight() {
    return cachedLineHeight;
  }

  private void detachPool() {
    FlixelUiTextPart[] p = pool;
    if (p == null) {
      return;
    }
    for (int i = 0; i < poolSize; i++) {
      if (p[i] != null) {
        p[i].detach();
      }
    }
  }

  private void destroyPool() {
    FlixelUiTextPart[] p = pool;
    if (p == null) {
      return;
    }
    for (int i = 0; i < poolSize; i++) {
      if (p[i] != null) {
        p[i].destroy();
        p[i] = null;
      }
    }
    pool = null;
    poolSize = 0;
  }

  @NotNull
  private static FlixelColor resolveRowTextColor(
      @Nullable FlixelUiDropdownStyle s, int rowIndex, int highlightIdx) {
    if (s == null) {
      return FlixelColor.WHITE;
    }
    if (rowIndex == highlightIdx && s.highlightFontColor != null) {
      return s.highlightFontColor;
    }
    return s.fontColor != null ? s.fontColor : FlixelColor.WHITE;
  }
}
