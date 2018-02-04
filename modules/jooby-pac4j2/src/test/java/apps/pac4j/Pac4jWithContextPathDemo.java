package apps.pac4j;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.jooby.Jooby;
import org.jooby.pac4j.Pac4j;
import org.pac4j.core.profile.UserProfile;

public class Pac4jWithContextPathDemo extends Jooby {

  {
    use(ConfigFactory.empty().withValue("application.path", ConfigValueFactory.fromAnyRef("/myapp")));

    use(new Pac4j().form());

    get("/", () -> "OK");

    get("/form", () -> "Form");

    get("/user", req -> {
      UserProfile profile = require(UserProfile.class);
      return profile;
    });
  }

  public static void main(String[] args) throws Exception {
    run(Pac4jWithContextPathDemo::new, args);
  }
}
