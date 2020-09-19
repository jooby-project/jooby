package kt

import io.jooby.Kooby

class KtMvcObjectApp : Kooby({

  mvc(KtObjectController)

})
