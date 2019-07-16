package kt

import org.jooby.Kooby

class App1300 : Kooby({
  get("route") {
    123
  }

  use("subroute", SubRoute1300())
})
