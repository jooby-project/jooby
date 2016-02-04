package apps;

import org.jooby.Jooby;
import org.jooby.MediaType;

public class SwaggerUI {

  public static void install(final String path, final Jooby app) {
    app.get(path + "/swagger.json",
        path + "/:tag/swagger.json",
        req -> null)
        .name("swagger(json)");

    app.get(path + "/swagger.yml", path + "/:tag/swagger.yml",
        req -> null)
        .name("swagger(yml)");

    app.get(path, path + "/:tag", () -> null)
        .name("swagger(html)")
        .produces(MediaType.html);
  }

}
