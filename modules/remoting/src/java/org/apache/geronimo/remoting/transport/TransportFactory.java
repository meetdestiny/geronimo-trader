/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" and
 *    "Apache Geronimo" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    "Apache Geronimo", nor may "Apache" appear in their name, without
 *    prior written permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * ====================================================================
 */
package org.apache.geronimo.remoting.transport;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * @version $Revision: 1.2 $ $Date: 2003/11/19 11:15:03 $
 */
public abstract class TransportFactory {

    private static ArrayList factories = new ArrayList();
    static {
        // Try our best to add a few known TransportFactory objects.
        // We may not be able to load due to ClassNotFound problems.
        try {
            addFactory(loadFactory("async"));
        } catch (Throwable ignore) {
        }
    }
    
    static private TransportFactory loadFactory(String type) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        String className = "org.apache.geronimo.remoting.transport."+type+".TransportFactory";
        return (TransportFactory) Thread.currentThread().getContextClassLoader().loadClass(className).newInstance();
    }

    public static TransportFactory getTransportFactory(URI uri) {
        Iterator iterator = factories.iterator();
        while (iterator.hasNext()) {
            TransportFactory i = (TransportFactory) iterator.next();
            if (i.handles(uri))
                return i;
        }

        // Not found.. see if we can dynamicly load a new Factory for it based on scheme.        
        try {
            TransportFactory factory = loadFactory(uri.getScheme());
            if (factory.handles(uri)) {
                addFactory(factory);
                return factory;                
            }
        } catch (Throwable ignore) {
        }
        return null;

    }

    public static boolean unexport(Object object) {
        boolean wasExported = false;
        Iterator iterator = factories.iterator();
        while (iterator.hasNext()) {
            TransportFactory i = (TransportFactory) iterator.next();
            if( i.doUnexport(object) )
                wasExported = true;
        }
        return wasExported;
    }

    static public void addFactory(TransportFactory tf) {
        factories.add(tf);
    }

    static public void removeFactory(TransportFactory tf) {
        factories.remove(tf);
    }

    abstract protected boolean handles(URI uri);
    abstract public TransportClient createClient();
    abstract public TransportServer createSever();
    abstract public boolean doUnexport(Object object);

}
