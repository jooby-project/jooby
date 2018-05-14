package apps.pac4j;

import org.jooby.Jooby;
import org.jooby.pac4j.Pac4j;
import org.pac4j.core.authorization.authorizer.ProfileAuthorizer;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.HttpAction;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.http.client.direct.HeaderClient;
import org.pac4j.http.credentials.authenticator.test.SimpleTestTokenAuthenticator;

import java.util.List;

public class Pac4jGuiceAuthorizerDemo extends Jooby {

  public static class MyAuhtorizer extends ProfileAuthorizer<CommonProfile> {

    @Override protected boolean isProfileAuthorized(WebContext context, CommonProfile profile)
        throws HttpAction {
      return profile.getId().startsWith("oo");
    }

    @Override public boolean isAuthorized(WebContext context, List<CommonProfile> profiles)
        throws HttpAction {
      return isAnyAuthorized(context, profiles);
    }
  }

  {
    use(new Pac4j().client("*", MyAuhtorizer.class, conf ->
            new HeaderClient("jwt-unique", new SimpleTestTokenAuthenticator())
        ));

    get("/", () -> "OK");

    get("/form", () -> "Form");

    get("/user", req -> {
      UserProfile profile = require(UserProfile.class);
      return profile.toString() + req.ifSession().isPresent();
    });

    get("/pm", req -> {
      ProfileManager<CommonProfile> pm = require(ProfileManager.class);
      return pm.getAll(true);
    });
  }

  public static void main(String[] args) throws Exception {
    run(Pac4jGuiceAuthorizerDemo::new, args);
  }
}
