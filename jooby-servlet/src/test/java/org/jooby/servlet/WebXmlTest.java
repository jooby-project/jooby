package org.jooby.servlet;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Test;

import com.google.common.io.CharStreams;

public class WebXmlTest {

  String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
      +
      "<web-app xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
      +
      "  xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_3_1.xsd\"\n"
      +
      "  version=\"3.1\">\n" +
      "  <context-param>\n" +
      "    <param-name>application.class</param-name>\n" +
      "    <param-value>${application.class}</param-value>\n" +
      "  </context-param>\n" +
      "\n" +
      "  <listener>\n" +
      "    <listener-class>%s</listener-class>\n" +
      "  </listener>\n" +
      "\n" +
      "  <servlet>\n" +
      "    <servlet-name>jooby</servlet-name>\n" +
      "    <servlet-class>%s</servlet-class>\n" +
      "    <load-on-startup>0</load-on-startup>\n" +
      "    <!-- MultiPart setup -->\n" +
      "    <multipart-config>\n" +
      "      <file-size-threshold>0</file-size-threshold>\n" +
      "      <max-request-size>${war.maxRequestSize}</max-request-size>\n" +
      "    </multipart-config>\n" +
      "  </servlet>\n" +
      "\n" +
      "  <servlet-mapping>\n" +
      "    <servlet-name>jooby</servlet-name>\n" +
      "    <url-pattern>/*</url-pattern>\n" +
      "  </servlet-mapping>\n" +
      "</web-app>\n";

  @Test
  public void webXmlMustHaveServletDefinition() throws IOException {
    InputStream in = getClass().getResourceAsStream("/WEB-INF/web.xml");
    String webxml = CharStreams.toString(new InputStreamReader(in));
    in.close();

    assertEquals(
        String.format(body, ServerInitializer.class.getName(), ServletHandler.class.getName()),
        webxml);
  }
}
