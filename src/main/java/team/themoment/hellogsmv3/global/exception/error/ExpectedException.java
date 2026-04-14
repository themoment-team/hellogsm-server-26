package team.themoment.hellogsmv3.global.exception.error;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class ExpectedException extends RuntimeException {

    private final HttpStatus statusCode;

    public ExpectedException(String message, HttpStatus statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public ExpectedException(HttpStatus statusCode) {
        this(statusCode.getReasonPhrase(), statusCode);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
