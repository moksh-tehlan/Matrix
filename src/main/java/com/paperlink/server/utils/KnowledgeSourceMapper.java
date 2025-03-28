package com.paperlink.server.utils;

import com.paperlink.server.dtos.response.KnowledgeSourceDto;
import com.paperlink.server.entities.KnowledgeSourceEntity;
import com.paperlink.server.entities.UserEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between KnowledgeSourceEntity and KnowledgeSourceDto
 */
@Component
public class KnowledgeSourceMapper {

    /**
     * Convert entity to DTO
     *
     * @param entity KnowledgeSourceEntity to convert
     * @return KnowledgeSourceDto
     */
    public KnowledgeSourceDto toDto(KnowledgeSourceEntity entity) {
        if (entity == null) {
            return null;
        }

        return KnowledgeSourceDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .processingStatus(entity.getProcessingStatus())
                .path(entity.getPath())
                .fileSize(entity.getFileSize())
                .mimeType(entity.getMimeType())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .chunkCount(entity.getChunkCount())
                .errorMessage(entity.getErrorMessage())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .build();
    }

    /**
     * Convert list of entities to list of DTOs
     *
     * @param entities List of KnowledgeSourceEntity to convert
     * @return List of KnowledgeSourceDto
     */
    public List<KnowledgeSourceDto> toDto(List<KnowledgeSourceEntity> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Convert DTO to entity for creating a new knowledge source
     *
     * @param dto  KnowledgeSourceDto to convert
     * @param user UserEntity associated with the knowledge source
     * @return KnowledgeSourceEntity
     */
    public KnowledgeSourceEntity toEntity(KnowledgeSourceDto dto, UserEntity user) {
        if (dto == null) {
            return null;
        }

        return KnowledgeSourceEntity.builder()
                .name(dto.getName())
                .processingStatus(dto.getProcessingStatus())
                .path(dto.getPath())
                .fileSize(dto.getFileSize())
                .mimeType(dto.getMimeType())
                .chunkCount(dto.getChunkCount())
                .errorMessage(dto.getErrorMessage())
                .user(user)
                .build();
    }

    /**
     * Update an existing entity from a DTO
     *
     * @param entity Existing KnowledgeSourceEntity to update
     * @param dto    KnowledgeSourceDto with updated values
     * @return Updated KnowledgeSourceEntity
     */
    public KnowledgeSourceEntity updateEntityFromDto(KnowledgeSourceEntity entity, KnowledgeSourceDto.UpdateRequest dto) {
        if (entity == null || dto == null) {
            return entity;
        }

        if (dto.getName() != null) {
            entity.setName(dto.getName());
        }

        if (dto.getProcessingStatus() != null) {
            entity.setProcessingStatus(dto.getProcessingStatus());
        }

        if (dto.getErrorMessage() != null) {
            entity.setErrorMessage(dto.getErrorMessage());
        }

        return entity;
    }
}