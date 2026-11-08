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
import org.flixelgdx.debug.inspector.FlixelSectionBuilder;
import org.flixelgdx.debug.panels.FlixelDebugInspectorPanel;
import org.flixelgdx.ui.text.FlixelTextBox;
import org.jetbrains.annotations.NotNull;

/**
 * Teaches the debugger's inspector how to show every UI widget.
 *
 * <p>Think of the inspector as a doctor reading a chart: without a chart for a type, it can only say
 * that the object exists. This class writes the charts for the whole toolkit, so selecting a
 * checkbox shows whether it is checked, a slider shows its value, a text box shows its text, and
 * so on. Sections stack from the base class up, so a checkbox shows the common widget rows plus
 * its own.
 *
 * <p>{@link FlixelUiDisplay} calls {@link #register()} the first time one is created while the game
 * runs in debug mode, so nothing has to be wired by hand and a release build pays nothing. It is
 * safe to call again; the sections are only added once per inspector.
 *
 * <p>Editable rows go through the widget's normal setters, so a value changed from the debugger
 * behaves like one changed in game code. That means {@link FlixelUiCheckbox#setChecked(boolean)},
 * {@link FlixelUiSlider#setValue(float)}, and {@link FlixelUiDropdown#select(int)} fire
 * their change signals, so the game reacts exactly as it would to a player.
 */
public final class FlixelUiDebugSections {

  private FlixelUiDebugSections() {}

  /**
   * Adds the UI sections to the running game's inspector when debug mode is on.
   *
   * <p>Does nothing when the debugger does not exist, when the game is not in debug mode, or when
   * the sections are already there.
   */
  public static void register() {
    if (Flixel.debug != null && Flixel.isDebugMode()) {
      register(Flixel.debug.inspector);
    }
  }

  /**
   * Adds the UI sections to an inspector, unless they are already there.
   *
   * @param inspector The inspector to teach.
   */
  public static void register(@NotNull FlixelDebugInspectorPanel inspector) {
    if (inspector.isRegistered(FlixelUiWidget.class)) {
      return;
    }
    inspector.register(FlixelUiDisplay.class, "UI display", b -> b
        .boolProp("Visible", FlixelUiDisplay::isVisible, FlixelUiDisplay::setVisible)
        .boolProp("Exists", FlixelUiDisplay::isExists, FlixelUiDisplay::setExists));
    inspector.register(FlixelUiWidget.class, "UI widget", FlixelUiDebugSections::widget);
    inspector.register(FlixelUiContainer.class, "UI container", b -> b
        .intProp("Children", FlixelUiContainer::getChildCount)
        .boolProp("Clip children", FlixelUiContainer::isClipChildren,
            FlixelUiContainer::setClipChildren));
    inspector.register(FlixelUiStack.class, "UI stack", b -> b
        .enumProp("Direction", FlixelUiStack::getDirection)
        .floatProp("Spacing", FlixelUiStack::getSpacing, FlixelUiStack::setSpacing));
    inspector.register(FlixelUiModal.class, "UI modal", b -> b
        .boolProp("Open", FlixelUiModal::isOpen));
    inspector.register(FlixelUiLabel.class, "UI label", b -> b
        .stringProp("Text", (w, out) -> out.concat(w.getText()),
            (w, text) -> w.setText(text.toString()))
        .floatProp("Wrap width", FlixelUiLabel::getWrapWidth, FlixelUiLabel::setWrapWidth));
    inspector.register(FlixelUiButton.class, "UI button", b -> b
        .stringProp("Text", (w, out) -> out.concat(w.getText()),
            (w, text) -> w.setText(text.toString())));
    inspector.register(FlixelUiAbstractToggle.class, "UI toggle", b -> b
        .stringProp("Text", (w, out) -> out.concat(w.getText()),
            (w, text) -> w.setText(text.toString())));
    inspector.register(FlixelUiCheckbox.class, "UI checkbox", b -> b
        .boolProp("Checked", FlixelUiCheckbox::isChecked, FlixelUiCheckbox::setChecked));
    inspector.register(FlixelUiRadioButton.class, "UI radio button", b -> b
        .boolProp("Selected", FlixelUiRadioButton::isSelected, (w, on) -> {
          if (on) {
            w.select();
          }
        }));
    inspector.register(FlixelUiSlider.class, "UI slider", b -> b
        .floatProp("Value", FlixelUiSlider::getValue, FlixelUiSlider::setValue)
        .floatProp("Min", FlixelUiSlider::getMin, (w, v) -> w.setRange(v, w.getMax()))
        .floatProp("Max", FlixelUiSlider::getMax, (w, v) -> w.setRange(w.getMin(), v))
        .floatProp("Step", FlixelUiSlider::getStep, FlixelUiSlider::setStep)
        .boolProp("Vertical", FlixelUiSlider::isVertical, FlixelUiSlider::setVertical)
        .boolProp("Dragging", FlixelUiSlider::isDragging));
    inspector.register(FlixelUiDropdown.class, "UI dropdown", b -> b
        .intProp("Selected index", FlixelUiDropdown::getSelectedIndex,
            FlixelUiDropdown::select)
        .stringProp("Selected item", (w, out) -> {
          CharSequence item = w.getSelectedItem();
          if (item != null) {
            out.concat(item);
          }
        })
        .intProp("Item count", FlixelUiDropdown::getItemCount)
        .boolProp("Open", FlixelUiDropdown::isOpen));
    inspector.register(FlixelTextBox.class, "UI text box", b -> b
        .stringProp("Text", (w, out) -> out.concat(w.getText()),
            (w, text) -> w.setText(text.toString()))
        .stringProp("Placeholder", (w, out) -> {
          CharSequence value = w.getPlaceholder();
          if (value != null) {
            out.concat(value);
          }
        }, (w, text) -> w.setPlaceholder(text.toString()))
        .boolProp("Focused", FlixelTextBox::isFocused, (w, on) -> {
          if (on) {
            w.focus();
          } else {
            w.blur();
          }
        })
        .intProp("Caret", FlixelTextBox::getCaret, FlixelTextBox::setCaret)
        .intProp("Selection start", FlixelTextBox::getSelectionStart)
        .intProp("Selection end", FlixelTextBox::getSelectionEnd)
        .intProp("Max length", FlixelTextBox::getMaxLength, FlixelTextBox::setMaxLength)
        .boolProp("Multi-line", FlixelTextBox::isMultiLine, FlixelTextBox::setMultiLine));
  }

  private static void widget(FlixelSectionBuilder<FlixelUiWidget> b) {
    b.boolProp("Visible", FlixelUiWidget::isVisible, FlixelUiWidget::setVisible)
        .boolProp("Enabled", FlixelUiWidget::isEnabled, FlixelUiWidget::setEnabled)
        .boolProp("Hovered", FlixelUiWidget::isHovered)
        .boolProp("Pressed", FlixelUiWidget::isPressed)
        .boolProp("Focused", FlixelUiWidget::isFocused)
        .floatProp("X", FlixelUiWidget::getX, FlixelUiWidget::setX)
        .floatProp("Y", FlixelUiWidget::getY, FlixelUiWidget::setY)
        .floatProp("Width", FlixelUiWidget::getWidth, FlixelUiWidget::setWidth)
        .floatProp("Height", FlixelUiWidget::getHeight, FlixelUiWidget::setHeight)
        .floatProp("Screen X", FlixelUiWidget::getScreenX)
        .floatProp("Screen Y", FlixelUiWidget::getScreenY)
        .floatProp("Alpha", FlixelUiWidget::getAlpha, FlixelUiWidget::setAlpha)
        .stringProp("Tooltip", (w, out) -> {
          CharSequence tip = w.getTooltip();
          if (tip != null) {
            out.concat(tip);
          }
        }, (w, text) -> w.setTooltip(text.length() == 0 ? null : text.toString()));
  }
}
