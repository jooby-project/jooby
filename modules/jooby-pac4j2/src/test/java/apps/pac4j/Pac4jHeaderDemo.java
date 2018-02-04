package apps.pac4j;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.jooby.Jooby;
import org.jooby.pac4j.Pac4j;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.http.client.direct.HeaderClient;
import org.pac4j.http.credentials.authenticator.test.SimpleTestTokenAuthenticator;

public class Pac4jHeaderDemo extends Jooby {

  {
    use(ConfigFactory.empty().withValue("application.path", ConfigValueFactory.fromAnyRef("/myapp")));

    use(new Pac4j().client(conf-> {
      return new HeaderClient("jwt-unique", new SimpleTestTokenAuthenticator());
    }));

    get("/", () -> "OK");

    get("/form", () -> "Form");

    get("/user", req -> {
      UserProfile profile = require(UserProfile.class);
      return profile.toString() + req.ifSession().isPresent();
    });

    get("/pm", req -> {
      ProfileManager<CommonProfile> pm = require(ProfileManager.class);
      return pm.getAll(true );
    });
  }

  public static void main(String[] args) throws Exception {
    run(Pac4jHeaderDemo::new, args);
  }
}
