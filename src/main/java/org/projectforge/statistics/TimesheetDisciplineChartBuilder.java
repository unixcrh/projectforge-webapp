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

package org.projectforge.statistics;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickMarkPosition;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYDifferenceRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.projectforge.calendar.DayHolder;
import org.projectforge.core.OrderDirection;
import org.projectforge.timesheet.TimesheetDO;
import org.projectforge.timesheet.TimesheetDao;
import org.projectforge.timesheet.TimesheetFilter;
import org.projectforge.user.PFUserContext;


/**
 * Erzeugt wahlweise eins von zwei Diagrammen:<br/>
 * <ol>
 * <li>Ein Diagramm, welches über die letzten n Tage die kummulierten IST-Arbeitsstunden und als Soll-Wert die tatsächlich gebuchten
 * Zeitberichte aufträgt. Dies wird in einem Differenz-XY-Diagramm visualisiert. Die Darstellung soll motivieren, dass Projektmitarbeiter
 * ihre Zeitberichte möglichst zeitnah eintragen.</li>
 * <li>Ein Diagramm, welches über die letzten n Tage die Tage visualisiert, die zwischen Zeitberichtsdatum und Zeitpunkt der tatsächlichen
 * Buchung liegen.</li>
 * </ol>
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class TimesheetDisciplineChartBuilder
{
  private static final double PLANNED_AVERAGE_DIFFERENCE_BETWEEN_TIMESHEET_AND_BOOKING = 2.0; // days.

  private double planWorkingHours;

  private double actualWorkingHours;

  private BigDecimal averageDifferenceBetweenTimesheetAndBooking;

  /**
   * Calculated total plan working hours calculated after call of create #1.
   */
  public double getPlanWorkingHours()
  {
    return planWorkingHours;
  }

  /**
   * Calculated total actual working hours calculated after call of create #1.
   */
  public double getActualWorkingHours()
  {
    return actualWorkingHours;
  }

  /**
   * Gets the calculated average number of days between the date of a time sheet and the date of creation (after call create #2).
   */
  public BigDecimal getAverageDifferenceBetweenTimesheetAndBooking()
  {
    return averageDifferenceBetweenTimesheetAndBooking;
  }

  public double getPlannedAverageDifferenceBetweenTimesheetAndBooking()
  {
    return PLANNED_AVERAGE_DIFFERENCE_BETWEEN_TIMESHEET_AND_BOOKING;
  }

  /**
   * Ein Diagramm, welches über die letzten n Tage die kummulierten IST-Arbeitsstunden und als Soll-Wert die tatsächlich gebuchten
   * Zeitberichte aufträgt. Dies wird in einem Differenz-XY-Diagramm visualisiert. Die Darstellung soll motivieren, dass Projektmitarbeiter
   * ihre Zeitberichte möglichst zeitnah eintragen.
   * @param timesheetDao
   * @param userId
   * @param workingHoursPerDay
   * @param forLastNDays
   * @param shape e. g. new Ellipse2D.Float(-3, -3, 6, 6) or null, if no marker should be printed.
   * @param stroke e. g. new BasicStroke(3.0f).
   * @param showAxisValues
   * @return
   */
  public JFreeChart create(final TimesheetDao timesheetDao, final Integer userId, final double workingHoursPerDay,
      final short forLastNDays, final Shape shape, final Stroke stroke, final boolean showAxisValues)
  {
    final DayHolder dh = new DayHolder();
    final TimesheetFilter filter = new TimesheetFilter();
    filter.setStopTime(dh.getDate());
    dh.add(Calendar.DATE, -forLastNDays);
    filter.setStartTime(dh.getDate());
    filter.setUserId(userId);
    filter.setOrderType(OrderDirection.ASC);
    final List<TimesheetDO> list = timesheetDao.getList(filter);
    final TimeSeries sollSeries = new TimeSeries("Soll");
    final TimeSeries istSeries = new TimeSeries("Ist");
    planWorkingHours = 0;
    actualWorkingHours = 0;
    final Iterator<TimesheetDO> it = list.iterator();
    TimesheetDO current = null;
    if (it.hasNext() == true) {
      current = it.next();
    }
    for (int i = 0; i <= forLastNDays; i++) {
      while (current != null && (dh.isSameDay(current.getStartTime()) == true || current.getStartTime().before(dh.getDate()) == true)) {
        actualWorkingHours += ((double) current.getWorkFractionDuration()) / 3600000;
        if (it.hasNext() == true) {
          current = it.next();
        } else {
          current = null;
          break;
        }
      }
      if (dh.isWorkingDay() == true) {
        final BigDecimal workFraction = dh.getWorkFraction();
        if (workFraction != null) {
          planWorkingHours += workFraction.doubleValue() * workingHoursPerDay;
        } else {
          planWorkingHours += workingHoursPerDay;
        }
      }
      final Day day = new Day(dh.getDayOfMonth(), dh.getMonth() + 1, dh.getYear());
      sollSeries.add(day, planWorkingHours);
      istSeries.add(day, actualWorkingHours);
      dh.add(Calendar.DATE, 1);
    }
    return create(sollSeries, istSeries, shape, stroke, showAxisValues, "hours");
  }

  /**
   * Ein Diagramm, welches über die letzten n Tage die Tage visualisiert, die zwischen Zeitberichtsdatum und Zeitpunkt der tatsächlichen
   * Buchung liegen.
   * @param timesheetDao
   * @param userId
   * @param forLastNDays
   * @param shape e. g. new Ellipse2D.Float(-3, -3, 6, 6) or null, if no marker should be printed.
   * @param stroke e. g. new BasicStroke(3.0f).
   * @param showAxisValues
   * @return
   */
  public JFreeChart create(final TimesheetDao timesheetDao, final Integer userId, final short forLastNDays, final Shape shape,
      final Stroke stroke, final boolean showAxisValues)
  {
    final DayHolder dh = new DayHolder();
    final TimesheetFilter filter = new TimesheetFilter();
    filter.setStopTime(dh.getDate());
    dh.add(Calendar.DATE, -forLastNDays);
    filter.setStartTime(dh.getDate());
    filter.setUserId(userId);
    filter.setOrderType(OrderDirection.ASC);
    final List<TimesheetDO> list = timesheetDao.getList(filter);
    final TimeSeries planSeries = new TimeSeries("Soll");
    final TimeSeries actualSeries = new TimeSeries("Ist");
    final Iterator<TimesheetDO> it = list.iterator();
    TimesheetDO current = null;
    if (it.hasNext() == true) {
      current = it.next();
    }
    long numberOfBookedDays = 0;
    long totalDifference = 0;
    for (int i = 0; i <= forLastNDays; i++) {
      long difference = 0;
      long totalDuration = 0; // Weight for average.
      while (current != null && (dh.isSameDay(current.getStartTime()) == true || current.getStartTime().before(dh.getDate()) == true)) {
        long duration = current.getWorkFractionDuration();
        difference += (current.getCreated().getTime() - current.getStartTime().getTime()) * duration;
        totalDuration += duration;
        if (it.hasNext() == true) {
          current = it.next();
        } else {
          current = null;
          break;
        }
      }
      double averageDifference = difference > 0 ? ((double) difference) / totalDuration / 86400000 : 0; // In days.
      final Day day = new Day(dh.getDayOfMonth(), dh.getMonth() + 1, dh.getYear());
      if (averageDifference > 0) {
        planSeries.add(day, PLANNED_AVERAGE_DIFFERENCE_BETWEEN_TIMESHEET_AND_BOOKING); // plan average
        // (PLANNED_AVERAGE_DIFFERENCE_BETWEEN_TIMESHEET_AND_BOOKING
        // days).
        actualSeries.add(day, averageDifference);
        totalDifference += averageDifference;
        numberOfBookedDays++;
      }
      dh.add(Calendar.DATE, 1);
    }
    averageDifferenceBetweenTimesheetAndBooking = numberOfBookedDays > 0 ? new BigDecimal(totalDifference).divide(new BigDecimal(
        numberOfBookedDays), 1, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    return create(actualSeries, planSeries, shape, stroke, showAxisValues, "days");
  }

  private JFreeChart create(final TimeSeries series1, final TimeSeries series2, final Shape shape, final Stroke stroke,
      final boolean showAxisValues, final String valueAxisUnitKey)
  {
    final TimeSeriesCollection dataset = new TimeSeriesCollection();
    dataset.addSeries(series1);
    dataset.addSeries(series2);
    final JFreeChart chart = ChartFactory.createXYLineChart(null, null, null, dataset, PlotOrientation.VERTICAL, true, true, false);

    final XYDifferenceRenderer renderer = new XYDifferenceRenderer(new Color(238, 176, 176), new Color(135, 206, 112), true);
    renderer.setSeriesPaint(0, new Color(222, 23, 33));
    renderer.setSeriesPaint(1, new Color(64, 169, 59));
    if (shape != null) {
      renderer.setSeriesShape(0, shape);
      renderer.setSeriesShape(1, shape);
    } else {
      final Shape none = new Rectangle();
      renderer.setSeriesShape(0, none);
      renderer.setSeriesShape(1, none);
    }
    renderer.setSeriesStroke(0, stroke);
    renderer.setSeriesStroke(1, stroke);
    renderer.setSeriesVisibleInLegend(0, false);
    renderer.setSeriesVisibleInLegend(1, false);
    final XYPlot plot = chart.getXYPlot();
    plot.setRenderer(renderer);
    plot.setBackgroundPaint(Color.white);
    plot.setDomainGridlinePaint(Color.lightGray);
    plot.setRangeGridlinePaint(Color.lightGray);
    final DateAxis xAxis = new DateAxis();
    xAxis.setTickMarkPosition(DateTickMarkPosition.MIDDLE);
    xAxis.setLowerMargin(0.0);
    xAxis.setUpperMargin(0.0);
    xAxis.setVisible(showAxisValues);
    plot.setDomainAxis(xAxis);
    final NumberAxis yAxis;
    if (showAxisValues == true) {
      yAxis = new NumberAxis(PFUserContext.getLocalizedString(valueAxisUnitKey));
    } else {
      yAxis = new NumberAxis();
    }
    yAxis.setVisible(showAxisValues);
    plot.setRangeAxis(yAxis);
    plot.setOutlineVisible(false);
    return chart;
  }
}
