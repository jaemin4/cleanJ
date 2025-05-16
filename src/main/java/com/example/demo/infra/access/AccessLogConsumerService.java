package com.example.demo.infra.access;

import com.example.demo.domain.access.AccessLog;
import com.example.demo.domain.access.AccessLogRepository;
import com.example.demo.support.util.Utils;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import static com.example.demo.support.constants.RabbitmqConstant.QUEUE_ACCESS_LOG_SAVE;

@Service
@Slf4j
@RequiredArgsConstructor
@Profile("consumer")
public class AccessLogConsumerService {

    private final AccessLogRepository accessLogRepository;
    private final List<AccessLogConsumerCommand.Save> listAccessLogCommand = new ArrayList<>();

    @RabbitListener(queues = QUEUE_ACCESS_LOG_SAVE, concurrency = "1")
    public void saveAccessLog(AccessLogConsumerCommand.Save command) {
        try {
            log.info("AccessLog Consumer: {}", Utils.toJson(command));

            listAccessLogCommand.add(command);
            if(listAccessLogCommand.size() >= 10) {
                accessLogRepository.saveAll(AccessLog.of(listAccessLogCommand));

                log.info("Save access logs : {}", Utils.toJson(listAccessLogCommand));
                listAccessLogCommand.clear();
            }

        }catch (Exception e){
            log.error("AccessLog Save Consumer Error : {},{}",e.getMessage(), Utils.toJson(listAccessLogCommand));
        }
    }
    @PreDestroy
    public void onShutdown() {
        log.info("Graceful Shutdown Start...");
        try{
            accessLogRepository.saveAll(AccessLog.of(listAccessLogCommand));
        }catch (Exception e){
            log.error("AccessLog Save Error In Graceful Shutdown : {},{}",e.getMessage(), Utils.toJson(listAccessLogCommand));
        }
    }

}
