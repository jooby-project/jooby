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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MediaType implements Comparable<MediaType> {

  public static final String JSON = "application/json";

  public static final String XML = "application/xml";

  public static final String TEXT = "text/plain";

  public static final String HTML = "text/html";

  public static final String JS = "application/javascript";

  public static final String CSS = "text/css";

  public static final String OCTET_STREAM = "application/octet-stream";

  public static final String FORM_URLENCODED = "application/x-www-form-urlencoded";

  public static final String MULTIPART_FORMDATA = "multipart/form-data";

  public static final String ALL = "*/*";

  public static final MediaType json = new MediaType(JSON, "UTF-8");

  public static final MediaType xml = new MediaType(XML, "UTF-8");

  public static final MediaType text = new MediaType(TEXT, "UTF-8");

  public static final MediaType html = new MediaType(HTML, "UTF-8");

  public static final MediaType js = new MediaType(JS, "UTF-8");

  public static final MediaType css = new MediaType(CSS, "UTF-8");

  public static final MediaType octetStream = new MediaType(OCTET_STREAM, null);

  public static final MediaType formUrlencoded = new MediaType(FORM_URLENCODED, "UTF-8");

  public static final MediaType multipartFormdata = new MediaType(MULTIPART_FORMDATA, "UTF-8");

  public static final MediaType all = new MediaType(ALL, null);

  private final String value;

  private final String charset;

  private final int subtypeStart;

  private final int subtypeEnd;

  private MediaType(@Nonnull String value, String charset) {
    this.value = value;
    this.subtypeStart = value.indexOf('/');
    if (subtypeStart < 0) {
      throw new IllegalArgumentException("Invalid media type: " + value);
    }
    int subtypeEnd = value.indexOf(';');
    this.subtypeEnd = subtypeEnd < 0 ? value.length() : subtypeEnd;
    this.charset = charset;
  }

  @Override public boolean equals(Object obj) {
    if (obj instanceof MediaType) {
      MediaType that = (MediaType) obj;
      return type().equals(that.type()) && subtype().equals(that.subtype());
    }
    return false;
  }

  @Override public int hashCode() {
    return value.hashCode();
  }

  public @Nullable String param(@Nonnull String name) {
    int paramStart = subtypeEnd + 1;
    for (int i = subtypeEnd; i < value.length(); i++) {
      char ch = value.charAt(i);
      if (ch == '=') {
        String pname = value.substring(paramStart, i).trim();
        int paramValueEnd = value.indexOf(';', i);
        if (paramValueEnd < 0) {
          paramValueEnd = value.length();
        }
        if (pname.equals(name)) {
          return value.substring(i + 1, paramValueEnd).trim();
        }
        paramStart = paramValueEnd + 1;
        i = paramStart;
      }
    }
    return null;
  }

  public @Nonnull String value() {
    return value.substring(0, subtypeEnd);
  }

  @Nonnull public float quality() {
    String q = param("q");
    return q == null ? 1f : Float.parseFloat(q);
  }

  @Override public int compareTo(MediaType other) {
    if (this == other) {
      return 0;
    }
    int diff = other.score() - score();
    if (diff == 0) {
      diff = Float.compare(other.quality(), quality());
      if (diff == 0) {
        diff = other.paramSize() - paramSize();
      }
    }
    return diff;
  }

  public boolean isTextual() {
    String type = type();
    if (type.equals("text")) {
      return true;
    }
    if (type.equals("application")) {
      String subtype = subtype();
      return subtype.endsWith("json") || subtype.endsWith("javascript") || subtype.endsWith("xml");
    }
    return false;
  }

  public @Nullable String charset() {
    String charset = _charset(this.charset);
    if (charset == null && isTextual()) {
      return "UTF-8";
    }
    return charset;
  }

  private String _charset(String charset) {
    String charsetName = param("charset");
    return charsetName == null ? charset : charsetName;
  }

  public @Nonnull String type() {
    return value.substring(0, subtypeStart).trim();
  }

  public @Nonnull String subtype() {
    return value.substring(subtypeStart + 1, subtypeEnd).trim();
  }

  public boolean matches(@Nonnull String contentType) {
    return matches(value(), contentType);
  }

  public boolean matches(@Nonnull MediaType type) {
    return matches(type.value());
  }

  public int score() {
    int precendence = 0;
    if (!type().equals("*")) {
      precendence += 1;
    }
    if (!subtype().equals("*")) {
      precendence += 1;
    }
    return precendence;
  }

  public int paramSize() {
    int p = 0;
    for (int i = subtypeEnd; i < value.length(); i++) {
      char ch = value.charAt(i);
      if (ch == '=') {
        p += 1;
      }
    }
    return p;
  }

  public static @Nonnull MediaType valueOf(@Nonnull String value) {
    if (value == null) {
      return all;
    }
    switch (value.trim()) {
      case HTML:
        return html;
      case TEXT:
        return text;
      case JSON:
        return json;
      case JS:
        return js;
      case CSS:
        return css;
      case FORM_URLENCODED:
        return formUrlencoded;
      case MULTIPART_FORMDATA:
        return multipartFormdata;
      case OCTET_STREAM:
        return octetStream;
      case XML:
        return xml;
      /** ALL */
      case "":
      case "*":
      case ALL:
        return all;
      /** Creates new: */
      default:
        return new MediaType(value, null);
    }
  }

  public static @Nonnull List<MediaType> parse(@Nullable String value) {
    if (value == null || value.length() == 0) {
      return Collections.emptyList();
    }
    List<MediaType> result = new ArrayList<>(5);
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

  public static boolean matches(@Nonnull String expected, @Nonnull String contentType) {
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

  public static @Nonnull MediaType byFile(@Nonnull File file) {
    return byFile(file.getName());
  }

  public static @Nonnull MediaType byFile(@Nonnull Path file) {
    return byFile(file.getFileName().toString());
  }

  public static @Nonnull MediaType byFile(@Nonnull String filename) {
    int index = filename.lastIndexOf('.');
    return index > 0 ? byFileExtension(filename.substring(index + 1)) : octetStream;
  }

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
        return new MediaType("application/xhtml+xml", "UTF-8");
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
        return new MediaType("application/vnd.mozilla.xul+xml", "UTF-8");
      case "xslt":
        return new MediaType("application/xslt+xml", "UTF-8");
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
        return new MediaType("application/voicexml+xml", "UTF-8");
      case "mathml":
        return new MediaType("application/mathml+xml", "UTF-8");
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
        return new MediaType("application/hocon", "UTF-8");
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
        return new MediaType("text/richtext", "UTF-8");
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
        return new MediaType("application/rdf+xml", "UTF-8");
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
        return new MediaType("application/yaml", "UTF-8");
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
        return new MediaType("text/comma-separated-values", "UTF-8");
      case "css":
        return css;
      case "xhtml":
        return new MediaType("application/xhtml+xml", "UTF-8");
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
    }
    return octetStream;
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
    return value;
  }
}
