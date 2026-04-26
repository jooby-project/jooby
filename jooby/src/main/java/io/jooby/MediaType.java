/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Implementation of media/content type.
 *
 * @since 2.0.0
 */
public final class MediaType implements Comparable<MediaType> {

  /** APPLICATION_JSON. */
  public static final String JSON = "application/json";

  /** APPLICATION_PROBLEM_JSON. */
  public static final String PROBLEM_JSON = "application/problem+json";

  /** APPLICATION_XML. */
  public static final String XML = "application/xml";

  /** APPLICATION_PROBLEM_XML. */
  public static final String PROBLEM_XML = "application/problem+xml";

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

  /** YAML. */
  public static final String YAML = "text/yaml";

  /** ALL. */
  public static final String ALL = "*/*";

  /**
   * APPLICATION_JSON. The application/json content type, as defined by RFC 8259, does not require a
   * charset parameter. This is because JSON text is typically expected to be encoded in UTF-8, and
   * the specification does not define other encodings or a charset parameter for this media type.
   */
  public static final MediaType json = new MediaType(JSON, null);

  /** YAML. */
  public static final MediaType yaml = new MediaType(YAML, UTF_8);

  /** APPLICATION_XML. */
  public static final MediaType xml = new MediaType(XML, UTF_8);

  /** TEXT_PLAIN. */
  public static final MediaType text = new MediaType(TEXT, UTF_8);

  /** TEXT_HTML. */
  public static final MediaType html = new MediaType(HTML, UTF_8);

  /** APPLICATION_JAVASCRIPT. */
  public static final MediaType js = new MediaType(JS, UTF_8);

  /** TEXT_CSS. */
  public static final MediaType css = new MediaType(CSS, UTF_8);

  /** APPLICATION_OCTET_STREAM. */
  public static final MediaType octetStream = new MediaType(OCTET_STREAM, null);

  /** FORM_URLENCODED. */
  public static final MediaType form = new MediaType(FORM_URLENCODED, UTF_8);

  /** MULTIPART_FORM_DATA. */
  public static final MediaType multipart = new MediaType(MULTIPART_FORMDATA, UTF_8);

  /** ALL. */
  public static final MediaType all = new MediaType(ALL, null);

  private final String raw;

  private final Charset charset;

  private final int subtypeStart;

  private final int subtypeEnd;

  private final String value;

  private final String contentTypeHeader;

  private MediaType(String value, Charset charset) {
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
    this.contentTypeHeader = this.charset == null ? value : value + ";charset=" + charset.name();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof MediaType) {
      MediaType that = (MediaType) obj;
      return getType().equals(that.getType()) && getSubtype().equals(that.getSubtype());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  /**
   * Get a parameter that matches the given name or <code>null</code>.
   *
   * @param name Parameter name.
   * @return Parameter value or <code>null</code>.
   */
  public @Nullable String getParameter(String name) {
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
  public String getValue() {
    return value;
  }

  /**
   * Render a content type header and add the charset parameter (when present).
   *
   * @return Content type header.
   */
  public String toContentTypeHeader() {
    return contentTypeHeader;
  }

  /**
   * Value of <code>q</code> parameter.
   *
   * @return Value of <code>q</code> parameter.
   */
  public float getQuality() {
    String q = getParameter("q");
    return q == null ? 1f : Float.parseFloat(q);
  }

  @Override
  public int compareTo(MediaType other) {
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
   *
   * @return True for textual mediatype.
   */
  public boolean isTextual() {
    if (getType().equals("text")) {
      return true;
    }
    String subtype = getSubtype();
    return subtype.endsWith("json")
        || subtype.endsWith("javascript")
        || subtype.endsWith("xml")
        || subtype.endsWith("yaml");
  }

  /**
   * Indicates whenever this is a json mediatype.
   *
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
  public String getType() {
    return raw.substring(0, subtypeStart).trim();
  }

  /**
   * Subtype segment of mediatype (trailing type).
   *
   * @return Subtype segment of mediatype (trailing type).
   */
  public String getSubtype() {
    return raw.substring(subtypeStart + 1, subtypeEnd).trim();
  }

  /**
   * True if this mediatype is compatible with the given content type.
   *
   * @param mediaType Media type to test.
   * @return True if this mediatype is compatible with the given content type.
   */
  public boolean matches(String mediaType) {
    return matches(value, mediaType);
  }

  /**
   * True if this mediatype is compatible with the given content type.
   *
   * @param type Media type to test.
   * @return True if this mediatype is compatible with the given content type.
   */
  public boolean matches(MediaType type) {
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
  public static MediaType valueOf(String value) {
    if (value == null || value.isEmpty() || value.equals("*") || value.equals("*/*")) {
      return all;
    }
    if (HTML.equalsIgnoreCase(value) || "html".equals(value)) {
      return html;
    }
    if (TEXT.equalsIgnoreCase(value) || "text".equals(value)) {
      return text;
    }
    if (JSON.equalsIgnoreCase(value) || "json".equals(value)) {
      return json;
    }
    if (JS.equalsIgnoreCase(value) || "js".equals(value) || "javascript".equals(value)) {
      return js;
    }
    if (CSS.equalsIgnoreCase(value) || "css".equals(value)) {
      return css;
    }
    if (FORM_URLENCODED.equalsIgnoreCase(value) || "form".equals(value)) {
      return form;
    }
    if (MULTIPART_FORMDATA.equalsIgnoreCase(value) || "multipart".equals(value)) {
      return multipart;
    }
    if (OCTET_STREAM.equalsIgnoreCase(value) || "octetStream".equals(value)) {
      return octetStream;
    }
    if (XML.equalsIgnoreCase(value) || "xml".equals(value)) {
      return xml;
    }
    if (YAML.equalsIgnoreCase(value) || "yaml".equals(value)) {
      return yaml;
    }
    if (value.startsWith("*;")) {
      return new MediaType("*/" + value, null);
    }
    return new MediaType(value, null);
  }

  /**
   * Parse one or more mediatype values. Mediatype must be separated by comma <code>,</code>.
   *
   * @param value Mediatype comma separated value.
   * @return One or more mediatypes.
   */
  public static List<MediaType> parse(@Nullable String value) {
    if (value == null || value.isEmpty()) {
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

  static boolean matches(String expected, String contentType) {
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
  public static MediaType byFile(File file) {
    return byFile(file.getName());
  }

  /**
   * Mediatype by file extension.
   *
   * @param file File.
   * @return Mediatype.
   */
  public static MediaType byFile(Path file) {
    return byFile(file.getFileName().toString());
  }

  /**
   * Mediatype by file extension.
   *
   * @param filename File.
   * @return Mediatype.
   */
  public static MediaType byFile(String filename) {
    int index = filename.lastIndexOf('.');
    return index > 0 ? byFileExtension(filename.substring(index + 1)) : octetStream;
  }

  /**
   * Mediatype by file extension.
   *
   * @param ext File extension.
   * @return Mediatype.
   */
  public static MediaType byFileExtension(String ext, String defaultType) {
    var result = byFileExtension(ext);
    if (result.equals(octetStream) || result.equals(all)) {
      return MediaType.valueOf(defaultType);
    }
    return result;
  }

  /**
   * Mediatype by file extension.
   *
   * @param ext File extension.
   * @return Mediatype.
   */
  public static MediaType byFileExtension(String ext) {
    return switch (ext) {
      case "spl" -> new MediaType("application/x-futuresplash", null);
      case "java" -> text;
      case "class" -> new MediaType("application/java-vm", null);
      case "cpt" -> new MediaType("application/mac-compactpro", null);
      case "etx" -> new MediaType("text/x-setext", null);
      case "tar" -> new MediaType("application/x-tar", null);
      case "js" -> js;
      case "ogg" -> new MediaType("application/ogg", null);
      case "xyz" -> new MediaType("chemical/x-xyz", null);
      case "msh" -> new MediaType("model/mesh", null);
      case "ustar" -> new MediaType("application/x-ustar", null);
      case "msi" -> octetStream;
      case "xht" -> new MediaType("application/xhtml+xml", UTF_8);
      case "bmp" -> new MediaType("image/bmp", null);
      case "silo" -> new MediaType("model/mesh", null);
      case "sv4crc" -> new MediaType("application/x-sv4crc", null);
      case "man" -> new MediaType("application/x-troff-man", null);
      case "map" -> text;
      case "cpio" -> new MediaType("application/x-cpio", null);
      case "snd" -> new MediaType("audio/basic", null);
      case "iges" -> new MediaType("model/iges", null);
      case "smi" -> new MediaType("application/smil", null);
      case "bcpio" -> new MediaType("application/x-bcpio", null);
      case "pgm" -> new MediaType("image/x-portable-graymap", null);
      case "pgn" -> new MediaType("application/x-chess-pgn", null);
      case "vcd" -> new MediaType("application/x-cdlink", null);
      case "aif" -> new MediaType("audio/x-aiff", null);
      case "ods" -> new MediaType("application/vnd.oasis.opendocument.spreadsheet", null);
      case "odt" -> new MediaType("application/vnd.oasis.opendocument.text", null);
      case "odp" -> new MediaType("application/vnd.oasis.opendocument.presentation", null);
      case "jpeg" -> new MediaType("image/jpeg", null);
      case "xwd" -> new MediaType("image/x-xwindowdump", null);
      case "odc" -> new MediaType("application/vnd.oasis.opendocument.chart", null);
      case "ots" -> new MediaType("application/vnd.oasis.opendocument.spreadsheet-template", null);
      case "ott" -> new MediaType("application/vnd.oasis.opendocument.text-template", null);
      case "odf" -> new MediaType("application/vnd.oasis.opendocument.formula", null);
      case "otp" -> new MediaType("application/vnd.oasis.opendocument.presentation-template", null);
      case "oda" -> new MediaType("application/oda", null);
      case "odb" -> new MediaType("application/vnd.oasis.opendocument.database", null);
      case "less" -> css;
      case "doc" -> new MediaType("application/msword", null);
      case "odm" -> new MediaType("application/vnd.oasis.opendocument.text-master", null);
      case "odg" -> new MediaType("application/vnd.oasis.opendocument.graphics", null);
      case "woff" -> new MediaType("application/x-font-woff", null);
      case "odi" -> new MediaType("application/vnd.oasis.opendocument.image", null);
      case "otc" -> new MediaType("application/vnd.oasis.opendocument.chart-template", null);
      case "otf" -> new MediaType("font/opentype", null);
      case "zip" -> new MediaType("application/zip", null);
      case "skt" -> new MediaType("application/x-koan", null);
      case "eps" -> new MediaType("application/postscript", null);
      case "mpe" -> new MediaType("video/mpeg", null);
      case "otg" -> new MediaType("application/vnd.oasis.opendocument.graphics-template", null);
      case "oth" -> new MediaType("application/vnd.oasis.opendocument.text-web", null);
      case "oti" -> new MediaType("application/vnd.oasis.opendocument.image-template", null);
      case "mpg" -> new MediaType("video/mpeg", null);
      case "ps" -> new MediaType("application/postscript", null);
      case "xul" -> new MediaType("application/vnd.mozilla.xul+xml", UTF_8);
      case "xslt" -> new MediaType("application/xslt+xml", UTF_8);
      case "dms" -> octetStream;
      case "mol" -> new MediaType("chemical/x-mdl-molfile", null);
      case "eot" -> new MediaType("application/vnd.ms-fontobject", null);
      case "skd" -> new MediaType("application/x-koan", null);
      case "wmlsc" -> new MediaType("application/vnd.wap.wmlscriptc", null);
      case "roff" -> new MediaType("application/x-troff", null);
      case "skp" -> new MediaType("application/x-koan", null);
      case "mpga" -> new MediaType("audio/mpeg", null);
      case "mov" -> new MediaType("video/quicktime", null);
      case "igs" -> new MediaType("model/iges", null);
      case "skm" -> new MediaType("application/x-koan", null);
      case "sv4cpio" -> new MediaType("application/x-sv4cpio", null);
      case "wbmp" -> new MediaType("image/vnd.wap.wbmp", null);
      case "bin" -> new MediaType("application/octet-stream", null);
      case "z" -> new MediaType("application/compress", null);
      case "html" -> html;
      case "gtar" -> new MediaType("application/x-gtar", null);
      case "pdb" -> new MediaType("chemical/x-pdb", null);
      case "t" -> new MediaType("application/x-troff", null);
      case "mp2" -> new MediaType("audio/mpeg", null);
      case "mp3" -> new MediaType("audio/mpeg", null);
      case "ms" -> new MediaType("application/x-troff-ms", null);
      case "wrl" -> new MediaType("model/vrml", null);
      case "mp4" -> new MediaType("video/mp4", null);
      case "vxml" -> new MediaType("application/voicexml+xml", UTF_8);
      case "mathml" -> new MediaType("application/mathml+xml", UTF_8);
      case "hdf" -> new MediaType("application/x-hdf", null);
      case "wav" -> new MediaType("audio/x-wav", null);
      case "pdf" -> new MediaType("application/pdf", null);
      case "nc" -> new MediaType("application/x-netcdf", null);
      case "sit" -> new MediaType("application/x-stuffit", null);
      case "htm" -> html;
      case "jnlp" -> new MediaType("application/x-java-jnlp-file", null);
      case "dll" -> new MediaType("application/x-msdownload", null);
      case "xsl" -> xml;
      case "ief" -> new MediaType("image/ief", null);
      case "rgb" -> new MediaType("image/x-rgb", null);
      case "htc" -> new MediaType("text/x-component", null);
      case "avi" -> new MediaType("video/x-msvideo", null);
      case "me" -> new MediaType("application/x-troff-me", null);
      case "tiff" -> new MediaType("image/tiff", null);
      case "pbm" -> new MediaType("image/x-portable-bitmap", null);
      case "xsd" -> xml;
      case "mesh" -> new MediaType("model/mesh", null);
      case "xbm" -> new MediaType("image/x-xbitmap", null);
      case "midi" -> new MediaType("audio/midi", null);
      case "texi" -> new MediaType("application/x-texinfo", null);
      case "conf" -> new MediaType("application/hocon", UTF_8);
      case "lzh" -> new MediaType("application/octet-stream", null);
      case "tr" -> new MediaType("application/x-troff", null);
      case "ts" -> js;
      case "hqx" -> new MediaType("application/mac-binhex40", null);
      case "tif" -> new MediaType("image/tiff", null);
      case "ice" -> new MediaType("x-conference/x-cooltalk", null);
      case "dir" -> new MediaType("application/x-director", null);
      case "sgm" -> new MediaType("text/sgml", null);
      case "woff2" -> new MediaType("application/font-woff2", null);
      case "sh" -> new MediaType("application/x-sh", null);
      case "ico" -> new MediaType("image/x-icon", null);
      case "asx" -> new MediaType("video/x.ms.asx", null);
      case "swf" -> new MediaType("application/x-shockwave-flash", null);
      case "texinfo" -> new MediaType("application/x-texinfo", null);
      case "ai" -> new MediaType("application/postscript", null);
      case "txt" -> text;
      case "asc" -> text;
      case "ppm" -> new MediaType("image/x-portable-pixmap", null);
      case "rtx" -> new MediaType("text/richtext", UTF_8);
      case "movie" -> new MediaType("video/x-sgi-movie", null);
      case "ra" -> new MediaType("audio/x-pn-realaudio", null);
      case "vrml" -> new MediaType("model/vrml", null);
      case "au" -> new MediaType("audio/basic", null);
      case "gzip" -> new MediaType("application/gzip", null);
      case "pps" -> new MediaType("application/vnd.ms-powerpoint", null);
      case "rdf" -> new MediaType("application/rdf+xml", UTF_8);
      case "ppt" -> new MediaType("application/vnd.ms-powerpoint", null);
      case "asf" -> new MediaType("video/x.ms.asf", null);
      case "xpm" -> new MediaType("image/x-xpixmap", null);
      case "dxr" -> new MediaType("application/x-director", null);
      case "ser" -> new MediaType("application/java-serialized-object", null);
      case "rm" -> new MediaType("audio/x-pn-realaudio", null);
      case "tgz" -> new MediaType("application/x-gtar", null);
      case "rv" -> new MediaType("video/vnd.rn-realvideo", null);
      case "shar" -> new MediaType("application/x-shar", null);
      case "rtf" -> new MediaType("application/rtf", null);
      case "svg" -> new MediaType("image/svg+xml", null);
      case "lha" -> new MediaType("application/octet-stream", null);
      case "mif" -> new MediaType("application/vnd.mif", null);
      case "mpeg" -> new MediaType("video/mpeg", null);
      case "wml" -> new MediaType("text/vnd.wap.wml", null);
      case "jsp" -> html;
      case "mid" -> new MediaType("audio/midi", null);
      case "qt" -> new MediaType("video/quicktime", null);
      case "yaml", "yml" -> yaml;
      case "pnm" -> new MediaType("image/x-portable-anymap", null);
      case "tar.gz" -> new MediaType("application/x-gtar", null);
      case "gz" -> new MediaType("application/gzip", null);
      case "ram" -> new MediaType("audio/x-pn-realaudio", null);
      case "jar" -> new MediaType("application/java-archive", null);
      case "apk" -> new MediaType("application/vnd.android.package-archive", null);
      case "tex" -> new MediaType("application/x-tex", null);
      case "png" -> new MediaType("image/png", null);
      case "ras" -> new MediaType("image/x-cmu-raster", null);
      case "cdf" -> new MediaType("application/x-netcdf", null);
      case "jad" -> new MediaType("text/vnd.sun.j2me.app-descriptor", null);
      case "dvi" -> new MediaType("application/x-dvi", null);
      case "xml" -> xml;
      case "exe" -> octetStream;
      case "xls" -> new MediaType("application/vnd.ms-excel", null);
      case "scss" -> css;
      case "csv" -> new MediaType("text/comma-separated-values", UTF_8);
      case "css" -> css;
      case "xhtml" -> new MediaType("application/xhtml+xml", UTF_8);
      case "rpm" -> new MediaType("application/x-rpm", null);
      case "wtls-ca-certificate" -> new MediaType("application/vnd.wap.wtls-ca-certificate", null);
      case "wmls" -> new MediaType("text/vnd.wap.wmlscript", null);
      case "csh" -> new MediaType("application/x-csh", null);
      case "aifc" -> new MediaType("audio/x-aiff", null);
      case "ez" -> new MediaType("application/andrew-inset", null);
      case "jpe" -> new MediaType("image/jpeg", null);
      case "jpg" -> new MediaType("image/jpeg", null);
      case "coffee" -> js;
      case "kar" -> new MediaType("audio/midi", null);
      case "tcl" -> new MediaType("application/x-tcl", null);
      case "wmlc" -> new MediaType("application/vnd.wap.wmlc", null);
      case "ttf" -> new MediaType("font/truetype", null);
      case "src" -> new MediaType("application/x-wais-source", null);
      case "crt" -> new MediaType("application/x-x509-ca-cert", null);
      case "qml" -> new MediaType("text/x-qml", null);
      case "tsv" -> new MediaType("text/tab-separated-values", null);
      case "smil" -> new MediaType("application/smil", null);
      case "dcr" -> new MediaType("application/x-director", null);
      case "dtd" -> new MediaType("application/xml-dtd", null);
      case "sgml" -> new MediaType("text/sgml", null);
      case "latex" -> new MediaType("application/x-latex", null);
      case "aiff" -> new MediaType("audio/x-aiff", null);
      case "json" -> json;
      case "cab" -> new MediaType("application/x-cabinet", null);
      case "gif" -> new MediaType("image/gif", null);
      case "wasm" -> new MediaType("application/wasm", null);
      default -> octetStream;
    };
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

  @Override
  public String toString() {
    return raw;
  }
}
