package apps;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.jooby.Deferred;
import org.jooby.mvc.Body;
import org.jooby.mvc.POST;
import org.jooby.mvc.Path;
import parser.Foo;

import java.util.List;
import java.util.Optional;

@TAnno(strategy = Foo.class)
public class Controller611 {

  /**
   * Find users by email address.
   *
   * @param userId User ID.
   * @param context Context value.
   * @param emails List of emails.
   * @return Returns a {@link List} of {@link User611}.
   */
  @POST
  @Path("/friends/email")
  public Deferred findUsersByEmail(final Long userId, final Optional<String> context,
      @Body final ArrayNode emails) {
    return Deferred.deferred(deferred -> {
      return null;
    });
  }
}
