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

import org.apache.commons.lang.BooleanUtils;
import org.projectforge.calendar.DayHolder;
import org.projectforge.common.DateFormatType;
import org.projectforge.common.DateFormats;
import org.projectforge.common.DateHolder;
import org.projectforge.common.DatePrecision;
import org.projectforge.excel.CellFormat;
import org.projectforge.excel.ContentProvider;
import org.projectforge.excel.ExportWorkbook;
import org.projectforge.excel.XlsContentProvider;

public class MyXlsContentProvider extends XlsContentProvider
{
  public static final int LENGTH_KOSTENTRAEGER = 11;

  public static final int LENGTH_USER = 20;

  public static final int LENGTH_ZIPCODE = 7;
  
  /**
   * @see org.projectforge.excel.XlsContentProvider#newInstance()
   */
  @Override
  public ContentProvider newInstance()
  {
    return new MyXlsContentProvider(this.workbook);
  }

  public MyXlsContentProvider(final ExportWorkbook workbook)
  {
    super(new MyXlsExportContext(), workbook);
    defaultFormatMap.put(DateHolder.class, new CellFormat("YYYY-MM-DD").setAutoDatePrecision(true)); // format unused.
    defaultFormatMap.put(DayHolder.class, new CellFormat(DateFormats.getExcelFormatString(DateFormatType.DATE)));
  }

  /**
   * @see org.projectforge.excel.XlsContentProvider#getCustomizedValue(java.lang.Object)
   */
  @Override
  public Object getCustomizedValue(final Object value)
  {
    if (value instanceof DateHolder) {
      return ((DateHolder) value).getCalendar();
    }
    return null;
  }

  /**
   * @see org.projectforge.excel.XlsContentProvider#getCellFormat(org.projectforge.excel.ExportCell, java.lang.Object, java.lang.String,
   *      java.util.Map)
   */
  @Override
  protected CellFormat getCustomizedCellFormat(final CellFormat format, final Object value)
  {
    if (value == null || DateHolder.class.isAssignableFrom(value.getClass()) == false) {
      return null;
    }
    if (format != null && BooleanUtils.isTrue(format.getAutoDatePrecision()) == false) {
      return null;
    }
    // Find a format dependent on the precision:
    final DatePrecision precision = ((DateHolder) value).getPrecision();
    if (precision == DatePrecision.DAY) {
      return new CellFormat(DateFormats.getExcelFormatString(DateFormatType.DATE));
    } else if (precision == DatePrecision.SECOND) {
      return new CellFormat(DateFormats.getExcelFormatString(DateFormatType.TIMESTAMP_SECONDS));
    } else if (precision == DatePrecision.MILLISECOND) {
      return new CellFormat(DateFormats.getExcelFormatString(DateFormatType.TIMESTAMP_MILLIS));
    } else {
      // HOUR_OF_DAY, MINUTE, MINUTE_15 or null
      return new CellFormat(DateFormats.getExcelFormatString(DateFormatType.TIMESTAMP_MINUTES));
    }
  }
}
