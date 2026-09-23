/**
 * Text entry widgets, their editing model, and character filters.
 *
 * <p>The three classes work together: {@link FlixelTextModel} owns the buffer and caret logic
 * without any rendering concern, {@link FlixelTextFilter} decides which characters are allowed in,
 * and {@link FlixelTextBox} is the visual widget the game adds to a display.
 *
 * <p>The style lives in {@link FlixelTextBoxStyle} alongside all other widget styles.
 */
package org.flixelgdx.ui.text;

import org.flixelgdx.ui.skin.FlixelTextBoxStyle;
