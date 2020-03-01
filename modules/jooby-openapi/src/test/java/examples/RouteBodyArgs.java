package examples;

import io.jooby.Jooby;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RouteBodyArgs extends Jooby {
  {
    get("/str", ctx -> {
      String str = ctx.body().value();
      return "...";
    });

    get("/int", ctx -> {
      int i = ctx.body().intValue();
      return "...";
    });

    get("/listOfStr", ctx -> {
      List<String> listStr = ctx.body().toList();
      return "...";
    });

    get("/listOfDouble", ctx -> {
      List<Double> listType = ctx.body().toList(Double.class);
      return "...";
    });

    get("/defstr", ctx -> {
      String defstr = ctx.body().value("200");
      return "...";
    });

    get("/opt-int", ctx -> {
      Optional<Integer> intoptional = ctx.body().toOptional(Integer.class);
      return "...";
    });

    get("/body-bean", ctx -> {
      ABean bean = ctx.body(ABean.class);
      return "...";
    });

    get("/body-bean2", ctx -> {
      ABean bean = ctx.body().to(ABean.class);
      return "...";
    });
  }
}
