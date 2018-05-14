package netty;

import com.google.common.io.ByteStreams;
import org.jooby.Jooby;
import org.jooby.MediaType;
import org.jooby.Results;
import org.jooby.funzy.Throwing;
import org.jooby.funzy.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class H2Netty extends Jooby {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  Throwing.Function<String, String> html = Throwing.<String, String>throwingFunction(path -> {
    return Try.with(() -> getClass().getResourceAsStream(path))
        .apply(in -> {
          byte[] bytes = ByteStreams.toByteArray(in);
          return new String(bytes, StandardCharsets.UTF_8);
        }).get();
  }).memoized();

  {
    http2();
    securePort(8443);

    use("*", (req, rsp) -> {
      log.info("************ {} ************", req.path());
    });

    assets("/assets/**");
    get("/", req -> {
      req.push("/assets/index.js");
      return Results.ok(html.apply("/index.html")).type(MediaType.html);
    });

  }

  public static void main(final String[] args) throws Throwable {
    run(H2Netty::new, args);
  }
}
