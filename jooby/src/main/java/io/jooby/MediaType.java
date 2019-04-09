/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BinaryOperator;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Implementation of media/content type.
 *
 * @since 2.0.0
 */
public final class MediaType implements Comparable<MediaType> {

  /**
   * Computes and returns the most specific media type of both.
   */
  public static final BinaryOperator<MediaType> MOST_SPECIFIC = (a, b) -> {
    int aScore = a.getScore();
    int bScore = b.getScore();
    return aScore >= bScore ? a : b;
  };

  /** APPLICATION_JSON. */
  public static final String JSON = "application/json";

  /** APPLICATION_XML. */
  public static final String XML = "application/xml";

  /** TEXT_PLAIN. */
  public static final String TEXT = "text/plain";

  /** TEXT_HTML. */
  public static final String HTML = "text/html";

  /** APPLICATION_JAVASCRIPT. */
  public static final String JS = "application/javascript";

  /** TEXT_CSS. */
  public static final String CSS = "text/css";

  /** APPLICATION_OCTET_STREAM. */
  public static final String OCTET_STREAM = "application/octet-stream";

  /** FORM_URLENCODED. */
  public static final String FORM_URLENCODED = "application/x-www-form-urlencoded";

  /** MULTIPART_FORM_DATA. */
  public static final String MULTIPART_FORMDATA = "multipart/form-data";

  /** ALL. */
  public static final String ALL = "*/*";

  /** APPLICATION_JSON. */
  public static final MediaType json = new MediaType(JSON, UTF_8);

  /** APPLICATION_XML. */
  public static final MediaType xml = new MediaType(XML, UTF_8);

  /** TEXT_PLAIN. */
  public static final MediaType text = new MediaType(TEXT, UTF_8);

  /** TEXT_HTML. */
  public static final MediaType html = new MediaType(HTML, UTF_8);

  /** APPLICATION_JSON. */
  public static final MediaType js = new MediaType(JS, UTF_8);

  /** TEXT_CSS. */
  public static final MediaType css = new MediaType(CSS, UTF_8);

  /** APPLICATION_OCTET_STREAM. */
  public static final MediaType octetStream = new MediaType(OCTET_STREAM, null);

  /** FORM_URLENCODED. */
  public static final MediaType formUrlencoded = new MediaType(FORM_URLENCODED, UTF_8);

  /** MULTIPART_FORM_DATA. */
  public static final MediaType multipartFormdata = new MediaType(MULTIPART_FORMDATA, UTF_8);

  /** ALL. */
  public static final MediaType all = new MediaType(ALL, null);

  private final String raw;

  private final Charset charset;

  private final int subtypeStart;

  private final int subtypeEnd;

  private final String value;

  private MediaType(@Nonnull String value, Charset charset) {
    this.raw = value;
    this.subtypeStart = value.indexOf('/');
    if (subtypeStart < 0) {
      throw new IllegalArgumentException("Invalid media type: " + value);
    }
    int subtypeEnd = value.indexOf(';');
    if (subtypeEnd < 0) {
      this.value = raw;
      this.subtypeEnd = value.length();
    } else {
      this.value = raw.substring(0, subtypeEnd);
      this.subtypeEnd = subtypeEnd;
    }
    this.charset = charset;
  }

  @Override public boolean equals(Object obj) {
    if (obj instanceof MediaType) {
      MediaType that = (MediaType) obj;
      return getType().equals(that.getType()) && getSubtype().equals(that.getSubtype());
    }
    return false;
  }

  @Override public int hashCode() {
    return value.hashCode();
  }

  /**
   * Get a parameter that matches the given name or <code>null</code>.
   *
   * @param name Parameter name.
   * @return Parameter value or <code>null</code>.
   */
  public @Nullable String getParameter(@Nonnull String name) {
    int paramStart = subtypeEnd + 1;
    for (int i = subtypeEnd; i < raw.length(); i++) {
      char ch = raw.charAt(i);
      if (ch == '=') {
        String pname = raw.substring(paramStart, i).trim();
        int paramValueEnd = raw.indexOf(';', i);
        if (paramValueEnd < 0) {
          paramValueEnd = raw.length();
        }
        if (pname.equals(name)) {
          return raw.substring(i + 1, paramValueEnd).trim();
        }
        paramStart = paramValueEnd + 1;
        i = paramStart;
      }
    }
    return null;
  }

  /**
   * Media type value without parameters.
   *
   * @return Media type value.
   */
  public @Nonnull String getValue() {
    return value;
  }

  /**
   * Render a content type header and add the charset parameter (when present).
   *
   * @param charset Charset.
   * @return Content type header.
   */
  public @Nonnull String toContentTypeHeader(@Nullable Charset charset) {
    if (charset == null) {
      Charset paramCharset = getCharset();
      if (paramCharset == null) {
        return value;
      }
      charset = paramCharset;
    }
    return value + ";charset=" + charset.name();
  }

  /**
   * Value of <code>q</code> parameter.
   *
   * @return Value of <code>q</code> parameter.
   */
  @Nonnull public float getQuality() {
    String q = getParameter("q");
    return q == null ? 1f : Float.parseFloat(q);
  }

  @Override public int compareTo(MediaType other) {
    if (this == other) {
      return 0;
    }
    int diff = other.getScore() - getScore();
    if (diff == 0) {
      diff = Float.compare(other.getQuality(), getQuality());
      if (diff == 0) {
        diff = other.getParameterCount() - getParameterCount();
      }
    }
    return diff;
  }

  /**
   * Indicates whenever this is a textual mediatype.
   * @return True for textual mediatype.
   */
  public boolean isTextual() {
    if (getType().equals("text")) {
      return true;
    }
    String subtype = getSubtype();
    return subtype.endsWith("json") || subtype.endsWith("javascript") || subtype.endsWith("xml");
  }

  /**
   * Indicates whenever this is a json mediatype.
   * @return True for json mediatype.
   */
  public boolean isJson() {
    String subtype = getSubtype();
    return subtype.equals("json") || subtype.endsWith("+json");
  }

  /**
   * Charset or <code>null</code>.
   *
   * @return Charset or <code>null</code>.
   */
  public @Nullable Charset getCharset() {
    Charset charset = charset(this.charset);
    if (charset == null && isTextual()) {
      return UTF_8;
    }
    return charset;
  }

  private Charset charset(Charset charset) {
    String charsetName = getParameter("charset");
    return charsetName == null ? charset : Charset.forName(charsetName);
  }

  /**
   * Type segment of mediatype (leading type).
   *
   * @return Type segment of mediatype (leading type).
   */
  public @Nonnull String getType() {
    return raw.substring(0, subtypeStart).trim();
  }

  /**
   * Subtype segment of mediatype (trailing type).
   *
   * @return Subtype segment of mediatype (trailing type).
   */
  public @Nonnull String getSubtype() {
    return raw.substring(subtypeStart + 1, subtypeEnd).trim();
  }

  /**
   * True if this mediatype is compatible with the given content type.
   *
   * @param mediaType Media type to test.
   * @return True if this mediatype is compatible with the given content type.
   */
  public boolean matches(@Nonnull String mediaType) {
    return matches(value, mediaType);
  }

  /**
   * True if this mediatype is compatible with the given content type.
   *
   * @param type Media type to test.
   * @return True if this mediatype is compatible with the given content type.
   */
  public boolean matches(@Nonnull MediaType type) {
    return matches(value, type.value);
  }

  private int getScore() {
    int precendence = 0;
    if (!getType().equals("*")) {
      precendence += 1;
    }
    if (!getSubtype().equals("*")) {
      precendence += 1;
    }
    return precendence;
  }

  private int getParameterCount() {
    int p = 0;
    for (int i = subtypeEnd; i < raw.length(); i++) {
      char ch = raw.charAt(i);
      if (ch == '=') {
        p += 1;
      }
    }
    return p;
  }

  /**
   * Parse a string value into a media-type.
   *
   * @param value String media-type.
   * @return Media type.
   */
  public static @Nonnull MediaType valueOf(@Nonnull String value) {
    if (value == null || value.length() == 0 || value.equals("*")) {
      return all;
    }
    if (HTML.equalsIgnoreCase(value)) {
      return html;
    }
    if (TEXT.equalsIgnoreCase(value)) {
      return text;
    }
    if (JSON.equalsIgnoreCase(value)) {
      return json;
    }
    if (JS.equalsIgnoreCase(value)) {
      return js;
    }
    if (CSS.equalsIgnoreCase(value)) {
      return css;
    }
    if (FORM_URLENCODED.equalsIgnoreCase(value)) {
      return formUrlencoded;
    }
    if (MULTIPART_FORMDATA.equalsIgnoreCase(value)) {
      return multipartFormdata;
    }
    if (OCTET_STREAM.equalsIgnoreCase(value)) {
      return octetStream;
    }
    if (XML.equalsIgnoreCase(value)) {
      return xml;
    }
    return new MediaType(value, null);
  }

  /**
   * Parse one or more mediatype values. Mediatype must be separated by comma <code>,</code>.
   *
   * @param value Mediatype comma separated value.
   * @return One or more mediatypes.
   */
  public static @Nonnull List<MediaType> parse(@Nullable String value) {
    if (value == null || value.length() == 0) {
      return Collections.emptyList();
    }
    List<MediaType> result = new ArrayList<>(3);
    int typeStart = 0;
    int len = value.length();
    for (int i = 0; i < len; i++) {
      char ch = value.charAt(i);
      if (ch == ',') {
        result.add(valueOf(value.substring(typeStart, i).trim()));
        typeStart = i + 1;
      }
    }
    if (typeStart == 0) {
      result.add(valueOf(value));
    } else if (typeStart < len) {
      result.add(valueOf(value.substring(typeStart, len).trim()));
    }
    return result;
  }

  static boolean matches(@Nonnull String expected, @Nonnull String contentType) {
    int start = 0;
    int len1 = expected.length();
    int end = contentType.indexOf(',');
    while (end != -1) {
      if (matchOne(expected, len1, contentType.substring(start, end).trim())) {
        return true;
      }
      start = end + 1;
      end = contentType.indexOf(',', start);
    }
    int clen = contentType.length();
    if (start < clen) {
      return matchOne(expected, len1, contentType.substring(start, clen).trim());
    }
    return false;
  }

  /**
   * Mediatype by file extension.
   *
   * @param file File.
   * @return Mediatype.
   */
  public static @Nonnull MediaType byFile(@Nonnull File file) {
    return byFile(file.getName());
  }

  /**
   * Mediatype by file extension.
   *
   * @param file File.
   * @return Mediatype.
   */
  public static @Nonnull MediaType byFile(@Nonnull Path file) {
    return byFile(file.getFileName().toString());
  }

  /**
   * Mediatype by file extension.
   *
   * @param filename File.
   * @return Mediatype.
   */
  public static @Nonnull MediaType byFile(@Nonnull String filename) {
    int index = filename.lastIndexOf('.');
    return index > 0 ? byFileExtension(filename.substring(index + 1)) : octetStream;
  }

  /**
   * Mediatype by file extension.
   *
   * @param ext File extension.
   * @return Mediatype.
   */
  public static @Nonnull MediaType byFileExtension(@Nonnull String ext) {
    switch (ext) {
      case "spl":
        return new MediaType("application/x-futuresplash", null);
      case "java":
        return text;
      case "class":
        return new MediaType("application/java-vm", null);
      case "cpt":
        return new MediaType("application/mac-compactpro", null);
      case "etx":
        return new MediaType("text/x-setext", null);
      case "tar":
        return new MediaType("application/x-tar", null);
      case "js":
        return js;
      case "ogg":
        return new MediaType("application/ogg", null);
      case "xyz":
        return new MediaType("chemical/x-xyz", null);
      case "msh":
        return new MediaType("model/mesh", null);
      case "ustar":
        return new MediaType("application/x-ustar", null);
      case "msi":
        return octetStream;
      case "xht":
        return new MediaType("application/xhtml+xml", UTF_8);
      case "bmp":
        return new MediaType("image/bmp", null);
      case "silo":
        return new MediaType("model/mesh", null);
      case "sv4crc":
        return new MediaType("application/x-sv4crc", null);
      case "man":
        return new MediaType("application/x-troff-man", null);
      case "map":
        return text;
      case "cpio":
        return new MediaType("application/x-cpio", null);
      case "snd":
        return new MediaType("audio/basic", null);
      case "iges":
        return new MediaType("model/iges", null);
      case "smi":
        return new MediaType("application/smil", null);
      case "bcpio":
        return new MediaType("application/x-bcpio", null);
      case "pgm":
        return new MediaType("image/x-portable-graymap", null);
      case "pgn":
        return new MediaType("application/x-chess-pgn", null);
      case "vcd":
        return new MediaType("application/x-cdlink", null);
      case "aif":
        return new MediaType("audio/x-aiff", null);
      case "ods":
        return new MediaType("application/vnd.oasis.opendocument.spreadsheet", null);
      case "odt":
        return new MediaType("application/vnd.oasis.opendocument.text", null);
      case "odp":
        return new MediaType("application/vnd.oasis.opendocument.presentation", null);
      case "jpeg":
        return new MediaType("image/jpeg", null);
      case "xwd":
        return new MediaType("image/x-xwindowdump", null);
      case "odc":
        return new MediaType("application/vnd.oasis.opendocument.chart", null);
      case "ots":
        return new MediaType("application/vnd.oasis.opendocument.spreadsheet-template", null);
      case "ott":
        return new MediaType("application/vnd.oasis.opendocument.text-template", null);
      case "odf":
        return new MediaType("application/vnd.oasis.opendocument.formula", null);
      case "otp":
        return new MediaType("application/vnd.oasis.opendocument.presentation-template", null);
      case "oda":
        return new MediaType("application/oda", null);
      case "odb":
        return new MediaType("application/vnd.oasis.opendocument.database", null);
      case "less":
        return css;
      case "doc":
        return new MediaType("application/msword", null);
      case "odm":
        return new MediaType("application/vnd.oasis.opendocument.text-master", null);
      case "odg":
        return new MediaType("application/vnd.oasis.opendocument.graphics", null);
      case "woff":
        return new MediaType("application/x-font-woff", null);
      case "odi":
        return new MediaType("application/vnd.oasis.opendocument.image", null);
      case "otc":
        return new MediaType("application/vnd.oasis.opendocument.chart-template", null);
      case "otf":
        return new MediaType("font/opentype", null);
      case "zip":
        return new MediaType("application/zip", null);
      case "skt":
        return new MediaType("application/x-koan", null);
      case "eps":
        return new MediaType("application/postscript", null);
      case "mpe":
        return new MediaType("video/mpeg", null);
      case "otg":
        return new MediaType("application/vnd.oasis.opendocument.graphics-template", null);
      case "oth":
        return new MediaType("application/vnd.oasis.opendocument.text-web", null);
      case "oti":
        return new MediaType("application/vnd.oasis.opendocument.image-template", null);
      case "mpg":
        return new MediaType("video/mpeg", null);
      case "ps":
        return new MediaType("application/postscript", null);
      case "xul":
        return new MediaType("application/vnd.mozilla.xul+xml", UTF_8);
      case "xslt":
        return new MediaType("application/xslt+xml", UTF_8);
      case "dms":
        return octetStream;
      case "mol":
        return new MediaType("chemical/x-mdl-molfile", null);
      case "eot":
        return new MediaType("application/vnd.ms-fontobject", null);
      case "skd":
        return new MediaType("application/x-koan", null);
      case "wmlsc":
        return new MediaType("application/vnd.wap.wmlscriptc", null);
      case "roff":
        return new MediaType("application/x-troff", null);
      case "skp":
        return new MediaType("application/x-koan", null);
      case "mpga":
        return new MediaType("audio/mpeg", null);
      case "mov":
        return new MediaType("video/quicktime", null);
      case "igs":
        return new MediaType("model/iges", null);
      case "skm":
        return new MediaType("application/x-koan", null);
      case "sv4cpio":
        return new MediaType("application/x-sv4cpio", null);
      case "wbmp":
        return new MediaType("image/vnd.wap.wbmp", null);
      case "bin":
        return new MediaType("application/octet-stream", null);
      case "z":
        return new MediaType("application/compress", null);
      case "html":
        return html;
      case "gtar":
        return new MediaType("application/x-gtar", null);
      case "pdb":
        return new MediaType("chemical/x-pdb", null);
      case "t":
        return new MediaType("application/x-troff", null);
      case "mp2":
        return new MediaType("audio/mpeg", null);
      case "mp3":
        return new MediaType("audio/mpeg", null);
      case "ms":
        return new MediaType("application/x-troff-ms", null);
      case "wrl":
        return new MediaType("model/vrml", null);
      case "mp4":
        return new MediaType("video/mp4", null);
      case "vxml":
        return new MediaType("application/voicexml+xml", UTF_8);
      case "mathml":
        return new MediaType("application/mathml+xml", UTF_8);
      case "hdf":
        return new MediaType("application/x-hdf", null);
      case "wav":
        return new MediaType("audio/x-wav", null);
      case "pdf":
        return new MediaType("application/pdf", null);
      case "nc":
        return new MediaType("application/x-netcdf", null);
      case "sit":
        return new MediaType("application/x-stuffit", null);
      case "htm":
        return html;
      case "jnlp":
        return new MediaType("application/x-java-jnlp-file", null);
      case "dll":
        return new MediaType("application/x-msdownload", null);
      case "xsl":
        return xml;
      case "ief":
        return new MediaType("image/ief", null);
      case "rgb":
        return new MediaType("image/x-rgb", null);
      case "htc":
        return new MediaType("text/x-component", null);
      case "avi":
        return new MediaType("video/x-msvideo", null);
      case "me":
        return new MediaType("application/x-troff-me", null);
      case "tiff":
        return new MediaType("image/tiff", null);
      case "pbm":
        return new MediaType("image/x-portable-bitmap", null);
      case "xsd":
        return xml;
      case "mesh":
        return new MediaType("model/mesh", null);
      case "xbm":
        return new MediaType("image/x-xbitmap", null);
      case "midi":
        return new MediaType("audio/midi", null);
      case "texi":
        return new MediaType("application/x-texinfo", null);
      case "conf":
        return new MediaType("application/hocon", UTF_8);
      case "lzh":
        return new MediaType("application/octet-stream", null);
      case "tr":
        return new MediaType("application/x-troff", null);
      case "ts":
        return js;
      case "hqx":
        return new MediaType("application/mac-binhex40", null);
      case "tif":
        return new MediaType("image/tiff", null);
      case "ice":
        return new MediaType("x-conference/x-cooltalk", null);
      case "dir":
        return new MediaType("application/x-director", null);
      case "sgm":
        return new MediaType("text/sgml", null);
      case "woff2":
        return new MediaType("application/font-woff2", null);
      case "sh":
        return new MediaType("application/x-sh", null);
      case "ico":
        return new MediaType("image/x-icon", null);
      case "asx":
        return new MediaType("video/x.ms.asx", null);
      case "swf":
        return new MediaType("application/x-shockwave-flash", null);
      case "texinfo":
        return new MediaType("application/x-texinfo", null);
      case "ai":
        return new MediaType("application/postscript", null);
      case "txt":
        return text;
      case "asc":
        return text;
      case "ppm":
        return new MediaType("image/x-portable-pixmap", null);
      case "rtx":
        return new MediaType("text/richtext", UTF_8);
      case "movie":
        return new MediaType("video/x-sgi-movie", null);
      case "ra":
        return new MediaType("audio/x-pn-realaudio", null);
      case "vrml":
        return new MediaType("model/vrml", null);
      case "au":
        return new MediaType("audio/basic", null);
      case "gzip":
        return new MediaType("application/gzip", null);
      case "pps":
        return new MediaType("application/vnd.ms-powerpoint", null);
      case "rdf":
        return new MediaType("application/rdf+xml", UTF_8);
      case "ppt":
        return new MediaType("application/vnd.ms-powerpoint", null);
      case "asf":
        return new MediaType("video/x.ms.asf", null);
      case "xpm":
        return new MediaType("image/x-xpixmap", null);
      case "dxr":
        return new MediaType("application/x-director", null);
      case "ser":
        return new MediaType("application/java-serialized-object", null);
      case "rm":
        return new MediaType("audio/x-pn-realaudio", null);
      case "tgz":
        return new MediaType("application/x-gtar", null);
      case "rv":
        return new MediaType("video/vnd.rn-realvideo", null);
      case "shar":
        return new MediaType("application/x-shar", null);
      case "rtf":
        return new MediaType("application/rtf", null);
      case "svg":
        return new MediaType("image/svg+xml", null);
      case "lha":
        return new MediaType("application/octet-stream", null);
      case "mif":
        return new MediaType("application/vnd.mif", null);
      case "mpeg":
        return new MediaType("video/mpeg", null);
      case "wml":
        return new MediaType("text/vnd.wap.wml", null);
      case "jsp":
        return html;
      case "mid":
        return new MediaType("audio/midi", null);
      case "qt":
        return new MediaType("video/quicktime", null);
      case "yaml":
        return new MediaType("application/yaml", UTF_8);
      case "pnm":
        return new MediaType("image/x-portable-anymap", null);
      case "tar.gz":
        return new MediaType("application/x-gtar", null);
      case "gz":
        return new MediaType("application/gzip", null);
      case "ram":
        return new MediaType("audio/x-pn-realaudio", null);
      case "jar":
        return new MediaType("application/java-archive", null);
      case "apk":
        return new MediaType("application/vnd.android.package-archive", null);
      case "tex":
        return new MediaType("application/x-tex", null);
      case "png":
        return new MediaType("image/png", null);
      case "ras":
        return new MediaType("image/x-cmu-raster", null);
      case "cdf":
        return new MediaType("application/x-netcdf", null);
      case "jad":
        return new MediaType("text/vnd.sun.j2me.app-descriptor", null);
      case "dvi":
        return new MediaType("application/x-dvi", null);
      case "xml":
        return xml;
      case "exe":
        return octetStream;
      case "xls":
        return new MediaType("application/vnd.ms-excel", null);
      case "scss":
        return css;
      case "csv":
        return new MediaType("text/comma-separated-values", UTF_8);
      case "css":
        return css;
      case "xhtml":
        return new MediaType("application/xhtml+xml", UTF_8);
      case "rpm":
        return new MediaType("application/x-rpm", null);
      case "wtls-ca-certificate":
        return new MediaType("application/vnd.wap.wtls-ca-certificate", null);
      case "wmls":
        return new MediaType("text/vnd.wap.wmlscript", null);
      case "csh":
        return new MediaType("application/x-csh", null);
      case "aifc":
        return new MediaType("audio/x-aiff", null);
      case "ez":
        return new MediaType("application/andrew-inset", null);
      case "jpe":
        return new MediaType("image/jpeg", null);
      case "jpg":
        return new MediaType("image/jpeg", null);
      case "coffee":
        return js;
      case "kar":
        return new MediaType("audio/midi", null);
      case "tcl":
        return new MediaType("application/x-tcl", null);
      case "wmlc":
        return new MediaType("application/vnd.wap.wmlc", null);
      case "ttf":
        return new MediaType("font/truetype", null);
      case "src":
        return new MediaType("application/x-wais-source", null);
      case "crt":
        return new MediaType("application/x-x509-ca-cert", null);
      case "qml":
        return new MediaType("text/x-qml", null);
      case "tsv":
        return new MediaType("text/tab-separated-values", null);
      case "smil":
        return new MediaType("application/smil", null);
      case "dcr":
        return new MediaType("application/x-director", null);
      case "dtd":
        return new MediaType("application/xml-dtd", null);
      case "sgml":
        return new MediaType("text/sgml", null);
      case "latex":
        return new MediaType("application/x-latex", null);
      case "aiff":
        return new MediaType("audio/x-aiff", null);
      case "json":
        return json;
      case "cab":
        return new MediaType("application/x-cabinet", null);
      case "gif":
        return new MediaType("image/gif", null);
      default:
        return octetStream;
    }

  }

  private static boolean matchOne(String expected, int len1, String contentType) {
    if (contentType.startsWith("*/*") || contentType.equals("*")) {
      return true;
    }
    int i = 0;
    int len2 = contentType.length();
    int len = Math.min(len1, len2);
    while (i < len) {
      char ch1 = expected.charAt(i);
      char ch2 = contentType.charAt(i);
      if (ch1 != ch2) {
        if (i > 0) {
          char prev = expected.charAt(i - 1);
          if (prev == '/') {
            if (ch1 == '*') {
              if (i == len1 - 1) {
                return true;
              }
              // tail/suffix matches
              for (int j = len1 - 1, k = len2 - 1; j > i; j--, k--) {
                if (expected.charAt(j) != contentType.charAt(k)) {
                  return false;
                }
              }
              return true;
            } else {
              return false;
            }
          } else {
            return false;
          }
        } else {
          return false;
        }
      }
      i += 1;
    }
    return i == len && len1 == len2;
  }

  @Override public String toString() {
    return raw;
  }
}
