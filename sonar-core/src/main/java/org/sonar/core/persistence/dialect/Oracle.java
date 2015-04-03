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
package org.sonar.core.persistence.dialect;

import org.apache.commons.lang.StringUtils;
import org.hibernate.dialect.Oracle10gDialect;
import org.sonar.api.database.DatabaseProperties;

import java.sql.Types;

/**
 * @since 1.12
 */
public class Oracle extends AbstractDialect {

  public static final String ID = "oracle";

  public Oracle() {
    super(ID, "oracle", "oracle.jdbc.OracleDriver", "1", "0", "SELECT 1 FROM DUAL");
  }

  @Override
  public Class<? extends org.hibernate.dialect.Dialect> getHibernateDialectClass() {
    return Oracle10gWithDecimalDialect.class;
  }

  @Override
  public boolean matchesJdbcURL(String jdbcConnectionURL) {
    return StringUtils.startsWithIgnoreCase(jdbcConnectionURL, "jdbc:oracle:");
  }

  @Override
  public boolean supportsMigration() {
    return true;
  }

  public static class Oracle10gWithDecimalDialect extends Oracle10gDialect {
    public Oracle10gWithDecimalDialect() {
      super();
      registerColumnType(Types.DOUBLE, "number($p,$s)");
      registerColumnType(Types.VARCHAR, DatabaseProperties.MAX_TEXT_SIZE, "clob");
      registerColumnType(Types.VARBINARY, "blob");
    }

    @Override
    public Class getNativeIdentifierGeneratorClass() {
      return OracleSequenceGenerator.class;
    }
  }
}
