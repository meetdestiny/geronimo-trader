/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.geronimo.mavenplugins.car;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;

import java.util.Iterator;
import java.util.Properties;

import org.apache.geronimo.genesis.MojoSupport;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.factory.ArtifactFactory;

/**
 * Support for <em>packaging</em> Mojos.
 *
 * @version $Rev$ $Date$
 */
public abstract class AbstractCarMojo
    extends MojoSupport
{
    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * The basedir of the project.
     *
     * @parameter expression="${basedir}"
     * @required
     * @readonly
     */
    protected File basedir;

    /**
     * The maven project's helper.
     *
     * @component
     * @required
     * @readonly
     */
    protected MavenProjectHelper projectHelper;

    //
    // MojoSupport Hooks
    //

    protected MavenProject getProject() {
        return project;
    }

    /**
     * @component
     * @required
     * @readonly
     */
    private ArtifactFactory artifactFactory = null;

    protected ArtifactFactory getArtifactFactory() {
        return artifactFactory;
    }

    /**
     * @component
     * @required
     * @readonly
     */
    private ArtifactResolver artifactResolver = null;

    protected ArtifactResolver getArtifactResolver() {
        return artifactResolver;
    }

    /**
     * @parameter expression="${localRepository}"
     * @readonly
     * @required
     */
    protected ArtifactRepository artifactRepository = null;

    protected ArtifactRepository getArtifactRepository() {
        return artifactRepository;
    }

    protected void generateExplicitVersionProperties(final File outputFile) throws MojoExecutionException, IOException {
        log.debug("Generating explicit version properties: " + outputFile);

        // Generate explicit_versions for all our dependencies...
        Properties props = new Properties();
        Iterator iter = getProjectArtifacts().iterator();
        while (iter.hasNext()) {
            Artifact artifact = (Artifact)iter.next();
            String name = artifact.getGroupId() + "/" + artifact.getArtifactId() + "//" + artifact.getType();
            String value = artifact.getGroupId() + "/" + artifact.getArtifactId() + "/" + artifact.getVersion() + "/" + artifact.getType();

            log.debug("Setting " + name + "=" + value);
            props.setProperty(name, value);
        }

        BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(outputFile));
        props.store(output, null);
        output.flush();
        output.close();
    }

    protected static File getArchiveFile(final File basedir, final String finalName, String classifier) {
        if (classifier == null) {
            classifier = "";
        }
        else if (classifier.trim().length() > 0 && !classifier.startsWith("-")) {
            classifier = "-" + classifier;
        }

        return new File(basedir, finalName + classifier + ".car");
    }
}
