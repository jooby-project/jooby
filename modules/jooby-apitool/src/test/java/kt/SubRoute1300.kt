package kt

import org.jooby.Kooby

class SubRoute1300 : Kooby({
  get("hello") {
    "word"
  }
})
