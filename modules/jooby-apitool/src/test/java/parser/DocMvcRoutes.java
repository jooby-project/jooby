package parser;

import org.jooby.mvc.Body;
import org.jooby.mvc.GET;
import org.jooby.mvc.Header;
import org.jooby.mvc.POST;
import org.jooby.mvc.Path;

import javax.inject.Named;

/**
 * MVC API.
 */
@Path("/mvc")
public class DocMvcRoutes {

  /**
   * MVC doIt.
   *
   * @param q Query string. Like: <code>q=foo</code>
   * @param offset
   * @param max
   * @param id
   * @param body
   * @return Sterinv value.
   */
  @POST
  @GET
  public String doIt(final String q, final int offset, @Named("Max") final int max,
      @Header("ID") final String id, @Body final Foo body) {
    return "dot";
  }
}
