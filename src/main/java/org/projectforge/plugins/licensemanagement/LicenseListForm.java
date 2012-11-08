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

package org.projectforge.plugins.licensemanagement;

import org.apache.log4j.Logger;
import org.projectforge.core.BaseSearchFilter;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivType;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

/**
 * The list formular for the list view (this example has no filter settings). See ToDoListPage for seeing how to use filter settings.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class LicenseListForm extends AbstractListForm<BaseSearchFilter, LicenseListPage>
{
  private static final long serialVersionUID = -8159930022688216785L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(LicenseListForm.class);

  public LicenseListForm(final LicenseListPage parentPage)
  {
    super(parentPage);
  }

  @Override
  protected void init()
  {
    super.init();
    gridBuilder.newColumnsPanel().newColumnPanel(DivType.COL_60);
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(parentPage.getString("label.options")).setNoLabelFor();
      final DivPanel checkBoxPanel = fs.addNewCheckBoxDiv();
      checkBoxPanel.add(createOnlyDeletedCheckBoxPanel(checkBoxPanel.newChildId()));
    }
    {
      // DropDownChoice page size
      gridBuilder.newColumnPanel(DivType.COL_40);
      addPageSizeFieldset();
    }
  }

  @Override
  protected BaseSearchFilter newSearchFilterInstance()
  {
    return new BaseSearchFilter();
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}