package com.workflowai.pdfprocessor.camel;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PdfCamelRouteBuilder extends RouteBuilder {

    @Value("${spring.kafka.bootstrap-servers}")
    private String kafkaBrokers;

    @Override
    public void configure() throws Exception {
        from("direct:processPdf")
            .routeId("pdfProcessingRoute")
            .log("Received file path: ${body}")
            // Gọi processor để đọc file PDF và tách thành List<PdfChunk>
            .process(new PdfChunkProcessor())
            // Split danh sách các chunk để xử lý song song
            .split(body())
                .parallelProcessing()
                // Gọi Hugging Face API nonblocking cho từng chunk
                .process(new OpenAiProcessor())
                // Gửi kết quả đã xử lý lên Kafka (sử dụng Camel Kafka component)
                .to("kafka:pdf-chunks?brokers=" + kafkaBrokers)
            .end();
    }
}