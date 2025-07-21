package ru.shapovalov.QuestionsConstitutionRussia.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VectorStoreInitializer implements CommandLineRunner {

    @Value("classpath:/constitution.pdf")
    private Resource pdfResource;

    private final PgVectorStore vectorStore;

    private final TextSplitter textSplitter;

    private final JdbcClient jdbcClient;

    public VectorStoreInitializer(PgVectorStore vectorStore, TextSplitter textSplitter, JdbcClient jdbcClient) {
        this.vectorStore = vectorStore;
        this.textSplitter = textSplitter;
        this.jdbcClient = jdbcClient;
    }


    @Override
    public void run(String... args) {
        Integer count =
                jdbcClient.sql("select COUNT(*) from vector_store")
                        .query(Integer.class)
                        .single();

        log.info("No of Records in the PG Vector Store = " + count);

        if (count == 0) {
            log.info("Loading Russian Constitution in the PG Vector Store");
            PdfDocumentReaderConfig config
                    = PdfDocumentReaderConfig.defaultConfig();

            PagePdfDocumentReader reader
                    = new PagePdfDocumentReader(pdfResource, config);

            var documents = textSplitter.apply(reader.get());

            vectorStore.accept(documents);

            log.info("Application is ready to Serve the Requests");
        }
    }
}
