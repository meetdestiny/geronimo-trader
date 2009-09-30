//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.3 in JDK 1.6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.06.02 at 10:12:18 AM PDT 
//


package org.apache.geronimo.tomcat.model;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.net.URI;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.namespace.QName;

import org.apache.catalina.Executor;
import org.apache.catalina.core.StandardThreadExecutor;
import org.apache.xbean.recipe.ObjectRecipe;
import org.apache.xbean.recipe.Option;
import org.apache.geronimo.kernel.Kernel;
import org.apache.geronimo.gbean.AbstractNameQuery;
import org.apache.geronimo.gbean.AbstractName;


/**
 * <p>Java class for ExecutorType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="ExecutorType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="className" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ExecutorType")
public class ExecutorType {

    @XmlAttribute
    protected String className = StandardThreadExecutor.class.getName();
    @XmlAnyAttribute
    private Map<QName, String> otherAttributes = new HashMap<QName, String>();

    /**
     * Gets the value of the className property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getClassName() {
        return className;
    }

    /**
     * Sets the value of the className property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setClassName(String value) {
        this.className = value;
    }

    /**
     * Gets a map that contains attributes that aren't bound to any typed property on this class.
     *
     * <p>
     * the map is keyed by the name of the attribute and
     * the value is the string value of the attribute.
     *
     * the map returned by this method is live, and you can add new attribute
     * by updating the map directly. Because of this design, there's no setter.
     *
     *
     * @return
     *     always non-null
     */
    public Map<QName, String> getOtherAttributes() {
        return otherAttributes;
    }


    public Executor getExecutor(ClassLoader cl, Kernel kernel) throws Exception {
        Map<String, Object> properties = new HashMap<String, Object>();

        for (Map.Entry<QName, String> entry: otherAttributes.entrySet()) {
            String name = entry.getKey().getLocalPart();
            if (name.endsWith("-ref")) {
                AbstractNameQuery query = new AbstractNameQuery(URI.create(entry.getValue()));
                Set<AbstractName> names = kernel.listGBeans(query);
                if (names.size() != 1) throw new IllegalStateException("Unsatisfied reference for name " + name + " and reference to " + entry.getValue());
                Object ref = kernel.getGBean(names.iterator().next());
                properties.put(name.substring(0, name.length() - 4), ref);

            } else {
                properties.put(name, entry.getValue());
            }
        }
        ObjectRecipe recipe = new ObjectRecipe(className, properties);
        recipe.allow(Option.IGNORE_MISSING_PROPERTIES);
        recipe.allow(Option.NAMED_PARAMETERS);
        Executor executor = (Executor) recipe.create(cl);
        return executor;
    }
}