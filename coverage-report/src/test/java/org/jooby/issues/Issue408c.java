package org.jooby.issues;

import java.util.Date;

import org.jooby.Parser;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue408c extends ServerFeature {

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

    get("/408", req -> req.params(Bean408.class).toString());

    err((req, rsp, err) -> {
      rsp.send(err.getMessage());
    });
  }

  @Test
  public void wrongValuesShouldBeReported() throws Exception {
    request()
        .get("/408?id=1&title=Title&releaseDate=*")
        .expect("Server Error(500): Failed to parse parameter 'releaseDate' to 'java.util.Date'");
  }

}
