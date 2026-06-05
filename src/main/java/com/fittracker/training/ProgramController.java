package com.fittracker.training;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.fittracker.common.pagination.PageRequest;
import com.fittracker.common.pagination.PageResponse;
import com.fittracker.common.security.CurrentUserProvider;
import com.fittracker.training.dto.ProgramCreateRequest;
import com.fittracker.training.dto.ProgramResponse;
import com.fittracker.training.dto.ProgramUpdateRequest;
import com.fittracker.training.mapper.ProgramMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
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
@RequestMapping(value = "/api/v1/programs", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Programs", description = "Programmes d'entrainement de l'utilisateur courant")
public class ProgramController {

  private final ProgramService service;
  private final ProgramMapper mapper;
  private final CurrentUserProvider currentUser;

  public ProgramController(
      ProgramService service, ProgramMapper mapper, CurrentUserProvider currentUser) {
    this.service = service;
    this.mapper = mapper;
    this.currentUser = currentUser;
  }

  @Operation(summary = "Liste paginee des programmes de l'utilisateur courant")
  @GetMapping
  public PageResponse<ProgramResponse> list(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    UUID userId = currentUser.currentUserId();
    PageRequest pr = PageRequest.of(page, size, "createdAt,desc");
    List<Program> all = service.listForUser(userId);
    long total = all.size();
    List<ProgramResponse> items =
        all.stream()
            .sorted(Comparator.comparing(Program::getCreatedAt).reversed())
            .skip(pr.offset())
            .limit(pr.size())
            .map(mapper::toResponse)
            .map(this::addItemLinks)
            .toList();
    PageResponse<ProgramResponse> response = new PageResponse<>(items, pr, total);
    return response.withNavigationLinks(p -> buildPageLink(p, pr.size()));
  }

  @Operation(summary = "Cree un programme")
  @PostMapping
  public ResponseEntity<ProgramResponse> create(@Valid @RequestBody ProgramCreateRequest body) {
    Program program = service.create(currentUser.currentUserId(), body);
    return ResponseEntity.created(URI.create("/api/v1/programs/" + program.getId()))
        .body(addItemLinks(mapper.toResponse(program)));
  }

  @Operation(summary = "Recupere un programme")
  @GetMapping("/{id}")
  public ProgramResponse getById(@PathVariable UUID id) {
    return addItemLinks(mapper.toResponse(service.getOwned(id, currentUser.currentUserId())));
  }

  @Operation(summary = "Met a jour un programme")
  @PutMapping("/{id}")
  public ProgramResponse update(
      @PathVariable UUID id, @Valid @RequestBody ProgramUpdateRequest body) {
    return addItemLinks(mapper.toResponse(service.update(id, currentUser.currentUserId(), body)));
  }

  @Operation(summary = "Supprime un programme")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    service.delete(id, currentUser.currentUserId());
    return ResponseEntity.noContent().build();
  }

  private ProgramResponse addItemLinks(ProgramResponse resp) {
    resp.add(linkTo(methodOn(ProgramController.class).getById(resp.getId())).withSelfRel());
    resp.add(
        Link.of(
                ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/v1/programs")
                    .toUriString())
            .withRel("collection"));
    return resp;
  }

  private Link buildPageLink(int page, int size) {
    return Link.of(
        ServletUriComponentsBuilder.fromCurrentRequest()
            .replaceQueryParam("page", page)
            .replaceQueryParam("size", size)
            .toUriString());
  }
}
