package com.linkedin.thirdeye.alert.content;

import com.google.common.base.Joiner;
import com.linkedin.thirdeye.anomaly.ThirdEyeAnomalyConfiguration;
import com.linkedin.thirdeye.anomaly.events.EventType;
import com.linkedin.thirdeye.anomalydetection.context.AnomalyFeedback;
import com.linkedin.thirdeye.anomalydetection.context.AnomalyResult;
import com.linkedin.thirdeye.datalayer.dto.AnomalyFunctionDTO;
import com.linkedin.thirdeye.datalayer.dto.EventDTO;
import com.linkedin.thirdeye.datalayer.dto.MergedAnomalyResultDTO;
import com.linkedin.thirdeye.datalayer.dto.RawAnomalyResultDTO;
import com.linkedin.thirdeye.datalayer.pojo.AlertConfigBean.COMPARE_MODE;
import com.linkedin.thirdeye.util.ThirdEyeUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This email content formatter provides a hierarchical view of anomalies. It categorizes the anomalies by its dimensions.
 * The top-level anomalies are defined as anomalies generated by anomaly function without dimension drill-down; otherwise,
 * it is in the lower-level anomalies. The content formatter takes hierarchical-anomalies-email-template.ftl in default.
 */
public class HierarchicalAnomaliesEmailContentFormatter extends BaseEmailContentFormatter{
  private static final Logger LOG = LoggerFactory.getLogger(HierarchicalAnomaliesEmailContentFormatter.class);

  public static final String EMAIL_TEMPLATE = "emailTemplate";
  public static final String USE_LATEST_ANOMALY_INFORMATION = "useLatestAnomaly";
  public static final String PRESENT_SEASONAL_VALUES = "presentSeasonalValues";

  public static final String DEFAULT_USE_LATEST_ANOMALY_INFORMATION = "true";
  public static final String DEFAULT_PRESENT_SEASONAL_VALUES = "false";

  public static final String DEFAULT_EMAIL_TEMPLATE = "hierarchical-anomalies-email-template.ftl";

  private boolean useLatestAnomaly;
  private boolean presentSeasonalValues;
  private Set<EventDTO> relatedEvents;
  public HierarchicalAnomaliesEmailContentFormatter(){
    relatedEvents = new HashSet<>();
  }

  @Override
  public void init(Properties properties, ThirdEyeAnomalyConfiguration configuration) {
    super.init(properties, configuration);
    this.emailTemplate = properties.getProperty(EMAIL_TEMPLATE, DEFAULT_EMAIL_TEMPLATE);
    useLatestAnomaly = Boolean.valueOf(properties.getProperty(USE_LATEST_ANOMALY_INFORMATION, DEFAULT_USE_LATEST_ANOMALY_INFORMATION));
    presentSeasonalValues = Boolean.valueOf(properties.getProperty(PRESENT_SEASONAL_VALUES, DEFAULT_PRESENT_SEASONAL_VALUES));
  }

  @Override
  protected void updateTemplateDataByAnomalyResults(Map<String, Object> templateData,
      Collection<AnomalyResult> anomalies) {
    List<AnomalyReportEntity> rootAnomalyDetails = new ArrayList<>();
    SortedMap<String, List<AnomalyReportEntity>> leafAnomalyDetails = new TreeMap<>();
    List<String> anomalyIds = new ArrayList<>();
    List<AnomalyResult> anomalyList = new ArrayList<>(anomalies);
    Collections.sort(anomalyList, new Comparator<AnomalyResult>() {
      @Override
      public int compare(AnomalyResult o1, AnomalyResult o2) {
        return Double.compare(o1.getWeight(), o2.getWeight());
      }
    });

    for (AnomalyResult anomalyResult : anomalyList) {
      if (!(anomalyResult instanceof MergedAnomalyResultDTO)) {
        LOG.warn("Anomaly result {} isn't an instance of MergedAnomalyResultDTO. Skip from alert.", anomalyResult);
        continue;
      }
      MergedAnomalyResultDTO anomaly = (MergedAnomalyResultDTO) anomalyResult;

      // include notified alerts only in the email
      if (includeSentAnomaliesOnly) {
        if (anomaly.isNotified()) {
          putAnomaliesIntoRootOrLeaf(anomaly, rootAnomalyDetails, leafAnomalyDetails);
          anomalyIds.add(Long.toString(anomaly.getId()));
        }
      } else {
        putAnomaliesIntoRootOrLeaf(anomaly, rootAnomalyDetails, leafAnomalyDetails);
        anomalyIds.add(Long.toString(anomaly.getId()));
      }
    }
    List<EventDTO> sortedEvents = new ArrayList<>(relatedEvents);
    Collections.sort(sortedEvents, new Comparator<EventDTO>() {
      @Override
      public int compare(EventDTO o1, EventDTO o2) {
        return Long.compare(o1.getStartTime(), o2.getStartTime());
      }
    });
    templateData.put("containsSeasonal", presentSeasonalValues);
    templateData.put("rootAnomalyDetails", rootAnomalyDetails);
    templateData.put("leafAnomalyDetails", leafAnomalyDetails);
    templateData.put("holidays", sortedEvents);
    templateData.put("anomalyIds", Joiner.on(",").join(anomalyIds));
  }

  /**
   * Generate the AnomalyReportEntity
   * @param anomaly
   * @param dashboardHost
   * @return
   */
  private AnomalyReportEntity generateAnomalyReportEntity(MergedAnomalyResultDTO anomaly, String dashboardHost) {
    RawAnomalyResultDTO latestRawAnomaly = getLatestRawAnomalyResult(anomaly);

    AnomalyFeedback feedback = anomaly.getFeedback();

    String feedbackVal = getFeedbackValue(feedback);

    AnomalyReportEntity
        anomalyReport = new AnomalyReportEntity(String.valueOf(anomaly.getId()),
        getAnomalyURL(anomaly, dashboardHost),
        ThirdEyeUtils.getRoundedValue(anomaly.getAvgBaselineVal()),
        ThirdEyeUtils.getRoundedValue(anomaly.getAvgCurrentVal()),
        anomaly.getImpactToGlobal(),
        getDimensionsList(anomaly.getDimensions()),
        getTimeDiffInHours(anomaly.getStartTime(), anomaly.getEndTime()), // duration
        feedbackVal,
        anomaly.getFunction().getFunctionName(),
        anomaly.getMetric(),
        getDateString(anomaly.getStartTime(), dateTimeZone),
        getDateString(anomaly.getEndTime(), dateTimeZone),
        getTimezoneString(dateTimeZone),
        getIssueType(anomaly)
    );

    List<String> affectedCountries = getMatchedFilterValues(anomaly, "country");
    if (affectedCountries.size() > 0) { // if the anomaly is on country level
      Map<String, List<String>> targetDimensions = new HashMap<>();
      targetDimensions.put(EVENT_FILTER_COUNTRY, affectedCountries);
      relatedEvents.addAll(getRelatedEvents(EventType.HOLIDAY,
          new DateTime(anomaly.getStartTime(), dateTimeZone), new DateTime(anomaly.getEndTime(), dateTimeZone),
          null, null, targetDimensions));
    }

    if (useLatestAnomaly && latestRawAnomaly != null) {
      anomalyReport.setCurrentVal(ThirdEyeUtils.getRoundedValue(latestRawAnomaly.getAvgCurrentVal()));
      anomalyReport.setBaselineVal(ThirdEyeUtils.getRoundedValue(latestRawAnomaly.getAvgBaselineVal()));
    }
    return anomalyReport;
  }

  /**
   * Generate the AnomalyReportEntity and determine if the given anomaly is root or leaf level
   * @param anomaly
   * @param rootAnomalyDetail
   * @param leafAnomalyDetail
   * @return
   */
  private AnomalyReportEntity putAnomaliesIntoRootOrLeaf(MergedAnomalyResultDTO anomaly,
      List<AnomalyReportEntity> rootAnomalyDetail, SortedMap<String, List<AnomalyReportEntity>> leafAnomalyDetail){
    AnomalyReportEntity anomalyReport = generateAnomalyReportEntity(anomaly, THIRDEYE_CONFIG.getDashboardHost());
    AnomalyFunctionDTO anomalyFunction = anomaly.getFunction();
    String exploredDimensions = anomalyFunction.getExploreDimensions();
    // Add WoW number
    if (presentSeasonalValues) {
      try {
        for (COMPARE_MODE compareMode : COMPARE_MODE.values()) {
          double avgValues = getAvgComparisonBaseline(anomaly, compareMode, anomaly.getStartTime(), anomaly.getEndTime());
          anomalyReport.setSeasonalValues(compareMode, avgValues, anomaly.getAvgCurrentVal());
        }
      } catch (Exception e) {
        LOG.warn("Unable to fetch wow information for {}.", anomalyFunction);
      }
    }
    if (StringUtils.isBlank(exploredDimensions)) {
      rootAnomalyDetail.add(anomalyReport);
    } else {
      if (!leafAnomalyDetail.containsKey(exploredDimensions)) {
        leafAnomalyDetail.put(exploredDimensions, new ArrayList<AnomalyReportEntity>());
      }
      leafAnomalyDetail.get(exploredDimensions).add(anomalyReport);
    }
    return anomalyReport;
  }
}
