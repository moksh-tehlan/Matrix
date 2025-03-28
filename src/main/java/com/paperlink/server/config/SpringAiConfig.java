package com.paperlink.server.config;

import com.paperlink.server.services.vector.ChatMemoryService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAiConfig {

    /**
     * Configures the ChatClient with necessary advisors for RAG
     *
     * @param chatClientBuilder The builder for the ChatClient
     * @param vectorStore The vector store for RAG
     * @param chatMemoryService Service for managing chat memory
     * @return Configured ChatClient
     */
    @Bean
    public ChatClient chatClient(
            ChatClient.Builder chatClientBuilder,
            VectorStore vectorStore,
            ChatMemoryService chatMemoryService) {

        // Configure vector search request
        SearchRequest searchRequest = SearchRequest.builder()
                .topK(3)
                .similarityThreshold(0.7f)
                .build();

        return chatClientBuilder.defaultAdvisors(
                new SimpleLoggerAdvisor(),
                new MessageChatMemoryAdvisor(chatMemoryService),
                new QuestionAnswerAdvisor(vectorStore, searchRequest)
        ).build();
    }

    /**
     * Configures the batching strategy for embeddings
     *
     * @return BatchingStrategy for token-based batching
     */
    @Bean
    public BatchingStrategy embeddingBatchingStrategy() {
        return new TokenCountBatchingStrategy();
    }
}