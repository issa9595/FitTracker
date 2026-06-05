package com.fittracker.social;

import com.fittracker.common.error.BusinessRuleException;
import com.fittracker.common.error.ConflictException;
import com.fittracker.common.error.ForbiddenException;
import com.fittracker.common.error.NotFoundException;
import com.fittracker.social.Follow.FollowKey;
import com.fittracker.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class FollowService {

  private final FollowRepository followRepository;
  private final UserRepository userRepository;

  public FollowService(FollowRepository followRepository, UserRepository userRepository) {
    this.followRepository = followRepository;
    this.userRepository = userRepository;
  }

  public Follow follow(UUID followerId, UUID actorId, UUID followeeId) {
    if (!followerId.equals(actorId)) {
      throw new ForbiddenException("Vous ne pouvez gerer que vos propres follows");
    }
    if (followerId.equals(followeeId)) {
      throw new BusinessRuleException("Impossible de se suivre soi-meme");
    }
    if (!userRepository.existsById(followeeId)) {
      throw new NotFoundException("User", followeeId);
    }
    FollowKey key = new FollowKey(followerId, followeeId);
    if (followRepository.existsById(key)) {
      throw new ConflictException("Vous suivez deja cet utilisateur");
    }
    return followRepository.save(new Follow(followerId, followeeId, OffsetDateTime.now()));
  }

  public void unfollow(UUID followerId, UUID actorId, UUID followeeId) {
    if (!followerId.equals(actorId)) {
      throw new ForbiddenException("Vous ne pouvez gerer que vos propres follows");
    }
    boolean removed = followRepository.deleteById(new FollowKey(followerId, followeeId));
    if (!removed) {
      throw new NotFoundException("Follow", followerId + "->" + followeeId);
    }
  }

  public List<Follow> listFollowing(UUID userId) {
    if (!userRepository.existsById(userId)) {
      throw new NotFoundException("User", userId);
    }
    return followRepository.findByFollower(userId);
  }
}
