/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import org.unbescape.html.HtmlEscape;
import org.unbescape.javascript.JavaScriptEscape;
import org.unbescape.json.JsonEscape;
import org.unbescape.uri.UriEscape;
import org.unbescape.xml.XmlEscape;

import edu.umd.cs.findbugs.annotations.Nullable;

/** Set of escaping routines for fixing cross-site scripting (XSS). */
public final class XSS {
  private XSS() {}

  /**
   * Perform am URI path <strong>escape</strong> operation on a <code>String</code> input using
   * <code>UTF-8</code> as encoding.
   *
   * <p>The following are the only allowed chars in an URI path (will not be escaped):
   *
   * <ul>
   *   <li><code>A-Z a-z 0-9</code>
   *   <li><code>- . _ ~</code>
   *   <li><code>! $ &amp; ' ( ) * + , ; =</code>
   *   <li><code>: @</code>
   *   <li><code>/</code>
   * </ul>
   *
   * <p>All other chars will be escaped by converting them to the sequence of bytes that represents
   * them in the <code>UTF-8</code> and then representing each byte in <code>%HH</code> syntax,
   * being <code>HH</code> the hexadecimal representation of the byte.
   *
   * <p>This method is <strong>thread-safe</strong>.
   *
   * @param value the <code>String</code> to be escaped.
   * @return The escaped result <code>String</code>. As a memory-performance improvement, will
   *     return the exact same object as the <code>text</code> input argument if no escaping
   *     modifications were required (and no additional <code>String</code> objects will be created
   *     during processing). Will return <code>null</code> if input is <code>null</code>.
   */
  public static @Nullable String uri(@Nullable String value) {
    return UriEscape.escapeUriPath(value);
  }

  /**
   * Perform an HTML5 level 2 (result is ASCII) <strong>escape</strong> operation on a <code>String
   * </code> input.
   *
   * <p><em>Level 2</em> means this method will escape:
   *
   * <ul>
   *   <li>The five markup-significant characters: <code>&lt;</code>, <code>&gt;</code>, <code>&amp;
   *       </code>, <code>&quot;</code> and <code>&#39;</code>
   *   <li>All non ASCII characters.
   * </ul>
   *
   * <p>This escape will be performed by replacing those chars by the corresponding HTML5 Named
   * Character References (e.g. <code>'&amp;acute;'</code>) when such NCR exists for the replaced
   * character, and replacing by a decimal character reference (e.g. <code>'&amp;#8345;'</code>)
   * when there there is no NCR for the replaced character.
   *
   * <p>This method is <strong>thread-safe</strong>.
   *
   * @param value the <code>String</code> to be escaped.
   * @return The escaped result <code>String</code>. As a memory-performance improvement, will
   *     return the exact same object as the <code>text</code> input argument if no escaping
   *     modifications were required (and no additional <code>String</code> objects will be created
   *     during processing). Will return <code>null</code> if input is <code>null</code>.
   */
  public static @Nullable String html(@Nullable String value) {
    return HtmlEscape.escapeHtml5(value);
  }

  /**
   * Perform a JavaScript level 2 (basic set and all non-ASCII chars) <strong>escape</strong>
   * operation on a <code>String</code> input.
   *
   * <p><em>Level 2</em> means this method will escape:
   *
   * <ul>
   *   <li>The JavaScript basic escape set:
   *       <ul>
   *         <li>The <em>Single Escape Characters</em>: <code>&#92;0</code> (<code>U+0000</code>),
   *             <code>&#92;b</code> (<code>U+0008</code>), <code>&#92;t</code> (<code>U+0009</code>
   *             ), <code>&#92;n</code> (<code>U+000A</code>), <code>&#92;v</code> (<code>U+000B
   *             </code>), <code>&#92;f</code> (<code>U+000C</code>), <code>&#92;r</code> (<code>
   *             U+000D</code>), <code>&#92;&quot;</code> (<code>U+0022</code>), <code>&#92;&#39;
   *             </code> (<code>U+0027</code>), <code>&#92;&#92;</code> (<code>U+005C</code>) and
   *             <code>&#92;&#47;</code> (<code>U+002F</code>). Note that <code>&#92;&#47;</code> is
   *             optional, and will only be used when the <code>&#47;</code> symbol appears after
   *             <code>&lt;</code>, as in <code>&lt;&#47;</code>. This is to avoid accidentally
   *             closing <code>&lt;script&gt;</code> tags in HTML. Also, note that <code>&#92;v
   *             </code> (<code>U+000B</code>) is actually included as a Single Escape Character in
   *             the JavaScript (ECMAScript) specification, but will not be used as it is not
   *             supported by Microsoft Internet Explorer versions &lt; 9.
   *         <li>Two ranges of non-displayable, control characters (some of which are already part
   *             of the <em>single escape characters</em> list): <code>U+0001</code> to <code>U+001F
   *             </code> and <code>U+007F</code> to <code>U+009F</code>.
   *       </ul>
   *   <li>All non ASCII characters.
   * </ul>
   *
   * <p>This escape will be performed by using the Single Escape Chars whenever possible. For
   * escaped characters that do not have an associated SEC, default to using <code>&#92;xFF</code>
   * Hexadecimal Escapes if possible (characters &lt;= <code>U+00FF</code>), then default to <code>
   * &#92;uFFFF</code> Hexadecimal Escapes. This type of escape <u>produces the smallest escaped
   * string possible</u>.
   *
   * <p>This method is <strong>thread-safe</strong>.
   *
   * @param value the <code>String</code> to be escaped.
   * @return The escaped result <code>String</code>. As a memory-performance improvement, will
   *     return the exact same object as the <code>text</code> input argument if no escaping
   *     modifications were required (and no additional <code>String</code> objects will be created
   *     during processing). Will return <code>null</code> if input is <code>null</code>.
   */
  public static @Nullable String javaScript(@Nullable String value) {
    return JavaScriptEscape.escapeJavaScript(value);
  }

  /**
   * Perform a JSON level 2 (basic set and all non-ASCII chars) <strong>escape</strong> operation on
   * a <code>String</code> input.
   *
   * <p><em>Level 2</em> means this method will escape:
   *
   * <ul>
   *   <li>The JSON basic escape set:
   *       <ul>
   *         <li>The <em>Single Escape Characters</em>: <code>&#92;b</code> (<code>U+0008</code>),
   *             <code>&#92;t</code> (<code>U+0009</code>), <code>&#92;n</code> (<code>U+000A</code>
   *             ), <code>&#92;f</code> (<code>U+000C</code>), <code>&#92;r</code> (<code>U+000D
   *             </code>), <code>&#92;&quot;</code> (<code>U+0022</code>), <code>&#92;&#92;</code> (
   *             <code>U+005C</code>) and <code>&#92;&#47;</code> (<code>U+002F</code>). Note that
   *             <code>&#92;&#47;</code> is optional, and will only be used when the <code>&#47;
   *             </code> symbol appears after <code>&lt;</code>, as in <code>&lt;&#47;</code>. This
   *             is to avoid accidentally closing <code>&lt;script&gt;</code> tags in HTML.
   *         <li>Two ranges of non-displayable, control characters (some of which are already part
   *             of the <em>single escape characters</em> list): <code>U+0000</code> to <code>U+001F
   *             </code> (required by the JSON spec) and <code>U+007F</code> to <code>U+009F</code>
   *             (additional).
   *       </ul>
   *   <li>All non ASCII characters.
   * </ul>
   *
   * <p>This escape will be performed by using the Single Escape Chars whenever possible. For
   * escaped characters that do not have an associated SEC, default to <code>&#92;uFFFF</code>
   * Hexadecimal Escapes.
   *
   * <p>This method is <strong>thread-safe</strong>.
   *
   * @param value the <code>String</code> to be escaped.
   * @return The escaped result <code>String</code>. As a memory-performance improvement, will
   *     return the exact same object as the <code>text</code> input argument if no escaping
   *     modifications were required (and no additional <code>String</code> objects will be created
   *     during processing). Will return <code>null</code> if input is <code>null</code>.
   */
  public static @Nullable String json(@Nullable String value) {
    return JsonEscape.escapeJson(value);
  }

  /**
   * Perform an XML 1.1 level 2 (markup-significant and all non-ASCII chars) <strong>escape</strong>
   * operation on a <code>String</code> input.
   *
   * <p><em>Level 2</em> means this method will escape:
   *
   * <ul>
   *   <li>The five markup-significant characters: <code>&lt;</code>, <code>&gt;</code>, <code>&amp;
   *       </code>, <code>&quot;</code> and <code>&#39;</code>
   *   <li>All non ASCII characters.
   * </ul>
   *
   * <p>This escape will be performed by replacing those chars by the corresponding XML Character
   * Entity References (e.g. <code>'&amp;lt;'</code>) when such CER exists for the replaced
   * character, and replacing by a hexadecimal character reference (e.g. <code>'&amp;#x2430;'</code>
   * ) when there there is no CER for the replaced character.
   *
   * <p>This method is <strong>thread-safe</strong>.
   *
   * @param value the <code>String</code> to be escaped.
   * @return The escaped result <code>String</code>. As a memory-performance improvement, will
   *     return the exact same object as the <code>text</code> input argument if no escaping
   *     modifications were required (and no additional <code>String</code> objects will be created
   *     during processing). Will return <code>null</code> if input is <code>null</code>.
   */
  public static @Nullable String xml(@Nullable String value) {
    return XmlEscape.escapeXml11(value);
  }
}
