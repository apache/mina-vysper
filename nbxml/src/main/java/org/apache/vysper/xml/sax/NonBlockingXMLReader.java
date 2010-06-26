/*
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
 *
 */
package org.apache.vysper.xml.sax;

import java.io.IOException;
import java.nio.charset.CharsetDecoder;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.vysper.xml.sax.impl.XMLParser;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public interface NonBlockingXMLReader {

    ////////////////////////////////////////////////////////////////////
    // Configuration.
    ////////////////////////////////////////////////////////////////////

    /**
     * Look up the value of a feature flag.
     *
     * <p>The feature name is any fully-qualified URI.  It is
     * possible for an XMLReader to recognize a feature name but
     * temporarily be unable to return its value.
     * Some feature values may be available only in specific
     * contexts, such as before, during, or after a parse.
     * Also, some feature values may not be programmatically accessible.
     * (In the case of an adapter for SAX1 {@link XMLParser}, there is no
     * implementation-independent way to expose whether the underlying
     * parser is performing validation, expanding external entities,
     * and so forth.) </p>
     *
     * <p>All XMLReaders are required to recognize the
     * http://xml.org/sax/features/namespaces and the
     * http://xml.org/sax/features/namespace-prefixes feature names.</p>
     *
     * <p>Typical usage is something like this:</p>
     *
     * <pre>
     * XMLReader r = new MySAXDriver();
     *
     *                         // try to activate validation
     * try {
     *   r.setFeature("http://xml.org/sax/features/validation", true);
     * } catch (SAXException e) {
     *   System.err.println("Cannot activate validation."); 
     * }
     *
     *                         // register event handlers
     * r.setContentHandler(new MyContentHandler());
     * r.setErrorHandler(new MyErrorHandler());
     *
     *                         // parse the first document
     * try {
     *   r.parse("http://www.foo.com/mydoc.xml");
     * } catch (IOException e) {
     *   System.err.println("I/O exception reading XML document");
     * } catch (SAXException e) {
     *   System.err.println("XML exception reading document.");
     * }
     * </pre>
     *
     * <p>Implementors are free (and encouraged) to invent their own features,
     * using names built on their own URIs.</p>
     *
     * @param name The feature name, which is a fully-qualified URI.
     * @return The current value of the feature (true or false).
     * @exception org.xml.sax.SAXNotRecognizedException If the feature
     *            value can't be assigned or retrieved.
     * @exception org.xml.sax.SAXNotSupportedException When the
     *            XMLReader recognizes the feature name but 
     *            cannot determine its value at this time.
     * @see #setFeature
     */
    public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException;

    /**
     * Set the value of a feature flag.
     *
     * <p>The feature name is any fully-qualified URI.  It is
     * possible for an XMLReader to expose a feature value but
     * to be unable to change the current value.
     * Some feature values may be immutable or mutable only 
     * in specific contexts, such as before, during, or after 
     * a parse.</p>
     *
     * <p>All XMLReaders are required to support setting
     * http://xml.org/sax/features/namespaces to true and
     * http://xml.org/sax/features/namespace-prefixes to false.</p>
     *
     * @param name The feature name, which is a fully-qualified URI.
     * @param value The requested value of the feature (true or false).
     * @exception org.xml.sax.SAXNotRecognizedException If the feature
     *            value can't be assigned or retrieved.
     * @exception org.xml.sax.SAXNotSupportedException When the
     *            XMLReader recognizes the feature name but 
     *            cannot set the requested value.
     * @see #getFeature
     */
    public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException;

    /**
     * Look up the value of a property.
     *
     * <p>The property name is any fully-qualified URI.  It is
     * possible for an XMLReader to recognize a property name but
     * temporarily be unable to return its value.
     * Some property values may be available only in specific
     * contexts, such as before, during, or after a parse.</p>
     *
     * <p>XMLReaders are not required to recognize any specific
     * property names, though an initial core set is documented for
     * SAX2.</p>
     *
     * <p>Implementors are free (and encouraged) to invent their own properties,
     * using names built on their own URIs.</p>
     *
     * @param name The property name, which is a fully-qualified URI.
     * @return The current value of the property.
     * @exception org.xml.sax.SAXNotRecognizedException If the property
     *            value can't be assigned or retrieved.
     * @exception org.xml.sax.SAXNotSupportedException When the
     *            XMLReader recognizes the property name but 
     *            cannot determine its value at this time.
     * @see #setProperty
     */
    public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException;

    /**
     * Set the value of a property.
     *
     * <p>The property name is any fully-qualified URI.  It is
     * possible for an XMLReader to recognize a property name but
     * to be unable to change the current value.
     * Some property values may be immutable or mutable only 
     * in specific contexts, such as before, during, or after 
     * a parse.</p>
     *
     * <p>XMLReaders are not required to recognize setting
     * any specific property names, though a core set is defined by 
     * SAX2.</p>
     *
     * <p>This method is also the standard mechanism for setting
     * extended handlers.</p>
     *
     * @param name The property name, which is a fully-qualified URI.
     * @param value The requested value for the property.
     * @exception org.xml.sax.SAXNotRecognizedException If the property
     *            value can't be assigned or retrieved.
     * @exception org.xml.sax.SAXNotSupportedException When the
     *            XMLReader recognizes the property name but 
     *            cannot set the requested value.
     */
    public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException;

    ////////////////////////////////////////////////////////////////////
    // Event handlers.
    ////////////////////////////////////////////////////////////////////

    /**
     * Allow an application to register an entity resolver.
     *
     * <p>If the application does not register an entity resolver,
     * the XMLReader will perform its own default resolution.</p>
     *
     * <p>Applications may register a new or different resolver in the
     * middle of a parse, and the SAX parser must begin using the new
     * resolver immediately.</p>
     *
     * @param resolver The entity resolver.
     * @see #getEntityResolver
     */
    public void setEntityResolver(EntityResolver resolver);

    /**
     * Return the current entity resolver.
     *
     * @return The current entity resolver, or null if none
     *         has been registered.
     * @see #setEntityResolver
     */
    public EntityResolver getEntityResolver();

    /**
     * Allow an application to register a DTD event handler.
     *
     * <p>If the application does not register a DTD handler, all DTD
     * events reported by the SAX parser will be silently ignored.</p>
     *
     * <p>Applications may register a new or different handler in the
     * middle of a parse, and the SAX parser must begin using the new
     * handler immediately.</p>
     *
     * @param handler The DTD handler.
     * @see #getDTDHandler
     */
    public void setDTDHandler(DTDHandler handler);

    /**
     * Return the current DTD handler.
     *
     * @return The current DTD handler, or null if none
     *         has been registered.
     * @see #setDTDHandler
     */
    public DTDHandler getDTDHandler();

    /**
     * Allow an application to register a content event handler.
     *
     * <p>If the application does not register a content handler, all
     * content events reported by the SAX parser will be silently
     * ignored.</p>
     *
     * <p>Applications may register a new or different handler in the
     * middle of a parse, and the SAX parser must begin using the new
     * handler immediately.</p>
     *
     * @param handler The content handler.
     * @see #getContentHandler
     */
    public void setContentHandler(ContentHandler handler);

    /**
     * Return the current content handler.
     *
     * @return The current content handler, or null if none
     *         has been registered.
     * @see #setContentHandler
     */
    public ContentHandler getContentHandler();

    /**
     * Allow an application to register an error event handler.
     *
     * <p>If the application does not register an error handler, all
     * error events reported by the SAX parser will be silently
     * ignored; however, normal processing may not continue.  It is
     * highly recommended that all SAX applications implement an
     * error handler to avoid unexpected bugs.</p>
     *
     * <p>Applications may register a new or different handler in the
     * middle of a parse, and the SAX parser must begin using the new
     * handler immediately.</p>
     *
     * @param handler The error handler.
     * @see #getErrorHandler
     */
    public void setErrorHandler(ErrorHandler handler);

    /**
     * Return the current error handler.
     *
     * @return The current error handler, or null if none
     *         has been registered.
     * @see #setErrorHandler
     */
    public ErrorHandler getErrorHandler();

    /**
     * Parse XML in non-blocking mode. Issues events to the registered {@link ContentHandler} and {@link ErrorHandler}
     * @param buffer Buffer containing XML input data 
     * @param decoder The charset decoder to use for parsing
     * @throws IOException
     * @throws SAXException
     */
    public void parse(IoBuffer buffer, CharsetDecoder decoder) throws IOException, SAXException;

}