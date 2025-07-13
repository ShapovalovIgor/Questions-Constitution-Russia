package ru.shapovalov.QuestionsConstitutionRussia.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
public class IngestionService implements CommandLineRunner {
    private static final Logger LOG = LoggerFactory.getLogger(IngestionService.class);

    public IngestionService(VectorStore vectorStore, JdbcClient jdbcClient) {
        this.vectorStore = vectorStore;
        this.jdbcClient = jdbcClient;
    }

    @Value("classpath:/constitution.pdf")
    private Resource pdfResource;

    private final VectorStore vectorStore;

    private final JdbcClient jdbcClient;

    @Override
    public void run(String... args) {
        Integer count =
                jdbcClient.sql("select COUNT(*) from vector_store")
                        .query(Integer.class)
                        .single();

        LOG.info("No of Records in the PG Vector Store = " + count);

        if (count == 0) {
            LOG.info("Loading Russian Constitution in the PG Vector Store");
            PdfDocumentReaderConfig config
                    = PdfDocumentReaderConfig.builder()
                    .withPagesPerDocument(1)
                    .build();

            PagePdfDocumentReader reader
                    = new PagePdfDocumentReader(pdfResource, config);

            var textSplitter = new TokenTextSplitter();
            vectorStore.accept(textSplitter.apply(reader.get()));
            LOG.info("Application is ready to Serve the Requests");
        }
    }
}
