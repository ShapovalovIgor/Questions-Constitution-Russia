package ru.shapovalov.QuestionsConstitutionRussia.Controller;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class QuestionsController {

    private final ChatModel chatModel;
    private final VectorStore vectorStore;

    private String prompt = """
            Ваша задача — ответить на вопросы о Конституции России. Используйте информацию из раздела «ДОКУМЕНТЫ»,
            чтобы дать точные ответы. Если вы не уверены или не нашли ответ в разделе «ДОКУМЕНТЫ»,
            просто укажите, что вы не знаете ответа..
                        
            ВОПРОС:
            {input}
                        
            ДОКУМЕНТЫ:
            {documents}
                        
            """;

    public QuestionsController(ChatModel chatModel, VectorStore vectorStore) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
    }

    @GetMapping("/")
    public String simplify(@RequestParam(value = "question",
    defaultValue = "Перечислите все статьи Конституции России.")
                           String question) {
        PromptTemplate template
                = new PromptTemplate(prompt);
        Map<String, Object> promptsParameters = new HashMap<>();
        promptsParameters.put("input", question);
        promptsParameters.put("documents", findSimilarData(question));

        return chatModel
                .call(template.create(promptsParameters))
                .getResult()
                .getOutput()
                .getText();
    }

    private String findSimilarData(String question) {
        SearchRequest.Builder builder = new SearchRequest.Builder();
        builder.query(question);
        builder.topK(5);
        List<Document> documents =
                vectorStore.similaritySearch(builder.build());

        return documents
                .stream()
                .map(Document::getText)
                .collect(Collectors.joining());

    }
}
