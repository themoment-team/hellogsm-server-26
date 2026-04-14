package team.themoment.hellogsmv3.global.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import team.themoment.hellogsmv3.global.common.response.CommonApiResponse;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ExpectedException.class)
    private CommonApiResponse expectedException(ExpectedException ex) {
        log.warn("ExpectedException : {} ", ex.getMessage());
        log.trace("ExpectedException Details : ", ex);
        return CommonApiResponse.error(ex.getMessage(), ex.getStatusCode());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            ConstraintViolationException.class, MethodArgumentTypeMismatchException.class})
    public CommonApiResponse validationException(Exception ex) {
        log.warn("Validation Failed : {}", ex.getMessage());
        log.trace("Validation Failed Details : ", ex);
        String errorMessage;
        if (ex instanceof MethodArgumentNotValidException) {
            errorMessage = methodArgumentNotValidExceptionToJson((MethodArgumentNotValidException) ex);
        } else {
            errorMessage = ex.getMessage();
        }
        return CommonApiResponse.error(errorMessage, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalStateException.class)
    public CommonApiResponse illegalStateException(IllegalStateException ex) {
        if (ex.getMessage() != null && ex.getMessage().contains("creationTime key must not be null")) {
            log.warn("Corrupted session detected, treating as invalid session: {}", ex.getMessage());
            return CommonApiResponse.error("Session is invalid or expired", HttpStatus.UNAUTHORIZED);
        }
        return unExpectedException(ex); // 다른 IllegalStateException은 RuntimeException 핸들러로 위임
    }

    @ExceptionHandler(RuntimeException.class)
    public CommonApiResponse unExpectedException(RuntimeException ex) {
        log.error("UnExpectedException Occur : ", ex);
        return CommonApiResponse.error("internal server error has occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public CommonApiResponse noHandlerFoundException(NoHandlerFoundException ex) {
        log.warn("Not Found Endpoint : {}", ex.getMessage());
        log.trace("Not Found Endpoint Details : ", ex);
        return CommonApiResponse.error(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public CommonApiResponse maxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        log.warn("The file is too big : {}", ex.getMessage());
        log.trace("The file is too big Details : ", ex);
        return CommonApiResponse.error("The file is too big, limited file size : " + ex.getMaxUploadSize(),
                HttpStatus.BAD_REQUEST);
    }

    private static String methodArgumentNotValidExceptionToJson(MethodArgumentNotValidException ex) {
        Map<String, Object> result = new HashMap<>();
        Map<String, String> fieldErrors = new HashMap<>();
        Map<String, String> globalErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

        ex.getBindingResult().getGlobalErrors()
                .forEach(error -> globalErrors.put(error.getObjectName(), error.getDefaultMessage()));

        result.put("fieldErrors", fieldErrors);
        if (!globalErrors.isEmpty()) {
            result.put("globalErrors", globalErrors);
        }
        return new JSONObject(result).toString().replace("\"", "'");
    }
}
