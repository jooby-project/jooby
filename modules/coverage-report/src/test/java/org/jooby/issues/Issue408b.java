package org.jooby.issues;

import java.util.Date;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue408b extends ServerFeature {

  public static class Bean408 {
    public Integer id;

    public String title;

    public Date releaseDate;

    @Override
    public String toString() {
      return id + ":" + title + ":" + releaseDate;
    }
  }

  {
    get("/408", req -> req.params(Bean408.class).toString());
  }

  @Test
  public void shouldIgnoreEmptyValues() throws Exception {
    request()
        .get("/408?id=1&title=Title")
        .expect("1:Title:null");

    request()
        .get("/408?title=Title")
        .expect("null:Title:null");

    request()
        .get("/408")
        .expect("null:null:null");
  }

}
