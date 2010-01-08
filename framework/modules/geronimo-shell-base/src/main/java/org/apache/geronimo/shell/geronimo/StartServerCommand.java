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

package org.apache.geronimo.shell.geronimo;

import java.util.Arrays;
import java.util.List;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.geronimo.cli.CLParserException;
import org.apache.geronimo.cli.daemon.DaemonCLParser;
import org.apache.geronimo.main.Bootstrapper;
/**
 * @version $Rev$ $Date$
 */
@Command(scope = "geronimo", name = "start-server", description = "Start Server")
public class StartServerCommand extends BaseJavaCommand {
     @Option(name = "-q", aliases = { "--quiet" }, description = "Suppress informative and warning messages")
     boolean quiet = false;

     @Option(name = "-v", aliases = { "--verbose" }, description = "Enable verbose output; specify multiple times to increase verbosity")
     boolean verbose = false;

     @Option(name = "-m", aliases = { "--module" }, description = "Start up a specific module by name")
     List<String> startModules;

     @Option(name = "-u", aliases = { "--username" }, description = "Username")
     protected String username = null;

     @Option(name = "-w", aliases = { "--password" }, description = "Password")
     protected String password = null;

     @Option(name = "-s", aliases = { "--server", "--hostname" }, description = "Hostname, default localhost")
     protected String hostname = "localhost";

     @Option(name = "-p", aliases = { "--port" }, description = "Port, default 1099")
     protected int port = 1099;

     @Option(name = "--secure", description = "Use secure channel")
     protected boolean secure = false;

     @Override
     protected Object doExecute() throws Exception {
      // If we are not backgrounding, then add a nice message for the user when ctrl-c gets hit
         if (!background) {
             Runtime.getRuntime().addShutdownHook(new Thread(){
                 public void run(){
                     println("Shutting down Server...");
                   }
             });
         }
        
        DaemonCLParser parser = getCLParser();
        try {
            parser.parse(null);
        } catch (CLParserException e) {
            System.err.println(e.getMessage());
            parser.displayHelp();
        }

        Bootstrapper boot = createBootstrapper();
        boot.execute(parser);
        
        return null;

     }

     protected Bootstrapper createBootstrapper() {
          Bootstrapper boot = new StartServerstrapper(bundleContext,session.getConsole());
          boot.setWaitForStop(false);
          boot.setStartBundles(Arrays.asList("org.apache.geronimo.framework/j2ee-system//car"));
          boot.setLog4jConfigFile("var/log/server-log4j.properties");
          return (Bootstrapper) boot;
     }

     protected DaemonCLParser getCLParser() {
          return new DaemonCLParser(System.out);
     }

}