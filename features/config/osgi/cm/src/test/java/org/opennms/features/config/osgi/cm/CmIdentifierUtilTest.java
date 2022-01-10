/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2021 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 2021-2021 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.features.config.osgi.cm;

import static org.junit.Assert.*;

import org.junit.Test;
import org.opennms.features.config.service.api.ConfigUpdateInfo;

public class CmIdentifierUtilTest {

    @Test
    public void shouldParse() {
        check("", "", CmIdentifierUtil.CONFIG_ID);
        check("abc", "abc", CmIdentifierUtil.CONFIG_ID);
        check("abc-def", "abc", "def");
    }

    private void check(String pid, String expectedName, String expectedId) {
        ConfigUpdateInfo id = CmIdentifierUtil.pidToCmIdentifier(pid);
        assertEquals(expectedName, id.getConfigName());
        assertEquals(expectedId, id.getConfigId());
    }
}