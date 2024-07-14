package info.kgeorgiy.ja.sitkina.bank;

/**
 * Exception thrown if request to remote or local object is illegal.
 */
public class IllegalRequestException extends Exception {
    public IllegalRequestException(String message) {
        super("IllegalRequest: " + message);
    }
}
