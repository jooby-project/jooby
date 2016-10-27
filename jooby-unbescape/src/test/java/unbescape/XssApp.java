package unbescape;

import org.jooby.Jooby;
import org.jooby.unbescape.XSS;

public class XssApp extends Jooby {

  {
    use(new XSS());

    get("/param", req -> "<html><body><a href=>" + req.param("p", "html").value() + "</html></body>");

    get("/h", req -> "<html><body>" + req.header("h", "html").value());

    get("*", req -> req.path(true));

  }

  public static void main(final String[] args) {
    run(XssApp::new, args);
  }
}
