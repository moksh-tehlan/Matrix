package com.paperlink.server.repositories;

import com.paperlink.server.entities.SlackUserEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SlackUserRepository extends JpaRepository<SlackUserEntity, String> {

  /**
   * Find a Slack user by their Slack user ID and workspace ID
   *
   * @param slackUserId Slack user ID
   * @param workspaceId Workspace ID
   * @return Optional containing the Slack user if found
   */
  Optional<SlackUserEntity> findBySlackUserIdAndWorkspaceId(String slackUserId, String workspaceId);

  /**
   * Find all Slack users in an organization
   *
   * @param organizationId Organization ID
   * @return List of Slack users
   */
  List<SlackUserEntity> findByOrganizationId(String organizationId);

  /**
   * Find all Slack users in a workspace
   *
   * @param workspaceId Workspace ID
   * @return List of Slack users
   */
  List<SlackUserEntity> findByWorkspaceId(String workspaceId);
}
