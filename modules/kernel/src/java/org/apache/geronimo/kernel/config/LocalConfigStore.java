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
package org.apache.geronimo.kernel.config;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.geronimo.gbean.GBean;
import org.apache.geronimo.gbean.GBeanContext;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoFactory;
import org.apache.geronimo.gbean.InvalidConfigurationException;
import org.apache.geronimo.gbean.WaitingException;
import org.apache.geronimo.gbean.jmx.GBeanMBean;

/**
 * Implementation of ConfigurationStore using the local filesystem.
 *
 * @version $Revision: 1.6 $ $Date: 2004/02/13 04:58:16 $
 */
public class LocalConfigStore implements ConfigurationStore, GBean {
    private static final String INDEX_NAME = "index.properties";
    private final File root;
    private final Properties index = new Properties();
    private int maxId;

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoFactory infoFactory = new GBeanInfoFactory(LocalConfigStore.class);
        infoFactory.addAttribute("root", true);
        infoFactory.setConstructor(new String[] {"root"}, new Class[]{File.class});
        GBEAN_INFO = infoFactory.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }

    public LocalConfigStore(File root) {
        this.root = root;
    }

    public void setGBeanContext(GBeanContext context) {
    }

    public void doStart() throws WaitingException, FileNotFoundException, IOException {
        if (!root.isDirectory()) {
            throw new FileNotFoundException("Store root does not exist or is not a directory: " + root);
        }

        index.clear();
        File indexfile = new File(root, INDEX_NAME);
        try {
            index.load(new BufferedInputStream(new FileInputStream(indexfile)));
            for (Iterator i = index.values().iterator(); i.hasNext();) {
                String id = (String) i.next();
                maxId = Math.max(maxId, Integer.parseInt(id));
            }
        } catch (FileNotFoundException e) {
            maxId = 0;
        }
    }

    public void doStop() throws WaitingException {
    }

    public void doFail() {
    }

    private void saveIndex() throws IOException {
        File indexFile = new File(root, INDEX_NAME);
        File tmpFile = File.createTempFile("index", ".tmp", root);
        FileOutputStream fos = new FileOutputStream(tmpFile);
        try {
            BufferedOutputStream os = new BufferedOutputStream(fos);
            index.store(os, null);
            os.close();
            fos = null;
            indexFile.delete();
            tmpFile.renameTo(indexFile);
        } catch (IOException e) {
            if (fos != null) {
                fos.close();
            }
            tmpFile.delete();
            throw e;
        }
    }

    public void install(URL source) throws IOException, InvalidConfigException {
        String newId;
        synchronized (this) {
            newId = Integer.toString(++maxId);
        }
        File bundleRoot = new File(root, newId);
        bundleRoot.mkdir();
        InputStream is = source.openStream();
        try {
            unpack(bundleRoot, is);
        } finally {
            is.close();
        }
        try {
            GBeanMBean config = loadConfig(bundleRoot);
            index.setProperty(config.getAttribute("ID").toString(), newId);
        } catch (Exception e) {
            delete(bundleRoot);
            throw new InvalidConfigException("Unable to get ID from downloaded configuration", e);
        }
        synchronized (this) {
            saveIndex();
        }

    }

    public synchronized GBeanMBean getConfig(URI configID) throws NoSuchConfigException, IOException, InvalidConfigException {
        return loadConfig(getRoot(configID));
    }

    public URL getBaseURL(URI configID) throws NoSuchConfigException {
        File root = getRoot(configID);
        try {
            return root.toURL();
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Unable to turn config root into URL: " + root);
        }
    }

    private synchronized File getRoot(URI configID) throws NoSuchConfigException {
        String id = index.getProperty(configID.toString());
        if (id == null) {
            throw new NoSuchConfigException("No such config: " + configID);
        }
        return new File(root, id);
    }

    private GBeanMBean loadConfig(File configRoot) throws IOException, InvalidConfigException {
        FileInputStream fis = new FileInputStream(new File(configRoot, "META-INF/config.ser"));
        try {
            ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(fis));
            GBeanInfo gbeanInfo = Configuration.GBEAN_INFO;
            GBeanMBean config;
            try {
                config = new GBeanMBean(gbeanInfo);
            } catch (InvalidConfigurationException e) {
                throw new InvalidConfigException("Unable to instantiate Configuration GBeanMBean", e);
            }
            try {
                Configuration.loadGMBeanState(config, ois);
            } catch (ClassNotFoundException e) {
                //TODO more informative exceptions
                throw new InvalidConfigException("Unable to read attribute ", e);
            } catch (Exception e) {
                throw new InvalidConfigException("Unable to set attribute ", e);
            }
            return config;
        } finally {
            fis.close();
        }
    }

    public static void unpack(File to, InputStream from) throws IOException {
        ZipInputStream zis = new ZipInputStream(from);
        try {
            ZipEntry entry;
            byte[] buffer = new byte[4096];
            while ((entry = zis.getNextEntry()) != null) {
                File out = new File(to, entry.getName());
                if (entry.isDirectory()) {
                    out.mkdirs();
                } else {
                    out.getParentFile().mkdirs();
                    OutputStream os = new FileOutputStream(out);
                    try {
                        int count;
                        while ((count = zis.read(buffer)) > 0) {
                            os.write(buffer, 0, count);
                        }
                    } finally {
                        os.close();
                    }
                    zis.closeEntry();
                }
            }
        } catch (IOException e) {
            delete(to);
            throw e;
        }
    }

    private static void delete(File root) throws IOException {
        File[] files = root.listFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isDirectory()) {
                delete(file);
            } else {
                file.delete();
            }
        }
        root.delete();
    }
}
