package org.jooby.issues;

import java.util.Date;

import org.jooby.Parser;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue408 extends ServerFeature {

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
    parser(Parser.bean(true));

    get("/408", req -> req.params().to(Bean408.class).toString());
  }

  @Test
  public void shouldIgnoreEmptyValues() throws Exception {
    request()
        .get("/408?id=1&title=Title&releaseDate=")
        .expect("1:Title:null");

    request()
        .get("/408")
        .expect("null:null:null");
  }

}
