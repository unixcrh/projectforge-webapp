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

package org.projectforge.plugins.teamcal.dialog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.markup.html.form.select.IOptionRenderer;
import org.apache.wicket.extensions.markup.html.form.select.Select;
import org.apache.wicket.extensions.markup.html.form.select.SelectOptions;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.plugins.teamcal.admin.TeamCalDO;
import org.projectforge.plugins.teamcal.admin.TeamCalDao;
import org.projectforge.plugins.teamcal.event.TeamEventDao;
import org.projectforge.plugins.teamcal.event.TeamEventRight;
import org.projectforge.plugins.teamcal.integration.TeamCalCalendarFilter;
import org.projectforge.plugins.teamcal.integration.TemplateCalendarProperties;
import org.projectforge.plugins.teamcal.integration.TemplateEntry;
import org.projectforge.user.PFUserContext;
import org.projectforge.user.PFUserDO;
import org.projectforge.user.UserRights;
import org.projectforge.web.common.ColorPickerPanel;
import org.projectforge.web.dialog.ModalDialog;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.flowlayout.AjaxIconButtonPanel;
import org.projectforge.web.wicket.flowlayout.ButtonGroupPanel;
import org.projectforge.web.wicket.flowlayout.CheckBoxPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DropDownChoicePanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.Heading3Panel;
import org.projectforge.web.wicket.flowlayout.IconButtonPanel;
import org.projectforge.web.wicket.flowlayout.IconType;
import org.projectforge.web.wicket.flowlayout.SelectPanel;

import com.vaynberg.wicket.select2.Select2MultiChoice;

/**
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 * 
 */
public class TeamCalDialog extends ModalDialog
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TeamCalDialog.class);

  private static final long serialVersionUID = 8687197318833240410L;

  private final List<TeamCalDO> selectedCalendars;

  private DropDownChoicePanel<TemplateEntry> templateChoice;

  // Adaption (fake) to display "Time Sheets" as selection option
  private final TeamCalDO timesheetsCalendar = new TeamCalDO();

  private final TeamCalCalendarFilter filter;

  private TeamCalCalendarFilter backupFilter;

  private CalendarColorPanel calendarColorPanel;

  private Select<Integer> defaultCalendarSelect;

  private Select2MultiChoice<TeamCalDO> teamCalsChoice;

  public static final Integer TIMESHEET_CALENDAR_ID = -1;

  @SpringBean
  private TeamCalDao teamCalDao;

  private final TeamEventRight teamEventRight;

  /**
   * @param id
   * @param titleModel
   * @param filter
   */
  public TeamCalDialog(final String id, final TeamCalCalendarFilter filter)
  {
    super(id);
    this.filter = filter;
    setTitle(new ResourceModel("plugins.teamcal.calendar.filterDialog.title"));
    setBigWindow().setShowCancelButton().wantsNotificationOnClose();
    selectedCalendars = new LinkedList<TeamCalDO>();
    teamEventRight = (TeamEventRight) UserRights.instance().getRight(TeamEventDao.USER_RIGHT_ID);
  }

  /**
   * @see org.apache.wicket.Component#renderHead(org.apache.wicket.markup.html.IHeaderResponse)
   */
  @Override
  public void renderHead(final IHeaderResponse response)
  {
    super.renderHead(response);
    response.render(CssHeaderItem.forUrl("scripts/spectrum/spectrum.css"));
    response.render(JavaScriptReferenceHeaderItem.forUrl("scripts/spectrum/spectrum.js"));
  }

  /**
   * @see org.projectforge.web.dialog.ModalDialog#handleCloseEvent(org.apache.wicket.ajax.AjaxRequestTarget)
   */
  @Override
  protected void handleCloseEvent(final AjaxRequestTarget target)
  {
    myClose(target);
  }

  /**
   * @see org.projectforge.web.dialog.ModalDialog#onCancelButtonSubmit(org.apache.wicket.ajax.AjaxRequestTarget)
   */
  @Override
  protected void onCancelButtonSubmit(final AjaxRequestTarget target)
  {
    // Restore values (valid at opening time of this dialog):
    filter.copyValuesFrom(backupFilter);
    close(target);
  }

  /**
   * @see org.projectforge.web.dialog.ModalDialog#onCloseButtonSubmit(org.apache.wicket.ajax.AjaxRequestTarget)
   */
  @Override
  protected void onCloseButtonSubmit(final AjaxRequestTarget target)
  {
    myClose(target);
  }

  /**
   * @see org.projectforge.web.dialog.ModalDialog#init()
   */
  @Override
  public void init()
  {
    init(new Form<String>(getFormId()));
    timesheetsCalendar.setTitle(getString("plugins.teamcal.timeSheetCalendar"));
    timesheetsCalendar.setId(TIMESHEET_CALENDAR_ID);
    // confirm
    setCloseButtonTooltip(null, new ResourceModel("plugins.teamcal.calendar.filterDialog.closeButton.tooltip"));
  }

  public TeamCalDialog redraw()
  {
    clearContent();
    gridBuilder.newSplitPanel(GridSize.COL50);
    addFilterFieldset();
    gridBuilder.newSplitPanel(GridSize.COL50);
    addDefaultCalenderSelection();
    gridBuilder.newGridPanel();
    addTeamCalsChoiceFieldset();
    final DivPanel panel = gridBuilder.getPanel();
    panel.add(new Heading3Panel(panel.newChildId(), getString("plugins.teamcal.selectColor")));
    panel.add(calendarColorPanel = new CalendarColorPanel(panel.newChildId()));
    calendarColorPanel.redraw();
    return this;
  }

  /**
   * @see org.projectforge.web.dialog.ModalDialog#open(org.apache.wicket.ajax.AjaxRequestTarget)
   */
  @Override
  public TeamCalDialog open(final AjaxRequestTarget target)
  {
    backupFilter = new TeamCalCalendarFilter().copyValuesFrom(filter);
    super.open(target);
    return this;
  }

  private void myClose(final AjaxRequestTarget target)
  {
    if (filter.isModified(backupFilter) == false) {
      // Do nothing.
      return;
    }
    final TemplateEntry activeTemplateEntry = filter.getActiveTemplateEntry();
    if (activeTemplateEntry != null) {
      activeTemplateEntry.setDirty();
    }
    filter.setSelectedCalendar(TemplateEntry.calcCalendarStringForCalendar(activeTemplateEntry.getDefaultCalendarId()));
    setResponsePage(getPage().getClass(), getPage().getPageParameters());
  }

  @SuppressWarnings("serial")
  private void addFilterFieldset()
  {
    final IChoiceRenderer<TemplateEntry> teamCalCollectionRenderer = new IChoiceRenderer<TemplateEntry>() {
      private static final long serialVersionUID = 613794318110089990L;

      @Override
      public String getIdValue(final TemplateEntry object, final int index)
      {
        return object.getName();
      }

      @Override
      public Object getDisplayValue(final TemplateEntry object)
      {
        return object.getName();
      }
    };

    final FieldsetPanel fs = gridBuilder.newFieldset(getString("filter"));
    // TEMPLATEENTRY DROPDOWN
    final IModel<List<TemplateEntry>> choicesModel = new PropertyModel<List<TemplateEntry>>(filter, "templateEntries");
    final IModel<TemplateEntry> currentModel = new PropertyModel<TemplateEntry>(filter, "activeTemplateEntry");
    templateChoice = new DropDownChoicePanel<TemplateEntry>(fs.newChildId(), currentModel, choicesModel, teamCalCollectionRenderer, false);
    fs.add(templateChoice);
    templateChoice.getDropDownChoice().setOutputMarkupId(true);

    templateChoice.getDropDownChoice().add(new AjaxFormComponentUpdatingBehavior("onChange") {
      private static final long serialVersionUID = 8999698636114154230L;

      /**
       * @see org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior#onUpdate(org.apache.wicket.ajax.AjaxRequestTarget)
       */
      @Override
      protected void onUpdate(final AjaxRequestTarget target)
      {
        selectedCalendars.clear();
        final TemplateEntry activeTemplateEntry = filter.getActiveTemplateEntry();
        selectedCalendars.addAll(activeTemplateEntry.getCalendars());
        calendarColorPanel.redraw();
        target.add(templateChoice.getDropDownChoice(), calendarColorPanel.main, defaultCalendarSelect);
      }
    });

    final ButtonGroupPanel buttonGroup = new ButtonGroupPanel(fs.newChildId());
    fs.add(buttonGroup);
    // ADD BUTTON FOR Template
    final IconButtonPanel addTemplateButton = new AjaxIconButtonPanel(buttonGroup.newChildId(), IconType.PLUS, getString("add")) {
      /**
       * @see org.projectforge.web.wicket.flowlayout.AjaxIconButtonPanel#onSubmit(org.apache.wicket.ajax.AjaxRequestTarget)
       */
      @Override
      protected void onSubmit(final AjaxRequestTarget target)
      {
        // currentName = "";
        // // this callback is evaluated when the name dialog was entered!
        // currentAjaxCallback = new AjaxCallback() {
        // private static final long serialVersionUID = -6959790939627419710L;
        //
        // @Override
        // public void callback(final AjaxRequestTarget target)
        // {
        // final TemplateEntry newTemplate = new TemplateEntry();
        // newTemplate.setName(currentName);
        // filter.add(newTemplate);
        // selectedCalendars.clear();
        // selectedCalendars.addAll(newTemplate.getCalendars());
        // }
        // };
        // nameDialog.open(target);
        // // Redraw the content:
        // nameDialog.redraw().addContent(target);
      }
    };
    addTemplateButton.setDefaultFormProcessing(false);
    buttonGroup.addButton(addTemplateButton);
    //
    // DELETE BUTTON
    final IconButtonPanel deleteTemplateButton = new AjaxIconButtonPanel(buttonGroup.newChildId(), IconType.TRASH, getString("delete")) {
      /**
       * @see org.projectforge.web.wicket.flowlayout.AjaxIconButtonPanel#onSubmit(org.apache.wicket.ajax.AjaxRequestTarget)
       */
      @Override
      protected void onSubmit(final AjaxRequestTarget target)
      {
        filter.remove(filter.getActiveTemplateEntry());
        selectedCalendars.clear();
        selectedCalendars.addAll(filter.getActiveTemplateEntry().getCalendars());
        calendarColorPanel.redraw();
        target.add(templateChoice.getDropDownChoice(), calendarColorPanel.main, defaultCalendarSelect);
      }
    };
    deleteTemplateButton.setDefaultFormProcessing(false);
    buttonGroup.addButton(deleteTemplateButton);

    final TemplateEntry activeTemplateEntry = filter.getActiveTemplateEntry();
    selectedCalendars.clear();
    if (activeTemplateEntry != null) {
      selectedCalendars.addAll(activeTemplateEntry.getCalendars());
    }

  }

  private void addTeamCalsChoiceFieldset()
  {
    final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.teamcal.selectCalendar")).setLabelSide(false);
    // TEAMCAL CHOICE FIELD
    final TeamCalChoiceProvider teamProvider = new TeamCalChoiceProvider();
    teamCalsChoice = new Select2MultiChoice<TeamCalDO>(fs.getSelect2MultiChoiceId(), new PropertyModel<Collection<TeamCalDO>>(
        TeamCalDialog.this, "selectedCalendars"), teamProvider);
    teamCalsChoice.setOutputMarkupId(true);
    teamCalsChoice.add(new AjaxFormComponentUpdatingBehavior("onChange") {
      private static final long serialVersionUID = 1L;

      @Override
      protected void onUpdate(final AjaxRequestTarget target)
      {
        final TemplateEntry activeTemplateEntry = filter.getActiveTemplateEntry();
        final Set<Integer> oldCalIds = activeTemplateEntry.getCalendarIds();
        final List<Integer> newIds = new LinkedList<Integer>();
        // add new keys
        for (final TeamCalDO calendar : selectedCalendars) {
          if (oldCalIds.contains(calendar.getId()) == false) {
            activeTemplateEntry.addNewCalendarProperties(filter, calendar.getId());
          }
          newIds.add(calendar.getId());
        }
        // delete removed keys
        for (final Integer key : oldCalIds) {
          if (newIds.contains(key) == false) {
            activeTemplateEntry.removeCalendarProperties(key);
          }
        }
        calendarColorPanel.redraw();
        target.add(templateChoice.getDropDownChoice(), calendarColorPanel.main, defaultCalendarSelect);
      }
    });
    fs.add(teamCalsChoice);
  }

  @SuppressWarnings("serial")
  private void addDefaultCalenderSelection()
  {
    final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.teamcal.defaultCalendar"));
    fs.addHelpIcon(getString("plugins.teamcal.defaultCalendar.tooltip"));
    final IOptionRenderer<Integer> renderer = new IOptionRenderer<Integer>() {

      @Override
      public String getDisplayValue(final Integer object)
      {
        if (TIMESHEET_CALENDAR_ID.equals(object)) {
          return timesheetsCalendar.getTitle();
        }
        return teamCalDao.getById(object).getTitle();
      }

      @Override
      public IModel<Integer> getModel(final Integer value)
      {
        return Model.of(value);
      }
    };

    // TEAMCAL SELECT
    defaultCalendarSelect = new Select<Integer>(fs.getSelectId(), new PropertyModel<Integer>(filter,
        "activeTemplateEntry.defaultCalendarId")) {

      /**
       * @see org.apache.wicket.Component#onBeforeRender()
       */
      @Override
      protected void onBeforeRender()
      {
        super.onBeforeRender();
        final TemplateEntry activeTemplateEntry = filter.getActiveTemplateEntry();
        final List<TeamCalDO> result = new ArrayList<TeamCalDO>();
        if (activeTemplateEntry != null) {
          for (final TeamCalDO cal : activeTemplateEntry.getCalendars()) {
            if (teamEventRight.hasUpdateAccess(PFUserContext.getUser(), cal) == true) {
              result.add(cal);
            }
          }
        }
        final PFUserDO user = PFUserContext.getUser();
        final List<Integer> filteredList = new ArrayList<Integer>();
        filteredList.add(0, TIMESHEET_CALENDAR_ID);
        if (result != null) {
          // remove teamCals where user has less than full access or is not owner.
          final Iterator<TeamCalDO> it = result.iterator();
          while (it.hasNext()) {
            final TeamCalDO teamCal = it.next();
            if (teamCalDao.hasUpdateAccess(user, teamCal, null, false) == true) {
              filteredList.add(teamCal.getId());
            }
          }
        }
        final SelectOptions<Integer> options = new SelectOptions<Integer>(SelectPanel.OPTIONS_WICKET_ID, filteredList, renderer);
        this.addOrReplace(options);
      }
    };
    defaultCalendarSelect.setOutputMarkupId(true);
    defaultCalendarSelect.add(new OnChangeAjaxBehavior() {

      @Override
      protected void onUpdate(final AjaxRequestTarget target)
      {
        final Integer value = defaultCalendarSelect.getModelObject();
        filter.getActiveTemplateEntry().setDefaultCalendarId(value);
      }
    });
    fs.add(defaultCalendarSelect);
  }

  /**
   * Inner class to represent a single calendar color and visibility panel.
   * 
   */
  private class CalendarColorPanel extends Panel
  {
    private static final long serialVersionUID = -4596590985776103813L;

    private RepeatingView columnRepeater;

    private final WebMarkupContainer main;

    /**
     * @param id
     */
    public CalendarColorPanel(final String id)
    {
      super(id);
      main = new WebMarkupContainer("main");
      main.setOutputMarkupId(true);
      add(main);
    }

    /**
     * @see org.apache.wicket.Component#onInitialize()
     */
    @Override
    protected void onInitialize()
    {
      super.onInitialize();
      columnRepeater = new RepeatingView("columnRepeater");
      main.add(columnRepeater);
    }

    /**
     * @see org.apache.wicket.Component#onBeforeRender()
     */
    @SuppressWarnings("serial")
    protected void redraw()
    {
      columnRepeater.removeAll();
      final int counter = selectedCalendars.size();
      int rowsPerColumn = counter / 3;
      if (counter % 3 > 0) {
        ++rowsPerColumn; // Need one more row.
      }
      final TemplateEntry activeTemplateEntry = filter.getActiveTemplateEntry();
      int rowCounter = 0;
      WebMarkupContainer columnContainer;
      RepeatingView rowRepeater = null;
      for (final TeamCalDO calendar : selectedCalendars) {
        if (rowCounter++ % rowsPerColumn == 0) {
          // Start new column:
          columnContainer = new WebMarkupContainer(columnRepeater.newChildId());
          columnContainer.add(AttributeModifier.append("class", GridSize.COL33.getClassAttrValue()));
          columnRepeater.add(columnContainer);
          rowRepeater = new RepeatingView("rowRepeater");
          columnContainer.add(rowRepeater);
        }
        final WebMarkupContainer container = new WebMarkupContainer(rowRepeater.newChildId());
        rowRepeater.add(container);
        final IModel<Boolean> model = Model.of(activeTemplateEntry.isVisible(calendar.getId()) == true);
        final CheckBoxPanel checkBoxPanel = new CheckBoxPanel("isVisible", model, "");
        checkBoxPanel.getCheckBox().add(new AjaxFormComponentUpdatingBehavior("onChange") {
          private static final long serialVersionUID = 3523446385818267608L;

          @Override
          protected void onUpdate(final AjaxRequestTarget target)
          {
            final Boolean newSelection = checkBoxPanel.getCheckBox().getConvertedInput();
            final TemplateCalendarProperties properties = activeTemplateEntry.getCalendarProperties(calendar.getId());
            if (newSelection != properties.isVisible()) {
              properties.setVisible(newSelection);
              activeTemplateEntry.setDirty();
            }
          }
        });
        container.add(checkBoxPanel);
        container.add(new Label("name", calendar.getTitle()));
        final ColorPickerPanel picker = new ColorPickerPanel("colorPicker", activeTemplateEntry.getColorCode(calendar.getId())) {
          @Override
          protected void onColorUpdate(final String selectedColor)
          {
            final TemplateCalendarProperties props = activeTemplateEntry.getCalendarProperties(calendar.getId());
            if (props != null) {
              props.setColorCode(selectedColor);
            } else {
              log.warn("TeamCalendarProperties not found: calendar.id='"
                  + calendar.getId()
                  + "' + for active template '"
                  + activeTemplateEntry.getName()
                  + "'.");
            }
          }
        };
        container.add(picker);
      }
    }
  }

}
