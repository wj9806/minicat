package io.github.wj9806.minicat.spring.boot.test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RestController
public class SseController {

    /**
     * 需要支持异步请求
     */
    @GetMapping("/sse")
    public SseEmitter handleSse() {
        // 创建 SseEmitter，超时时间为 30 秒
        SseEmitter emitter = new SseEmitter(30_000L);

        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                // 模拟定时发送消息
                for (int i = 0; i < 5; i++) {
                    emitter.send("当前时间: " + System.currentTimeMillis());
                    TimeUnit.SECONDS.sleep(2); // 每 2 秒发送一次
                }
                emitter.complete(); // 完成消息发送
            } catch (IOException | InterruptedException e) {
                emitter.completeWithError(e); // 发送错误
            }
        });

        return emitter;
    }

    /**
     * 需要支持异步请求
     */
    @GetMapping("/sse2")
    public ResponseEntity<StreamingResponseBody> streamEvents() {
        StreamingResponseBody stream = outputStream -> {
            try {
                for (int i = 0; i < 10; i++) {
                    String eventData = "Event " + i + "\n";
                    outputStream.write(("data: " + eventData).getBytes());
                    outputStream.write("\n".getBytes());  // SSE格式要求每个事件后有一个换行符
                    outputStream.flush();
                    Thread.sleep(1000); // 每隔1秒发送一个事件
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_EVENT_STREAM_VALUE)
                .body(stream);
    }
}
