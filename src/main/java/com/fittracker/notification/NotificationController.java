package com.fittracker.notification;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.fittracker.common.pagination.CursorPageRequest;
import com.fittracker.common.pagination.CursorPageResponse;
import com.fittracker.common.security.CurrentUserProvider;
import com.fittracker.notification.dto.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.hateoas.Link;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(value = "/api/v1/notifications", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Notifications", description = "Notifications utilisateur (pagination par curseur)")
public class NotificationController {

  private final NotificationService service;
  private final CurrentUserProvider currentUser;

  public NotificationController(NotificationService service, CurrentUserProvider currentUser) {
    this.service = service;
    this.currentUser = currentUser;
  }

  @Operation(
      summary = "Liste des notifications (pagination cursor)",
      description =
          "Renvoie un nextCursor opaque. Tant qu'il n'est pas null, il existe plus d'elements. La"
              + " pagination par curseur est recommandee pour les flux a fort volume.")
  @GetMapping
  public CursorPageResponse<NotificationResponse> list(
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int size) {
    CursorPageRequest pr = CursorPageRequest.of(cursor, size);
    var slice = service.list(currentUser.currentUserId(), pr);
    List<NotificationResponse> items = slice.content().stream().map(this::toResponse).toList();

    CursorPageResponse<NotificationResponse> response =
        new CursorPageResponse<>(items, slice.nextCursor(), pr.size());

    Link self = Link.of(ServletUriComponentsBuilder.fromCurrentRequest().toUriString());
    return response.withNavigationLinks(
        self,
        next ->
            Link.of(
                ServletUriComponentsBuilder.fromCurrentRequest()
                    .replaceQueryParam("cursor", next)
                    .replaceQueryParam("size", pr.size())
                    .toUriString()));
  }

  @Operation(summary = "Marque une notification comme lue")
  @PatchMapping("/{id}/read")
  public NotificationResponse markRead(@PathVariable UUID id) {
    Notification n = service.markAsRead(id, currentUser.currentUserId());
    return toResponse(n);
  }

  private NotificationResponse toResponse(Notification n) {
    NotificationResponse response =
        new NotificationResponse(n.getId(), n.getType(), n.getPayload(), n.getReadAt(), n.getCreatedAt());
    response.add(linkTo(methodOn(NotificationController.class).markRead(n.getId())).withRel("mark-read"));
    return response;
  }
}
