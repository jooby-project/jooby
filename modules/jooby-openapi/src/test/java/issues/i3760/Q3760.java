/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3760;

import jakarta.validation.constraints.Size;

public class Q3760 {

  private String text;

  @Size(min = 10, max = 1000) public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }
}
