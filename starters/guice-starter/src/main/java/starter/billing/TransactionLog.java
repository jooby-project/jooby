package starter.billing;

public interface TransactionLog {
  long save(PizzaOrder order);
}
