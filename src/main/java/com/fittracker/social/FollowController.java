package com.fittracker.social;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.fittracker.common.security.CurrentUserProvider;
import com.fittracker.social.dto.FollowRequest;
import com.fittracker.social.dto.FollowResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/users/{userId}/follows", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Follows", description = "Relations sociales (qui suit qui)")
public class FollowController {

  private final FollowService service;
  private final CurrentUserProvider currentUser;

  public FollowController(FollowService service, CurrentUserProvider currentUser) {
    this.service = service;
    this.currentUser = currentUser;
  }

  @Operation(summary = "Liste les utilisateurs suivis par {userId}")
  @GetMapping
  public List<FollowResponse> list(@PathVariable UUID userId) {
    return service.listFollowing(userId).stream().map(this::toResponse).toList();
  }

  @Operation(summary = "Suit un utilisateur (followee) au nom de {userId}")
  @PostMapping
  public ResponseEntity<FollowResponse> follow(
      @PathVariable UUID userId, @Valid @RequestBody FollowRequest body) {
    Follow follow = service.follow(userId, currentUser.currentUserId(), body.followeeId());
    return ResponseEntity.status(201).body(toResponse(follow));
  }

  @Operation(summary = "Cesse de suivre followeeId au nom de {userId}")
  @DeleteMapping("/{followeeId}")
  public ResponseEntity<Void> unfollow(@PathVariable UUID userId, @PathVariable UUID followeeId) {
    service.unfollow(userId, currentUser.currentUserId(), followeeId);
    return ResponseEntity.noContent().build();
  }

  private FollowResponse toResponse(Follow f) {
    FollowResponse response = new FollowResponse(f.getFollowerId(), f.getFolloweeId(), f.getCreatedAt());
    response.add(
        linkTo(methodOn(FollowController.class).list(f.getFollowerId())).withRel("following"));
    return response;
  }
}
