package apps;

import org.jooby.mvc.Body;
import org.jooby.mvc.Header;
import org.jooby.mvc.POST;
import org.jooby.mvc.Path;

import javax.inject.Named;

/**
 * MVC API.
 */
@Path("/mvc")
public class Routes947 {

  /**
   * MVC doIt.
   *
   * @param q Query string. Like: <code>q=foo</code>
   * @return Sterinv value.
   */
  @POST
  public String doIt(final String q) {
    return "dot";
  }
}
