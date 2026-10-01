/**
 * Backgrounds that widgets draw behind themselves: nine-slices, stretched images, and plain color
 * fills.
 *
 * <p>Every background implements
 * {@link org.flixelgdx.ui.graphics.FlixelUiBackground FlixelUiBackground}. A widget hands its
 * background a rectangle ("be this big, right here") and the background fills it, so the same
 * background can dress a tiny checkbox and a full-screen panel. Because every kind shares that
 * interface, a style field such as {@code FlixelUiButtonStyle.up} can hold any of them.
 *
 * <h2>Available backgrounds</h2>
 *
 * <ul>
 *   <li>{@link org.flixelgdx.ui.graphics.FlixelNineSlice FlixelNineSlice} cuts an image into a
 *       3x3 grid. The four corners never stretch, the edges stretch along one axis, and the
 *       center stretches both ways, so rounded or bordered art keeps its shape at any size. Use it
 *       for panels, buttons, and text box frames.</li>
 *   <li>{@link org.flixelgdx.ui.graphics.FlixelUiImage FlixelUiImage} stretches one whole image
 *       to the rectangle. Use it for check marks, radio dots, slider thumbs, and other icons drawn
 *       at their own size.</li>
 *   <li>{@link org.flixelgdx.ui.graphics.FlixelUiColorFill FlixelUiColorFill} paints a solid
 *       color without needing an image at all. It is handy for prototypes, selection highlights,
 *       carets, and dimmed modal backdrops.</li>
 * </ul>
 *
 * <p>To show an image as a widget of its own, with a position, anchors, and scaling, use
 * {@link org.flixelgdx.ui.FlixelUiPicture FlixelUiPicture} instead; it draws through a
 * {@code FlixelUiImage} internally.
 *
 * <h2>Tinting and fading</h2>
 *
 * <p>A background draws with the batch's current color. The widget sets that color (its tint,
 * multiplied with the combined alpha of the widget and its parents) right before drawing, so a
 * background never needs to know about fades or tints, and one background can be shared by many
 * widgets in different states. Drawing never allocates.
 *
 * <h2>Loading images and cleaning up</h2>
 *
 * <p>Images come from the game's own files. Load them with
 * {@link org.flixelgdx.ui.graphics.FlixelNineSlice#load(org.flixelgdx.file.FlixelFile, int, int, int, int)
 * FlixelNineSlice.load(...)} or
 * {@link org.flixelgdx.ui.graphics.FlixelUiImage#load(org.flixelgdx.file.FlixelFile)
 * FlixelUiImage.load(...)}, or build them from an existing
 * {@link org.flixelgdx.graphics.FlixelFrame FlixelFrame}, such as a frame from a texture atlas:
 *
 * <pre>{@code
 * // Corners are 8 pixels on every side of the source image.
 * FlixelNineSlice panel = FlixelNineSlice.load(Flixel.files.internal("ui/panel.png"), 8, 8, 8, 8);
 * FlixelUiImage check = FlixelUiImage.load(Flixel.files.internal("ui/check.png"));
 * FlixelUiColorFill highlight = new FlixelUiColorFill(0.2f, 0.5f, 1f, 0.4f);
 * }</pre>
 *
 * <p>A background that loaded its own image releases it in {@code destroy()}; one built from a
 * frame you passed in leaves that frame alone. Since styles often share backgrounds, hand each
 * loaded background to
 * {@link org.flixelgdx.ui.skin.FlixelUiSkin#track(org.flixelgdx.ui.graphics.FlixelUiBackground)
 * FlixelUiSkin.track(...)} so the skin destroys it exactly once.
 *
 * <p>On the browser backend, images are decoded asynchronously. Calling {@code load(...)} before
 * the file has been decoded throws an exception. Preload image files in a loading state with
 * {@code Flixel.assets.load(path)} and wait for {@code Flixel.assets.update()} to return
 * {@code true} before building the skin.
 *
 * @see org.flixelgdx.ui.graphics.FlixelUiBackground
 * @see org.flixelgdx.ui.skin.FlixelUiSkin
 */
package org.flixelgdx.ui.graphics;
