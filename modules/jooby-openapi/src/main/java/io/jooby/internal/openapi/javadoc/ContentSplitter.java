/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.javadoc;

public class ContentSplitter {

  public record ContentResult(String summary, String description) {}

  public static ContentResult split(String text) {
    if (text == null || text.isEmpty()) {
      return new ContentResult("", "");
    }

    int len = text.length();
    int splitIndex = -1;

    // State trackers
    int parenDepth = 0; // ( )
    int bracketDepth = 0; // [ ]
    int braceDepth = 0; // { }
    boolean inHtmlDef = false; // < ... >
    boolean inCodeBlock = false; // <pre>...</pre> or <code>...</code>

    for (int i = 0; i < len; i++) {
      char c = text.charAt(i);

      // 1. Handle HTML Tags start
      if (c == '<') {
        // Check for <p> (Paragraph Split - Exclusive)
        if (!inCodeBlock && !inHtmlDef && isTag(text, i, "p")) {
          splitIndex = i;
          break;
        }
        // Check for Protected Blocks (<pre>, <code>)
        if (!inCodeBlock && (isTag(text, i, "pre") || isTag(text, i, "code"))) {
          inCodeBlock = true;
        }
        // Check for end of Protected Blocks
        if (inCodeBlock && (isCloseTag(text, i, "pre") || isCloseTag(text, i, "code"))) {
          inCodeBlock = false;
        }
        inHtmlDef = true;
        continue;
      }

      // 2. Handle HTML Tags end
      if (c == '>') {
        inHtmlDef = false;
        continue;
      }

      // 3. Handle Nesting & Split
      if (!inHtmlDef && !inCodeBlock) {
        if (c == '(') {
          parenDepth++;
        } else if (c == ')') {
          if (parenDepth > 0) parenDepth--;
        } else if (c == '[') {
          bracketDepth++;
        } else if (c == ']') {
          if (bracketDepth > 0) bracketDepth--;
        } else if (c == '{') {
          braceDepth++;
        } else if (c == '}') {
          if (braceDepth > 0) braceDepth--;
        }
        // 4. Check for Period
        else if (c == '.') {
          if (parenDepth == 0 && bracketDepth == 0 && braceDepth == 0) {
            splitIndex = i + 1;
            break;
          }
        }
      }
    }

    String summary;
    String description;

    if (splitIndex == -1) {
      summary = text.trim();
      description = "";
    } else {
      summary = text.substring(0, splitIndex).trim();
      description = text.substring(splitIndex).trim();
    }

    // Clean up: Strip <p> tags without using Regex
    return new ContentResult(stripParagraphTags(summary), stripParagraphTags(description));
  }

  /**
   * Removes
   *
   * <p>and tags (and their attributes) from the text. Keeps content inside the tags.
   */
  private static String stripParagraphTags(String text) {
    if (text.isEmpty()) return text;

    StringBuilder sb = new StringBuilder(text.length());
    int len = text.length();

    for (int i = 0; i < len; i++) {
      char c = text.charAt(i);

      if (c == '<') {
        // Detect <p...> or </p...>
        if (isTag(text, i, "p") || isCloseTag(text, i, "p")) {
          // Fast-forward until we find the closing '>'
          while (i < len && text.charAt(i) != '>') {
            i++;
          }
          // We are now at '>', loop increment will move past it
          continue;
        }
      }
      sb.append(c);
    }
    return sb.toString().trim();
  }

  // --- Helper Methods ---

  private static boolean isTag(String text, int i, String tagName) {
    int len = tagName.length();
    if (i + 1 + len > text.length()) return false;

    // Match tagName (case insensitive)
    for (int k = 0; k < len; k++) {
      char c = text.charAt(i + 1 + k);
      if (Character.toLowerCase(c) != tagName.charAt(k)) return false;
    }

    // Check delimiter (must be '>' or whitespace or end of string)
    if (i + 1 + len == text.length()) return true;
    char delimiter = text.charAt(i + 1 + len);
    return delimiter == '>' || Character.isWhitespace(delimiter);
  }

  private static boolean isCloseTag(String text, int i, String tagName) {
    int len = tagName.length();
    if (i + 2 + len > text.length()) return false;
    if (text.charAt(i + 1) != '/') return false;

    // Match tagName (case insensitive)
    for (int k = 0; k < len; k++) {
      char c = text.charAt(i + 2 + k);
      if (Character.toLowerCase(c) != tagName.charAt(k)) return false;
    }

    // Check delimiter
    char delimiter = text.charAt(i + 2 + len);
    return delimiter == '>' || Character.isWhitespace(delimiter);
  }
}
