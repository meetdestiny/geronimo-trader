/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.geronimo.j2ee.deployment.annotation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.EJBs;
import javax.jws.HandlerChain;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContexts;
import javax.persistence.PersistenceUnit;
import javax.persistence.PersistenceUnits;
import javax.xml.ws.WebServiceRef;
import javax.xml.ws.WebServiceRefs;

import org.apache.geronimo.kernel.config.Configuration;
import org.apache.geronimo.testsupport.XmlBeansTestSupport;
import org.apache.geronimo.xbeans.javaee.WebAppDocument;
import org.apache.geronimo.xbeans.javaee.WebAppType;
import org.apache.xbean.finder.ClassFinder;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;

/**
 * Testcases for each of the various AnnotationHelper class
 */
public class AnnotationHelperTest extends XmlBeansTestSupport {

    private Class[] classes = {EJBAnnotationTest.class, HandlerChainAnnotationTest.class,
        PersistenceContextAnnotationTest.class, PersistenceUnitAnnotationTest.class,
        WebServiceRefAnnotationTest.class, SecurityAnnotationTest.class};

    private ClassFinder classFinder = new ClassFinder(classes);
    private ClassLoader classLoader = this.getClass().getClassLoader();
    private XmlOptions options = new XmlOptions();


    public void testEJBAnnotationHelper() throws Exception {

        //-------------------------------------------------
        // Ensure annotations are discovered correctly
        //-------------------------------------------------
        List<Class> annotatedClasses = classFinder.findAnnotatedClasses(EJBs.class);
        assertNotNull(annotatedClasses);
        assertEquals(1, annotatedClasses.size());
        assertTrue(annotatedClasses.contains(EJBAnnotationTest.class));

        List<Method> annotatedMethods = classFinder.findAnnotatedMethods(EJB.class);
        assertNotNull(annotatedMethods);
        assertEquals(2, annotatedMethods.size());
        assertTrue(annotatedMethods.contains(EJBAnnotationTest.class.getDeclaredMethod("setAnnotatedMethod1", new Class[]{int.class})));
        assertTrue(annotatedMethods.contains(EJBAnnotationTest.class.getDeclaredMethod("setAnnotatedMethod2", new Class[]{String.class})));

        List<Field> annotatedFields = classFinder.findAnnotatedFields(EJB.class);
        assertNotNull(annotatedFields);
        assertEquals(2, annotatedFields.size());
        assertTrue(annotatedFields.contains(EJBAnnotationTest.class.getDeclaredField("annotatedField1")));
        assertTrue(annotatedFields.contains(EJBAnnotationTest.class.getDeclaredField("annotatedField2")));

        //-------------------------------------------------
        // Ensure annotations are processed correctly
        //-------------------------------------------------
        URL srcXML = classLoader.getResource("annotation/empty-web-src.xml");
        XmlObject xmlObject = XmlObject.Factory.parse(srcXML, options);
        WebAppDocument webAppDoc = (WebAppDocument) xmlObject.changeType(WebAppDocument.type);
        WebAppType webApp = webAppDoc.getWebApp();
        AnnotatedWebApp annotatedWebApp = new AnnotatedWebApp(webApp);
        EJBAnnotationHelper.processAnnotations(annotatedWebApp, classFinder);
        URL expectedXML = classLoader.getResource("annotation/ejb-expected.xml");
        XmlObject expected = XmlObject.Factory.parse(expectedXML);
        log.debug("[@EJB Source XML] " + '\n' + webApp.toString() + '\n');
        log.debug("[@EJB Expected XML]" + '\n' + expected.toString() + '\n');
        List problems = new ArrayList();
        boolean ok = compareXmlObjects(webApp, expected, problems);
        assertTrue("Differences: " + problems, ok);
    }


    public void testHandlerChainAnnotationHelper() throws Exception {

        //-------------------------------------------------
        // Ensure annotations are discovered correctly
        //-------------------------------------------------
        List<Class> annotatedClasses = classFinder.findAnnotatedClasses(HandlerChain.class);
        assertNotNull(annotatedClasses);
        assertEquals(1, annotatedClasses.size());
        assertTrue(annotatedClasses.contains(HandlerChainAnnotationTest.class));

        List<Method> annotatedMethods = classFinder.findAnnotatedMethods(HandlerChain.class);
        assertNotNull(annotatedMethods);
        assertEquals(2, annotatedMethods.size());
        assertTrue(annotatedMethods.contains(HandlerChainAnnotationTest.class.getDeclaredMethod("setAnnotatedMethod1", new Class[]{String.class})));
        assertTrue(annotatedMethods.contains(HandlerChainAnnotationTest.class.getDeclaredMethod("setAnnotatedMethod2", new Class[]{int.class})));

        List<Field> annotatedFields = classFinder.findAnnotatedFields(HandlerChain.class);
        assertNotNull(annotatedFields);
        assertEquals(2, annotatedFields.size());
        assertTrue(annotatedFields.contains(HandlerChainAnnotationTest.class.getDeclaredField("annotatedField1")));
        assertTrue(annotatedFields.contains(HandlerChainAnnotationTest.class.getDeclaredField("annotatedField2")));

        //-------------------------------------------------
        // Ensure annotations are processed correctly
        //-------------------------------------------------
        URL srcXML = classLoader.getResource("annotation/handler-chain-src.xml");
        XmlObject xmlObject = XmlObject.Factory.parse(srcXML, options);
        WebAppDocument webAppDoc = (WebAppDocument) xmlObject.changeType(WebAppDocument.type);
        WebAppType webApp = webAppDoc.getWebApp();
        AnnotatedWebApp annotatedWebApp = new AnnotatedWebApp(webApp);
        HandlerChainAnnotationHelper.processAnnotations(annotatedWebApp, classFinder);
        URL expectedXML = classLoader.getResource("annotation/handler-chain-expected.xml");
        XmlObject expected = XmlObject.Factory.parse(expectedXML);
        log.debug("[@HandlerChain Source XML] " + '\n' + webApp.toString() + '\n');
        log.debug("[@HandlerChain Expected XML]" + '\n' + expected.toString() + '\n');
        List problems = new ArrayList();
        boolean ok = compareXmlObjects(webApp, expected, problems);
        assertTrue("Differences: " + problems, ok);
    }


    public void testPersistenceContextAnnotationHelper() throws Exception {

        //-------------------------------------------------
        // Ensure annotations are discovered correctly
        //-------------------------------------------------
        List<Class> annotatedClasses = classFinder.findAnnotatedClasses(PersistenceContexts.class);
        assertNotNull(annotatedClasses);
        assertEquals(1, annotatedClasses.size());
        assertTrue(annotatedClasses.contains(PersistenceContextAnnotationTest.class));

        List<Method> annotatedMethods = classFinder.findAnnotatedMethods(PersistenceContext.class);
        assertNotNull(annotatedMethods);
        assertEquals(2, annotatedMethods.size());
        assertTrue(annotatedMethods.contains(PersistenceContextAnnotationTest.class.getDeclaredMethod("setAnnotatedMethod1", new Class[]{String.class})));
        assertTrue(annotatedMethods.contains(PersistenceContextAnnotationTest.class.getDeclaredMethod("setAnnotatedMethod2", new Class[]{String.class})));

        List<Field> annotatedFields = classFinder.findAnnotatedFields(PersistenceContext.class);
        assertNotNull(annotatedFields);
        assertEquals(2, annotatedFields.size());
        assertTrue(annotatedFields.contains(PersistenceContextAnnotationTest.class.getDeclaredField("annotatedField1")));
        assertTrue(annotatedFields.contains(PersistenceContextAnnotationTest.class.getDeclaredField("annotatedField2")));

        //-------------------------------------------------
        // Ensure annotations are processed correctly
        //-------------------------------------------------
        URL srcXML = classLoader.getResource("annotation/empty-web-src.xml");
        XmlObject xmlObject = XmlObject.Factory.parse(srcXML, options);
        WebAppDocument webAppDoc = (WebAppDocument) xmlObject.changeType(WebAppDocument.type);
        WebAppType webApp = webAppDoc.getWebApp();
        AnnotatedWebApp annotatedWebApp = new AnnotatedWebApp(webApp);
        PersistenceContextAnnotationHelper.processAnnotations(annotatedWebApp, classFinder);
        URL expectedXML = classLoader.getResource("annotation/persistence-context-expected.xml");
        XmlObject expected = XmlObject.Factory.parse(expectedXML);
        log.debug("[@PersistenceContext Source XML] " + '\n' + webApp.toString() + '\n');
        log.debug("[@PersistenceContext Expected XML]" + '\n' + expected.toString() + '\n');
        List problems = new ArrayList();
        boolean ok = compareXmlObjects(webApp, expected, problems);
        assertTrue("Differences: " + problems, ok);
    }


    public void testPersistenceUnitAnnotationHelper() throws Exception {

        List<Class> annotatedClasses = classFinder.findAnnotatedClasses(PersistenceUnits.class);
        assertNotNull(annotatedClasses);
        assertEquals(1, annotatedClasses.size());
        assertTrue(annotatedClasses.contains(PersistenceUnitAnnotationTest.class));

        List<Method> annotatedMethods = classFinder.findAnnotatedMethods(PersistenceUnit.class);
        assertNotNull(annotatedMethods);
        assertEquals(2, annotatedMethods.size());
        assertTrue(annotatedMethods.contains(PersistenceUnitAnnotationTest.class.getDeclaredMethod("setAnnotatedMethod1", new Class[]{int.class})));
        assertTrue(annotatedMethods.contains(PersistenceUnitAnnotationTest.class.getDeclaredMethod("setAnnotatedMethod2", new Class[]{boolean.class})));

        List<Field> annotatedFields = classFinder.findAnnotatedFields(PersistenceUnit.class);
        assertNotNull(annotatedFields);
        assertEquals(2, annotatedFields.size());
        assertTrue(annotatedFields.contains(PersistenceUnitAnnotationTest.class.getDeclaredField("annotatedField1")));
        assertTrue(annotatedFields.contains(PersistenceUnitAnnotationTest.class.getDeclaredField("annotatedField2")));

        //-------------------------------------------------
        // Ensure annotations are processed correctly
        //-------------------------------------------------
        URL srcXML = classLoader.getResource("annotation/empty-web-src.xml");
        XmlObject xmlObject = XmlObject.Factory.parse(srcXML, options);
        WebAppDocument webAppDoc = (WebAppDocument) xmlObject.changeType(WebAppDocument.type);
        WebAppType webApp = webAppDoc.getWebApp();
        AnnotatedWebApp annotatedWebApp = new AnnotatedWebApp(webApp);
        PersistenceUnitAnnotationHelper.processAnnotations(annotatedWebApp, classFinder);
        URL expectedXML = classLoader.getResource("annotation/persistence-unit-expected.xml");
        XmlObject expected = XmlObject.Factory.parse(expectedXML);
        log.debug("[@PersistenceUnit Source XML] " + '\n' + webApp.toString() + '\n');
        log.debug("[@PersistenceUnit Expected XML]" + '\n' + expected.toString() + '\n');
        List problems = new ArrayList();
        boolean ok = compareXmlObjects(webApp, expected, problems);
        assertTrue("Differences: " + problems, ok);
    }


    public void testWebServiceRefAnnotationHelper() throws Exception {

        //-------------------------------------------------
        // Ensure annotations are discovered correctly
        //-------------------------------------------------
        List<Class> annotatedClasses = classFinder.findAnnotatedClasses(WebServiceRefs.class);
        assertNotNull(annotatedClasses);
        assertEquals(1, annotatedClasses.size());
        assertTrue(annotatedClasses.contains(WebServiceRefAnnotationTest.class));

        List<Method> annotatedMethods = classFinder.findAnnotatedMethods(WebServiceRef.class);
        assertNotNull(annotatedMethods);
        assertEquals(4, annotatedMethods.size());
        assertTrue(annotatedMethods.contains(WebServiceRefAnnotationTest.class.getDeclaredMethod("setAnnotatedMethod1", new Class[]{boolean.class})));
        assertTrue(annotatedMethods.contains(WebServiceRefAnnotationTest.class.getDeclaredMethod("setAnnotatedMethod2", new Class[]{String.class})));
        assertTrue(annotatedMethods.contains(HandlerChainAnnotationTest.class.getDeclaredMethod("setAnnotatedMethod1", new Class[]{String.class})));
        assertTrue(annotatedMethods.contains(HandlerChainAnnotationTest.class.getDeclaredMethod("setAnnotatedMethod2", new Class[]{int.class})));

        List<Field> annotatedFields = classFinder.findAnnotatedFields(WebServiceRef.class);
        assertNotNull(annotatedFields);
        assertEquals(4, annotatedFields.size());
        assertTrue(annotatedFields.contains(WebServiceRefAnnotationTest.class.getDeclaredField("annotatedField1")));
        assertTrue(annotatedFields.contains(WebServiceRefAnnotationTest.class.getDeclaredField("annotatedField2")));
        assertTrue(annotatedFields.contains(HandlerChainAnnotationTest.class.getDeclaredField("annotatedField1")));
        assertTrue(annotatedFields.contains(HandlerChainAnnotationTest.class.getDeclaredField("annotatedField2")));

        //-------------------------------------------------
        // Ensure annotations are processed correctly
        //-------------------------------------------------
        URL srcXML = classLoader.getResource("annotation/empty-web-src.xml");
        XmlObject xmlObject = XmlObject.Factory.parse(srcXML, options);
        WebAppDocument webAppDoc = (WebAppDocument) xmlObject.changeType(WebAppDocument.type);
        WebAppType webApp = webAppDoc.getWebApp();
        AnnotatedWebApp annotatedWebApp = new AnnotatedWebApp(webApp);
        WebServiceRefAnnotationHelper.processAnnotations(annotatedWebApp, classFinder);
        URL expectedXML = classLoader.getResource("annotation/webservice-ref-expected.xml");
        XmlObject expected = XmlObject.Factory.parse(expectedXML);
        log.debug("[@WebServiceRef Source XML] " + '\n' + webApp.toString() + '\n');
        log.debug("[@WebServiceRef Expected XML]" + '\n' + expected.toString() + '\n');
        List problems = new ArrayList();
        boolean ok = compareXmlObjects(webApp, expected, problems);
        assertTrue("Differences: " + problems, ok);
    }


    public void testSecurityAnnotationHelper() throws Exception {

        //-------------------------------------------------
        // Ensure annotations are discovered correctly
        //-------------------------------------------------
        List<Class> annotatedClasses = classFinder.findAnnotatedClasses(DeclareRoles.class);
        assertNotNull(annotatedClasses);
        assertEquals(1, annotatedClasses.size());
        assertTrue(annotatedClasses.contains(SecurityAnnotationTest.class));

        annotatedClasses.clear();
        annotatedClasses = classFinder.findAnnotatedClasses(RunAs.class);
        assertNotNull(annotatedClasses);
        assertEquals(1, annotatedClasses.size());
        assertTrue(annotatedClasses.contains(SecurityAnnotationTest.class));

        //-------------------------------------------------
        // Ensure annotations are processed correctly
        //-------------------------------------------------
        URL srcXML = classLoader.getResource("annotation/empty-web-src.xml");
        XmlObject xmlObject = XmlObject.Factory.parse(srcXML, options);
        WebAppDocument webAppDoc = (WebAppDocument) xmlObject.changeType(WebAppDocument.type);
        WebAppType webApp = webAppDoc.getWebApp();
        SecurityAnnotationHelper.processAnnotations(webApp, classFinder);
        URL expectedXML = classLoader.getResource("annotation/security-expected.xml");
        XmlObject expected = XmlObject.Factory.parse(expectedXML);
        log.debug("[Security Source XML] " + '\n' + webApp.toString() + '\n');
        log.debug("[Security Expected XML]" + '\n' + expected.toString() + '\n');
        List problems = new ArrayList();
        boolean ok = compareXmlObjects(webApp, expected, problems);
        assertTrue("Differences: " + problems, ok);

        srcXML = classLoader.getResource("annotation/security-src.xml");
        xmlObject = XmlObject.Factory.parse(srcXML, options);
        webAppDoc = (WebAppDocument) xmlObject.changeType(WebAppDocument.type);
        webApp = webAppDoc.getWebApp();
        SecurityAnnotationHelper.processAnnotations(webApp, classFinder);
        expectedXML = classLoader.getResource("annotation/security-expected-1.xml");
        expected = XmlObject.Factory.parse(expectedXML);
        log.debug("[Security Source XML] " + '\n' + webApp.toString() + '\n');
        log.debug("[Security Expected XML]" + '\n' + expected.toString() + '\n');
        problems = new ArrayList();
        ok = compareXmlObjects(webApp, expected, problems);
        assertTrue("Differences: " + problems, ok);
    }
}
