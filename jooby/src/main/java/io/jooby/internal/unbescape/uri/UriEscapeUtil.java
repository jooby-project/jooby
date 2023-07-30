/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.unbescape.uri;

import java.io.UnsupportedEncodingException;

/**
 * Internal class in charge of performing the real escape/unescape operations.
 *
 * @author Daniel Fern&aacute;ndez
 * @since 1.1.0
 */
public final class UriEscapeUtil {

  /*
   * URI ESCAPE/UNESCAPE OPERATIONS
   * ------------------------------
   *
   *   See: http://www.ietf.org/rfc/rfc3986.txt
   *        http://www.w3.org/TR/html401/interact/forms.html#h-17.13.4
   *
   *   Different parts of an URI allow different characters, and therefore require different sets of
   *   characters to be escaped (see RFC3986 for a list of reserved characters for each URI part) - but
   *   the escaping method is always the same: convert the character to the bytes representing it in a
   *   specific encoding (UTF-8 by default) and then percent-encode these bytes with two hexadecimal
   *   digits, like '%0A'.
   *
   *   - PATH:            Part of the URI path, might include several path levels/segments:
   *                      '/admin/users/list?x=1' -> 'users/list'
   *   - PATH SEGMENT:    Part of the URI path, can include only one path level ('/' chars will be escaped):
   *                      '/admin/users/list?x=1' -> 'users'
   *   - QUERY PARAMETER: Names and values of the URI query parameters:
   *                      '/admin/users/list?x=1' -> 'x' (name), '1' (value)
   *   - URI FRAGMENT ID: URI fragments:
   *                      '/admin/users/list?x=1#something' -> '#something'
   *
   */

  public enum UriEscapeType {
    PATH {
      @Override
      public boolean isAllowed(final int c) {
        return isPchar(c) || '/' == c;
      }
    },

    PATH_SEGMENT {
      @Override
      public boolean isAllowed(final int c) {
        return isPchar(c);
      }
    },

    QUERY_PARAM {
      @Override
      public boolean isAllowed(final int c) {
        // We specify these symbols separately because some of them are considered 'pchar'
        if ('=' == c || '&' == c || '+' == c || '#' == c) {
          return false;
        }
        return isPchar(c) || '/' == c || '?' == c;
      }

      @Override
      public boolean canPlusEscapeWhitespace() {
        return true;
      }
    },

    FRAGMENT_ID {
      @Override
      public boolean isAllowed(final int c) {
        return isPchar(c) || '/' == c || '?' == c;
      }
    };

    public abstract boolean isAllowed(final int c);

    /*
     * Determines whether whitespace could appear escaped as '+' in the
     * current escape type.
     *
     * This allows unescaping of application/x-www-form-urlencoded
     * URI query parameters, which specify '+' as escape character
     * for whitespace instead of the '%20' specified by RFC3986.
     *
     * http://www.w3.org/TR/html401/interact/forms.html#h-17.13.4
     * http://www.ietf.org/rfc/rfc3986.txt
     */
    public boolean canPlusEscapeWhitespace() {
      // Will only be true for QUERY_PARAM
      return false;
    }

    /*
     * Specification of 'pchar' according to RFC3986
     * http://www.ietf.org/rfc/rfc3986.txt
     */
    private static boolean isPchar(final int c) {
      return isUnreserved(c) || isSubDelim(c) || ':' == c || '@' == c;
    }

    /*
     * Specification of 'unreserved' according to RFC3986
     * http://www.ietf.org/rfc/rfc3986.txt
     */
    private static boolean isUnreserved(final int c) {
      return isAlpha(c) || isDigit(c) || '-' == c || '.' == c || '_' == c || '~' == c;
    }

    /*
     * Specification of 'reserved' according to RFC3986
     * http://www.ietf.org/rfc/rfc3986.txt
     */
    private static boolean isReserved(final int c) {
      return isGenDelim(c) || isSubDelim(c);
    }

    /*
     * Specification of 'sub-delims' according to RFC3986
     * http://www.ietf.org/rfc/rfc3986.txt
     */
    private static boolean isSubDelim(final int c) {
      return '!' == c || '$' == c || '&' == c || '\'' == c || '(' == c || ')' == c || '*' == c
          || '+' == c || ',' == c || ';' == c || '=' == c;
    }

    /*
     * Specification of 'gen-delims' according to RFC3986
     * http://www.ietf.org/rfc/rfc3986.txt
     */
    private static boolean isGenDelim(final int c) {
      return ':' == c || '/' == c || '?' == c || '#' == c || '[' == c || ']' == c || '@' == c;
    }

    /*
     * Character.isLetter() is not used here because it would include
     * non a-to-z letters.
     */
    static boolean isAlpha(final int c) {
      return c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z';
    }

    /*
     * Character.isDigit() is not used here because it would include
     * non 0-to-9 numbers like i.e. arabic or indian numbers.
     */
    private static boolean isDigit(final int c) {
      return c >= '0' && c <= '9';
    }
  }

  /*
   * Prefixes defined for use in escape and unescape operations
   */
  private static final char ESCAPE_PREFIX = '%';

  /*
   * Small utility char arrays for hexadecimal conversion.
   */
  private static char[] HEXA_CHARS_UPPER = "0123456789ABCDEF".toCharArray();

  private UriEscapeUtil() {}

  static char[] printHexa(final byte b) {
    final char[] result = new char[2];
    result[0] = HEXA_CHARS_UPPER[(b >> 4) & 0xF];
    result[1] = HEXA_CHARS_UPPER[b & 0xF];
    return result;
  }

  /*
   * Perform an escape operation, based on String, according to the specified type.
   */
  public static String escape(
      final String text, final UriEscapeType escapeType, final String encoding) {

    StringBuilder strBuilder = null;

    final int offset = 0;
    final int max = text.length();

    int readOffset = offset;

    for (int i = offset; i < max; i++) {

      final int codepoint = Character.codePointAt(text, i);

      /*
       * Shortcut: most characters will be alphabetic, and we won't need to do anything at
       * all for them. No need to use the complete UriEscapeType check system at all.
       */
      if (UriEscapeType.isAlpha(codepoint)) {
        continue;
      }

      /*
       * Check whether the character is allowed or not
       */
      if (escapeType.isAllowed(codepoint)) {
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
       * Perform the real escape
       *
       * -----------------------------------------------------------------------------------------
       */

      final byte[] charAsBytes;
      try {
        charAsBytes = new String(Character.toChars(codepoint)).getBytes(encoding);
      } catch (final UnsupportedEncodingException e) {
        throw new IllegalArgumentException(
            "Exception while escaping URI: Bad encoding '" + encoding + "'", e);
      }
      for (final byte b : charAsBytes) {
        strBuilder.append('%');
        strBuilder.append(printHexa(b));
      }
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
