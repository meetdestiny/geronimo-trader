/**
 *
 * Copyright 2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.geronimo.security.remoting.jmx;

import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.core.service.Interceptor;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.gbean.GBeanLifecycle;
import org.apache.geronimo.kernel.Kernel;
import org.apache.geronimo.kernel.jmx.JMXUtil;
import org.apache.geronimo.proxy.ProxyContainer;
import org.apache.geronimo.proxy.ReflexiveInterceptor;
import org.apache.geronimo.remoting.DeMarshalingInterceptor;
import org.apache.geronimo.remoting.router.JMXRouter;
import org.apache.geronimo.remoting.router.JMXTarget;
import org.apache.geronimo.security.jaas.JaasLoginServiceMBean;


/**
 * A server-side utility that exposes a JaasLoginService to remote clients.
 * It prevents clients from connecting to arbitrary server-side MBeans through
 * this listener -- only the JaasLoginService is exposed.
 * 
 * @version $Rev: 56022 $ $Date: 2004-10-30 01:16:18 -0400 (Sat, 30 Oct 2004) $
 */
public class JaasLoginServiceRemotingServer implements GBeanLifecycle, JMXTarget {
    private static final Log log = LogFactory.getLog(JaasLoginServiceRemotingServer.class);
    private final Kernel kernel;
    private final ObjectName objectName;
    private ProxyContainer serverContainer;
    private DeMarshalingInterceptor demarshaller;
    private JMXRouter router;
    private JaasLoginServiceMBean loginService;

    public JaasLoginServiceRemotingServer(Kernel kernel, String objectName) {
        this.kernel = kernel;
        this.objectName = JMXUtil.getObjectName(objectName);
    }

    public Interceptor getRemotingEndpointInterceptor() {
        return demarshaller;
    }

    public JMXRouter getRouter() {
        return router;
    }

    public void setRouter(JMXRouter router) {
        this.router = router;
    }

    public void doStart() throws Exception {
        router.register(objectName, this);

        // Setup the server side contianer..
        // todo dain: alan, why is this not a dependency?
        // todo dain: hard coded object names are very very bery bad
        loginService = (JaasLoginServiceMBean) kernel.getProxyManager().createProxy(JMXUtil.getObjectName("geronimo.security:type=JaasLoginService"), JaasLoginServiceMBean.class);
        Interceptor firstInterceptor = new ReflexiveInterceptor(loginService);
        demarshaller = new DeMarshalingInterceptor(firstInterceptor, getClass().getClassLoader());
        serverContainer = new ProxyContainer(firstInterceptor);

        log.info("Started login service stub");
    }

    public void doStop() {
        router.unregister(objectName);
        kernel.getProxyManager().destroyProxy(loginService);
        serverContainer = null;
        demarshaller = null;
        log.info("Stopped login service stub");
    }

    public void doFail() {
        serverContainer = null;
        demarshaller = null;
        log.info("Failed login service stub");
    }

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoBuilder infoFactory = new GBeanInfoBuilder(JaasLoginServiceRemotingServer.class);
        infoFactory.addAttribute("kernel", Kernel.class, false);
        infoFactory.addAttribute("objectName", String.class, false);
        infoFactory.addReference("Router", JMXRouter.class);
        infoFactory.addOperation("getRemotingEndpointInterceptor");
        infoFactory.setConstructor(new String[]{"kernel", "objectName"});
        GBEAN_INFO = infoFactory.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }
}
