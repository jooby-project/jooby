package org.jooby.integration;

import static org.junit.Assert.assertEquals;

import java.util.Optional;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class RequestBeanParamFeature extends ServerFeature {

  public interface IBean {

    String name();

    boolean isValid();

    int getAge();
  }

  public static class BeanWithArgs {

    public static String ignored = "ignored";

    public final String name;

    public final Optional<Integer> age;

    public BeanWithArgs(final String name, final Optional<Integer> age) {
      this.name = name;
      this.age = age;
    }
  }

  public static class BeanNoArg {

    private String name;

    public Optional<Integer> age;

    public Optional<Integer> getAge() {
      return age;
    }

    public String getName() {
      return name;
    }

  }

  public static class InvalidBean {
    public InvalidBean(final String arg) {
    }

    public InvalidBean() {
    }
  }

  @Path("/r")
  public static class Resource {

    @org.jooby.mvc.GET
    @Path("/ibean")
    public String getibean(final org.jooby.Request req, final IBean bean) throws Exception {
      assertEquals(req.param("name").stringValue(), bean.name());
      assertEquals(req.param("valid").booleanValue(), bean.isValid());
      assertEquals(req.param("age").intValue(), bean.getAge());
      return "OK";
    }

    @org.jooby.mvc.POST
    @Path("/ibean")
    public String postibean(final org.jooby.Request req, final IBean bean) throws Exception {
      assertEquals(req.param("name").stringValue(), bean.name());
      assertEquals(req.param("valid").booleanValue(), bean.isValid());
      assertEquals(req.param("age").intValue(), bean.getAge());
      return "OK";
    }

    @org.jooby.mvc.GET
    @Path("/beanwithargs")
    public String getbeanwithargs(final org.jooby.Request req, final BeanWithArgs bean)
        throws Exception {
      assertEquals(req.param("name").stringValue(), bean.name);
      assertEquals(req.param("age").intValue(), (int) bean.age.get());
      return "OK";
    }

    @org.jooby.mvc.POST
    @Path("/beanwithargs")
    public String postbeanwithargs(final org.jooby.Request req, final BeanWithArgs bean)
        throws Exception {
      assertEquals(req.param("name").stringValue(), bean.name);
      assertEquals(req.param("age").intValue(), (int) bean.age.get());
      return "OK";
    }

    @org.jooby.mvc.GET
    @Path("/beannoarg")
    public String getbeannoarg(final org.jooby.Request req, final BeanNoArg bean) throws Exception {
      assertEquals(req.param("name").stringValue(), bean.getName());
      assertEquals(req.param("age").intValue(), (int) bean.getAge().get());
      return "OK";
    }

    @org.jooby.mvc.POST
    @Path("/beannoarg")
    public String postbeannoarg(final org.jooby.Request req, final BeanNoArg bean) throws Exception {
      assertEquals(req.param("name").stringValue(), bean.getName());
      assertEquals(req.param("age").intValue(), (int) bean.getAge().get());
      return "OK";
    }

    @org.jooby.mvc.GET
    @Path("/invalidbean")
    public String invalidbean(final InvalidBean bean) throws Exception {
      return "OK";
    }

  }

  {
    get("/ibean", req -> {
      IBean bean = req.params(IBean.class);
      assertEquals(req.param("name").stringValue(), bean.name());
      assertEquals(req.param("valid").booleanValue(), bean.isValid());
      assertEquals(req.param("age").intValue(), bean.getAge());
      return "OK";
    });

    post("/ibean", req -> {
      IBean bean = req.body(IBean.class);
      assertEquals(req.param("name").stringValue(), bean.name());
      assertEquals(req.param("valid").booleanValue(), bean.isValid());
      assertEquals(req.param("age").intValue(), bean.getAge());
      return "OK";
    });

    get("/beanwithargs", req -> {
      BeanWithArgs bean = req.params(BeanWithArgs.class);
      assertEquals(req.param("name").stringValue(), bean.name);
      assertEquals(req.param("age").intValue(), (int) bean.age.get());
      return "OK";
    });

    get("/invalidbean", req -> {
      req.params(InvalidBean.class);
      return "OK";
    });

    post("/beanwithargs", req -> {
      BeanWithArgs bean = req.body(BeanWithArgs.class);
      assertEquals(req.param("name").stringValue(), bean.name);
      assertEquals(req.param("age").intValue(), (int) bean.age.get());
      return "OK";
    });

    get("/beannoarg", req -> {
      BeanNoArg bean = req.params(BeanNoArg.class);
      assertEquals(req.param("name").stringValue(), bean.getName());
      assertEquals(req.param("age").intValue(), (int) bean.getAge().get());
      return "OK";
    });

    post("/beannoarg", req -> {
      BeanNoArg bean = req.body(BeanNoArg.class);
      assertEquals(req.param("name").stringValue(), bean.getName());
      assertEquals(req.param("age").intValue(), (int) bean.getAge().get());
      return "OK";
    });

    use(Resource.class);
  }

  @Test
  public void getibean() throws Exception {
    assertEquals(
        "OK",
        GET(uri("ibean").addParameter("name", "edgar").addParameter("valid", "true")
            .addParameter("age", "17")));

    assertEquals(
        "OK",
        GET(uri("r", "ibean").addParameter("name", "edgar").addParameter("valid", "true")
            .addParameter("age", "17")));
  }

  @Test
  public void getbeanwithargs() throws Exception {
    assertEquals(
        "OK",
        GET(uri("beanwithargs").addParameter("name", "edgar")
            .addParameter("age", "17")));

    assertEquals(
        "OK",
        GET(uri("r", "beanwithargs").addParameter("name", "edgar")
            .addParameter("age", "17")));
  }

  @Test
  public void getbeannoarg() throws Exception {
    assertEquals(
        "OK",
        GET(uri("beannoarg").addParameter("name", "edgar")
            .addParameter("age", "17")));

    assertEquals(
        "OK",
        GET(uri("r", "beannoarg").addParameter("name", "edgar")
            .addParameter("age", "17")));
  }

  @Test
  public void invalidbean() throws Exception {
    HttpResponse rsp = Request.Get(uri("invalidbean").addParameter("name", "edgar")
        .addParameter("age", "17").build()).execute().returnResponse();
    assertEquals(400, rsp.getStatusLine().getStatusCode());

    HttpResponse rsp2 = Request.Get(uri("r", "invalidbean").addParameter("name", "edgar")
        .addParameter("age", "17").build()).execute().returnResponse();
    assertEquals(400, rsp2.getStatusLine().getStatusCode());

  }

  @Test
  public void postibean() throws Exception {
    assertEquals("OK", Request.Post(uri("ibean").build())
        .bodyForm(
            new BasicNameValuePair("name", "edgar"),
            new BasicNameValuePair("age", "34"),
            new BasicNameValuePair("valid", "false")
        ).execute().returnContent().asString());

    assertEquals("OK", Request.Post(uri("r", "ibean").build())
        .bodyForm(
            new BasicNameValuePair("name", "edgar"),
            new BasicNameValuePair("age", "34"),
            new BasicNameValuePair("valid", "false")
        ).execute().returnContent().asString());

    // multidata
    assertEquals("OK",
        Request.Post(uri("ibean").build())
            .body(MultipartEntityBuilder.create()
                .addTextBody("name", "edgar")
                .addTextBody("age", "34")
                .addTextBody("valid", "true")
                .build()).execute().returnContent().asString());
    assertEquals("OK",
        Request.Post(uri("r", "ibean").build())
            .body(MultipartEntityBuilder.create()
                .addTextBody("name", "edgar")
                .addTextBody("age", "34")
                .addTextBody("valid", "true")
                .build()).execute().returnContent().asString());
  }

  @Test
  public void bean415() throws Exception {

    // multidata
    assertEquals(415, Request.Post(uri("ibean").build())
        .addHeader("Content-Type", "application/xml")
        .body(MultipartEntityBuilder.create()
            .addTextBody("name", "edgar")
            .addTextBody("age", "34")
            .addTextBody("valid", "true")
            .build()).execute().returnResponse().getStatusLine().getStatusCode());
    assertEquals(415,
        Request.Post(uri("r", "ibean").build())
            .addHeader("Content-Type", "application/xml")
            .body(MultipartEntityBuilder.create()
                .addTextBody("name", "edgar")
                .addTextBody("age", "34")
                .addTextBody("valid", "true")
                .build()).execute().returnResponse().getStatusLine().getStatusCode());
  }

  @Test
  public void postbeanwithargs() throws Exception {
    assertEquals("OK", Request.Post(uri("beanwithargs").build())
        .bodyForm(
            new BasicNameValuePair("name", "edgar"),
            new BasicNameValuePair("age", "34")
        ).execute().returnContent().asString());

    assertEquals("OK", Request.Post(uri("r", "beanwithargs").build())
        .bodyForm(
            new BasicNameValuePair("name", "edgar"),
            new BasicNameValuePair("age", "34")
        ).execute().returnContent().asString());
  }

  @Test
  public void postbeannoarg() throws Exception {
    assertEquals("OK", Request.Post(uri("beannoarg").build())
        .bodyForm(
            new BasicNameValuePair("name", "edgar"),
            new BasicNameValuePair("age", "34")
        ).execute().returnContent().asString());

    assertEquals("OK", Request.Post(uri("r", "beannoarg").build())
        .bodyForm(
            new BasicNameValuePair("name", "edgar"),
            new BasicNameValuePair("age", "34")
        ).execute().returnContent().asString());
  }

  private static String GET(final URIBuilder uri) throws Exception {
    return Request.Get(uri.build()).execute().returnContent().asString();
  }

}
