/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.unbescape.json;

/**
 * Levels defined for JSON escape/unescape operations:
 *
 * <ul>
 *   <li><strong>Level 1</strong>: Escape only the basic escape set. Note the result of a level-1
 *       escape operation might still contain non-ASCII characters if they existed in input, and
 *       therefore you will still need to correctly manage your input/output character encoding
 *       settings. Such <em>basic set</em> consists of:
 *       <ul>
 *         <li>The <em>Single Escape Characters</em>: <tt>&#92;b</tt> (<tt>U+0008</tt>),
 *             <tt>&#92;t</tt> (<tt>U+0009</tt>), <tt>&#92;n</tt> (<tt>U+000A</tt>), <tt>&#92;f</tt>
 *             (<tt>U+000C</tt>), <tt>&#92;r</tt> (<tt>U+000D</tt>), <tt>&#92;&quot;</tt>
 *             (<tt>U+0022</tt>), <tt>&#92;&#92;</tt> (<tt>U+005C</tt>) and <tt>&#92;&#47;</tt>
 *             (<tt>U+002F</tt>). Note that <tt>&#92;&#47;</tt> is optional, and will only be used
 *             when the <tt>&#47;</tt> symbol appears after <tt>&lt;</tt>, as in <tt>&lt;&#47;</tt>.
 *             This is to avoid accidentally closing <tt>&lt;script&gt;</tt> tags in HTML.
 *         <li>The ampersand symbol (<tt>&amp;</tt>, <tt>U+0026</tt>), which will be escaped in
 *             order to protect from code injection in XHTML environments (browsers will parse XHTML
 *             escape codes inside literals in <tt>&lt;script&gt;</tt> tags). Note there is no
 *             <em>Single Escape Character</em> for this symbol, so it will be escaped using the
 *             sequence corresponding to the selected escape type (e.g. <tt>&#92;u0026</tt>).
 *         <li>Two ranges of non-displayable, control characters (some of which are already part of
 *             the <em>single escape characters</em> list): <tt>U+0000</tt> to <tt>U+001F</tt>
 *             (required by the JSON spec) and <tt>U+007F</tt> to <tt>U+009F</tt> (additional).
 *       </ul>
 *   <li><strong>Level 2</strong>: Escape the basic escape set (as defined in level 1), plus all
 *       non-ASCII characters. The result of a level-2 escape operation is therefore always
 *       ASCII-only text, and safer to use in complex scenarios with mixed input/output character
 *       encodings.
 *   <li><strong>Level 3</strong>: Escape all non-alphanumeric characters, this is, all but those in
 *       the <tt>A</tt>-<tt>Z</tt>, <tt>a</tt>-<tt>z</tt> and <tt>0</tt>-<tt>9</tt> ranges. This
 *       level can be safely used for completely escaping texts, including whitespace, line feeds,
 *       punctuation, etc. in scenarios where this adds an extra level of safety.
 *   <li><strong>Level 4</strong>: Escape all characters, even alphanumeric ones.
 * </ul>
 *
 * <p>For further information, see the <em>Glossary</em> and the <em>References</em> sections at the
 * documentation for the {@link JsonEscape} class.
 *
 * @author Daniel Fern&aacute;ndez
 * @since 1.0.0
 */
public enum JsonEscapeLevel {

  /**
   * Level 1 escape: escape only the basic escape set: Single Escape Chars plus non-displayable
   * control chars.
   */
  LEVEL_1_BASIC_ESCAPE_SET(1),

  /**
   * Level 2 escape: escape the basic escape set plus all non-ASCII characters (result will always
   * be ASCII).
   */
  LEVEL_2_ALL_NON_ASCII_PLUS_BASIC_ESCAPE_SET(2),

  /**
   * Level 3 escape: escape all non-alphanumeric characteres (escape all but those in the
   * <tt>A</tt>-<tt>Z</tt>, <tt>a</tt>-<tt>z</tt> and <tt>0</tt>-<tt>9</tt> ranges).
   */
  LEVEL_3_ALL_NON_ALPHANUMERIC(3),

  /** Level 4 escape: escape all characters, including alphanumeric. */
  LEVEL_4_ALL_CHARACTERS(4);

  private final int escapeLevel;

  /**
   * Utility method for obtaining an enum value from its corresponding <tt>int</tt> level value.
   *
   * @param level the level
   * @return the escape level enum constant, or <tt>IllegalArgumentException</tt> if level does not
   *     exist.
   */
  public static JsonEscapeLevel forLevel(final int level) {
    switch (level) {
      case 1:
        return LEVEL_1_BASIC_ESCAPE_SET;
      case 2:
        return LEVEL_2_ALL_NON_ASCII_PLUS_BASIC_ESCAPE_SET;
      case 3:
        return LEVEL_3_ALL_NON_ALPHANUMERIC;
      case 4:
        return LEVEL_4_ALL_CHARACTERS;
      default:
        throw new IllegalArgumentException(
            "No escape level enum constant defined for level: " + level);
    }
  }

  JsonEscapeLevel(final int escapeLevel) {
    this.escapeLevel = escapeLevel;
  }

  /**
   * Return the <tt>int</tt> escape level.
   *
   * @return the escape level.
   */
  public int getEscapeLevel() {
    return this.escapeLevel;
  }
}
