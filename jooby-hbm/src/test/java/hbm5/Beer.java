package hbm5;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@EntityListeners(BeerListener.class)
public class Beer {
  @Id
  @GeneratedValue
  public Long id;

  public String name;
}
