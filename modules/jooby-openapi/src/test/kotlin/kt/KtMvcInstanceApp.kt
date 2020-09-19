package kt

import io.jooby.Kooby

class KtMvcInstanceApp : Kooby({

  mvc(KtController())

})
