package sogeun.backend.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final String code;
    private final String message;
    private final String path;
    private final String timestamp;
    private final Object details;

    public ErrorResponse(String code, String message, String path) {
        this(code, message, path, null);
    }

    public ErrorResponse(String code, String message, String path, Object details) {
        this.code = code;
        this.message = message;
        this.path = path;
        this.details = details;

        this.timestamp = OffsetDateTime.now().toString();
    }

    public String getCode() { return code; }
    public String getMessage() { return message; }
    public String getPath() { return path; }
    public String getTimestamp() { return timestamp; }

    public Object getDetails() { return details; }
}