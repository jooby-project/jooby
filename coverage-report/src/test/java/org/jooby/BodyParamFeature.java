package org.jooby;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.jooby.Body;
import org.jooby.MediaType;
import org.jooby.mvc.POST;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
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

    param((type, values, ctx) -> {
      if (values != null) {
        return new Bean(values[0]);
      }
      return ctx.convert(type, values);
    });

    use(new Body.Formatter() {

      @Override
      public void format(final Object body, final Body.Writer writer) throws Exception {
        writer.text(out -> new CharSink() {
          @Override
          public Writer openStream() throws IOException {
            return out;
          }
        }.write(body.toString()));
      }

      @Override
      public List<MediaType> types() {
        return ImmutableList.of(MediaType.json);
      }

      @Override
      public boolean canFormat(final Class<?> type) {
        return type == Bean.class;
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
