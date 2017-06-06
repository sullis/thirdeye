package com.linkedin.thirdeye.anomaly.classification;

import com.linkedin.thirdeye.anomaly.task.TaskConstants;
import com.linkedin.thirdeye.anomaly.utils.AnomalyUtils;
import com.linkedin.thirdeye.datalayer.bao.AnomalyFunctionManager;
import com.linkedin.thirdeye.datalayer.bao.ClassificationConfigManager;
import com.linkedin.thirdeye.datalayer.bao.JobManager;
import com.linkedin.thirdeye.datalayer.bao.TaskManager;
import com.linkedin.thirdeye.datalayer.dto.AnomalyFunctionDTO;
import com.linkedin.thirdeye.datalayer.dto.ClassificationConfigDTO;
import com.linkedin.thirdeye.datalayer.dto.JobDTO;
import com.linkedin.thirdeye.datalayer.dto.TaskDTO;
import com.linkedin.thirdeye.datasource.DAORegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassificationJobScheduler implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(ClassificationJobScheduler.class);
  private static final DAORegistry DAO_REGISTRY = DAORegistry.getInstance();

  private ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
  private long maxMonitoringWindowSizeInMS = 259_200_000L; // 3 days
  private boolean forceSyncDetectionJobs = false;

  public ClassificationJobScheduler() { }

  public ClassificationJobScheduler(ClassificationJobConfig classificationJobConfig) {
    this.maxMonitoringWindowSizeInMS = classificationJobConfig.getMaxMonitoringWindowSizeInMS();
    this.forceSyncDetectionJobs = classificationJobConfig.getForceSyncDetectionJobs();
  }

  public void start() {
    LOG.info("Starting anomaly classification service");
    this.scheduledExecutorService.scheduleWithFixedDelay(this, 0, 1, TimeUnit.MINUTES);
  }

  public void shutdown() {
    AnomalyUtils.safelyShutdownExecutionService(scheduledExecutorService, this.getClass());
  }

  @Override
  public void run() {
    ClassificationConfigManager classificationConfigDAO = DAO_REGISTRY.getClassificationConfigDAO();

    List<ClassificationConfigDTO> classificationConfigs = classificationConfigDAO.findActives();

    for (ClassificationConfigDTO classificationConfig : classificationConfigs) {
      LOG.info("Running classifier: {}", classificationConfig);
      mainMetricTimeBasedClassification(classificationConfig);
    }
  }

  /**
   * Creates a classification job whose start time is the end time of the most recent classification job and end time
   * is the minimal end times of anomaly detection jobs, which are given through a configuration DTO. In the current
   * implementation, we assume that there exists single main anomaly function and it has to be activated in order to
   * classify of its anomalies.
   *
   * @param classificationConfig a configuration file which provides main and correlated anomaly functions.
   */
  private void mainMetricTimeBasedClassification(ClassificationConfigDTO classificationConfig) {
    AnomalyFunctionManager anomalyFunctionDAO = DAO_REGISTRY.getAnomalyFunctionDAO();
    JobManager jobDAO = DAO_REGISTRY.getJobDAO();

    // Get all involved anomaly functions that are activated
    List<AnomalyFunctionDTO> syncedAnomalyFunctions = new ArrayList<>();
    List<Long> functionIdList = classificationConfig.getFunctionIdList();
    for (long functionId : functionIdList) {
      AnomalyFunctionDTO anomalyFunctionDTO = anomalyFunctionDAO.findById(functionId);
      if (anomalyFunctionDTO.getIsActive()) {
        syncedAnomalyFunctions.add(anomalyFunctionDTO);
      }
    }
    // TODO: Determine if funnel effect has main metric. If it does not, then remove the block below
    if (!functionIdList.contains(classificationConfig.getMainFunctionId())) {
      AnomalyFunctionDTO mainAnomalyFunction = anomalyFunctionDAO.findById(classificationConfig.getMainFunctionId());
      if (mainAnomalyFunction.getIsActive()) {
        syncedAnomalyFunctions.add(mainAnomalyFunction);
      } else {
        LOG.info("Main anomaly function is not activated. Classification job: {} is skipped.", classificationConfig);
        return;
      }
    }

    // By default, we look at only the anomalies in a certain time window just in case of that the previous
    // classification job does not exists or was executed long time ago.
    //   minTimeBoundary is the start time of the boundary window.
    long currentMillis = System.currentTimeMillis();
    long minTimeBoundary = currentMillis - maxMonitoringWindowSizeInMS;

    // Check the latest detection time among all anomaly functions in this classification config.
    // If an activated anomaly function does not have any detection jobs that are executed within the time window
    // [minTimeBoundary, currentMillis), then it is ignored (i.e., minDetectionEndTime is computed without its endTime).
    long minDetectionEndTime = Long.MAX_VALUE;
    for (AnomalyFunctionDTO anomalyFunctionDTO : syncedAnomalyFunctions) {
      JobDTO job =
          findLatestCompletedJobByTypeAndConfigId(TaskConstants.TaskType.ANOMALY_DETECTION, anomalyFunctionDTO.getId(),
              minTimeBoundary);
      if (job != null && job.getWindowEndTime() >= minTimeBoundary) {
        minDetectionEndTime = Math.min(minDetectionEndTime, job.getWindowEndTime());
      } else {
        if (forceSyncDetectionJobs) {
          minDetectionEndTime = Long.MAX_VALUE;
          LOG.warn("Could not find most recent executed job for function {}; aborting classification job {id={}}",
              anomalyFunctionDTO.getId(), classificationConfig.getId());
          break;
        }
      }
    }
    if (minDetectionEndTime == Long.MAX_VALUE) {
      return;
    }
    long endTime = minDetectionEndTime;

    long startTime = Math.max(minTimeBoundary, classificationConfig.getEndTimeWatermark());
    if (startTime >= endTime) {
      LOG.info(
          "Skipped classification job (id={}) because min detection end time among all anomaly functions {} is not larger than last window end time {}",
          classificationConfig.getId(), minDetectionEndTime, startTime);
      return;
    }

    // Create classification job
    ClassificationJobContext jobContext = new ClassificationJobContext();
    jobContext.setWindowStartTime(startTime);
    jobContext.setWindowEndTime(endTime);
    jobContext.setConfigDTO(classificationConfig);
    ClassificationJobRunner jobRunner = new ClassificationJobRunner(jobContext);
    jobRunner.run();
  }

  private JobDTO findLatestCompletedJobByTypeAndConfigId(TaskConstants.TaskType taskType, long configId,
      long minTimeBoundary) {
    List<JobDTO> jobDTOs =
        DAO_REGISTRY.getJobDAO().findRecentScheduledJobByTypeAndConfigId(taskType, configId, minTimeBoundary);
    if (CollectionUtils.isNotEmpty(jobDTOs)) {
      return findLatestCompletedJob(jobDTOs);
    } else {
      return null;
    }
  }

  private JobDTO findLatestCompletedJob(List<JobDTO> jobs) {
    TaskManager taskDAO = DAO_REGISTRY.getTaskDAO();
    if (CollectionUtils.isNotEmpty(jobs)) {
      for (JobDTO job : jobs) {
        List<TaskDTO> taskDTOS = taskDAO.findByJobIdStatusNotIn(job.getId(), TaskConstants.TaskStatus.COMPLETED);
        if (CollectionUtils.isEmpty(taskDTOS)) {
          return job;
        }
      }
    }
    return null;
  }
}
