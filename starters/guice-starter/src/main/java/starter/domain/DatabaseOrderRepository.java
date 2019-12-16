package starter.domain;

import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class DatabaseOrderRepository implements OrderRepository {
  private Jdbi jdbi;
  private Logger log = LoggerFactory.getLogger(getClass());

  /**
   * Jdbi instance is provisioned by Guice using the Jooby module architecture.
   *
   * @param jdbi
   */
  @Inject
  public DatabaseOrderRepository(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override public Order save(Order order) {
    log.info("saving order: {}", order);

    Long orderId = jdbi.withHandle(h -> {
      return h.createUpdate("insert into orders (name) values (:name)")
          .bind("name", order.getName())
          .executeAndReturnGeneratedKeys()
          .mapTo(Long.class)
          .one();
    });
    order.setId(orderId);
    return order;
  }
}
