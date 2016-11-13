package app;

import org.jooby.Jooby;
import org.jooby.banner.Banner;
import org.jooby.crash.Crash;
import org.jooby.crash.HttpShellPlugin;
import org.jooby.json.Jackson;

public class CrashApp extends Jooby {
  {
    conf("crash.conf");

    use(new Jackson());

    use(new Banner("crash me!"));

    use(new Crash()
        .plugin(HttpShellPlugin.class)
        .plugin(AuthPlugin.class));

    before("/path", (req, rsp) -> {
    });

    after("/path", (req, rsp, v) -> {
      return v;
    });

    complete("/path", (req, rsp, v) -> {
    });

    get("/", () -> "OK");

  }

  public static void main(final String[] args) {
    run(CrashApp::new, args);
  }
}
