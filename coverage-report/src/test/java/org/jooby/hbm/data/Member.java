package org.jooby.hbm.data;

import java.util.Optional;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

@Entity
public class Member {

  @Id
  private int id;

  private String name;

  @Transient
  public Optional<Boolean> viewErr = Optional.empty();

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setId(final int id) {
    this.id = id;
  }

  public void setName(final String name) {
    this.name = name;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof Member) {
      return id == ((Member) obj).id;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return id;
  }

  @Override
  public String toString() {
    if (viewErr.isPresent()) {
      throw new NullPointerException("err while rendering object");
    }
    return name + "(" + id + ")";
  }
}
