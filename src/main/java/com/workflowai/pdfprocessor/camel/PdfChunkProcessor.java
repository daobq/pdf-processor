package com.workflowai.pdfprocessor.camel;

import com.workflowai.pdfprocessor.model.PdfChunk;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.tika.Tika;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PdfChunkProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        String pdfFilePath = exchange.getIn().getBody(String.class);
        Tika tika = new Tika();
        String text = tika.parseToString(new File(pdfFilePath));
        List<PdfChunk> chunkList = chunkText(text);
        exchange.getIn().setBody(chunkList);
    }

    private List<PdfChunk> chunkText(String text) {
        List<PdfChunk> chunkList = new ArrayList<>();
        // Ước tính token: tách theo khoảng trắng (mỗi từ xem như 1 token)
        String[] tokens = text.split("\\s+");
        int chunkTokenSize = 3000;
        int overlapTokens = (int)(chunkTokenSize * 0.3); // khoảng 900 token
        int start = 0;
        int chunkId = 1;
        while (start < tokens.length) {
            int end = Math.min(start + chunkTokenSize, tokens.length);
            StringBuilder sb = new StringBuilder();
            for (int i = start; i < end; i++) {
                sb.append(tokens[i]).append(" ");
            }
            chunkList.add(new PdfChunk(chunkId, sb.toString().trim()));
            chunkId++;
            // Overlap: chunk tiếp theo bắt đầu từ (end - overlapTokens)
            start = end - overlapTokens;
            if(start < 0) start = 0;
        }
        return chunkList;
    }
}