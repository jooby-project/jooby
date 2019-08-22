/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.QueryString;
import io.jooby.SneakyThrows;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class UrlParser {
  private static final QueryStringValue NO_QUERY_STRING = new QueryStringValue("");
  private static final char SPACE = 0x20;

  public static QueryString queryString(String queryString) {
    if (queryString == null || queryString.length() == 0) {
      return NO_QUERY_STRING;
    }
    QueryStringValue result = new QueryStringValue("?" + queryString);
    decodeParams(result, queryString, 0, StandardCharsets.UTF_8, 1024);
    return result;
  }

  public static String decodePathSegment(String value) {
    if (value == null || value.length() == 0) {
      return "";
    }
    return decodeComponent(value, 0, value.length(), StandardCharsets.UTF_8, true);
  }

  private static void decodeParams(HashValue root, String s, int from, Charset charset,
      int paramsLimit) {
    int len = s.length();
    if (from >= len) {
      return;
    }
    if (s.charAt(from) == '?') {
      from++;
    }
    int nameStart = from;
    int valueStart = -1;
    int i;
    loop:
    for (i = from; i < len; i++) {
      switch (s.charAt(i)) {
        case '=':
          if (nameStart == i) {
            nameStart = i + 1;
          } else if (valueStart < nameStart) {
            valueStart = i + 1;
          }
          break;
        case '&':
        case ';':
          if (addParam(root, s, nameStart, valueStart, i, charset)) {
            paramsLimit--;
            if (paramsLimit == 0) {
              return;
            }
          }
          nameStart = i + 1;
          break;
        case '#':
          break loop;
        default:
          // continue
      }
    }
    addParam(root, s, nameStart, valueStart, i, charset);
  }

  private static boolean addParam(HashValue root, String s, int nameStart, int valueStart,
      int valueEnd, Charset charset) {
    if (nameStart >= valueEnd) {
      return false;
    }
    if (valueStart <= nameStart) {
      valueStart = valueEnd + 1;
    }
    String name = decodeComponent(s, nameStart, valueStart - 1, charset, false);
    String value = decodeComponent(s, valueStart, valueEnd, charset, false);
    int comma = value.indexOf(',');
    if (comma > 0) {
      root.put(name, Arrays.asList(value.split(",")));
    } else {
      root.put(name, value);
    }
    return true;
  }

  private static String decodeComponent(String s, int from, int toExcluded, Charset charset,
      boolean isPath) {
    int len = toExcluded - from;
    if (len <= 0) {
      return "";
    }
    int firstEscaped = -1;
    for (int i = from; i < toExcluded; i++) {
      char c = s.charAt(i);
      if (c == '%' || c == '+' && !isPath) {
        firstEscaped = i;
        break;
      }
    }
    if (firstEscaped == -1) {
      return s.substring(from, toExcluded);
    }

    CharsetDecoder decoder = charset.newDecoder();

    // Each encoded byte takes 3 characters (e.g. "%20")
    int decodedCapacity = (toExcluded - firstEscaped) / 3;
    ByteBuffer byteBuf = ByteBuffer.allocate(decodedCapacity);
    CharBuffer charBuf = CharBuffer.allocate(decodedCapacity);

    StringBuilder strBuf = new StringBuilder(len);
    strBuf.append(s, from, firstEscaped);

    for (int i = firstEscaped; i < toExcluded; i++) {
      char c = s.charAt(i);
      if (c != '%') {
        strBuf.append(c != '+' || isPath ? c : SPACE);
        continue;
      }

      byteBuf.clear();
      do {
        if (i + 3 > toExcluded) {
          throw new IllegalArgumentException(
              "unterminated escape sequence at index " + i + " of: " + s);
        }
        byteBuf.put(decodeHexByte(s, i + 1));
        i += 3;
      } while (i < toExcluded && s.charAt(i) == '%');
      i--;

      byteBuf.flip();
      charBuf.clear();
      CoderResult result = decoder.reset().decode(byteBuf, charBuf, true);
      try {
        if (!result.isUnderflow()) {
          result.throwException();
        }
        result = decoder.flush(charBuf);
        if (!result.isUnderflow()) {
          result.throwException();
        }
      } catch (CharacterCodingException ex) {
        throw SneakyThrows.propagate(ex);
      }
      strBuf.append(charBuf.flip());
    }
    return strBuf.toString();
  }

  /**
   * Helper to decode half of a hexadecimal number from a string.
   * @param c The ASCII character of the hexadecimal number to decode.
   * Must be in the range {@code [0-9a-fA-F]}.
   * @return The hexadecimal value represented in the ASCII character
   * given, or {@code -1} if the character is invalid.
   */
  private static int decodeHexNibble(final char c) {
    // Character.digit() is not used here, as it addresses a larger
    // set of characters (both ASCII and full-width latin letters).
    if (c >= '0' && c <= '9') {
      return c - '0';
    }
    if (c >= 'A' && c <= 'F') {
      return c - 'A' + 0xA;
    }
    if (c >= 'a' && c <= 'f') {
      return c - 'a' + 0xA;
    }
    return -1;
  }

  /**
   * Decode a 2-digit hex byte from within a string.
   */
  private static byte decodeHexByte(CharSequence s, int pos) {
    int hi = decodeHexNibble(s.charAt(pos));
    int lo = decodeHexNibble(s.charAt(pos + 1));
    if (hi == -1 || lo == -1) {
      throw new IllegalArgumentException(String.format(
          "invalid hex byte '%s' at index %d of '%s'", s.subSequence(pos, pos + 2), pos, s));
    }
    return (byte) ((hi << 4) + lo);
  }
}
