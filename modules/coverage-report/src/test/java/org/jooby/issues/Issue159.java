package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue159 extends ServerFeature {

  public static class Country {
    private String name;

    private String city;

    @Override
    public String toString() {
      return name + "(" + city + ")";
    }
  }

  public static class Address {
    private Country country;

    private String line1;

    @Override
    public String toString() {
      return line1 + "(" + country + ")";
    }
  }

  public static class Profile {

    private String username;

    private Address address;

    @Override
    public String toString() {
      return username + "(" + address + ")";
    }
  }

  {

    get("/beanparser/nested", req -> {
      return req.params().to(Profile.class);
    });
  }

  @Test
  public void nested() throws Exception {
    request()
        .get(
            "/beanparser/nested?username=xyz&address[country][name]=AR&address[line1]=Line1&address[country][city]=BA")
        .expect("xyz(Line1(AR(BA)))");

  }
}
