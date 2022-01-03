/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2018 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2018 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.config.snmp;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Top-level element for the snmp-config.xml configuration file.
 */

public class SnmpConfig extends Configuration implements Serializable {
    private static final long serialVersionUID = -5963402509661530467L;

    /**
     * Maps IP addresses to specific SNMP parameters (retries, timeouts...)
     */
    @JsonProperty("definition")
    private List<Definition> definitions = new ArrayList<>();

    @JsonProperty("profiles")
    private SnmpProfiles profiles;

    public SnmpConfig() {
        super();
    }

    public SnmpConfig(
            final Integer port,
            final Integer retry,
            final Integer timeout,
            final String readCommunity,
            final String writeCommunity,
            final String proxyHost,
            final String version,
            final Integer maxVarsPerPdu,
            final Integer maxRepetitions,
            final Integer maxRequestSize,
            final String securityName,
            final Integer securityLevel,
            final String authPassphrase,
            final String authProtocol,
            final String engineId,
            final String contextEngineId,
            final String contextName,
            final String privacyPassphrase,
            final String privacyProtocol,
            final String enterpriseId,
            final List<Definition> definitions) {
        super(port, retry, timeout, readCommunity, writeCommunity, proxyHost, version, maxVarsPerPdu, maxRepetitions, maxRequestSize,
              securityName, securityLevel, authPassphrase, authProtocol, engineId, contextEngineId, contextName, privacyPassphrase,
              privacyProtocol, enterpriseId);
        setDefinitions(definitions);
    }

    public List<Definition> getDefinitions() {
        if (definitions == null) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableList(definitions);
        }
    }

    public void setDefinitions(final List<Definition> definitions) {
        this.definitions = new ArrayList<Definition>(definitions);
    }

    public void addDefinition(final Definition definitions) throws IndexOutOfBoundsException {
        this.definitions.add(definitions);
    }

    public boolean removeDefinition(final Definition definitions) {
        return this.definitions.remove(definitions);
    }


    public SnmpProfiles getProfiles() {
        return profiles;
    }


    public void setProfiles(SnmpProfiles profiles) {
        this.profiles = profiles;
    }

    public SnmpProfiles getSnmpProfiles() {
        return profiles;
    }
    // changed for backward compatibility
    public void setSnmpProfiles(SnmpProfiles snmpProfiles) {
        this.profiles = snmpProfiles;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((definitions == null) ? 0 : definitions.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof SnmpConfig)) {
            return false;
        }
        SnmpConfig other = (SnmpConfig) obj;
        if (definitions == null) {
            if (other.definitions != null) {
                return false;
            }
        } else if (!definitions.equals(other.definitions)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "SnmpConfig [definitions=" + definitions + "]";
    }

    public void visit(SnmpConfigVisitor visitor) {
        visitor.visitSnmpConfig(this);
        for (final Definition definition : definitions) {
            definition.visit(visitor);
        }
        visitor.visitSnmpConfigFinished();
    }

    public Definition findDefinition(final InetAddress agentInetAddress) {
        final AddressSnmpConfigVisitor visitor = new AddressSnmpConfigVisitor(agentInetAddress);
        visit(visitor);
        return visitor.getDefinition();
    }

}
