package com.workflowai.pdfprocessor.controller;

import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/pdf")
public class PdfUploadController {
    private final ProducerTemplate producerTemplate;

    @Value("${pdf.upload.path}")
    private String uploadPath;

    @Value("${pdf.max-files}")
    private int maxFiles;

    public PdfUploadController(ProducerTemplate producerTemplate) {
        this.producerTemplate = producerTemplate;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<String>> uploadPdf(@RequestPart("file") FilePart filePart) {
        return filePart.transferTo(saveFile(filePart.filename()))
                .then(Mono.fromCallable(() -> {
                    // Gửi đường dẫn file đã lưu đến Camel route "direct:processPdf"
                    producerTemplate.sendBody("direct:processPdf", saveFile(filePart.filename()).getAbsolutePath());
                    return ResponseEntity.ok("File uploaded and processing job started.");
                }));
    }

    // Phương thức lưu file vào thư mục được cấu hình
    private File saveFile(String originalFilename) {
        try {
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            long fileCount = Files.list(uploadDir).count();
            if (fileCount >= maxFiles) {
                throw new RuntimeException("Đã đạt số lượng file tối đa cho phép.");
            }
            File destFile = new File(uploadDir.toFile(), originalFilename);
            return destFile;
        } catch (Exception e) {
            throw new RuntimeException("Error saving file: " + e.getMessage(), e);
        }
    }

}
