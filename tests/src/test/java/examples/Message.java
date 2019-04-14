package examples;

public class Message {
  private final String value;

  public Message(String value) {
    this.value = value;
  }

  @Override public String toString() {
    return value;
  }
}
