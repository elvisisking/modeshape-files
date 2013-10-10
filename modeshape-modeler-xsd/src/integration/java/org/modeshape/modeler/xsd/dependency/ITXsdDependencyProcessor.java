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
package org.modeshape.modeler.xsd.dependency;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.net.URL;

import javax.jcr.Node;
import javax.jcr.Session;

import org.junit.Test;
import org.modeshape.modeler.ModelType;
import org.modeshape.modeler.integration.BaseIntegrationTest;
import org.modeshape.modeler.internal.DependencyProcessor;
import org.modeshape.modeler.internal.ModelImpl;
import org.modeshape.modeler.internal.ModelerLexicon;
import org.modeshape.modeler.internal.Task;
import org.modeshape.modeler.xsd.XsdLexicon;

/**
 * An integration test for the {@link XsdDependencyProcessor}.
 */
@SuppressWarnings( "javadoc" )
public class ITXsdDependencyProcessor extends BaseIntegrationTest {

    DependencyProcessor processor;

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.test.BaseTest#before()
     */
    @Override
    public void before() throws Exception {
        super.before();
        this.processor = new XsdDependencyProcessor();
        modelTypeManager().install( "sramp" );
        modelTypeManager().install( "xsd" );
    }

    @Test
    public void shouldNotFindDependenciesInMusicXsd() throws Exception {
        final URL xsdUrl = getClass().getClassLoader().getResource( "music.xsd" );
        final String path = modeler().importFile( new File( xsdUrl.toURI() ), null );
        assertThat( path, is( "/music.xsd" ) );

        final ModelType xsdModelType = xsdModelType();
        final ModelImpl model = ( ModelImpl ) modeler().generateModel( path, ARTIFACT_NAME, xsdModelType );

        manager().run( new Task< Node >() {

            @Override
            public Node run( final Session session ) throws Exception {
                final Node modelNode = session.getNode( model.absolutePath() );
                final String dependenciesPath = processor.process( modelNode, xsdModelType, modeler() );
                assertThat( dependenciesPath, nullValue() );

                return null;
            }
        } );
    }

    @Test
    public void shouldSetDependencyPathsOfBooksXsd() throws Exception {
        final URL xsdUrl = getClass().getClassLoader().getResource( "Books/Books.xsd" );
        final String path = modeler().importFile( new File( xsdUrl.toURI() ), null );
        assertThat( path, is( "/Books.xsd" ) );

        final ModelType xsdModelType = xsdModelType();
        final ModelImpl model = ( ModelImpl ) modeler().generateModel( path, ARTIFACT_NAME, xsdModelType );

        manager().run( new Task< Node >() {

            @Override
            public Node run( final Session session ) throws Exception {
                final Node modelNode = session.getNode( model.absolutePath() );
                final String dependenciesPath = processor.process( modelNode, xsdModelType, modeler() );
                assertThat( dependenciesPath, notNullValue() );

                final Node dependenciesNode = session.getNode( dependenciesPath );
                assertThat( dependenciesNode.getNodes().getSize(), is( 1L ) );

                final Node dependencyNode = dependenciesNode.getNodes().nextNode();
                assertThat( dependencyNode.getPrimaryNodeType().getName(), is( ModelerLexicon.DEPENDENCY_NODE ) );
                assertThat( dependencyNode.getProperty( ModelerLexicon.PATH_PROPERTY ).getString(), is( "/data/types/BookDatatypes.xsd" ) );

                final String input =
                    dependencyNode.getProperty( ModelerLexicon.SOURCE_REFERENCE_PROPERTY ).getValues()[ 0 ].getString();
                assertThat( input, is( "../data/types/BookDatatypes.xsd" ) );

                return null;
            }
        } );
    }

    @Test
    public void shouldSetDependencyPathsOfMoviesXsd() throws Exception {
        final URL xsdUrl = getClass().getClassLoader().getResource( "Movies/Movies.xsd" );
        final String path = modeler().importFile( new File( xsdUrl.toURI() ), null );
        assertThat( path, is( "/Movies.xsd" ) );

        final ModelType xsdModelType = xsdModelType();
        final ModelImpl model = ( ModelImpl ) modeler().generateModel( path, ARTIFACT_NAME, xsdModelType );

        manager().run( new Task< Node >() {

            @Override
            public Node run( final Session session ) throws Exception {
                final Node modelNode = session.getNode( model.absolutePath() );
                final String dependenciesPath = processor.process( modelNode, xsdModelType, modeler() );
                assertThat( dependenciesPath, notNullValue() );

                final Node dependenciesNode = session.getNode( dependenciesPath );
                assertThat( dependenciesNode.getNodes().getSize(), is( 1L ) );

                final Node dependencyNode = dependenciesNode.getNodes().nextNode();
                assertThat( dependencyNode.getPrimaryNodeType().getName(), is( ModelerLexicon.DEPENDENCY_NODE ) );
                assertThat( dependencyNode.getProperty( ModelerLexicon.PATH_PROPERTY ).getString(), is( model.absolutePath() + "/MovieDatatypes.xsd" ) );

                final String input =
                    dependencyNode.getProperty( ModelerLexicon.SOURCE_REFERENCE_PROPERTY ).getValues()[ 0 ].getString();
                assertThat( input, is( "MovieDatatypes.xsd" ) );

                return null;
            }
        } );

    }

    private ModelType xsdModelType() throws Exception {
        ModelType xsdModelType = null;

        for ( final ModelType type : modelTypeManager().modelTypes() ) {
            if ( type.name().equals( XsdLexicon.MODEL_ID ) ) {
                xsdModelType = type;
                break;
            }
        }

        assertThat( xsdModelType, notNullValue() );
        return xsdModelType;
    }

}
