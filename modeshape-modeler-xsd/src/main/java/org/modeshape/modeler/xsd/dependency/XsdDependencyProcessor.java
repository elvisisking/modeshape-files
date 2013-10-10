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
import javax.jcr.RepositoryException;

import org.modeshape.common.util.CheckArg;
import org.modeshape.modeler.ModelType;
import org.modeshape.modeler.Modeler;
import org.modeshape.modeler.ModelerException;
import org.modeshape.modeler.internal.DependencyProcessor;
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
        // if no colon prepend file:
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
     * @see DependencyProcessor#process(Node, ModelType, Modeler)
     */
    @Override
    public String process( final Node modelNode,
                           final ModelType modelType,
                           final Modeler modeler ) throws ModelerException {
        // method should not be called unless the right type of model node
        if ( !processable( modelNode ) ) {
            try {
                throw new ModelerException( XsdModelerI18n.notAnXsdModel, modelNode.getName() );
            } catch ( final RepositoryException e ) {
                throw new ModelerException( e );
            }
        }

        try {
            LOGGER.debug( "Processing model node '%s'", modelNode.getName() );
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
                throw new ModelerException( XsdModelerI18n.schemaNodeNotFound, modelNode.getName() );
            }

            // iterate over schema node's children to find dependencies
            final NodeIterator itr = schemaNode.getNodes();

            if ( !itr.hasNext() ) {
                return null; // no dependencies node created
            }

            Node dependenciesNode = null;
            final List< String > pathsToMissingDependencies = new ArrayList< String >( ( int ) itr.getSize() );

            // find the dependency nodes
            while ( itr.hasNext() ) {
                final Node kid = itr.nextNode();

                if ( !dependencyNode( kid ) ) {
                    continue;
                }

                LOGGER.debug( "Processing dependency node '%s'", kid.getName() );

                // create dependencies folder node if not already created
                if ( dependenciesNode == null ) {
                    dependenciesNode = modelNode.addNode( ModelerLexicon.DEPENDENCIES_NODE, ModelerLexicon.DEPENDENCIES_NODE );
                    LOGGER.debug( "Created dependencies folder node '%s'", dependenciesNode.getPath() );
                }

                // create dependency node
                final Node dependencyNode =
                    dependenciesNode.addNode( ModelerLexicon.DEPENDENCY_NODE, ModelerLexicon.DEPENDENCY_NODE );

                // set input property
                final Property locationProp = kid.getProperty( SCHEMA_LOCATION );
                final String location = locationProp.getString();
                dependencyNode.setProperty( ModelerLexicon.SOURCE_REFERENCE_PROPERTY, new String[] { location } );
                LOGGER.debug( "Setting dependency source reference property to '%s'", location );

                // derive path using model node path as starting point
                Node node = modelNode;
                String path = normalizePath( location );
                boolean exists = false;

                if ( pathIsRelative( path ) ) {
                    while ( path.startsWith( SELF_PATH ) || path.startsWith( PARENT_PATH ) ) {
                        if ( path.startsWith( PARENT_PATH ) ) {
                            // if root node there is no parent
                            if ( node.getDepth() == 0 ) {
                                throw new ModelerException( XsdModelerI18n.relativePathNotValid, path, modelNode.getName() );
                            }

                            node = node.getParent();
                            path = path.substring( ( PARENT_PATH + '/' ).length() );
                        } else {
                            path = path.substring( ( SELF_PATH + '/' ).length() );
                        }
                    }

                    // insert parent path
                    String parentPath = node.getPath();

                    if ( !parentPath.endsWith( "/" ) ) {
                        parentPath += "/";
                    }

                    path = parentPath + path;
                } else {
                    // TODO need more path analysis to include the original path property
                }

                // strip off leading slash to make path relative
                exists = rootNode( node ).hasNode( path.substring( 1 ) );
                LOGGER.debug( "Path '%s' exists '%s'", path, exists );

                dependencyNode.setProperty( ModelerLexicon.PATH_PROPERTY, path );
                LOGGER.debug( "Setting dependency path property to '%s'", path );

                if ( !exists ) {
                    pathsToMissingDependencies.add( path );
                }
            }

            // did not find any dependencies
            if ( dependenciesNode == null ) {
                return null;
            }

            // process any missing dependencies
            if ( !pathsToMissingDependencies.isEmpty() ) {
                uploadMissingDependencies( pathsToMissingDependencies, modeler, modelType );
            }

            return dependenciesNode.getPath();
        } catch ( final Exception e ) {
            throw new ModelerException( e );
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.modeler.internal.DependencyProcessor#processable(javax.jcr.Node)
     */
    @Override
    public boolean processable( final Node modelNode ) throws ModelerException {
        CheckArg.isNotNull( modelNode, "modelNode" );

        try {
            return MODEL_ID.equals( modelNode.getProperty( ModelerLexicon.MODEL_TYPE ).getString() );
        } catch ( final Exception e ) {
            throw new ModelerException( e );
        }
    }

    private Node rootNode( final Node node ) throws Exception {
        return ( Node ) node.getAncestor( 0 );
    }

    private void uploadMissingDependencies( final List< String > paths,
                                            final Modeler modeler,
                                            final ModelType modelType ) {
        assert ( paths != null );
        assert ( modeler != null );
        // TODO implement uploadMissingDependencies
        //
        // for ( final String path : paths ) {
        // try {
        // modeler.importArtifact( name, new URL( path ).openStream(), path );
        // modeler.createModel( path, modelType );
        // } catch ( final Exception e ) {
        // LOGGER.error( e, message, params );
        // }
        // }
    }

}
