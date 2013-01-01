/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2013 Kai Reinhard (k.reinhard@micromata.de)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.database;

import org.apache.commons.lang.StringUtils;

/**
 * All data base dialect specific implementations should be placed here.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class DatabaseSupport
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(DatabaseSupport.class);

  private static boolean errorMessageShown = false;

  private static DatabaseSupport instance;

  private final HibernateDialect dialect;

  public static DatabaseSupport instance()
  {
    if (instance == null) {
      instance = new DatabaseSupport();
    }
    return instance;
  }

  public DatabaseSupport()
  {
    this.dialect = HibernateUtils.getDialect();
  }

  public DatabaseSupport(final HibernateDialect dialect)
  {
    this.dialect = dialect;
  }

  /**
   * Optimization for getting sum of durations. Currently only an optimization for PostgreSQL is implemented:
   * "extract(epoch from sum(toProperty - fromProperty))". <br/>
   * If no optimization is given, the caller selects all data base entries and aggregates via Java the sum (full table scan).
   * @param fromProperty
   * @param toProperty
   * @return part of select string or null if for the used data base no optimization is given.
   */
  public String getIntervalInSeconds(final String fromProperty, final String toProperty)
  {
    if (dialect == HibernateDialect.PostgreSQL) {
      return "EXTRACT(EPOCH FROM SUM(" + toProperty + " - " + fromProperty + "))"; // Seconds since 1970
    } else {
      if (errorMessageShown == false) {
        errorMessageShown = true;
        log.warn("No data base optimization implemented for the used data base. Please contact the developer if you have an installation with mor than 10.000 time sheet entries for increasing performance");
      }
      // No optimization for this data base.
      return null;
    }
  }

  // PK INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL PRIMARY KEY

  /**
   * For Hypersoniq "GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL PRIMARY KEY" is returned if the primary key should be generated
   * by the data-base, otherwise an empty string. <br/>
   * Expected result for Hypersoniq: " <pk col name> INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL PRIMARY KEY"
   */
  public String getPrimaryKeyAttributeSuffix(final TableAttribute primaryKey)
  {
    if (dialect == HibernateDialect.HSQL && primaryKey.isGenerated() == true) {
      return " GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL PRIMARY KEY";
    }
    return "";
  }

  /** For Hypersoniq an empty string is returned if the pk has to be generated by Hypersonic, otherwise ",\n  PRIMARY KEY (<pk col name>)". */
  public String getPrimaryKeyTableSuffix(final TableAttribute primaryKey)
  {
    if (dialect == HibernateDialect.HSQL && primaryKey.isGenerated() == true) {
      return "";
    }
    return ",\n  PRIMARY KEY (" + primaryKey.getName() + ")";
  }

  public String getType(final TableAttribute attr)
  {
    switch (attr.getType()) {
      case CHAR:
        return "CHAR(" + attr.getLength() + ")";
      case VARCHAR:
        return "VARCHAR(" + attr.getLength() + ")";
      case BOOLEAN:
        return "BOOLEAN";
      case INT:
        if (dialect == HibernateDialect.PostgreSQL) {
          return "INT4";
        } else {
          return "INT";
        }
      case TIMESTAMP:
        return "TIMESTAMP";
      case LOCALE:
        return "VARCHAR(255)";
      case DATE:
        return "DATE";
      case DECIMAL:
        return "DECIMAL(" + attr.getPrecision() + ", " + attr.getScale() + ")";
      default:
        throw new UnsupportedOperationException("Type '" + attr.getType() + "' not supported for the current database dialect: " + dialect);
    }
  }

  public void addDefaultAndNotNull(final StringBuffer buf, final TableAttribute attr)
  {
    if (dialect != HibernateDialect.HSQL) {
      if (attr.isNullable() == false) {
        buf.append(" NOT NULL");
      }
      if (StringUtils.isNotBlank(attr.getDefaultValue()) == true) {
        buf.append(" DEFAULT(").append(attr.getDefaultValue()).append(")");
      }
    } else {
      if (StringUtils.isNotBlank(attr.getDefaultValue()) == true) {
        buf.append(" DEFAULT '").append(attr.getDefaultValue()).append("'");
      }
      if (attr.isNullable() == false) {
        buf.append(" NOT NULL");
      }
    }
  }

  public String renameAttribute(final String table, final String oldName, final String newName)
  {
    final StringBuffer buf = new StringBuffer();
    buf.append("ALTER TABLE ").append(table).append(" ");
    if (dialect == HibernateDialect.HSQL) {
      buf.append("ALTER COLUMN ").append(oldName).append(" RENAME TO ").append(newName);
    } else {
      buf.append("RENAME COLUMN ").append(oldName).append(" TO ").append(newName);
    }
    return buf.toString();
  }
}
