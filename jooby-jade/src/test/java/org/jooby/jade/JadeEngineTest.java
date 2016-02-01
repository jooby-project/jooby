package org.jooby.jade;

import de.neuland.jade4j.JadeConfiguration;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JadeEngineTest {

  @Test
  public void testToString() throws Exception {
    Engine engine = new Engine(new JadeConfiguration(), ".jade");
    assertEquals("jade", engine.toString());
  }
}
