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
package org.apache.geronimo.jaxws.builder;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.wsdl.Binding;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLLocator;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceClient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.common.DeploymentException;
import org.apache.geronimo.jaxws.JAXWSUtils;
import org.apache.geronimo.jaxws.client.EndpointInfo;
import org.apache.geronimo.xbeans.geronimo.naming.GerPortType;
import org.apache.geronimo.xbeans.geronimo.naming.GerServiceRefType;
import org.xml.sax.InputSource;

public class EndpointInfoBuilder {

    private static final Log LOG = LogFactory.getLog(EndpointInfoBuilder.class);

    private JarFile moduleFile;

    private URI wsdlURI;

    private QName serviceQName;

    private Class serviceClass;

    private GerServiceRefType serviceRefType;

    private Map<Object, EndpointInfo> portInfoMap = new HashMap<Object, EndpointInfo>();

    private Map<Class, String> portComponentRefMap;

    public EndpointInfoBuilder(Class serviceClass,
                               GerServiceRefType serviceRefType,
                               Map<Class, String> portComponentRefMap,
                               JarFile moduleFile,
                               URI wsdlURI,
                               QName serviceQName) {
        this.serviceClass = serviceClass;
        this.serviceRefType = serviceRefType;
        this.portComponentRefMap = portComponentRefMap;
        this.moduleFile = moduleFile;
        this.wsdlURI = wsdlURI;
        this.serviceQName = serviceQName;
    }

    public URI getWsdlURI() {
        return this.wsdlURI;
    }
    
    public QName getServiceQName() {
        return this.serviceQName;
    }

    public Map<Object, EndpointInfo> getEndpointInfo() {
        return this.portInfoMap;
    }
    
    public void build() throws DeploymentException {
        if (this.wsdlURI == null) {
            // wsdl explitely not specified, try to get it from 
            // the service class annotation
            WebServiceClient webServiceClient = 
                (WebServiceClient) this.serviceClass.getAnnotation(WebServiceClient.class);
            if (webServiceClient != null) {
                this.wsdlURI = getWSDLLocation(webServiceClient);               
                this.serviceQName = getServiceQName(webServiceClient);
            }
           
            if (this.wsdlURI == null) {                
                return;
            }
        }
        
        JarWSDLLocator wsdlLocator = null;
        URL wsdlURL = null;
        try {
            wsdlURL = new URL(this.wsdlURI.toString());
        } catch (MalformedURLException e1) {
            // not a URL, assume it's a local reference
            wsdlLocator = new JarWSDLLocator(this.wsdlURI);
        }

        Definition definition;
        WSDLFactory wsdlFactory;
        try {
            wsdlFactory = WSDLFactory.newInstance();
        } catch (WSDLException e) {
            throw new DeploymentException("Could not create WSDLFactory", e);
        }
        WSDLReader wsdlReader = wsdlFactory.newWSDLReader();
        wsdlReader.setFeature("javax.wsdl.importDocuments", false);
        try {
            if (wsdlURL != null) {
                definition = wsdlReader.readWSDL(wsdlURL.toString());
            } else if (wsdlLocator != null) {
                definition = wsdlReader.readWSDL(wsdlLocator);
            } else {
                throw new DeploymentException("unknown");
            }
        } catch (WSDLException e) {
            throw new DeploymentException("Failed to read wsdl document", e);
        } catch (RuntimeException e) {
            throw new DeploymentException(e.getMessage(), e);
        }

        verifyPortComponentList(definition);
        
        Map services = definition.getServices();
        if (services.size() == 0) {
            // partial wsdl, return as is

            if (this.serviceRefType != null && this.serviceRefType.isSetServiceCompletion()) {
                LOG.warn("Service completion is not supported with partial wsdl");
            }
        } else {
            // full wsdl

            if (this.serviceRefType != null && this.serviceRefType.isSetServiceCompletion()) {
                throw new DeploymentException("Full wsdl, but service completion supplied");
            }
            
            Service service = null;
            if (this.serviceQName != null) {
                service = definition.getService(this.serviceQName);
                if (service == null) {
                    throw new DeploymentException(
                            "No service wsdl for supplied service qname "
                                    + this.serviceQName);
                }
            } else if (services.size() == 1) {
                service = (Service) services.values().iterator().next();
                this.serviceQName = service.getQName();
            } else {
                throw new DeploymentException(
                        "No service qname supplied, and there are "
                                + services.size() + " services");
            }

            // organize the extra port info
            Map<String, GerPortType> portMap = new HashMap<String, GerPortType>();
            if (serviceRefType != null) {
                GerPortType[] ports = serviceRefType.getPortArray();
                for (int i = 0; i < ports.length; i++) {
                    GerPortType port = ports[i];
                    String portName = port.getPortName().trim();
                    portMap.put(portName, port);
                }
            }

            Map wsdlPortMap = service.getPorts();
            for (Iterator iterator = wsdlPortMap.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry entry = (Map.Entry) iterator.next();
                String portName = (String) entry.getKey();
                Port port = (Port) entry.getValue();

                GerPortType gerPort = portMap.get(portName);

                URL location = (gerPort == null) ? getAddressLocation(port) : getLocation(gerPort);
                // skip non-soap ports
                if (location == null) {
                    continue;
                }
                String credentialsName = (gerPort == null) ? null : getCredentialsName(gerPort);
                
                Binding binding = port.getBinding();
                if (binding == null) {
                    throw new DeploymentException("No binding for port: " + portName);
                }
                
                PortType portType = binding.getPortType();
                if (portType == null) {
                    throw new DeploymentException("No portType for binding: " + binding.getQName());
                }

                EndpointInfo info = new EndpointInfo(location, credentialsName);
                this.portInfoMap.put(portName, info);
                // prefer first binding listed in wsdl
                if (!this.portInfoMap.containsKey(portType.getQName())) {
                    this.portInfoMap.put(portType.getQName(), info);
                }
            }
        }
    }

    private QName getServiceQName(WebServiceClient webServiceClient) {
        if (webServiceClient.targetNamespace() != null && webServiceClient.name() != null) {
            return new QName(webServiceClient.targetNamespace(), webServiceClient.name());
        } else {
            return null;
        }
    }
    
    private URI getWSDLLocation(WebServiceClient webServiceClient) throws DeploymentException {
        String wsdlLocation = webServiceClient.wsdlLocation();
        if (wsdlLocation != null) {
            try {
                return new URI(wsdlLocation);
            } catch (URISyntaxException e) {
                throw new DeploymentException(
                        "Invalid wsdl location in annotation: " + wsdlLocation);
            }
        }

        return null;
    }

    private String getCredentialsName(GerPortType port) {
        String credentialsName = port.getCredentialsName();
        return (credentialsName == null) ? null : credentialsName.trim();        
    }
    
    private URL getLocation(GerPortType port) throws DeploymentException {
        String protocol = port.getProtocol().trim();
        String host = port.getHost().trim();
        int portNum = port.getPort();
        String uri = port.getUri().trim();
        String locationURIString = protocol + "://" + host + ":" + portNum + uri;
        URL location = getURL(locationURIString);
        return location;
    }

    private URL getAddressLocation(Port port) throws DeploymentException {
        SOAPAddress soapAddress = 
            (SOAPAddress) getExtensibilityElement(SOAPAddress.class, port.getExtensibilityElements());
        URL location = null;
        if (soapAddress != null) {
            String locationURIString = soapAddress.getLocationURI();
            location = getURL(locationURIString);
        }
        return location;
    }

    private URL getURL(String locationURIString) throws DeploymentException {
        try {
            return new URL(locationURIString);
        } catch (MalformedURLException e) {
            throw new DeploymentException(
                    "Could not construct web service location URL from "
                            + locationURIString);
        }
    }
    
    public static ExtensibilityElement getExtensibilityElement(Class clazz,
                                                               List extensibilityElements) {
        for (Iterator iterator = extensibilityElements.iterator(); iterator
                .hasNext();) {
            ExtensibilityElement extensibilityElement = (ExtensibilityElement) iterator
                    .next();
            if (clazz.isAssignableFrom(extensibilityElement.getClass())) {
                return extensibilityElement;
            }
        }
        return null;
    }
    
    private void verifyPortComponentList(Definition wsdl) throws DeploymentException {
        if (this.portComponentRefMap == null) {
            return;
        }
        for (Class sei : this.portComponentRefMap.keySet()) {
            QName portType = JAXWSUtils.getPortType(sei);
            if (portType == null) {
                continue;
            }
            if (wsdl.getPortType(portType) == null) {
                throw new DeploymentException("No portType found in WSDL for SEI: " + sei.getName());
            }            
        }        
    }
    
    private class JarWSDLLocator implements WSDLLocator {

        private final List<InputStream> streams = new ArrayList<InputStream>();

        private final URI wsdlURI;

        private URI latestImportURI;

        public JarWSDLLocator(URI wsdlURI) {
            this.wsdlURI = wsdlURI;
        }

        public InputSource getBaseInputSource() {
            InputStream wsdlInputStream;
            ZipEntry entry = moduleFile.getEntry(wsdlURI.toString());
            if (entry == null) {
                throw new RuntimeException(
                        "WSDL file does not exist in the module " + wsdlURI.toString());
            }
            try {
                wsdlInputStream = moduleFile.getInputStream(entry);
                streams.add(wsdlInputStream);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Could not open stream to wsdl file", e);
            }
            return new InputSource(wsdlInputStream);
        }

        public String getBaseURI() {
            return wsdlURI.toString();
        }

        public InputSource getImportInputSource(String parentLocation,
                                                String relativeLocation) {
            URI parentURI = URI.create(parentLocation);
            latestImportURI = parentURI.resolve(relativeLocation);
            InputStream importInputStream;
            ZipEntry entry = moduleFile.getEntry(latestImportURI.toString());
            if (entry == null) {
                throw new RuntimeException(
                        "File does not exist in the module " + latestImportURI.toString());
            }
            try {                
                importInputStream = moduleFile.getInputStream(entry);
                streams.add(importInputStream);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Could not open stream to import file", e);
            }
            InputSource inputSource = new InputSource(importInputStream);
            inputSource.setSystemId(getLatestImportURI());
            return inputSource;
        }

        public String getLatestImportURI() {
            return latestImportURI.toString();
        }

        public void close() {
            for (InputStream inputStream : this.streams) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            streams.clear();
        }
    }
}
