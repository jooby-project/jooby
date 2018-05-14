package apps.pac4j;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.jooby.Jooby;
import org.jooby.pac4j.Pac4j;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.oauth.profile.facebook.FacebookProfile;

public class Pac4jFacebookDemo extends Jooby {

  {
    use(ConfigFactory.empty().withValue("application.path", ConfigValueFactory.fromAnyRef("/myapp")));

    use(new Pac4j().client("*", conf -> {
      final FacebookClient client = new FacebookClient("145278422258960",
          "be21409ba8f39b5dae2a7de525484da8");
      return client;
    }));

    get("/", () -> "OK");

    get("/form", () -> "Form");

    get("/user", req -> {
      FacebookProfile profile = require(FacebookProfile.class);
      return profile.toString() + req.ifSession().isPresent();
    });
  }

  public static void main(String[] args) throws Exception {
    run(Pac4jFacebookDemo::new, args);
  }
}
