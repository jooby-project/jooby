package starter.billing;

public class PizzaOrder {
  private String type;

  private int count;

  private String creditCard;

  public PizzaOrder(String type, int count, String creditCard) {
    this.type = type;
    this.count = count;
    this.creditCard = creditCard;
  }

  public String getType() {
    return type;
  }

  public int getCount() {
    return count;
  }

  public String getCreditCard() {
    return creditCard;
  }

  @Override public String toString() {
    return "PizzaOrder{" +
        "type='" + type + '\'' +
        ", count=" + count +
        '}';
  }
}
