package org.jooby.issues;

import java.util.List;
import java.util.Optional;

import org.jooby.Parser;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue483 extends ServerFeature {

  public static class Member {
    String firstname;

    String lastname;

    @Override
    public String toString() {
      return firstname + " " + lastname;
    }
  }

  public static class Group {

    List<Member> members;

    @Override
    public String toString() {
      return Optional.ofNullable(members).map(it -> it.toString()).orElse("[]");
    }

  }

  public static class NullableBean {

    String foo;

    Optional<String> bar;

    @Override
    public String toString() {
      return foo + bar;
    }
  }

  {
    parser(Parser.bean(true));

    get("/483/dot", req -> {
      return req.params().toList(Member.class);
    });

    get("/483/nested", req -> {
      return req.params(Group.class);
    });

    get("/483/null", req -> {
      return req.params(NullableBean.class).toString();
    });

  }

  @Test
  public void dotNotation() throws Exception {
    request()
        .get("/483/dot?0.firstname=Pedro&0.lastname=PicaPiedra")
        .expect("[Pedro PicaPiedra]");

    request()
        .get("/483/nested?members.0.firstname=Pedro&members.0.lastname=PicaPiedra")
        .expect("[Pedro PicaPiedra]");

  }

  @Test
  public void documentNullBeanInjection() throws Exception {
    request()
        .get("/483/null?foo=foo")
        .expect("foonull");

    request()
        .get("/483/null?foo=foo&bar")
        .expect("fooOptional[]");

    request()
        .get("/483/null?foo=foo&bar=bar")
        .expect("fooOptional[bar]");
  }

}
