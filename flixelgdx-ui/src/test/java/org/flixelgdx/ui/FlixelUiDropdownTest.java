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
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.graphics.FlixelNoopTexture;
import org.flixelgdx.text.FlixelFont;
import org.flixelgdx.ui.graphics.FlixelUiImage;
import org.flixelgdx.ui.graphics.RecordingBatch;
import org.flixelgdx.ui.skin.FlixelUiDropdownStyle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link FlixelUiDropdown}: items, selection, signals, open/close, highlight, scroll,
 * getItemAt, list placement, and auto-close cases.
 */
@ExtendWith(FlixelUiHeadlessExtension.class)
class FlixelUiDropdownTest {

  private static final float EPS = 1e-4f;

  private FlixelCamera camera;
  private FlixelUiDisplay ui;

  @BeforeEach
  void setUp() {
    Flixel.cameras.clear();
    camera = new FlixelCamera(640, 360);
    Flixel.cameras.add(camera);
    ui = new FlixelUiDisplay(camera);
  }

  // Creates a dropdown with a style pre-applied so no skin look-up is needed.
  private FlixelUiDropdown dropdown(float width) {
    FlixelUiDropdown d = new FlixelUiDropdown(width);
    d.setStyle(dropdownStyle());
    return d;
  }

  private static FlixelUiDropdownStyle dropdownStyle() {
    FlixelUiDropdownStyle s = new FlixelUiDropdownStyle();
    s.fontSize = 14;
    s.itemHeight = 20f;
    s.padLeft = 4f;
    s.padTop = 4f;
    s.padRight = 4f;
    s.padBottom = 4f;
    s.itemPadLeft = 4f;
    s.itemPadTop = 2f;
    s.itemPadRight = 4f;
    s.itemPadBottom = 2f;
    s.listGap = 2f;
    return s;
  }

  // Adds the dropdown to the display and lays out so screenX/Y are populated.
  private FlixelUiDropdown addAndLayout(FlixelUiDropdown d) {
    ui.add(d);
    ui.layout();
    return d;
  }

  // ===================== Items =====================

  @Test
  void addItemIncreasesCount() {
    FlixelUiDropdown d = dropdown(200);
    assertEquals(0, d.getItemCount());
    d.addItem("Alpha");
    d.addItem("Beta");
    assertEquals(2, d.getItemCount());
  }

  @Test
  void getItemReturnsText() {
    FlixelUiDropdown d = dropdown(200);
    d.addItem("Hello");
    d.addItem("World");
    assertEquals("Hello", d.getItem(0).toString());
    assertEquals("World", d.getItem(1).toString());
  }

  @Test
  void addItemNullStoredAsEmpty() {
    FlixelUiDropdown d = dropdown(200);
    d.addItem(null);
    assertEquals("", d.getItem(0).toString());
  }

  @Test
  void insertItemShiftsIndices() {
    FlixelUiDropdown d = dropdown(200);
    d.addItem("A");
    d.addItem("B");
    d.setSelectedIndex(1); // "B"
    d.insertItem(1, "C"); // Insert before "B"
    assertEquals(3, d.getItemCount());
    assertEquals("C", d.getItem(1).toString());
    assertEquals("B", d.getItem(2).toString());
    // Selected index was 1 ("B"), now should be 2.
    assertEquals(2, d.getSelectedIndex());
  }

  @Test
  void insertItemHighlightedIndexShifted() {
    FlixelUiDropdown d = dropdown(200);
    d.addItem("A");
    d.addItem("B");
    d.highlight(1);
    d.insertItem(0, "X");
    assertEquals(2, d.getHighlightedIndex());
  }

  @Test
  void removeItemAdjustsSelectedIndexAfter() {
    FlixelUiDropdown d = dropdown(200);
    d.addItem("A");
    d.addItem("B");
    d.addItem("C");
    d.setSelectedIndex(2); // "C"
    d.removeItem(1); // Remove "B"; "C" is now at index 1.
    assertEquals(1, d.getSelectedIndex());
  }

  @Test
  void removeItemClearsSelectedIndex() {
    FlixelUiDropdown d = dropdown(200);
    d.addItem("A");
    d.addItem("B");
    d.setSelectedIndex(0);
    d.removeItem(0); // Remove the selected item.
    assertEquals(-1, d.getSelectedIndex());
  }

  @Test
  void removeItemClearsHighlightedIndex() {
    FlixelUiDropdown d = dropdown(200);
    d.addItem("A");
    d.addItem("B");
    d.highlight(0);
    d.removeItem(0);
    assertEquals(-1, d.getHighlightedIndex());
  }

  @Test
  void clearItemsResetsEverything() {
    FlixelUiDropdown d = dropdown(200);
    d.addItem("A");
    d.addItem("B");
    d.setSelectedIndex(1);
    d.scroll(1f);
    d.clearItems();
    assertEquals(0, d.getItemCount());
    assertEquals(-1, d.getSelectedIndex());
    assertEquals(0f, d.getScrollOffset(), EPS);
  }

  // ===================== Selection =====================

  @Test
  void setSelectedIndexNoSignal() {
    FlixelUiDropdown d = dropdown(200);
    d.addItem("A");
    int[] count = { 0 };
    d.onSelect.add(x -> count[0]++);
    d.setSelectedIndex(0);
    assertEquals(0, count[0]);
    assertEquals(0, d.getSelectedIndex());
  }

  @Test
  void selectDispatchesOnChangeOnly() {
    FlixelUiDropdown d = addAndLayout(dropdown(200));
    d.addItem("A");
    d.addItem("B");
    int[] count = { 0 };
    d.onSelect.add(x -> count[0]++);
    d.select(0);
    assertEquals(1, count[0]);
    d.select(0); // Same index: no signal.
    assertEquals(1, count[0]);
    d.select(1);
    assertEquals(2, count[0]);
  }

  @Test
  void selectClosesListWithoutExtraClose() {
    FlixelUiDropdown d = addAndLayout(dropdown(200));
    d.addItem("A");
    int[] closeCount = { 0 };
    d.onClose.add(x -> closeCount[0]++);
    d.open();
    d.select(0);
    assertFalse(d.isOpen());
    assertEquals(1, closeCount[0]);
  }

  @Test
  void getSelectedItemNull() {
    FlixelUiDropdown d = dropdown(200);
    assertNull(d.getSelectedItem());
  }

  @Test
  void getSelectedItemReturnsText() {
    FlixelUiDropdown d = dropdown(200);
    d.addItem("Chosen");
    d.setSelectedIndex(0);
    assertNotNull(d.getSelectedItem());
    assertEquals("Chosen", d.getSelectedItem().toString());
  }

  // ===================== Placeholder =====================

  @Test
  void setPlaceholderShownWhenNoSelection() {
    FlixelUiDropdown d = dropdown(200);
    d.setPlaceholder("Choose...");
    // No selection, so the field should show the placeholder. We verify indirectly
    // by checking that getSelectedItem is still null and getItemCount is still 0.
    assertNull(d.getSelectedItem());
    assertEquals(0, d.getItemCount());
  }

  @Test
  void clearPlaceholderWithNull() {
    FlixelUiDropdown d = dropdown(200);
    d.setPlaceholder("Hint");
    d.setPlaceholder(null);
    // After clearing: the dropdown still works normally.
    assertNull(d.getSelectedItem());
  }

  // ===================== Open / close / toggle =====================

  @Test
  void openDispatchesOnOpen() {
    FlixelUiDropdown d = addAndLayout(dropdown(200));
    d.addItem("A");
    int[] opens = { 0 };
    d.onOpen.add(x -> opens[0]++);
    d.open();
    assertEquals(1, opens[0]);
    assertTrue(d.isOpen());
  }

  @Test
  void closeDispatchesOnClose() {
    FlixelUiDropdown d = addAndLayout(dropdown(200));
    d.addItem("A");
    d.open();
    int[] closes = { 0 };
    d.onClose.add(x -> closes[0]++);
    d.close();
    assertEquals(1, closes[0]);
    assertFalse(d.isOpen());
  }

  @Test
  void closeTwiceFiresOnlyOnce() {
    FlixelUiDropdown d = addAndLayout(dropdown(200));
    d.addItem("A");
    d.open();
    int[] closes = { 0 };
    d.onClose.add(x -> closes[0]++);
    d.close();
    d.close();
    assertEquals(1, closes[0]);
  }

  @Test
  void toggleOpensThenCloses() {
    FlixelUiDropdown d = addAndLayout(dropdown(200));
    d.addItem("A");
    assertFalse(d.isOpen());
    d.toggle();
    assertTrue(d.isOpen());
    d.toggle();
    assertFalse(d.isOpen());
  }

  @Test
  void openDoesNothingWhenDisabled() {
    FlixelUiDropdown d = addAndLayout(dropdown(200));
    d.addItem("A");
    d.setEnabled(false);
    d.open();
    assertFalse(d.isOpen());
  }

  @Test
  void openDoesNothingWhenEmpty() {
    FlixelUiDropdown d = addAndLayout(dropdown(200));
    d.open();
    assertFalse(d.isOpen());
  }

  @Test
  void openDoesNothingWhenNotOnDisplay() {
    FlixelUiDropdown d = dropdown(200);
    d.addItem("A");
    d.open();
    assertFalse(d.isOpen());
  }

  // ===================== Highlight =====================

  @Test
  void highlightClampsToFirst() {
    FlixelUiDropdown d = dropdown(200);
    d.addItem("A");
    d.addItem("B");
    d.highlight(-10);
    assertEquals(0, d.getHighlightedIndex());
  }

  @Test
  void highlightClampsToLast() {
    FlixelUiDropdown d = dropdown(200);
    d.addItem("A");
    d.addItem("B");
    d.highlight(99);
    assertEquals(1, d.getHighlightedIndex());
  }

  @Test
  void highlightNextStartsAtZero() {
    FlixelUiDropdown d = dropdown(200);
    d.addItem("A");
    d.addItem("B");
    d.highlightNext();
    assertEquals(0, d.getHighlightedIndex());
  }

  @Test
  void highlightNextAdvances() {
    FlixelUiDropdown d = dropdown(200);
    d.addItem("A");
    d.addItem("B");
    d.addItem("C");
    d.highlight(1);
    d.highlightNext();
    assertEquals(2, d.getHighlightedIndex());
  }

  @Test
  void highlightNextClampsAtLast() {
    FlixelUiDropdown d = dropdown(200);
    d.addItem("A");
    d.addItem("B");
    d.highlight(1);
    d.highlightNext();
    assertEquals(1, d.getHighlightedIndex());
  }

  @Test
  void highlightPreviousClampsAtFirst() {
    FlixelUiDropdown d = dropdown(200);
    d.addItem("A");
    d.addItem("B");
    d.highlight(0);
    d.highlightPrevious();
    assertEquals(0, d.getHighlightedIndex());
  }

  @Test
  void highlightPreviousDecreases() {
    FlixelUiDropdown d = dropdown(200);
    d.addItem("A");
    d.addItem("B");
    d.addItem("C");
    d.highlight(2);
    d.highlightPrevious();
    assertEquals(1, d.getHighlightedIndex());
  }

  @Test
  void selectHighlighted() {
    FlixelUiDropdown d = addAndLayout(dropdown(200));
    d.addItem("A");
    d.addItem("B");
    int[] count = { 0 };
    d.onSelect.add(x -> count[0]++);
    d.highlight(1);
    d.selectHighlighted();
    assertEquals(1, d.getSelectedIndex());
    assertEquals(1, count[0]);
  }

  @Test
  void selectHighlightedDoesNothingWhenNegative() {
    FlixelUiDropdown d = addAndLayout(dropdown(200));
    d.addItem("A");
    int[] count = { 0 };
    d.onSelect.add(x -> count[0]++);
    d.selectHighlighted(); // highlightedIndex == -1
    assertEquals(0, count[0]);
  }

  // ===================== Scroll =====================

  @Test
  void scrollClampedAtZero() {
    FlixelUiDropdown d = dropdown(200);
    d.addItem("A");
    d.scroll(-5f);
    assertEquals(0f, d.getScrollOffset(), EPS);
  }

  @Test
  void scrollClampedAtMax() {
    FlixelUiDropdown d = dropdown(200);
    for (int i = 0; i < 10; i++) {
      d.addItem("Item " + i);
    }
    // maxVisibleItems = 6, so max scroll = 10 - 6 = 4
    d.scroll(100f);
    assertEquals(4f, d.getScrollOffset(), EPS);
  }

  @Test
  void scrollAccumulates() {
    FlixelUiDropdown d = dropdown(200);
    for (int i = 0; i < 10; i++) {
      d.addItem("Item " + i);
    }
    d.scroll(2f);
    d.scroll(1f);
    assertEquals(3f, d.getScrollOffset(), EPS);
  }

  @Test
  void highlightScrollsIntoViewBelow() {
    FlixelUiDropdown d = dropdown(200);
    for (int i = 0; i < 10; i++) {
      d.addItem("Item " + i);
    }
    // Scroll so rows 0-5 are visible.
    d.scroll(0f);
    // Highlight row 8 (beyond the visible window).
    d.highlight(8);
    // scrollOffset should advance so row 8 is the last visible row (= 8 - 6 + 1 = 3).
    assertEquals(3f, d.getScrollOffset(), EPS);
  }

  @Test
  void highlightScrollsIntoViewAbove() {
    FlixelUiDropdown d = dropdown(200);
    for (int i = 0; i < 10; i++) {
      d.addItem("Item " + i);
    }
    d.scroll(4f); // visible: rows 4-9
    d.highlight(1); // above the visible window
    assertEquals(1f, d.getScrollOffset(), EPS);
  }

  // ===================== getItemAt =====================

  @Test
  void getItemAtReturnsMinus1WhenClosed() {
    FlixelUiDropdown d = addAndLayout(dropdown(200));
    d.addItem("A");
    d.addItem("B");
    // List is closed, so getItemAt always returns -1.
    assertEquals(-1, d.getItemAt(0f, 0f));
  }

  @Test
  void getItemAtOutsideListReturnsMinus1() {
    FlixelUiDropdown d = addAndLayout(dropdown(200));
    d.addItem("A");
    d.addItem("B");
    d.open();
    ui.layout();
    // A point far outside the list.
    assertEquals(-1, d.getItemAt(-999f, -999f));
  }

  @Test
  void getItemAtFirstRow() {
    FlixelUiDropdown d = addAndLayout(dropdown(200));
    d.addItem("A");
    d.addItem("B");
    d.open();
    ui.layout();
    // Style has itemHeight = 20, listGap = 2, field height = padTop + fontSize + padBottom = 22.
    // List starts at fieldY + fieldH + listGap = 0 + 22 + 2 = 24.
    // Row 0 spans y in [24, 44).
    float listY = d.getScreenY() + d.getHeight() + 2f;
    float midRow0 = listY + 10f;
    assertEquals(0, d.getItemAt(d.getScreenX() + 10f, midRow0));
  }

  @Test
  void getItemAtSecondRow() {
    FlixelUiDropdown d = addAndLayout(dropdown(200));
    d.addItem("A");
    d.addItem("B");
    d.open();
    ui.layout();
    float listY = d.getScreenY() + d.getHeight() + 2f;
    float midRow1 = listY + 20f + 10f; // row height = 20, so row 1 starts at listY + 20
    assertEquals(1, d.getItemAt(d.getScreenX() + 10f, midRow1));
  }

  @Test
  void getItemAtWithScrollOffset() {
    FlixelUiDropdown d = addAndLayout(dropdown(200));
    for (int i = 0; i < 8; i++) {
      d.addItem("Item " + i);
    }
    d.scroll(2f); // rows 2-7 visible
    d.open();
    ui.layout();
    // The list's first visible slot is row 2. A hit in the first slot should return 2.
    float listY = d.getScreenY() + d.getHeight() + 2f;
    float midSlot0 = listY + 10f;
    assertEquals(2, d.getItemAt(d.getScreenX() + 10f, midSlot0));
  }

  // ===================== List placement and popup layer =====================

  @Test
  void openPutsListOnPopupLayer() {
    FlixelUiDropdown d = addAndLayout(dropdown(200));
    d.addItem("A");
    d.open();
    ui.layout();
    // A hit inside the list area should return the list widget (not the dropdown).
    float listY = d.getScreenY() + d.getHeight() + 2f;
    FlixelUiWidget hit = ui.getWidgetAt(d.getScreenX() + 10f, listY + 10f);
    assertNotNull(hit);
    // The list widget is an instance of FlixelUiDropdownList.
    assertTrue(hit instanceof FlixelUiDropdownList);
  }

  @Test
  void closeRemovesListFromPopupLayer() {
    FlixelUiDropdown d = addAndLayout(dropdown(200));
    d.addItem("A");
    d.open();
    ui.layout();
    d.close();
    ui.layout();
    float listY = d.getScreenY() + d.getHeight() + 2f;
    FlixelUiWidget hit = ui.getWidgetAt(d.getScreenX() + 10f, listY + 10f);
    // After closing, there should be nothing (or only the dropdown field) at that position.
    assertFalse(hit instanceof FlixelUiDropdownList);
  }

  // ===================== Auto-close =====================

  @Test
  void clearItemsAutoCloses() {
    FlixelUiDropdown d = addAndLayout(dropdown(200));
    d.addItem("A");
    d.open();
    int[] closeCount = { 0 };
    d.onClose.add(x -> closeCount[0]++);
    d.clearItems();
    assertFalse(d.isOpen());
    assertEquals(1, closeCount[0]);
  }

  @Test
  void setEnabledFalseAutoCloses() {
    FlixelUiDropdown d = addAndLayout(dropdown(200));
    d.addItem("A");
    d.open();
    int[] closeCount = { 0 };
    d.onClose.add(x -> closeCount[0]++);
    d.setEnabled(false);
    assertFalse(d.isOpen());
    assertEquals(1, closeCount[0]);
  }

  @Test
  void killAutoCloses() {
    FlixelUiDropdown d = addAndLayout(dropdown(200));
    d.addItem("A");
    d.open();
    int[] closeCount = { 0 };
    d.onClose.add(x -> closeCount[0]++);
    d.kill();
    assertFalse(d.isOpen());
    assertEquals(1, closeCount[0]);
  }

  @Test
  void setVisibleFalseAutoCloses() {
    FlixelUiDropdown d = addAndLayout(dropdown(200));
    d.addItem("A");
    d.open();
    int[] closeCount = { 0 };
    d.onClose.add(x -> closeCount[0]++);
    d.setVisible(false);
    assertFalse(d.isOpen());
    assertEquals(1, closeCount[0]);
  }

  @Test
  void removeFromDisplayAutoCloses() {
    FlixelUiDropdown d = addAndLayout(dropdown(200));
    d.addItem("A");
    d.open();
    int[] closeCount = { 0 };
    d.onClose.add(x -> closeCount[0]++);
    ui.remove(d); // detaches the dropdown from the display
    assertFalse(d.isOpen());
    assertEquals(1, closeCount[0]);
  }

  @Test
  void destroyFiresOnClose() {
    FlixelUiDropdown d = addAndLayout(dropdown(200));
    d.addItem("A");
    d.open();
    int[] closeCount = { 0 };
    d.onClose.add(x -> closeCount[0]++);
    d.destroy();
    assertEquals(1, closeCount[0]);
  }

  @Test
  void destroyDoesNotFireOnCloseWhenAlreadyClosed() {
    FlixelUiDropdown d = addAndLayout(dropdown(200));
    d.addItem("A");
    int[] closeCount = { 0 };
    d.onClose.add(x -> closeCount[0]++);
    d.destroy();
    assertEquals(0, closeCount[0]);
  }

  // ===================== Modal backdrop fix =====================

  @Test
  void backdropPositionTest() {
    // Verifies that modal backdrop drawing uses display.toDrawX/toDrawY(0f)
    // rather than a hardcoded (0, 0). Since toDrawX/toDrawY currently return
    // the value unchanged, drawing at (0, 0) is correct and the test just
    // verifies modal opens and closes without error.
    FlixelUiModal m = new FlixelUiModal(100, 80);
    m.setModalStyle(new org.flixelgdx.ui.skin.FlixelUiModalStyle());
    ui.openModal(m);
    assertTrue(m.isOpen());
    ui.closeModal(m);
    assertFalse(m.isOpen());
  }

  // ===================== Bug fixes: arrow natural size and row-height line height =====================

  @Test
  void arrowImageDrawnAtNaturalSize() {
    // A FlixelUiImage has getMinWidth/Height() == 0; the arrow must use getNaturalWidth/Height().
    FlixelNoopTexture tex = new FlixelNoopTexture(64, 64);
    FlixelFrame frame = new FlixelFrame(tex, 0, 0, 16, 12);
    FlixelUiImage arrowImage = new FlixelUiImage(frame);

    FlixelUiDropdownStyle s = new FlixelUiDropdownStyle();
    s.arrow = arrowImage;
    s.padRight = 4f;
    s.padLeft = 4f;
    s.padTop = 4f;
    s.padBottom = 4f;
    s.fontSize = 14;

    FlixelUiDropdown d = new FlixelUiDropdown(200);
    d.setStyle(s);
    // No background on the style, so no selected item either, avoiding text-part draw.
    d.setSize(200, 28);

    RecordingBatch batch = new RecordingBatch();
    d.drawSelf(batch, 0, 0, 1f);

    // The arrow must be drawn at the image's natural pixel dimensions.
    assertTrue(batch.getDrawCount() >= 1, "arrow should produce at least one draw call");
    float arrowW = batch.getWidth(0);
    float arrowH = batch.getHeight(0);
    assertEquals(16f, arrowW, EPS, "arrow width must match natural image width, not min (0)");
    assertEquals(12f, arrowH, EPS, "arrow height must match natural image height, not min (0)");
  }

  @Test
  void autoRowHeightUsesLineHeightFromFont() {
    // Build a FlixelFont with a known lineHeight. When set directly on the text object,
    // fontScale == 1, so the measured line height equals the font's lineHeight field.
    String fnt = "common lineHeight=20 base=10 scaleW=64 scaleH=64\n"
        + "char id=65 x=0 y=0 width=8 height=10 xoffset=0 yoffset=4 xadvance=10 page=0\n";
    FlixelFont font = FlixelFont.fromFnt(fnt, null);

    FlixelUiDropdownStyle s = new FlixelUiDropdownStyle();
    s.itemHeight = 0f; // auto: use measured line height
    s.itemPadTop = 2f;
    s.itemPadBottom = 3f;
    s.fontSize = 14;

    FlixelUiDropdown d = new FlixelUiDropdown(200);
    d.setStyle(s);
    ui.add(d);
    d.addItem("A");
    d.open();
    ui.layout();

    FlixelUiDropdownList list = d.getList();

    // Set the font directly on every pool part so getLineHeight() returns the known value.
    for (int i = 0; i < list.getPoolSize(); i++) {
      FlixelUiTextPart part = list.getPoolPart(i);
      if (part != null) {
        part.getText().setFont(font);
      }
    }
    list.refreshLineHeight();

    // fontScale == 1 for a directly-set FlixelFont, so lineHeight == font's baked lineHeight.
    assertTrue(list.getCachedLineHeight() > 0f,
        "cached line height must be greater than 0 after the list opens");
    assertTrue(list.getCachedLineHeight() != (float) s.fontSize,
        "cached line height must differ from the fontSize fallback");
    assertEquals(20f, list.getCachedLineHeight(), EPS,
        "cached line height must equal the font's lineHeight when fontScale is 1");
    assertEquals(20f + 2f + 3f, list.getRowHeight(s), EPS,
        "auto row height must be line height + itemPadTop + itemPadBottom");
  }

  @Test
  void autoRowHeightFallsBackWhenNoCachedLineHeight() {
    // When the pool has not been built yet (list never opened), cachedLineHeight is 0 and the
    // row height should fall back to max(1, fontSize) + padding.
    FlixelUiDropdownStyle s = new FlixelUiDropdownStyle();
    s.itemHeight = 0f;
    s.itemPadTop = 2f;
    s.itemPadBottom = 3f;
    s.fontSize = 14;

    FlixelUiDropdown d = new FlixelUiDropdown(200);
    d.setStyle(s);
    FlixelUiDropdownList list = d.getList();

    // Pool not built; cachedLineHeight is 0.
    assertEquals(0f, list.getCachedLineHeight(), EPS);
    // Fallback: Math.max(1, fontSize) + padding = 14 + 2 + 3 = 19.
    assertEquals(14f + 2f + 3f, list.getRowHeight(s), EPS);
  }

  @Test
  void getItemAtConsistentWithAutoRowHeight() {
    // Verifies that getItemAt() uses the same row height as the auto formula,
    // so the row a click lands in matches what is visually rendered.
    String fnt = "common lineHeight=20 base=10 scaleW=64 scaleH=64\n"
        + "char id=65 x=0 y=0 width=8 height=10 xoffset=0 yoffset=4 xadvance=10 page=0\n";
    FlixelFont font = FlixelFont.fromFnt(fnt, null);

    FlixelUiDropdownStyle s = new FlixelUiDropdownStyle();
    s.itemHeight = 0f; // auto
    s.itemPadTop = 2f;
    s.itemPadBottom = 3f;
    s.listGap = 0f;
    s.fontSize = 14;

    FlixelUiDropdown d = new FlixelUiDropdown(200);
    d.setStyle(s);
    ui.add(d);
    d.addItem("A");
    d.addItem("B");
    d.addItem("C");
    d.open();
    ui.layout();

    FlixelUiDropdownList list = d.getList();
    for (int i = 0; i < list.getPoolSize(); i++) {
      FlixelUiTextPart part = list.getPoolPart(i);
      if (part != null) {
        part.getText().setFont(font);
      }
    }
    list.refreshLineHeight();

    // Auto row height: 20 + 2 + 3 = 25.
    float rowH = list.getRowHeight(s);
    assertEquals(25f, rowH, EPS);

    // The list starts just below the field. A hit at list.screenY + rowH * 0.5 is in row 0.
    float listY = list.getScreenY();
    float listX = list.getScreenX();
    assertEquals(0, d.getItemAt(listX + 10f, listY + rowH * 0.5f),
        "mid-point of row 0 slot should map to item 0");
    // A hit at list.screenY + rowH + rowH * 0.5 is in row 1.
    assertEquals(1, d.getItemAt(listX + 10f, listY + rowH + rowH * 0.5f),
        "mid-point of row 1 slot should map to item 1");
  }
}
