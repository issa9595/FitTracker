package com.fittracker.social;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fittracker.common.error.BusinessRuleException;
import com.fittracker.common.error.ConflictException;
import com.fittracker.common.error.ForbiddenException;
import com.fittracker.common.error.NotFoundException;
import com.fittracker.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests unitaires (Mockito + AssertJ) du {@link FollowService}. */
@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

  @Mock private FollowRepository followRepository;
  @Mock private UserRepository userRepository;

  private FollowService followService;

  private final UUID follower = UUID.randomUUID();
  private final UUID followee = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    followService = new FollowService(followRepository, userRepository);
  }

  @Test
  void should_create_follow_when_valid() {
    when(userRepository.existsById(followee)).thenReturn(true);
    when(followRepository.existsById(any(FollowId.class))).thenReturn(false);
    when(followRepository.save(any(Follow.class))).thenAnswer(inv -> inv.getArgument(0));

    Follow created = followService.follow(follower, follower, followee);

    assertThat(created.getFollowerId()).isEqualTo(follower);
    assertThat(created.getFolloweeId()).isEqualTo(followee);
  }

  @Test
  void should_throw_forbidden_when_actor_is_not_follower() {
    assertThatThrownBy(() -> followService.follow(follower, UUID.randomUUID(), followee))
        .isInstanceOf(ForbiddenException.class);
    verify(followRepository, never()).save(any());
  }

  @Test
  void should_throw_business_rule_when_following_self() {
    assertThatThrownBy(() -> followService.follow(follower, follower, follower))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void should_throw_not_found_when_followee_absent() {
    when(userRepository.existsById(followee)).thenReturn(false);

    assertThatThrownBy(() -> followService.follow(follower, follower, followee))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void should_throw_conflict_when_already_following() {
    when(userRepository.existsById(followee)).thenReturn(true);
    when(followRepository.existsById(any(FollowId.class))).thenReturn(true);

    assertThatThrownBy(() -> followService.follow(follower, follower, followee))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void should_remove_follow_when_present() {
    when(followRepository.deleteByIdFollowerIdAndIdFolloweeId(follower, followee)).thenReturn(1L);

    followService.unfollow(follower, follower, followee);

    verify(followRepository).deleteByIdFollowerIdAndIdFolloweeId(follower, followee);
  }

  @Test
  void should_throw_forbidden_when_unfollow_actor_mismatch() {
    assertThatThrownBy(() -> followService.unfollow(follower, UUID.randomUUID(), followee))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void should_throw_not_found_when_unfollow_absent() {
    when(followRepository.deleteByIdFollowerIdAndIdFolloweeId(follower, followee)).thenReturn(0L);

    assertThatThrownBy(() -> followService.unfollow(follower, follower, followee))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void should_list_following_when_user_exists() {
    List<Follow> following = List.of(new Follow(follower, followee, OffsetDateTime.now()));
    when(userRepository.existsById(follower)).thenReturn(true);
    when(followRepository.findByIdFollowerId(follower)).thenReturn(following);

    assertThat(followService.listFollowing(follower)).isEqualTo(following);
  }

  @Test
  void should_throw_not_found_when_listing_following_of_unknown_user() {
    when(userRepository.existsById(follower)).thenReturn(false);

    assertThatThrownBy(() -> followService.listFollowing(follower))
        .isInstanceOf(NotFoundException.class);
  }
}
