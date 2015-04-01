/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.platform.ws;

import com.google.common.io.Resources;
import org.sonar.api.platform.Server;
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.persistence.Database;
import org.sonar.server.db.migration.DatabaseMigration;

/**
 * SystemStatusWsAction
 *
 * @author Sébastien Lesaint (sebastien.lesaint@sonarsource.com)
 */
public class SystemStatusWsAction implements SystemWsAction {

  private final Server server;
  private final ServerUpgradeStatus serverUpgradeStatus;
  private final Database database;
  private final DatabaseMigration databaseMigration;

  public SystemStatusWsAction(Server server, ServerUpgradeStatus serverUpgradeStatus,
                              Database database, DatabaseMigration databaseMigration) {
    this.server = server;
    this.serverUpgradeStatus = serverUpgradeStatus;
    this.database = database;
    this.databaseMigration = databaseMigration;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("index")
      .setDescription("Get the server status:" +
        "<ul>" +
        "<li>UP</li>" +
        "<li>DOWN (generally for database connection failures)</li>" +
        "<li>SETUP (if the server must be upgraded)</li>" +
        "<li>MIGRATION_RUNNING (the upgrade process is currently running)</li>" +
        "</ul>")
      .setSince("5.2")
      .setResponseExample(Resources.getResource(this.getClass(), "example-status.json"))
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    Status status = computeStatus();

    writeJson(response, status);
  }

  private void writeJson(Response response, Status status) {
    JsonWriter json = response.newJsonWriter();
    json.beginObject();
    json.prop("id", server.getId());
    json.prop("version", server.getVersion());
    json.prop("status", status.toString());
    json.endObject();
    json.close();
  }

  private Status computeStatus() {
    if (!isConnectedToDB())  {
      return Status.DOWN;
    }

    MigrationStatus migrationStatus = computeMigrationStatus();
    switch (migrationStatus) {
      case NEEDED:
        return Status.SETUP;
      case RUNNING:
        return Status.MIGRATION_RUNNING;
      case SUCCESS:
      case UP_TO_DATE:
        return Status.UP;
      case FAILED:
      case NOT_SUPPORTED:
        return Status.DOWN;
      default:
        throw new IllegalArgumentException("Unsupported MigrationStatus value");
    }
  }

  private boolean isConnectedToDB() {
    // TODO check DB connection is up
    return true;
  }

  private MigrationStatus computeMigrationStatus() {
    if (serverUpgradeStatus.isUpgraded()) {
      return MigrationStatus.UP_TO_DATE;
    }
    if (!database.getDialect().supportsMigration()) {
      return MigrationStatus.NOT_SUPPORTED;
    }

    switch (databaseMigration.status()) {
      case NONE:
        return MigrationStatus.NEEDED;
      case RUNNING:
        return MigrationStatus.RUNNING;
      case FAILED:
        return MigrationStatus.FAILED;
      case SUCCEEDED: // we should never hit that branch of code since serverUpgradeStatus.isUpgraded() is true
        return MigrationStatus.UP_TO_DATE;
      default:
        throw new IllegalArgumentException("Unsupported DatabaseMigration.Status value");
    }
  }

  private enum Status {
    UP, DOWN, SETUP, MIGRATION_RUNNING
  }

  private enum MigrationStatus {
    NEEDED, RUNNING, FAILED, SUCCESS, UP_TO_DATE, NOT_SUPPORTED
  }
}
