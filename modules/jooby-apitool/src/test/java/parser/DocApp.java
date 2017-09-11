package parser;

import org.jooby.Jooby;

/**
 * Documented class.
 */
public class DocApp extends Jooby {

  {

    /**
     * List all the TODO Items.
     * TODO API.
     */
    use("/api/todo")
        /**
         * Get under use.
         *
         * @return <code>200</code> or <code>404: Not found</code>.
         */
        .get(req -> {
          return "Success";
        });
  }
}
