package kt.i1905

import io.jooby.Kooby

fun staticCreateApp(): SubApp1905 {
  return SubApp1905()
}

class App1905 : Kooby({

  fun instanceCreateApp(): SubApp1905 {
    return SubApp1905()
  }

  install(::SubApp1905)

  install("/static/ref", ::staticCreateApp)

  install("/instance/ref", ::instanceCreateApp)

  install("/supplier") {
    val app = SubApp1905()
    print(app)
    app
  }


})
