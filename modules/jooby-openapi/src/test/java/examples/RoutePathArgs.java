package examples;

import io.jooby.Jooby;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RoutePathArgs extends Jooby {
  {
    get("/", ctx -> {
      String str = ctx.path("str").value();
      int i = ctx.path("i").intValue();
      List<String> listStr = ctx.path("listStr").toList();
      List<Double> listType = ctx.path("listType").toList(Double.class);
      String defstr = ctx.path("defstr").value("200");
      int defint = ctx.path("defint").intValue(87);
      int defint0 = ctx.path("defint0").intValue(0);
      boolean defbool = ctx.path("defbool").booleanValue(true);
      Optional<String> defoptional = ctx.path("optstr").toOptional();
      Optional<Integer> intoptional = ctx.path("optint").toOptional(Integer.class);
      String str2 = ctx.path("optstr2").toOptional().orElse("optional");
      Integer toInt = ctx.path("toI").to(Integer.class);
      Letter l = ctx.path("letter").toEnum(Letter::valueOf);
      Map<String, String> q = ctx.path("path").toMap();
      Map<String, List<String>> qa = ctx.path("pathList").toMultimap();
      ABean bean = ctx.path(ABean.class);
      return "...";
    });
  }
}
