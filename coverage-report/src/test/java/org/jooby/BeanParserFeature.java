package org.jooby;

import static org.junit.Assert.assertEquals;

import java.util.Optional;

import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class BeanParserFeature extends ServerFeature {

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

  public static class DefConstBean {
    public DefConstBean(final String arg) {
    }

    public DefConstBean() {
    }
  }

  @Path("/r")
  public static class Resource {

    @org.jooby.mvc.GET
    @Path("/ibean")
    public String getibean(final org.jooby.Request req, final IBean bean) throws Exception {
      assertEquals(req.param("name").value(), bean.name());
      assertEquals(req.param("valid").booleanValue(), bean.isValid());
      assertEquals(req.param("age").intValue(), bean.getAge());
      return "OK";
    }

    @org.jooby.mvc.POST
    @Path("/ibean")
    public String postibean(final org.jooby.Request req, final IBean bean) throws Exception {
      assertEquals(req.param("name").value(), bean.name());
      assertEquals(req.param("valid").booleanValue(), bean.isValid());
      assertEquals(req.param("age").intValue(), bean.getAge());
      return "OK";
    }

    @org.jooby.mvc.GET
    @Path("/beanwithargs")
    public String getbeanwithargs(final org.jooby.Request req, final BeanWithArgs bean)
        throws Exception {
      assertEquals(req.param("name").value(), bean.name);
      assertEquals(req.param("age").intValue(), (int) bean.age.get());
      return "OK";
    }

    @org.jooby.mvc.POST
    @Path("/beanwithargs")
    public String postbeanwithargs(final org.jooby.Request req, final BeanWithArgs bean)
        throws Exception {
      assertEquals(req.param("name").value(), bean.name);
      assertEquals(req.param("age").intValue(), (int) bean.age.get());
      return "OK";
    }

    @org.jooby.mvc.GET
    @Path("/beannoarg")
    public String getbeannoarg(final org.jooby.Request req, final BeanNoArg bean) throws Exception {
      assertEquals(req.param("name").value(), bean.getName());
      assertEquals(req.param("age").intValue(), (int) bean.getAge().get());
      return "OK";
    }

    @org.jooby.mvc.POST
    @Path("/beannoarg")
    public String postbeannoarg(final org.jooby.Request req, final BeanNoArg bean) throws Exception {
      assertEquals(req.param("name").value(), bean.getName());
      assertEquals(req.param("age").intValue(), (int) bean.getAge().get());
      return "OK";
    }

    @org.jooby.mvc.GET
    @Path("/defaultConstructor")
    public String invalidbean(final DefConstBean bean) throws Exception {
      return "OK";
    }

  }

  {
    get("/ibean", req -> {
      IBean bean = req.params().to(IBean.class);
      assertEquals(req.param("name").value(), bean.name());
      assertEquals(req.param("valid").booleanValue(), bean.isValid());
      assertEquals(req.param("age").intValue(), bean.getAge());
      return "OK";
    });

    post("/ibean", req -> {
      IBean bean = req.form(IBean.class);
      System.out.println(bean.name());

      assertEquals(req.param("name").value(), bean.name());
      assertEquals(req.param("valid").booleanValue(), bean.isValid());
      assertEquals(req.param("age").intValue(), bean.getAge());
      return "OK";
    });

    get("/beanwithargs", req -> {
      BeanWithArgs bean = req.params().to(BeanWithArgs.class);
      assertEquals(req.param("name").value(), bean.name);
      assertEquals(req.param("age").intValue(), (int) bean.age.get());
      return "OK";
    });

    get("/defaultConstructor", req -> {
      req.form(DefConstBean.class);
      return "OK";
    });

    post("/beanwithargs", req -> {
      BeanWithArgs bean = req.form(BeanWithArgs.class);
      assertEquals(req.param("name").value(), bean.name);
      assertEquals(req.param("age").intValue(), (int) bean.age.get());
      return "OK";
    });

    get("/beannoarg", req -> {
      BeanNoArg bean = req.params().to(BeanNoArg.class);
      assertEquals(req.param("name").value(), bean.getName());
      assertEquals(req.param("age").intValue(), (int) bean.getAge().get());
      return "OK";
    });

    post("/beannoarg", req -> {
      BeanNoArg bean = req.form(BeanNoArg.class);
      assertEquals(req.param("name").value(), bean.getName());
      assertEquals(req.param("age").intValue(), (int) bean.getAge().get());
      return "OK";
    });

    use(Resource.class);
  }

  @Test
  public void getibean() throws Exception {
    request()
        .get("/ibean?name=edgar&valid=true&age=17")
        .expect("OK");

    request()
        .get("/r/ibean?name=edgar&valid=true&age=17")
        .expect("OK");

  }

  @Test
  public void getbeanwithargs() throws Exception {
    request()
        .get("/beanwithargs?name=edgar&age=17")
        .expect("OK");

    request()
        .get("/r/beanwithargs?name=edgar&age=17")
        .expect("OK");

  }

  @Test
  public void getbeannoarg() throws Exception {
    request()
        .get("/beannoarg?name=edgar&age=17")
        .expect("OK");

    request()
        .get("/r/beannoarg?name=edgar&age=17")
        .expect("OK");

  }

  @Test
  public void defaultConstructor() throws Exception {
    request()
        .get("/defaultConstructor?name=edgar&age=17")
        .expect(200);

    request()
        .get("/r/defaultConstructor?name=edgar&age=17")
        .expect(200);

  }

  @Test
  public void postibean() throws Exception {
    request()
        .post("/ibean")
        .form()
        .add("name", "edgar")
        .add("age", 34)
        .add("valid", false)
        .expect("OK");

    request()
        .post("/r/ibean")
        .form()
        .add("name", "edgar")
        .add("age", 34)
        .add("valid", false)
        .expect("OK");

    request()
        .post("/ibean")
        .multipart()
        .add("name", "edgar")
        .add("age", 34)
        .add("valid", false)
        .expect("OK");

    request()
        .post("/r/ibean")
        .multipart()
        .add("name", "edgar")
        .add("age", 34)
        .add("valid", false)
        .expect("OK");

  }

  @Test
  public void postbeanwithargs() throws Exception {
    request()
        .post("/beanwithargs")
        .form()
        .add("name", "edgar")
        .add("age", 34)
        .expect("OK");

    request()
        .post("/r/beanwithargs")
        .form()
        .add("name", "edgar")
        .add("age", 34)
        .expect("OK");

  }

  @Test
  public void postbeannoarg() throws Exception {
    request()
        .post("/beannoarg")
        .form()
        .add("name", "edgar")
        .add("age", 34)
        .expect("OK");

    request()
        .post("/r/beannoarg")
        .form()
        .add("name", "edgar")
        .add("age", 34)
        .expect("OK");

  }

}
