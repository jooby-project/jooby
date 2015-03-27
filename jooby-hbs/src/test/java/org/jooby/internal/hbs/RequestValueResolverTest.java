package org.jooby.internal.hbs;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.jooby.MockUnit;
import org.jooby.Request;
import org.junit.Test;

import com.github.jknack.handlebars.ValueResolver;

public class RequestValueResolverTest {

  @Test
  public void resolveProperty() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request Request = unit.get(Request.class);
          expect(Request.get("prop")).andReturn(Optional.of("x"));
        })
        .run(unit -> {
          assertEquals("x", new RequestValueResolver().resolve(unit.get(Request.class), "prop"));
        });
  }

  @Test
  public void resolveMissingProperty() throws Exception {
    new MockUnit(Request.class)
        .expect(unit -> {
          Request Request = unit.get(Request.class);
          expect(Request.get("prop")).andReturn(Optional.empty());
        })
        .run(
            unit -> {
              assertEquals(ValueResolver.UNRESOLVED,
                  new RequestValueResolver().resolve(unit.get(Request.class), "prop"));
            });
  }

  @Test
  public void skipNoRequest() throws Exception {
    new MockUnit(Request.class)
        .run(
            unit -> {
              assertEquals(ValueResolver.UNRESOLVED,
                  new RequestValueResolver().resolve(new Object(), "prop"));
            });
  }

  @Test
  public void resolveContext() throws Exception {
    new MockUnit(Request.class)
        .run(unit -> {
          assertEquals(unit.get(Request.class),
              new RequestValueResolver().resolve(unit.get(Request.class)));
        });
  }

  @Test
  public void resolveNoRequest() throws Exception {
    new MockUnit()
        .run(unit -> {
          assertEquals(ValueResolver.UNRESOLVED,
              new RequestValueResolver().resolve(new Object()));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void propertySet() throws Exception {
    Set<Entry<String, Object>> entries = new HashSet<>();
    new MockUnit(Request.class)
        .expect(unit -> {
          Request Request = unit.get(Request.class);
          Map<String, Object> attributes = unit.mock(Map.class);
          expect(Request.attributes()).andReturn(attributes);
          expect(attributes.entrySet()).andReturn(entries);
        })
        .run(unit -> {
          assertEquals(entries,
              new RequestValueResolver().propertySet(unit.get(Request.class)));
        });
  }

  @Test
  public void propertySetAnything() throws Exception {
    new MockUnit(Request.class)
        .run(unit -> {
          assertEquals(Collections.emptySet(),
              new RequestValueResolver().propertySet(new Object()));
        });
  }

}
