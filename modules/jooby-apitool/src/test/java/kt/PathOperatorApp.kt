package kt

import org.jooby.Kooby
import parser.Foo

class PathOperatorApp : Kooby({

  /**
   * Summary API.
   */
  path("/api/path") {
    /** List all. */
    get {
      val foos = listOf<Foo>()
      foos
    }

    /**
     * List one.
     * @param id ID.
     * @return Foo.
     */
    get("/:id") {
      val id = param("id").intValue()
      Foo(id)
    }
  }

})
