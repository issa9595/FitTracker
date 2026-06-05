package com.fittracker.training;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.fittracker.common.filter.FilterCriterion;
import com.fittracker.common.filter.FilterMatchers;
import com.fittracker.common.filter.FilterParser;
import com.fittracker.common.filter.FilterSpec;
import com.fittracker.common.pagination.PageRequest;
import com.fittracker.common.pagination.PageResponse;
import com.fittracker.common.security.CurrentUserProvider;
import com.fittracker.common.sort.SortSpec;
import com.fittracker.training.dto.SessionExerciseRequest;
import com.fittracker.training.dto.TrainingSessionCreateRequest;
import com.fittracker.training.dto.TrainingSessionResponse;
import com.fittracker.training.dto.TrainingSessionUpdateRequest;
import com.fittracker.training.mapper.TrainingSessionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import org.springframework.hateoas.Link;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(value = "/api/v1/training-sessions", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Training Sessions", description = "Sessions d'entrainement de l'utilisateur courant")
public class TrainingSessionController {

  private final TrainingSessionService service;
  private final TrainingSessionMapper mapper;
  private final CurrentUserProvider currentUser;
  private final FilterParser filterParser;

  public TrainingSessionController(
      TrainingSessionService service,
      TrainingSessionMapper mapper,
      CurrentUserProvider currentUser,
      FilterParser filterParser) {
    this.service = service;
    this.mapper = mapper;
    this.currentUser = currentUser;
    this.filterParser = filterParser;
  }

  @Operation(summary = "Liste paginee des sessions de l'utilisateur courant")
  @GetMapping
  public PageResponse<TrainingSessionResponse> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "startedAt,desc") String sort,
      @RequestParam(required = false) String filter,
      @RequestParam(required = false) String filterOp) {

    UUID userId = currentUser.currentUserId();
    PageRequest pr = PageRequest.of(page, size, sort);
    FilterSpec spec = filterParser.parse(filter, filterOp);
    Predicate<TrainingSession> predicate = spec.toPredicate(this::predicateFor);
    Comparator<TrainingSession> comparator = comparatorFor(SortSpec.parse(sort, "startedAt"));

    List<TrainingSession> all = service.listForUser(userId);
    long total = all.stream().filter(predicate).count();
    List<TrainingSessionResponse> items =
        all.stream()
            .filter(predicate)
            .sorted(comparator)
            .skip(pr.offset())
            .limit(pr.size())
            .map(mapper::toResponse)
            .map(this::addItemLinks)
            .toList();

    PageResponse<TrainingSessionResponse> response = new PageResponse<>(items, pr, total);
    final String f = filter;
    final String fo = filterOp;
    return response.withNavigationLinks(p -> buildPageLink(p, pr.size(), sort, f, fo));
  }

  @Operation(summary = "Cree une session pour l'utilisateur courant")
  @PostMapping
  public ResponseEntity<TrainingSessionResponse> create(
      @Valid @RequestBody TrainingSessionCreateRequest body) {
    TrainingSession session = service.create(currentUser.currentUserId(), body);
    TrainingSessionResponse response = addItemLinks(mapper.toResponse(session));
    return ResponseEntity.created(URI.create("/api/v1/training-sessions/" + session.getId()))
        .body(response);
  }

  @Operation(summary = "Recupere une session par id (proprietaire seulement)")
  @GetMapping("/{id}")
  public TrainingSessionResponse getById(@PathVariable UUID id) {
    return addItemLinks(mapper.toResponse(service.getOwned(id, currentUser.currentUserId())));
  }

  @Operation(summary = "Met a jour une session")
  @PutMapping("/{id}")
  public TrainingSessionResponse update(
      @PathVariable UUID id, @Valid @RequestBody TrainingSessionUpdateRequest body) {
    return addItemLinks(mapper.toResponse(service.update(id, currentUser.currentUserId(), body)));
  }

  @Operation(summary = "Supprime une session")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    service.delete(id, currentUser.currentUserId());
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Ajoute un exercice a la session")
  @PostMapping("/{id}/exercises")
  public ResponseEntity<TrainingSessionResponse> addExercise(
      @PathVariable UUID id, @Valid @RequestBody SessionExerciseRequest body) {
    TrainingSession session = service.addExercise(id, currentUser.currentUserId(), body);
    return ResponseEntity.status(201).body(addItemLinks(mapper.toResponse(session)));
  }

  private TrainingSessionResponse addItemLinks(TrainingSessionResponse resp) {
    resp.add(linkTo(methodOn(TrainingSessionController.class).getById(resp.getId())).withSelfRel());
    resp.add(
        Link.of(
                ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/v1/training-sessions")
                    .toUriString())
            .withRel("collection"));
    resp.add(
        linkTo(methodOn(TrainingSessionController.class).addExercise(resp.getId(), null))
            .withRel("add-exercise"));
    return resp;
  }

  private Link buildPageLink(int page, int size, String sort, String filter, String filterOp) {
    var builder = ServletUriComponentsBuilder.fromCurrentRequest()
        .replaceQueryParam("page", page)
        .replaceQueryParam("size", size)
        .replaceQueryParam("sort", sort);
    if (filter != null && !filter.isBlank()) {
      builder.replaceQueryParam("filter", filter);
    } else {
      builder.replaceQueryParam("filter");
    }
    if (filterOp != null && !filterOp.isBlank()) {
      builder.replaceQueryParam("filterOp", filterOp);
    } else {
      builder.replaceQueryParam("filterOp");
    }
    return Link.of(builder.toUriString());
  }

  private Predicate<TrainingSession> predicateFor(FilterCriterion c) {
    return switch (c.field()) {
      case "type" -> FilterMatchers.string(s -> s.getType().name(), c);
      case "durationSeconds" -> FilterMatchers.longValue(s -> (long) s.getDurationSeconds(), c);
      case "notes" -> FilterMatchers.string(TrainingSession::getNotes, c);
      default -> throw new IllegalArgumentException("Filtre non supporte : " + c.field());
    };
  }

  private Comparator<TrainingSession> comparatorFor(SortSpec sort) {
    return switch (sort.field()) {
      case "startedAt" -> sort.toComparator(TrainingSession::getStartedAt);
      case "durationSeconds" -> sort.toComparator(s -> (long) s.getDurationSeconds());
      case "createdAt" -> sort.toComparator(TrainingSession::getCreatedAt);
      default -> sort.toComparator(TrainingSession::getStartedAt);
    };
  }
}
