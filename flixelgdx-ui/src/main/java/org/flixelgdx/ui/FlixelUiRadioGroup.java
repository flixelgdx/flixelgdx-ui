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
import org.flixelgdx.util.signal.FlixelSignal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages a set of {@link FlixelUiRadioButton} widgets so that at most one is selected at a time.
 *
 * <p>Think of a group as a radio channel selector: turning one station on automatically turns the
 * others off. The group is a plain object, not a widget. It does not draw anything; it only tracks
 * membership and the current selection.
 *
 * <pre>{@code
 * FlixelUiRadioGroup difficulty = new FlixelUiRadioGroup();
 * FlixelUiRadioButton easy   = new FlixelUiRadioButton("Easy",   difficulty);
 * FlixelUiRadioButton medium = new FlixelUiRadioButton("Medium", difficulty);
 * FlixelUiRadioButton hard   = new FlixelUiRadioButton("Hard",   difficulty);
 * difficulty.select(0); // "Easy" selected by default.
 * difficulty.onChange.add(g -> applyDifficulty(g.getSelected()));
 * }</pre>
 *
 * <h2>Moving radios between groups</h2>
 *
 * <p>{@link #add(FlixelUiRadioButton)} removes the button from its previous group first, then
 * adds it here. A newly added button is not selected, and the group's current selection is
 * unchanged.
 *
 * <h2>Signal order</h2>
 *
 * <p>When a selection changes, each affected radio's own {@link FlixelUiRadioButton#onChange}
 * fires first (the deselected radio, then the newly selected one), and then {@link #onChange}
 * fires once for the group.
 */
public final class FlixelUiRadioGroup {

  /**
   * Dispatched once after every selection change, after the affected radios' own signals.
   *
   * <p>The payload is this group, so one listener can serve several groups without allocating.
   */
  public final FlixelSignal<FlixelUiRadioGroup> onChange = new FlixelSignal<>();

  /** Radios in add order. */
  @NotNull
  private final FlixelArray<FlixelUiRadioButton> radios =
      new FlixelArray<>(FlixelUiRadioButton[]::new);

  /** The currently selected radio, or {@code null} when nothing is selected. */
  @Nullable
  private FlixelUiRadioButton selected;

  /** Creates an empty group with no selection. */
  public FlixelUiRadioGroup() {}

  /**
   * Adds a radio button to this group, removing it from its previous group first.
   *
   * <p>The button is added at the end of the list. Its selected state is not changed; if it is
   * already selected, it becomes this group's selection.
   *
   * @param radio The button to add; must not be {@code null}.
   */
  public void add(@NotNull FlixelUiRadioButton radio) {
    FlixelUiRadioGroup previous = radio.getGroup();
    if (previous == this) {
      return;
    }
    if (previous != null) {
      previous.removeInternal(radio);
    }
    radio.setGroup(this);
    radios.add(radio);
    if (radio.isSelected()) {
      // Become the group's selection; deselect any current one silently.
      if (selected != null && selected != radio) {
        selected.setSelectedInternal(false, true);
      }
      selected = radio;
    }
  }

  /**
   * Removes a radio button from this group.
   *
   * <p>If the removed button was the selected one, the selection is cleared silently (no signal).
   *
   * @param radio The button to remove.
   */
  public void remove(@NotNull FlixelUiRadioButton radio) {
    if (radio.getGroup() != this) {
      return;
    }
    removeInternal(radio);
    radio.setGroup(null);
  }

  /**
   * Selects a radio button, deselecting the currently selected one.
   *
   * <p>Does nothing when the button is disabled or not in this group.
   *
   * @param radio The button to select.
   */
  public void select(@NotNull FlixelUiRadioButton radio) {
    select(radio, true);
  }

  /**
   * Selects the button at a zero-based index in add order.
   *
   * <p>Does nothing when the index is out of range or the button is disabled.
   *
   * @param index The zero-based index.
   */
  public void select(int index) {
    if (index < 0 || index >= radios.getSize()) {
      return;
    }
    select(radios.getItems()[index], true);
  }

  /**
   * Selects a radio button, optionally firing signals.
   *
   * <p>Deselects the currently selected button first. If the new button is already selected,
   * nothing happens. Does nothing when the button is not in this group.
   *
   * @param radio The button to select.
   * @param notify {@code true} to fire {@link FlixelUiRadioButton#onChange} on the affected
   *     radios and then {@link #onChange} on this group.
   */
  public void select(@NotNull FlixelUiRadioButton radio, boolean notify) {
    if (radio.getGroup() != this) {
      return;
    }
    if (selected == radio) {
      return;
    }
    FlixelUiRadioButton prev = selected;
    selected = radio;
    if (prev != null) {
      prev.setSelectedInternal(false, notify);
    }
    radio.setSelectedInternal(true, notify);
    if (notify) {
      onChange.dispatch(this);
    }
  }

  /**
   * Selects the next radio button after the currently selected one, wrapping around and skipping
   * disabled or invisible buttons.
   *
   * <p>Does nothing when all buttons are disabled or invisible.
   */
  public void selectNext() {
    int n = radios.getSize();
    if (n == 0) {
      return;
    }
    int start = selectedIndex();
    int next = (start < 0 ? 0 : start + 1) % n;
    for (int i = 0; i < n; i++) {
      FlixelUiRadioButton candidate = radios.getItems()[next];
      if (canSelect(candidate)) {
        select(candidate, true);
        return;
      }
      next = (next + 1) % n;
    }
  }

  /**
   * Selects the radio button before the currently selected one, wrapping around and skipping
   * disabled or invisible buttons.
   *
   * <p>Does nothing when all buttons are disabled or invisible.
   */
  public void selectPrevious() {
    int n = radios.getSize();
    if (n == 0) {
      return;
    }
    int start = selectedIndex();
    int prev = (start <= 0 ? n - 1 : start - 1);
    for (int i = 0; i < n; i++) {
      FlixelUiRadioButton candidate = radios.getItems()[prev];
      if (canSelect(candidate)) {
        select(candidate, true);
        return;
      }
      prev = (prev - 1 + n) % n;
    }
  }

  /**
   * Clears the current selection without firing any signal.
   */
  public void clearSelection() {
    FlixelUiRadioButton prev = selected;
    selected = null;
    if (prev != null) {
      prev.setSelectedInternal(false, false);
    }
  }

  /**
   * Returns the currently selected radio button, or {@code null} when nothing is selected.
   *
   * @return The selected button, or {@code null}.
   */
  @Nullable
  public FlixelUiRadioButton getSelected() {
    return selected;
  }

  /**
   * Returns the zero-based index of the currently selected button in add order.
   *
   * @return The index, or {@code -1} when nothing is selected.
   */
  public int getSelectedIndex() {
    return selectedIndex();
  }

  /**
   * Selects the button at {@code index} without firing any signal, like a silent
   * {@link #select(int)}.
   *
   * <p>Use it for an initial selection, such as one loaded from a save. An index outside the group
   * (for example {@code -1}) clears the selection.
   *
   * @param index The zero-based index in add order, or {@code -1} for no selection.
   */
  public void setSelectedIndex(int index) {
    if (index < 0 || index >= radios.getSize()) {
      clearSelection();
      return;
    }
    select(radios.getItems()[index], false);
  }

  /**
   * Returns the number of radio buttons in this group.
   *
   * @return The count.
   */
  public int getCount() {
    return radios.getSize();
  }

  /**
   * Returns the radio button at the given zero-based index.
   *
   * @param index The index in add order.
   * @return The button at that index.
   * @throws ArrayIndexOutOfBoundsException If the index is out of range.
   */
  @NotNull
  public FlixelUiRadioButton getRadioAt(int index) {
    return radios.getItems()[index];
  }

  /** Removes a button from the internal list and clears the selection if it was selected. */
  private void removeInternal(@NotNull FlixelUiRadioButton radio) {
    radios.removeValue(radio, true);
    if (selected == radio) {
      selected = null;
    }
  }

  /** Returns the index of the selected button, or -1. */
  private int selectedIndex() {
    FlixelUiRadioButton sel = selected;
    if (sel == null) {
      return -1;
    }
    FlixelUiRadioButton[] items = radios.getItems();
    for (int i = 0, n = radios.getSize(); i < n; i++) {
      if (items[i] == sel) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Returns whether a radio can be chosen during a next/previous traversal.
   *
   * @param radio The candidate button.
   * @return {@code true} when the button is enabled and visible.
   */
  private static boolean canSelect(@NotNull FlixelUiRadioButton radio) {
    return radio.isEnabled() && radio.isVisible();
  }
}
