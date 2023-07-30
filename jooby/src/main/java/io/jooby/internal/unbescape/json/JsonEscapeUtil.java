/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.unbescape.json;

import java.util.Arrays;

/**
 * Internal class in charge of performing the real escape/unescape operations.
 *
 * @author Daniel Fern&aacute;ndez
 * @since 1.0.0
 */
public final class JsonEscapeUtil {

  /*
   * JSON ESCAPE/UNESCAPE OPERATIONS
   * -------------------------------
   *
   *   See: http://www.ietf.org/rfc/rfc4627.txt
   *        http://timelessrepo.com/json-isnt-a-javascript-subset
   *        http://benalman.com/news/2010/03/theres-no-such-thing-as-a-json/
   *
   *   (Note that, in the following examples, and in order to avoid escape problems during the compilation
   *    of this class, the backslash symbol is replaced by '%')
   *
   *   - SINGLE ESCAPE CHARACTERS (SECs):
   *        U+0008 -> %b
   *        U+0009 -> %t
   *        U+000A -> %n
   *        U+000C -> %f
   *        U+000D -> %r
   *        U+0022 -> %"
   *        U+005C -> %%
   *        U+002F -> %/  [ONLY USED WHEN / APPEARS IN </, IN ORDER TO AVOID ISSUES INSIDE <script> TAGS]
   *   - UNICODE ESCAPE [UHEXA]
   *        Characters <= U+FFFF: %u????
   *        Characters > U+FFFF : %u????%u???? (surrogate character pair)
   *                              %u{?*} [NOT USED - Possible syntax for ECMAScript 6]
   *
   *
   *   ------------------------
   *   NOTE: JSON is NOT equivalent to the JavaScript Object Literal notation. JSON is just a format for
   *         data interchange that happens to have a syntax that is ALMOST A SUBSET of the JavaScript Object
   *         Literal notation (U+2028 and U+2029 LineTerminators are allowed in JSON but not in JavaScript).
   *   ------------------------
   *
   */

  /*
   * Prefixes defined for use in escape and unescape operations
   */
  private static final char ESCAPE_PREFIX = '\\';
  private static final char[] ESCAPE_UHEXA_PREFIX = "\\u".toCharArray();

  /*
   * Small utility char arrays for hexadecimal conversion.
   */
  private static char[] HEXA_CHARS_UPPER = "0123456789ABCDEF".toCharArray();

  /*
   * Structures for holding the Single Escape Characters
   */
  private static int SEC_CHARS_LEN = '\\' + 1; // 0x5C + 1 = 0x5D
  private static char SEC_CHARS_NO_SEC = '*';
  private static char[] SEC_CHARS;

  /*
   * Structured for holding the 'escape level' assigned to chars (not codepoints) up to ESCAPE_LEVELS_LEN.
   * - The last position of the ESCAPE_LEVELS array will be used for determining the level of all
   *   codepoints >= (ESCAPE_LEVELS_LEN - 1)
   */
  private static final char ESCAPE_LEVELS_LEN =
      0x9f + 2; // Last relevant char to be indexed is 0x9f
  private static final byte[] ESCAPE_LEVELS;

  static {

    /*
     * Initialize Single Escape Characters
     */
    SEC_CHARS = new char[SEC_CHARS_LEN];
    Arrays.fill(SEC_CHARS, SEC_CHARS_NO_SEC);
    SEC_CHARS[0x08] = 'b';
    SEC_CHARS[0x09] = 't';
    SEC_CHARS[0x0A] = 'n';
    SEC_CHARS[0x0C] = 'f';
    SEC_CHARS[0x0D] = 'r';
    SEC_CHARS[0x22] = '"';
    SEC_CHARS[0x5C] = '\\';
    // slash (solidus) character: will only be escaped if in '</'
    SEC_CHARS[0x2F] = '/';

    /*
     * Initialization of escape levels.
     * Defined levels :
     *
     *    - Level 1 : Basic escape set
     *    - Level 2 : Basic escape set plus all non-ASCII
     *    - Level 3 : All non-alphanumeric characters
     *    - Level 4 : All characters
     *
     */
    ESCAPE_LEVELS = new byte[ESCAPE_LEVELS_LEN];

    /*
     * Everything is level 3 unless contrary indication.
     */
    Arrays.fill(ESCAPE_LEVELS, (byte) 3);

    /*
     * Everything non-ASCII is level 2 unless contrary indication.
     */
    for (char c = 0x80; c < ESCAPE_LEVELS_LEN; c++) {
      ESCAPE_LEVELS[c] = 2;
    }

    /*
     * Alphanumeric characters are level 4.
     */
    for (char c = 'A'; c <= 'Z'; c++) {
      ESCAPE_LEVELS[c] = 4;
    }
    for (char c = 'a'; c <= 'z'; c++) {
      ESCAPE_LEVELS[c] = 4;
    }
    for (char c = '0'; c <= '9'; c++) {
      ESCAPE_LEVELS[c] = 4;
    }

    /*
     * Simple Escape Character will be level 1 (always escaped)
     */
    ESCAPE_LEVELS[0x08] = 1;
    ESCAPE_LEVELS[0x09] = 1;
    ESCAPE_LEVELS[0x0A] = 1;
    ESCAPE_LEVELS[0x0C] = 1;
    ESCAPE_LEVELS[0x0D] = 1;
    ESCAPE_LEVELS[0x22] = 1;
    ESCAPE_LEVELS[0x5C] = 1;
    // slash (solidus) character: will only be escaped if in '</', but we signal it as level 1
    // anyway
    ESCAPE_LEVELS[0x2F] = 1;

    /*
     * Ampersand (&) will be level 1 (always escaped) in order to protect from code injection in XHTML
     */
    ESCAPE_LEVELS[0x26] = 1;

    /*
     * JSON defines one ranges of non-displayable, control characters: U+0000 to U+001F.
     * Additionally, the U+007F to U+009F range is also escaped (which is allowed).
     */
    for (char c = 0x00; c <= 0x1F; c++) {
      ESCAPE_LEVELS[c] = 1;
    }
    for (char c = 0x7F; c <= 0x9F; c++) {
      ESCAPE_LEVELS[c] = 1;
    }
  }

  private JsonEscapeUtil() {
    super();
  }

  static char[] toUHexa(final int codepoint) {
    final char[] result = new char[4];
    result[3] = HEXA_CHARS_UPPER[codepoint % 0x10];
    result[2] = HEXA_CHARS_UPPER[(codepoint >>> 4) % 0x10];
    result[1] = HEXA_CHARS_UPPER[(codepoint >>> 8) % 0x10];
    result[0] = HEXA_CHARS_UPPER[(codepoint >>> 12) % 0x10];
    return result;
  }

  /*
   * Perform an escape operation, based on String, according to the specified level and type.
   */
  public static String escape(
      final String text, final JsonEscapeType escapeType, final JsonEscapeLevel escapeLevel) {

    final int level = escapeLevel.getEscapeLevel();
    final boolean useSECs = escapeType.getUseSECs();

    StringBuilder strBuilder = null;

    final int offset = 0;
    final int max = text.length();

    int readOffset = offset;

    for (int i = offset; i < max; i++) {

      final int codepoint = Character.codePointAt(text, i);

      /*
       * Shortcut: most characters will be ASCII/Alphanumeric, and we won't need to do anything at
       * all for them
       */
      if (codepoint <= (ESCAPE_LEVELS_LEN - 2) && level < ESCAPE_LEVELS[codepoint]) {
        continue;
      }

      /*
       * Check whether the character is a slash (solidus). In such case, only escape if it
       * appears after a '<' ('</') or level >= 3 (non alphanumeric)
       */
      if (codepoint == '/' && level < 3 && (i == offset || text.charAt(i - 1) != '<')) {
        continue;
      }

      /*
       * Shortcut: we might not want to escape non-ASCII chars at all either.
       */
      if (codepoint > (ESCAPE_LEVELS_LEN - 2) && level < ESCAPE_LEVELS[ESCAPE_LEVELS_LEN - 1]) {

        if (Character.charCount(codepoint) > 1) {
          // This is to compensate that we are actually escaping two char[] positions with a single
          // codepoint.
          i++;
        }

        continue;
      }

      /*
       * At this point we know for sure we will need some kind of escape, so we
       * can increase the offset and initialize the string builder if needed, along with
       * copying to it all the contents pending up to this point.
       */

      if (strBuilder == null) {
        strBuilder = new StringBuilder(max + 20);
      }

      if (i - readOffset > 0) {
        strBuilder.append(text, readOffset, i);
      }

      if (Character.charCount(codepoint) > 1) {
        // This is to compensate that we are actually reading two char[] positions with a single
        // codepoint.
        i++;
      }

      readOffset = i + 1;

      /*
       * -----------------------------------------------------------------------------------------
       *
       * Perform the real escape, attending the different combinations of SECs and UHEXA
       *
       * -----------------------------------------------------------------------------------------
       */

      if (useSECs && codepoint < SEC_CHARS_LEN) {
        // We will try to use a SEC

        final char sec = SEC_CHARS[codepoint];

        if (sec != SEC_CHARS_NO_SEC) {
          // SEC found! just write it and go for the next char
          strBuilder.append(ESCAPE_PREFIX);
          strBuilder.append(sec);
          continue;
        }
      }

      /*
       * No SEC-escape was possible, so we need uhexa escape.
       */

      if (Character.charCount(codepoint) > 1) {
        final char[] codepointChars = Character.toChars(codepoint);
        strBuilder.append(ESCAPE_UHEXA_PREFIX);
        strBuilder.append(toUHexa(codepointChars[0]));
        strBuilder.append(ESCAPE_UHEXA_PREFIX);
        strBuilder.append(toUHexa(codepointChars[1]));
        continue;
      }

      strBuilder.append(ESCAPE_UHEXA_PREFIX);
      strBuilder.append(toUHexa(codepoint));
    }

    /*
     * -----------------------------------------------------------------------------------------------
     * Final cleaning: return the original String object if no escape was actually needed. Otherwise
     *                 append the remaining unescaped text to the string builder and return.
     * -----------------------------------------------------------------------------------------------
     */

    if (strBuilder == null) {
      return text;
    }

    if (max - readOffset > 0) {
      strBuilder.append(text, readOffset, max);
    }

    return strBuilder.toString();
  }
}
