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

package org.projectforge.export;

import java.util.Locale;

import org.projectforge.excel.ExcelDateFormats;
import org.projectforge.excel.ExportContext;
import org.projectforge.user.PFUserContext;
import org.projectforge.user.PFUserDO;

/**
 * This default context does nothing special, you may implement your own context.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class MyXlsExportContext implements ExportContext
{
  private Locale locale;

  /**
   * @return the default locale of the system or the locale set by {@link #setLocale(Locale)}.
   * @see org.projectforge.excel.ExportContext#getLocale()
   */
  public Locale getLocale()
  {
    if (this.locale != null) {
      locale = PFUserContext.getLocale();
    }
    return locale;
  }

  @Override
  public void setLocale(final Locale locale)
  {
    this.locale = locale;
  }

  /**
   * @return Does not translation: returns the i18nKey itself.
   * @see org.projectforge.excel.ExportContext#getLocalizedString(java.lang.String)
   */
  public String getLocalizedString(final String i18nKey)
  {
    return PFUserContext.getLocalizedString(i18nKey);
  }

  /**
   * Returns the excel format of the context user if found, otherwise: {@link ExcelDateFormats#EXCEL_DEFAULT_DATE}
   * @see org.projectforge.excel.ExportContext#getExcelDateFormat()
   */
  @Override
  public String getExcelDateFormat()
  {
    final PFUserDO user = PFUserContext.getUser();
    return user != null ? user.getExcelDateFormat() : ExcelDateFormats.EXCEL_DEFAULT_DATE;
  }
}
