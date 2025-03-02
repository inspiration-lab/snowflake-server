package snowflakeserver;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@Slf4j
public class SnowflakeController {

    private final SnowflakeIDGenerator idGenerator;

    // 从命令传入或配置文件注入worker-id，默认值为1
    public SnowflakeController(@Value("${snowflake.worker-id:1}") int workerId) {
        this.idGenerator = new SnowflakeIDGenerator(workerId);
        log.info("snowflake.worker-id: {}", workerId);
    }

    @RequestMapping(value = "/snowflake/id", method = RequestMethod.GET)
    public ApiResponse generateSnowflakeId(HttpServletRequest request) {
        LocalDateTime requestTime = LocalDateTime.now();
        String clientIp = request.getRemoteAddr();
        try {
            long id = idGenerator.generateId();
            log.info("Request from IP: {}, Time: {}, Generated ID: {}", clientIp, requestTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), id);
            return new ApiResponse(200, "OK", id);
        } catch (IllegalStateException e) {
            log.error("Request from IP: {}, Time: {}, Error: ID生成失败: {}", clientIp, requestTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), e.getMessage());
            return new ApiResponse(500, String.format("ID生成失败: %s", e.getMessage()), null);
        } catch (Exception e) {
            log.error("Request from IP: {}, Time: {}, Error: 服务器内部错误", clientIp, requestTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            return new ApiResponse(500, "服务器内部错误", null);
        }
    }
}
