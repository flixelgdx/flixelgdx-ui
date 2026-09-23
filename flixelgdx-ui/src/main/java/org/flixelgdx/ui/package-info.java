/**
 * UI widget toolkit for FlixelGDX: panels, modals, buttons, text boxes, checkboxes, radio
 * buttons, dropdowns, and tooltips.
 *
 * <h2>Overview</h2>
 *
 * <p>A game shows UI by adding one {@link FlixelUiDisplay} to its state. The display is bound to a
 * camera (usually a transparent one from {@link FlixelUiDisplay#createHudCamera()}), owns the
 * widget tree, lays it out, and draws it during that camera's pass. Every piece of UI is a
 * {@link FlixelUiWidget}; widgets that hold other widgets are {@link FlixelUiContainer}s, and a
 * {@link FlixelUiStack} is a container that places its children in a column or a row.
 *
 * <p>Layout is anchors plus stacks. A widget can be anchored to one of the nine
 * {@link FlixelAlign} positions of its parent with an offset, sized as a
 * fraction of its parent, or placed by a stack. When the camera's visible area changes size, the
 * display lays everything out again.
 *
 * <p>Widgets implement the {@code org.flixelgdx.functional} interfaces instead of extending a
 * scene object, so existing tools such as {@code FlixelTween} work on them unchanged. Every image
 * or font parameter takes a {@code FlixelFile} rather than a string path. There is no built-in
 * skin; a game supplies its own assets and assembles them into a
 * {@link FlixelUiSkin}.
 *
 * <h2>You wire the input</h2>
 *
 * <p>The toolkit never reads input itself. A widget exposes plain methods such as
 * {@link FlixelUiWidget#hover()}, {@link FlixelUiWidget#press()}, and {@code click()} that the game
 * calls after doing its own hit-testing and input mapping; nothing here registers a listener or
 * polls a device. The display only offers geometric helpers such as
 * {@link FlixelUiDisplay#getWidgetAt(float, float)}, which takes a point in the same space that
 * {@code Flixel.mouse.getWorldX(camera)} reports. A minimal mouse wiring looks like this:
 *
 * <pre>{@code
 * FlixelUiWidget hovered;
 *
 * @Override
 * public void update(float elapsed) {
 *   super.update(elapsed);
 *   float mx = Flixel.mouse.getWorldX(ui.getCamera());
 *   float my = Flixel.mouse.getWorldY(ui.getCamera());
 *   FlixelUiWidget hit = ui.getWidgetAt(mx, my);
 *   if (hit != hovered) {
 *     if (hovered != null) hovered.unhover();
 *     if (hit != null) hit.hover();
 *     hovered = hit;
 *   }
 *   if (hit != null && Flixel.mouse.justPressed(FlixelMouseButton.LEFT)) hit.press();
 *   if (hit instanceof FlixelUiButton b && Flixel.mouse.justReleased(FlixelMouseButton.LEFT)) {
 *     b.release();
 *     b.click();
 *   }
 * }
 * }</pre>
 *
 * <p>A gamepad or keyboard maps onto the same methods through the game's own actions, for example
 * moving focus with {@link FlixelUiWidget#focus()} and {@link FlixelUiWidget#blur()} and reading
 * {@link FlixelUiDisplay#getFocused()}. This keeps the toolkit simple and leaves the game in full
 * control of how mouse, keyboard, or gamepad input drives the interface.
 */
package org.flixelgdx.ui;

import org.flixelgdx.ui.skin.FlixelUiSkin;
import org.flixelgdx.util.FlixelAlign;
