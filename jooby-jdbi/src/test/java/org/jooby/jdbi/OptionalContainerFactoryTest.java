package org.jooby.jdbi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Test;
import org.skife.jdbi.v2.ContainerBuilder;

public class OptionalContainerFactoryTest {

  @Test
  public void accepts() {
    assertTrue(new OptionalContainerFactory().accepts(Optional.class));
    assertFalse(new OptionalContainerFactory().accepts(Object.class));
  }

  @Test
  public void newContainerBuilderFor() {
    ContainerBuilder<Optional<?>> cb = new OptionalContainerFactory()
        .newContainerBuilderFor(Optional.class);
    assertNotNull(cb);
    assertEquals(Optional.empty(), cb.build());

    cb.add("x");

    assertEquals(Optional.of("x"), cb.build());
  }

}
