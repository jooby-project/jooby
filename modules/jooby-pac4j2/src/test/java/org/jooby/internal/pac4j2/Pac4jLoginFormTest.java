package org.jooby.internal.pac4j2;

import static org.easymock.EasyMock.expect;
import org.jooby.MediaType;
import org.jooby.Mutant;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.test.MockUnit;
import org.junit.Test;

public class Pac4jLoginFormTest {

  @Test
  public void handle() throws Exception {
    new MockUnit(Request.class, Response.class)
        .expect(param("error", ""))
        .expect(param("username", ""))
        .expect(form("<!DOCTYPE html>\n"
            + "<html>\n"
            + "<head>\n"
            + "<title>Login Page</title>\n"
            + "<script type=\"text/javascript\">\n"
            + "\n"
            + "  function submitForm() {\n"
            + "    document.form.submit();\n"
            + "  }\n"
            + "\n"
            + "  function onKeyPressEvent(event) {\n"
            + "    var key = event.keyCode || event.which;\n"
            + "    if (key === 13) {\n"
            + "      if (event.preventDefault) {\n"
            + "        event.preventDefault();\n"
            + "      } else {\n"
            + "        event.returnValue = false;\n"
            + "      }\n"
            + "\n"
            + "      submitForm('');\n"
            + "      return false;\n"
            + "    }\n"
            + "  }\n"
            + "</script>\n"
            + "</head>\n"
            + "<body onload=\"document.form.username.focus();\">\n"
            + "  <h3>Login</h3>\n"
            + "  <p style=\"color: red;\">\n"
            + "  \n"
            + "  </p>\n"
            + "  <form name=\"form\" action=\"/auth?client_name=FormClient\" method=\"POST\">\n"
            + "    <input name=\"username\" onkeypress=\"onKeyPressEvent(event)\" value=\"\" />\n"
            + "    <p></p>\n"
            + "    <input type=\"password\" name=\"password\" onkeypress=\"onKeyPressEvent(event)\" />\n"
            + "    <p></p>\n"
            + "    <input type=\"submit\" value=\"Submit\" />\n"
            + "  </form>\n"
            + "</body>\n"
            + "</html>\n"))
        .run(unit -> {
          new Pac4jLoginForm("/auth")
              .handle(unit.get(Request.class), unit.get(Response.class));
        });
  }

  @Test
  public void handleWithContextPath() throws Exception {
    new MockUnit(Request.class, Response.class)
        .expect(param("error", "Invalid Credentials"))
        .expect(param("username", "bar"))
        .expect(form("<!DOCTYPE html>\n"
            + "<html>\n"
            + "<head>\n"
            + "<title>Login Page</title>\n"
            + "<script type=\"text/javascript\">\n"
            + "\n"
            + "  function submitForm() {\n"
            + "    document.form.submit();\n"
            + "  }\n"
            + "\n"
            + "  function onKeyPressEvent(event) {\n"
            + "    var key = event.keyCode || event.which;\n"
            + "    if (key === 13) {\n"
            + "      if (event.preventDefault) {\n"
            + "        event.preventDefault();\n"
            + "      } else {\n"
            + "        event.returnValue = false;\n"
            + "      }\n"
            + "\n"
            + "      submitForm('');\n"
            + "      return false;\n"
            + "    }\n"
            + "  }\n"
            + "</script>\n"
            + "</head>\n"
            + "<body onload=\"document.form.username.focus();\">\n"
            + "  <h3>Login</h3>\n"
            + "  <p style=\"color: red;\">\n"
            + "  Invalid Credentials\n"
            + "  </p>\n"
            + "  <form name=\"form\" action=\"/app/auth?client_name=FormClient\" method=\"POST\">\n"
            + "    <input name=\"username\" onkeypress=\"onKeyPressEvent(event)\" value=\"bar\" />\n"
            + "    <p></p>\n"
            + "    <input type=\"password\" name=\"password\" onkeypress=\"onKeyPressEvent(event)\" />\n"
            + "    <p></p>\n"
            + "    <input type=\"submit\" value=\"Submit\" />\n"
            + "  </form>\n"
            + "</body>\n"
            + "</html>\n"))
        .run(unit -> {
          new Pac4jLoginForm("/app/auth")
              .handle(unit.get(Request.class), unit.get(Response.class));
        });
  }

  private MockUnit.Block form(String page) {
    return unit -> {
      Response rsp = unit.get(Response.class);
      expect(rsp.type(MediaType.html)).andReturn(rsp);
      rsp.send(page);
    };
  }

  private MockUnit.Block param(String name, String value) {
    return unit -> {
      Mutant error = unit.mock(Mutant.class);
      expect(error.value("")).andReturn(value);

      Request req = unit.get(Request.class);
      expect(req.param(name)).andReturn(error);

      expect(req.set(name, value)).andReturn(req);
    };
  }
}
