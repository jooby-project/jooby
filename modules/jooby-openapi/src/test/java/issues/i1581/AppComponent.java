/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i1581;

import issues.i1580.Controller1580;

public class AppComponent {
  public Controller1580 myController() {
    return new Controller1580();
  }
}
