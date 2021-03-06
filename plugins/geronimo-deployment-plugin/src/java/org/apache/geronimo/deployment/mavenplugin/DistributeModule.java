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

package org.apache.geronimo.deployment.mavenplugin;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.status.ProgressObject;

/**
 *
 *
 * @version $Rev$ $Date$
 *
 * */
public class DistributeModule extends AbstractModuleCommand {

    private String module;
    private String plan;

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public void execute() throws Exception {
        DeploymentManager manager = getDeploymentManager();

        Target[] targets = manager.getTargets();
        File moduleFile = (getModule() == null)? null: getFile(getModule());
        File planFile = (getPlan() == null)? null: getFile((getPlan()));
        ProgressObject progress = manager.distribute(targets, moduleFile, planFile);
        DeploymentClient.waitFor(progress);
    }

    private File getFile(String location) throws MalformedURLException {
    	try {
			File f = new File(location).getCanonicalFile();
			if (!f.exists() || !f.canRead()) {
				throw new MalformedURLException("Invalid location: " + location);
			}
			return f;
		} catch (IOException e) {
			throw (MalformedURLException) new MalformedURLException("Invalid location: " + location).initCause(e);
		}
            
    }

}
