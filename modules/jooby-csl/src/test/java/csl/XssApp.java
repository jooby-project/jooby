package csl;

import org.jooby.Jooby;
import org.jooby.csl.XSS;

public class XssApp extends Jooby {

  {
    use(new XSS());

    get("/param", req -> "<html><body><a href=>" + req.param("p", "html").value() + "</html></body>");

    get("/h", req -> "<html><body>" + req.header("h", "html").value("<h1>X</h1>") + "</html></body>");

    get("*", req -> req.path(true));

  }

  public static void main(final String[] args) {
    run(XssApp::new, args);
  }
}
