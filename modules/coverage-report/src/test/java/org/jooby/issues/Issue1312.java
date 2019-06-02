package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Issue1312 extends ServerFeature {

  public static class Person {
    String name;

    String country;

    Person child;


    @Override
    public String toString() {
      String data = name + " " + country;
      if (child!=null) {
        data += "; child {" + child.toString() + "}";
      }
      return data;
    }
  }

  {
    get("/", req -> {
      return req.params(Person.class);
    });

  }

  @Test
  public void rootList() throws Exception {
    request()
        .get("/?name=P&country=AR&child.name=X&child.country=UY")
        .expect("[Pedro PicaPiedra]");
  }

}
