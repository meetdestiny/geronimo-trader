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

package org.apache.geronimo.myfaces.webapp;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.apache.geronimo.osgi.web.WebApplicationConstants;
import org.apache.geronimo.web.WebAttributeName;
import org.apache.geronimo.web.info.WebAppInfo;
import org.apache.myfaces.shared_impl.webapp.webxml.WebXml;
import org.apache.myfaces.webapp.StartupServletContextListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * @version $Rev$ $Date$
 */
public class GeronimoStartupServletContextListener extends StartupServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        ServletContext servletContext = servletContextEvent.getServletContext();
        WebAppInfo webAppInfo = (WebAppInfo) servletContext.getAttribute(WebAttributeName.WEB_APP_INFO.name());
        Bundle bundle = ((BundleContext) servletContext.getAttribute(WebApplicationConstants.BUNDLE_CONTEXT_ATTRIBUTE)).getBundle();
        //TODO remove the delegateFacesServlet constructor patameter once upgrading to MyFaces 2.0.3
        GeronimoWebXml webXml = new GeronimoWebXml(bundle, webAppInfo, servletContext.getInitParameter("org.apache.myfaces.DELEGATE_FACES_SERVLET"));
        servletContext.setAttribute(WebXml.class.getName(), webXml);
        super.contextInitialized(servletContextEvent);
    }
}
