package io.jooby.internal;

import io.jooby.internal.$Chi.ZeroCopyString;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ZeroCopyStringTest {
  @Test
  public void shouldCreateFromString() {
    String string = "/path";
    ZeroCopyString search = new ZeroCopyString(string);
    assertEquals(string, search.toString());
    assertEquals(string.length(), search.length());
  }

  @Test
  public void shouldStartWith() {
    ZeroCopyString search = new ZeroCopyString("/api/profile");
    ZeroCopyString s1 = new ZeroCopyString("/api");
    assertTrue(search.startsWith(s1));
  }

  @Test
  public void shouldSubstringFromBeginning() {
    ZeroCopyString search = new ZeroCopyString("/api/profile");
    ZeroCopyString profile = search.substring("/api".length());
    assertEquals("/profile", profile.toString());
    assertEquals("/profile".length(), profile.length());
    assertTrue(profile.startsWith(new ZeroCopyString("/profile")), profile.toString());
    ZeroCopyString empty = profile.substring("/profile".length());
    assertEquals("", empty.toString());
    assertEquals(0, empty.length());
  }

  @Test
  public void shouldSubstring() {
    ZeroCopyString sfoos = new ZeroCopyString("/foo/");
    ZeroCopyString s = new ZeroCopyString("/");
    ZeroCopyString foos = sfoos.substring(s.length());
    assertEquals("foo/", foos.toString());
    ZeroCopyString f = foos.substring("foo".length());
    assertEquals("/", f.toString());
  }

  @Test
  public void shouldSubstringRange() {
    ZeroCopyString string = new ZeroCopyString("/articles/{id}");
    assertEquals("id", string.substring("/articles/{".length(), string.length() - 1).toString());
    assertEquals("id", string.substring("/articles/".length()).substring(1, 3).toString());
  }

  @Test
  public void shouldSubstringRangeBug() {
    ZeroCopyString string = new ZeroCopyString("/regex/678/edit".toCharArray(), 7, 8);
    assertEquals("678/edit", string.toString());
    assertEquals("678", string.substring(0, 3).toString());
  }

  @Test
  public void shouldFindCharacter() {
    ZeroCopyString string = new ZeroCopyString("/articles/tail/match");
    ZeroCopyString tailmatch = string.substring("/articles/".length());
    assertEquals("tail/match", tailmatch.toString());
    assertEquals("tail/match".indexOf('/'), tailmatch.indexOf('/'));
  }
}
