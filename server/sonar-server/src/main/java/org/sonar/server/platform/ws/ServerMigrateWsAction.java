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
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.platform.monitoring.DatabaseMonitorMBean;

public class ServerMigrateWsAction implements ServerWsAction {

  private final ServerUpgradeStatus serverUpgradeStatus;
  private final DatabaseMonitorMBean databaseMonitor;
  private final DBMigration dbMigration;

  public ServerMigrateWsAction(ServerUpgradeStatus serverUpgradeStatus,
                               DatabaseMonitorMBean databaseMonitor,
                               DBMigration dbMigration) {
    this.serverUpgradeStatus = serverUpgradeStatus;
    this.databaseMonitor = databaseMonitor;
    this.dbMigration = dbMigration;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("migrate")
        .setDescription("Migrate the database to match the current version of SonarQube")
        .setSince("5.2")
        .setPost(true)
        .setHandler(this)
        .setResponseExample(Resources.getResource(this.getClass(), "example-migrate.json"));
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    if (serverUpgradeStatus.isUpgraded())  {
//      writeNoMigrationResponse(response);
    }
//    if (databaseMonitor.attributes().get(DatabaseMonitor.DatabaseAttributes.PRODUCT))
  }
}
