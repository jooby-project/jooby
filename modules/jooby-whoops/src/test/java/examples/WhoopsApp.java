package examples;

import generator.GenerateHTML;
import io.jooby.ErrorHandler;
import io.jooby.Jooby;
import io.jooby.MediaType;
import io.jooby.whoops.Whoops;

import java.nio.file.Path;
import java.nio.file.Paths;

public class WhoopsApp extends Jooby {
  {
    GenerateHTML.create().generate();
    get("/{id}", ctx -> {
      Object id = ctx.path("id").intValue();
      return function(id);
    });

    assets("/whoops/*", "/whoops");
    error((ctx, cause, statusCode) -> {
      Whoops whoops = new Whoops(basedir());
      Whoops.Result result = whoops.render(ctx, cause, statusCode);
      if (result.failure == null) {
        if (result.output != null) {
          getLog().error(ErrorHandler.errorMessage(ctx, statusCode), cause);
          ctx.setResponseType(MediaType.html).send(result.output);
        }
      } else {
        getLog().error("whoops resulted into new exception", result.failure);
      }
    });
  }

  private Object function(Object id) {
    try {
      return innerFunction(id);
    } catch (Exception x) {
      throw new IllegalStateException("Something Happened", x);
    }
  }

  private Object innerFunction(Object id) {
    throw new IllegalArgumentException("Something Broke!");
  }

  public static void main(String[] args) {
    runApp(args, WhoopsApp::new);
  }

  private static Path basedir() {
    Path basedir = Paths.get(System.getProperty("user.dir"));
    if (!basedir.getFileName().toString().equals("jooby-whoops")) {
      // IDE vs Maven
      basedir = basedir.resolve("modules").resolve("jooby-whoops");
    }
    return basedir;
  }
}
