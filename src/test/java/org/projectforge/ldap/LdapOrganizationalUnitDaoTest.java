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

package org.projectforge.ldap;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

public class LdapOrganizationalUnitDaoTest extends LdapRealTestBase
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(LdapOrganizationalUnitDaoTest.class);

  LdapOrganizationalUnitDao ldapOrganizationalUnitDao;

  @Override
  @Before
  public void setup()
  {
    super.setup();
    ldapOrganizationalUnitDao = new LdapOrganizationalUnitDao();
    ldapOrganizationalUnitDao.ldapConnector = ldapConnector;
  }

  @Test
  public void deleteAndCreateOu()
  {
    if (ldapConfig == null) {
      log.info("No LDAP server configured for tests. Skipping test.");
      return;
    }
    final String ou = "deactivated";
    final String path = "ou=pf-test-ou";
    ldapOrganizationalUnitDao.createIfNotExist(path, "description");
    Assert.assertTrue(ldapOrganizationalUnitDao.doesExist(path));
    ldapOrganizationalUnitDao.createIfNotExist(ou, "description", path);
    Assert.assertTrue(ldapOrganizationalUnitDao.doesExist(ou, path));

    ldapOrganizationalUnitDao.deleteIfExists(ou, path);
    Assert.assertFalse(ldapOrganizationalUnitDao.doesExist(ou, path));
    ldapOrganizationalUnitDao.deleteIfExists(path);
    Assert.assertFalse(ldapOrganizationalUnitDao.doesExist(path));
  }
}