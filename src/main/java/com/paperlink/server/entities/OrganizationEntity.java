package com.paperlink.server.entities;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "organizations")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class OrganizationEntity extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "subdomain", nullable = false, unique = true)
  private String subdomain;

  @Column(name = "email", nullable = false)
  private String contactEmail;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "is_active", nullable = false)
  private boolean isActive;

  @Column(name = "verification_token")
  private String verificationToken;

  @OneToOne(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
  private SlackWorkspaceEntity slackWorkspace;

  @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<UserEntity> users = new ArrayList<>();

  @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<KnowledgeSourceEntity> knowledgeSources = new ArrayList<>();
}
