/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2005-2014 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.config;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.hamcrest.Matchers;
import org.opennms.core.test.MockLogAppender;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.core.utils.LocationUtils;
import org.opennms.features.config.dao.impl.util.XmlConverter;
import org.opennms.netmgt.config.snmp.SnmpProfile;
import org.opennms.netmgt.snmp.SnmpAgentConfig;
import org.springframework.core.io.ByteArrayResource;

import junit.framework.TestCase;

public class SnmpPeerFactoryTest extends TestCase {

    private int m_version;

    private SnmpPeerFactory snmpPeerFactory;

    private void updateConfig(String amiConfigXml) throws IOException {
        try {
            XmlConverter converter = new XmlConverter("snmp-config.xsd", "snmp-config");
            String json = converter.xmlToJson(amiConfigXml);
            snmpPeerFactory.updateConfig(json);
            //factory.reload();
        }catch(Exception e){
            e.printStackTrace();;
        }
    }

    @Override
    protected void setUp() throws Exception {
        setVersion(SnmpAgentConfig.VERSION2C);
        //SnmpPeerFactory.setInstance(new SnmpPeerFactory(new ByteArrayResource(getSnmpConfig().getBytes())));
        MockLogAppender.setupLogging(true);
        snmpPeerFactory = mock(SnmpPeerFactory.class);
    }

    public void setVersion(int version) {
        m_version = version;
    }


    /**
     * String representing snmp-config.xml
     */
    public String getSnmpConfig() {
        return "<?xml version=\"1.0\"?>\n" + 
                "<snmp-config "+ 
                " retry=\"3\" timeout=\"3000\"\n" + 
                " read-community=\"public\"" +
                " write-community=\"private\"\n" + 
                " port=\"161\"\n" +
                " max-vars-per-pdu = \"23\" " +
                " version=\"v1\">\n" +
                "\n" +
                "   <definition port=\"9161\" version=\""+myVersion()+"\" " +
                "       security-name=\"opennmsUser\" \n" + 
                "       auth-passphrase=\"0p3nNMSv3\" \n" +
                "       privacy-passphrase=\"0p3nNMSv3\" >\n" +
                "       <specific>"+myLocalHost()+"</specific>\n" +
                "   </definition>\n" + 
                "\n" + 
                "   <definition version=\"v1\" read-community=\"specificv1\">\n" + 
                "       <specific>10.0.0.1</specific>\n" +
                "   </definition>\n" + 
                "\n" + 
                "   <definition version=\"v1\" read-community=\"specificv1\" max-request-size=\"484\">\n" + 
                "       <specific>10.0.0.2</specific>\n" +
                "   </definition>\n" + 
                "\n" + 
                "   <definition version=\"v1\" read-community=\"specificv1\" proxy-host=\""+myLocalHost()+"\">\n" + 
                "       <specific>10.0.0.3</specific>\n" +
                "   </definition>\n" + 
                "\n" + 
                "   <definition version=\"v3\" " +
                "       security-name=\"opennmsUser\" \n" + 
                "       auth-passphrase=\"0p3nNMSv3\" >\n" +
                "       <specific>20.20.20.20</specific>\n" +
                "   </definition>\n" + 
                "   <definition version=\"v3\" " +
                "       security-name=\"opennmsRangeUser\" \n" + 
                "       auth-passphrase=\"0p3nNMSv3\" >\n" +
                "       <range begin=\"1.1.1.1\" end=\"1.1.1.100\"/>\n" +
                "   </definition>\n" + 
                "   <definition version=\"v3\" " +
                "       security-name=\"opennmsContextUser\" \n" + 
                "       context-name=\"testContext\" \n" +
                "       engine-id=\"testEngineId\" \n" +
                "       context-engine-id=\"testContextEngineId\" >\n" +
                "       <specific>1.1.1.101</specific>" +
                "   </definition>\n" + 
                "   <definition version=\"v3\" " +
                "       security-name=\"opennmsuser1\" \n" + 
                "       context-name=\"VF:2\" >\n" +
                "       <specific>10.11.12.13</specific>\n" +
                "   </definition>\n" + 
                "   <definition version=\"v3\" " +
                "       security-name=\"opennmsuser2\" \n" + 
                "       context-name=\"VF:3\" auth-passphrase=\"\" auth-protocol=\"MD5\" privacy-passphrase=\"\" privacy-protocol=\"DES\">\n" +
                "       <specific>10.11.12.14</specific>\n" +
                "   </definition>\n" + 
                "   <definition version=\"v1\" read-community=\"rangev1\" max-vars-per-pdu=\"55\"> \n" + 
                "       <range begin=\"10.0.0.101\" end=\"10.0.0.200\"/>\n" +
                "   </definition>\n" + 
                "\n" + 
                "   <definition version=\"v2c\" read-community=\"rangev2c\">\n" + 
                "       <range begin=\"10.0.1.100\" end=\"10.0.5.100\"/>\n" +
                "       <range begin=\"10.7.20.100\" end=\"10.7.25.100\"/>\n" +
                "   </definition>\n" + 
                "\n" +
                "   <definition version=\"v2c\" read-community=\"specificv2c\">\n" + 
                "       <specific>192.168.0.50</specific>\n" +
                "   </definition>\n" + 
                "\n" +
                "   <definition version=\"v2c\" location=\"MINION\" timeout=\"2500\">\n" + 
                "       <specific>192.168.0.50</specific>\n" +
                "   </definition>\n" +
                "\n" +
                "   <definition version=\"v2c\" location=\"OPENNMS\" timeout=\"2000\">\n" + 
                "       <specific>192.160.0.50</specific>\n" +
                "   </definition>\n" + 
                "\n" +
                "   <definition version=\"v2c\" location=\"MINION\" timeout=\"2200\">\n" + 
                "       <specific>192.167.0.50</specific>\n" +
                "   </definition>\n" + 
                "\n" +
                "   <definition version=\"v2c\" location=\"MINION\" timeout=\"2300\">\n" + 
                "       <specific>192.166.0.50</specific>\n" +
                "   </definition>\n" + 
                "\n" +
                "   <definition version=\"v2c\" location=\"AUSTIN\" timeout=\"2100\">\n" + 
                "       <specific>192.164.0.50</specific>\n" +
                "   </definition>\n" +
                "\n" +
                "   <definition version=\"v3\"  timeout=\"2400\">\n" + 
                "       <specific>192.164.0.50</specific>\n" +
                "   </definition>\n" +
                "\n" +
                "   <definition version=\"v2c\" read-community=\"rangev2\">\n" + 
                "       <specific>192.167.0.50</specific>\n" +
                "   </definition>\n" + 
                "\n" +
                "   <definition version=\"v2c\" read-community=\"ipmatch\" max-vars-per-pdu=\"128\" max-repetitions=\"7\" >\n" + 
                "       <ip-match>77.5-12,15.1-255.255</ip-match>\n" +
                "   </definition>\n" + 
                "\n" +
                    "<profiles>"
                        +"<profile " +  "retry=\"2\" "
                        + "  timeout=\"100\" "
                        + "  read-community=\"public\" "
                        + "  proxy-host=\""+myLocalHost()+"\""
                        + "  version=\"v2c\" "
                        + "  max-vars-per-pdu=\"4\" "
                        + "  max-repetitions=\"5\" "
                        + "  max-request-size=\"484\" "
                        + "  security-name=\"securityName\" "
                        + "  security-level=\"3\" "
                        + "  auth-protocol=\"MD5\" "
                        + "  engine-id=\"engineId\" "
                        + "  context-engine-id=\"contextEngineId\" "
                        + "  context-name=\"profileContext\" "
                        + "  enterprise-id=\"enterpriseId\">"
                        + "<label>profile2</label>"
                        + "<filter>*.opennms.com</filter>"
                        + "</profile>"
                        + "<profile "  + "retry=\"5\" "
                        + "  timeout=\"300\" "
                        + "  write-community=\"private\" "
                        + "  proxy-host=\""+myLocalHost()+"\""
                        + "  version=\"v3\" "
                        + "  max-vars-per-pdu=\"4\" "
                        + "  max-repetitions=\"5\" "
                        + "  max-request-size=\"484\" "
                        + "  security-level=\"3\" "
                        + "  auth-protocol=\"MD5\" "
                        + "  engine-id=\"engineId\" "
                        + "  context-engine-id=\"contextEngineId\" "
                        + "  context-name=\"profileContext\" "
                        //+ "  privacy-protocol=\"DES\" "
                        + "  enterprise-id=\"enterpriseId\">"
                        + "<label>profile2</label>"
                        + "<filter>*.opennms.org</filter>"
                        + "</profile>"
                    + "</profiles>"
                + "</snmp-config>";
    }

    /**
     * String representing snmp-config.xml
     */
    public String getBadRangeSnmpConfig() {
        return "<?xml version=\"1.0\"?>\n" + 
                "<snmp-config "+ 
                " retry=\"3\" timeout=\"3000\"\n" + 
                " read-community=\"public\"" +
                " write-community=\"private\"\n" + 
                " port=\"161\"\n" +
                " max-vars-per-pdu = \"23\" " +
                " version=\"v1\">\n" +
                "\n" +
                "   <definition version=\"v2c\" read-community=\"rangev2c\">\n" + 
                "       <range begin=\"10.0.5.100\" end=\"10.0.1.100\"/>\n" +
                "       <range begin=\"10.7.25.100\" end=\"10.7.20.100\"/>\n" +
                "   </definition>\n" + 
                "\n" + 
                "</snmp-config>";
    }

    protected String myLocalHost() {

        //      try {
        //          return InetAddressUtils.str(InetAddress.getLocalHost());
        //      } catch (UnknownHostException e) {
        //          e.printStackTrace();
        //          fail("Exception getting localhost");
        //      }
        //      
        //      return null;

        return "127.0.0.1";
    }

    private String myVersion() {
        switch (m_version) {
        case SnmpAgentConfig.VERSION1 :
            return "v1";
        case SnmpAgentConfig.VERSION2C :
            return "v2c";
        case SnmpAgentConfig.VERSION3 :
            return "v3";
        default :
            return "v1";
        }
    }



    @Override
    protected void tearDown() {

    }

    public void testProxiedAgent() throws IOException {
        this.updateConfig(this.getSnmpConfig());
        SnmpAgentConfig agentConfig = snmpPeerFactory.getAgentConfig(InetAddressUtils.addr("10.0.0.3"));
        assertEquals("10.0.0.3", InetAddressUtils.str(agentConfig.getProxyFor()));
        assertEquals("127.0.0.1", InetAddressUtils.str(agentConfig.getAddress()));
        agentConfig.toString();
    }

    public void testDefaultMaxRequestSize() throws IOException {
        this.updateConfig(this.getSnmpConfig());

        SnmpAgentConfig agentConfig = snmpPeerFactory.getInstance().getAgentConfig(InetAddressUtils.addr("10.0.0.1"));
        assertEquals(SnmpAgentConfig.DEFAULT_MAX_REQUEST_SIZE, agentConfig.getMaxRequestSize());

        agentConfig = SnmpPeerFactory.getInstance().getAgentConfig(InetAddressUtils.addr("10.0.0.2"));
        assertEquals(484, agentConfig.getMaxRequestSize());
    }

    public void testDefaultMaxVarsPerPdu() throws IOException {
        this.updateConfig(this.getSnmpConfig());

        SnmpAgentConfig agentConfig = snmpPeerFactory.getInstance().getAgentConfig(InetAddressUtils.addr(myLocalHost()));
        assertEquals(23, agentConfig.getMaxVarsPerPdu());
    }

    public void testConfigureDefaultMaxVarsPerPdu() throws IOException {
        this.updateConfig(this.getSnmpConfig());

        SnmpAgentConfig agentConfig = snmpPeerFactory.getInstance().getAgentConfig(InetAddressUtils.addr("10.0.0.150"));
        assertEquals(55, agentConfig.getMaxVarsPerPdu());
    }

    public void testGetMaxRepetitions() throws IOException {
        this.updateConfig(this.getSnmpConfig());

        SnmpAgentConfig agentConfig = snmpPeerFactory.getInstance().getAgentConfig(InetAddressUtils.addr("77.5.5.255"));
        assertEquals("ipmatch", agentConfig.getReadCommunity());
        assertEquals(7, agentConfig.getMaxRepetitions());

        agentConfig = SnmpPeerFactory.getInstance().getAgentConfig(InetAddressUtils.addr("10.0.0.1"));
        assertEquals("specificv1", agentConfig.getReadCommunity());
        assertEquals(2, agentConfig.getMaxRepetitions());
    }

    public void testGetTargetFromPatterns() throws IOException {
        this.updateConfig(this.getSnmpConfig());

        SnmpAgentConfig agentConfig = snmpPeerFactory.getInstance().getAgentConfig(InetAddressUtils.addr("77.5.5.255"));
        assertEquals("ipmatch", agentConfig.getReadCommunity());
        assertEquals(128, agentConfig.getMaxVarsPerPdu());

        agentConfig = SnmpPeerFactory.getInstance().getAgentConfig(InetAddressUtils.addr("77.15.80.255"));
        assertEquals("ipmatch", agentConfig.getReadCommunity());
        assertEquals(7, agentConfig.getMaxRepetitions());

        //should be default community "public" because of 4
        agentConfig = SnmpPeerFactory.getInstance().getAgentConfig(InetAddressUtils.addr("77.4.5.255"));
        assertEquals("public", agentConfig.getReadCommunity());

        //should be default community because of 0
        agentConfig = SnmpPeerFactory.getInstance().getAgentConfig(InetAddressUtils.addr("77.6.0.255"));
        assertEquals("public", agentConfig.getReadCommunity());
    }

    public void testGetSnmpAgentConfig() throws IOException {
        this.updateConfig(this.getSnmpConfig());

        SnmpAgentConfig agentConfig = snmpPeerFactory.getInstance().getAgentConfig(InetAddressUtils.addr(myLocalHost()));
        assertEquals(SnmpAgentConfig.VERSION2C, agentConfig.getVersion());
    }

    /**
     * This tests getting an SnmpAgentConfig
     * @throws UnknownHostException
     */
    public void testGetConfig() throws IOException {
        this.updateConfig(this.getSnmpConfig());

        assertNotNull(SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getLocalHost()));
    }

    /**
     * This tests for ranges configured for a v2 node and community string
     * @throws UnknownHostException
     */
    public void testGetv2cInRange() throws IOException {
        this.updateConfig(this.getSnmpConfig());

        SnmpAgentConfig agentConfig = snmpPeerFactory.getInstance().getAgentConfig(InetAddressUtils.addr("10.7.23.100"));
        assertNotNull(agentConfig);
        assertEquals(SnmpAgentConfig.VERSION2C, agentConfig.getVersion());
        assertEquals("rangev2c", agentConfig.getReadCommunity());
    }

    /**
     * This tests for ranges configured for v3 node and security name
     * @throws UnknownHostException 
     */
    public void testGetv3ConfigInRange() throws IOException {
        this.updateConfig(this.getSnmpConfig());

        SnmpAgentConfig agentConfig = snmpPeerFactory.getInstance().getAgentConfig(InetAddressUtils.addr("1.1.1.50"));
        assertNotNull(agentConfig);
        assertEquals(SnmpAgentConfig.VERSION3, agentConfig.getVersion());
        assertEquals("opennmsRangeUser", agentConfig.getSecurityName());
    }

    /**
     * This tests for context-name configured for v3 node
     * @throws UnknownHostException 
     */
    public void testGetv3ConfigWithContextNameAndMore() throws IOException {
        this.updateConfig(this.getSnmpConfig());

        SnmpAgentConfig agentConfig = snmpPeerFactory.getInstance().getAgentConfig(InetAddressUtils.addr("1.1.1.101"));
        assertNotNull(agentConfig);
        assertEquals(SnmpAgentConfig.VERSION3, agentConfig.getVersion());
        assertEquals("opennmsContextUser", agentConfig.getSecurityName());
        assertEquals("testContext", agentConfig.getContextName());
        assertEquals("testEngineId", agentConfig.getEngineId());
        assertEquals("testContextEngineId", agentConfig.getContextEngineId());
    }
    /**
     * This tests getting a v1 config
     * @throws UnknownHostException
     */
    public void testGetV1Config() throws IOException {
        this.updateConfig(this.getSnmpConfig());

        SnmpAgentConfig agentConfig = snmpPeerFactory.getInstance().getAgentConfig(InetAddressUtils.addr("10.0.0.1"));
        assertNotNull(agentConfig);
        assertEquals(SnmpAgentConfig.VERSION1, agentConfig.getVersion());
        assertEquals("specificv1", agentConfig.getReadCommunity());
    }

    /**
     * This tests for a specifically defined v2c agentConfig
     * 
     * @throws UnknownHostException
     */
    public void testGetV2cConfig() throws IOException {
        this.updateConfig(this.getSnmpConfig());

        SnmpAgentConfig agentConfig = snmpPeerFactory.getInstance()
                .getAgentConfig(InetAddressUtils.addr("192.168.0.50"));
        assertNotNull(agentConfig);
        assertEquals(agentConfig.getVersion(), SnmpAgentConfig.VERSION2C);
        assertEquals("specificv2c", agentConfig.getReadCommunity());
    }

    /**
     * Below tests for a valid/invalid location match
     * 
     * @throws UnknownHostException
     */
    public void testGetConfigWithValidLocation() throws IOException {
        this.updateConfig(this.getSnmpConfig());

        SnmpAgentConfig agentConfig = snmpPeerFactory.getInstance()
                .getAgentConfig(InetAddressUtils.addr("192.168.0.50"), "MINION");
        assertNotNull(agentConfig);
        assertEquals(agentConfig.getVersion(), SnmpAgentConfig.VERSION2C);
        assertEquals(2500, agentConfig.getTimeout());
    }

    public void testGetConfigWithInvalidLocation() throws IOException {
        this.updateConfig(this.getSnmpConfig());

        SnmpAgentConfig agentConfig = snmpPeerFactory.getInstance()
                .getAgentConfig(InetAddressUtils.addr("192.168.0.51"), "AUSTIN");
        assertNotNull(agentConfig);
        assertEquals(SnmpAgentConfig.VERSION1, agentConfig.getVersion());
        assertEquals("public", agentConfig.getReadCommunity());
    }

    public void testGetV2cConfigWithoutLocation() throws IOException {
        this.updateConfig(this.getSnmpConfig());

        SnmpAgentConfig agentConfig = snmpPeerFactory.getInstance()
                .getAgentConfig(InetAddressUtils.addr("192.167.0.50"), LocationUtils.DEFAULT_LOCATION_NAME);
        assertNotNull(agentConfig);
        assertEquals(SnmpAgentConfig.VERSION2C, agentConfig.getVersion());
        assertEquals("rangev2", agentConfig.getReadCommunity());
    }

    public void testGetV2cConfigWithDifferentLocation() throws IOException {
        this.updateConfig(this.getSnmpConfig());

        SnmpAgentConfig agentConfig = snmpPeerFactory.getInstance()
                .getAgentConfig(InetAddressUtils.addr("192.166.0.50"));
        assertNotNull(agentConfig);
        assertEquals(SnmpAgentConfig.VERSION1, agentConfig.getVersion());
        assertEquals("public", agentConfig.getReadCommunity());
    }


    public void testGetV3ConfigWithoutLocation() throws IOException {
        this.updateConfig(this.getSnmpConfig());

        SnmpAgentConfig agentConfig = snmpPeerFactory.getInstance()
                .getAgentConfig(InetAddressUtils.addr("192.164.0.50"), LocationUtils.DEFAULT_LOCATION_NAME);
        assertNotNull(agentConfig);
        assertEquals(SnmpAgentConfig.VERSION3, agentConfig.getVersion());
        assertEquals(2400, agentConfig.getTimeout());
    }

    public void testNoMatchedDefinitionWithLocationMatch() throws IOException {
        this.updateConfig(this.getSnmpConfig());

        SnmpAgentConfig agentConfig = snmpPeerFactory.getInstance()
                .getAgentConfig(InetAddressUtils.addr("192.160.0.50"), "MINION");
        assertNotNull(agentConfig);
        assertEquals(SnmpAgentConfig.VERSION1, agentConfig.getVersion());
        assertEquals("public", agentConfig.getReadCommunity());
    }

    public void testFallbackToDefaultLocation() throws IOException {
        this.updateConfig(this.getSnmpConfig());

        SnmpAgentConfig agentConfig = snmpPeerFactory.getInstance().getAgentConfig(
                InetAddressUtils.addr("10.7.23.100"), "SOME-LOCATION");
        assertNotNull(agentConfig);
        assertEquals(SnmpAgentConfig.VERSION2C, agentConfig.getVersion());
        assertEquals("rangev2c", agentConfig.getReadCommunity());
    }

    /**
     * This tests for ranges configured for a v2 node and community string
     * 
     * @throws UnknownHostException
     */
    public void testReversedRange() throws IOException {
       // SnmpPeerFactory.setInstance(new SnmpPeerFactory(new ByteArrayResource(getBadRangeSnmpConfig().getBytes())));
        this.updateConfig(this.getSnmpConfig());

        SnmpAgentConfig agentConfig = snmpPeerFactory.getInstance().getAgentConfig(InetAddressUtils.addr("10.7.23.100"));
        assertNotNull(agentConfig);
        assertEquals(SnmpAgentConfig.VERSION2C, agentConfig.getVersion());
        assertEquals("rangev2c", agentConfig.getReadCommunity());
    }

    public void testSnmpv3WithNoAuthNoPriv() throws Exception {
        //SnmpPeerFactory.setInstance(new SnmpPeerFactory(new ByteArrayResource(getSnmpConfig().getBytes())));
        this.updateConfig(this.getSnmpConfig());

        SnmpAgentConfig agentConfig = snmpPeerFactory.getInstance().getAgentConfig(InetAddressUtils.addr("10.11.12.13"));
        assertEquals("opennmsuser1", agentConfig.getSecurityName());
        assertEquals("VF:2", agentConfig.getContextName());
        assertNull(agentConfig.getAuthProtocol());
        assertNull(agentConfig.getPrivProtocol());
        assertNull(agentConfig.getAuthPassPhrase());
        assertNull(agentConfig.getPrivPassPhrase());
        assertEquals(1, agentConfig.getSecurityLevel());

        agentConfig = SnmpPeerFactory.getInstance().getAgentConfig(InetAddressUtils.addr(myLocalHost()));
        assertEquals(3, agentConfig.getSecurityLevel());
    }

    public void testSnmpProfile() throws IOException {
        //SnmpPeerFactory.setInstance(new SnmpPeerFactory(new ByteArrayResource(getSnmpConfig().getBytes())));
        this.updateConfig(this.getSnmpConfig());

        List<SnmpProfile> profiles = snmpPeerFactory.getInstance().getProfiles();
        assertEquals(2, profiles.size());
        for(SnmpProfile snmpProfile : profiles) {
            SnmpAgentConfig snmpAgentConfig = SnmpPeerFactory.getInstance().
                    getAgentConfigFromProfile(snmpProfile, InetAddressUtils.addr("10.1.12.1"));
            assertEquals("profileContext", snmpAgentConfig.getContextName());
            // Even if read-community/write-community is not specified, should use defaults.
            assertEquals("public", snmpAgentConfig.getReadCommunity());
            assertEquals("private", snmpAgentConfig.getWriteCommunity());
            assertNull(snmpAgentConfig.getPrivProtocol());
            assertThat(snmpAgentConfig.getVersionAsString(), Matchers.isOneOf("v2c", "v3"));
        }
    }
}
