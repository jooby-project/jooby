package org.jooby.issues;

import org.jooby.json.Jackson;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue616 extends ServerFeature {

  public static class Person {

    // mandatory fields
    private String name = null;
    private String surname = null;

    // optional fields
    private String nickname;

    public Person(final String name, final String surname) {
      this.name = name;
      this.surname = surname;
    }

    public Person() {
    }

    public String getName() {
      return name;
    }

    public String getSurname() {
      return surname;
    }

    public String getNickname() {
      return nickname;
    }

    public void setNickname(final String nickname) {
      this.nickname = nickname;
    }

    @Override
    public String toString() {
      return name + " " + surname + "(" + nickname + ")";
    }
  }

  {
    use(new Jackson());
    post("/616", req -> {
      return req.body(Person.class).toString();
    });
  }

  @Test
  public void jacksonWithParamNamesModule() throws Exception {
    request()
        .post("/616")
        .body("{\"name\":\"N\",\"surname\":\"S\"}", "application/json")
        .expect("\"N S(null)\"");

    request()
        .post("/616")
        .body("{\"name\":\"N\",\"surname\":\"S\",\"nickname\":\".\"}", "application/json")
        .expect("\"N S(.)\"");
  }
}
