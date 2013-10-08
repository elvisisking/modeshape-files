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
package org.modeshape.modeler;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import javax.jcr.Session;

import org.junit.Test;
import org.modeshape.modeler.integration.BaseIntegrationTest;
import org.modeshape.modeler.internal.ModelImpl;
import org.modeshape.modeler.internal.Task;

@SuppressWarnings( "javadoc" )
public class ITModeler extends BaseIntegrationTest {
    
    private static final String XSD_MODEL_TYPE_NAME = "org.modeshape.modeler.xsd.Xsd";
    
    @Test
    public void shouldCreateModelOfSuppliedType() throws Exception {
        modelTypeManager.install( "xml" );
        modelTypeManager.install( "sramp" );
        modelTypeManager.install( "xsd" );
        final String path = importArtifact( XSD_ARTIFACT );
        ModelType modelType = null;
        for ( final ModelType type : modelTypeManager.modelTypesForArtifact( path ) ) {
            if ( type.name().equals( XSD_MODEL_TYPE_NAME ) ) {
                modelType = type;
                break;
            }
        }
        modeler.createModel( path, modelType );
        manager.run( new Task< Void >() {
            
            @Override
            public Void run( final Session session ) throws Exception {
                assertThat( session.getNode( path ).hasNode( XSD_MODEL_TYPE_NAME ), is( true ) );
                return null;
            }
        } );
    }
    
    @Test( expected = ModelerException.class )
    public void shouldFailToCreateDefaultModelIfFileIsInvalid() throws Exception {
        modelTypeManager.install( "xml" );
        modeler.createDefaultModel( importArtifact( XML_DECLARATION + "<stuff>" ) );
    }
    
    @Test( expected = ModelerException.class )
    public void shouldFailToCreateModelIfFileIsInvalid() throws Exception {
        modelTypeManager.install( "xml" );
        modeler.createModel( importArtifact( XML_DECLARATION + "<stuff>" ), modelTypeManager.modelTypes().iterator().next() );
    }
    
    @Test( expected = ModelerException.class )
    public void shouldFailToCreateModelIfTypeIsInapplicable() throws Exception {
        modelTypeManager.install( "xml" );
        modeler.createModel( importArtifact( "stuff" ), modelTypeManager.modelTypes().iterator().next() );
    }
    
    @Test
    public void shouldNotFindDependencyProcessorForXsdModelNode() throws Exception {
        modelTypeManager.install( "sramp" );
        modelTypeManager.install( "xsd" );
        
        // find XSD model type
        ModelType xsdModelType = null;
        
        for ( final ModelType type : modelTypeManager.modelTypes() ) {
            if ( type.name().equals( XSD_MODEL_TYPE_NAME ) ) {
                xsdModelType = type;
                break;
            }
        }
        
        assertThat( xsdModelType, notNullValue() );
        
        final String path = importArtifact( XSD_ARTIFACT );
        final ModelImpl model = ( ModelImpl ) modeler.createModel( path, xsdModelType );
        modeler.manager.run( new Task< Void >() {
            
            @Override
            public Void run( final Session session ) throws Exception {
                assertThat( modelTypeManager.dependencyProcessor( session.getNode( model.path() ) ), nullValue() );
                return null;
            }
        } );
    }
    
}
