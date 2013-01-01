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

package org.projectforge.plugins.banking;

import org.apache.log4j.Logger;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;


public class BankAccountListForm extends AbstractListForm<BankAccountListFilter, BankAccountListPage>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(BankAccountListForm.class);

  private static final long serialVersionUID = -5969136444233092172L;

  @Override
  protected void init()
  {
    super.init();
    gridBuilder.newSplitPanel(GridSize.COL66);
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(parentPage.getString("label.options")).setNoLabelFor();
      final DivPanel checkBoxPanel = fs.addNewCheckBoxDiv();
      checkBoxPanel.add(createOnlyDeletedCheckBoxPanel(checkBoxPanel.newChildId()));
    }
    {
      // DropDownChoice page size
      gridBuilder.newSplitPanel(GridSize.COL33);
      addPageSizeFieldset();
    }
  }

  public BankAccountListForm(final BankAccountListPage parentPage)
  {
    super(parentPage);
  }

  @Override
  protected BankAccountListFilter newSearchFilterInstance()
  {
    return new BankAccountListFilter();
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
