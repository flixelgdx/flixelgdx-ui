/**
 * Skins and styles: how a game tells every widget what it should look like.
 *
 * <p>Looks and behavior are kept apart. A widget such as a button only knows how to behave (hover,
 * press, click); everything about its appearance lives in a separate style object. That lets one
 * game reuse the same button code with several looks, and lets an artist change the look without
 * touching the code that wires buttons up.
 *
 * <h2>Styles</h2>
 *
 * <p>Each widget type has a style class that implements
 * {@link org.flixelgdx.ui.skin.FlixelUiStyle FlixelUiStyle}: a plain object with public fields
 * for its backgrounds, font file, font size, colors, and padding. For example,
 * {@link org.flixelgdx.ui.skin.FlixelUiButtonStyle FlixelUiButtonStyle} has {@code up},
 * {@code over}, {@code down}, and {@code disabled} backgrounds for each state, and
 * {@link org.flixelgdx.ui.skin.FlixelUiSliderStyle FlixelUiSliderStyle} has a track, an optional
 * fill, and a thumb. State variants that are left {@code null} fall back to the plain one, so a
 * quick prototype only needs to fill in a field or two.
 *
 * <h2>Skins</h2>
 *
 * <p>A {@link org.flixelgdx.ui.skin.FlixelUiSkin FlixelUiSkin} stores named styles for every
 * style class, such as a {@code "default"} and a {@code "fab"} button style. Styles are filed by
 * their exact class, so a button style and a checkbox style can both be called {@code "default"}
 * without clashing. The skin handed to a {@link org.flixelgdx.ui.FlixelUiDisplay FlixelUiDisplay}
 * is where its widgets find their style, by the name set with
 * {@link org.flixelgdx.ui.FlixelUiWidget#setStyleName(String) FlixelUiWidget.setStyleName(...)}.
 * A widget can also skip the skin and take a style object directly, for a one-off look.
 *
 * <pre>{@code
 * FlixelUiSkin skin = new FlixelUiSkin();
 *
 * FlixelUiButtonStyle button = new FlixelUiButtonStyle();
 * button.up = skin.track(FlixelNineSlice.load(Flixel.files.internal("ui/button.png"), 6, 6, 6, 6));
 * button.font = Flixel.files.internal("fonts/pixel.ttf");
 * skin.add("default", button);
 *
 * FlixelUiDisplay ui = new FlixelUiDisplay(FlixelUiDisplay.createHudCamera(), skin);
 * }</pre>
 *
 * <h2>Bring your own assets</h2>
 *
 * <p>The extension ships no built-in skin. A game loads its own images and fonts, fills in the
 * style objects, and adds them to a skin. Backgrounds handed to
 * {@link org.flixelgdx.ui.skin.FlixelUiSkin#track(org.flixelgdx.ui.graphics.FlixelUiBackground)
 * FlixelUiSkin.track(...)} are destroyed with the skin, once each, even when several styles share
 * them. Fonts are {@link org.flixelgdx.file.FlixelFile FlixelFile} references, so the skin never
 * owns a loaded font.
 *
 * @see org.flixelgdx.ui.skin.FlixelUiSkin
 * @see org.flixelgdx.ui.skin.FlixelUiStyle
 * @see org.flixelgdx.ui.graphics.FlixelUiBackground
 */
package org.flixelgdx.ui.skin;
