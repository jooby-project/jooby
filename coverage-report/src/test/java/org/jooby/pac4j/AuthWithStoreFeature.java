package org.jooby.pac4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Singleton;

import org.jooby.test.ServerFeature;
import org.junit.Test;
import org.pac4j.http.profile.HttpProfile;

public class AuthWithStoreFeature extends ServerFeature {

  @Singleton
  public static class InMemory implements AuthStore<HttpProfile> {

    private Map<String, HttpProfile> profiles = new HashMap<>();

    @Override
    public Optional<HttpProfile> get(final String id) throws Exception {
      return Optional.of(profiles.get(id));
    }

    @Override
    public void set(final HttpProfile profile) throws Exception {
      profiles.put(profile.getId(), profile);

    }

    @Override
    public Optional<HttpProfile> unset(final String id) throws Exception {
      return Optional.ofNullable(profiles.remove(id));
    }

  }

  {

    use(new Auth().form().store(InMemory.class));

    get("/", req -> req.require(HttpProfile.class).getId());
  }

  @Test
  public void auth() throws Exception {
    request()
        .get("/auth?username=test&password=test")
        .expect("test");
  }

}
