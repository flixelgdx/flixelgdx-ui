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
import org.flixelgdx.ui.graphics.FlixelUiBackground;
import org.flixelgdx.ui.skin.FlixelUiDropdownStyle;
import org.flixelgdx.util.FlixelColor;
import org.flixelgdx.util.FlixelString;
import org.flixelgdx.util.signal.FlixelSignal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A widget that shows the selected item in a fixed-width field and, when opened, displays a
 * floating list from which the game can select a different item.
 *
 * <p>Think of a dropdown like a filing-cabinet drawer: the label on the front shows what is inside,
 * and pulling the drawer open reveals the full list of files. The game is the hand that opens and
 * closes the drawer, highlights a file, and picks it. The widget never reads input itself.
 *
 * <h2>Adding items and selecting one</h2>
 *
 * <pre>{@code
 * FlixelUiDropdown res = new FlixelUiDropdown(200);
 * res.addItem("1280 x 720");
 * res.addItem("1920 x 1080");
 * res.addItem("2560 x 1440");
 * res.setSelectedIndex(0);
 * res.anchor(FlixelAlign.CENTER, 0, 0);
 * ui.add(res);
 * }</pre>
 *
 * <h2>Wiring input</h2>
 *
 * <p>The game reads the cursor with its own input code, finds which widget is under the cursor via
 * {@link FlixelUiDisplay#getWidgetAt(float, float)}, and calls the matching method on the dropdown:
 *
 * <pre>{@code
 * // In the game's update(), after a click event:
 * float mx = Flixel.mouse.getWorldX(ui.getCamera());
 * float my = Flixel.mouse.getWorldY(ui.getCamera());
 * FlixelUiWidget hit = ui.getWidgetAt(mx, my);
 * if (Flixel.mouse.justPressed(FlixelMouseButton.LEFT)) {
 *   if (hit == res) {
 *     res.toggle();            // Click the field: open or close.
 *   } else if (res.isOpen()) {
 *     int row = res.getItemAt(mx, my);
 *     if (row >= 0) {
 *       res.select(row);       // Click a row: select and close.
 *     } else {
 *       res.close();           // Click outside: close.
 *     }
 *   }
 * }
 * if (res.isOpen() && hit == res) {
 *   int row = res.getItemAt(mx, my);
 *   if (row >= 0) {
 *     res.highlight(row);      // Hover over a row: highlight it.
 *   }
 * }
 * }</pre>
 *
 * <h2>Listening for a selection</h2>
 *
 * <pre>{@code
 * res.onSelect.add(d -> {
 *   int idx = d.getSelectedIndex();
 *   applyResolution(idx);
 * });
 * }</pre>
 *
 * @see FlixelUiDropdownStyle
 * @see FlixelUiDisplay#getWidgetAt(float, float)
 */
public class FlixelUiDropdown extends FlixelUiWidget {

  /**
   * Dispatched by {@link #select(int)} when the selection changes.
   *
   * <p>Not dispatched by {@link #setSelectedIndex(int)}. The payload is this dropdown so one
   * listener can serve several dropdowns.
   */
  public final FlixelSignal<FlixelUiDropdown> onSelect = new FlixelSignal<>();

  /**
   * Dispatched by {@link #open()} after the list is added to the popup layer.
   */
  public final FlixelSignal<FlixelUiDropdown> onOpen = new FlixelSignal<>();

  /**
   * Dispatched when the list closes, whether by {@link #close()}, {@link #select(int)},
   * {@link #clearItems()}, or because the dropdown was removed from its display.
   */
  public final FlixelSignal<FlixelUiDropdown> onClose = new FlixelSignal<>();

  /** Owned list of item strings. Each entry is a copy of the text passed by the caller. */
  @NotNull
  final FlixelArray<FlixelString> items = new FlixelArray<>(FlixelString[]::new);

  /** The text part that draws the selected item (or placeholder) inside the field. */
  @NotNull
  private final FlixelUiTextPart fieldPart = new FlixelUiTextPart();

  /** The floating list widget placed on the popup layer while the dropdown is open. */
  @NotNull
  private final FlixelUiDropdownList list = new FlixelUiDropdownList(this);

  /** Index of the selected item, or {@code -1} when nothing is selected. */
  int selectedIndex = -1;

  /** Index of the highlighted row in the list, or {@code -1} for no highlight. */
  int highlightedIndex = -1;

  /** Maximum number of rows the list shows before scrolling is needed. */
  int maxVisibleItems = 6;

  /** How many rows have scrolled past the top of the visible list. */
  float scrollOffset;

  /** A copy of the placeholder text, or {@code null} when no placeholder was set. */
  @Nullable
  private FlixelString placeholder;

  /** The style in use, or {@code null} before one is resolved. */
  @Nullable
  FlixelUiDropdownStyle style;

  /** Whether {@link #style} was set with {@link #setStyle(FlixelUiDropdownStyle)} directly. */
  private boolean customStyle;

  /** Whether the floating list is currently on the popup layer. */
  private boolean listOpen;

  /**
   * Creates a dropdown with a fixed width and no items yet.
   *
   * @param width The field width in pixels.
   */
  public FlixelUiDropdown(float width) {
    super(width, 0f);
  }

  @Override
  protected void onAttached(@NotNull FlixelUiDisplay display) {
    fieldPart.attach(display);
  }

  @Override
  protected void onDetached() {
    fieldPart.detach();
    // Close without dispatching close from here -- closeIfOpen checks listOpen and dispatches.
    closeIfOpen();
  }

  @Override
  protected void onStyleChanged() {
    if (!customStyle) {
      style = resolveStyle(FlixelUiDropdownStyle.class);
    }
    applyStyle();
  }

  @Override
  protected void onStateChanged() {
    if (!isEnabled()) {
      closeIfOpen();
    }
  }

  @Override
  protected void onMeasure() {
    FlixelUiDropdownStyle s = style;
    if (s == null) {
      return;
    }
    FlixelUiBackground bg = getBackground();
    float minH = bg != null ? bg.getMinHeight() : 0f;
    float textH = Math.max(1, s.fontSize);
    height = Math.max(minH, s.padTop + textH + s.padBottom);

    // Limit the field text width so it cannot overlap the arrow icon.
    float aw = FlixelUiBgSize.width(s.arrow);
    float fieldW = width - s.padLeft - s.padRight - aw;
    fieldPart.getText().setFieldWidth(Math.max(0f, fieldW));
  }

  @Override
  protected void drawSelf(@NotNull FlixelBatch batch, float drawX, float drawY, float alpha) {
    FlixelUiDropdownStyle s = style;
    FlixelColor tint = getColor();

    FlixelUiBackground bg = getBackground();
    if (bg != null) {
      batch.setColor(tint.r, tint.g, tint.b, alpha);
      bg.draw(batch, drawX, drawY, width, height);
      batch.setColor(FlixelColor.WHITE);
    }

    if (s == null) {
      return;
    }

    // Arrow icon at the right edge, vertically centered.
    FlixelUiBackground arrow = s.arrow;
    if (arrow != null) {
      float aw = FlixelUiBgSize.width(arrow);
      float ah = FlixelUiBgSize.height(arrow);
      float ax = drawX + width - s.padRight - aw;
      float ay = drawY + (height - ah) * 0.5f;
      batch.setColor(tint.r, tint.g, tint.b, alpha);
      arrow.draw(batch, ax, ay, aw, ah);
      batch.setColor(FlixelColor.WHITE);
    }

    // Field text: selected item or placeholder.
    CharSequence fieldText = getFieldText();
    if (fieldText != null) {
      fieldPart.setText(fieldText);
      fieldPart.setColor(getFieldTextColor(s));
      float partH = fieldPart.getHeight();
      float textY = partH > 0f
          ? screenY + s.padTop + (height - s.padTop - s.padBottom - partH) * 0.5f
          : screenY + s.padTop;
      fieldPart.draw(batch, screenX + s.padLeft, textY, tint, alpha);
    }
  }

  @Override
  public void kill() {
    super.kill();
    closeIfOpen();
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if (!visible) {
      closeIfOpen();
    }
  }

  /**
   * Destroys this dropdown, the floating list, and the field text part.
   *
   * <p>The list is closed first (firing {@link #onClose} if open), then all signal listeners are
   * removed. After this call the dropdown must not be used again.
   */
  @Override
  public void destroy() {
    // super.destroy() calls onDetached() -> closeIfOpen() -> onClose.dispatch(this) if open,
    // and the dropdown-specific signals are still alive at that point.
    super.destroy();
    list.destroy();
    fieldPart.destroy();
    items.clear();
    onSelect.clear();
    onOpen.clear();
    onClose.clear();
    style = null;
    placeholder = null;
  }

  /**
   * Adds an item at the end of the list.
   *
   * <p>The text is copied; the original buffer can be reused. If the list is open when an item is
   * added, it reflows to include the new entry.
   *
   * @param text The item text. {@code null} is stored as an empty string.
   */
  public void addItem(@Nullable CharSequence text) {
    FlixelString s = new FlixelString();
    s.set(text != null ? text : "");
    items.add(s);
    invalidateLayout();
  }

  /**
   * Inserts an item at the given position, shifting all items at or after that index down by one.
   *
   * <p>Indices for {@link #getSelectedIndex()} and {@link #getHighlightedIndex()} are adjusted so
   * they continue to point at the same items.
   *
   * @param index The zero-based position to insert at.
   * @param text The item text. {@code null} is stored as an empty string.
   * @throws IndexOutOfBoundsException If {@code index} is outside {@code [0, getItemCount()]}.
   */
  public void insertItem(int index, @Nullable CharSequence text) {
    FlixelString s = new FlixelString();
    s.set(text != null ? text : "");
    items.insert(index, s);
    if (selectedIndex >= index) {
      selectedIndex++;
    }
    if (highlightedIndex >= index) {
      highlightedIndex++;
    }
    invalidateLayout();
  }

  /**
   * Removes the item at the given index.
   *
   * <p>If the removed item was selected, the selection is cleared ({@link #getSelectedIndex()}
   * returns {@code -1}). If a later item was selected, its index is decremented by one. The same
   * adjustments apply to the highlighted index. The scroll offset is clamped so the list does not
   * scroll past the last item.
   *
   * @param index The zero-based index of the item to remove.
   * @throws IndexOutOfBoundsException If {@code index} is outside {@code [0, getItemCount() - 1]}.
   */
  public void removeItem(int index) {
    items.removeIndex(index);
    if (selectedIndex == index) {
      selectedIndex = -1;
    } else if (selectedIndex > index) {
      selectedIndex--;
    }
    if (highlightedIndex == index) {
      highlightedIndex = -1;
    } else if (highlightedIndex > index) {
      highlightedIndex--;
    }
    float maxScroll = Math.max(0f, items.getSize() - maxVisibleItems);
    if (scrollOffset > maxScroll) {
      scrollOffset = maxScroll;
    }
    invalidateLayout();
  }

  /**
   * Removes all items, clears the selection, and closes the list if it is open.
   */
  public void clearItems() {
    closeIfOpen();
    items.clear();
    selectedIndex = -1;
    highlightedIndex = -1;
    scrollOffset = 0f;
    invalidateLayout();
  }

  /**
   * Returns the number of items.
   *
   * @return The item count.
   */
  public int getItemCount() {
    return items.getSize();
  }

  /**
   * Returns the text of the item at the given index.
   *
   * @param index The zero-based item index.
   * @return The item text; never {@code null}.
   * @throws IndexOutOfBoundsException If {@code index} is outside {@code [0, getItemCount() - 1]}.
   */
  @NotNull
  public CharSequence getItem(int index) {
    return items.get(index);
  }

  /**
   * Sets the placeholder text shown when no item is selected.
   *
   * <p>The text is copied into an owned {@link FlixelString}. Passing {@code null} clears the
   * placeholder.
   *
   * @param text The placeholder text, or {@code null} to clear it.
   */
  public void setPlaceholder(@Nullable CharSequence text) {
    if (text == null || text.length() == 0) {
      if (placeholder != null) {
        placeholder.clear();
      }
      return;
    }
    if (placeholder == null) {
      placeholder = new FlixelString();
    }
    placeholder.set(text);
    invalidateLayout();
  }

  /**
   * Returns the index of the selected item, or {@code -1} when nothing is selected.
   *
   * @return The selected index.
   */
  public int getSelectedIndex() {
    return selectedIndex;
  }

  /**
   * Returns the text of the selected item, or {@code null} when nothing is selected.
   *
   * @return The selected item text, or {@code null}.
   */
  @Nullable
  public CharSequence getSelectedItem() {
    return selectedIndex >= 0 && selectedIndex < items.getSize() ? items.get(selectedIndex) : null;
  }

  /**
   * Sets the selected item index without dispatching {@link #onSelect} or closing the list.
   *
   * <p>Use this for a silent initial selection. Use {@link #select(int)} to respond to player
   * input.
   *
   * @param index The zero-based index, or {@code -1} to clear the selection.
   */
  public void setSelectedIndex(int index) {
    selectedIndex = index;
    if (index >= 0) {
      highlightedIndex = index;
    }
    invalidateLayout();
  }

  /**
   * Selects an item in response to player input: changes the selection, closes the list, and
   * dispatches {@link #onSelect} only when the selection actually changes.
   *
   * <p>Does nothing when this dropdown is disabled.
   *
   * @param index The zero-based index to select.
   */
  public void select(int index) {
    if (!isEnabled()) {
      return;
    }
    boolean changed = index != selectedIndex;
    selectedIndex = index;
    highlightedIndex = index;
    closeIfOpen();
    if (changed) {
      onSelect.dispatch(this);
    }
  }

  /**
   * Opens the floating list and dispatches {@link #onOpen}.
   *
   * <p>Does nothing when already open, when disabled, when there are no items, or when this
   * dropdown is not on a display.
   */
  public void open() {
    if (listOpen || !isEnabled() || items.getSize() == 0 || display == null) {
      return;
    }
    listOpen = true;
    display.getPopupLayer().add(list);
    invalidateLayout();
    onOpen.dispatch(this);
  }

  /**
   * Closes the floating list and dispatches {@link #onClose}.
   *
   * <p>Does nothing when the list is not open.
   */
  public void close() {
    closeIfOpen();
  }

  /**
   * Toggles the list: opens it when closed, closes it when open.
   *
   * <p>Opening is still subject to the same guards as {@link #open()}.
   */
  public void toggle() {
    if (listOpen) {
      closeIfOpen();
    } else {
      open();
    }
  }

  /**
   * Returns whether the floating list is currently open.
   *
   * @return {@code true} while the list is on the popup layer.
   */
  public boolean isOpen() {
    return listOpen;
  }

  /**
   * Highlights the item at the given index, scrolling the list so it is visible.
   *
   * <p>The index is clamped to {@code [0, getItemCount() - 1]}. Does nothing when there are no
   * items.
   *
   * @param index The zero-based row to highlight.
   */
  public void highlight(int index) {
    int count = items.getSize();
    if (count == 0) {
      highlightedIndex = -1;
      return;
    }
    if (index < 0) {
      index = 0;
    }
    if (index >= count) {
      index = count - 1;
    }
    highlightedIndex = index;
    scrollIntoView(index);
    invalidateLayout();
  }

  /**
   * Moves the highlight one row down, clamping at the last item.
   *
   * <p>When nothing is highlighted, the first item is highlighted. When the last item is already
   * highlighted, nothing changes.
   */
  public void highlightNext() {
    int count = items.getSize();
    if (count == 0) {
      return;
    }
    int next = highlightedIndex < 0 ? 0 : Math.min(highlightedIndex + 1, count - 1);
    highlight(next);
  }

  /**
   * Moves the highlight one row up, clamping at the first item.
   *
   * <p>When nothing is highlighted, the first item is highlighted. When the first item is already
   * highlighted, nothing changes.
   */
  public void highlightPrevious() {
    if (items.getSize() == 0) {
      return;
    }
    int prev = highlightedIndex <= 0 ? 0 : highlightedIndex - 1;
    highlight(prev);
  }

  /**
   * Returns the index of the currently highlighted row, or {@code -1} when nothing is highlighted.
   *
   * @return The highlighted index.
   */
  public int getHighlightedIndex() {
    return highlightedIndex;
  }

  /**
   * Selects the highlighted item as if the game had called {@link #select(int)} with that index.
   *
   * <p>Does nothing when nothing is highlighted.
   */
  public void selectHighlighted() {
    if (highlightedIndex >= 0) {
      select(highlightedIndex);
    }
  }

  /**
   * Scrolls the list by the given number of rows, clamping to the valid range.
   *
   * <p>Positive values scroll toward the end of the list; negative values scroll toward the start.
   *
   * @param rows The number of rows to scroll.
   */
  public void scroll(float rows) {
    float max = Math.max(0f, items.getSize() - maxVisibleItems);
    scrollOffset = Math.max(0f, Math.min(scrollOffset + rows, max));
    invalidateLayout();
  }

  /**
   * Returns the current scroll offset in rows.
   *
   * @return How many rows have scrolled past the top of the visible list.
   */
  public float getScrollOffset() {
    return scrollOffset;
  }

  /**
   * Sets the maximum number of rows the list shows before the game must scroll.
   *
   * <p>The default is {@code 6}. When the list is open and the new value differs, the text-part
   * pool is rebuilt immediately.
   *
   * @param max The maximum number of visible rows; clamped to at least {@code 1}.
   */
  public void setMaxVisibleItems(int max) {
    if (max < 1) {
      max = 1;
    }
    if (max == maxVisibleItems) {
      return;
    }
    maxVisibleItems = max;
    if (listOpen && list.display != null) {
      list.rebuildPool(list.display);
    }
    invalidateLayout();
  }

  /**
   * Returns the maximum number of rows shown before scrolling is needed.
   *
   * @return The maximum visible item count.
   */
  public int getMaxVisibleItems() {
    return maxVisibleItems;
  }

  /**
   * Returns the zero-based index of the item whose row contains the point, or {@code -1} when the
   * point is outside the list or the list is closed.
   *
   * <p>This is a pure geometric lookup for use in the game's own input handling, for example to
   * map a mouse position to a row. It does not allocate.
   *
   * @param x The X coordinate in the display camera's view space.
   * @param y The Y coordinate in the display camera's view space.
   * @return The item index, or {@code -1}.
   */
  public int getItemAt(float x, float y) {
    if (!listOpen || !list.containsPoint(x, y)) {
      return -1;
    }
    float rowH = list.getRowHeight(style);
    if (rowH <= 0f) {
      return -1;
    }
    float relY = y - list.screenY;
    int row = (int) (relY / rowH + scrollOffset);
    int count = items.getSize();
    return row >= 0 && row < count ? row : -1;
  }

  /**
   * Replaces the style object used by this dropdown.
   *
   * <p>Passing {@code null} reverts to the skin-resolved style. The style is applied immediately
   * when on a display.
   *
   * @param newStyle The style to use, or {@code null} to use the skin again.
   */
  public void setStyle(@Nullable FlixelUiDropdownStyle newStyle) {
    customStyle = newStyle != null;
    style = newStyle;
    applyStyle();
  }

  /**
   * Returns the current style, or {@code null} before one is resolved.
   *
   * @return The active style.
   */
  @Nullable
  public FlixelUiDropdownStyle getDropdownStyle() {
    return style;
  }

  // --- Internal helpers ---

  /** Closes the list and dispatches onClose if it was open. */
  private void closeIfOpen() {
    if (!listOpen) {
      return;
    }
    listOpen = false;
    removeListFromPopup();
    onClose.dispatch(this);
  }

  /** Removes the list from the popup layer without altering {@link #listOpen}. */
  private void removeListFromPopup() {
    FlixelUiContainer parent = list.parent;
    if (parent != null) {
      parent.remove(list);
    }
  }

  /** Applies the current style: sets font format on the field part and list, then invalidates. */
  private void applyStyle() {
    FlixelUiDropdownStyle s = style;
    if (s != null) {
      fieldPart.setFormat(s.font, s.fontSize, s.fontColor);
    }
    list.applyFormat();
    invalidateLayout();
  }

  /**
   * Returns the background for the current interaction state, following the style's fallback chain.
   *
   * @return The background to draw, or {@code null} when there is no style.
   */
  @Nullable
  private FlixelUiBackground getBackground() {
    FlixelUiDropdownStyle s = style;
    if (s == null) {
      return null;
    }
    FlixelUiBackground bg;
    if (!isEnabled()) {
      bg = s.disabled;
    } else if (listOpen) {
      bg = s.open;
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

  /** Returns the text to display in the field, or {@code null} for nothing. */
  @Nullable
  private CharSequence getFieldText() {
    if (selectedIndex >= 0 && selectedIndex < items.getSize()) {
      return items.get(selectedIndex);
    }
    return (placeholder != null && !placeholder.isEmpty()) ? placeholder : null;
  }

  /** Returns the text color for the field content based on the current state. */
  @NotNull
  private FlixelColor getFieldTextColor(@NotNull FlixelUiDropdownStyle s) {
    FlixelColor base = s.fontColor != null ? s.fontColor : FlixelColor.WHITE;
    if (!isEnabled()) {
      return s.disabledFontColor != null ? s.disabledFontColor : base;
    }
    boolean showingPlaceholder = selectedIndex < 0 || selectedIndex >= items.getSize();
    if (showingPlaceholder) {
      return s.placeholderFontColor != null ? s.placeholderFontColor : base;
    }
    return base;
  }

  /**
   * Returns the floating list, for tests.
   *
   * @return The list widget.
   */
  @NotNull
  FlixelUiDropdownList getList() {
    return list;
  }

  /** Adjusts the scroll offset so that {@code idx} is inside the visible window. */
  private void scrollIntoView(int idx) {
    if (idx < (int) scrollOffset) {
      scrollOffset = idx;
    } else if (idx >= (int) scrollOffset + maxVisibleItems) {
      scrollOffset = idx - maxVisibleItems + 1;
    }
  }
}
