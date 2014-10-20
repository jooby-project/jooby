package org.jooby;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class User {

  @Id
  private String id;

  private String firstName;

  private String lastName;

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setFirstName(final String firstName) {
    this.firstName = firstName;
  }

  public void setLastName(final String lastName) {
    this.lastName = lastName;
  }

}
