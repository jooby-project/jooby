package starter.billing;

import javax.inject.Inject;

public class BillingService {
  private final TransactionLog transactionLog;

  @Inject BillingService(TransactionLog transactionLog) {
    this.transactionLog = transactionLog;
  }

  public Receipt chargeOrder(PizzaOrder order) {
    return new Receipt(transactionLog.save(order));
  }
}
