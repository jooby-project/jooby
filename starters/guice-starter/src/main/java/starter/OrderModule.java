package starter;

import com.google.inject.AbstractModule;
import starter.domain.DatabaseOrderRepository;
import starter.domain.OrderRepository;

public class OrderModule extends AbstractModule {
  @Override
  protected void configure() {
    /*
     * This tells Guice that whenever it sees a dependency on a TransactionLog,
     * it should satisfy the dependency using a DatabaseTransactionLog.
     */
    bind(OrderRepository.class).to(DatabaseOrderRepository.class);
  }
}
