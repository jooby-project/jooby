package io.jooby.i3787;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.jooby.Jooby;
import io.jooby.problem.HttpProblem;

import java.util.Map;

class Application extends Jooby {
  {
    Config problemDetailsConfig = ConfigFactory.parseMap(
        Map.of("problem.details.enabled", true)
    );
    getEnvironment().setConfig(problemDetailsConfig.withFallback(getConfig()));

    get("/throw", ctx -> {
      throw new CustomException();
    });

    error(CustomException.class, (ctx, throwable, statusCode) -> {
      var problem = HttpProblem.badRequest("A Client Error — Obviously");
      ctx.getRouter().getErrorHandler().apply(ctx, problem, statusCode);
    });
  }

  public static void main(String[] args) {
    runApp(args, Application::new);
  }

  static class CustomException extends RuntimeException {
  }
}
