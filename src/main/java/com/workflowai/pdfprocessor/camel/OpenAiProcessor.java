package com.workflowai.pdfprocessor.camel;

import com.workflowai.pdfprocessor.model.PdfChunk;
import org.apache.camel.AsyncCallback;
import org.apache.camel.AsyncProcessor;
import org.apache.camel.Exchange;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenAiProcessor implements AsyncProcessor {

    private final WebClient webClient;

    public OpenAiProcessor() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + "your-openai-api-key-here")
                .build();
    }

    @Override
    public boolean process(Exchange exchange, AsyncCallback callback) {
        PdfChunk chunk = exchange.getIn().getBody(PdfChunk.class);
        String prompt = "Convert the following text to well-structured Markdown:\n" + chunk.getText();

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", "gpt-3.5-turbo"); // hoặc "gpt-4" nếu cần
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        messages.add(message);
        payload.put("messages", messages);

        Mono<Map> responseMono = webClient.post()
                .uri("/chat/completions")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class);

        responseMono.subscribe(response -> {
            try {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, Object> msg = (Map<String, Object>) firstChoice.get("message");
                    String markdown = (String) msg.get("content");
                    chunk.setText(markdown);
                }
            } catch (Exception e) {
                exchange.setException(e);
            }
            exchange.getIn().setBody(chunk);
            callback.done(false);
        }, error -> {
            exchange.setException(error);
            callback.done(false);
        });
        return false; // báo hiệu xử lý không đồng bộ
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        // Phương thức blocking không dùng (vì chúng ta đã sử dụng AsyncProcessor)
        process(exchange, done -> {});
    }
}