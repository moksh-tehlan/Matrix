package com.paperlink.server.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.document.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DocumentListDeserializer extends JsonDeserializer<List<Document>> {
    @Override
    public List<Document> deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
        ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
        JsonNode rootNode = mapper.readTree(jsonParser);
        List<Document> documents = new ArrayList<>();

        if (rootNode.isArray()) {
            for (JsonNode node : rootNode) {
                String text = node.get("text").asText();
                Map<String, Object> metadata = mapper.convertValue(node.get("metadata"), new TypeReference<>() {
                });
                Document document = Document.builder().text(text).metadata(metadata).build();
                documents.add(document);
            }
        }

        return documents;
    }
}
