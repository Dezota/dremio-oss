/*
 * Copyright (C) 2017-2019 Dremio Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dremio.service.nessie;

import java.util.Map;

import org.projectnessie.versioned.persist.adapter.DatabaseAdapter;

import com.dremio.options.OptionManager;
import com.dremio.options.Options;
import com.dremio.options.TypeValidators;
import com.dremio.service.scheduler.Schedule;
import com.dremio.service.scheduler.SchedulerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * A scheduled task that runs Nessie Database Adapter maintenance operations.
 */
@Options
public class NessieRepoMaintenanceTask {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NessieRepoMaintenanceTask.class);

  public static final TypeValidators.PositiveLongValidator MAINTENANCE_PERIOD_MINUTES = new TypeValidators.PositiveLongValidator(
    "nessie.kvversionstore.maintenance.period_minutes", Integer.MAX_VALUE, 4 * 60L); // 4 hours

  private final DatabaseAdapter adapter;
  private final OptionManager optionManager;
  private final ObjectMapper mapper = new ObjectMapper();

  public NessieRepoMaintenanceTask(DatabaseAdapter adapter, OptionManager optionManager) {
    this.adapter = adapter;
    this.optionManager = optionManager;
  }

  public void schedule(SchedulerService schedulerService) {
    Schedule schedule = Schedule.Builder.everyMinutes(
        optionManager.getOption(MAINTENANCE_PERIOD_MINUTES))
      .build();
    schedulerService.schedule(schedule, this::runRepoMaintenance);
    logger.info("Scheduled repo maintenance at interval {}", schedule.getPeriod());
  }

  private void runRepoMaintenance() {
    try {
      logger.debug("Starting Nessie repository maintenance");
      EmbeddedRepoMaintenanceParams params = EmbeddedRepoMaintenanceParams.builder()
        .setEmbeddedRepoPurgeParams(EmbeddedRepoPurgeParams.builder().setDryRun(false).build())
        .build();

      long t = System.currentTimeMillis();
      Map<String, Map<String, String>> result = adapter.repoMaintenance(params);
      t = System.currentTimeMillis() - t;

      // Pretty print maintenance output to the log as JSON
      ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
      String stringResult = writer.writeValueAsString(result);
      logger.info("Completed Nessie repository maintenance in {} ms with output: {}", t, stringResult);
    } catch (Exception e) {
      logger.error("Nessie repository maintenance failed.", e);
    }
  }
}