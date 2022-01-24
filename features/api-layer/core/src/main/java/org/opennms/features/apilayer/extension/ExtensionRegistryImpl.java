/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
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

package org.opennms.features.apilayer.extension;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.opennms.integration.api.v1.extension.OpenNMSExtension;
import org.opennms.integration.api.v1.extension.OpenNMSExtensionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtensionRegistryImpl implements ExtensionRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(ExtensionRegistryImpl.class);
    private final Map<String, OpenNMSExtension> extensionByID = new HashMap<>();
    private final Map<String, String> extensionIDByFactoryClass = new HashMap<>(); //TODO do we need this?

    public synchronized void onBind(OpenNMSExtensionFactory  extensionFactory, Map properties) {
        LOG.debug("Extension registry bind called with {}: {}", extensionFactory, properties);
        if(extensionFactory != null) {
            OpenNMSExtension extension = extensionFactory.createExtension();
            extensionByID.put(extension.getExtensionID(), extension);
            extensionIDByFactoryClass.put(extensionFactory.getClass().getCanonicalName(), extension.getExtensionID());
        }
    }

    public synchronized void onUnbind(OpenNMSExtensionFactory extensionFactory, Map properties) {
        LOG.debug("Extension registry unBind called with {}: {}", extensionFactory, properties);
        if(extensionFactory != null) {
            String factoryClassName = extensionFactory.getClass().getCanonicalName();
            extensionByID.remove(extensionIDByFactoryClass.get(extensionFactory.getClass().getCanonicalName()));
            extensionIDByFactoryClass.remove(factoryClassName);
        }
    }

    @Override
    public Set<String> getExtensionNames() {
        return extensionByID.keySet();
    }

    @Override
    public OpenNMSExtension getExtensionByID(String id) {
        return extensionByID.get(id);
    }
}

