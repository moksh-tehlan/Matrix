package com.paperlink.server.repositories;

import com.paperlink.server.entities.SlackWorkspaceEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SlackWorkspaceRepository extends JpaRepository<SlackWorkspaceEntity, String> {
  /**
   * Find a Slack workspace by its Slack workspace ID
   *
   * @param workspaceId Slack workspace ID
   * @return Optional containing the Slack workspace if found
   */
  Optional<SlackWorkspaceEntity> findByWorkspaceId(String workspaceId);

  /**
   * Find a Slack workspace by organization ID
   *
   * @param organizationId Organization ID
   * @return Optional containing the Slack workspace if found
   */
  Optional<SlackWorkspaceEntity> findByOrganizationId(String organizationId);
}
