/*
 * Polyglotter (http://polyglotter.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Polyglotter is free software. Unless otherwise indicated, all code in Polyglotter
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * Polyglotter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.modeler.xsd;

/**
 * The model lexicon for the XSD modeler.
 */
public interface XsdLexicon {
    
    /**
     * The name of the XSD model node. Value is {@value} .
     */
    String MODEL_TYPE_ID = "org.modeshape.modeler.xsd.Xsd";
    
    /**
     * The XSD namespace prefix. Value is {@value} .
     */
    String NAMESPACE_PREFIX = "xs";
    
    /**
     * The XSD namespace prefix appended with the local name separator. Value is {@value} .
     */
    String NAMESPACE_PREFIX_WITH_SEPARATOR = NAMESPACE_PREFIX + ':';
    
    /**
     * The name of the XSD import element. Value is {@value} .
     */
    String IMPORT = NAMESPACE_PREFIX_WITH_SEPARATOR + "import";
    
    /**
     * The name of the XSD include element. Value is {@value} .
     */
    String INCLUDE = NAMESPACE_PREFIX_WITH_SEPARATOR + "include";
    
    /**
     * The name of the XSD schema document node type. Value is {@value} .
     */
    String SCHEMA_DOCUMENT = NAMESPACE_PREFIX_WITH_SEPARATOR + "schemaDocument";
    
    /**
     * The name of the XSD schema location attribute. Value is {@value} .
     */
    String SCHEMA_LOCATION = NAMESPACE_PREFIX_WITH_SEPARATOR + "schemaLocation";
    
    /**
     * The name of the XSD namespace attribute. Value is {@value} .
     */
    String NAMESPACE = NAMESPACE_PREFIX_WITH_SEPARATOR + "namespace";
    
    /**
     * The name of the XSD redefine element. Value is {@value} .
     */
    String REDEFINE = NAMESPACE_PREFIX_WITH_SEPARATOR + "redefine";
    
}
