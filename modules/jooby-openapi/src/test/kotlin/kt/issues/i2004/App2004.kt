/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package kt.issues.i2004

import io.jooby.Kooby

class App2004 : Kooby({ mvc(Controller2004()) })
