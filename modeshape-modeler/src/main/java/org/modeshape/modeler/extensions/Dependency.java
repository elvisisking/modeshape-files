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
package org.modeshape.modeler.extensions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.modeshape.common.util.CheckArg;

/**
 * A model dependency.
 */
public class Dependency {

    private static final List< String > NO_SRC_REFS = Collections.emptyList();

    private final boolean exists;

    private final String path;

    private final List< String > sourceReferences;

    /**
     * @param path
     *        the workspace full path of the dependency (can be <code>null</code> or empty)
     * @param exists
     *        <code>true</code> if the dependency exists in the workspace
     */
    public Dependency( final String path,
                       final boolean exists ) {
        this.path = path;
        this.exists = exists;
        this.sourceReferences = new ArrayList< String >( 5 );
    }

    /**
     * @param sourceReference
     *        the source reference being added (cannot be <code>null</code> or empty)
     */
    public void addSourceReference( final String sourceReference ) {
        CheckArg.isNotEmpty( sourceReference, "sourceReference" );
        this.sourceReferences.add( sourceReference );
    }

    /**
     * @return <code>true</code> if exists in the workspace
     */
    public boolean exists() {
        return this.exists;
    }

    /**
     * @return the workspace path (can be <code>null</code> or empty if not known)
     */
    public String path() {
        return this.path;
    }

    /**
     * @return a collection of inputs used when creating this dependency (never <code>null</code> but can be empty)
     */
    public List< String > sourceReferences() {
        if ( ( this.sourceReferences == null ) || this.sourceReferences.isEmpty() ) {
            return NO_SRC_REFS;
        }

        return this.sourceReferences;
    }

}
