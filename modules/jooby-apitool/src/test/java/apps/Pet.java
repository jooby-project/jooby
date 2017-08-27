package apps;

import java.util.List;

public class Pet {

  private Long id;

  private String name;

  private Category category;

  private List<String> photoUrls;

  private List<Tag> tags;

  private Status status;

  public Pet() {
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public Long getId() {
    return id;
  }

  public void setId(final Long id) {
    this.id = id;
  }

  public Category getCategory() {
    return category;
  }

  public void setCategory(final Category category) {
    this.category = category;
  }

  public List<String> getPhotoUrls() {
    return photoUrls;
  }

  public void setPhotoUrls(final List<String> photoUrls) {
    this.photoUrls = photoUrls;
  }

  public List<Tag> getTags() {
    return tags;
  }

  public void setTags(final List<Tag> tags) {
    this.tags = tags;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(final Status status) {
    this.status = status;
  }
}
