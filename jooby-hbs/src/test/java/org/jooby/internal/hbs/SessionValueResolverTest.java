package org.jooby.internal.hbs;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.jooby.MockUnit;
import org.jooby.Mutant;
import org.jooby.Session;
import org.junit.Test;

import com.github.jknack.handlebars.ValueResolver;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

public class SessionValueResolverTest {

  @Test
  public void resolveProperty() throws Exception {
    new MockUnit(Session.class)
        .expect(unit -> {
          Session session = unit.get(Session.class);
          Mutant v = unit.mock(Mutant.class);
          expect(v.toOptional()).andReturn(Optional.of("x"));
          expect(session.get("prop")).andReturn(v);
        })
        .run(unit -> {
          assertEquals("x", new SessionValueResolver().resolve(unit.get(Session.class), "prop"));
        });
  }

  @Test
  public void resolveMissingProperty() throws Exception {
    new MockUnit(Session.class)
        .expect(unit -> {
          Session session = unit.get(Session.class);
          Mutant v = unit.mock(Mutant.class);
          expect(v.toOptional()).andReturn(Optional.empty());
          expect(session.get("prop")).andReturn(v);
        })
        .run(
            unit -> {
              assertEquals(ValueResolver.UNRESOLVED,
                  new SessionValueResolver().resolve(unit.get(Session.class), "prop"));
            });
  }

  @Test
  public void skipSession() throws Exception {
    new MockUnit(Session.class)
        .run(
            unit -> {
              assertEquals(ValueResolver.UNRESOLVED,
                  new SessionValueResolver().resolve(new Object(), "prop"));
            });
  }

  @Test
  public void resolveContext() throws Exception {
    new MockUnit(Session.class)
        .run(unit -> {
          assertEquals(unit.get(Session.class),
              new SessionValueResolver().resolve(unit.get(Session.class)));
        });
  }

  @Test
  public void resolveNoSession() throws Exception {
    new MockUnit()
        .run(unit -> {
          assertEquals(ValueResolver.UNRESOLVED,
              new SessionValueResolver().resolve(new Object()));
        });
  }

  @Test
  public void propertySet() throws Exception {
    Map<String, String> attributes = ImmutableMap.of("k", "v");
    Set<Entry<String, Object>> entries = ImmutableSet.of(Maps.immutableEntry("k", "v"));
    new MockUnit(Session.class)
        .expect(unit -> {
          Session Rsession = unit.get(Session.class);

          expect(Rsession.attributes()).andReturn(attributes);
        })
        .run(unit -> {
          assertEquals(entries,
              new SessionValueResolver().propertySet(unit.get(Session.class)));
        });
  }

  @Test
  public void propertySetAnything() throws Exception {
    new MockUnit(Session.class)
        .run(unit -> {
          assertEquals(Collections.emptySet(),
              new SessionValueResolver().propertySet(new Object()));
        });
  }

}
