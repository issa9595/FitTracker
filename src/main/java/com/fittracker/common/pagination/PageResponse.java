package com.fittracker.common.pagination;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;

/**
 * Reponse paginee offset/limit, incluant les liens HATEOAS self/first/prev/next/last.
 *
 * @param <T> type des elements
 */
@Schema(description = "Page de resultats avec metadata et liens HATEOAS")
public class PageResponse<T> extends RepresentationModel<PageResponse<T>> {

  private final List<T> content;
  private final int page;
  private final int size;
  private final long totalElements;
  private final int totalPages;

  public PageResponse(List<T> content, int page, int size, long totalElements) {
    this.content = content;
    this.page = page;
    this.size = size;
    this.totalElements = totalElements;
    this.totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
  }

  public PageResponse(List<T> content, PageRequest request, long totalElements) {
    this(content, request.page(), request.size(), totalElements);
  }

  public PageResponse<T> withNavigationLinks(java.util.function.Function<Integer, Link> linkBuilder) {
    add(linkBuilder.apply(page).withSelfRel());
    add(linkBuilder.apply(0).withRel("first"));
    if (page > 0) {
      add(linkBuilder.apply(page - 1).withRel("prev"));
    }
    if (page < totalPages - 1) {
      add(linkBuilder.apply(page + 1).withRel("next"));
    }
    if (totalPages > 0) {
      add(linkBuilder.apply(totalPages - 1).withRel("last"));
    }
    return this;
  }

  public List<T> getContent() {
    return content;
  }

  public int getPage() {
    return page;
  }

  public int getSize() {
    return size;
  }

  public long getTotalElements() {
    return totalElements;
  }

  public int getTotalPages() {
    return totalPages;
  }
}
