package io.jooby.i3787;

import io.jooby.Jooby;
import io.jooby.problem.HttpProblem;

class Application extends Jooby {
  {
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

  static class CustomException extends RuntimeException {}
}
