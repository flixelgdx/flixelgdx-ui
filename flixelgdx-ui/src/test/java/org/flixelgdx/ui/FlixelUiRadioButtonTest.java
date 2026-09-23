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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link FlixelUiRadioButton} and {@link FlixelUiRadioGroup}: mutual exclusivity,
 * click semantics, group navigation, signal order, and moving buttons between groups.
 */
@ExtendWith(FlixelUiHeadlessExtension.class)
class FlixelUiRadioButtonTest {

  // --- basic exclusivity ---

  @Test
  void selectingOneButtonDeselectsThePreviousOne() {
    FlixelUiRadioGroup group = new FlixelUiRadioGroup();
    FlixelUiRadioButton a = new FlixelUiRadioButton("A", group);
    FlixelUiRadioButton b = new FlixelUiRadioButton("B", group);

    group.select(a);
    assertTrue(a.isSelected());
    assertFalse(b.isSelected());

    group.select(b);
    assertFalse(a.isSelected());
    assertTrue(b.isSelected());
    assertSame(b, group.getSelected());
  }

  @Test
  void initialStateIsNotSelected() {
    FlixelUiRadioGroup group = new FlixelUiRadioGroup();
    FlixelUiRadioButton r = new FlixelUiRadioButton("X", group);
    assertFalse(r.isSelected());
    assertNull(group.getSelected());
  }

  // --- click semantics ---

  @Test
  void clickSelectsAnUnselectedButton() {
    FlixelUiRadioGroup group = new FlixelUiRadioGroup();
    FlixelUiRadioButton a = new FlixelUiRadioButton("A", group);
    a.click();
    assertTrue(a.isSelected());
  }

  @Test
  void clickDoesNothingWhenAlreadySelected() {
    FlixelUiRadioGroup group = new FlixelUiRadioGroup();
    FlixelUiRadioButton a = new FlixelUiRadioButton("A", group);
    FlixelUiRadioButton b = new FlixelUiRadioButton("B", group);
    group.select(a);
    int[] count = new int[1];
    group.onChange.add(g -> count[0]++);
    a.click(); // Already selected; must do nothing.
    assertEquals(0, count[0]);
    assertTrue(a.isSelected());
    assertFalse(b.isSelected());
  }

  @Test
  void disabledButtonIgnoresClick() {
    FlixelUiRadioGroup group = new FlixelUiRadioGroup();
    FlixelUiRadioButton a = new FlixelUiRadioButton("A", group);
    a.setEnabled(false);
    a.click();
    assertFalse(a.isSelected());
    assertNull(group.getSelected());
  }

  // --- select(int) ---

  @Test
  void selectByIndexSelectsTheCorrectButton() {
    FlixelUiRadioGroup group = new FlixelUiRadioGroup();
    FlixelUiRadioButton a = new FlixelUiRadioButton("A", group);
    FlixelUiRadioButton b = new FlixelUiRadioButton("B", group);
    FlixelUiRadioButton c = new FlixelUiRadioButton("C", group);
    group.select(1);
    assertFalse(a.isSelected());
    assertTrue(b.isSelected());
    assertFalse(c.isSelected());
    assertEquals(1, group.getSelectedIndex());
  }

  @Test
  void selectByIndexOutOfRangeDoesNothing() {
    FlixelUiRadioGroup group = new FlixelUiRadioGroup();
    new FlixelUiRadioButton("A", group);
    group.select(5); // Out of range.
    assertNull(group.getSelected());
  }

  // --- clearSelection ---

  @Test
  void clearSelectionDeselectsWithoutSignal() {
    FlixelUiRadioGroup group = new FlixelUiRadioGroup();
    FlixelUiRadioButton a = new FlixelUiRadioButton("A", group);
    group.select(a);
    int[] count = new int[1];
    group.onChange.add(g -> count[0]++);
    a.onChange.add(r -> count[0]++);
    group.clearSelection();
    assertNull(group.getSelected());
    assertFalse(a.isSelected());
    assertEquals(0, count[0], "clearSelection must not fire signals");
  }

  // --- selectNext / selectPrevious wrapping ---

  @Test
  void selectNextWrapsAroundToTheFirstButton() {
    FlixelUiRadioGroup group = new FlixelUiRadioGroup();
    FlixelUiRadioButton a = new FlixelUiRadioButton("A", group);
    FlixelUiRadioButton b = new FlixelUiRadioButton("B", group);
    FlixelUiRadioButton c = new FlixelUiRadioButton("C", group);
    group.select(c);
    group.selectNext();
    assertSame(a, group.getSelected());
  }

  @Test
  void selectPreviousWrapsAroundToTheLastButton() {
    FlixelUiRadioGroup group = new FlixelUiRadioGroup();
    FlixelUiRadioButton a = new FlixelUiRadioButton("A", group);
    new FlixelUiRadioButton("B", group);
    FlixelUiRadioButton c = new FlixelUiRadioButton("C", group);
    group.select(a);
    group.selectPrevious();
    assertSame(c, group.getSelected());
  }

  @Test
  void selectNextSkipsDisabledButtons() {
    FlixelUiRadioGroup group = new FlixelUiRadioGroup();
    FlixelUiRadioButton a = new FlixelUiRadioButton("A", group);
    FlixelUiRadioButton b = new FlixelUiRadioButton("B", group);
    FlixelUiRadioButton c = new FlixelUiRadioButton("C", group);
    b.setEnabled(false);
    group.select(a);
    group.selectNext(); // B is disabled; should jump to C.
    assertSame(c, group.getSelected());
  }

  @Test
  void selectNextDoesNothingWhenAllDisabled() {
    FlixelUiRadioGroup group = new FlixelUiRadioGroup();
    FlixelUiRadioButton a = new FlixelUiRadioButton("A", group);
    FlixelUiRadioButton b = new FlixelUiRadioButton("B", group);
    group.select(a);
    a.setEnabled(false);
    b.setEnabled(false);
    int[] count = new int[1];
    group.onChange.add(g -> count[0]++);
    group.selectNext();
    assertSame(a, group.getSelected());
    assertEquals(0, count[0]);
  }

  @Test
  void selectPreviousSkipsInvisibleButtons() {
    FlixelUiRadioGroup group = new FlixelUiRadioGroup();
    FlixelUiRadioButton a = new FlixelUiRadioButton("A", group);
    FlixelUiRadioButton b = new FlixelUiRadioButton("B", group);
    FlixelUiRadioButton c = new FlixelUiRadioButton("C", group);
    b.setVisible(false);
    group.select(c);
    group.selectPrevious(); // B is invisible; should jump to A.
    assertSame(a, group.getSelected());
  }

  // --- signal order ---

  @Test
  void signalOrderIsDeselectedThenSelectedThenGroup() {
    FlixelUiRadioGroup group = new FlixelUiRadioGroup();
    FlixelUiRadioButton a = new FlixelUiRadioButton("A", group);
    FlixelUiRadioButton b = new FlixelUiRadioButton("B", group);
    group.select(a);

    List<String> order = new ArrayList<>();
    a.onChange.add(r -> order.add("a"));
    b.onChange.add(r -> order.add("b"));
    group.onChange.add(g -> order.add("group"));

    group.select(b);

    assertEquals(3, order.size());
    assertEquals("a", order.get(0), "deselected radio fires first");
    assertEquals("b", order.get(1), "newly selected radio fires second");
    assertEquals("group", order.get(2), "group fires last");
  }

  @Test
  void eachSignalFiresExactlyOnce() {
    FlixelUiRadioGroup group = new FlixelUiRadioGroup();
    FlixelUiRadioButton a = new FlixelUiRadioButton("A", group);
    FlixelUiRadioButton b = new FlixelUiRadioButton("B", group);
    group.select(a);

    int[] aCount = { 0 };
    int[] bCount = { 0 };
    int[] gCount = { 0 };
    a.onChange.add(r -> aCount[0]++);
    b.onChange.add(r -> bCount[0]++);
    group.onChange.add(g -> gCount[0]++);

    group.select(b);

    assertEquals(1, aCount[0]);
    assertEquals(1, bCount[0]);
    assertEquals(1, gCount[0]);
  }

  @Test
  void groupSignalNotFiredOnNoChange() {
    FlixelUiRadioGroup group = new FlixelUiRadioGroup();
    FlixelUiRadioButton a = new FlixelUiRadioButton("A", group);
    group.select(a);
    int[] count = { 0 };
    group.onChange.add(g -> count[0]++);
    group.select(a); // Already selected.
    assertEquals(0, count[0]);
  }

  // --- moving a radio between groups ---

  @Test
  void movingRadioToAnotherGroupRemovesItFromTheOriginal() {
    FlixelUiRadioGroup g1 = new FlixelUiRadioGroup();
    FlixelUiRadioGroup g2 = new FlixelUiRadioGroup();
    FlixelUiRadioButton r = new FlixelUiRadioButton("R", g1);
    assertEquals(1, g1.getCount());
    assertEquals(0, g2.getCount());
    assertSame(g1, r.getGroup());

    g2.add(r);
    assertEquals(0, g1.getCount());
    assertEquals(1, g2.getCount());
    assertSame(g2, r.getGroup());
  }

  @Test
  void movingSelectedRadioClearsOldGroupSelection() {
    FlixelUiRadioGroup g1 = new FlixelUiRadioGroup();
    FlixelUiRadioGroup g2 = new FlixelUiRadioGroup();
    FlixelUiRadioButton a = new FlixelUiRadioButton("A", g1);
    FlixelUiRadioButton b = new FlixelUiRadioButton("B", g1);
    group_select(g1, a);
    assertSame(a, g1.getSelected());

    g2.add(a);
    assertNull(g1.getSelected(), "old group's selection must be cleared");
    assertSame(a, g2.getSelected(), "moved button stays selected in new group");
  }

  // --- count and getRadioAt ---

  @Test
  void countAndGetRadioAt() {
    FlixelUiRadioGroup group = new FlixelUiRadioGroup();
    FlixelUiRadioButton a = new FlixelUiRadioButton("A", group);
    FlixelUiRadioButton b = new FlixelUiRadioButton("B", group);
    assertEquals(2, group.getCount());
    assertSame(a, group.getRadioAt(0));
    assertSame(b, group.getRadioAt(1));
  }

  // --- select(radio, false) suppresses signal ---

  @Test
  void selectSilentDoesNotFireSignals() {
    FlixelUiRadioGroup group = new FlixelUiRadioGroup();
    FlixelUiRadioButton a = new FlixelUiRadioButton("A", group);
    FlixelUiRadioButton b = new FlixelUiRadioButton("B", group);
    group.select(a);
    int[] count = { 0 };
    a.onChange.add(r -> count[0]++);
    b.onChange.add(r -> count[0]++);
    group.onChange.add(g -> count[0]++);
    group.select(b, false);
    assertEquals(0, count[0]);
    assertSame(b, group.getSelected());
  }

  // --- helpers ---

  /** Selects without relying on the group.select(FlixelUiRadioButton) path under test. */
  private static void group_select(FlixelUiRadioGroup g, FlixelUiRadioButton r) {
    g.select(r, false);
  }
}
