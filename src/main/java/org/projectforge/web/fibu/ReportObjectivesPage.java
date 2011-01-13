/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2011 Kai Reinhard (k.reinhard@me.com)
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

package org.projectforge.web.fibu;

import java.io.InputStream;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.common.DateHolder;
import org.projectforge.fibu.kost.reporting.Report;
import org.projectforge.fibu.kost.reporting.ReportDao;
import org.projectforge.fibu.kost.reporting.ReportStorage;
import org.projectforge.user.ProjectForgeGroup;
import org.projectforge.web.wicket.AbstractSecuredPage;

public class ReportObjectivesPage extends AbstractSecuredPage
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ReportObjectivesPage.class);

  private static final String KEY_REPORT_STORAGE = "ReportObjectivesPage:storage";

  @SpringBean(name = "reportDao")
  private ReportDao reportDao;

  private ReportObjectivesForm form;

  protected transient ReportStorage reportStorage;

  public ReportObjectivesPage(final PageParameters parameters)
  {
    super(parameters);
    body.add(new FeedbackPanel("feedback").setOutputMarkupId(true));
    form = new ReportObjectivesForm(this);
    body.add(form);
    form.init();
  }

  protected void importReportObjectivs()
  {
    checkAccess();
    log.info("import report objectives.");
    final FileUpload fileUpload = form.fileUploadField.getFileUpload();
    if (fileUpload != null) {
      boolean delete = false;
      try {
        final String clientFileName = fileUpload.getClientFileName();
        final InputStream is = fileUpload.getInputStream();
        final Report report = reportDao.createReport(is);
        reportStorage = new ReportStorage(report);
        reportStorage.setFileName(clientFileName);
        putUserPrefEntry(KEY_REPORT_STORAGE, reportStorage, false);
      } catch (Exception ex) {
        log.error(ex.getMessage(), ex);
        error("An error occurred (see log files for details): " + ex.getMessage());
      } finally {
        if (delete == true) {
          fileUpload.delete();
        }
      }
    }
  }

  protected void createReport()
  {
    checkAccess();
    log.info("load report.");
    final Report report = getReportStorage().getRoot();
    final ReportObjectivesFilter filter = form.getFilter();
    final DateHolder day = new DateHolder(filter.getFromDate());
    report.setFrom(day.getYear(), day.getMonth());
    day.setDate(filter.getToDate());
    report.setTo(day.getYear(), day.getMonth());
    reportDao.loadReport(report);
  }

  protected void clear()
  {
    checkAccess();
    log.info("clear report.");
    removeUserPrefEntry(KEY_REPORT_STORAGE);
    this.reportStorage = null;
  }

  /**
   * @return Any existing user storage or null if not exist (wether in class nor in user's session).
   */
  protected ReportStorage getReportStorage()
  {
    if (reportStorage != null) {
      return reportStorage;
    }
    return (ReportStorage) getUserPrefEntry(KEY_REPORT_STORAGE);
  }

  private void checkAccess()
  {
    accessChecker.checkIsUserMemberOfGroup(ProjectForgeGroup.FINANCE_GROUP, ProjectForgeGroup.CONTROLLING_GROUP);
    accessChecker.checkDemoUser();
  }

  @Override
  protected String getTitle()
  {
    return getString("fibu.kost.reporting");
  }
}
