package apps.pac4j;

import org.jooby.Jooby;
import org.jooby.pac4j.Pac4j;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.UserProfile;

public class Pac4jCustomConfigDemo extends Jooby {

  {
    use(new Pac4j().doWith(pac4j -> {
      // configure pac4j
    }));

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
    run(Pac4jCustomConfigDemo::new, args);
  }
}
