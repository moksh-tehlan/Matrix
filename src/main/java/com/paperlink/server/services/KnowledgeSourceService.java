package com.paperlink.server.services;

import com.paperlink.server.dtos.enums.ProcessingStatus;
import com.paperlink.server.entities.KnowledgeSourceEntity;
import com.paperlink.server.entities.UserEntity;
import com.paperlink.server.exceptions.ResourceNotFoundException;
import com.paperlink.server.repositories.KnowledgeSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing Knowledge Source entities
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeSourceService {

    private final KnowledgeSourceRepository knowledgeSourceRepository;

    /**
     * Get a knowledge source by ID
     *
     * @param id Knowledge source ID
     * @return KnowledgeSourceEntity
     * @throws ResourceNotFoundException if knowledge source is not found
     */
    @Transactional(readOnly = true)
    public KnowledgeSourceEntity getKnowledgeSource(String id) {
        log.debug("Fetching knowledge source with ID: {}", id);
        return knowledgeSourceRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Knowledge source not found with ID: {}", id);
                    return new ResourceNotFoundException("Knowledge source not found with ID: " + id);
                });
    }

    /**
     * Save a knowledge source entity
     *
     * @param knowledgeSource Knowledge source to save
     * @return Saved knowledge source
     */
    @Transactional
    public KnowledgeSourceEntity saveKnowledgeSource(KnowledgeSourceEntity knowledgeSource) {
        log.debug("Saving knowledge source: {}", knowledgeSource);
        return knowledgeSourceRepository.save(knowledgeSource);
    }

    /**
     * Update knowledge source status
     *
     * @param id     Knowledge source ID
     * @param status New processing status
     * @throws ResourceNotFoundException if knowledge source is not found
     */
    @Transactional
    public KnowledgeSourceEntity updateKnowledgeSourceStatus(String id, ProcessingStatus status) {
        log.debug("Updating knowledge source status: id={}, status={}", id, status);

        KnowledgeSourceEntity knowledgeSource = getKnowledgeSource(id);
        knowledgeSource.setProcessingStatus(status);

        if (status == ProcessingStatus.FAILED) {
            knowledgeSource.setErrorMessage("Processing failed");
        }

        return saveKnowledgeSource(knowledgeSource);
    }

    /**
     * Update knowledge source error message
     *
     * @param id           Knowledge source ID
     * @param errorMessage Error message
     * @return Updated knowledge source
     * @throws ResourceNotFoundException if knowledge source is not found
     */
    @Transactional
    public KnowledgeSourceEntity updateKnowledgeSourceError(String id, String errorMessage) {
        log.debug("Updating knowledge source error: id={}, error={}", id, errorMessage);

        KnowledgeSourceEntity knowledgeSource = getKnowledgeSource(id);
        knowledgeSource.setErrorMessage(errorMessage);
        knowledgeSource.setProcessingStatus(ProcessingStatus.FAILED);

        return saveKnowledgeSource(knowledgeSource);
    }

    /**
     * Get all knowledge sources
     *
     * @return List of all knowledge sources
     */
    @Transactional(readOnly = true)
    public List<KnowledgeSourceEntity> getAllKnowledgeSources() {
        log.debug("Fetching all knowledge sources");
        return knowledgeSourceRepository.findAll();
    }

    /**
     * Get knowledge sources by user
     *
     * @param user User entity
     * @return List of knowledge sources for the user
     */
    @Transactional(readOnly = true)
    public List<KnowledgeSourceEntity> getKnowledgeSourcesByUser(UserEntity user) {
        log.debug("Fetching knowledge sources for user: {}", user.getId());
        return knowledgeSourceRepository.findByUploadedBy(user);
    }

    /**
     * Update chunk count for a knowledge source
     *
     * @param id         Knowledge source ID
     * @param chunkCount Number of chunks
     * @throws ResourceNotFoundException if knowledge source is not found
     */
    @Transactional
    public KnowledgeSourceEntity updateChunkCount(String id, int chunkCount) {
        log.debug("Updating knowledge source chunk count: id={}, chunkCount={}", id, chunkCount);

        KnowledgeSourceEntity knowledgeSource = getKnowledgeSource(id);
        knowledgeSource.setChunkCount(chunkCount);

        return saveKnowledgeSource(knowledgeSource);
    }
}
