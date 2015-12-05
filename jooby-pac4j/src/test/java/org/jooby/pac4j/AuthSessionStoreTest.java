package org.jooby.pac4j;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Map;
import java.util.Optional;

import javax.inject.Provider;

import org.jooby.Mutant;
import org.jooby.Session;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.UserProfile;

import com.google.common.collect.ImmutableMap;

public class AuthSessionStoreTest {

  @SuppressWarnings("unchecked")
  @Test
  public void defaults() throws Exception {
    new MockUnit(Provider.class)
        .run(unit -> {
          new AuthSessionStore(unit.get(Provider.class));
        });
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Test
  public void get() throws Exception {
    new MockUnit(Provider.class, Session.class)
        .expect(unit -> {
          Provider provider = unit.get(Provider.class);
          expect(provider.get()).andReturn(unit.get(Session.class));
        })
        .expect(unit -> {
          Mutant remembered = unit.mock(Mutant.class);
          expect(remembered.booleanValue()).andReturn(false);

          Mutant permissions = unit.mock(Mutant.class);
          expect(permissions.value()).andReturn("p1");

          Mutant roles = unit.mock(Mutant.class);
          expect(roles.value()).andReturn("r1");

          Session session = unit.get(Session.class);
          Map<String, String> attributes = ImmutableMap.of(
              "pac4jUserProfile.1.class", CommonProfile.class.getName(),
              "pac4jUserProfile.1.username", "test",
              "pac4jUserProfile.1.email", "test@fake.com"
              );

          expect(session.attributes()).andReturn(attributes);
          expect(session.get("pac4jUserProfile.1.remembered")).andReturn(remembered);
          expect(session.get("pac4jUserProfile.1.permissions")).andReturn(permissions);
          expect(session.get("pac4jUserProfile.1.roles")).andReturn(roles);
        })
        .run(unit -> {
          CommonProfile profile = (CommonProfile) new AuthSessionStore(unit.get(Provider.class))
              .get("1").get();
          assertNotNull(profile);
          assertEquals("1", profile.getId());
          assertEquals("test", profile.getUsername());
          assertEquals("test@fake.com", profile.getEmail());
          assertEquals("[p1]", profile.getPermissions().toString());
          assertEquals("[r1]", profile.getRoles().toString());
        });
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Test
  public void getNotFound() throws Exception {
    new MockUnit(Provider.class, Session.class)
        .expect(unit -> {
          Provider provider = unit.get(Provider.class);
          expect(provider.get()).andReturn(unit.get(Session.class));
        })
        .expect(unit -> {
          Session session = unit.get(Session.class);
          Map<String, String> attributes = ImmutableMap.of(
              "pac4jUserProfile.1.class", CommonProfile.class.getName(),
              "pac4jUserProfile.1.username", "test",
              "pac4jUserProfile.1.email", "test@fake.com"
              );

          expect(session.attributes()).andReturn(attributes);
        })
        .run(unit -> {
          Optional<UserProfile> profile = new AuthSessionStore(unit.get(Provider.class))
              .get("2");
          assertFalse(profile.isPresent());
        });
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Test
  public void set() throws Exception {
    new MockUnit(Provider.class, Session.class)
        .expect(unit -> {
          Provider provider = unit.get(Provider.class);
          expect(provider.get()).andReturn(unit.get(Session.class));
        })
        .expect(unit -> {
          Session session = unit.get(Session.class);
          expect(session.set("pac4jUserProfile.1.email", "test@fake.com")).andReturn(session);
          expect(session.set("pac4jUserProfile.1.username", "test")).andReturn(session);
          expect(session.set("pac4jUserProfile.1.class", CommonProfile.class.getName()))
              .andReturn(session);
          expect(session.set("pac4jUserProfile.1.remembered", false)).andReturn(session);
          expect(session.set("pac4jUserProfile.1.permissions", "p1__;_;p2")).andReturn(session);
          expect(session.set("pac4jUserProfile.1.roles", "r1")).andReturn(session);
        })
        .run(unit -> {
          CommonProfile profile = new CommonProfile();
          profile.setId("1");
          profile.addAttribute("username", "test");
          profile.addAttribute("email", "test@fake.com");
          profile.addPermission("p1");
          profile.addPermission("p2");
          profile.addRole("r1");

          new AuthSessionStore(unit.get(Provider.class)).set(profile);
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void unset() throws Exception {
    new MockUnit(Provider.class, Session.class)
        .expect(unit -> {
          Provider provider = unit.get(Provider.class);
          expect(provider.get()).andReturn(unit.get(Session.class)).times(2);
        })
        .expect(unit -> {
          Mutant remembered = unit.mock(Mutant.class);
          expect(remembered.booleanValue()).andReturn(true);

          Mutant permissions = unit.mock(Mutant.class);
          expect(permissions.value()).andReturn("p1");

          Mutant roles = unit.mock(Mutant.class);
          expect(roles.value()).andReturn("r1");

          Session session = unit.get(Session.class);
          Map<String, String> attributes = ImmutableMap.of(
              "pac4jUserProfile.1.class", CommonProfile.class.getName(),
              "pac4jUserProfile.1.username", "test",
              "pac4jUserProfile.1.email", "test@fake.com",
              "pac4jUserProfile.1.remembered", "true",
              "ignored", "y"
              );

          expect(session.attributes()).andReturn(attributes).times(2);
          expect(session.get("pac4jUserProfile.1.remembered")).andReturn(remembered);
          expect(session.get("pac4jUserProfile.1.permissions")).andReturn(permissions);
          expect(session.get("pac4jUserProfile.1.roles")).andReturn(roles);
        })
        .expect(
            unit -> {
              Session session = unit.get(Session.class);
              expect(session.unset("pac4jUserProfile.1.email")).andReturn(unit.mock(Mutant.class));
              expect(session.unset("pac4jUserProfile.1.username")).andReturn(
                  unit.mock(Mutant.class));
              expect(session.unset("pac4jUserProfile.1.class"))
                  .andReturn(unit.mock(Mutant.class));
              expect(session.unset("pac4jUserProfile.1.remembered")).andReturn(
                  unit.mock(Mutant.class));
            })
        .run(unit -> {
          CommonProfile profile = (CommonProfile) new AuthSessionStore(unit.get(Provider.class))
              .unset("1").get();

          assertNotNull(profile);
          assertEquals("1", profile.getId());
          assertEquals("test", profile.getUsername());
          assertEquals("test@fake.com", profile.getEmail());
        });
  }

}
