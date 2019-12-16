package starter.service;

import starter.domain.Order;
import starter.domain.OrderRepository;

import javax.inject.Inject;

public class BillingService {
  private final OrderRepository transactionLog;

  @Inject public BillingService(OrderRepository transactionLog) {
    this.transactionLog = transactionLog;
  }

  public Order chargeOrder(Order order) {
    return transactionLog.save(order);
  }
}
