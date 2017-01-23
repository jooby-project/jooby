package swagger;

import java.util.List;
import java.util.Optional;

import org.jooby.Deferred;
import org.jooby.mvc.Body;
import org.jooby.mvc.POST;
import org.jooby.mvc.Path;

import com.fasterxml.jackson.databind.node.ArrayNode;

public class Controller611 {

  /**
   * Find users by email address.
   *
   * @param userId User ID.
   * @param context Context value.
   * @param emails {@link List} of {@link String mails}.
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
