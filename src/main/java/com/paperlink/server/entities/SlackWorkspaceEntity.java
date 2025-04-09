package com.paperlink.server.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "slack_workspaces")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class SlackWorkspaceEntity extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(name = "workspace_id", nullable = false, unique = true)
  private String workspaceId;

  @Column(name = "workspace_name")
  private String workspaceName;

  @Column(name = "access_token", columnDefinition = "TEXT")
  private String accessToken;

  @Column(name = "bot_token", columnDefinition = "TEXT")
  private String botToken;

  @Column(name = "bot_user_id")
  private String botUserId;

  @Column(name = "team_name")
  private String teamName;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id", nullable = false)
  private OrganizationEntity organization;
}
