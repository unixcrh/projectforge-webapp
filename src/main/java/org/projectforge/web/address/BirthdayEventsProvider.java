/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2012 Kai Reinhard (k.reinhard@micromata.com)
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

package org.projectforge.web.address;

import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import net.ftlines.wicket.fullcalendar.Event;

import org.apache.wicket.Component;
import org.joda.time.DateTime;
import org.projectforge.address.AddressDO;
import org.projectforge.address.AddressDao;
import org.projectforge.address.BirthdayAddress;
import org.projectforge.common.DateFormatType;
import org.projectforge.common.DateFormats;
import org.projectforge.common.DateHolder;
import org.projectforge.web.calendar.DateTimeFormatter;
import org.projectforge.web.calendar.MyFullCalendarEventsProvider;

/**
 * Creates events for FullCalendar.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class BirthdayEventsProvider extends MyFullCalendarEventsProvider
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(BirthdayEventsProvider.class);

  private static final long serialVersionUID = 2241430630558260146L;

  private final AddressDao addressDao;

  private final boolean dataProtection;

  /**
   * @param parent For i18n.
   * @param addressDao
   * @param dataProtection If true (default) then no ages will be shown, only the names.
   * @see Component#getString(String)
   */
  public BirthdayEventsProvider(final Component parent, final AddressDao addressDao, final boolean dataProtection)
  {
    super(parent);
    this.addressDao = addressDao;
    this.dataProtection = dataProtection;
  }

  /**
   * @see org.projectforge.web.calendar.MyFullCalendarEventsProvider#buildEvents(org.joda.time.DateTime, org.joda.time.DateTime)
   */
  @Override
  protected void buildEvents(final DateTime start, final DateTime end)
  {
    Date from = start.toDate();
    if (start.getMonthOfYear() == Calendar.MARCH && start.getDayOfMonth() == 1) {
      final DateHolder dh = new DateHolder(start.toDate());
      dh.add(Calendar.DAY_OF_MONTH, -1); // Take birthday from February 29th into March, 1st.
      from = dh.getDate();
    }
    final Set<BirthdayAddress> set = addressDao.getBirthdays(from, end.toDate(), 1000, true);
    for (final BirthdayAddress birthdayAddress : set) {
      final AddressDO address = birthdayAddress.getAddress();
      final int month = birthdayAddress.getMonth() + 1;
      final int dayOfMonth = birthdayAddress.getDayOfMonth();
      DateTime date = getDate(start, end, month, dayOfMonth);
      // February, 29th fix:
      if (date == null && month == Calendar.FEBRUARY + 1 && dayOfMonth == 29) {
        date = getDate(start, end, month + 1, 1);
      }
      if (date == null) {
        log.warn("Date "
            + birthdayAddress.getDayOfMonth()
            + "/"
            + (birthdayAddress.getMonth() + 1)
            + " not found between "
            + start
            + " and "
            + end);
        continue;
      } else {
        if (dataProtection == false) {
          birthdayAddress.setAge(date.toDate());
        }
      }
      final Event event = new Event().setAllDay(true);
      final String id = "b-" + address.getId();
      event.setId(id);
      event.setStart(date);
      final StringBuffer buf = new StringBuffer();
      if (dataProtection == false) {
        // Birthday is not visible for all users (age == 0).
        buf.append(
            DateTimeFormatter.instance().getFormattedDate(address.getBirthday(), DateFormats.getFormatString(DateFormatType.DATE_SHORT)))
            .append(" ");
      }
      buf.append(address.getFirstName()).append(" ").append(address.getName());
      if (dataProtection == false && birthdayAddress.getAge() > 0) {
        // Birthday is not visible for all users (age == 0).
        buf.append(" (").append(birthdayAddress.getAge()).append(" ").append(getString("address.age.short")).append(")");
      }
      event.setTitle(buf.toString());
      if (birthdayAddress.isFavorite() == true) {
        event.setTextColor("#227722");
        event.setBackgroundColor("#BBEEBB");
      }
      events.put(id, event);
    }
  }

  private DateTime getDate(final DateTime start, final DateTime end, final int month, final int dayOfMonth)
  {
    DateTime day = start;
    int paranoiaCounter = 0;
    do {
      if (day.getMonthOfYear() == month && day.getDayOfMonth() == dayOfMonth) {
        return day;
      }
      day = day.plusDays(1);
      if (++paranoiaCounter > 1000) {
        log.error("Paranoia counter exceeded! Dear developer, please have a look at the implementation of getDate.");
        break;
      }
    } while (day.isAfter(end) == false);
    return null;
  }
}