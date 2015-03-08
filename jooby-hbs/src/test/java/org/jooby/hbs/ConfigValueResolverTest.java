package org.jooby.hbs;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jooby.MockUnit;
import org.junit.Test;

import com.github.jknack.handlebars.ValueResolver;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;

public class ConfigValueResolverTest {

  @Test
  public void resolveProperty() throws Exception {
    new MockUnit(Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.hasPath("prop")).andReturn(true);
          expect(config.getAnyRef("prop")).andReturn("x");
        })
        .run(unit -> {
          assertEquals("x", new ConfigValueResolver().resolve(unit.get(Config.class), "prop"));
        });
  }

  @Test
  public void resolveNotFoundProperty() throws Exception {
    new MockUnit(Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.hasPath("prop")).andReturn(false);
        })
        .run(unit -> {
          assertEquals(ValueResolver.UNRESOLVED,
              new ConfigValueResolver().resolve(unit.get(Config.class), "prop"));
        });
  }

  @Test
  public void resolveAnythingElse() throws Exception {
    new MockUnit()
        .run(unit -> {
          assertEquals(ValueResolver.UNRESOLVED,
              new ConfigValueResolver().resolve(new Object(), "prop"));
        });
  }

  @Test
  public void resolveContext() throws Exception {
    new MockUnit(Config.class)
        .run(unit -> {
          assertEquals(unit.get(Config.class),
              new ConfigValueResolver().resolve(unit.get(Config.class)));
        });
  }

  @Test
  public void resolveContextNoContext() throws Exception {
    new MockUnit(Config.class)
        .run(unit -> {
          assertEquals(ValueResolver.UNRESOLVED,
              new ConfigValueResolver().resolve(new Object()));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void propertySet() throws Exception {
    Set<Entry<String, Object>> entries = new HashSet<>();
    new MockUnit(Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          ConfigObject root = unit.mock(ConfigObject.class);
          Map<String, Object> unwrapped = unit.mock(Map.class);
          expect(config.root()).andReturn(root);
          expect(root.unwrapped()).andReturn(unwrapped);
          expect(unwrapped.entrySet()).andReturn(entries);
        })
        .run(unit -> {
          assertEquals(entries,
              new ConfigValueResolver().propertySet(unit.get(Config.class)));
        });
  }

  @Test
  public void propertySetAnything() throws Exception {
    new MockUnit(Config.class)
        .run(unit -> {
          assertEquals(Collections.emptySet(),
              new ConfigValueResolver().propertySet(new Object()));
        });
  }

}
