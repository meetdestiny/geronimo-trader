/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
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
package org.apache.geronimo.deployment.plugin.local;

import java.util.Set;
import java.util.Iterator;
import java.util.List;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileOutputStream;
import javax.enterprise.deploy.shared.CommandType;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.Target;
import javax.management.ObjectName;

import org.apache.geronimo.kernel.jmx.JMXUtil;
import org.apache.geronimo.kernel.Kernel;
import org.apache.geronimo.common.DeploymentException;
import org.apache.geronimo.deployment.plugin.TargetModuleIDImpl;

/**
 * @version $Rev: 190584 $ $Date$
 */
public abstract class AbstractDeployCommand extends CommandSupport {
    private final static String DEPLOYER_NAME = "*:name=Deployer,j2eeType=Deployer,*";

    protected final Kernel kernel;
    private static final String[] DEPLOY_SIG = {File.class.getName(), File.class.getName()};
    protected final boolean spool;
    protected File moduleArchive;
    protected File deploymentPlan;
    protected InputStream moduleStream;
    protected InputStream deploymentStream;

    public AbstractDeployCommand(CommandType command, Kernel kernel, File moduleArchive, File deploymentPlan, InputStream moduleStream, InputStream deploymentStream, boolean spool) {
        super(command);
        this.kernel = kernel;
        this.moduleArchive = moduleArchive;
        this.deploymentPlan = deploymentPlan;
        this.moduleStream = moduleStream;
        this.deploymentStream = deploymentStream;
        this.spool = spool;
    }

    protected ObjectName getDeployerName() {
        Set deployers = kernel.listGBeans(JMXUtil.getObjectName(DEPLOYER_NAME));
        if (deployers.isEmpty()) {
            fail("No Deployer GBean present in running Geronimo server. " +
                 "This usually indicates a serious problem with the configuration of " +
                 "your running Geronimo server.  If " +
                 "the deployer is present but not started, the workaround is to run " +
                 "a deploy command like 'start org/apache/geronimo/RuntimeDeployer'.  " +
                 "If the deployer service is not present at all (it was undeployed) then " +
                 "you need to either re-install Geronimo or get a deployment plan for the " +
                 "runtime deployer and distribute it while the server is not running and " +
                 "then start the server with a command like the above.  For help on this, " +
                 "write to user@geronimo.apache.org and include the contents of your " +
                 "config-store/index.properties and var/config/config.list files.");
            return null;
        }
        Iterator j = deployers.iterator();
        ObjectName deployer = (ObjectName) j.next();
        if (j.hasNext()) {
            fail("More than one deployer found");
            return null;
        }
        return deployer;

    }

    protected void copyTo(File outfile, InputStream is) throws IOException {
        byte[] buffer = new byte[4096];
        int count;
        OutputStream os = new FileOutputStream(outfile);
        try {
            while ((count = is.read(buffer)) > 0) {
                os.write(buffer, 0, count);
            }
        } finally {
            os.close();
        }
    }

    protected void doDeploy(ObjectName deployer, Target target, boolean finished) throws Exception {
        Object[] args = {moduleArchive, deploymentPlan};
        List objectNames = (List) kernel.invoke(deployer, "deploy", args, DEPLOY_SIG);
        if (objectNames == null || objectNames.isEmpty()) {
            DeploymentException deploymentException = new DeploymentException("Got empty list");
            deploymentException.printStackTrace();
            throw deploymentException;
        }
        String parentName = (String) objectNames.get(0);
        String[] childIDs = new String[objectNames.size()-1];
        for (int j=0; j < childIDs.length; j++) {
            childIDs[j] = (String)objectNames.get(j+1);
        }

        TargetModuleID moduleID = new TargetModuleIDImpl(target, parentName.toString(), childIDs);
        addModule(moduleID);
        if(finished) {
            complete("Completed with id " + parentName);
        }
    }
}
