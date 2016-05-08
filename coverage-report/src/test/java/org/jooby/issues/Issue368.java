package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class Issue368 extends ServerFeature {

  public interface S {
    String doSomething();
  }

  public interface Sx {
    String doSomething();
  }

  public static class S1 implements S {

    @Override
    public String doSomething() {
      return "s1";
    }

  }

  public static class S12 implements S {
    private String v;

    public S12(final String v) {
      this.v = v;
    }

    @Override
    public String doSomething() {
      return v;
    }

  }

  public static class Sx12 implements Sx {
    private String v;

    public Sx12(final String v) {
      this.v = v;
    }

    @Override
    public String doSomething() {
      return v;
    }

  }

  public static class S2 {

    public String doSomething() {
      return "s2";
    }

  }

  public static class S3 {

    public String doSomething() {
      return "s3";
    }

  }

  public static class S4 implements S {

    @Override
    public String doSomething() {
      return "s4";
    }

  }

  {
    use(ConfigFactory.empty().withValue("v", ConfigValueFactory.fromAnyRef("s12")));

    bind(S2.class);
    bind(c -> new S12(c.getString("v")));
    bind(new S3());
    bind(S.class, S1.class);
    bind(Sx.class, c -> new Sx12(c.getString("v")));
    bind(S4.class, S4::new);

    get("/bind", req -> {
      return req.require(S2.class).doSomething()
          + ";" + req.require(S12.class).doSomething()
          + ";" + req.require(S3.class).doSomething()
          + ";" + req.require(S.class).doSomething()
          + ";" + req.require(Sx.class).doSomething()
          + ";" + req.require(S4.class).doSomething();
    });
  }

  @Test
  public void shouldSimplifyBinding() throws Exception {
    request()
        .get("/bind")
        .expect("s2;s12;s3;s1;s12;s4");
  }

}
