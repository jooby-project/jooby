package jooby;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
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
  }

  @Test
  public void order() {
    assertMediaTypes(new TreeSet<>(MediaType.valueOf("*/*", "audio/*", "audio/basic")),
        "audio/basic;q=1", "audio/*;q=1", "*/*;q=1");

    assertMediaTypes(new TreeSet<>(MediaType.valueOf("audio/*;q=0.7", "audio/*;q=0.3", "audio/*")),
        "audio/*;q=1", "audio/*;q=0.7", "audio/*;q=0.3");

    assertMediaTypes(
        new TreeSet<>(MediaType.valueOf("text/plain; q=0.5", "text/html", "text/x-dvi; q=0.8",
            "text/x-c")),
        "text/html;q=1", "text/x-c;q=1", "text/x-dvi;q=0.8", "text/plain;q=0.5");
  }

  @Test
  public void precedenceWithLevel() {
    assertMediaTypes(
        new TreeSet<>(MediaType.valueOf("text/*", "text/html", "text/html;level=1", "*/*")),
        "text/html;q=1;level=1", "text/html;q=1", "text/*;q=1", "*/*;q=1");
  }

  @Test
  public void precedenceWithLevelAndQuality() {
    assertMediaTypes(new TreeSet<>(MediaType.valueOf(
        "text/*;q=0.3", "text/html;q=0.7", "text/html;level=1",
        "text/html;level=2;q=0.4", "*/*;q=0.5")),
        "text/html;q=1;level=1", "text/html;q=0.7", "text/html;q=0.4;level=2", "text/*;q=0.3",
        "*/*;q=0.5");
  }

  private void assertMediaTypes(final Collection<MediaType> types, final String... expected) {
    assertEquals(types.toString(), expected.length, types.size());
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
