package io.jooby.i2413;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.jooby.MediaType;
import io.jooby.WebClient;
import io.jooby.json.JacksonModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue2413 {
  @ServerTest
  public void shouldGenerateJsonAndXmlDefaultsJson(ServerTestRunner runner) {
    runner.define(app -> {
      app.install(new JacksonModule(new ObjectMapper()));

      app.install(new JacksonModule(new XmlMapper()));

      app.get("/2413", ctx -> new B2413("someId", "someName"));
    }).ready((WebClient http) -> {

      http.header("Accept", "application/json")
          .get("/2413", rsp -> {
            assertEquals("application/json;charset=utf-8", rsp.body().contentType().toString().toLowerCase());
            assertEquals("{\"id\":\"someId\",\"name\":\"someName\"}", rsp.body().string());
          });

      http.header("Accept", "application/xml")
          .get("/2413", rsp -> {
            assertEquals("application/xml;charset=utf-8", rsp.body().contentType().toString().toLowerCase());
            assertEquals("<B2413><id>someId</id><name>someName</name></B2413>",
                rsp.body().string());
          });

      http
          .get("/2413", rsp -> {
            assertEquals("application/json;charset=utf-8", rsp.body().contentType().toString().toLowerCase());
            assertEquals("{\"id\":\"someId\",\"name\":\"someName\"}", rsp.body().string());
          });
    });
  }

  @ServerTest
  public void shouldGenerateJsonAndXmlDefaultsXml(ServerTestRunner runner) {
    runner.define(app -> {
      app.install(new JacksonModule(new XmlMapper(), MediaType.xml));

      app.install(new JacksonModule(new ObjectMapper()));

      app.get("/2413", ctx -> new B2413("someId", "someName"));
    }).ready((WebClient http) -> {

      http.header("Accept", "application/json")
          .get("/2413", rsp -> {
            assertEquals("application/json;charset=utf-8", rsp.body().contentType().toString().toLowerCase());
            assertEquals("{\"id\":\"someId\",\"name\":\"someName\"}", rsp.body().string());
          });

      http.header("Accept", "application/xml")
          .get("/2413", rsp -> {
            assertEquals("application/xml;charset=utf-8", rsp.body().contentType().toString().toLowerCase());
            assertEquals("<B2413><id>someId</id><name>someName</name></B2413>",
                rsp.body().string());
          });

      http
          .get("/2413", rsp -> {
            assertEquals("application/xml;charset=utf-8", rsp.body().contentType().toString().toLowerCase());
            assertEquals("<B2413><id>someId</id><name>someName</name></B2413>",
                rsp.body().string());
          });
    });
  }
}
