/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (the "License").  You may not use this file
 * except in compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://opensource.org/licenses/cddl1.php
 * See the License for the specific language governing permissions and limitations
 * under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://opensource.org/licenses/cddl1.php.
 * If applicable, add the following below this CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.identityconnectors.oracleerp;

import java.util.HashSet;
import java.util.Set;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.InvalidCredentialException;
import org.identityconnectors.framework.common.exceptions.InvalidPasswordException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

/**
 * Attempts to test the {@link OracleERPConnector} with the framework.
 *
 * @author petr
 * @since 1.0
 */
@Test(groups = { "integration" })
public class AccountOperationAutenticateTests extends OracleERPTestsBase {
    /**
     * Test method .
     */
    @Test
    public void testAuthenticate() {
        final OracleERPConnector c = getConnector(CONFIG_TST);

        final Set<Attribute> attrs = getAttributeSet(ACCOUNT_REQUIRED_ATTRS);
        replaceNameByRandom(attrs);
        final GuardedString password =
                AttributeUtil.getGuardedStringValue(AttributeUtil.find(
                        OperationalAttributes.PASSWORD_NAME, attrs));
        final Uid uid = c.create(ObjectClass.ACCOUNT, attrs, null);
        AssertJUnit.assertNotNull(uid);

        AssertJUnit.assertEquals(uid, c.authenticate(ObjectClass.ACCOUNT, uid.getUidValue(),
                password, null));
    }

    /**
     * Test method .
     */
    @Test
    public void testResolveUsername() {
        final OracleERPConnector c = getConnector(CONFIG_TST);

        final Set<Attribute> attrs = getAttributeSet(ACCOUNT_REQUIRED_ATTRS);
        replaceNameByRandom(attrs);
        final Uid uid = c.create(ObjectClass.ACCOUNT, attrs, null);
        AssertJUnit.assertNotNull(uid);

        AssertJUnit.assertEquals(uid, c.resolveUsername(ObjectClass.ACCOUNT, uid.getUidValue(),
                null));
    }

    /**
     * Test method .
     */
    @Test(expectedExceptions = InvalidCredentialException.class)
    public void testAuthenticateDisabled() {
        final OracleERPConnector c = getConnector(CONFIG_TST);

        final Set<Attribute> attrs = getAttributeSet(ACCOUNT_REQUIRED_ATTRS);
        replaceNameByRandom(attrs);
        final GuardedString password =
                AttributeUtil.getGuardedStringValue(AttributeUtil.find(
                        OperationalAttributes.PASSWORD_NAME, attrs));
        Uid uid = c.create(ObjectClass.ACCOUNT, attrs, null);
        AssertJUnit.assertNotNull(uid);

        AssertJUnit.assertEquals(uid, c.authenticate(ObjectClass.ACCOUNT, uid.getUidValue(),
                password, null));

        // Dissable
        final Set<Attribute> update = new HashSet<Attribute>();
        update.add(uid);
        update.add(AttributeBuilder.buildEnabled(false));
        uid = c.update(ObjectClass.ACCOUNT, uid, update, null);
        AssertJUnit.assertNotNull(uid);

        c.authenticate(ObjectClass.ACCOUNT, uid.getUidValue(), password, null);
    }

    /**
     * Test method .
     */
    @Test(expectedExceptions = InvalidPasswordException.class)
    public void testAuthenticateExpired() {
        final OracleERPConnector c = getConnector(CONFIG_TST);

        final Set<Attribute> attrs = getAttributeSet(ACCOUNT_REQUIRED_ATTRS);
        replaceNameByRandom(attrs);
        final GuardedString password =
                AttributeUtil.getGuardedStringValue(AttributeUtil.find(
                        OperationalAttributes.PASSWORD_NAME, attrs));
        Uid uid = c.create(ObjectClass.ACCOUNT, attrs, null);
        AssertJUnit.assertNotNull(uid);

        AssertJUnit.assertEquals(uid, c.authenticate(ObjectClass.ACCOUNT, uid.getUidValue(),
                password, null));

        // Dissable
        final Set<Attribute> update = new HashSet<Attribute>();
        update.add(uid);
        update.add(AttributeBuilder.buildPasswordExpired(true));
        uid = c.update(ObjectClass.ACCOUNT, uid, update, null);
        AssertJUnit.assertNotNull(uid);

        c.authenticate(ObjectClass.ACCOUNT, uid.getUidValue(), password, null);
    }
}
