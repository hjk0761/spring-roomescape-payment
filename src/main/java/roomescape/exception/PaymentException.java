package roomescape.exception;

public class PaymentException extends RuntimeException {

    public PaymentException(String message) {
        super("[ERROR] " + message);
    }
}
