/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class MediaTypeTest {

  @Test
  public void json() {
    MediaType type = MediaType.json;
    assertEquals("application/json", type.toString());
    assertEquals("application/json", type.getValue());
    assertEquals("application", type.getType());
    assertEquals("json", type.getSubtype());
    assertEquals(1f, type.getQuality());
    assertEquals("UTF-8", type.getCharset().name());
  }

  @Test
  public void text() {
    MediaType type = MediaType.text;
    assertEquals("text/plain", type.toString());
    assertEquals("text/plain", type.getValue());
    assertEquals("text", type.getType());
    assertEquals("plain", type.getSubtype());
    assertEquals(1f, type.getQuality());
    assertEquals("UTF-8", type.getCharset().name());

    assertEquals("text/plain", MediaType.valueOf("text/plain").getValue());

    assertEquals("text/plain", MediaType.valueOf("text/plain;charset=UTF-8").getValue());
  }

  @Test
  public void html() {
    MediaType type = MediaType.html;
    assertEquals("text/html", type.toString());
    assertEquals("text/html", type.getValue());
    assertEquals("text", type.getType());
    assertEquals("html", type.getSubtype());
    assertEquals(1f, type.getQuality());
    assertEquals("UTF-8", type.getCharset().name());
  }

  @Test
  public void valueOf() {
    MediaType type = MediaType.valueOf("application / json; q=0.5; charset=us-ascii");
    assertEquals("application / json; q=0.5; charset=us-ascii", type.toString());
    assertEquals("application / json", type.getValue());
    assertEquals("application", type.getType());
    assertEquals("json", type.getSubtype());
    assertEquals(.5f, type.getQuality());
    assertEquals("us-ascii", type.getCharset().name().toLowerCase());

    MediaType any = MediaType.valueOf("*");
    assertEquals("*/*", any.getValue());
    assertEquals("*", any.getType());
    assertEquals("*", any.getSubtype());

    any = MediaType.valueOf("");
    assertEquals("*/*", any.getValue());
    assertEquals("*", any.getType());
    assertEquals("*", any.getSubtype());

    any = MediaType.valueOf(null);
    assertEquals("*/*", any.getValue());
    assertEquals("*", any.getType());
    assertEquals("*", any.getSubtype());
  }

  @Test
  public void valueOfAliases() {
    assertEquals(MediaType.html, MediaType.valueOf("html"));
    assertEquals(MediaType.html, MediaType.valueOf("text/html"));
    assertEquals(MediaType.text, MediaType.valueOf("text"));
    assertEquals(MediaType.text, MediaType.valueOf("text/plain"));
    assertEquals(MediaType.json, MediaType.valueOf("json"));
    assertEquals(MediaType.js, MediaType.valueOf("js"));
    assertEquals(MediaType.js, MediaType.valueOf("javascript"));
    assertEquals(MediaType.css, MediaType.valueOf("css"));
    assertEquals(MediaType.form, MediaType.valueOf("form"));
    assertEquals(MediaType.multipart, MediaType.valueOf("multipart"));
    assertEquals(MediaType.octetStream, MediaType.valueOf("octetStream"));
    assertEquals(MediaType.xml, MediaType.valueOf("xml"));
    assertEquals(MediaType.yaml, MediaType.valueOf("yaml"));
  }

  @Test
  public void constructorError() {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> MediaType.valueOf("invalidTypeString"));
    assertEquals("Invalid media type: invalidTypeString", ex.getMessage());
  }

  @Test
  public void getParameter() {
    MediaType t = MediaType.valueOf("application/json; q=0.8; charset=utf-8; foo=bar");
    assertEquals("0.8", t.getParameter("q"));
    assertEquals("utf-8", t.getParameter("charset"));
    assertEquals("bar", t.getParameter("foo"));
    assertNull(t.getParameter("baz"));

    MediaType emptyParams = MediaType.valueOf("application/json");
    assertNull(emptyParams.getParameter("q"));
  }

  @Test
  public void toContentTypeHeader() {
    assertEquals("application/json", MediaType.json.toContentTypeHeader());
    assertEquals("application/octet-stream", MediaType.octetStream.toContentTypeHeader());
    assertEquals(
        "application/xml;charset=us-ascii",
        MediaType.valueOf("application/xml;charset=us-ascii").toContentTypeHeader());
    assertEquals("text/plain;charset=UTF-8", MediaType.text.toContentTypeHeader());
  }

  @Test
  public void textualAndJsonChecks() {
    assertTrue(MediaType.text.isTextual());
    assertTrue(MediaType.json.isTextual());
    assertTrue(MediaType.xml.isTextual());
    assertTrue(MediaType.yaml.isTextual());
    assertTrue(MediaType.js.isTextual());
    assertTrue(MediaType.html.isTextual());
    assertTrue(MediaType.css.isTextual());
    assertFalse(MediaType.octetStream.isTextual());
    assertFalse(MediaType.valueOf("image/png").isTextual());

    assertTrue(MediaType.json.isJson());
    assertTrue(MediaType.valueOf("application/problem+json").isJson());
    assertFalse(MediaType.xml.isJson());
    assertFalse(MediaType.text.isJson());
  }

  @Test
  public void getCharset() {
    assertEquals("UTF-8", MediaType.text.getCharset().name());
    assertNull(MediaType.octetStream.getCharset());
    assertEquals(
        "US-ASCII", MediaType.valueOf("application/json;charset=us-ascii").getCharset().name());

    // Textual types fallback to UTF-8
    assertEquals("UTF-8", MediaType.valueOf("application/xml").getCharset().name());
  }

  @Test
  public void parse() {
    List<MediaType> result = MediaType.parse("application/json , text/html,*");
    assertEquals(3, result.size());
    assertEquals("application/json", result.get(0).getValue());
    assertEquals("text/html", result.get(1).getValue());
    assertEquals("*/*", result.get(2).getValue());

    assertEquals(0, MediaType.parse(null).size());
    assertEquals(0, MediaType.parse("").size());
    assertEquals(1, MediaType.parse("text/plain,").size());
    assertEquals(2, MediaType.parse("text/plain, application/json").size());
  }

  @Test
  public void allBadFormats() {
    assertEquals(MediaType.all, MediaType.valueOf("*"));
    assertEquals(MediaType.all, MediaType.valueOf("*;"));
    assertEquals(1f, MediaType.valueOf("*;").getQuality());
    assertEquals(MediaType.all, MediaType.valueOf("*; q=.2"));
    assertEquals(.2f, MediaType.valueOf("*; q=.2").getQuality());
  }

  @Test
  public void matches() {
    assertTrue(
        MediaType.matches(
            "application/json",
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"));

    assertTrue(MediaType.matches("application/json", "text/html, */*"));

    assertTrue(MediaType.matches("application/json", "application/json"));

    assertTrue(MediaType.matches("application/*+json", "application/xml, application/bar+json "));

    assertFalse(MediaType.matches("application/json", "text/plain"));

    assertFalse(MediaType.matches("application/json", "text/plain"));

    assertTrue(MediaType.matches("application/json", "*"));
    assertTrue(MediaType.matches("application/json", "*/*"));

    assertFalse(MediaType.matches("application/json", "application/jsonx"));
    assertFalse(MediaType.matches("application/json", "application/xjson"));

    // wild
    assertTrue(MediaType.matches("application/*json", "application/json"));
    assertTrue(MediaType.matches("application/*+json", "application/foo+json"));

    assertTrue(MediaType.matches("application/*json", "application/foojson"));
    assertFalse(MediaType.matches("application/*+json", "application/foojson"));
    assertFalse(MediaType.matches("application/*+json", "text/plain"));
    assertFalse(MediaType.matches("application/*+json", "application/jsonplain"));

    // wild edge cases
    assertTrue(MediaType.matches("application/*", "application/json"));
    assertFalse(MediaType.matches("application/*", "text/plain"));
    assertFalse(
        MediaType.matches("application/x-*", "application/x-json")); // `prev == '/'` check fails
    assertFalse(MediaType.matches("application/js*", "application/json"));

    // accept header
    assertTrue(MediaType.matches("application/json", "application/json, application/xml"));

    assertTrue(MediaType.matches("application/json", "application/xml, application/json"));

    assertTrue(MediaType.matches("application/*+json", "application/xml, application/bar+json"));

    assertTrue(MediaType.matches("application/json", "application/json, application/xml"));

    // Overloaded matches(MediaType) test
    assertTrue(MediaType.json.matches(MediaType.valueOf("application/json")));
    assertTrue(
        MediaType.valueOf("application/*+json")
            .matches(MediaType.valueOf("application/problem+json")));
    assertFalse(MediaType.json.matches(MediaType.xml));
  }

  @Test
  public void byFile() {
    assertEquals(MediaType.json, MediaType.byFile(new File("test.json")));
    assertEquals(MediaType.html, MediaType.byFile(Path.of("index.html")));
    assertEquals(MediaType.xml, MediaType.byFile("data.xml"));

    assertEquals(MediaType.octetStream, MediaType.byFile("unknown")); // no extension
    assertEquals(MediaType.octetStream, MediaType.byFile("test.unknownext"));

    assertEquals(MediaType.json, MediaType.byFileExtension("json"));
    assertEquals(MediaType.octetStream, MediaType.byFileExtension("unknownext"));

    // Fallback defaultType
    assertEquals(MediaType.text, MediaType.byFileExtension("unknownext", "text/plain"));
    assertEquals(
        MediaType.valueOf("application/custom"),
        MediaType.byFileExtension("unknownext", "application/custom"));
  }

  @Test
  public void comprehensiveExtensions() {
    assertEquals(MediaType.js, MediaType.byFileExtension("js"));
    assertEquals(MediaType.js, MediaType.byFileExtension("ts"));
    assertEquals(MediaType.js, MediaType.byFileExtension("coffee"));
    assertEquals(MediaType.yaml, MediaType.byFileExtension("yml"));
    assertEquals(MediaType.yaml, MediaType.byFileExtension("yaml"));
    assertEquals(MediaType.text, MediaType.byFileExtension("txt"));
    assertEquals(MediaType.css, MediaType.byFileExtension("css"));
    assertEquals(MediaType.css, MediaType.byFileExtension("scss"));
    assertEquals(MediaType.css, MediaType.byFileExtension("less"));
    assertEquals(MediaType.valueOf("image/png"), MediaType.byFileExtension("png"));
    assertEquals(MediaType.valueOf("image/jpeg"), MediaType.byFileExtension("jpg"));
    assertEquals(MediaType.valueOf("image/jpeg"), MediaType.byFileExtension("jpeg"));
    assertEquals(MediaType.valueOf("application/wasm"), MediaType.byFileExtension("wasm"));
    assertEquals(MediaType.valueOf("application/pdf"), MediaType.byFileExtension("pdf"));
  }

  @Test
  public void equalsAndHashCode() {
    MediaType t1 = MediaType.valueOf("application/json");
    MediaType t2 = MediaType.valueOf("application/json;q=0.5");
    MediaType t3 = MediaType.valueOf("text/html");

    assertTrue(t1.equals(t1));
    assertTrue(t1.equals(t2)); // equals only compares getType() and getSubtype()
    assertFalse(t1.equals(t3));
    assertFalse(t1.equals(null));
    assertFalse(t1.equals("application/json"));

    assertEquals(t1.hashCode(), t1.hashCode());
    assertEquals(t3.hashCode(), t3.hashCode());
  }

  @Test
  public void compareToSelf() {
    assertEquals(0, MediaType.json.compareTo(MediaType.json));
  }

  @Test
  public void precedence() {
    accept(
        "text/*, text/plain, text/plain;format=flowed, */*",
        types -> {
          assertEquals("text/plain;format=flowed", types.get(0).toString());
          assertEquals("text/plain", types.get(1).toString());
          assertEquals("text/*", types.get(2).toString());
          assertEquals("*/*", types.get(3).toString());
        });

    accept(
        "text/*;q=0.3, text/html;q=0.7, text/html;level=1,text/html;level=2;q=0.4, */*;q=0.5",
        types -> {
          assertEquals("text/html;level=1", types.get(0).toString());
          assertEquals("text/html;q=0.7", types.get(1).toString());
          assertEquals("text/html;level=2;q=0.4", types.get(2).toString());
          assertEquals("text/*;q=0.3", types.get(3).toString());
          assertEquals("*/*;q=0.5", types.get(4).toString());
        });

    accept(
        "text/html, application/xhtml+xml, application/xml;q=0.9, image/webp, */*;q=0.8,"
            + " application/json",
        types -> {
          assertEquals("text/html", types.get(0).toString());
          assertEquals("application/xhtml+xml", types.get(1).toString());
          assertEquals("image/webp", types.get(2).toString());
          assertEquals("application/json", types.get(3).toString());
          assertEquals("application/xml;q=0.9", types.get(4).toString());
          assertEquals("*/*;q=0.8", types.get(5).toString());
        });
  }

  @ParameterizedTest(name = "Extension: ''{0}'' should map to MediaType: ''{1}''")
  @MethodSource("provideExtensions")
  void testByFileExtension(String extension, String expectedMediaType) {
    // Retrieve the MediaType for the given extension
    MediaType result = MediaType.byFileExtension(extension);

    assertEquals(expectedMediaType, result.getValue());
  }

  private static Stream<Arguments> provideExtensions() {
    return Stream.of(
        // Explicit static constants mapped in the switch
        Arguments.of("java", "text/plain"),
        Arguments.of("js", "application/javascript"),
        Arguments.of("msi", "application/octet-stream"),
        Arguments.of("map", "text/plain"),
        Arguments.of("less", "text/css"),
        Arguments.of("ts", "application/javascript"),
        Arguments.of("txt", "text/plain"),
        Arguments.of("asc", "text/plain"),
        Arguments.of("yaml", "text/yaml"),
        Arguments.of("yml", "text/yaml"),
        Arguments.of("html", "text/html"),
        Arguments.of("htm", "text/html"),
        Arguments.of("jsp", "text/html"),
        Arguments.of("xml", "application/xml"),
        Arguments.of("xsl", "application/xml"),
        Arguments.of("xsd", "application/xml"),
        Arguments.of("scss", "text/css"),
        Arguments.of("css", "text/css"),
        Arguments.of("coffee", "application/javascript"),
        Arguments.of("json", "application/json"),

        // Explicit new MediaType(...) mappings
        Arguments.of("spl", "application/x-futuresplash"),
        Arguments.of("class", "application/java-vm"),
        Arguments.of("cpt", "application/mac-compactpro"),
        Arguments.of("etx", "text/x-setext"),
        Arguments.of("tar", "application/x-tar"),
        Arguments.of("ogg", "application/ogg"),
        Arguments.of("xyz", "chemical/x-xyz"),
        Arguments.of("msh", "model/mesh"),
        Arguments.of("ustar", "application/x-ustar"),
        Arguments.of("xht", "application/xhtml+xml"),
        Arguments.of("bmp", "image/bmp"),
        Arguments.of("silo", "model/mesh"),
        Arguments.of("sv4crc", "application/x-sv4crc"),
        Arguments.of("man", "application/x-troff-man"),
        Arguments.of("cpio", "application/x-cpio"),
        Arguments.of("snd", "audio/basic"),
        Arguments.of("iges", "model/iges"),
        Arguments.of("smi", "application/smil"),
        Arguments.of("bcpio", "application/x-bcpio"),
        Arguments.of("pgm", "image/x-portable-graymap"),
        Arguments.of("pgn", "application/x-chess-pgn"),
        Arguments.of("vcd", "application/x-cdlink"),
        Arguments.of("aif", "audio/x-aiff"),
        Arguments.of("ods", "application/vnd.oasis.opendocument.spreadsheet"),
        Arguments.of("odt", "application/vnd.oasis.opendocument.text"),
        Arguments.of("odp", "application/vnd.oasis.opendocument.presentation"),
        Arguments.of("jpeg", "image/jpeg"),
        Arguments.of("xwd", "image/x-xwindowdump"),
        Arguments.of("odc", "application/vnd.oasis.opendocument.chart"),
        Arguments.of("ots", "application/vnd.oasis.opendocument.spreadsheet-template"),
        Arguments.of("ott", "application/vnd.oasis.opendocument.text-template"),
        Arguments.of("odf", "application/vnd.oasis.opendocument.formula"),
        Arguments.of("otp", "application/vnd.oasis.opendocument.presentation-template"),
        Arguments.of("oda", "application/oda"),
        Arguments.of("odb", "application/vnd.oasis.opendocument.database"),
        Arguments.of("doc", "application/msword"),
        Arguments.of("odm", "application/vnd.oasis.opendocument.text-master"),
        Arguments.of("odg", "application/vnd.oasis.opendocument.graphics"),
        Arguments.of("woff", "application/x-font-woff"),
        Arguments.of("odi", "application/vnd.oasis.opendocument.image"),
        Arguments.of("otc", "application/vnd.oasis.opendocument.chart-template"),
        Arguments.of("otf", "font/opentype"),
        Arguments.of("zip", "application/zip"),
        Arguments.of("skt", "application/x-koan"),
        Arguments.of("eps", "application/postscript"),
        Arguments.of("mpe", "video/mpeg"),
        Arguments.of("otg", "application/vnd.oasis.opendocument.graphics-template"),
        Arguments.of("oth", "application/vnd.oasis.opendocument.text-web"),
        Arguments.of("oti", "application/vnd.oasis.opendocument.image-template"),
        Arguments.of("mpg", "video/mpeg"),
        Arguments.of("ps", "application/postscript"),
        Arguments.of("xul", "application/vnd.mozilla.xul+xml"),
        Arguments.of("xslt", "application/xslt+xml"),
        Arguments.of("dms", "application/octet-stream"),
        Arguments.of("mol", "chemical/x-mdl-molfile"),
        Arguments.of("eot", "application/vnd.ms-fontobject"),
        Arguments.of("skd", "application/x-koan"),
        Arguments.of("wmlsc", "application/vnd.wap.wmlscriptc"),
        Arguments.of("roff", "application/x-troff"),
        Arguments.of("skp", "application/x-koan"),
        Arguments.of("mpga", "audio/mpeg"),
        Arguments.of("mov", "video/quicktime"),
        Arguments.of("igs", "model/iges"),
        Arguments.of("skm", "application/x-koan"),
        Arguments.of("sv4cpio", "application/x-sv4cpio"),
        Arguments.of("wbmp", "image/vnd.wap.wbmp"),
        Arguments.of("bin", "application/octet-stream"),
        Arguments.of("z", "application/compress"),
        Arguments.of("gtar", "application/x-gtar"),
        Arguments.of("pdb", "chemical/x-pdb"),
        Arguments.of("t", "application/x-troff"),
        Arguments.of("mp2", "audio/mpeg"),
        Arguments.of("mp3", "audio/mpeg"),
        Arguments.of("ms", "application/x-troff-ms"),
        Arguments.of("wrl", "model/vrml"),
        Arguments.of("mp4", "video/mp4"),
        Arguments.of("vxml", "application/voicexml+xml"),
        Arguments.of("mathml", "application/mathml+xml"),
        Arguments.of("hdf", "application/x-hdf"),
        Arguments.of("wav", "audio/x-wav"),
        Arguments.of("pdf", "application/pdf"),
        Arguments.of("nc", "application/x-netcdf"),
        Arguments.of("sit", "application/x-stuffit"),
        Arguments.of("jnlp", "application/x-java-jnlp-file"),
        Arguments.of("dll", "application/x-msdownload"),
        Arguments.of("ief", "image/ief"),
        Arguments.of("rgb", "image/x-rgb"),
        Arguments.of("htc", "text/x-component"),
        Arguments.of("avi", "video/x-msvideo"),
        Arguments.of("me", "application/x-troff-me"),
        Arguments.of("tiff", "image/tiff"),
        Arguments.of("pbm", "image/x-portable-bitmap"),
        Arguments.of("mesh", "model/mesh"),
        Arguments.of("xbm", "image/x-xbitmap"),
        Arguments.of("midi", "audio/midi"),
        Arguments.of("texi", "application/x-texinfo"),
        Arguments.of("conf", "application/hocon"),
        Arguments.of("lzh", "application/octet-stream"),
        Arguments.of("tr", "application/x-troff"),
        Arguments.of("hqx", "application/mac-binhex40"),
        Arguments.of("tif", "image/tiff"),
        Arguments.of("ice", "x-conference/x-cooltalk"),
        Arguments.of("dir", "application/x-director"),
        Arguments.of("sgm", "text/sgml"),
        Arguments.of("woff2", "application/font-woff2"),
        Arguments.of("sh", "application/x-sh"),
        Arguments.of("ico", "image/x-icon"),
        Arguments.of("asx", "video/x.ms.asx"),
        Arguments.of("swf", "application/x-shockwave-flash"),
        Arguments.of("texinfo", "application/x-texinfo"),
        Arguments.of("ai", "application/postscript"),
        Arguments.of("ppm", "image/x-portable-pixmap"),
        Arguments.of("rtx", "text/richtext"),
        Arguments.of("movie", "video/x-sgi-movie"),
        Arguments.of("ra", "audio/x-pn-realaudio"),
        Arguments.of("vrml", "model/vrml"),
        Arguments.of("au", "audio/basic"),
        Arguments.of("gzip", "application/gzip"),
        Arguments.of("pps", "application/vnd.ms-powerpoint"),
        Arguments.of("rdf", "application/rdf+xml"),
        Arguments.of("ppt", "application/vnd.ms-powerpoint"),
        Arguments.of("asf", "video/x.ms.asf"),
        Arguments.of("xpm", "image/x-xpixmap"),
        Arguments.of("dxr", "application/x-director"),
        Arguments.of("ser", "application/java-serialized-object"),
        Arguments.of("rm", "audio/x-pn-realaudio"),
        Arguments.of("tgz", "application/x-gtar"),
        Arguments.of("rv", "video/vnd.rn-realvideo"),
        Arguments.of("shar", "application/x-shar"),
        Arguments.of("rtf", "application/rtf"),
        Arguments.of("svg", "image/svg+xml"),
        Arguments.of("lha", "application/octet-stream"),
        Arguments.of("mif", "application/vnd.mif"),
        Arguments.of("mpeg", "video/mpeg"),
        Arguments.of("wml", "text/vnd.wap.wml"),
        Arguments.of("mid", "audio/midi"),
        Arguments.of("qt", "video/quicktime"),
        Arguments.of("pnm", "image/x-portable-anymap"),
        Arguments.of("tar.gz", "application/x-gtar"),
        Arguments.of("gz", "application/gzip"),
        Arguments.of("ram", "audio/x-pn-realaudio"),
        Arguments.of("jar", "application/java-archive"),
        Arguments.of("apk", "application/vnd.android.package-archive"),
        Arguments.of("tex", "application/x-tex"),
        Arguments.of("png", "image/png"),
        Arguments.of("ras", "image/x-cmu-raster"),
        Arguments.of("cdf", "application/x-netcdf"),
        Arguments.of("jad", "text/vnd.sun.j2me.app-descriptor"),
        Arguments.of("dvi", "application/x-dvi"),
        Arguments.of("exe", "application/octet-stream"),
        Arguments.of("xls", "application/vnd.ms-excel"),
        Arguments.of("csv", "text/comma-separated-values"),
        Arguments.of("xhtml", "application/xhtml+xml"),
        Arguments.of("rpm", "application/x-rpm"),
        Arguments.of("wtls-ca-certificate", "application/vnd.wap.wtls-ca-certificate"),
        Arguments.of("wmls", "text/vnd.wap.wmlscript"),
        Arguments.of("csh", "application/x-csh"),
        Arguments.of("aifc", "audio/x-aiff"),
        Arguments.of("ez", "application/andrew-inset"),
        Arguments.of("jpe", "image/jpeg"),
        Arguments.of("jpg", "image/jpeg"),
        Arguments.of("kar", "audio/midi"),
        Arguments.of("tcl", "application/x-tcl"),
        Arguments.of("wmlc", "application/vnd.wap.wmlc"),
        Arguments.of("ttf", "font/truetype"),
        Arguments.of("src", "application/x-wais-source"),
        Arguments.of("crt", "application/x-x509-ca-cert"),
        Arguments.of("qml", "text/x-qml"),
        Arguments.of("tsv", "text/tab-separated-values"),
        Arguments.of("smil", "application/smil"),
        Arguments.of("dcr", "application/x-director"),
        Arguments.of("dtd", "application/xml-dtd"),
        Arguments.of("sgml", "text/sgml"),
        Arguments.of("latex", "application/x-latex"),
        Arguments.of("aiff", "audio/x-aiff"),
        Arguments.of("cab", "application/x-cabinet"),
        Arguments.of("gif", "image/gif"),
        Arguments.of("wasm", "application/wasm"),

        // The Default fallback case
        Arguments.of("completely_unknown_extension_123", "application/octet-stream"));
  }

  public static void accept(String value, Consumer<List<MediaType>> consumer) {
    List<MediaType> types = MediaType.parse(value);
    Collections.sort(types);
    consumer.accept(types);
  }
}
