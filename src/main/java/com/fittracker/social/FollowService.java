package com.fittracker.social;

import com.fittracker.common.error.BusinessRuleException;
import com.fittracker.common.error.ConflictException;
import com.fittracker.common.error.ForbiddenException;
import com.fittracker.common.error.NotFoundException;
import com.fittracker.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowService {

  private final FollowRepository followRepository;
  private final UserRepository userRepository;

  public FollowService(FollowRepository followRepository, UserRepository userRepository) {
    this.followRepository = followRepository;
    this.userRepository = userRepository;
  }

  @Transactional
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
    if (followRepository.existsById(new FollowId(followerId, followeeId))) {
      throw new ConflictException("Vous suivez deja cet utilisateur");
    }
    Follow follow = new Follow(followerId, followeeId, OffsetDateTime.now());
    // @MapsId : renseigner les deux extremites (proxies suffisent, on a deja verifie l'existence).
    follow.setFollower(userRepository.getReferenceById(followerId));
    follow.setFollowee(userRepository.getReferenceById(followeeId));
    return followRepository.save(follow);
  }

  @Transactional
  public void unfollow(UUID followerId, UUID actorId, UUID followeeId) {
    if (!followerId.equals(actorId)) {
      throw new ForbiddenException("Vous ne pouvez gerer que vos propres follows");
    }
    long removed = followRepository.deleteByIdFollowerIdAndIdFolloweeId(followerId, followeeId);
    if (removed == 0) {
      throw new NotFoundException("Follow", followerId + "->" + followeeId);
    }
  }

  @Transactional(readOnly = true)
  public List<Follow> listFollowing(UUID userId) {
    if (!userRepository.existsById(userId)) {
      throw new NotFoundException("User", userId);
    }
    return followRepository.findByIdFollowerId(userId);
  }
}
