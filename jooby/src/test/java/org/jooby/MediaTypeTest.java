package org.jooby;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Test;

public class MediaTypeTest {

  @Test
  public void first1() {
    List<MediaType> supported = MediaType.valueOf("text/html", "application/xhtml+xml",
        "application/xml;q=0.9", "image/webp", "*/*;q=0.8");

    assertFirst(supported, MediaType.valueOf("text/html"));

    assertFirst(supported, MediaType.valueOf("text/plain"));
  }

  @Test
  public void firstFilter() {
    assertEquals(MediaType.js, MediaType.matcher(MediaType.js).first(MediaType.js).get());
    assertEquals(true, MediaType.matcher(MediaType.js).matches(MediaType.js));
    assertEquals(false, MediaType.matcher(MediaType.js).matches(MediaType.json));
  }

  @Test
  public void types() {
    assertEquals("application", MediaType.js.type());
    assertEquals("javascript", MediaType.js.subtype());
  }

  @Test(expected = IllegalArgumentException.class)
  public void nullFilter() {
    MediaType.matcher(MediaType.js).filter(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyFilter() {
    MediaType.matcher(MediaType.js).filter(Collections.emptyList());
  }

  @Test
  public void firstMany() {
    List<MediaType> supported = MediaType.valueOf("text/html", "application/*+json");

    assertFirst(supported, MediaType.valueOf("text/html"));

    assertFirst(supported, MediaType.valueOf("application/vnd.github+json"));
  }

  private void assertFirst(final List<MediaType> supported, final MediaType candidate) {
    assertFirst(supported, candidate, candidate);
  }

  private void assertFirst(final List<MediaType> supported, final MediaType candidate,
      final MediaType expected) {
    assertEquals(expected, MediaType.matcher(supported).first(candidate).get());
  }

  @Test
  public void matchesEq() {
    assertTrue(MediaType.valueOf("text/html").matches(MediaType.valueOf("text/html")));
  }

  @Test
  public void matchesAny() {
    assertTrue(MediaType.valueOf("*/*").matches(MediaType.valueOf("text/html")));
  }

  @Test
  public void matchesSubtype() {
    assertTrue(MediaType.valueOf("text/*").matches(MediaType.valueOf("text/html")));
  }

  @Test
  public void matchesSubtypeSuffix() {
    assertTrue(MediaType.valueOf("application/*+xml").matches(
        MediaType.valueOf("application/soap+xml")));
    assertTrue(MediaType.valueOf("application/*xml").matches(
        MediaType.valueOf("application/soapxml")));
  }

  @Test
  public void order() {
    assertMediaTypes((MediaType.valueOf("*/*", "audio/*", "audio/basic")),
        "audio/basic;q=1", "audio/*;q=1", "*/*;q=1");

    assertMediaTypes((MediaType.valueOf("audio/*;q=0.7", "audio/*;q=0.3", "audio/*")),
        "audio/*;q=1", "audio/*;q=0.7", "audio/*;q=0.3");

    assertMediaTypes(
        (MediaType.valueOf("text/plain; q=0.5", "text/html", "text/x-dvi; q=0.8",
            "text/x-c")),
        "text/html;q=1", "text/x-c;q=1", "text/x-dvi;q=0.8", "text/plain;q=0.5");
  }

  @Test
  public void precedenceWithLevel() {
    assertMediaTypes(
        (MediaType.valueOf("text/*", "text/html", "text/html;level=1", "*/*")),
        "text/html;q=1;level=1", "text/html;q=1", "text/*;q=1", "*/*;q=1");
  }

  @Test
  public void precedenceWithLevelAndQuality() {
    assertMediaTypes((MediaType.valueOf(
        "text/*;q=0.3", "text/html;q=0.7", "text/html;level=1",
        "text/html;level=2;q=0.4", "*/*;q=0.5")),
        "text/html;q=1;level=1", "text/html;q=0.7", "text/html;q=0.4;level=2", "text/*;q=0.3",
        "*/*;q=0.5");
  }

  @Test
  public void text() {
    assertTrue(MediaType.json.isText());
    assertTrue(MediaType.html.isText());
    assertTrue(MediaType.xml.isText());
    assertTrue(MediaType.css.isText());
    assertTrue(MediaType.js.isText());
    assertTrue(MediaType.valueOf("application/*+xml").isText());
    assertTrue(MediaType.valueOf("application/*xml").isText());
    assertFalse(MediaType.octetstream.isText());
    assertTrue(MediaType.valueOf("application/hocon").isText());
  }

  @Test
  public void compareSameInstance() {
    assertTrue(MediaType.json.compareTo(MediaType.json) == 0);
  }

  @Test
  public void wildcardHasLessPrecendence() {
    assertTrue(MediaType.all.compareTo(MediaType.json) == 1);

    assertTrue(MediaType.json.compareTo(MediaType.all) == -1);
  }

  @Test
  public void compareParams() {
    MediaType one = MediaType.valueOf("application/json;charset=UTF-8");
    assertEquals(-1, one.compareTo(MediaType.json));
    assertEquals(0, MediaType.valueOf("application/json").compareTo(MediaType.json));
    assertEquals(1, MediaType.json.compareTo(one));
  }

  @Test
  public void hash() {
    assertEquals(MediaType.json.hashCode(), MediaType.json.hashCode());
    assertNotEquals(MediaType.html.hashCode(), MediaType.json.hashCode());
  }

  @Test
  public void eq() {
    assertEquals(MediaType.json, MediaType.json);
    assertEquals(MediaType.json, MediaType.valueOf("application/json"));
    assertEquals(MediaType.valueOf("application/json"), MediaType.json);
    assertNotEquals(MediaType.html, MediaType.json);
    assertNotEquals(MediaType.json, MediaType.html);
    assertNotEquals(MediaType.text, MediaType.html);
    assertNotEquals(MediaType.json, MediaType.valueOf("application/json;text=true"));
    assertNotEquals(MediaType.json, new Object());
  }

  @Test(expected = IllegalArgumentException.class)
  public void badMediaType() {
    MediaType.valueOf("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void badMediaType2() {
    MediaType.valueOf("application/and/something");
  }

  @Test(expected = IllegalArgumentException.class)
  public void badMediaType3() {
    MediaType.valueOf("*/json");
  }

  @Test
  public void params() {
    MediaType type = MediaType.valueOf("application/json;q=1.7;charset=UTF-16");
    assertEquals("1.7", type.params().get("q"));
    assertEquals("utf-16", type.params().get("charset"));
  }

  @Test
  public void badParam() {
    MediaType type = MediaType.valueOf("application/json;charset");
    assertEquals(null, type.params().get("charset"));
  }

  @Test
  public void acceptHeader() {
    List<MediaType> types = MediaType.parse("json , html");
    assertEquals(MediaType.json, types.get(0));
    assertEquals(MediaType.html, types.get(1));
  }

  @Test
  public void byPath() {
    Optional<MediaType> type = MediaType.byPath(Paths.get("file.json"));
    assertEquals(MediaType.json, type.get());
  }

  @Test
  public void byBadPath() {
    Optional<MediaType> type = MediaType.byPath(Paths.get("file"));
    assertEquals(Optional.empty(), type);
  }

  @Test
  public void byExt() {
    Optional<MediaType> type = MediaType.byExtension("json");
    assertEquals(MediaType.json, type.get());
  }

  @Test
  public void byUnknownExt() {
    Optional<MediaType> type = MediaType.byExtension("unk");
    assertEquals(Optional.empty(), type);
  }

  private void assertMediaTypes(final List<MediaType> types, final String... expected) {
    assertEquals(types.toString(), expected.length, types.size());
    Collections.sort(types);
    Iterator<MediaType> iterator = types.iterator();
    for (int i = 0; i < expected.length; i++) {
      MediaType m = iterator.next();
      String found = m.name()
          + m.params().entrySet().stream().map(Map.Entry::toString)
              .collect(Collectors.joining(";", ";", ""));
      assertEquals("types[" + i + "] must be: " + expected[i] + " found: " + types, expected[i],
          found);
    }
  }
}
