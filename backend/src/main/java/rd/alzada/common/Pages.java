package rd.alzada.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class Pages {

  private Pages() {}

  public static Pageable desc(int page, int size) {
    return PageRequest.of(safePage(page), safeSize(size), Sort.by(Sort.Direction.DESC, "createdAt"));
  }

  public static Pageable unsorted(int page, int size) {
    return PageRequest.of(safePage(page), safeSize(size));
  }

  private static int safePage(int page) {
    return Math.max(page, 0);
  }

  private static int safeSize(int size) {
    return Math.min(Math.max(size == 0 ? 20 : size, 1), 100);
  }
}
