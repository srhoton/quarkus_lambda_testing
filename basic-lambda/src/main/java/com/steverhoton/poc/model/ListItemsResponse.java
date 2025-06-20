package com.steverhoton.poc.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response model for listing items with pagination.
 *
 * <p>Contains a list of items and pagination information for cursor-based navigation.
 */
public class ListItemsResponse {

  @JsonProperty("items")
  private List<ItemResponse> items;

  @JsonProperty("next_cursor")
  private String nextCursor;

  @JsonProperty("has_more")
  private boolean hasMore;

  @JsonProperty("count")
  private int count;

  public ListItemsResponse() {}

  public ListItemsResponse(
      List<ItemResponse> items, String nextCursor, boolean hasMore, int count) {
    this.items = items;
    this.nextCursor = nextCursor;
    this.hasMore = hasMore;
    this.count = count;
  }

  public List<ItemResponse> getItems() {
    return items;
  }

  public void setItems(List<ItemResponse> items) {
    this.items = items;
  }

  public String getNextCursor() {
    return nextCursor;
  }

  public void setNextCursor(String nextCursor) {
    this.nextCursor = nextCursor;
  }

  public boolean isHasMore() {
    return hasMore;
  }

  public void setHasMore(boolean hasMore) {
    this.hasMore = hasMore;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }
}
