/**
 * Backgrounds that widgets draw behind themselves: nine-slices, stretched images, and plain color
 * fills.
 *
 * <p>Every background implements {@link FlixelUiBackground}, which fills any rectangle it is given
 * using the batch's current color. A {@link FlixelNineSlice} keeps its corners sharp while its
 * edges and center stretch, a {@link FlixelUiImage} stretches one image (and doubles as an icon),
 * and a {@link FlixelUiColorFill} paints a solid color without needing an image at all.
 *
 * <p>Images come from the game's own files: load them with
 * {@code FlixelNineSlice.load(Flixel.files.internal("ui/panel.png"), 8, 8, 8, 8)} or
 * {@code FlixelUiImage.load(...)}, or build them from an existing atlas frame. A background that
 * loaded its own image releases it in {@code destroy()}; one built from a frame you passed in
 * leaves that frame alone.
 *
 * <p>On the browser backend, images are decoded asynchronously. Calling {@code load(...)} before
 * the file has been decoded throws an exception. Preload image files in a loading state with
 * {@code Flixel.assets.load(path)} and wait for {@code Flixel.assets.update()} to return
 * {@code true} before building the skin.
 */
package org.flixelgdx.ui.graphics;
