package apps.flashscope;

import java.util.concurrent.atomic.AtomicInteger;

import org.jooby.FlashScope;
import org.jooby.Jooby;
import org.jooby.Results;
import org.jooby.hbs.Hbs;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class FlashScopeApp extends Jooby {

  {
    use(ConfigFactory.empty().withValue("server.module",
        ConfigValueFactory.fromAnyRef("org.jooby.undertow.Undertow")));

    use(new Hbs("/apps/flashscope"));

    use(new FlashScope());

    get("/", () -> Results.html("flash"));

    get("/send", req -> {
      req.flash("success", req.param("message").value("It works"));
      return Results.redirect("/");
    });

    AtomicInteger inc = new AtomicInteger(100);
    get("/toggle", req -> {
      return req.ifFlash("n").orElseGet(() -> {
        String v = Integer.toString(inc.incrementAndGet());
        req.flash("n", v);
        return v;
      });
    });
  }

  public static void main(final String[] args) throws Throwable {
    run(FlashScopeApp::new, args);
  }
}
