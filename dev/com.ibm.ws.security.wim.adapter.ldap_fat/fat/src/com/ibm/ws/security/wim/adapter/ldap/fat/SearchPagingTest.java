/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.adapter.ldap.fat;

import static componenttest.topology.utils.LDAPFatUtils.updateConfigDynamically;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.AttributesCache;
import com.ibm.websphere.simplicity.config.wim.LdapCache;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.config.wim.SearchResultsCache;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.apacheds.EmbeddedApacheDS;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPFatUtils;
import componenttest.topology.utils.LDAPUtils;

/**
 * LDAP search paging tests.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class SearchPagingTest {
    private static LibertyServer libertyServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.adapter.ldap.fat.search.paging");
    private static final Class<?> c = SearchPagingTest.class;
    private static UserRegistryServletConnection servlet;

    /**
     * Nearly empty server configuration. This should just contain the feature manager configuration with no
     * registries or federated repository configured.
     */
    private static ServerConfiguration serverConfiguration = null;

    private static EmbeddedApacheDS ldapServer = null;
    private static final String LDAP_BASE_ENTRY = "o=ibm,c=us";

    private static final int MAX_ENTRIES = 100;

    /**
     * Setup the test case.
     *
     * @throws Exception If the setup failed for some reason.
     */
    @BeforeClass
    public static void setupClass() throws Exception {
        setupLibertyServer();
        setupLdapServer();
    }

    /**
     * Tear down the test.
     */
    @AfterClass
    public static void teardownClass() throws Exception {
        if (libertyServer != null) {
            try {
                libertyServer.stopServer();
            } catch (Exception e) {
                Log.error(c, "teardown", e, "Liberty server threw error while stopping. " + e.getMessage());
            }
        }
        if (ldapServer != null) {
            try {
                ldapServer.stopService();
            } catch (Exception e) {
                Log.error(c, "teardown", e, "LDAP server threw error while stopping. " + e.getMessage());
            }
        }

        libertyServer.deleteFileFromLibertyInstallRoot("lib/features/internalfeatures/securitylibertyinternals-1.0.mf");
    }

    /**
     * Setup the Liberty server. This server will start with very basic configuration. The tests
     * will configure the server dynamically.
     *
     * @throws Exception If there was an issue setting up the Liberty server.
     */
    private static void setupLibertyServer() throws Exception {

        final String methodName = "setupLibertyServer";
        Log.info(c, methodName, "Starting Liberty server setup");

        /*
         * Add LDAP variables to bootstrap properties file
         */
        LDAPUtils.addLDAPVariables(libertyServer);
        Log.info(c, "setUp", "Starting the server... (will wait for userRegistry servlet to start)");
        libertyServer.copyFileToLibertyInstallRoot("lib/features", "internalfeatures/securitylibertyinternals-1.0.mf");
        libertyServer.addInstalledAppForValidation("userRegistry");
        libertyServer.startServer(c.getName() + ".log");

        /*
         * Make sure the application has come up before proceeding
         */
        assertNotNull("Application userRegistry does not appear to have started.", libertyServer.waitForStringInLog("CWWKZ0001I:.*userRegistry"));
        assertNotNull("Security service did not report it was ready", libertyServer.waitForStringInLog("CWWKS0008I"));
        assertNotNull("Server did not came up", libertyServer.waitForStringInLog("CWWKF0011I"));

        Log.info(c, "setUp", "Creating servlet connection the server");
        servlet = new UserRegistryServletConnection(libertyServer.getHostname(), libertyServer.getHttpDefaultPort());

        servlet.getRealm();
        Thread.sleep(5000);
        servlet.getRealm();

        /*
         * The original server configuration has no registry or Federated Repository configuration.
         */
        serverConfiguration = libertyServer.getServerConfiguration();

        Log.info(c, methodName, "Finished Liberty server setup");
    }

    /**
     * Configure the LDAP server.
     *
     * @throws Exception If the server failed to start for some reason.
     */
    private static void setupLdapServer() throws Exception {
        final String methodName = "setupLdapServer";
        Log.info(c, methodName, "Starting LDAP server setup");

        ldapServer = new EmbeddedApacheDS("testing");
        ldapServer.addPartition("testing", LDAP_BASE_ENTRY);
        ldapServer.startServer();

        /*
         * Add the partition entries.
         */
        Entry entry = ldapServer.newEntry(LDAP_BASE_ENTRY);
        entry.add("objectclass", "organization");
        entry.add("o", "ibm");
        ldapServer.add(entry);

        /*
         * Create the users and groups.
         */
        for (int idx = 0; idx < MAX_ENTRIES; idx++) {
            String userName = "user" + idx;
            String userDn = "uid=" + userName + "," + LDAP_BASE_ENTRY;
            entry = ldapServer.newEntry(userDn);
            entry.add("objectclass", "inetorgperson");
            entry.add("uid", userName);
            entry.add("sn", userName);
            entry.add("cn", userName);
            entry.add("userPassword", "password");
            ldapServer.add(entry);

            String groupName = "group" + idx;
            String groupDn = "cn=" + groupName + "," + LDAP_BASE_ENTRY;
            entry = ldapServer.newEntry(groupDn);
            entry.add("objectclass", "groupofnames");
            entry.add("cn", groupName);
            entry.add("member", userDn);
            ldapServer.add(entry);
        }

        Log.info(c, methodName, "Finished LDAP server setup");
    }

    /**
     * Update the Liberty server with a new paging size.
     *
     * @param searchPageSize The search paging size to use.
     * @throws Exception If the update failed for some reason.
     */
    private static void updateLibertyServer(Integer searchPageSize) throws Exception {
        final String methodName = "updateLibertyServer";
        Log.info(c, methodName, "Starting Liberty server update");

        ServerConfiguration server = serverConfiguration.clone();

        LdapRegistry ldap = new LdapRegistry();

        ldap.setRealm("LDAPRealm");
        ldap.setHost("localhost");
        ldap.setPort(String.valueOf(ldapServer.getLdapServer().getPort()));
        ldap.setBaseDN(LDAP_BASE_ENTRY);
        ldap.setBindDN(EmbeddedApacheDS.getBindDN());
        ldap.setBindPassword(EmbeddedApacheDS.getBindPassword());
        ldap.setLdapType("Custom");
        ldap.setLdapCache(new LdapCache(new AttributesCache(false, 0, 0, "0s"), new SearchResultsCache(false, 0, 0, "0s")));

        if (searchPageSize != null) {
            ldap.setSearchPageSize(searchPageSize);
        }

        server.getLdapRegistries().add(ldap);
        updateConfigDynamically(libertyServer, server);

        Log.info(c, methodName, "Finished Liberty server update");
    }

    /**
     * Basic searching test with no paging enabled.
     *
     * @throws Exception If the test failed for some reason.
     */
    @Test
    public void searchWithNoPaging() throws Exception {
        Log.info(c, "searchWithNoPaging", "Starting searchWithNoPaging test...");
        updateLibertyServer(null);

        /*
         * Check a user search.
         */
        LDAPFatUtils.resetMarksInLogs(libertyServer);
        assertEquals(MAX_ENTRIES, servlet.getUsers("*", 0).getList().size());
        assertTrue(libertyServer.findStringsInLogsAndTrace("Search page: 1").isEmpty());

        /*
         * Check a group search.
         */
        LDAPFatUtils.resetMarksInLogs(libertyServer);
        assertEquals(MAX_ENTRIES, servlet.getGroups("*", 0).getList().size());
        assertTrue(libertyServer.findStringsInLogsAndTrace("Search page: 1").isEmpty());
    }

    /**
     * Test searching with paging enabled.
     *
     * @throws Exception If the test failed for some reason.
     */
    @Test
    public void searchWithPaging() throws Exception {
        Log.info(c, "searchWithPaging", "Starting searchWithPaging test...");
        updateLibertyServer(MAX_ENTRIES / 10); // Force 10 paged searches.

        /*
         * Check a user search.
         */
        LDAPFatUtils.resetMarksInLogs(libertyServer);
        assertEquals(MAX_ENTRIES, servlet.getUsers("*", 0).getList().size());
        assertFalse("Didn't find search for page 1", libertyServer.findStringsInLogsAndTrace("Search page: 1").isEmpty());
        assertFalse("Didn't find search for page 2", libertyServer.findStringsInLogsAndTrace("Search page: 2").isEmpty());
        assertFalse("Didn't find search for page 3", libertyServer.findStringsInLogsAndTrace("Search page: 3").isEmpty());
        assertFalse("Didn't find search for page 4", libertyServer.findStringsInLogsAndTrace("Search page: 4").isEmpty());
        assertFalse("Didn't find search for page 5", libertyServer.findStringsInLogsAndTrace("Search page: 5").isEmpty());
        assertFalse("Didn't find search for page 6", libertyServer.findStringsInLogsAndTrace("Search page: 6").isEmpty());
        assertFalse("Didn't find search for page 7", libertyServer.findStringsInLogsAndTrace("Search page: 7").isEmpty());
        assertFalse("Didn't find search for page 8", libertyServer.findStringsInLogsAndTrace("Search page: 8").isEmpty());
        assertFalse("Didn't find search for page 9", libertyServer.findStringsInLogsAndTrace("Search page: 9").isEmpty());
        assertFalse("Didn't find search for page 10", libertyServer.findStringsInLogsAndTrace("Search page: 10").isEmpty());

        /*
         * Check a group search.
         */
        LDAPFatUtils.resetMarksInLogs(libertyServer);
        assertEquals(MAX_ENTRIES, servlet.getGroups("*", 0).getList().size());
        assertFalse("Didn't find search for page 1", libertyServer.findStringsInLogsAndTrace("Search page: 1").isEmpty());
        assertFalse("Didn't find search for page 2", libertyServer.findStringsInLogsAndTrace("Search page: 2").isEmpty());
        assertFalse("Didn't find search for page 3", libertyServer.findStringsInLogsAndTrace("Search page: 3").isEmpty());
        assertFalse("Didn't find search for page 4", libertyServer.findStringsInLogsAndTrace("Search page: 4").isEmpty());
        assertFalse("Didn't find search for page 5", libertyServer.findStringsInLogsAndTrace("Search page: 5").isEmpty());
        assertFalse("Didn't find search for page 6", libertyServer.findStringsInLogsAndTrace("Search page: 6").isEmpty());
        assertFalse("Didn't find search for page 7", libertyServer.findStringsInLogsAndTrace("Search page: 7").isEmpty());
        assertFalse("Didn't find search for page 8", libertyServer.findStringsInLogsAndTrace("Search page: 8").isEmpty());
        assertFalse("Didn't find search for page 9", libertyServer.findStringsInLogsAndTrace("Search page: 9").isEmpty());
        assertFalse("Didn't find search for page 10", libertyServer.findStringsInLogsAndTrace("Search page: 10").isEmpty());
    }
}
