package org.jooby.pac4j2;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Optional;

import javax.inject.Provider;

import org.jooby.Mutant;
import org.jooby.Session;
import org.jooby.internal.pac4j2.AuthSerializer;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pac4j.core.profile.CommonProfile;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AuthSessionStore.class, AuthSerializer.class })
public class AuthSessionStoreTest {

  @SuppressWarnings("unchecked")
  @Test
  public void defaults() throws Exception {
    new MockUnit(Provider.class)
        .run(unit -> {
          new AuthSessionStore<>(unit.get(Provider.class));
        });
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Test
  public void get() throws Exception {
    CommonProfile profile = new CommonProfile();

    new MockUnit(Provider.class, Session.class)
        .expect(unit -> {
          Provider provider = unit.get(Provider.class);
          expect(provider.get()).andReturn(unit.get(Session.class));
        })
        .expect(unit -> {
          unit.mockStatic(AuthSerializer.class);
          expect(AuthSerializer.strToObject("serialized")).andReturn(profile);

          Mutant ser = unit.mock(Mutant.class);
          expect(ser.toOptional()).andReturn(Optional.of("serialized"));

          Session session = unit.get(Session.class);
          expect(session.get("pac4jUserProfile.1")).andReturn(ser);
        })
        .run(unit -> {
          CommonProfile result = (CommonProfile) new AuthSessionStore(unit.get(Provider.class))
              .get("1").get();
          assertEquals(profile, result);
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
          Mutant ser = unit.mock(Mutant.class);
          expect(ser.toOptional()).andReturn(Optional.empty());

          Session session = unit.get(Session.class);
          expect(session.get("pac4jUserProfile.2")).andReturn(ser);
        })
        .run(unit -> {
          Optional<CommonProfile> profile = new AuthSessionStore(unit.get(Provider.class))
              .get("2");
          assertFalse(profile.isPresent());
        });
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Test
  public void set() throws Exception {
    CommonProfile profile = new CommonProfile();
    profile.setId("1");
    profile.addAttribute("username", "test");
    profile.addAttribute("email", "test@fake.com");
    profile.addPermission("p1");
    profile.addPermission("p2");
    profile.addRole("r1");

    new MockUnit(Provider.class, Session.class)
        .expect(unit -> {
          Provider provider = unit.get(Provider.class);
          expect(provider.get()).andReturn(unit.get(Session.class));
        })
        .expect(unit -> {
          unit.mockStatic(AuthSerializer.class);
          expect(AuthSerializer.objToStr(profile)).andReturn("serialized");

          Session session = unit.get(Session.class);
          expect(session.set("pac4jUserProfile.1", "serialized")).andReturn(session);
        })
        .run(unit -> {
          new AuthSessionStore(unit.get(Provider.class)).set(profile);
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void unset() throws Exception {
    CommonProfile profile = new CommonProfile();
    new MockUnit(Provider.class, Session.class)
        .expect(unit -> {
          Provider provider = unit.get(Provider.class);
          expect(provider.get()).andReturn(unit.get(Session.class));
        })
        .expect(unit -> {
          unit.mockStatic(AuthSerializer.class);
          expect(AuthSerializer.strToObject("serialized")).andReturn(profile);

          Mutant ser = unit.mock(Mutant.class);
          expect(ser.toOptional()).andReturn(Optional.of("serialized"));

          Session session = unit.get(Session.class);
          expect(session.unset("pac4jUserProfile.1")).andReturn(ser);
        })
        .run(unit -> {
          CommonProfile result = (CommonProfile) new AuthSessionStore(unit.get(Provider.class))
              .unset("1").get();

          assertEquals(profile, result);
        });
  }

}
