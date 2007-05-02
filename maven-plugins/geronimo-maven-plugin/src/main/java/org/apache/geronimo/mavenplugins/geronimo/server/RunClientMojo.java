/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.geronimo.mavenplugins.geronimo.server;

import java.io.File;

import org.apache.tools.ant.taskdefs.Java;

import org.apache.maven.plugin.MojoExecutionException;

import org.apache.geronimo.mavenplugins.geronimo.ServerProxy;
import org.apache.geronimo.mavenplugins.geronimo.reporting.ReportingMojoSupport;

import org.codehaus.plexus.util.FileUtils;

/**
 * Execute application client.
 *
 * @goal run-client
 *
 * @version $Rev$ $Date$
 */
public class RunClientMojo extends ReportingMojoSupport
{
    /**
     * The id of the client module to be executed
     *
     * @parameter expression="${moduleId}
     * @required
     */
    protected String moduleId = null;

    /**
     * Set the maximum memory for the forked JVM.
     *
     * @parameter expression="${maximumMemory}"
     */
    private String maximumMemory = null;

    /**
     * Time in seconds to wait before terminating the forked JVM.
     *
     * @parameter expression="${timeout}" default-value="-1"
     */
    private int timeout = -1;

    /**
     * The arguments
     *
     * @parameter expression="${arg}
     * @optional
     */
    protected String[] arg = null;

    protected void doExecute() throws Exception {
        ServerProxy server =
            new ServerProxy(hostname, port, username, password);

        String geronimoHomeStr = server.getGeronimoHome();

        log.info("Geronimo Home: " + geronimoHomeStr);

        if (geronimoHomeStr == null) {
            throw new MojoExecutionException("Unable to determine Geronimo installation directory");
        }

        File geronimoHome = new File(geronimoHomeStr);

        if (!geronimoHome.exists()) {
            throw new MojoExecutionException("Geronimo installation directory does not exist: " + geronimoHomeStr);
        }

        log.info("Starting Geronimo client...");

        Java java = (Java)createTask("java");
        java.setJar(new File(geronimoHome, "bin/client.jar"));
        java.setDir(geronimoHome);
        java.setFailonerror(true);
        java.setFork(true);

        if (timeout > 0) {
            java.setTimeout(new Long(timeout * 1000));
        }

        if (maximumMemory != null) {
            java.setMaxmemory(maximumMemory);
        }

        // Set the properties which we pass to the JVM from the startup script
        setSystemProperty(java, "org.apache.geronimo.base.dir", geronimoHome);
        setSystemProperty(java, "java.io.tmpdir", "var/temp");
        setSystemProperty(java, "java.endorsed.dirs", prefixSystemPath("java.endorsed.dirs", new File(geronimoHome, "lib/endorsed")));
        setSystemProperty(java, "java.ext.dirs", prefixSystemPath("java.ext.dirs", new File(geronimoHome, "lib/ext")));

        java.createArg().setValue(moduleId);

        for (int i=0;arg != null && i<arg.length;i++) {
            java.createArg().setValue(arg[i]);
        }

        if (logOutput) {
            File file = getLogFile();
            FileUtils.forceMkdir(file.getParentFile());

            log.info("Redirecting output to: " + file);

            java.setOutput(file);
        }

        java.execute();
    }

    private String prefixSystemPath(final String name, final File file) {
        assert name != null;
        assert file != null;

        String dirs = file.getPath();
        String prop = System.getProperty(name, "");
        if (prop.length() > 0) {
            dirs += File.pathSeparator;
            dirs += prop;
        }
        return dirs;
    }

    protected String getFullClassName() {
        return this.getClass().getName();
    }
}
