/* (C) 2024 */
package oxygen.annotations.generator;

/**
 * ModMetaObj
 */
public class ModMetadata {
  public String name, minGameVersion, displayName, author, version, description, repo, subtitle;
  public String[] dependencies;
  public String main;
  public Boolean pregenerated, hidden, keepOutlines, java;
  public Float texturescale;

  public void set(ModMetaG meta) {
    name = meta.name();
    minGameVersion = meta.minGameVersion();
    displayName = meta.displayName();
    author = meta.author();
    version = meta.version();
    dependencies = meta.dependencies();
    repo = meta.repo();
    subtitle = meta.subtitle();
    description = meta.description();
    pregenerated = meta.pregenerated();
    hidden = meta.hidden();
    keepOutlines = meta.keepOutlines();
    java = meta.java();
    texturescale = meta.texturescale();
  }

  public void Null() {
    pregenerated = pregenerated ? pregenerated : null;
    hidden = hidden ? hidden : null;
    keepOutlines = keepOutlines ? keepOutlines : null;
    java = java ? java : null;
    texturescale = texturescale == 1.0f ? null : texturescale;
    displayName = displayName.isEmpty() ? null : displayName;
    author = author.isEmpty() ? null : author;
    version = version.isEmpty() ? null : version;
    description = description.isEmpty() ? null : description;
    repo = repo.isEmpty() ? null : repo;
    dependencies = dependencies.length == 0 ? null : dependencies;
    subtitle = subtitle.isEmpty() ? null : subtitle;
  }

  public ModMetadata(ModMetaG meta, String clazz, boolean Null) {
    this(meta, clazz);
    Null();
  }

  public ModMetadata(ModMetaG meta, String clazz) {
    set(meta);
    this.main = clazz;
  }
}
