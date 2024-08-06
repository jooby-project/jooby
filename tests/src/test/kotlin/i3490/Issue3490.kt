/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package i3490

import io.jooby.junit.ServerTest
import io.jooby.junit.ServerTestRunner
import io.jooby.kt.Kooby

class Issue3490 {
  @ServerTest
  fun shouldBootComplexGenericTypes(runner: ServerTestRunner) =
    runner
      .use { Kooby { mvc(C3490_()) } }
      .ready { _ ->
        // NOOP
      }
}
