/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.Route;
import io.jooby.pac4j.Pac4jContext;
import org.pac4j.core.config.Config;
import org.pac4j.core.http.url.UrlResolver;

import javax.annotation.Nonnull;

public class DevLoginForm implements Route.Handler {

  private static final String FORM = "<!DOCTYPE html>\n" +
      "<html>\n" +
      "<head>\n" +
      "<title>Login Page</title>\n" +
      "<script type=\"text/javascript\">\n" +
      "\n" +
      "  function submitForm() {\n" +
      "    document.form.submit();\n" +
      "  }\n" +
      "\n" +
      "  function onKeyPressEvent(event) {\n" +
      "    var key = event.keyCode || event.which;\n" +
      "    if (key === 13) {\n" +
      "      if (event.preventDefault) {\n" +
      "        event.preventDefault();\n" +
      "      } else {\n" +
      "        event.returnValue = false;\n" +
      "      }\n" +
      "\n" +
      "      submitForm('');\n" +
      "      return false;\n" +
      "    }\n" +
      "  }\n" +
      "</script>\n" +
      "</head>\n" +
      "<body onload=\"document.form.username.focus();\">\n" +
      "  <h3>Login</h3>\n" +
      "  <p style=\"color: red;\">\n" +
      "  %s\n" +
      "  </p>\n" +
      "  <form name=\"form\" action=\"%s?client_name=FormClient\" method=\"POST\">\n" +
      "    <input name=\"username\" onkeypress=\"onKeyPressEvent(event)\" value=\"%s\" />\n" +
      "    <p></p>\n" +
      "    <input type=\"password\" name=\"password\" onkeypress=\"onKeyPressEvent(event)\" />\n" +
      "    <p></p>\n" +
      "    <input type=\"submit\" value=\"Submit\" />\n" +
      "  </form>\n" +
      "</body>\n" +
      "</html>\n";

  private Config pac4j;

  private String callbackPath;

  public DevLoginForm(Config pac4j, String callbackPath) {
    this.pac4j = pac4j;
    this.callbackPath = callbackPath;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) throws Exception {
    String error = ctx.query("error").value("");
    String username = ctx.query("username").value("");

    ctx.attribute("username", username);
    ctx.attribute("error", error);

    UrlResolver urlResolver = pac4j.getClients().getUrlResolver();
    String url = urlResolver.compute(callbackPath, Pac4jContext.create(ctx));
    // default login form
    return ctx.setResponseType(MediaType.html)
        .send(String.format(FORM, error, url, username));
  }
}
