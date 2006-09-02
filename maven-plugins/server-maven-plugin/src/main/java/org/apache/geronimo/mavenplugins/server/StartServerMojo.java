/*
 *  Copyright 2006 The Apache Software Foundation
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

package org.apache.geronimo.mavenplugins.server;

import org.apache.maven.project.MavenProject;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.factory.ArtifactFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.ConnectException;

import org.apache.geronimo.genesis.AntMojoSupport;
import org.apache.geronimo.plugin.ArtifactItem;
import org.apache.commons.lang.SystemUtils;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.taskdefs.Expand;

/**
 * Start the Geronimo server.
 *
 * @goal start
 *
 * @version $Id$
 */
public class StartServerMojo
    extends AntMojoSupport
{
    /**
     * The assembly to unpack which contains the serer to start.
     *
     * @parameter
     * @required
     */
    private ArtifactItem assembly = null;

    /**
     * Directory to extract the assembly into.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private File outputDirectory = null;

    //
    // MojoSupport Hooks
    //

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project = null;


    protected MavenProject getProject() {
        return project;
    }

    /**
     * ???
     *
     * @component
     * @required
     * @readonly
     */
    private ArtifactFactory artifactFactory = null;

    protected ArtifactFactory getArtifactFactory() {
        return artifactFactory;
    }

    /**
     * ???
     *
     * @component
     * @required
     * @readonly
     */
    private ArtifactResolver artifactResolver = null;

    protected ArtifactResolver getArtifactResolver() {
        return artifactResolver;
    }

    /**
     * ???
     *
     * @parameter expression="${localRepository}"
     * @readonly
     * @required
     */
    private ArtifactRepository artifactRepository = null;

    protected ArtifactRepository getArtifactRepository() {
        return artifactRepository;
    }

    //
    // Mojo
    //

    protected void doExecute() throws Exception {
        log.info("Starting server...");

        log.debug("Using assembly: " + assembly);

        // Unzip the assembly
        Artifact artifact = getArtifact(assembly);

        // What are we running... where?
        final File workDir = new File(outputDirectory, artifact.getArtifactId() + "-" + artifact.getVersion());
        final String executable = "java" +  (SystemUtils.IS_OS_WINDOWS ? ".exe" : "");

        if (!workDir.exists()) {
            log.info("Extracting assembly: " + artifact.getFile());

            Expand unzip = (Expand)createTask("unzip");
            unzip.setSrc(artifact.getFile());
            unzip.setDest(outputDirectory);
            unzip.execute();
        }
        else {
            log.debug("Assembly already unpacked... reusing");
        }

        // Start the server int a seperate thread
        Thread t = new Thread("Server Runner") {
            public void run() {
                try {
                    ExecTask exec = (ExecTask)createTask("exec");
                    exec.setExecutable(executable);
                    exec.createArg().setValue("-jar");
                    exec.createArg().setFile(new File(workDir, "bin/server.jar"));
                    exec.createArg().setValue("--quiet");
                    exec.setDir(workDir);
                    exec.setLogError(true);
                    exec.execute();

                    synchronized(this) {
                        wait();
                    }
                }
                catch (Exception e) {
                    // ignore
                }
            }
        };
        t.start();
        
        //
        // TODO: Check the status via JMX
        //
        
        // Verify server started
        URL url = new URL("http://localhost:8080");
        boolean started = false;
        while (!started) {
            log.debug("Trying connection to: " + url);

            try {
                Object input = url.openConnection().getContent();
                log.debug("Input: " + input);
                started = true;
            }
            catch (Exception e) {
                // ignore
            }

            Thread.sleep(1000);
        }

        log.info("Server started");
    }
}
