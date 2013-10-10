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

import java.net.URL;

import javax.jcr.Node;
import javax.jcr.Session;

import org.modeshape.modeler.Model;
import org.modeshape.modeler.ModelType;
import org.modeshape.modeler.ModelerException;

/**
 * 
 */
public class ModelImpl extends ModelObjectImpl implements Model {
    
    /**
     * @param manager
     *        the ModeShapeModeler's manager
     * @param modelPath
     *        a path to a model
     */
    public ModelImpl( final Manager manager,
                      final String modelPath ) {
        super( manager, modelPath, -1 );
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.Model#externalLocation()
     */
    @Override
    public URL externalLocation() throws ModelerException {
        return manager.run( new Task< URL >() {
            
            @Override
            public URL run( final Session session ) throws Exception {
                final Node model = session.getNode( path );
                return model.hasProperty( ModelerLexicon.EXTERNAL_LOCATION ) ? new URL( model.getProperty( ModelerLexicon.EXTERNAL_LOCATION ).getString() )
                                                                            : null;
            }
        } );
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.Model#modelType()
     */
    @Override
    public ModelType modelType() throws ModelerException {
        return manager.run( new Task< ModelType >() {
            
            @Override
            public ModelType run( final Session session ) throws Exception {
                return manager.modelTypeManager.modelType( session.getNode( path ).getProperty( ModelerLexicon.MODEL_TYPE ).getString() );
            }
        } );
    }
}
