package org.apache.pinot.thirdeye.spi.detection.v2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.pinot.thirdeye.spi.dataframe.DataFrame;

public interface DataTable extends DetectionPipelineResult {

  static Object[] getRow(final DataTable dataTable, final int rowIdx) {
    int columnCount = dataTable.getColumnCount();
    Object[] row = new Object[columnCount];
    for (int colIdx = 0; colIdx < columnCount; colIdx++) {
      row[colIdx] = dataTable.getObject(rowIdx, colIdx);
    }
    return row;
  }

  static Map<String, Object> getRecord(final List<String> columnNames, final Object[] event) {
    Map<String, Object> record = new HashMap<>();
    for (int i = 0; i < columnNames.size(); i++) {
      record.put(columnNames.get(i), event[i]);
    }
    return record;
  }

  int getRowCount();

  int getColumnCount();

  List<String> getColumns();

  List<ColumnType> getColumnTypes();

  DataFrame getDataFrame();

  Object getObject(int rowIdx, int colIdx);

  String getString(int rowIdx, int colIdx);

  long getLong(int rowIdx, int colIdx);

  double getDouble(int rowIdx, int colIdx);
}

