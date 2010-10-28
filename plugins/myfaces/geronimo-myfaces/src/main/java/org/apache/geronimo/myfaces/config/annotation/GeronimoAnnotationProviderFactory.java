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

package org.apache.geronimo.myfaces.config.annotation;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import javax.faces.context.ExternalContext;

import org.apache.myfaces.spi.AnnotationProvider;
import org.apache.myfaces.spi.AnnotationProviderFactory;

/**
 * @version $Rev$ $Date$
 */
public class GeronimoAnnotationProviderFactory extends AnnotationProviderFactory {

    private AnnotationProvider defaultAnnotationProvider;

    public GeronimoAnnotationProviderFactory(final Map<Class<? extends Annotation>, Set<Class<?>>> annotationClassSetMap) {
        this.defaultAnnotationProvider = new AnnotationProvider() {

            @Override
            public Map<Class<? extends Annotation>, Set<Class<?>>> getAnnotatedClasses(ExternalContext arg0) {
                return annotationClassSetMap;
            }

            @Override
            public Set<URL> getBaseUrls() throws IOException {
                return null;
            }

        };
    }

    @Override
    public AnnotationProvider createAnnotationProvider(ExternalContext externalContext) {

        return defaultAnnotationProvider;
    }
}
