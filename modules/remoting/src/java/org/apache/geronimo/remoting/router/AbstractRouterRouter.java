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
package org.apache.geronimo.remoting.router;

import java.net.URI;

import org.apache.geronimo.kernel.service.GeronimoMBeanContext;
import org.apache.geronimo.kernel.service.GeronimoMBeanTarget;
import org.apache.geronimo.remoting.transport.Msg;
import org.apache.geronimo.remoting.transport.TransportException;

import EDU.oswego.cs.dl.util.concurrent.Latch;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.TimeoutSync;

/**
 *
 * @version $Revision: 1.2 $ $Date: 2003/11/19 11:15:03 $
 */
abstract public class AbstractRouterRouter
    implements GeronimoMBeanTarget, Router
{

    private long stoppedRoutingTimeout = 1000 * 60; // 1 min.

    /** 
     * Allows us to pause invocations when in the stopped state.
     */
    private Sync routerLock = createNewRouterLock();

    /**
     *
     * @jmx:managed-attribute
     *
     * @return
     */
    public long getStoppedRoutingTimeout() {
        return stoppedRoutingTimeout;
    }

    /**
     *
     * @jmx:managed-attribute
     *
     * @param stoppedRoutingTimeout
     */
    public void setStoppedRoutingTimeout(long stoppedRoutingTimeout) {
        this.stoppedRoutingTimeout = stoppedRoutingTimeout;
    }

    /**
     * @return
     */
    private Sync createNewRouterLock() {
        Latch lock = new Latch();
        return new TimeoutSync(lock, stoppedRoutingTimeout);
    }

    /**
     *
     *
     * @see org.apache.geronimo.remoting.transport.Router#sendRequest(java.net.URI, org.apache.geronimo.remoting.transport.Msg)
     */
    public Msg sendRequest(URI to, Msg msg) throws TransportException {
        try {
            routerLock.acquire();
            Router next = lookupRouterFrom(to);
            if( next == null )
                throw new TransportException("No route is available to: "+to);
                
            return next.sendRequest(to, msg);

        } catch (Throwable e) {
            e.printStackTrace();
            throw new TransportException(e.getMessage());
        }
    }

    /**
     * @see org.apache.geronimo.remoting.transport.Router#sendDatagram(java.net.URI, org.apache.geronimo.remoting.transport.Msg)
     */
    public void sendDatagram(URI to, Msg msg) throws TransportException {
        try {
            routerLock.acquire();
            Router next = lookupRouterFrom(to);
            next.sendDatagram(to, msg);
        } catch (Throwable e) {
            throw new TransportException(e.getMessage());
        }
    }

    /**
     * @param to
     * @return
     */
    abstract protected Router lookupRouterFrom(URI to);
        
    
    /**
     * @see org.apache.geronimo.kernel.service.GeronimoMBeanTarget#setMBeanContext(org.apache.geronimo.kernel.service.GeronimoMBeanContext)
     */
    public void setMBeanContext(GeronimoMBeanContext context) {
    }

    /**
     * @see org.apache.geronimo.core.service.AbstractManagedObject#doStart()
     */
    public void doStart() {
        routerLock.release();
    }

    /**
     * @see org.apache.geronimo.core.service.AbstractManagedObject#doStop()
     */
    public void doStop() {
        routerLock = createNewRouterLock();
    }

    /**
     * @see org.apache.geronimo.kernel.service.GeronimoMBeanTarget#canStart()
     */
    public boolean canStart() {
        return true;
    }

    /**
     * @see org.apache.geronimo.kernel.service.GeronimoMBeanTarget#canStop()
     */
    public boolean canStop() {
        return true;
    }

    /**
     * @see org.apache.geronimo.kernel.service.GeronimoMBeanTarget#doFail()
     */
    public void doFail() {
    }

}
