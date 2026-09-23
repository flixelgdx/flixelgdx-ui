/**
 * Skins and styles: how a game tells every widget what it should look like.
 *
 * <p>Each widget type has a style class that implements {@link FlixelUiStyle}: a plain object with
 * public fields for its backgrounds, font file, font size, colors, and padding. A
 * {@link FlixelUiSkin} stores named styles for every style class, such as a {@code "default"} and
 * a {@code "fab"} button style. The skin handed to a {@link FlixelUiDisplay} is where its widgets
 * find their style, by the name set with {@link FlixelUiWidget#setStyle(String)}.
 *
 * <p>The extension ships no built-in skin. A game loads its own images and fonts, fills in the
 * style objects, and adds them to a skin. Backgrounds handed to
 * {@link FlixelUiSkin#track(FlixelUiBackground)} are destroyed with the skin.
 */
package org.flixelgdx.ui.skin;

import org.flixelgdx.ui.FlixelUiDisplay;
import org.flixelgdx.ui.FlixelUiWidget;
import org.flixelgdx.ui.graphics.FlixelUiBackground;
