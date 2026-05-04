package ar.edu.itba.paw.models;

import java.util.List;

public class PaginatedResult<T> {

    private final List<T> items;
    private final int totalCount;
    private final int page;
    private final int pageSize;

    public PaginatedResult(
            final List<T> items, final int totalCount, final int page, final int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be positive, got: " + pageSize);
        }
        this.items = items;
        this.totalCount = totalCount;
        this.page = page;
        this.pageSize = pageSize;
    }

    public List<T> getItems() {
        return items;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotalPages() {
        if (totalCount == 0) {
            return 1;
        }
        return (int) Math.ceil((double) totalCount / pageSize);
    }

    public boolean hasPrevious() {
        return page > 1;
    }

    public boolean hasNext() {
        return page < getTotalPages();
    }

    @Override
    public String toString() {
        return "PaginatedResult{"
                + "itemCount="
                + (items == null ? 0 : items.size())
                + ", totalCount="
                + totalCount
                + ", page="
                + page
                + ", pageSize="
                + pageSize
                + ", totalPages="
                + getTotalPages()
                + '}';
    }
}
