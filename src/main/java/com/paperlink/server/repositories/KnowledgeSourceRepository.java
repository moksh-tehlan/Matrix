package com.paperlink.server.repositories;

import com.paperlink.server.entities.KnowledgeSourceEntity;
import com.paperlink.server.entities.UserEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeSourceRepository extends JpaRepository<KnowledgeSourceEntity, String> {
  /**
   * Find all knowledge sources for a user
   *
   * @param user The user
   * @return List of knowledge sources
   */
  List<KnowledgeSourceEntity> findByUploadedBy(UserEntity user);
}
