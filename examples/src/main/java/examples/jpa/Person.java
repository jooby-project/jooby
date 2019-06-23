/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples.jpa;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class Person {
  @Id
  @GeneratedValue
  private Long id;

  private String fooBar;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getFooBar() {
    return fooBar;
  }

  public void setFooBar(String fooBar) {
    this.fooBar = fooBar;
  }
}
