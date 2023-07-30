/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.unbescape.html;

/**
 * Internal class in charge of performing the real escape/unescape operations.
 *
 * @author Daniel Fern&aacute;ndez
 * @since 1.0.0
 */
public final class HtmlEscapeUtil {

  /*
   * GLOSSARY
   * ------------------------
   *
   *   NCR
   *      Named Character Reference or Character Entity Reference: textual
   *      representation of an Unicode codepoint: &aacute;
   *
   *   DCR
   *      Decimal Character Reference: base-10 numerical representation of an Unicode codepoint: &#225;
   *
   *   HCR
   *      Hexadecimal Character Reference: hexadecimal numerical representation of an Unicode codepoint: &#xE1;
   *
   *   Unicode Codepoint
   *      Each of the int values conforming the Unicode code space.
   *      Normally corresponding to a Java char primitive value (codepoint <= \uFFFF),
   *      but might be two chars for codepoints \u10000 to \u10FFFF if the first char is a high
   *      surrogate (\uD800 to \uDBFF) and the second is a low surrogate (\uDC00 to \uDFFF).
   *      See: http://www.oracle.com/technetwork/articles/javase/supplementary-142654.html
   *
   */

  private static final char[] REFERENCE_DECIMAL_PREFIX = "&#".toCharArray();
  private static final char[] REFERENCE_HEXA_PREFIX = "&#x".toCharArray();
  private static final char REFERENCE_SUFFIX = ';';

  private HtmlEscapeUtil() {}

  /*
   * Perform an escape operation, based on String, according to the specified level and type.
   */
  public static String escape(
      final String text, final HtmlEscapeType escapeType, final HtmlEscapeLevel escapeLevel) {

    if (text == null) {
      return null;
    }

    final int level = escapeLevel.getEscapeLevel();
    final boolean useNCRs = escapeType.getUseNCRs();
    final boolean useHexa = escapeType.getUseHexa();

    final HtmlEscapeSymbols symbols = HtmlEscapeSymbols.HTML5_SYMBOLS;

    StringBuilder strBuilder = null;

    final int offset = 0;
    final int max = text.length();

    int readOffset = offset;

    for (int i = offset; i < max; i++) {

      final char c = text.charAt(i);

      /*
       * Shortcut: most characters will be ASCII/Alphanumeric, and we won't need to do anything at
       * all for them
       */
      if (c <= HtmlEscapeSymbols.MAX_ASCII_CHAR && level < symbols.ESCAPE_LEVELS[c]) {
        continue;
      }

      /*
       * Shortcut: we might not want to escape non-ASCII chars at all either.
       */
      if (c > HtmlEscapeSymbols.MAX_ASCII_CHAR
          && level < symbols.ESCAPE_LEVELS[HtmlEscapeSymbols.MAX_ASCII_CHAR + 1]) {
        continue;
      }

      /*
       * Compute the codepoint. This will be used instead of the char for the rest of the process.
       */
      final int codepoint = Character.codePointAt(text, i);

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
        // This is to compensate that we are actually escaping two char[] positions with a single
        // codepoint.
        i++;
      }

      readOffset = i + 1;

      /*
       * -----------------------------------------------------------------------------------------
       *
       * Perform the real escape, attending the different combinations of NCR, DCR and HCR needs.
       *
       * -----------------------------------------------------------------------------------------
       */

      if (useNCRs) {
        // We will try to use an NCR

        if (codepoint < symbols.NCRS_BY_CODEPOINT_LEN) {
          // codepoint < 0x2fff - all HTML4, most HTML5

          final short ncrIndex = symbols.NCRS_BY_CODEPOINT[codepoint];
          if (ncrIndex != symbols.NO_NCR) {
            // There is an NCR for this codepoint!
            strBuilder.append(symbols.SORTED_NCRS[ncrIndex]);
            continue;
          } // else, just let it exit the block and let decimal/hexa escape do its job

        } else if (symbols.NCRS_BY_CODEPOINT_OVERFLOW != null) {
          // codepoint >= 0x2fff. NCR, if exists, will live at the overflow map (if there is one).

          final Short ncrIndex = symbols.NCRS_BY_CODEPOINT_OVERFLOW.get(Integer.valueOf(codepoint));
          if (ncrIndex != null) {
            strBuilder.append(symbols.SORTED_NCRS[ncrIndex.shortValue()]);
            continue;
          } // else, just let it exit the block and let decimal/hexa escape do its job
        }
      }

      /*
       * No NCR-escape was possible (or allowed), so we need decimal/hexa escape.
       */

      if (useHexa) {
        strBuilder.append(REFERENCE_HEXA_PREFIX);
        strBuilder.append(Integer.toHexString(codepoint));
      } else {
        strBuilder.append(REFERENCE_DECIMAL_PREFIX);
        strBuilder.append(codepoint);
      }
      strBuilder.append(REFERENCE_SUFFIX);
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
