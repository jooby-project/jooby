/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.internal.pac4j;

import java.net.URI;

import org.jooby.MediaType;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route.Chain;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.http.profile.HttpProfile;

public class FormFilter extends AuthFilter {

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
      "  <form name=\"form\" action=\"%s\" method=\"POST\">\n" +
      "    <input name=\"username\" onkeypress=\"onKeyPressEvent(event)\" value=\"%s\" />\n" +
      "    <p></p>\n" +
      "    <input type=\"password\" name=\"password\" onkeypress=\"onKeyPressEvent(event)\" />\n" +
      "    <p></p>\n" +
      "    <input type=\"submit\" value=\"Submit\" />\n" +
      "  </form>\n" +
      "</body>\n" +
      "</html>\n";

  private String loginUrl;

  private String callback;

  public FormFilter(final String loginUrl, final String callback) {
    super(FormClient.class, HttpProfile.class);
    this.loginUrl = loginUrl;
    this.callback = URI.create(callback).getPath();
  }

  @Override
  public void handle(final Request req, final Response rsp, final Chain chain) throws Exception {
    String error = req.param("error").toOptional().orElse("");
    String username = req.param("username").toOptional().orElse("");

    req.set("username", username);
    req.set("error", error);

    if (loginUrl.equals(req.path())) {
      // default login form
      rsp.type(MediaType.html).send(String.format(FORM, error, callback, username));
    } else {
      super.handle(req, rsp, chain);
    }
  }

}
