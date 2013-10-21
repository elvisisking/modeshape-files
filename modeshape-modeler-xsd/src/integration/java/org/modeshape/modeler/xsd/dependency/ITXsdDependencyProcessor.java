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
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

import org.junit.Test;
import org.modeshape.modeler.ModelType;
import org.modeshape.modeler.extensions.DependencyProcessor;
import org.modeshape.modeler.integration.BaseIntegrationTest;
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
        final ModelImpl model = ( ModelImpl ) modeler().generateModel( path, MODEL_NAME, xsdModelType );

        manager().run( new Task< Node >() {

            @Override
            public Node run( final Session session ) throws Exception {
                final Node modelNode = session.getNode( model.absolutePath() );
                final String dependenciesPath = processor.process( path, modelNode, modeler() );
                assertThat( dependenciesPath, nullValue() );

                return null;
            }
        } );
    }

    @Test
    public void shouldProcessBooksSoapEncodingXsd() throws Exception {
        final URL xsdUrl = getClass().getClassLoader().getResource( "Books/SOAP/BooksWithSOAPEncoding.xsd" );
        final String artifactPath = modeler().importFile( new File( xsdUrl.toURI() ), "Artifact/Books/SOAP" );
        assertThat( artifactPath, is( "/Artifact/Books/SOAP/BooksWithSOAPEncoding.xsd" ) );

        final ModelType xsdModelType = xsdModelType();
        final String modelPath = "Model/Books/SOAP/BooksWithSOAPEncoding.xsd";
        final ModelImpl model = ( ModelImpl ) modeler().generateModel( artifactPath, modelPath, xsdModelType );

        manager().run( new Task< Node >() {

            @Override
            public Node run( final Session session ) throws Exception {
                final Node modelNode = session.getNode( model.absolutePath() );
                final String dependenciesPath = processor.process( artifactPath, modelNode, modeler() );
                assertThat( dependenciesPath, notNullValue() );

                final Node dependenciesNode = session.getNode( dependenciesPath );
                assertThat( dependenciesNode.getNodes().getSize(), is( 2L ) );

                final NodeIterator itr = dependenciesNode.getNodes();

                final String dataTypesArtifactPath = "/Artifact/Books/data/types/BookDatatypes.xsd";
                final String dataTypesModelPath = "/Model/Books/data/types/BookDatatypes.xsd";
                final String dataTypesSourceRef = "../data/types/BookDatatypes.xsd";

                final String soapEncodingArtifactPath = "/Artifact/Books/SOAP/encoding/soap_encoding.xsd";
                final String soapEncodingModelPath = "/Model/Books/SOAP/encoding/soap_encoding.xsd";
                final String soapEncodingSourceRef = "./encoding/soap_encoding.xsd";

                final Node depOneNode = itr.nextNode();
                assertThat( depOneNode.getPrimaryNodeType().getName(), is( ModelerLexicon.DEPENDENCY ) );

                final Node depTwoNode = itr.nextNode();
                assertThat( depOneNode.getPrimaryNodeType().getName(), is( ModelerLexicon.DEPENDENCY ) );

                if ( depOneNode.getProperty( ModelerLexicon.PATH ).getString().equals( dataTypesModelPath ) ) {
                    { // first dependency node is datatypes
                        final String input =
                            depOneNode.getProperty( ModelerLexicon.SOURCE_REFERENCE_PROPERTY ).getValues()[ 0 ].getString();
                        assertThat( input, is( dataTypesSourceRef ) );

                        // make sure dependency resource was imported and model created (getNode throws exception if path not found)
                        session.getNode( dataTypesArtifactPath );
                        session.getNode( dataTypesModelPath );
                    }

                    { // dependency two must be soap encoding
                        final String input =
                            depTwoNode.getProperty( ModelerLexicon.SOURCE_REFERENCE_PROPERTY ).getValues()[ 0 ].getString();
                        assertThat( input, is( soapEncodingSourceRef ) );

                        // make sure dependency resource was imported and model created (getNode throws exception if path not found)
                        session.getNode( soapEncodingArtifactPath );
                        session.getNode( soapEncodingModelPath );
                    }
                } else if ( depOneNode.getProperty( ModelerLexicon.PATH ).getString().equals( soapEncodingModelPath ) ) {
                    { // first dependency is soap encoding
                        final String input =
                            depOneNode.getProperty( ModelerLexicon.SOURCE_REFERENCE_PROPERTY ).getValues()[ 0 ].getString();
                        assertThat( input, is( soapEncodingSourceRef ) );

                        // make sure dependency resource was imported and model created (getNode throws exception if path not found)
                        session.getNode( soapEncodingArtifactPath );
                        session.getNode( soapEncodingModelPath );
                    }

                    { // dependency two must be datatypes
                        final String input =
                            depTwoNode.getProperty( ModelerLexicon.SOURCE_REFERENCE_PROPERTY ).getValues()[ 0 ].getString();
                        assertThat( input, is( dataTypesSourceRef ) );

                        // make sure dependency resource was imported and model created (getNode throws exception if path not found)
                        session.getNode( dataTypesArtifactPath );
                        session.getNode( dataTypesModelPath );
                    }
                } else {
                    fail( "path=" + depOneNode.getProperty( ModelerLexicon.PATH ).getString() );
                }

                return null;
            }
        } );
    }

    @Test
    public void shouldProcessBooksXsd() throws Exception {
        final URL xsdUrl = getClass().getClassLoader().getResource( "Books/Books.xsd" );
        final String artifactPath = modeler().importFile( new File( xsdUrl.toURI() ), "Artifact/Books" );
        assertThat( artifactPath, is( "/Artifact/Books/Books.xsd" ) );

        final ModelType xsdModelType = xsdModelType();
        final ModelImpl model = ( ModelImpl ) modeler().generateModel( artifactPath, "Model/Books/Books.xsd", xsdModelType );

        manager().run( new Task< Node >() {

            @Override
            public Node run( final Session session ) throws Exception {
                final Node modelNode = session.getNode( model.absolutePath() );
                final String dependenciesPath = processor.process( artifactPath, modelNode, modeler() );
                assertThat( dependenciesPath, notNullValue() );

                final Node dependenciesNode = session.getNode( dependenciesPath );
                assertThat( dependenciesNode.getNodes().getSize(), is( 1L ) );

                final Node dependencyNode = dependenciesNode.getNodes().nextNode();
                assertThat( dependencyNode.getPrimaryNodeType().getName(), is( ModelerLexicon.DEPENDENCY ) );

                final String dependencyPath = "/Model/Books/data/types/BookDatatypes.xsd";
                assertThat( dependencyNode.getProperty( ModelerLexicon.PATH ).getString(), is( dependencyPath ) );

                final String input =
                    dependencyNode.getProperty( ModelerLexicon.SOURCE_REFERENCE_PROPERTY ).getValues()[ 0 ].getString();
                assertThat( input, is( "./data/types/BookDatatypes.xsd" ) );

                // make sure dependency resource was imported and model created (getNode throws exception if path not found)
                session.getNode( "/Artifact/Books/data/types/BookDatatypes.xsd" );
                session.getNode( dependencyPath );

                return null;
            }
        } );
    }

    @Test
    public void shouldProcessMoviesXsd() throws Exception {
        final URL xsdUrl = getClass().getClassLoader().getResource( "Movies/Movies.xsd" );
        final String artifactPath = modeler().importFile( new File( xsdUrl.toURI() ), null );
        assertThat( artifactPath, is( "/Movies.xsd" ) );

        final ModelType xsdModelType = xsdModelType();
        final ModelImpl model = ( ModelImpl ) modeler().generateModel( artifactPath, "Model/Movies.xsd", xsdModelType );

        manager().run( new Task< Node >() {

            @Override
            public Node run( final Session session ) throws Exception {
                final Node modelNode = session.getNode( model.absolutePath() );
                final String dependenciesPath = processor.process( artifactPath, modelNode, modeler() );
                assertThat( dependenciesPath, notNullValue() );

                final Node dependenciesNode = session.getNode( dependenciesPath );
                assertThat( dependenciesNode.getNodes().getSize(), is( 1L ) );

                final Node dependencyNode = dependenciesNode.getNodes().nextNode();
                assertThat( dependencyNode.getPrimaryNodeType().getName(), is( ModelerLexicon.DEPENDENCY ) );

                final String dependencyPath = "/Model/MovieDatatypes.xsd";
                assertThat( dependencyNode.getProperty( ModelerLexicon.PATH ).getString(), is( dependencyPath ) );

                final String input =
                    dependencyNode.getProperty( ModelerLexicon.SOURCE_REFERENCE_PROPERTY ).getValues()[ 0 ].getString();
                assertThat( input, is( "MovieDatatypes.xsd" ) );

                // make sure dependency resource was imported and model created (getNode throws exception if path not found)
                session.getNode( dependencyPath );
                session.getNode( "/MovieDatatypes.xsd" );

                return null;
            }
        } );

    }

    private ModelType xsdModelType() throws Exception {
        ModelType xsdModelType = null;

        for ( final ModelType type : modelTypeManager().modelTypes() ) {
            if ( type.id().equals( XsdLexicon.MODEL_TYPE_ID ) ) {
                xsdModelType = type;
                break;
            }
        }

        assertThat( xsdModelType, notNullValue() );
        return xsdModelType;
    }

}
