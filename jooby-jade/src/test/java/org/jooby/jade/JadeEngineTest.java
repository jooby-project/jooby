package org.jooby.jade;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.neuland.jade4j.JadeConfiguration;

public class JadeEngineTest {

  @Test
  public void testToString() throws Exception {
    Engine engine = new Engine(new JadeConfiguration(), ".jade");
    assertEquals("jade", engine.toString());
  }
}
