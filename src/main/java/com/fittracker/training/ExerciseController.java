package com.fittracker.training;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.fittracker.common.error.NotFoundException;
import com.fittracker.common.filter.FilterCriterion;
import com.fittracker.common.filter.FilterMatchers;
import com.fittracker.common.filter.FilterParser;
import com.fittracker.common.filter.FilterSpec;
import com.fittracker.common.pagination.PageRequest;
import com.fittracker.common.pagination.PageResponse;
import com.fittracker.common.sort.SortSpec;
import com.fittracker.training.dto.ExerciseResponse;
import com.fittracker.training.mapper.ExerciseMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import org.springframework.hateoas.Link;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(value = "/api/v1/exercises", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Exercises", description = "Referentiel partage des exercices")
public class ExerciseController {

  private final ExerciseRepository repository;
  private final ExerciseMapper mapper;
  private final FilterParser filterParser;

  public ExerciseController(
      ExerciseRepository repository, ExerciseMapper mapper, FilterParser filterParser) {
    this.repository = repository;
    this.mapper = mapper;
    this.filterParser = filterParser;
  }

  @Operation(
      summary = "Liste paginee et filtrable des exercices",
      description =
          "Pagination offset/limit. Filtre via ?filter=field:op:value,...&filterOp=AND. Operateurs"
              + " supportes : eq, neq, in, like sur name/category/muscleGroup/unit.")
  @GetMapping
  public PageResponse<ExerciseResponse> list(
      @Parameter(description = "Numero de page (0-based)") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Taille de la page") @RequestParam(defaultValue = "20") int size,
      @Parameter(description = "Tri 'champ,direction'")
          @RequestParam(required = false, defaultValue = "name,asc")
          String sort,
      @Parameter(description = "DSL de filtre") @RequestParam(required = false) String filter,
      @Parameter(description = "Combinator AND|OR") @RequestParam(required = false) String filterOp) {

    PageRequest pageRequest = PageRequest.of(page, size, sort);
    FilterSpec spec = filterParser.parse(filter, filterOp);
    Predicate<Exercise> predicate = spec.toPredicate(this::predicateFor);
    Comparator<Exercise> comparator = comparatorFor(SortSpec.parse(sort, "name"));

    long total =
        repository.all().stream().filter(predicate).count();
    List<ExerciseResponse> items =
        repository.all().stream()
            .filter(predicate)
            .sorted(comparator)
            .skip(pageRequest.offset())
            .limit(pageRequest.size())
            .map(mapper::toResponse)
            .map(this::addItemLinks)
            .toList();

    PageResponse<ExerciseResponse> response = new PageResponse<>(items, pageRequest, total);
    final String filterFinal = filter;
    final String filterOpFinal = filterOp;
    return response.withNavigationLinks(p -> buildPageLink(p, pageRequest.size(), sort, filterFinal, filterOpFinal));
  }

  @Operation(summary = "Recupere un exercice par son id")
  @GetMapping("/{id}")
  public ExerciseResponse getById(@PathVariable UUID id) {
    Exercise exercise =
        repository.findById(id).orElseThrow(() -> new NotFoundException("Exercise", id));
    return addItemLinks(mapper.toResponse(exercise));
  }

  private ExerciseResponse addItemLinks(ExerciseResponse resp) {
    resp.add(linkTo(methodOn(ExerciseController.class).getById(resp.getId())).withSelfRel());
    resp.add(Link.of(ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/v1/exercises").toUriString())
            .withRel("collection"));
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

  private Predicate<Exercise> predicateFor(FilterCriterion c) {
    return switch (c.field()) {
      case "name" -> FilterMatchers.string(Exercise::getName, c);
      case "category" -> FilterMatchers.string(e -> e.getCategory().name(), c);
      case "muscleGroup" -> FilterMatchers.string(Exercise::getMuscleGroup, c);
      case "unit" -> FilterMatchers.string(e -> e.getUnit().name(), c);
      default ->
          throw new IllegalArgumentException("Champ de filtre non supporte : " + c.field());
    };
  }

  private Comparator<Exercise> comparatorFor(SortSpec sort) {
    return switch (sort.field()) {
      case "name" -> sort.toComparator(Exercise::getName);
      case "category" -> sort.toComparator(e -> e.getCategory().name());
      case "muscleGroup" -> sort.toComparator(Exercise::getMuscleGroup);
      default -> sort.toComparator(Exercise::getName);
    };
  }
}
