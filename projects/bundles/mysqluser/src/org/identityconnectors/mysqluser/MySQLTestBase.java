/*
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * U.S. Government Rights - Commercial software. Government users 
 * are subject to the Sun Microsystems, Inc. standard license agreement
 * and applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms.
 * 
 * This distribution may include materials developed by third parties.
 * Sun, Sun Microsystems, the Sun logo, Java and Project Identity 
 * Connectors are trademarks or registered trademarks of Sun 
 * Microsystems, Inc. or its subsidiaries in the U.S. and other
 * countries.
 * 
 * UNIX is a registered trademark in the U.S. and other countries,
 * exclusively licensed through X/Open Company, Ltd. 
 * 
 * -----------
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved. 
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License(CDDL) (the License).  You may not use this file
 * except in  compliance with the License. 
 * 
 * You can obtain a copy of the License at
 * http://identityconnectors.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing permissions and 
 * limitations under the License.  
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each
 * file and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * -----------
 */
package org.identityconnectors.mysqluser;

import static org.junit.Assert.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.SQLParam;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.InvalidCredentialException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfoUtil;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EndsWithFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;
import org.identityconnectors.test.common.TestHelpers;
import org.junit.Test;

/**
 * @author Petr Jung
 * @version $Revision 1.0$
 * @since 1.0
 */
public abstract class MySQLTestBase {
    /**
     * Setup logging for the {@link MySQLUserConnector}.
     */
    private static final Log log = Log.getLog(MySQLTestBase.class);
    
    
    protected static String idmDriver = null;
    protected static String idmHost = null;
    protected static String idmUser = null;
    protected static GuardedString testPassword = null;
    protected static String idmModelUser = null;
    protected static GuardedString idmPassword = null;
    protected static String idmPort = null;
    protected static final String TST_USER1 = "test1";
    protected static final String TST_USER2 = "test2";
    protected static final String TST_USER3 = "test3";

    /**
     * Get the configuration
     * @return the initialized configuration
     */
    public abstract MySQLUserConfiguration newConfiguration();


    protected MySQLUserConfiguration config = null;
    protected ConnectorFacade facade = null;

    /**
     * Create not created user and test it was created
     */
    private void quitellyCreateUser(String userName, GuardedString testPassword) {
        PreparedStatement ps = null;
        MySQLUserConnection conn = null;
        ResultSet result = null;
        final List<SQLParam> values = new ArrayList<SQLParam>();
        values.add(new SQLParam("user", userName, Types.VARCHAR));
        values.add(new SQLParam("password", testPassword));
        final String SQL_CREATE_TEMPLATE = "CREATE USER ? IDENTIFIED BY ?";
        log.info("quitelly Create User {0}", userName);
        try {
            conn = MySQLUserConnection.getConnection(newConfiguration());
            ps = conn.prepareStatement(SQL_CREATE_TEMPLATE, values);
            ps.execute();
            conn.commit();
        } catch (SQLException ex) {
            log.info("quitelly Create User {0} has expected exception {1}", userName, ex.getMessage());
            quitellyGrantUssage(userName, testPassword);
        } finally {
            SQLUtil.closeQuietly(result);
            SQLUtil.closeQuietly(ps);
            SQLUtil.closeQuietly(conn);
        }
        testUserFound(userName, true);
        log.ok("quitelly Create User {0}", userName);
    }


    /**
     * Test method for {@link MySQLUserConnector#create(ObjectClass, Set, OperationOptions)}.
     */
    @Test
    public void testCreate() {
        assertNotNull(facade);
        String userName=TST_USER1;
        quitellyDeleteUser(userName);
        final Uid uid = createUser(userName, testPassword);
        assertNotNull(uid);
        assertEquals(userName, uid.getUidValue());
        //Delete it at the end
        quitellyDeleteUser(userName);
    }

    /**
     * Test method for {@link MySQLUserConnector#create(ObjectClass, Set, OperationOptions)}.
     */
    @Test(expected = AlreadyExistsException.class)
    public void testCreateDuplicate() {
        assertNotNull(facade);
        String userName=TST_USER1;
        quitellyDeleteUser(userName);
        final Uid uid = createUser(userName, testPassword);
        assertNotNull(uid);
        assertEquals(userName, uid.getUidValue());
        //duplicate
        try {
            createUser(userName, testPassword);
            fail("Duplicate user created");
        } finally {
            //Delete it at the end        
            quitellyDeleteUser(userName);
        }
    }

    /**
     * Test method for {@link MySQLUserConnector#create(ObjectClass, Set, OperationOptions)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateUnsupported() {
        assertNotNull(facade);
        String userName=TST_USER1;
        quitellyDeleteUser(userName);
        
        Set<Attribute> tuas = getUserAttributeSet(TST_USER1, testPassword);
        assertNotNull(tuas);
        ObjectClass oc = new ObjectClass("UNSUPPORTED");
        facade.create(oc, tuas, null);   
    }

    /**
     * Test method for {@link MySQLUserConnector#update(ObjectClass, Set, OperationOptions)}.
     * Test creating of the connector object, searching using UID and update
     */
    @Test
    public void testCreateAndUpdate() {
        String userName = TST_USER1;
        String newName = TST_USER3;
        assertNotNull(facade);
        //To be sure it is created
        quitellyCreateUser(userName, testPassword);    
        quitellyDeleteUser(newName);    
        
        // retrieve the object      
        Uid uid = new Uid(testUserFound(userName, true)); 
        
        // create updated connector object
        ConnectorObjectBuilder coBuilder = new ConnectorObjectBuilder();
        coBuilder.setName(newName); //Going to change name -> id change follows
        coBuilder.setUid(uid);
        coBuilder.setObjectClass(ObjectClass.ACCOUNT);
        ConnectorObject coBeforeUpdate = coBuilder.build();
        
        // do the update
        Set<Attribute> changeSet = CollectionUtil.newSet(coBeforeUpdate.getAttributes());
        final Uid uidUpdate = facade.update(ObjectClass.ACCOUNT, coBeforeUpdate.getUid(), AttributeUtil.filterUid(changeSet), null);
        
        // uids should be the same
        assertEquals(newName, uidUpdate.getUidValue());
        
        // retrieve the updated object
        // retrieve the object      
        String actual = testUserFound(newName, true); 
        assertEquals(coBeforeUpdate.getName().getNameValue(), actual);
        
        quitellyDeleteUser(TST_USER1); 
        quitellyDeleteUser(TST_USER3);   
    }

    /**
     * Test method for {@link MySQLUserConnector#update(ObjectClass, Set, OperationOptions)}.
     * Test creating of the connector object, searching using UID and update
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateUnsupported() {
        String userName = TST_USER1;
        assertNotNull(facade);
        //To be sure it is created
        quitellyCreateUser(userName, testPassword);    
        // retrieve the object      
        Uid uid = new Uid(testUserFound(userName, true)); 
        // create updated connector object
        ConnectorObjectBuilder coBuilder = new ConnectorObjectBuilder();
        ObjectClass oc = new ObjectClass("UNSUPPORTED");
        coBuilder.setUid(uid);
        coBuilder.setObjectClass(ObjectClass.ACCOUNT);
        ConnectorObject coBeforeUpdate = coBuilder.build();
        
        // do the update
        Set<Attribute> changeSet = CollectionUtil.newSet(coBeforeUpdate.getAttributes());
    
        facade.update(oc, coBeforeUpdate.getUid(), AttributeUtil.filterUid(changeSet), null);
    }

    /**
     * Test method for {@link MySQLUserConnector#delete(ObjectClass, Uid, OperationOptions)}.
     * 
     */
    @Test
    public void testDelete() {
        assertNotNull(facade);
        String userName = TST_USER2;
        //To be sure it is created
        quitellyCreateUser(userName, testPassword);
        // retrieve the object
        testUserFound(userName, true);  
        
        facade.delete(ObjectClass.ACCOUNT, new Uid(TST_USER2), null);
        
        // retrieve the object
        testUserFound(userName, false);  
    }

    /**
     * Test method for {@link MySQLUserConnector#delete(ObjectClass, Uid, OperationOptions)}.
     * 
     */
    @Test(expected = UnknownUidException.class)
    public void testDeleteUnexisting() {
        assertNotNull(facade);  
        facade.delete(ObjectClass.ACCOUNT, new Uid("UNKNOWN"), null); 
    }

    /**
     * test method
     */
    @Test
    public void testTestMethod() {
        ConnectorFacade facade = getFacade();
        facade.test();
    }

    /**
     * Test Find the user model, this must be found for proper functionality
     */
    @SuppressWarnings("synthetic-access")
    @Test
    public void testFindJustOneObject() {
        final Attribute expected = AttributeBuilder.build(Name.NAME, idmModelUser);
        FindUidObjectHandler handler = new FindUidObjectHandler(new Uid(idmModelUser));
        // attempt to find the newly created object..
        facade.search(ObjectClass.ACCOUNT, new EqualsFilter(expected), handler, null);
        assertTrue("The modeluser was not found", handler.found);
        final ConnectorObject actual = handler.getConnectorObject();
        assertNotNull(actual);
        assertEquals("Expected user is not same",idmModelUser, AttributeUtil.getAsStringValue(actual.getName()));
     }

    /**
     * Test Find the user model, this must be found for proper functionality
     */
    @SuppressWarnings("synthetic-access")
    @Test
    public void testFindModelUserByContains() {
        final Attribute expected = AttributeBuilder.build(Name.NAME, idmModelUser);
        FindUidObjectHandler handler = new FindUidObjectHandler(new Uid(idmModelUser));
        // attempt to find the newly created object..
        facade.search(ObjectClass.ACCOUNT, new ContainsFilter(expected), handler, null);
        assertTrue("The modeluser was not found", handler.found);
        final ConnectorObject actual = handler.getConnectorObject();
        assertNotNull(actual);
        assertEquals("Expected user is not same",idmModelUser, AttributeUtil.getAsStringValue(actual.getName()));
     }

    /**
     * Test Find the user model, this must be found for proper functionality
     */
    @SuppressWarnings("synthetic-access")
    @Test
    public void testFindModelUserByEndWith() {
        final Attribute expected = AttributeBuilder.build(Name.NAME, idmModelUser);
        FindUidObjectHandler handler = new FindUidObjectHandler(new Uid(idmModelUser));
        // attempt to find the newly created object..
        facade.search(ObjectClass.ACCOUNT, new EndsWithFilter(expected), handler, null);
        assertTrue("The modeluser was not found", handler.found);
        final ConnectorObject actual = handler.getConnectorObject();
        assertNotNull(actual);
        assertEquals("Expected user is not same",idmModelUser, AttributeUtil.getAsStringValue(actual.getName()));
     }

    /**
     * Test Find the user model, this must be found for proper functionality
     */
    @SuppressWarnings("synthetic-access")
    @Test
    public void testFindModelUserByStartWith() {
        final Attribute expected = AttributeBuilder.build(Name.NAME, idmModelUser);
        FindUidObjectHandler handler = new FindUidObjectHandler(new Uid(idmModelUser));
        // attempt to find the newly created object..
        facade.search(ObjectClass.ACCOUNT, new StartsWithFilter(expected), handler, null);
        assertTrue("The modeluser was not found", handler.found);
        final ConnectorObject actual = handler.getConnectorObject();
        assertNotNull(actual);
        assertEquals("Expected user is not same",idmModelUser, AttributeUtil.getAsStringValue(actual.getName()));
     }

    /**
     * Test Find the user model, this must be found for proper functionality
     */
    @SuppressWarnings("synthetic-access")
    @Test
    public void testFindModelUserByUid() {
        final Uid expected = new Uid(config.getUsermodel());
        FindUidObjectHandler handler = new FindUidObjectHandler(expected);
        // attempt to find the newly created object..
        facade.search(ObjectClass.ACCOUNT, new EqualsFilter(expected), handler, null);
        assertTrue("The modeluser was not found", handler.found);
        final Uid actual = handler.getUid();
        assertNotNull(actual);
        assertTrue(actual.is(expected.getName()));  
     }


    /**
     * Test method 
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConfigurationUsermodel() throws Exception {
        config.setUsermodel("");
        config.validate();
    }

    /**
     * Test method 
     * @throws Exception
     */
    @Test()
    public void testValidateConfiguration() throws Exception {
        config.validate();
    }

    /**
     * Test method
     * 
     * @throws Exception
     */
    @Test()
    public void testValidateConfigurationDatasource() throws Exception {
        config.setPort("");
        config.setDriver("");
        config.setDatasource("java:");
        config.validate();
    
        final String[] tstProp = {"a=a","b=b"};
        config.setJndiProperties(tstProp);
        assertEquals(tstProp[0], config.getJndiProperties()[0]);
        assertEquals(tstProp[1], config.getJndiProperties()[1]);
        config.validate();
    }

    /**
     * Test method for {@link MySQLUserConnector#authenticate(username, password, options)}.
     * Test creating of the connector object, searching using UID and update
     */
    @Test
    public void testAuthenticateOriginal() {
        String userName = TST_USER1;
        assertNotNull(facade);
        //To be sure it is created
        quitellyCreateUser(userName, testPassword);    
        
        // test user created
        testUserFound(userName, true);
        
        final Uid uid = facade.authenticate(ObjectClass.ACCOUNT, userName, testPassword, null);
        assertEquals(userName, uid.getUidValue());
        quitellyDeleteUser(userName); 
    }

    /**
     * Test method for {@link MySQLUserConnector#authenticate(String, GuardedString, OperationOptions)}.
     * Test creating of the connector object, searching using UID and update
     */
    @Test(expected = InvalidCredentialException.class)
    public void testAuthenticateWrongOriginal() {
        String userName = TST_USER1;
        assertNotNull(facade);
        //To be sure it is created
        quitellyCreateUser(userName, testPassword);
        
        // retrieve the object
        testUserFound(userName, true);
        try {
            facade.authenticate(ObjectClass.ACCOUNT, userName, new GuardedString("blaf".toCharArray()), null);
        } finally {
            quitellyDeleteUser(userName);
        }
    }

    /**
     * Test method for {@link MySQLUserConnector#update(ObjectClass, Set, OperationOptions)}.
     * Test creating of the connector object, searching using UID and update
     */
    @Test
    public void testCreateUpdateAutenticate() {
        final String NEWPWD = "newvalue";        
        String userName = TST_USER1;
        assertNotNull(facade);
        //To be sure it is created
        quitellyCreateUser(userName, testPassword);
        
        // retrieve the object      
        Uid uid = new Uid(testUserFound(userName, true)); 
        
        // create updated connector object
        ConnectorObjectBuilder coBuilder = new ConnectorObjectBuilder();
        coBuilder.setObjectClass(ObjectClass.ACCOUNT);
        coBuilder.addAttribute(AttributeBuilder.buildPassword(NEWPWD.toCharArray()));
        coBuilder.setName(userName);
        coBuilder.setUid(uid);
        ConnectorObject coUpdate = coBuilder.build();
        
        // do the update
        final Uid uidUpdate = facade.update(coUpdate.getObjectClass() , coUpdate.getUid(), AttributeUtil.filterUid(coUpdate.getAttributes()), null);
        
        // uids should be the same
        assertEquals(userName, uidUpdate.getUidValue());
        
        facade.authenticate(ObjectClass.ACCOUNT, userName, new GuardedString(NEWPWD.toCharArray()), null);
        
        quitellyDeleteUser(TST_USER1);   
    }

    /**
     * Test method for {@link MySQLUserConnector#schema()}.
     */
    @Test
    public void testSchemaApi() {
        Schema schema = facade.schema();
        // Schema should not be null
        assertNotNull(schema);
        Set<ObjectClassInfo> objectInfos = schema.getObjectClassInfo();
        assertNotNull(objectInfos);
        assertEquals(1, objectInfos.size());
        ObjectClassInfo objectInfo = (ObjectClassInfo) objectInfos.toArray()[0];
        assertNotNull(objectInfo);
        // the object class has to ACCOUNT_NAME
        assertTrue(objectInfo.is(ObjectClass.ACCOUNT_NAME));
        // iterate through AttributeInfo Set
        Set<AttributeInfo> attInfos = objectInfo.getAttributeInfo();
        
        assertNotNull(AttributeInfoUtil.find(Name.NAME, attInfos));
        assertNotNull(AttributeInfoUtil.find(OperationalAttributes.PASSWORD_NAME, attInfos));
    }

    /**
     * Create not created user and test it was created
     */
    private void quitellyGrantUssage(String userName, GuardedString testPassword) {
        PreparedStatement ps = null;
        MySQLUserConnection conn = null;
        ResultSet result = null;
        final List<SQLParam> values = new ArrayList<SQLParam>();
        values.add(new SQLParam("user", userName, Types.VARCHAR));
        values.add(new SQLParam("password", testPassword));
        final String SQL_GRANT_USSAGE = "GRANT USAGE ON *.* TO ?@'localhost' IDENTIFIED BY ?";
        log.info("quitelly Grant Ussage to {0}", userName);
        try {
            conn = MySQLUserConnection.getConnection(newConfiguration());
            ps = conn.prepareStatement(SQL_GRANT_USSAGE, values);
            ps.execute();
            conn.commit();
        } catch (SQLException ex) {
            log.info("quitelly Grant Ussage to {0} has expected exception {1}", userName, ex.getMessage());
            
        } finally {
            SQLUtil.closeQuietly(result);
            SQLUtil.closeQuietly(ps);
            SQLUtil.closeQuietly(conn);
        }
        testUserFound(userName, true);
        log.ok("quitelly Grant Ussage to {0}", userName);
    }

    /**
     * Delete not deleted User and test it was deleted
     */
    protected void quitellyDeleteUser(String userName) {
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        MySQLUserConnection conn = null;
        final List<SQLParam> values = new ArrayList<SQLParam>();
        values.add(new SQLParam("user", userName, Types.VARCHAR));
        final String SQL_DELETE_TEMPLATE = "DROP USER ?";
        final String SQL_DELETE_TEMPLATE_LOCAL = "DROP USER ?@'localhost'";
        log.info("quitelly Delete User {0}", userName);
        conn = MySQLUserConnection.getConnection(newConfiguration());
        try {
            ps1 = conn.prepareStatement(SQL_DELETE_TEMPLATE, values);
            ps1.execute();
            conn.commit();
        } catch (SQLException ex) {
            log.info("quitelly Delete User {0} has expected exception {1}", userName, ex.getMessage());
            quitellyDeleteUser41(userName);
        } finally {
            SQLUtil.closeQuietly(ps1);
        }
        try {
            ps2 = conn.prepareStatement(SQL_DELETE_TEMPLATE_LOCAL, values);
            ps2.execute();
            conn.commit();
        } catch (SQLException ex) {
            log.info("quitelly Delete User {0} has expected exception {1}", userName, ex.getMessage());
        } finally {
            SQLUtil.closeQuietly(ps2);
            SQLUtil.closeQuietly(conn);
        }        
        testUserFound(userName, false);
        log.ok("quitelly Delete User {0}", userName);
    }

    /**
     * Delete user on MySQL41 resource
     */
    private void quitellyDeleteUser41(final String userName) {
        final String SQL_DELETE_USERS="DELETE FROM user WHERE User=?";
        final String SQL_DELETE_DB="DELETE FROM db WHERE User=?";
        final String SQL_DELETE_TABLES="DELETE FROM tables_priv WHERE User=?";
        final String SQL_DELETE_COLUMNS="DELETE FROM columns_priv WHERE User=?";
    
        MySQLUserConnection conn = MySQLUserConnection.getConnection(newConfiguration());
    
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        PreparedStatement ps3 = null;
        PreparedStatement ps4 = null;
        try {
            // created, read the model user grants
            ps1 = conn.getConnection().prepareStatement(SQL_DELETE_USERS);
            ps1.setString(1, userName);
            ps1.execute();
            ps2 = conn.getConnection().prepareStatement(SQL_DELETE_DB);
            ps2.setString(1, userName);
            ps2.execute();
            ps3 = conn.getConnection().prepareStatement(SQL_DELETE_TABLES);
            ps3.setString(1, userName);
            ps3.execute();
            ps4 = conn.getConnection().prepareStatement(SQL_DELETE_COLUMNS);
            ps4.setString(1, userName);
            ps4.execute();
        } catch (SQLException e) {
            log.error(e, "expected delete user 41 error");
        } finally {
            // clean up..
            SQLUtil.closeQuietly(ps1);
            SQLUtil.closeQuietly(ps2);
            SQLUtil.closeQuietly(ps3);
            SQLUtil.closeQuietly(ps4);
        }
        log.ok("Deleted Uid: {0}", userName);
    }

    /**
     * 
     */
    protected void createTestModelUser(final String modelUser, GuardedString testPassword) {
        log.info("create model user {0}", modelUser);
        quitellyCreateUser(modelUser, testPassword);
        createUserGrants(modelUser, testPassword);
    }

    /**
     * 
     */
    private void createUserGrants(final String userName, GuardedString testPassword) {
        final String SQL1 = "GRANT SELECT, INSERT, UPDATE, DELETE ON *.* TO ?@'%' IDENTIFIED BY ?";
        final String SQL2 = "GRANT SELECT, INSERT, UPDATE, DELETE ON *.* TO ?@'localhost' IDENTIFIED BY ?";
        final String SQL3 = "GRANT CREATE, DROP ON `mysql`.* TO ?@'%'";
        final String SQL4 = "GRANT ALL PRIVILEGES ON `test`.* TO ?@'%'";
        final String SQL5 = "GRANT CREATE, DROP ON `mysql`.* TO ?@'localhost'";
        final String SQL6 = "GRANT ALL PRIVILEGES ON `test`.* TO ?@'localhost'";
        final String[] stmts = { SQL1, SQL2, SQL3, SQL4, SQL5, SQL6 };
        log.info("Creating the Test Model User {0}", userName);
        PreparedStatement ps = null;
        MySQLUserConnection conn = null;
        String sql = null;    
        try {
            for (int i = 0; i < stmts.length; i++) {                
                sql = stmts[i];
                final List<SQLParam> values = new ArrayList<SQLParam>();
                values.add(new SQLParam("user", userName, Types.VARCHAR));
                if(sql.contains("IDENTIFIED BY ?")) {
                    values.add(new SQLParam("password", testPassword));
                }
                log.info("Create User {0} Grants , statement:{1}", userName, sql);
                conn = MySQLUserConnection.getConnection(newConfiguration());
                ps = conn.prepareStatement(sql,values);
                ps.execute();
            }
            conn.commit();
        } catch (SQLException ex) {
            log.error(ex, "Fail to create User {0} Grants , statement:{1}", userName, sql);
            fail(ex.getMessage());
        } finally {
            SQLUtil.closeQuietly(ps);
            SQLUtil.closeQuietly(conn);
        }
        testUserFound(userName, true);
        log.ok("The User {0} Grants created", userName);
    }

    private static Set<Attribute> getUserAttributeSet(String tstUser, GuardedString tstPassword) {
        Set<Attribute> ret = new HashSet<Attribute>();
        ret.add(AttributeBuilder.build(Name.NAME, tstUser));
        ret.add(AttributeBuilder.buildPassword(tstPassword));
        return ret;
    }

    /**
     * @param userName
     */
    private String testUserFound(String userName, boolean found) {
        String ret = null;
    
        // update the last change
        PreparedStatement ps = null;
        MySQLUserConnection conn = null;
        ResultSet result = null;
        final List<SQLParam> values = new ArrayList<SQLParam>();
        values.add(new SQLParam("user", userName, Types.VARCHAR));
        final String SQL_SELECT = "SELECT DISTINCT user FROM mysql.user WHERE user = ?";               
        log.info("test User {0} found {1} ", userName, found);   
        try {
            conn  =  MySQLUserConnection.getConnection(newConfiguration());
            ps = conn.prepareStatement(SQL_SELECT, values);
            result = ps.executeQuery();
            if(result.next()) {
                ret = result.getString(1);
            }
            conn.commit();
        } catch (SQLException ex) {
            log.error(ex,"test User {0} found {1} ", userName, found);   
        } finally {
            SQLUtil.closeQuietly(result);
            SQLUtil.closeQuietly(ps);
            SQLUtil.closeQuietly(conn);
        }
        if(found) {
            assertNotNull("The object for "+userName+" is null", ret);
        } else {
            assertNull("The object for "+userName+" is not null", ret);
        }
        log.ok("test User {0} found {1} ", userName, found);   
        return ret;
    }

    /**
         * Test internal implementation for finding the objects
         * @author Petr Jung
         */
        protected static class FindUidObjectHandler implements ResultsHandler {
    
            private ConnectorObject connectorObject = null;
            
            private boolean found = false;
            
            private final Uid uid;
            
            /**
             * @param uid
             */
            public FindUidObjectHandler(Uid uid) {
                this.uid = uid;
            }
            
            /**
             * getter method
             * @return object value
             */
            public ConnectorObject getConnectorObject() {
                return connectorObject;
            }
            
            /**
             * @return the uid
             */
            public Uid getUid() {
                return uid;
            }
    
            public boolean handle(ConnectorObject obj) {
                if (obj.getUid().equals(uid)) {
                    if(found) {
                        throw new IllegalStateException("Duplicate object found");
                    }
                    found = true;
                    this.connectorObject = obj;
                }
                return true;
            }
    
            /**
             * @return the found
             */
            public boolean isFound() {
                return found;
            }
    
            /**
             * @param found the found to set
             */
            public void setFound(boolean found) {
                this.found = found;
            }
    
        }

    protected Uid createUser(String user, GuardedString password) {
        Set<Attribute> tuas = getUserAttributeSet(user, password);
        assertNotNull(tuas);
        return facade.create(ObjectClass.ACCOUNT, tuas, null);
    }

    protected ConnectorFacade getFacade() {
        ConnectorFacadeFactory factory = ConnectorFacadeFactory.getInstance();
        // **test only**
        APIConfiguration impl = TestHelpers.createTestConfiguration(MySQLUserConnector.class, config);
        return factory.newInstance(impl);
    }

    /**
     * 
     */
    public MySQLTestBase() {
        super();
    }

}