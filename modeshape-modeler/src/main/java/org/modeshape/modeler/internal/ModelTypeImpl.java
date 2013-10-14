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
package org.modeshape.modeler.internal;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.Session;

import org.infinispan.util.ReflectionUtil;
import org.modeshape.jcr.ExtensionLogger;
import org.modeshape.jcr.api.Repository;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.modeler.ModelType;
import org.modeshape.modeler.ModelerException;
import org.modeshape.modeler.extensions.DependencyProcessor;
import org.modeshape.modeler.extensions.Desequencer;

/**
 * 
 */
public final class ModelTypeImpl implements ModelType {

    private final Manager manager;
    final Class< ? > sequencerClass;
    final Class< ? > desequencerClass;
    private final String category;
    private final String name;
    private final Set< String > sourceFileExtensions = new HashSet<>();

    private DependencyProcessor dependencyProcessor;
    private Class< DependencyProcessor > dependencyProcessorClass;

    ModelTypeImpl( final Manager manager,
                   final String category,
                   final String name,
                   final Class< ? > sequencerClass,
                   final Class< ? > desequencerClass ) {
        this.manager = manager;
        this.category = category;
        this.name = name;
        this.sequencerClass = sequencerClass;
        this.desequencerClass = desequencerClass;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelType#category()
     */
    @Override
    public String category() {
        return category;
    }

    /**
     * @return the dependency processor or <code>null</code> if one does not exist
     * @throws ModelerException
     *         if there is an error constructing the dependency processor
     */
    public DependencyProcessor dependencyProcessor() throws ModelerException {
        if ( ( this.dependencyProcessor == null ) && ( this.dependencyProcessorClass != null ) ) {
            try {
                this.dependencyProcessor = this.dependencyProcessorClass.newInstance();
            } catch ( final Exception e ) {
                throw new ModelerException( e );
            }
        }

        return this.dependencyProcessor;
    }

    /**
     * @return this model type's desequencer
     * @throws ModelerException
     *         if any problem occurs
     */
    public Desequencer desequencer() throws ModelerException {
        return manager.run( new Task< Desequencer >() {

            @Override
            public Desequencer run( final Session session ) throws Exception {
                final Desequencer desequencer = ( Desequencer ) desequencerClass.newInstance();
                // Initialize
                ReflectionUtil.setValue( desequencer, "logger", ExtensionLogger.getLogger( desequencer.getClass() ) );
                ReflectionUtil.setValue( desequencer,
                                         "repositoryName",
                                         session.getRepository().getDescriptor( Repository.REPOSITORY_NAME ) );
                ReflectionUtil.setValue( desequencer, "name", desequencer.getClass().getSimpleName() );
                return desequencer;
            }
        } );
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelType#name()
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * @return this model type's sequencer
     * @throws ModelerException
     *         if any problem occurs
     */
    public Sequencer sequencer() throws ModelerException {
        return manager.run( new Task< Sequencer >() {

            @Override
            public Sequencer run( final Session session ) throws Exception {
                final Sequencer sequencer = ( Sequencer ) sequencerClass.newInstance();
                // Initialize
                ReflectionUtil.setValue( sequencer, "logger", ExtensionLogger.getLogger( sequencer.getClass() ) );
                ReflectionUtil.setValue( sequencer, "repositoryName",
                                         session.getRepository().getDescriptor( Repository.REPOSITORY_NAME ) );
                ReflectionUtil.setValue( sequencer, "name", sequencer.getClass().getSimpleName() );
                sequencer.initialize( session.getWorkspace().getNamespaceRegistry(),
                                      ( NodeTypeManager ) session.getWorkspace().getNodeTypeManager() );
                return sequencer;
            }
        } );
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.ModelType#sourceFileExtensions()
     */
    @Override
    public String[] sourceFileExtensions() {
        return sourceFileExtensions.toArray( new String[ sourceFileExtensions.size() ] );
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return name + " [ category = " + category + ']';
    }
}
