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

package org.projectforge.core;

import java.text.ParseException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.projectforge.database.MyDatabaseUpdateDao;
import org.projectforge.database.MyDatabaseUpdater;
import org.projectforge.meb.MebJobExecutor;
import org.projectforge.meb.MebPollingJob;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

/**
 * Setup of the Quartz scheduler.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class CronSetup
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(CronSetup.class);

  private Scheduler scheduler;

  private MyDatabaseUpdater myDatabaseUpdater;

  private HibernateSearchReindexer hibernateSearchReindexer;

  private MebJobExecutor mebJobExecutor;

  /**
   * Should be called at the start-up time of the application.<br/>
   * Initializes and starts the scheduler.
   */
  public void initialize()
  {
    synchronized (this) {
      if (scheduler != null) {
        return;
      }
      try {
        // Grab the Scheduler instance from the Factory
        scheduler = StdSchedulerFactory.getDefaultScheduler();
        // and start it off
        scheduler.start();

      } catch (final SchedulerException ex) {
        log.error(ex.getMessage(), ex);
      }
      final ConfigXml cfg = ConfigXml.getInstance();
      if (cfg.isMebMailAccountConfigured() == false) {
        mebJobExecutor = null; // MEB is not configured.
      }
      final MyDatabaseUpdateDao databaseUpdateDao = myDatabaseUpdater.getDatabaseUpdateDao();
      // run every hour at *:00: 0 0 * * * ?
      createCron("hourlyJob", CronHourlyJob.class, "0 0 * * * ?", cfg.getCronExpressionHourlyJob(), "databaseUpdateDao", databaseUpdateDao,
          "hibernateSearchReindexer", hibernateSearchReindexer);
      // run every morning at 2:30 AM (UTC): 0 30 2 * * ?
      createCron("nightlyJob", CronNightlyJob.class, "0 30 2 * * ?", cfg.getCronExpressionNightlyJob(), "hibernateSearchReindexer",
          hibernateSearchReindexer, "mebJobExecutor", mebJobExecutor);
      if (mebJobExecutor != null) {
        // run every 10 minutes (5, 15, 25, ...): 0 5/10 * * * ?
        createCron("mebPollingJob", MebPollingJob.class, "0 5/10 * * * ?", cfg.getCronExpressionMebPollingJob(), "mebJobExecutor",
            mebJobExecutor);
      }
    }
  }

  public void registerCronJob(final String givenName, final Class< ? extends Job> jobClass, final String cronExpression,
      final Object... params)
  {
    if (jobClass != null) {
      String name = givenName;
      if (StringUtils.isBlank(name) == true) {
        name = "generatedExternalName " + jobClass.getName() + " " + System.currentTimeMillis();
      }
      // default is run every 10 minutes
      createCron(name, jobClass, "0 */10 * * * ?", cronExpression, params);
    }
  }

  /**
   * Should be called at the shutdown of the application.
   */
  public void shutdown()
  {
    try {
      scheduler.shutdown();
    } catch (final SchedulerException ex) {
      log.error(ex.getMessage(), ex);
    }
  }

  private void createCron(final String name, final Class< ? extends Job> jobClass, final String cronDefaultExpression,
      final String cronExpression, final Object... params)
  {
    // Define job instance (group = "default")
    final JobDetail job = new JobDetail(name, "default", jobClass);
    if (params != null) {
      Validate.isTrue(params.length % 2 == 0);
      final JobDataMap map = job.getJobDataMap();
      for (int i = 0; i < params.length - 1; i += 2) {
        Validate.isTrue(params[i] instanceof String);
        map.put(params[i], params[i + 1]);
      }
    }
    final String cronEx;
    if (StringUtils.isNotBlank(cronExpression) == true) {
      cronEx = cronExpression;
    } else {
      cronEx = cronDefaultExpression;
    }
    final Trigger trigger;
    try {
      trigger = new CronTrigger(name + "Trigger", "default", cronEx);
    } catch (final ParseException ex) {
      log.error("Could not create cron trigger with expression '" + cronEx + "' (cron job is disabled): " + ex.getMessage(), ex);
      return;
    }
    try {
      // Schedule the job with the trigger
      scheduler.scheduleJob(job, trigger);
    } catch (final SchedulerException ex) {
      log.error("Could not create cron job: " + ex.getMessage(), ex);
      return;
    }
    log.info("Cron job '" + name + "' successfully configured: " + cronEx);
  }

  public void setMyDatabaseUpdater(final MyDatabaseUpdater myDatabaseUpdater)
  {
    this.myDatabaseUpdater = myDatabaseUpdater;
  }

  public void setHibernateSearchReindexer(final HibernateSearchReindexer hibernateSearchReindexer)
  {
    this.hibernateSearchReindexer = hibernateSearchReindexer;
  }

  public void setMebJobExecutor(final MebJobExecutor mebJobExecutor)
  {
    this.mebJobExecutor = mebJobExecutor;
  }
}
