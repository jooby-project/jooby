package org.jooby;

import static org.junit.Assert.assertEquals;

import javax.inject.Singleton;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;

public class ParamRequireFeature extends ServerFeature {

  public static class ParamBean {
    @Override
    public String toString() {
      return "OK";
    }
  }

  @Singleton
  public static class Dependency {
  }

  {

    param((type, values, ctx) -> {
      if (type.getRawType() == ParamBean.class) {
        Dependency dep1 = ctx.require(Dependency.class);
        assertEquals(dep1, ctx.require(Key.get(Dependency.class)));
        assertEquals(dep1, ctx.require(TypeLiteral.get(Dependency.class)));
        return new ParamBean();
      }
      return ctx.convert(type, values);
    });

    get("/require", req -> {
      return req.require(ParamBean.class);
    });

  }

  @Test
  public void require() throws Exception {
    request()
        .get("/require")
        .expect("OK");
  }

}
