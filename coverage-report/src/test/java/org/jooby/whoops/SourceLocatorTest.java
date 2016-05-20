package org.jooby.whoops;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.jooby.whoops.SourceLocator.Source;
import org.junit.Test;

public class SourceLocatorTest {

  @Test
  public void findJava() throws IOException {
    SourceLocator locator = SourceLocator.local();
    Source source = locator.source(WhoopsApp.class.getName());
    assertNotNull(source);
    assertTrue(source.getPath().toFile().exists());
    assertTrue(source.getLines().size() > 0);
    assertEquals("public class WhoopsApp extends Jooby {", source.source(5, 6));
    assertEquals(source.getPath().toString(), source.toString());
  }

  @Test
  public void range() throws IOException {
    SourceLocator locator = SourceLocator.local();
    Source source = locator.source(WhoopsApp.class.getName());
    assertNotNull(source);
    assertArrayEquals(new int[]{0, 20}, source.range(1, 10));
    assertArrayEquals(new int[]{5, 25}, source.range(15, 10));
    assertArrayEquals(new int[]{15, 35}, source.range(33, 10));
  }

  @Test
  public void emptyLinesShouldBeOneSpace() throws IOException {
    SourceLocator locator = SourceLocator.local();
    Source source = locator.source(WhoopsApp.class.getName());
    assertNotNull(source);
    assertEquals(" ", source.source(1, 2));
    assertEquals("", source.source(-1, 2));
    assertEquals("", source.source(10, Integer.MAX_VALUE));
  }

  @Test
  public void findFile() throws IOException {
    SourceLocator locator = SourceLocator.local();
    SourceLocator.Source source = locator.source("whoops.js");
    assertNotNull(source);
    assertTrue(source.getPath().toFile().exists());
    assertTrue(source.getLines().size() > 0);
    assertEquals("  console.log('hey')", source.source(1, 2));

    assertEquals("})(jQuery);", source.source(2, 3));
  }

  @Test
  public void missingFile() throws IOException {
    SourceLocator locator = SourceLocator.local();
    Source source = locator.source("missing.js");
    assertNotNull(source);
    assertFalse(source.getPath().toFile().exists());
    assertTrue(source.getLines().size() == 0);
    assertEquals("", source.source(2, 1));
  }

}
