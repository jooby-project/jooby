package hbm5;

import javax.inject.Inject;

import org.hibernate.SessionFactory;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PostLoadEventListener;

public class BeerLoaded implements PostLoadEventListener {

  @Inject
  public BeerLoaded(final SessionFactory sf) {
    // TODO Auto-generated constructor stub
  }

  /**
   *
   */
  private static final long serialVersionUID = 7020624895466166085L;

  @Override
  public void onPostLoad(final PostLoadEvent event) {
    System.out.println(System.identityHashCode(this));
    System.out.println(event);
  }

}
