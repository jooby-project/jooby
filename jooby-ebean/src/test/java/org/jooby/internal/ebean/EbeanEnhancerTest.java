package org.jooby.internal.ebean;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.common.collect.Sets;

public class EbeanEnhancerTest {

  @Test
  public void newEnhancer() {
    assertTrue(EbeanEnhancer.newEnhancer() instanceof EbeanAgentEnhancer);
  }

  @Test
  public void newNoopEnhancer() {
    assertTrue(EbeanEnhancer.newEnhancer("/Missing.class", "/M1.class") instanceof EbeanEnhancer);
  }

  @Test
  public void runNoop() {
    EbeanEnhancer.newEnhancer("/Missing.class", "/M1.class").run(null);
  }

  @Test
  public void runAgent() throws Exception {
    EbeanEnhancer.newEnhancer().run(Sets.newHashSet("my.pkg"));
    // ignored
    EbeanEnhancer.newEnhancer().run(null);
  }

  @Test
  public void runAgentFromEmbeddedClass() throws Exception {
    EbeanEnhancer.newEnhancer("/org/jooby/ebean/Ebeanby.class", "/org/jooby/ebean/Ebeanby.class")
        .run(Sets.newHashSet("my.pkg"));
  }

}
