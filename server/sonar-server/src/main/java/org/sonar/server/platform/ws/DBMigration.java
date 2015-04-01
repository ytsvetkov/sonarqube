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

public interface DBMigration {
  /**
   * Starts a DB migration to the schema that matches the current version of SonarQube.
   * <p>
   * This method throws an {@link IllegalStateException} when called as a db migration is already running. To avoid
   * getting an exception, one should check the value returned by {@link #running()} before calling this method.
   * </p>
   *
   * @throws IllegalStateException if the DB migration is already running
   */
  void start() throws IllegalStateException;

  /**
   * Indicates whether a DB migration is running.
   *
   * @return a boolean
   */
  boolean running();

  /**
   * Indicates whether a migration has been started since the last boot of SonarQube and if it ended successfully.
   * <p>
   * This flag is {@code false} when SonarQube is started and reset to {@code false} whenever {@link #start()} is
   * called.
   * </p>
   *
   * @return a boolean
   */
  boolean succeeded();
}
