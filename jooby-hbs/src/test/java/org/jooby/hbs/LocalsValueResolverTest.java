package org.jooby.hbs;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.jooby.Locals;
import org.jooby.MockUnit;
import org.junit.Test;

import com.github.jknack.handlebars.ValueResolver;

public class LocalsValueResolverTest {

  @Test
  public void resolveProperty() throws Exception {
    new MockUnit(Locals.class)
        .expect(unit -> {
          Locals locals = unit.get(Locals.class);
          expect(locals.get("prop")).andReturn(Optional.of("x"));
        })
        .run(unit -> {
          assertEquals("x", new LocalsValueResolver().resolve(unit.get(Locals.class), "prop"));
        });
  }

  @Test
  public void resolveMissingProperty() throws Exception {
    new MockUnit(Locals.class)
        .expect(unit -> {
          Locals locals = unit.get(Locals.class);
          expect(locals.get("prop")).andReturn(Optional.empty());
        })
        .run(
            unit -> {
              assertEquals(ValueResolver.UNRESOLVED,
                  new LocalsValueResolver().resolve(unit.get(Locals.class), "prop"));
            });
  }

  @Test
  public void skipNoLocals() throws Exception {
    new MockUnit(Locals.class)
        .run(
            unit -> {
              assertEquals(ValueResolver.UNRESOLVED,
                  new LocalsValueResolver().resolve(new Object(), "prop"));
            });
  }

  @Test
  public void resolveContext() throws Exception {
    new MockUnit(Locals.class)
        .run(unit -> {
          assertEquals(unit.get(Locals.class),
              new LocalsValueResolver().resolve(unit.get(Locals.class)));
        });
  }

  @Test
  public void resolveNoLocals() throws Exception {
    new MockUnit()
        .run(unit -> {
          assertEquals(ValueResolver.UNRESOLVED,
              new LocalsValueResolver().resolve(new Object()));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void propertySet() throws Exception {
    Set<Entry<String, Object>> entries = new HashSet<>();
    new MockUnit(Locals.class)
        .expect(unit -> {
          Locals locals = unit.get(Locals.class);
          Map<String, Object> attributes = unit.mock(Map.class);
          expect(locals.attributes()).andReturn(attributes);
          expect(attributes.entrySet()).andReturn(entries);
        })
        .run(unit -> {
          assertEquals(entries,
              new LocalsValueResolver().propertySet(unit.get(Locals.class)));
        });
  }

  @Test
  public void propertySetAnything() throws Exception {
    new MockUnit(Locals.class)
        .run(unit -> {
          assertEquals(Collections.emptySet(),
              new LocalsValueResolver().propertySet(new Object()));
        });
  }

}
