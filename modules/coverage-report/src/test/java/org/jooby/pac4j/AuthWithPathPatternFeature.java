package org.jooby.pac4j;

import org.jooby.test.ServerFeature;
import org.junit.Test;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.creator.AuthenticatorProfileCreator;
import org.pac4j.http.client.direct.DirectBasicAuthClient;
import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator;

import java.util.Optional;

public class AuthWithPathPatternFeature extends ServerFeature {

    {
        use(new Auth()
                .client(config ->
                        new DirectBasicAuthClient(
                                new SimpleTestUsernamePasswordAuthenticator(),
                                new AuthenticatorProfileCreator())
                ));
        use("/private", new AuthFilter("IndirectBasicAuthClient"));

        get("/hello", () -> "hi");

        get("/private", req -> {
            ProfileManager profileManager = req.require(ProfileManager.class);

            Optional<CommonProfile> op = profileManager.get(true);
            return op.map(CommonProfile::getId).orElse("no user id");
        });
    }

    @Test
    public void noauth() throws Exception {
        request()
                .get("/hello")
                .expect("hi");
    }

    @Test
    public void auth() throws Exception {
        request()
                .basic("test", "test")
                .get("/private")
                .expect("test");
    }
}
