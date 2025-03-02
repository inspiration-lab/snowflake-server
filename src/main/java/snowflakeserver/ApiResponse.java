package snowflakeserver;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiResponse {
    @JsonProperty("status")
    private int status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private Object data;

    public ApiResponse(int status, String message, Object data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }
}
    