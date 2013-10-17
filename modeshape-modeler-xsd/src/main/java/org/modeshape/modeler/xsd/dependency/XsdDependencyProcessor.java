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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;

import org.modeshape.common.util.CheckArg;
import org.modeshape.modeler.Modeler;
import org.modeshape.modeler.ModelerException;
import org.modeshape.modeler.extensions.DependencyProcessor;
import org.modeshape.modeler.internal.ModelerLexicon;
import org.modeshape.modeler.xsd.XsdLexicon;
import org.modeshape.modeler.xsd.XsdModelerI18n;
import org.polyglotter.common.Logger;

/**
 * The XSD dependency processor for the ModeShape modeler.
 */
public final class XsdDependencyProcessor implements DependencyProcessor, XsdLexicon {

    private static final Logger LOGGER = Logger.getLogger( XsdDependencyProcessor.class );

    /**
     * @param path
     *        the path being normalized (cannot be <code>null</code> or empty)
     * @return the normalized path (never <code>null</code> or empty)
     * @throws Exception
     *         if an error occurs
     */
    private static String normalizePath( final String path ) throws Exception {
        final URI uri = new URI( path ).normalize();
        return uri.toString();
    }

    /**
     * @param input
     *        the text being checked (cannot be <code>null</code> or empty)
     * @return true if the text represents a URI that is not absolute
     * @throws Exception
     *         if input is empty or not in a valid format
     */
    private static boolean pathIsRelative( final String input ) throws Exception {
        CheckArg.isNotEmpty( input, "input" );
        final URI uri = new URI( input ).normalize();
        return !uri.isAbsolute();
    }

    private boolean dependencyNode( final Node node ) throws Exception {
        assert ( node != null );

        final String primaryType = node.getPrimaryNodeType().getName();
        return ( IMPORT.equals( primaryType ) || INCLUDE.equals( primaryType ) || REDEFINE.equals( primaryType ) );
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.extensions.DependencyProcessor#process(javax.jcr.Node, org.modeshape.modeler.Modeler)
     */
    @Override
    public String process( final Node modelNode,
                           final Modeler modeler ) throws ModelerException {
        Node dependenciesNode = null;
        List< String > pathsToMissingDependencies = null;

        try {
            final String modelName = modelNode.getName();
            LOGGER.debug( "Processing model node '%s'", modelName );
            Node schemaNode = null;

            { // find schema node
                final NodeIterator itr = modelNode.getParent().getNodes();

                while ( itr.hasNext() ) {
                    final Node kid = itr.nextNode();

                    if ( SCHEMA_DOCUMENT.equals( kid.getPrimaryNodeType().getName() ) ) {
                        schemaNode = kid;
                        break;
                    }
                }
            }

            // should always have a schema node
            if ( schemaNode == null ) {
                throw new ModelerException( XsdModelerI18n.schemaNodeNotFound, modelName );
            }

            // iterate over schema node's children to find dependencies
            final NodeIterator itr = schemaNode.getNodes();

            if ( !itr.hasNext() ) {
                return null; // no children of schema node so dependencies node not created
            }

            pathsToMissingDependencies = new ArrayList<>();

            // find the dependency nodes
            while ( itr.hasNext() ) {
                final Node kid = itr.nextNode();

                if ( !dependencyNode( kid ) ) {
                    continue;
                }

                LOGGER.debug( "Processing dependency node '%s'", kid.getName() );

                // create dependencies folder node if not already created
                if ( dependenciesNode == null ) {
                    dependenciesNode = modelNode.addNode( ModelerLexicon.DEPENDENCIES, ModelerLexicon.DEPENDENCIES );
                    LOGGER.debug( "Created dependencies folder node '%s'", dependenciesNode.getPath() );
                }

                // create dependency node
                final Node dependencyNode =
                    dependenciesNode.addNode( ModelerLexicon.DEPENDENCY, ModelerLexicon.DEPENDENCY );

                // set input property
                final Property locationProp = kid.getProperty( SCHEMA_LOCATION );
                final String location = locationProp.getString();
                dependencyNode.setProperty( ModelerLexicon.SOURCE_REFERENCE_PROPERTY, new String[] { location } );
                LOGGER.debug( "Setting dependency source reference property to '%s'", location );

                // derive path using model node as starting point
                Node node = modelNode;
                String path = normalizePath( location );
                boolean exists = false;

                if ( pathIsRelative( path ) ) {
                    while ( path.startsWith( SELF_PATH ) || path.startsWith( PARENT_PATH ) ) {
                        if ( path.startsWith( PARENT_PATH ) ) {
                            // if root node there is no parent
                            if ( node.getDepth() == 0 ) {
                                throw new ModelerException( XsdModelerI18n.relativePathNotValid, path, modelName );
                            }

                            node = node.getParent();
                            path = path.substring( ( PARENT_PATH + '/' ).length() );
                        } else {
                            path = path.substring( ( SELF_PATH + '/' ).length() );
                        }
                    }

                    exists = node.hasNode( path );
                } else {
                    // TODO need more path analysis to include the original path property
                }

                // insert parent path so that path starts at root
                String parentPath = node.getPath();

                if ( !parentPath.endsWith( "/" ) ) {
                    parentPath += "/";
                }

                path = parentPath + path;
                dependencyNode.setProperty( ModelerLexicon.PATH, path );
                LOGGER.debug( "Setting dependency path property to '%s'", path );

                if ( !exists ) {
                    pathsToMissingDependencies.add( path );
                }

                // upload dependencies if necessary
                if ( !pathsToMissingDependencies.isEmpty() ) {
                    uploadMissingDependencies( modelNode, pathsToMissingDependencies, modeler );
                }
            }

            if ( dependenciesNode == null ) {
                return null;
            }

            // process any missing dependencies
            if ( !pathsToMissingDependencies.isEmpty() ) {
                uploadMissingDependencies( modelNode, pathsToMissingDependencies, modeler );
            }

            modelNode.getSession().save();
            return dependenciesNode.getPath();
        } catch ( final Exception e ) {
            throw new ModelerException( e );
        }
    }

    void uploadMissingDependencies( final Node modelNode,
                                    final List< String > paths,
                                    final Modeler modeler ) throws Exception {
        assert ( modelNode != null );
        assert ( paths != null );
        assert ( modeler != null );

        if ( !modelNode.hasProperty( ModelerLexicon.EXTERNAL_LOCATION )
             || !modelNode.hasProperty( ModelerLexicon.MODEL_TYPE )
             || paths.isEmpty() ) {
            return;
        }

        final String sourceName = modelNode.getParent().getName();
        final String externalLocation = modelNode.getProperty( ModelerLexicon.EXTERNAL_LOCATION ).getString();

        for ( final String dependencyPath : paths ) {
            final String extPath = externalLocation + dependencyPath;

            try {
                // upload
                // TODO implement uploadMissingDependencies
                // LOGGER.debug( "Importing XSD dependency from external path '%s' for source '%s'", extPath, sourceName );
                // final String artifactPath = modeler.importArtifact( new URL( extPath ).openStream(), dependencyPath );
                //
                // // create model
                // LOGGER.debug( "Generating model for XSD dependency of model '%s' from path '%s'", sourceName, extPath );
                // final String type = modelNode.getSession().getNode( dependencyPath ).getProperty( ModelerLexicon.MODEL_TYPE
                // ).getString();
                // final ModelType modelType = modeler.modelTypeManager().modelType( type );
                // modeler.generateModel( artifactPath, dependencyPath, modelType );
            } catch ( final Exception e ) {
                LOGGER.error( e, XsdModelerI18n.errorImportingXsdDependencyArtifact, extPath, sourceName );
            }
        }
    }
}
