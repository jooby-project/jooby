package org.jooby.issues;

import static org.junit.Assert.assertEquals;

import org.jooby.MediaType;
import org.junit.Test;

public class Issue197 {

  @Test
  public void shouldParseOddMediaType() {
    MediaType type = MediaType.parse("*; q=.2").iterator().next();
    assertEquals("*/*", type.name());
  }
}
