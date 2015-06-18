/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2009-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
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

package org.opennms.web.rest;

import javax.ws.rs.Path;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;

import org.opennms.core.config.api.ConfigurationResourceException;
import org.opennms.web.rest.config.AgentConfigurationResource;
import org.opennms.web.rest.config.CollectionConfigurationResource;
import org.opennms.web.rest.config.DataCollectionConfigResource;
import org.opennms.web.rest.config.JmxDataCollectionConfigResource;
import org.opennms.web.rest.config.PollerConfigurationResource;
import org.opennms.web.rest.config.SnmpConfigurationResource;
import org.springframework.stereotype.Component;

/**
 * ReST service for (JAXB) ConfigurationResource files.
 */

@Component("configRestService")
@Path("config")
public class ConfigRestService extends OnmsRestService {

    private ResourceContext m_context;

    @Context
    public void setResourceContext(ResourceContext context) {
        m_context = context;
    }

    @Path("{location}/polling")
    public PollerConfigurationResource getPollerConfiguration() {
        return m_context.getResource(PollerConfigurationResource.class);
    }

    @Path("{location}/collection")
    public CollectionConfigurationResource getCollectionConfigurationResource() throws ConfigurationResourceException {
        return m_context.getResource(CollectionConfigurationResource.class);
    }

    @Path("datacollection")
    public DataCollectionConfigResource getDatacollectionConfigurationResource() throws ConfigurationResourceException {
        return m_context.getResource(DataCollectionConfigResource.class);
    }

    @Path("agents")
    public AgentConfigurationResource getAgentConfigurationResource() throws ConfigurationResourceException {
        return m_context.getResource(AgentConfigurationResource.class);
    }

    @Path("snmp")
    public SnmpConfigurationResource getSnmpConfigurationResource() throws ConfigurationResourceException {
        return m_context.getResource(SnmpConfigurationResource.class);
    }

    @Path("jmx")
    public JmxDataCollectionConfigResource getJmxDataCollectionConfigResource() throws ConfigurationResourceException {
        return m_context.getResource(JmxDataCollectionConfigResource.class);
    }
}
