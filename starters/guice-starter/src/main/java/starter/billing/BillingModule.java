package starter.billing;

import com.google.inject.AbstractModule;

public class BillingModule extends AbstractModule {
  @Override
  protected void configure() {
    /*
     * This tells Guice that whenever it sees a dependency on a TransactionLog,
     * it should satisfy the dependency using a DatabaseTransactionLog.
     */
    bind(TransactionLog.class).to(DatabaseTransactionLog.class);
  }
}
