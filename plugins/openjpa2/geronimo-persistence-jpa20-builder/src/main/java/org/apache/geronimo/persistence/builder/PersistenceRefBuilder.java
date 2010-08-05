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
package org.apache.geronimo.persistence.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import org.apache.geronimo.common.DeploymentException;
import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.gbean.AbstractNameQuery;
import org.apache.geronimo.gbean.GBeanData;
import org.apache.geronimo.gbean.annotation.GBean;
import org.apache.geronimo.gbean.annotation.ParamAttribute;
import org.apache.geronimo.j2ee.deployment.EARContext;
import org.apache.geronimo.j2ee.deployment.Module;
import org.apache.geronimo.j2ee.deployment.annotation.PersistenceContextAnnotationHelper;
import org.apache.geronimo.j2ee.deployment.annotation.PersistenceUnitAnnotationHelper;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;
import org.apache.geronimo.kernel.GBeanNotFoundException;
import org.apache.geronimo.kernel.Naming;
import org.apache.geronimo.kernel.config.Configuration;
import org.apache.geronimo.kernel.repository.Environment;
import org.apache.geronimo.naming.deployment.AbstractNamingBuilder;
import org.apache.geronimo.naming.reference.PersistenceContextReference;
import org.apache.geronimo.naming.reference.PersistenceUnitReference;
import org.apache.geronimo.schema.NamespaceElementConverter;
import org.apache.geronimo.schema.SchemaConversionUtils;
import org.apache.geronimo.xbeans.geronimo.naming.GerPatternType;
import org.apache.geronimo.xbeans.geronimo.naming.GerPersistenceContextRefDocument;
import org.apache.geronimo.xbeans.geronimo.naming.GerPersistenceContextRefType;
import org.apache.geronimo.xbeans.geronimo.naming.GerPersistenceContextTypeType;
import org.apache.geronimo.xbeans.geronimo.naming.GerPersistenceUnitRefDocument;
import org.apache.geronimo.xbeans.geronimo.naming.GerPersistenceUnitRefType;
import org.apache.geronimo.xbeans.geronimo.naming.GerPropertyType;
import org.apache.openejb.jee.InjectionTarget;
import org.apache.openejb.jee.JndiConsumer;
import org.apache.openejb.jee.PersistenceContextRef;
import org.apache.openejb.jee.PersistenceContextType;
import org.apache.openejb.jee.PersistenceRef;
import org.apache.openejb.jee.PersistenceUnitRef;
import org.apache.openejb.jee.Property;
import org.apache.xmlbeans.QNameSet;
import org.apache.xmlbeans.XmlObject;

/**
 * @version $Rev$ $Date$
 */
@GBean(j2eeType = NameFactory.MODULE_BUILDER)
public class PersistenceRefBuilder extends AbstractNamingBuilder {
    private static final QName PERSISTENCE_UNIT_REF_QNAME = new QName(JEE_NAMESPACE, "persistence-unit-ref");
    private static final QNameSet PERSISTENCE_UNIT_REF_QNAME_SET = QNameSet.singleton(PERSISTENCE_UNIT_REF_QNAME);
    private static final QName GER_PERSISTENCE_UNIT_REF_QNAME = GerPersistenceUnitRefDocument.type.getDocumentElementName();
    private static final QNameSet GER_PERSISTENCE_UNIT_REF_QNAME_SET = QNameSet.singleton(GER_PERSISTENCE_UNIT_REF_QNAME);
    private static final Set PERSISTENCE_UNIT_INTERFACE_TYPES = Collections.singleton("org.apache.geronimo.persistence.PersistenceUnitGBean");
    private static final QName GER_PERSISTENCE_CONTEXT_REF_QNAME = GerPersistenceContextRefDocument.type.getDocumentElementName();
    private static final QNameSet GER_PERSISTENCE_CONTEXT_REF_QNAME_SET = QNameSet.singleton(GER_PERSISTENCE_CONTEXT_REF_QNAME);
    private final AbstractNameQuery defaultPersistenceUnitAbstractNameQuery;
    private final boolean strictMatching;


    public PersistenceRefBuilder(@ParamAttribute(name = "defaultEnvironment") Environment defaultEnvironment,
                                     @ParamAttribute(name = "defaultPersistenceUnitAbstractNameQuery") AbstractNameQuery defaultPersistenceUnitAbstractNameQuery,
                                     @ParamAttribute(name = "strictMatching") boolean strictMatching) {
        super(defaultEnvironment);
        this.defaultPersistenceUnitAbstractNameQuery = defaultPersistenceUnitAbstractNameQuery;
        this.strictMatching = strictMatching;
    }

    protected boolean willMergeEnvironment(JndiConsumer specDD, XmlObject plan) throws DeploymentException {
        if (specDD != null && !specDD.getPersistenceUnitRef().isEmpty()) {
            return true;
        }
        return plan != null && plan.selectChildren(PersistenceRefBuilder.GER_PERSISTENCE_UNIT_REF_QNAME_SET).length > 0;
    }

    public void buildNaming(JndiConsumer specDD, XmlObject plan, Module module, Map<EARContext.Key, Object> sharedContext) throws DeploymentException {
        Configuration localConfiguration = module.getEarContext().getConfiguration();
        List<DeploymentException> problems = new ArrayList<DeploymentException>();

        // Discover and process any @PersistenceUnitRef and @PersistenceContextRef annotations (if !metadata-complete)
        if (module.getClassFinder() != null) {
            processAnnotations(specDD, module);
        }

        //persistenceUnit refs
        Collection<PersistenceUnitRef> specPersistenceUnitRefsUntyped = specDD.getPersistenceUnitRef();
        Map<String, GerPersistenceUnitRefType> gerPersistenceUnitRefsUntyped = getGerPersistenceUnitRefs(plan);
        for (Map.Entry<String, PersistenceUnitRef> entry : specDD.getPersistenceUnitRefMap().entrySet()) {
            try {
                String persistenceUnitRefName = entry.getKey();
                PersistenceUnitRef persistenceUnitRef = entry.getValue();
                AbstractNameQuery persistenceUnitNameQuery;
                GerPersistenceUnitRefType gerPersistenceUnitRef = gerPersistenceUnitRefsUntyped.remove(persistenceUnitRefName);
                if (gerPersistenceUnitRef != null) {
                    persistenceUnitNameQuery = findPersistenceUnit(gerPersistenceUnitRef);
                    checkForGBean(localConfiguration, persistenceUnitNameQuery, true);
                } else {
                    persistenceUnitNameQuery = findPersistenceUnitQuery(module, localConfiguration, persistenceUnitRef);
                }

                PersistenceUnitReference reference = new PersistenceUnitReference(module.getConfigId(), persistenceUnitNameQuery);

                put(persistenceUnitRefName, reference, module.getJndiContext(), persistenceUnitRef.getInjectionTarget(), sharedContext);
            } catch (DeploymentException e) {
                problems.add(e);
            }

        }
        //geronimo-only persistence unit refs have no injections
        for (GerPersistenceUnitRefType gerPersistenceUnitRef : gerPersistenceUnitRefsUntyped.values()) {
            try {
                String persistenceUnitRefName = gerPersistenceUnitRef.getPersistenceUnitRefName();
                AbstractNameQuery persistenceUnitNameQuery = findPersistenceUnit(gerPersistenceUnitRef);
                checkForGBean(localConfiguration, persistenceUnitNameQuery, true);
                PersistenceUnitReference reference = new PersistenceUnitReference(module.getConfigId(), persistenceUnitNameQuery);
                put(persistenceUnitRefName, reference, module.getJndiContext(), Collections.<InjectionTarget>emptyList(), sharedContext);
            } catch (DeploymentException e) {
                problems.add(e);
            }
        }


        //persistence context refs
        Collection<PersistenceContextRef> specPersistenceContextRefsUntyped = specDD.getPersistenceContextRef();
        Map<String, GerPersistenceContextRefType> gerPersistenceContextRefsUntyped = getGerPersistenceContextRefs(plan);
        for (Map.Entry<String, PersistenceContextRef> entry : specDD.getPersistenceContextRefMap().entrySet()) {
            try {
                String persistenceContextRefName = entry.getKey();
                PersistenceContextRef persistenceContextRef = entry.getValue();
                PersistenceContextType persistenceContextType = persistenceContextRef.getPersistenceContextType();
                boolean transactionScoped = persistenceContextType == null || persistenceContextType.equals(PersistenceContextType.TRANSACTION);

                List<Property> propertyTypes = persistenceContextRef.getPersistenceProperty();
                Map<String, String> properties = new HashMap<String, String>();
                for (Property propertyType : propertyTypes) {
                    String key = propertyType.getName();
                    String value = propertyType.getValue();
                    properties.put(key, value);
                }

                AbstractNameQuery persistenceUnitNameQuery;
                GerPersistenceContextRefType gerPersistenceContextRef = gerPersistenceContextRefsUntyped.remove(persistenceContextRefName);
                if (gerPersistenceContextRef != null) {
                    persistenceUnitNameQuery = findPersistenceUnit(gerPersistenceContextRef);
                    addProperties(gerPersistenceContextRef, properties);
                    checkForGBean(localConfiguration, persistenceUnitNameQuery, true);
                } else {
                    persistenceUnitNameQuery = findPersistenceUnitQuery(module, localConfiguration, persistenceContextRef);
                }
                PersistenceContextReference reference = new PersistenceContextReference(module.getConfigId(), persistenceUnitNameQuery, transactionScoped, properties);
                put(persistenceContextRefName, reference, module.getJndiContext(), persistenceContextRef.getInjectionTarget(), sharedContext);
            } catch (DeploymentException e) {
                problems.add(e);
            }
        }

        // Support persistence context refs that are mentioned only in the geronimo plan
        for (GerPersistenceContextRefType gerPersistenceContextRef : gerPersistenceContextRefsUntyped.values()) {
            try {
                String persistenceContextRefName = gerPersistenceContextRef.getPersistenceContextRefName();
                GerPersistenceContextTypeType.Enum persistenceContextType = gerPersistenceContextRef.getPersistenceContextType();
                boolean transactionScoped = persistenceContextType == null || !persistenceContextType.equals(GerPersistenceContextTypeType.EXTENDED);
                Map<String, String> properties = new HashMap<String, String>();
                addProperties(gerPersistenceContextRef, properties);
                AbstractNameQuery persistenceUnitNameQuery = findPersistenceUnit(gerPersistenceContextRef);
                checkForGBean(localConfiguration, persistenceUnitNameQuery, true);
                PersistenceContextReference reference = new PersistenceContextReference(module.getConfigId(), persistenceUnitNameQuery, transactionScoped, properties);
                put(persistenceContextRefName, reference, module.getJndiContext(), Collections.<InjectionTarget>emptyList(), sharedContext);
            } catch (DeploymentException e) {
                problems.add(e);
            }

        }

        if (!problems.isEmpty()) {
            throw new DeploymentException("At least one deployment problem:", problems);
        }
    }

    private AbstractNameQuery findPersistenceUnitQuery(Module module, Configuration localConfiguration, PersistenceRef persistenceRef) throws DeploymentException {
        AbstractNameQuery persistenceUnitNameQuery;
        if (persistenceRef.getPersistenceUnitName() != null && !persistenceRef.getPersistenceUnitName().trim().isEmpty()) {
            String persistenceUnitName = persistenceRef.getPersistenceUnitName().trim();
            AbstractName childName = module.getEarContext().getNaming().createChildName(module.getModuleName(), persistenceUnitName, NameFactory.PERSISTENCE_UNIT);
            persistenceUnitNameQuery = new AbstractNameQuery(null, childName.getName(), PERSISTENCE_UNIT_INTERFACE_TYPES);
            if (!checkForGBean(localConfiguration, persistenceUnitNameQuery, strictMatching)) {
                persistenceUnitName = "persistence/" + persistenceUnitName;
                childName = module.getEarContext().getNaming().createChildName(module.getModuleName(), persistenceUnitName, NameFactory.PERSISTENCE_UNIT);
                persistenceUnitNameQuery = new AbstractNameQuery(null, childName.getName(), PERSISTENCE_UNIT_INTERFACE_TYPES);
                checkForGBean(localConfiguration, persistenceUnitNameQuery, true);
            }
        } else {
            AbstractName childName = module.getEarContext().getNaming().createChildName(module.getModuleName(), "", NameFactory.PERSISTENCE_UNIT);
            Map<String, String> name = new HashMap<String, String>(childName.getName());
            name.remove(NameFactory.J2EE_NAME);
            persistenceUnitNameQuery = new AbstractNameQuery(null, name, PERSISTENCE_UNIT_INTERFACE_TYPES);
            Set<AbstractNameQuery> patterns = Collections.singleton(persistenceUnitNameQuery);
            LinkedHashSet<GBeanData> gbeans = localConfiguration.findGBeanDatas(localConfiguration, patterns);
            persistenceUnitNameQuery = checkForDefaultPersistenceUnit(gbeans);
            if (gbeans.isEmpty()) {
                gbeans = localConfiguration.findGBeanDatas(patterns);
                persistenceUnitNameQuery = checkForDefaultPersistenceUnit(gbeans);

                if (gbeans.isEmpty()) {
                    if (defaultPersistenceUnitAbstractNameQuery == null) {
                        throw new DeploymentException("No default PersistenceUnit specified, and none located");
                    }
                    persistenceUnitNameQuery = defaultPersistenceUnitAbstractNameQuery;
                }
            }
        }
        checkForGBean(localConfiguration, persistenceUnitNameQuery, true);
        return persistenceUnitNameQuery;
    }

    private static AbstractNameQuery checkForDefaultPersistenceUnit(LinkedHashSet<GBeanData> gbeans) throws DeploymentException {
        AbstractNameQuery persistenceUnitNameQuery = null;
        for (java.util.Iterator it = gbeans.iterator(); it.hasNext();) {
            GBeanData gbean = (GBeanData) it.next();
            AbstractName name = gbean.getAbstractName();
            Map nameMap = name.getName();
            if ("cmp".equals(nameMap.get("name"))) {
                it.remove();
            } else {
                persistenceUnitNameQuery = new AbstractNameQuery(name);
            }
        }
        if (gbeans.size() > 1) {
            throw new DeploymentException("Too many matches for no-name persistence unit: " + gbeans);
        }
        return persistenceUnitNameQuery;
    }

    private static boolean checkForGBean(Configuration localConfiguration, AbstractNameQuery persistenceQuery, boolean complainIfMissing) throws DeploymentException {
        try {
            localConfiguration.findGBeanData(persistenceQuery);
            return true;
        } catch (GBeanNotFoundException e) {
            if (complainIfMissing || e.hasMatches()) {
                if (e.hasMatches()) {
                    throw new DeploymentException("More than one reference at deploy time for query " + persistenceQuery + ". " + e.getMatches(), e);
                } else {
                    throw new DeploymentException("No references found at deploy time for query " + persistenceQuery, e);
                }
            }
            return false;
        }
    }

    private void processAnnotations(JndiConsumer specDD, Module module) throws DeploymentException {
        // Process all the annotations for this naming builder type
        PersistenceUnitAnnotationHelper.processAnnotations(specDD, module.getClassFinder());
        PersistenceContextAnnotationHelper.processAnnotations(specDD, module.getClassFinder());
    }

    private AbstractNameQuery findPersistenceUnit(GerPersistenceUnitRefType gerPersistenceRef) {
        AbstractNameQuery persistenceUnitNameQuery;
        if (gerPersistenceRef.isSetPersistenceUnitName()) {
            String persistenceUnitName = gerPersistenceRef.getPersistenceUnitName();
            persistenceUnitNameQuery = new AbstractNameQuery(null, Collections.singletonMap("name", persistenceUnitName), PERSISTENCE_UNIT_INTERFACE_TYPES);
        } else {
            GerPatternType gbeanLocator = gerPersistenceRef.getPattern();

            persistenceUnitNameQuery = buildAbstractNameQuery(gbeanLocator, null, null, PERSISTENCE_UNIT_INTERFACE_TYPES);
        }
        return persistenceUnitNameQuery;
    }

    public QNameSet getSpecQNameSet() {
        SchemaConversionUtils.registerNamespaceConversions(Collections.singletonMap(PersistenceRefBuilder.GER_PERSISTENCE_UNIT_REF_QNAME.getLocalPart(), new NamespaceElementConverter(PersistenceRefBuilder.GER_PERSISTENCE_UNIT_REF_QNAME.getNamespaceURI())));
        return PERSISTENCE_UNIT_REF_QNAME_SET;
    }

    public QNameSet getPlanQNameSet() {
        return GER_PERSISTENCE_UNIT_REF_QNAME_SET;
    }

    private Map<String, GerPersistenceUnitRefType> getGerPersistenceUnitRefs(XmlObject plan) throws DeploymentException {
        Map<String, GerPersistenceUnitRefType> map = new HashMap<String, GerPersistenceUnitRefType>();
        if (plan != null) {
            List<GerPersistenceUnitRefType> refs = convert(plan.selectChildren(PersistenceRefBuilder.GER_PERSISTENCE_UNIT_REF_QNAME_SET), NAMING_CONVERTER, GerPersistenceUnitRefType.class, GerPersistenceUnitRefType.type);
            for (GerPersistenceUnitRefType ref : refs) {
                map.put(ref.getPersistenceUnitRefName().trim(), ref);
            }
        }
        return map;
    }

    private void addProperties(GerPersistenceContextRefType persistenceContextRef, Map<String, String> properties) {
        GerPropertyType[] propertyTypes = persistenceContextRef.getPropertyArray();
        for (GerPropertyType propertyType : propertyTypes) {
            String key = propertyType.getKey();
            String value = propertyType.getValue();
            properties.put(key, value);
        }
    }

    private Map<String, GerPersistenceContextRefType> getGerPersistenceContextRefs(XmlObject plan) throws DeploymentException {
        Map<String, GerPersistenceContextRefType> map = new HashMap<String, GerPersistenceContextRefType>();
        if (plan != null) {
            List<GerPersistenceContextRefType> refs = convert(plan.selectChildren(GER_PERSISTENCE_CONTEXT_REF_QNAME_SET), NAMING_CONVERTER, GerPersistenceContextRefType.class, GerPersistenceContextRefType.type);
            for (GerPersistenceContextRefType ref : refs) {
                map.put(ref.getPersistenceContextRefName().trim(), ref);
            }
        }
        return map;
    }

    private AbstractNameQuery findPersistenceUnit(GerPersistenceContextRefType persistenceContextRef) {
        AbstractNameQuery persistenceUnitNameQuery;
        if (persistenceContextRef.isSetPersistenceUnitName()) {
            String persistenceUnitName = persistenceContextRef.getPersistenceUnitName();
            persistenceUnitNameQuery = new AbstractNameQuery(null, Collections.singletonMap("name", persistenceUnitName), PERSISTENCE_UNIT_INTERFACE_TYPES);
        } else {
            GerPatternType gbeanLocator = persistenceContextRef.getPattern();

            persistenceUnitNameQuery = buildAbstractNameQuery(gbeanLocator, null, null, PERSISTENCE_UNIT_INTERFACE_TYPES);
        }
        return persistenceUnitNameQuery;
    }


}
