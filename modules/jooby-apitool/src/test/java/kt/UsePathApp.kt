package kt

import org.jooby.*
import parser.*

class UsePathApp : Kooby({

  /**
   * Summary API.
   */
  path("/api/path") {
    /** List all. */
    get({->
      val foos = listOf<Foo>()
      foos
    })

    /**
     * List one.
     * @param id ID.
     * @return Foo.
     */
    get("/:id") {->
      val id = param("id").intValue
      Foo(id)
    }
  }

})
