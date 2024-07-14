package info.kgeorgiy.ja.sitkina.walk;

import java.io.IOException;

public class WriteException extends IOException {
    public WriteException(String s, Throwable cause) {
        super(s, cause);
    }
}
