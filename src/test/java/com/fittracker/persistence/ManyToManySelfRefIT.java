package com.fittracker.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fittracker.social.Follow;
import com.fittracker.social.FollowRepository;
import com.fittracker.support.AbstractIntegrationTest;
import com.fittracker.user.User;
import com.fittracker.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Relation Many-to-Many self-referencante : User suit User via Follow (cle composite). */
@Transactional
class ManyToManySelfRefIT extends AbstractIntegrationTest {

  @Autowired UserRepository users;
  @Autowired FollowRepository follows;

  @Test
  void user_can_follow_another_user() {
    var alice =
        users.save(
            new User(
                UUID.randomUUID(), "a-" + UUID.randomUUID() + "@x.dev", "h", "Alice",
                OffsetDateTime.now()));
    var bob =
        users.save(
            new User(
                UUID.randomUUID(), "b-" + UUID.randomUUID() + "@x.dev", "h", "Bob",
                OffsetDateTime.now()));

    var follow = new Follow(alice.getId(), bob.getId(), OffsetDateTime.now());
    follow.setFollower(alice);
    follow.setFollowee(bob);
    follows.save(follow);

    assertThat(follows.findByIdFollowerId(alice.getId())).hasSize(1);
    assertThat(follows.findByIdFolloweeId(bob.getId())).hasSize(1);
  }
}
