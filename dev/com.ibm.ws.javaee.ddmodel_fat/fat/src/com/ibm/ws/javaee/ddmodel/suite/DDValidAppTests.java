/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.suite;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class DDValidAppTests {

    public static final String[] ALLOWED_ERRORS = new String[] {
        // EMPTY
    };

    @BeforeClass
    public static void setUp() throws Exception {
        CommonTests.commonSetUp(
            DDValidAppTests.class,
            "server_app.xml",
            CommonTests.setUpTestApp);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        CommonTests.commonTearDown(
            DDValidAppTests.class,
            CommonTests.tearDownTestApp,
            ALLOWED_ERRORS);
    }

    //

    @Test
    public void testAutoInstall_Application() throws Exception {
        CommonTests.test(DDValidAppTests.class, "testAutoInstall");    
    }

    @Test
    public void testConfigurationSide_Application() throws Exception {
        CommonTests.test(DDValidAppTests.class, "testConfigurationSide");
    }

    @Test
    public void testBasicBindingsConfiguration_Application() throws Exception {
        CommonTests.test(DDValidAppTests.class, "testBasicBindingsConfiguration");
    }
    
    @Test
    public void testSecurityRoleOverrides_Application() throws Exception {
        CommonTests.test(DDValidAppTests.class, "testSecurityRoleOverrides");
    }
    
    @Test
    public void testWebExtensions_Application() throws Exception {
        CommonTests.test(DDValidAppTests.class, "testWebExtensions");
    }
    
    @Test
    public void testWebExtensionsNoBindings_Application() throws Exception {
        CommonTests.test(DDValidAppTests.class, "testWebExtensionsNoBindings");
    }
    
    @Test
    public void testWebBindings_Application() throws Exception {
        CommonTests.test(DDValidAppTests.class, "testWebBindings");
    }
    
    @Test
    public void testWebBindingsNoBindings_Application() throws Exception {
        CommonTests.test(DDValidAppTests.class, "testWebBindingsNoBindings");
    }
    
    @Test
    public void testEJBBindings_Application() throws Exception {
        CommonTests.test(DDValidAppTests.class, "testEJBBindings");
    }
    
    @Test
    public void testEJBExtensions_Application() throws Exception {
        CommonTests.test(DDValidAppTests.class, "testEJBExtensions");
    }
    
    @Test
    public void testEJBBindingsNoBindings_Application() throws Exception {
        CommonTests.test(DDValidAppTests.class, "testEJBBindingsNoBindings");
    }
    
    @Test
    public void testEJBExtensionsNoBindings_Application() throws Exception {
        CommonTests.test(DDValidAppTests.class, "testEJBExtensionsNoBindings");
    }
    
    @Test
    public void testApplicationExtensions_Application() throws Exception {
        CommonTests.test(DDValidAppTests.class, "testApplicationExtensions");
    }
    
    @Test
    public void testManagedBeanBindings_Application() throws Exception {
        CommonTests.test(DDValidAppTests.class, "testManagedBeanBindings");
    }
    
    @Test
    public void testWebserviceBindings_Application() throws Exception {
        CommonTests.test(DDValidAppTests.class, "testWebserviceBindings");
    }
    @Test
    public void testWebserviceBindingsNoBindings_Application() throws Exception {
        CommonTests.test(DDValidAppTests.class, "testWebserviceBindingsNoBindings");
    }
}
