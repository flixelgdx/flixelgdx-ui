/**
 * Text entry widgets, their editing model, and character filters.
 *
 * <p>The three classes split the job of a text field into parts that can be used and tested on
 * their own:
 *
 * <ul>
 *   <li>{@link org.flixelgdx.ui.text.FlixelTextModel FlixelTextModel} owns the text, the caret,
 *       and the selection, and knows how to type, delete, move by character or word, and select a
 *       word. It draws nothing, so it can drive any custom text widget.</li>
 *   <li>{@link org.flixelgdx.ui.text.FlixelTextFilter FlixelTextFilter} decides which characters
 *       are let in. The built-in filters cover digits, decimal numbers, letters and digits, and
 *       text without spaces, and a lambda handles anything else.</li>
 *   <li>{@link org.flixelgdx.ui.text.FlixelTextBox FlixelTextBox} is the widget the game adds to a
 *       display. It draws the text, the caret, the selection highlight, and an optional
 *       placeholder, scrolls long text, masks passwords, and supports single-line and multi-line
 *       editing.</li>
 * </ul>
 *
 * <h2>Keyboard input</h2>
 *
 * <p>Typing is the one place the toolkit listens on its own. A text box implements
 * {@link org.flixelgdx.input.FlixelKeyboardListener FlixelKeyboardListener} and registers itself
 * when it is created, but only the focused, enabled box reacts, so typing goes into the box the
 * player clicked. It handles the usual editing keys and the copy, cut, paste, and select-all
 * shortcuts. Destroy a text box when you are done with it so it stops listening.
 *
 * <h2>Mouse selection</h2>
 *
 * <p>{@link org.flixelgdx.ui.text.FlixelTextBox#pointerDown(float, float, int)
 * FlixelTextBox.pointerDown(...)},
 * {@link org.flixelgdx.ui.text.FlixelTextBox#pointerDrag(float, float)
 * FlixelTextBox.pointerDrag(...)}, and
 * {@link org.flixelgdx.ui.text.FlixelTextBox#pointerUp() FlixelTextBox.pointerUp()} turn a press
 * and drag into a selection: one click places the caret, dragging extends the selection, two
 * clicks select a word, and three select a line. A
 * {@link org.flixelgdx.ui.FlixelUiPointer FlixelUiPointer} calls these for you; pass it the click
 * count through {@link org.flixelgdx.ui.FlixelUiPointer#down(int) FlixelUiPointer.down(...)}.
 *
 * <pre>{@code
 * FlixelTextBox name = new FlixelTextBox(200);
 * name.setPlaceholder("Your name");
 * name.setMaxLength(16);
 * name.setFilter(FlixelTextFilter.ALPHANUMERIC);
 * name.onSubmit.add(box -> startGame(box.getText().toString()));
 * ui.add(name);
 * }</pre>
 *
 * <p>The look lives in {@link org.flixelgdx.ui.skin.FlixelTextBoxStyle FlixelTextBoxStyle},
 * alongside all other widget styles.
 *
 * @see org.flixelgdx.ui.text.FlixelTextBox
 * @see org.flixelgdx.ui.text.FlixelTextModel
 * @see org.flixelgdx.ui.text.FlixelTextFilter
 */
package org.flixelgdx.ui.text;
