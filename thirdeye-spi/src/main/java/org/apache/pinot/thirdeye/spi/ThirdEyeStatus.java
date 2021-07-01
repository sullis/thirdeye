package org.apache.pinot.thirdeye.spi;

public enum ThirdEyeStatus {

  ERR_DATASOURCE_NOT_FOUND("Data Source not found! %s"),
  ERR_DATASET_NOT_FOUND("Dataset not found! %s"),
  ERR_MULTIPLE_DATASETS_FOUND(
      "Multiple datasets found based on the dataset's display name %s, candidates: %s"),
  ERR_DATA_UNAVAILABLE("Data not available! %s"),
  ERR_CRON_INVALID("Failed to parse cron expression: %s"),
  ERR_DUPLICATE_NAME("Name must be unique!"),
  ERR_MISSING_ID("ID is null!"),
  ERR_UNEXPECTED_QUERY_PARAM("Unexpected Query Param. Allowed values: %s"),
  ERR_ID_UNEXPECTED_AT_CREATION("ID should be null at creation time."),
  ERR_INVALID_QUERY_PARAM_OPERATOR("Invalid operator for query param. Allowed Values:"),
  ERR_OBJECT_UNEXPECTED("Object should be null/empty! %s"),
  ERR_OBJECT_DOES_NOT_EXIST("Object does not exist! %s"),
  ERR_OPERATION_UNSUPPORTED("Operation is not supported!"),
  ERR_CONFIG("Configuration Error! %s"),
  ERR_TIMEOUT("Operation timed out!"),
  ERR_UNKNOWN("%s");

  final String message;

  ThirdEyeStatus(final String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}
