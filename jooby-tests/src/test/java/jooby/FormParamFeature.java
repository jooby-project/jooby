package jooby;

import static org.junit.Assert.assertEquals;
import jooby.mvc.POST;
import jooby.mvc.Path;

import org.apache.http.client.fluent.Request;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Test;

public class FormParamFeature extends ServerFeature {

  @Path("/r")
  public static class Resource {

    @Path("/form")
    @POST
    public String text(final String name, final int age) {
      return name + " " + age;
    }

  }

  {
    post("/form", (req, resp) -> {
      String name = req.param("name").stringValue();
      int age = req.param("age").intValue();
      resp.send(name + " " + age);
    });

    route(Resource.class);
  }

  @Test
  public void form() throws Exception {
    assertEquals("edgar 34", Request.Post(uri("form").build())
        .bodyForm(
            new BasicNameValuePair("name", "edgar"), new BasicNameValuePair("age", "34")
        ).execute().returnContent().asString());

    assertEquals("edgar 34", Request.Post(uri("r", "form").build())
        .bodyForm(
            new BasicNameValuePair("name", "edgar"), new BasicNameValuePair("age", "34")
        ).execute().returnContent().asString());
  }

}
