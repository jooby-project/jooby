/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3853;

import java.util.ArrayList;
import java.util.List;

import io.jooby.Jooby;
import io.jooby.Projected;

public class App3853Script extends Jooby {
  public static class Service {
    public List<U3853> list() {
      return null;
    }
  }

  {
    get(
        "/3853/{id}",
        ctx -> {
          var id = ctx.path("id").intValue();
          return Projected.wrap(U3853.createUser()).include("(id, name)");
        });

    get(
        "/3853/list-variable",
        ctx -> {
          List<U3853> list = new ArrayList<>();
          list.add(U3853.createUser());
          return Projected.wrap(list).include("(id, name)");
        });

    get(
        "/3853/list-variable-var",
        ctx -> {
          var list = new ArrayList<U3853>();
          list.add(U3853.createUser());
          return Projected.wrap(list).include("(id, name)");
        });

    get(
        "/3853/list-call",
        ctx -> {
          var service = ctx.require(Service.class);
          return Projected.wrap(service.list()).include("(id, name)");
        });

    get(
        "/3853/list-call-variable",
        ctx -> {
          var service = ctx.require(Service.class);
          var result = service.list();

          return Projected.wrap(result).include("(id, name)");
        });

    get(
        "/3853/list",
        ctx -> {
          return Projected.wrap(List.of(U3853.createUser())).include("(id, name)");
        });

    get(
        "/3853/address",
        ctx -> {
          return Projected.wrap(U3853.createUser()).include("(id, address(*))");
        });

    get(
        "/3853/address/partial",
        ctx -> {
          return Projected.wrap(U3853.createUser()).include("(id, address(city))");
        });
  }
}
