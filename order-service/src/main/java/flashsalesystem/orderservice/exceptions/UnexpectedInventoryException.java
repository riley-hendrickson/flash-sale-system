package flashsalesystem.orderservice.exceptions;

public class UnexpectedInventoryException extends RuntimeException {
  public UnexpectedInventoryException(String message) {
    super(message);
  }
}
