/**
 * UI widget toolkit for FlixelGDX: panels, modals, buttons, text boxes, checkboxes, radio
 * buttons, dropdowns, sliders, pictures, and tooltips.
 *
 * <h2>Overview</h2>
 *
 * <p>A game shows UI by adding one {@link org.flixelgdx.ui.FlixelUiDisplay FlixelUiDisplay} to its state. The display is bound to a
 * camera (usually a transparent one from {@link org.flixelgdx.ui.FlixelUiDisplay#createHudCamera() FlixelUiDisplay.createHudCamera()}), owns the
 * widget tree, lays it out, and draws it during that camera's pass. Every piece of UI is a
 * {@link org.flixelgdx.ui.FlixelUiWidget FlixelUiWidget}; widgets that hold other widgets are {@link org.flixelgdx.ui.FlixelUiContainer FlixelUiContainer}s, and a
 * {@link org.flixelgdx.ui.FlixelUiStack FlixelUiStack} is a container that places its children in a column or a row.
 *
 * <p>Layout is anchors plus stacks. A widget can be anchored to one of the nine
 * {@link org.flixelgdx.util.FlixelAlign FlixelAlign} positions of its parent with an offset, sized as a
 * fraction of its parent, or placed by a stack. When the camera's visible area changes size, the
 * display lays everything out again.
 *
 * <p>Widgets implement the {@code org.flixelgdx.functional} interfaces instead of extending a
 * scene object, so existing tools such as {@link org.flixelgdx.tween.FlixelTween FlixelTween} work
 * on them unchanged. Every image or font parameter takes a
 * {@link org.flixelgdx.file.FlixelFile FlixelFile} rather than a string path. There is no built-in
 * skin; a game supplies its own assets and assembles them into a
 * {@link org.flixelgdx.ui.skin.FlixelUiSkin FlixelUiSkin}.
 *
 * <h2>Packages</h2>
 *
 * <ul>
 *   <li>{@code org.flixelgdx.ui} (this package) holds the display, the pointer, and every widget:
 *       panels, modals, stacks, labels, buttons, checkboxes, radio buttons, dropdowns, sliders,
 *       pictures, and tooltips.</li>
 *   <li>{@code org.flixelgdx.ui.graphics} holds the backgrounds widgets draw behind themselves,
 *       such as nine-slices, stretched images, and color fills.</li>
 *   <li>{@code org.flixelgdx.ui.skin} holds the skin and one style class per widget type, which
 *       together decide what every widget looks like.</li>
 *   <li>{@code org.flixelgdx.ui.text} holds the text box, its editing model, and character
 *       filters.</li>
 * </ul>
 *
 * <h2>You wire the input</h2>
 *
 * <p>The toolkit never reads input itself. A widget exposes plain methods such as
 * {@link org.flixelgdx.ui.FlixelUiWidget#hover() FlixelUiWidget.hover()}, {@link org.flixelgdx.ui.FlixelUiWidget#press() FlixelUiWidget.press()}, and {@code click()} that the game
 * calls after doing its own hit-testing and input mapping; nothing here registers a listener or
 * polls a device. The display only offers geometric helpers such as
 * {@link org.flixelgdx.ui.FlixelUiDisplay#getWidgetAt(float, float) FlixelUiDisplay.getWidgetAt(...)}, which takes a point in the same space that
 * {@code Flixel.mouse.getWorldX(camera)} reports.
 *
 * <h2>The pointer: wiring for most games</h2>
 *
 * <p>Calling each widget's methods by hand gets repetitive fast. Buttons want hover, press, and a
 * click only when the release lands on them; text boxes want caret placement, drag selection, and
 * double clicks; sliders want to follow the cursor while held; dropdowns and tooltips need their
 * own care. {@link org.flixelgdx.ui.FlixelUiPointer FlixelUiPointer} does all of that for you, so most games should start with it.
 * Think of it as a universal remote: instead of walking up to every device and pressing its own
 * buttons, the game reports a few simple things (where the cursor is, when the button goes down,
 * when it comes up) and the pointer turns them into the right call for whatever widget is
 * underneath.
 *
 * <p>The pointer still reads no input. The game tells it where the cursor is and what counts as
 * a press, so the same pointer works for a mouse, a touch screen, or a cursor moved with a
 * gamepad stick:
 *
 * <pre>{@code
 * // In create():
 * pointer = new FlixelUiPointer(ui);
 *
 * // In update():
 * pointer.move(Flixel.mouse.getWorldX(ui.getCamera()), Flixel.mouse.getWorldY(ui.getCamera()));
 * if (Flixel.mouse.justPressed(FlixelMouseButton.LEFT)) pointer.down();
 * if (Flixel.mouse.justReleased(FlixelMouseButton.LEFT)) pointer.up();
 * pointer.scroll(Flixel.mouse.getScrollDeltaY());
 * }</pre>
 *
 * <p>The pointer has no clock, so it cannot tell a double click from two single clicks. A game
 * that wants word and line selection in text boxes counts its own clicks and passes the count to
 * {@link org.flixelgdx.ui.FlixelUiPointer#down(int) FlixelUiPointer.down(...)}. Before treating a click as a click in the game world, check
 * {@link org.flixelgdx.ui.FlixelUiPointer#getHovered() FlixelUiPointer.getHovered()}: it is {@code null} when the cursor is not over the UI, so
 * pressing a button does not also fire the player's weapon.
 *
 * <h2>Wiring widgets by hand</h2>
 *
 * <p>When a game needs something the pointer does not do, it can skip the pointer (or mix it with
 * direct calls) and drive the widgets itself. A minimal mouse wiring by hand looks like this:
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
 * moving focus with {@link org.flixelgdx.ui.FlixelUiWidget#focus() FlixelUiWidget.focus()} and {@link org.flixelgdx.ui.FlixelUiWidget#blur() FlixelUiWidget.blur()} and reading
 * {@link org.flixelgdx.ui.FlixelUiDisplay#getFocused() FlixelUiDisplay.getFocused()}. This keeps the toolkit simple and leaves the game in full
 * control of how mouse, keyboard, or gamepad input drives the interface.
 *
 * @see org.flixelgdx.ui.FlixelUiDisplay
 * @see org.flixelgdx.ui.FlixelUiPointer
 * @see org.flixelgdx.ui.FlixelUiWidget
 * @see org.flixelgdx.ui.skin.FlixelUiSkin
 */
package org.flixelgdx.ui;
