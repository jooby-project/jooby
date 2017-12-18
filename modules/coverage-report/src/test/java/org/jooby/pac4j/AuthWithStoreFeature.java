package org.jooby.pac4j;

import org.jooby.test.ServerFeature;
import org.junit.Test;
import org.pac4j.core.profile.CommonProfile;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AuthWithStoreFeature extends ServerFeature {

  @Singleton
  public static class InMemory implements AuthStore<CommonProfile> {

    private Map<String, CommonProfile> profiles = new HashMap<>();

    @Override
    public Optional<CommonProfile> get(final String id) throws Exception {
      return Optional.of(profiles.get(id));
    }

    @Override
    public void set(final CommonProfile profile) throws Exception {
      profiles.put(profile.getId(), profile);

    }

    @Override
    public Optional<CommonProfile> unset(final String id) throws Exception {
      return Optional.ofNullable(profiles.remove(id));
    }

  }

  {

    use(new Auth().form().store(InMemory.class));

    get("/", req -> req.require(CommonProfile.class).getId());
  }

  @Test
  public void auth() throws Exception {
    request()
        .get("/auth?username=test&password=test")
        .expect("test");
  }

}
