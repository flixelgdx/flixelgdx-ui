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
package org.flixelgdx.ui.text;

/**
 * A per-character gate that decides which typed characters a {@link FlixelTextBox} accepts.
 *
 * <p>Think of it as a bouncer at the door: every character must pass inspection before it is
 * admitted into the text box. The built-in constants cover the most common cases, and
 * implementing the interface directly handles anything unusual:
 *
 * <pre>{@code
 * // Accept only uppercase A-Z.
 * FlixelTextBox shout = new FlixelTextBox(200);
 * shout.setFilter(c -> c >= 'A' && c <= 'Z');
 *
 * // Combine with an existing constant.
 * FlixelTextBox hex = new FlixelTextBox(120);
 * hex.setFilter(c -> FlixelTextFilter.DIGITS.accept(c)
 *     || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'));
 *
 * // PIN box: digits only.
 * FlixelTextBox pin = new FlixelTextBox(120);
 * pin.setFilter(FlixelTextFilter.DIGITS);
 * pin.setPasswordChar('*');
 * }</pre>
 *
 * @see FlixelTextBox
 * @see FlixelTextModel#setFilter(FlixelTextFilter)
 */
@FunctionalInterface
public interface FlixelTextFilter {

  /** Accepts every character without restriction. */
  FlixelTextFilter ANY = c -> true;

  /** Accepts only ASCII digit characters ({@code 0} to {@code 9}). */
  FlixelTextFilter DIGITS = c -> c >= '0' && c <= '9';

  /**
   * Accepts characters that form a decimal number: digits, a decimal point ({@code .}), and a
   * minus sign ({@code -}).
   *
   * <p>The filter allows only characters that can appear in a decimal literal; it does not
   * enforce the placement of the point or sign. Use it when the game validates the full string.
   */
  FlixelTextFilter DECIMAL = c -> (c >= '0' && c <= '9') || c == '.' || c == '-';

  /**
   * Accepts Unicode letters, Unicode digits, and the ASCII space character.
   *
   * <p>Suitable for player names and other natural-language fields that should not allow special
   * symbols.
   */
  FlixelTextFilter ALPHANUMERIC = c -> Character.isLetterOrDigit(c) || c == ' ';

  /**
   * Accepts every character except the ASCII space ({@code ' '}).
   *
   * <p>Handy for usernames, passwords, and codes that must not contain whitespace.
   */
  FlixelTextFilter NO_SPACES = c -> c != ' ';

  /**
   * Returns {@code true} when the character is allowed in the text box.
   *
   * <p>Called once per character typed or pasted. Control characters (code points below 32) and
   * newlines are never passed here; {@link FlixelTextBox} rejects them before consulting the
   * filter.
   *
   * @param c The character to test.
   * @return {@code true} to allow it, {@code false} to silently drop it.
   */
  boolean accept(char c);

  /**
   * Returns a composed filter that accepts a character only when both this filter and
   * {@code other} accept it.
   *
   * @param other The second filter to combine with.
   * @return A filter whose {@link #accept(char)} returns {@code true} only when both sources do.
   */
  default FlixelTextFilter and(FlixelTextFilter other) {
    FlixelTextFilter self = this;
    return c -> self.accept(c) && other.accept(c);
  }

  /**
   * Returns a composed filter that accepts a character when either this filter or {@code other}
   * accepts it.
   *
   * @param other The second filter to combine with.
   * @return A filter whose {@link #accept(char)} returns {@code true} when either source does.
   */
  default FlixelTextFilter or(FlixelTextFilter other) {
    FlixelTextFilter self = this;
    return c -> self.accept(c) || other.accept(c);
  }

  /**
   * Returns a filter that inverts this one: it rejects what this accepts, and accepts what this
   * rejects.
   *
   * @return A negated filter.
   */
  default FlixelTextFilter negate() {
    FlixelTextFilter self = this;
    return c -> !self.accept(c);
  }
}
