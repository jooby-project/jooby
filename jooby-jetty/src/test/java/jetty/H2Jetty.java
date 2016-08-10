package jetty;

import java.nio.charset.StandardCharsets;

import org.jooby.Jooby;
import org.jooby.MediaType;
import org.jooby.Results;

import com.google.common.io.ByteStreams;

import javaslang.Lazy;
import javaslang.control.Try;

public class H2Jetty extends Jooby {

  Lazy<String> html = Lazy.of(() -> {
    return Try.of(() -> {
      byte[] bytes = ByteStreams.toByteArray(getClass().getResourceAsStream("/index.html"));
      return new String(bytes, StandardCharsets.UTF_8);
    }).get();
  });

  {
    http2();
    securePort(8443);

    assets("/assets/**");
    get("/", req -> {
      req.push("/assets/index.js");
      return Results.ok(html.get()).type(MediaType.html);
    });

  }

  public static void main(final String[] args) throws Throwable {
    run(H2Jetty::new, args);
  }
}
