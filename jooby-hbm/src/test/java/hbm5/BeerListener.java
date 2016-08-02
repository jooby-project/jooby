package hbm5;

import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;

public class BeerListener {

  @Inject
  public BeerListener(final EntityManagerFactory emf) {
    System.out.println(emf);
  }

  @PostUpdate
  @PostPersist
  @PostLoad
  void after(final Beer beer) {
    System.out.println("XXX " + beer);
  }
}
