package com.fittracker.user;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.fittracker.common.security.CurrentUserProvider;
import com.fittracker.support.rgpd.UserAnonymizationService;
import com.fittracker.user.dto.ProfileResponse;
import com.fittracker.user.dto.ProfileUpdateRequest;
import com.fittracker.user.dto.UserResponse;
import com.fittracker.user.dto.UserUpdateRequest;
import com.fittracker.user.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/users/me", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Users", description = "Acces a l'utilisateur courant et son profil sportif")
public class UserController {

  private final UserService userService;
  private final UserMapper mapper;
  private final CurrentUserProvider currentUser;
  private final UserAnonymizationService anonymizationService;

  public UserController(
      UserService userService,
      UserMapper mapper,
      CurrentUserProvider currentUser,
      UserAnonymizationService anonymizationService) {
    this.userService = userService;
    this.mapper = mapper;
    this.currentUser = currentUser;
    this.anonymizationService = anonymizationService;
  }

  @Operation(summary = "Recupere l'utilisateur courant")
  @ApiResponse(responseCode = "200", description = "Utilisateur courant")
  @GetMapping
  public UserResponse getMe() {
    var me = userService.getById(currentUser.currentUserId());
    return addLinks(mapper.toResponse(me));
  }

  @Operation(summary = "Met a jour l'utilisateur courant")
  @PutMapping
  public UserResponse updateMe(@Valid @RequestBody UserUpdateRequest body) {
    var updated = userService.updateUser(currentUser.currentUserId(), body);
    return addLinks(mapper.toResponse(updated));
  }

  @Operation(summary = "Recupere le profil sportif de l'utilisateur courant")
  @GetMapping("/profile")
  public ProfileResponse getMyProfile() {
    var profile = userService.getProfile(currentUser.currentUserId());
    return addLinks(mapper.toResponse(profile));
  }

  @Operation(summary = "Met a jour le profil sportif de l'utilisateur courant")
  @PutMapping("/profile")
  public ProfileResponse updateMyProfile(@Valid @RequestBody ProfileUpdateRequest body) {
    var profile = userService.updateProfile(currentUser.currentUserId(), body);
    return addLinks(mapper.toResponse(profile));
  }

  @Operation(summary = "Supprime (anonymise) le compte de l'utilisateur courant (RGPD effacement)")
  @ApiResponse(responseCode = "204", description = "Compte anonymise")
  @DeleteMapping
  public ResponseEntity<Void> deleteMe() {
    anonymizationService.anonymize(currentUser.currentUserId());
    return ResponseEntity.noContent().build();
  }

  private UserResponse addLinks(UserResponse response) {
    response.add(linkTo(methodOn(UserController.class).getMe()).withSelfRel());
    response.add(linkTo(methodOn(UserController.class).getMyProfile()).withRel("profile"));
    return response;
  }

  private ProfileResponse addLinks(ProfileResponse response) {
    response.add(linkTo(methodOn(UserController.class).getMyProfile()).withSelfRel());
    response.add(linkTo(methodOn(UserController.class).getMe()).withRel("user"));
    return response;
  }
}
