package team.themoment.hellogsmv3.global.common.response;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CommonApiResponse<T> {

    @Schema(description = "상태 메시지", nullable = false, example = "OK")
    HttpStatus status;

    @Schema(description = "상태 코드", nullable = false, example = "200")
    int code;

    @Schema(description = "메시지", nullable = false, example = "완료되었습니다.")
    String message;

    @Schema(description = "데이터", nullable = true)
    @JsonInclude(Include.NON_NULL)
    T data;

    public static CommonApiResponse success(@Nonnull String message) {
        return new CommonApiResponse(HttpStatus.OK, HttpStatus.OK.value(), message, null);
    }

    public static CommonApiResponse created(@Nonnull String message) {
        return new CommonApiResponse(HttpStatus.CREATED, HttpStatus.CREATED.value(), message, null);
    }

    public static CommonApiResponse error(@Nonnull String message, @Nonnull HttpStatus status) {
        return new CommonApiResponse(status, status.value(), message, null);
    }
}
