package com.fittracker.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fittracker.support.AbstractIntegrationTest;
import com.fittracker.user.Profile;
import com.fittracker.user.ProfileRepository;
import com.fittracker.user.User;
import com.fittracker.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Relation One-to-One : User et Profile partagent la cle primaire via @MapsId. */
@Transactional
class OneToOneRelationIT extends AbstractIntegrationTest {

  @Autowired UserRepository users;
  @Autowired ProfileRepository profiles;

  @Test
  void user_and_profile_share_primary_key_via_one_to_one() {
    var user =
        users.save(
            new User(
                UUID.randomUUID(),
                "alice-" + UUID.randomUUID() + "@fittracker.dev",
                "hash",
                "Alice",
                OffsetDateTime.now()));

    var profile = new Profile(user.getId(), 170, 65.0, 60.0, "bio");
    profile.setUser(user);
    profiles.save(profile);

    var loaded = profiles.findById(user.getId()).orElseThrow();
    assertThat(loaded.getUserId()).isEqualTo(user.getId());
    assertThat(loaded.getUser().getEmail()).isEqualTo(user.getEmail());
  }
}
