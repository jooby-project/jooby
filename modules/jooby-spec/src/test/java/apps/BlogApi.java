package apps;

import org.jooby.Jooby;

public class BlogApi extends Jooby {

  {
    /**
     * Get by ID.
     */
    get("/:id", req -> {
      int id = req.param("id").intValue();
      DB db = req.require(DB.class);
      Blog result = db.find(id);
      return result;
    });
  }

}
