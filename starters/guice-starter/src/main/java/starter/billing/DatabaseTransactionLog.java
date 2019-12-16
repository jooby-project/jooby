package starter.billing;

import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class DatabaseTransactionLog implements TransactionLog {
  private Jdbi jdbi;
  private Logger log = LoggerFactory.getLogger(getClass());

  /**
   * Jdbi instance is provisioned by Guice using the Jooby module architecture.
   *
   * @param jdbi
   */
  @Inject
  public DatabaseTransactionLog(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override public long save(PizzaOrder order) {
    log.info("saving order: {}", order);

    return jdbi.withHandle(h -> {
      return h.createUpdate("insert into pizza (type, count, credit_card) "
          + "values (:type, :count, :creditCard)")
          .bind("type", order.getType())
          .bind("count", order.getCount())
          .bind("creditCard", order.getCreditCard())
          .executeAndReturnGeneratedKeys()
          .mapTo(Long.class)
          .one();
    });
  }
}
