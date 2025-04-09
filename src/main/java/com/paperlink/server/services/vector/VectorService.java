package com.paperlink.server.services.vector;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.paperlink.server.exceptions.VectorStoreException;
import com.paperlink.server.utils.DocumentListDeserializer;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

/** Service for managing vector embeddings and similarity search */
@Service
@Slf4j
@RequiredArgsConstructor
public class VectorService {

  private final VectorStore vectorStore;
  private final ChatClient chatClient;

  /**
   * Add documents to the vector store for embedding and indexing.
   *
   * @param documentList List of documents to add to the vector store
   * @throws VectorStoreException if there's an error adding documents to the vector store
   */
  public void addVectorData(List<Document> documentList) {
    // Validate input
    if (documentList == null || documentList.isEmpty()) {
      log.warn("No documents provided to add to vector store");
      return;
    }

    // Track performance
    StopWatch stopWatch = new StopWatch("vectorStoreAddition");
    stopWatch.start("addVectorData");
    log.info("Adding {} documents to vector store", documentList.size());

    try {
      // Add documents to vector store
      vectorStore.add(documentList);

      // Log performance metrics
      stopWatch.stop();
      log.info(
          "Successfully added {} documents to vector store in {} ms",
          documentList.size(),
          stopWatch.lastTaskInfo().getTimeMillis());
    } catch (Exception e) {
      log.error("Error adding documents to vector store: {}", e.getMessage(), e);
      throw new VectorStoreException("Failed to add documents to vector store", e);
    }
  }

  /**
   * Get response from AI model with conversation tracking
   *
   * @param query User query
   * @param conversationId Conversation ID for memory tracking
   * @return AI response
   */
  public String getResponse(String query, String conversationId) {
    long startTime = System.currentTimeMillis();
    log.debug("Processing query: {} for conversation: {}", query, conversationId);

    try {
      String response =
          chatClient
              .prompt()
              .advisors(
                  advisor ->
                      advisor.param(
                          MessageChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId))
              .advisors(
                  advisor ->
                      advisor.param(MessageChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY, 20))
              .user(query)
              .call()
              .content();

      long endTime = System.currentTimeMillis();
      log.info("Query processing took {} ms", (endTime - startTime));

      return response;
    } catch (Exception e) {
      log.error("Error processing query: {}", e.getMessage(), e);
      throw new RuntimeException("Error processing query: " + e.getMessage(), e);
    }
  }

  public List<Document> documentListFromJson(byte[] jsonContent) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      SimpleModule module = new SimpleModule();

      module.addDeserializer(List.class, new DocumentListDeserializer());
      objectMapper.registerModule(module);
      return objectMapper.readValue(jsonContent, new TypeReference<>() {});
    } catch (Exception e) {
      throw new RuntimeException("Failed to convert json file to document " + e.getMessage());
    }
  }
}
