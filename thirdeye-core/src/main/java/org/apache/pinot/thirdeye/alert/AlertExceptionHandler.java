package org.apache.pinot.thirdeye.alert;

import static org.apache.pinot.thirdeye.spi.ThirdEyeStatus.ERR_DATA_UNAVAILABLE;
import static org.apache.pinot.thirdeye.spi.ThirdEyeStatus.ERR_TIMEOUT;
import static org.apache.pinot.thirdeye.spi.ThirdEyeStatus.ERR_UNKNOWN;
import static org.apache.pinot.thirdeye.util.ResourceUtils.serverError;
import static org.apache.pinot.thirdeye.util.ResourceUtils.statusApi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.apache.pinot.thirdeye.detection.DataProviderException;
import org.apache.pinot.thirdeye.spi.ThirdEyeException;
import org.apache.pinot.thirdeye.spi.api.StatusApi;
import org.apache.pinot.thirdeye.spi.api.StatusListApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlertExceptionHandler {

  protected static final Logger LOG = LoggerFactory.getLogger(AlertExceptionHandler.class);

  public static void handleAlertEvaluationException(final Exception e) {
    LOG.error("Error in Alert Evaluation", e);
    final StatusListApi statusListApi = new StatusListApi().setList(new ArrayList<>());

    populateStatusListApi(e, statusListApi);
    throw serverError(statusListApi);
  }

  private static void populateStatusListApi(final Throwable e, StatusListApi statusListApi) {
    final List<StatusApi> l = statusListApi.getList();
    if (e instanceof TimeoutException) {
      l.add(statusApi(ERR_TIMEOUT));
    } else if (e instanceof DataProviderException) {
      l.add(statusApi(ERR_DATA_UNAVAILABLE, e.getMessage()));
    } else if (e instanceof ThirdEyeException) {
      final ThirdEyeException thirdEyeException = (ThirdEyeException) e;
      l.add(new StatusApi()
          .setCode(thirdEyeException.getStatus())
          .setMsg(thirdEyeException.getMessage()));
    } else  {
      l.add(statusApi(ERR_UNKNOWN, e.getMessage()));
    }
    if (e.getCause() != null) {
      populateStatusListApi(e.getCause(), statusListApi);
    }
  }
}
