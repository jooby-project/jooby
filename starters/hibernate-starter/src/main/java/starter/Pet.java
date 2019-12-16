package starter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Objects;

@Entity
public class Pet {

  @Id
  @GeneratedValue
  private int id;

  private String name;

  public Pet(final String name) {
    this.name = name;
  }

  public Pet() {
  }

  public int getId() {
    return id;
  }

  public void setId(final int id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  @Override public boolean equals(Object that) {
    if (that instanceof Pet) {
      return Objects.equals(getId(), ((Pet) that).getId());
    }
    return false;
  }

  @Override public int hashCode() {
    return Objects.hash(getId());
  }
}
