package parser;

import org.jooby.Jooby;

public class EdgeApp extends Jooby {
  {
    /**
     * Edge comment 1
     */
    use("*", "*", (req, rsp) -> {

    });

    /** Edge comment 2 */
    use("*", (req, rsp, chain) -> {

    });
  }
}
