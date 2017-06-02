package morphia;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.EntityListeners;
import org.mongodb.morphia.annotations.Id;

@Entity
@EntityListeners(MyListener.class)
public class Beer {

  @Id
  public String id;

  public String name;
}
