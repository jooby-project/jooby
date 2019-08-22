/**
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

import javax.annotation.Nullable;

/**
 * Set of escaping routines for fixing cross-site scripting (XSS).
 */
public final class XSS {
  private XSS() {
  }

  /**
   * <p>
   *   Perform am URI path <strong>escape</strong> operation
   *   on a <tt>String</tt> input using <tt>UTF-8</tt> as encoding.
   * </p>
   * <p>
   *   The following are the only allowed chars in an URI path (will not be escaped):
   * </p>
   * <ul>
   *   <li><tt>A-Z a-z 0-9</tt></li>
   *   <li><tt>- . _ ~</tt></li>
   *   <li><tt>! $ &amp; ' ( ) * + , ; =</tt></li>
   *   <li><tt>: @</tt></li>
   *   <li><tt>/</tt></li>
   * </ul>
   * <p>
   *   All other chars will be escaped by converting them to the sequence of bytes that
   *   represents them in the <tt>UTF-8</tt> and then representing each byte
   *   in <tt>%HH</tt> syntax, being <tt>HH</tt> the hexadecimal representation of the byte.
   * </p>
   * <p>
   *   This method is <strong>thread-safe</strong>.
   * </p>
   *
   * @param value the <tt>String</tt> to be escaped.
   * @return The escaped result <tt>String</tt>. As a memory-performance improvement, will return the exact
   *         same object as the <tt>text</tt> input argument if no escaping modifications were required (and
   *         no additional <tt>String</tt> objects will be created during processing). Will
   *         return <tt>null</tt> if input is <tt>null</tt>.
   */
  public static @Nullable String uri(@Nullable String value) {
    return UriEscape.escapeUriPath(value);
  }

  /**
   * <p>
   *   Perform an HTML5 level 2 (result is ASCII) <strong>escape</strong> operation on a <tt>String</tt> input.
   * </p>
   * <p>
   *   <em>Level 2</em> means this method will escape:
   * </p>
   * <ul>
   *   <li>The five markup-significant characters: <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>,
   *       <tt>&quot;</tt> and <tt>&#39;</tt></li>
   *   <li>All non ASCII characters.</li>
   * </ul>
   * <p>
   *   This escape will be performed by replacing those chars by the corresponding HTML5 Named Character References
   *   (e.g. <tt>'&amp;acute;'</tt>) when such NCR exists for the replaced character, and replacing by a decimal
   *   character reference (e.g. <tt>'&amp;#8345;'</tt>) when there there is no NCR for the replaced character.
   * </p>
   * <p>
   *   This method is <strong>thread-safe</strong>.
   * </p>
   *
   * @param value the <tt>String</tt> to be escaped.
   * @return The escaped result <tt>String</tt>. As a memory-performance improvement, will return the exact
   *         same object as the <tt>text</tt> input argument if no escaping modifications were required (and
   *         no additional <tt>String</tt> objects will be created during processing). Will
   *         return <tt>null</tt> if input is <tt>null</tt>.
   */
  public static @Nullable String html(@Nullable String value) {
    return HtmlEscape.escapeHtml5(value);
  }

  /**
   * <p>
   *   Perform a JavaScript level 2 (basic set and all non-ASCII chars) <strong>escape</strong> operation
   *   on a <tt>String</tt> input.
   * </p>
   * <p>
   *   <em>Level 2</em> means this method will escape:
   * </p>
   * <ul>
   *   <li>The JavaScript basic escape set:
   *         <ul>
   *           <li>The <em>Single Escape Characters</em>:
   *               <tt>&#92;0</tt> (<tt>U+0000</tt>),
   *               <tt>&#92;b</tt> (<tt>U+0008</tt>),
   *               <tt>&#92;t</tt> (<tt>U+0009</tt>),
   *               <tt>&#92;n</tt> (<tt>U+000A</tt>),
   *               <tt>&#92;v</tt> (<tt>U+000B</tt>),
   *               <tt>&#92;f</tt> (<tt>U+000C</tt>),
   *               <tt>&#92;r</tt> (<tt>U+000D</tt>),
   *               <tt>&#92;&quot;</tt> (<tt>U+0022</tt>),
   *               <tt>&#92;&#39;</tt> (<tt>U+0027</tt>),
   *               <tt>&#92;&#92;</tt> (<tt>U+005C</tt>) and
   *               <tt>&#92;&#47;</tt> (<tt>U+002F</tt>).
   *               Note that <tt>&#92;&#47;</tt> is optional, and will only be used when the <tt>&#47;</tt>
   *               symbol appears after <tt>&lt;</tt>, as in <tt>&lt;&#47;</tt>. This is to avoid accidentally
   *               closing <tt>&lt;script&gt;</tt> tags in HTML. Also, note that <tt>&#92;v</tt>
   *               (<tt>U+000B</tt>) is actually included as a Single Escape
   *               Character in the JavaScript (ECMAScript) specification, but will not be used as it
   *               is not supported by Microsoft Internet Explorer versions &lt; 9.
   *           </li>
   *           <li>
   *               Two ranges of non-displayable, control characters (some of which are already part of the
   *               <em>single escape characters</em> list): <tt>U+0001</tt> to <tt>U+001F</tt> and
   *               <tt>U+007F</tt> to <tt>U+009F</tt>.
   *           </li>
   *         </ul>
   *   </li>
   *   <li>All non ASCII characters.</li>
   * </ul>
   * <p>
   *   This escape will be performed by using the Single Escape Chars whenever possible. For escaped
   *   characters that do not have an associated SEC, default to using <tt>&#92;xFF</tt> Hexadecimal Escapes
   *   if possible (characters &lt;= <tt>U+00FF</tt>), then default to <tt>&#92;uFFFF</tt>
   *   Hexadecimal Escapes. This type of escape <u>produces the smallest escaped string possible</u>.
   * </p>
   * <p>
   *   This method is <strong>thread-safe</strong>.
   * </p>
   *
   * @param value the <tt>String</tt> to be escaped.
   * @return The escaped result <tt>String</tt>. As a memory-performance improvement, will return the exact
   *         same object as the <tt>text</tt> input argument if no escaping modifications were required (and
   *         no additional <tt>String</tt> objects will be created during processing). Will
   *         return <tt>null</tt> if input is <tt>null</tt>.
   */
  public static @Nullable String javaScript(@Nullable String value) {
    return JavaScriptEscape.escapeJavaScript(value);
  }

  /**
   * <p>
   *   Perform a JSON level 2 (basic set and all non-ASCII chars) <strong>escape</strong> operation
   *   on a <tt>String</tt> input.
   * </p>
   * <p>
   *   <em>Level 2</em> means this method will escape:
   * </p>
   * <ul>
   *   <li>The JSON basic escape set:
   *         <ul>
   *           <li>The <em>Single Escape Characters</em>:
   *               <tt>&#92;b</tt> (<tt>U+0008</tt>),
   *               <tt>&#92;t</tt> (<tt>U+0009</tt>),
   *               <tt>&#92;n</tt> (<tt>U+000A</tt>),
   *               <tt>&#92;f</tt> (<tt>U+000C</tt>),
   *               <tt>&#92;r</tt> (<tt>U+000D</tt>),
   *               <tt>&#92;&quot;</tt> (<tt>U+0022</tt>),
   *               <tt>&#92;&#92;</tt> (<tt>U+005C</tt>) and
   *               <tt>&#92;&#47;</tt> (<tt>U+002F</tt>).
   *               Note that <tt>&#92;&#47;</tt> is optional, and will only be used when the <tt>&#47;</tt>
   *               symbol appears after <tt>&lt;</tt>, as in <tt>&lt;&#47;</tt>. This is to avoid accidentally
   *               closing <tt>&lt;script&gt;</tt> tags in HTML.
   *           </li>
   *           <li>
   *               Two ranges of non-displayable, control characters (some of which are already part of the
   *               <em>single escape characters</em> list): <tt>U+0000</tt> to <tt>U+001F</tt> (required
   *               by the JSON spec) and <tt>U+007F</tt> to <tt>U+009F</tt> (additional).
   *           </li>
   *         </ul>
   *   </li>
   *   <li>All non ASCII characters.</li>
   * </ul>
   * <p>
   *   This escape will be performed by using the Single Escape Chars whenever possible. For escaped
   *   characters that do not have an associated SEC, default to <tt>&#92;uFFFF</tt>
   *   Hexadecimal Escapes.
   * </p>
   * <p>
   *   This method is <strong>thread-safe</strong>.
   * </p>
   *
   * @param value the <tt>String</tt> to be escaped.
   * @return The escaped result <tt>String</tt>. As a memory-performance improvement, will return the exact
   *         same object as the <tt>text</tt> input argument if no escaping modifications were required (and
   *         no additional <tt>String</tt> objects will be created during processing). Will
   *         return <tt>null</tt> if input is <tt>null</tt>.
   */
  public static @Nullable String json(@Nullable String value) {
    return JsonEscape.escapeJson(value);
  }

  /**
   * <p>
   *   Perform an XML 1.1 level 2 (markup-significant and all non-ASCII chars) <strong>escape</strong> operation
   *   on a <tt>String</tt> input.
   * </p>
   * <p>
   *   <em>Level 2</em> means this method will escape:
   * </p>
   * <ul>
   *   <li>The five markup-significant characters: <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>,
   *       <tt>&quot;</tt> and <tt>&#39;</tt></li>
   *   <li>All non ASCII characters.</li>
   * </ul>
   * <p>
   *   This escape will be performed by replacing those chars by the corresponding XML Character Entity References
   *   (e.g. <tt>'&amp;lt;'</tt>) when such CER exists for the replaced character, and replacing by a hexadecimal
   *   character reference (e.g. <tt>'&amp;#x2430;'</tt>) when there there is no CER for the replaced character.
   * </p>
   * <p>
   *   This method is <strong>thread-safe</strong>.
   * </p>
   *
   * @param value the <tt>String</tt> to be escaped.
   * @return The escaped result <tt>String</tt>. As a memory-performance improvement, will return the exact
   *         same object as the <tt>text</tt> input argument if no escaping modifications were required (and
   *         no additional <tt>String</tt> objects will be created during processing). Will
   *         return <tt>null</tt> if input is <tt>null</tt>.
   */
  public static @Nullable String xml(@Nullable String value) {
    return XmlEscape.escapeXml11(value);
  }
}
