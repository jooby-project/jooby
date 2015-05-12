package org.jooby;

import java.io.IOException;

import org.jooby.mvc.POST;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.common.io.CharSink;

public class BodyParamFeature extends ServerFeature {

  private static class Bean {
    private Object value;

    public Bean(final Object value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value.toString();
    }
  }

  @Path("/r")
  public static class Resource {

    @Path("/body")
    @POST
    public Bean body(final Bean param) throws IOException {
      return param;
    }

  }

  {

    parser((type, ctx) -> {
      if (type.getRawType() == Bean.class) {
        return ctx.param(values -> new Bean(values.get(0)));
      }
      return ctx.next();
    });

    renderer((object, ctx) -> {
      if (ctx.accepts("json") && object instanceof Bean) {
        ctx.text(out -> new CharSink() {
          @Override
          public java.io.Writer openStream() throws IOException {
            return out;
          }
        }.write(object.toString()));
      }
    });

    post("/body", (req, resp) -> {
      Bean bean = req.param("param").to(Bean.class);
      resp.send(bean);
    });

    use(Resource.class);
  }

  @Test
  public void multipart() throws Exception {
    request()
        .post("/body")
        .multipart()
        .add("param", "{}", "application/json")
        .expect("{}");

    request()
        .post("/r/body")
        .multipart()
        .add("param", "{}", "application/json")
        .expect("{}");

  }

}
