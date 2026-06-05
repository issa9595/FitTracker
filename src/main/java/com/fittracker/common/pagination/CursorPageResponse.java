package com.fittracker.common.pagination;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;

@Schema(description = "Page par curseur. Quand nextCursor est null, on est en fin de flux.")
public class CursorPageResponse<T> extends RepresentationModel<CursorPageResponse<T>> {

  private final List<T> content;
  private final String nextCursor;
  private final int size;

  public CursorPageResponse(List<T> content, String nextCursor, int size) {
    this.content = content;
    this.nextCursor = nextCursor;
    this.size = size;
  }

  public CursorPageResponse<T> withNavigationLinks(
      Link selfLink, java.util.function.Function<String, Link> nextBuilder) {
    add(selfLink.withSelfRel());
    if (nextCursor != null) {
      add(nextBuilder.apply(nextCursor).withRel("next"));
    }
    return this;
  }

  public List<T> getContent() {
    return content;
  }

  public String getNextCursor() {
    return nextCursor;
  }

  public int getSize() {
    return size;
  }
}
