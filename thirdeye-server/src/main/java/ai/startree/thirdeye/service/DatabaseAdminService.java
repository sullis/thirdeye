/*
 * Copyright 2024 StarTree Inc
 *
 * Licensed under the StarTree Community License (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.startree.ai/legal/startree-community-license
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT * WARRANTIES OF ANY KIND,
 * either express or implied.
 * See the License for the specific language governing permissions and limitations under
 * the License.
 */
package ai.startree.thirdeye.service;

import static ai.startree.thirdeye.util.ResourceUtils.resultSetToMap;

import ai.startree.thirdeye.datalayer.DatabaseAdministratorClient;
import ai.startree.thirdeye.resources.DatabaseAdminResource;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
// FIXME CYRIL - does not have authorization and workspace checks everywhere because it is not exposed in production - add checks everywhere when time just in case - or remove
public class DatabaseAdminService {

  private static final Logger LOG = LoggerFactory.getLogger(DatabaseAdminResource.class);

  private final DatabaseAdministratorClient databaseAdministrator;

  @Inject
  public DatabaseAdminService(final DatabaseAdministratorClient databaseAdministrator) {
    this.databaseAdministrator = databaseAdministrator;
  }

  public List<String> getTables() throws SQLException {
    return databaseAdministrator.getTables();
  }

  @NonNull
  public List<Map<String, Object>> executeQuery(final String sql) throws SQLException {
    return resultSetToMap(databaseAdministrator.executeQuery(sql));
  }

  public void createAllTables() throws SQLException, IOException {
    databaseAdministrator.createAllTables();
  }

  public void deleteAllData() throws SQLException {
    LOG.warn("DELETING ALL DATABASE DATA!!! TRUNCATING TABLES!!!");
    databaseAdministrator.truncateTables();
  }

  public void dropAllTables() throws SQLException {
    LOG.warn("DELETING ALL DATABASE TABLES!!! DROPPING TABLES!!!");
    databaseAdministrator.dropTables();
  }
}