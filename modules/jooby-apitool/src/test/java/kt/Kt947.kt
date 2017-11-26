package kt

import apps.Routes947
import org.jooby.Kooby

class Kt947: Kooby( {
    use("/kpath", Routes947::class)
})
