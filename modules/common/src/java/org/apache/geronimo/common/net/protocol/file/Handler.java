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

package org.apache.geronimo.common.net.protocol.file;

import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import sun.net.www.ParseUtil;

/**
 * A protocol handler for the 'file' protocol.
 *
 * @version $Revision: 1.1 $ $Date: 2003/09/01 15:18:25 $
 */
public class Handler
    extends URLStreamHandler
{
    protected void parseURL(final URL url, final String s, final int i, final int j)
    {
        super.parseURL(url, s.replace(File.separatorChar, '/'), i, j);
    }
    
    /**
     * Open a connection to the file.
     *
     * <p>NOTE: Sun's impl attempts to translate into a 'ftp' URL which is dumb
     *          so fix that by removing it ;-)
     */
    public URLConnection openConnection(final URL url)
        throws IOException
    {
        String path = ParseUtil.decode(url.getPath());
        path = path.replace('/', File.separatorChar).replace('|', ':');
        File file = new File(path);
        
        // Handle the hostname of the URL if given, puke if not valid
        String hostname = url.getHost();
        if (hostname == null ||
            hostname.equals("") ||
            hostname.equals("~") || 
            hostname.equals("localhost") ||
            file.exists())
        {
            return new FileURLConnection(url, file);
        }
        
        throw new FileNotFoundException("Invalid host specification: " + url);
    }
}
