package com.paperlink.server.repositories;

import com.paperlink.server.entities.ChatEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for accessing and managing chat messages in the database. */
@Repository
public interface ChatRepository extends JpaRepository<ChatEntity, String> {

  /**
   * Find all chat messages for a user, ordered by creation time.
   *
   * @param userId User ID
   * @return List of chat messages ordered by creation time (ascending)
   */
  List<ChatEntity> findByUserIdOrderByCreatedAtAsc(String userId);

  /**
   * Delete all chat messages associated with a user.
   *
   * @param userId User ID
   */
  void deleteByUserId(String userId);
}
