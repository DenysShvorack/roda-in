package org.roda.rodain.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.net.util.Base64;
import org.roda.rodain.core.AppProperties;
import org.roda.rodain.rules.MetadataOptions;
import org.roda.rodain.rules.TemplateToForm;
import org.roda.rodain.sip.MetadataValue;
import org.roda.rodain.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Andre Pereira apereira@keep.pt
 * @since 07-12-2015.
 */
@JsonIgnoreProperties({"path", "loaded", "version"})
public class DescObjMetadata {
  private static final Logger LOGGER = LoggerFactory.getLogger(DescObjMetadata.class.getName());
  private String id, content, contentEncoding, metadataType;
  private Map<String, Object> additionalProperties = new HashMap<>();
  private TreeSet<MetadataValue> values;
  private Path path;
  private boolean loaded = false;

  // template
  private MetadataOptions creatorOption;
  private String version, templateType;

  public DescObjMetadata() {
    creatorOption = MetadataOptions.NEW_FILE;
  }

  public DescObjMetadata(MetadataOptions creatorOption, String templateType, String version) {
    this.creatorOption = creatorOption;
    this.templateType = templateType;
    this.version = version;
    this.contentEncoding = "Base64";
    this.id = templateType + ".xml";
    this.metadataType = templateType;
  }

  public DescObjMetadata(MetadataOptions creatorOption, Path path, String metadataType) {
    this.creatorOption = creatorOption;
    this.path = path;
    this.metadataType = metadataType;
    this.contentEncoding = "Base64";
    if (path != null) {
      this.id = path.getFileName().toString();
    }
  }

  @JsonIgnore
  public Path getPath() {
    return path;
  }

  @JsonIgnore
  public boolean isLoaded() {
    return loaded;
  }

  public MetadataOptions getCreatorOption() {
    return creatorOption;
  }

  public String getMetadataType() {
    return metadataType;
  }

  public void setCreatorOption(MetadataOptions creatorOption) {
    this.creatorOption = creatorOption;
  }

  /**
   * @return The set of MetadataValue objects. Used to create the form.
   */
  public Set<MetadataValue> getValues() {
    if (values == null) {
      values = TemplateToForm.createSet(getContentDecoded());
    }
    return values;
  }

  public void setValues(TreeSet<MetadataValue> val) {
    this.values = val;
  }

  public String getVersion() {
    return version;
  }

  public String getTemplateType() {
    return templateType;
  }

  /**
   * Gets the id of the description object metadata.
   *
   * @return The id
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the id of the description object metadata.
   *
   * @param id
   *          The id
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Gets the content of the description object metadata.
   *
   * @return The content
   */
  public String getContent() {
    if (content == null) {
      loadMetadata();
    }
    return this.content;
  }

  /**
   * Gets the decoded content of the description object metadata.
   *
   * @return The content decoded
   */
  @JsonIgnore
  public String getContentDecoded() {
    if (!loaded) {
      loadMetadata();
    }
    if (content != null) {
      byte[] decoded = Base64.decodeBase64(content);
      return new String(decoded);
    } else
      return "";
  }

  private void loadMetadata() {
    try {
      if (creatorOption == MetadataOptions.TEMPLATE) {
        if (templateType != null) {
          String tempContent = AppProperties.getMetadataFile(templateType);
          setContentDecoded(tempContent);
          loaded = true;
        }
      } else {
        if (path != null) {
          String tempContent = Utils.readFile(path.toString(), Charset.defaultCharset());
          setContentDecoded(tempContent);
          loaded = true;
        }
      }
    } catch (IOException e) {
      LOGGER.error("Error reading metadata file", e);
    }
  }

  /**
   * Sets the content of the description object metadata.
   *
   * @param content
   *          The content encoded in Base64
   */
  public void setContent(String content) {
    this.content = content;
  }

  /**
   * Sets the content of the description object metadata.
   *
   * @param content
   *          The decoded content
   */
  public void setContentDecoded(String content) {
    if (content != null) {
      byte[] encoded = Base64.encodeBase64(content.getBytes());
      this.content = new String(encoded);
    }
  }

  /**
   * Gets the content encoding of the description object metadata.
   *
   * @return The content encoding
   */
  public String getContentEncoding() {
    return contentEncoding;
  }

  /**
   * Sets the content encoding of the description object metadata.
   *
   * @param contentEncoding
   *          The contentEncoding
   */
  public void setContentEncoding(String contentEncoding) {
    this.contentEncoding = contentEncoding;
  }

  @JsonIgnore
  public String getSchema() {
    String result = null;
    if (templateType != null) {
      result = AppProperties.getSchemaFile(templateType);
    } else {
      if (path != null)
        result = AppProperties.getSchemaFile(FilenameUtils.removeExtension(path.getFileName().toString()));
    }
    return result;
  }

  /**
   * Gets the additional properties map.
   *
   * @return The additional properties map.
   */
  public Map<String, Object> getAdditionalProperties() {
    return this.additionalProperties;
  }

  /**
   * Sets an additional property.
   *
   * @param name
   *          The name of the property.
   * @param value
   *          The value of the property.
   */
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }

}