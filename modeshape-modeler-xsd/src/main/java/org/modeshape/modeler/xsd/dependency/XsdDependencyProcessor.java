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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;

import org.modeshape.common.util.CheckArg;
import org.modeshape.modeler.ModelType;
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
     * @see org.modeshape.modeler.extensions.DependencyProcessor#process(java.lang.String, javax.jcr.Node,
     *      org.modeshape.modeler.Modeler, boolean)
     */
    @Override
    public String process( final String artifactPath,
                           final Node modelNode,
                           final Modeler modeler,
                           final boolean persistArtifacts ) throws ModelerException {
        Node dependenciesNode = null;
        List< MissingDependency > pathsToMissingDependencies = null;

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
            DEPENDENCIES:
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

                // derive path using model node parent as starting point
                Node node = modelNode.getParent();
                String path = normalizePath( location );
                boolean exists = false;
                int count = 0;

                if ( pathIsRelative( path ) ) {
                    while ( path.startsWith( SELF_PATH ) || path.startsWith( PARENT_PATH ) ) {
                        if ( path.startsWith( PARENT_PATH ) ) {
                            // if root node there is no parent
                            if ( node.getDepth() == 0 ) {
                                LOGGER.debug( "The relative path of '%s' is not valid for a dependency node of model '%s'", path, modelName );
                                continue DEPENDENCIES;
                            }

                            node = node.getParent();
                            path = path.substring( ( PARENT_PATH + '/' ).length() );
                            ++count;
                        } else {
                            path = path.substring( ( SELF_PATH + '/' ).length() );
                        }
                    }

                    exists = node.hasNode( path );
                } else {
                    // TODO need more path analysis to include the original path property
                }

                String parentModelPath = node.getPath();

                if ( !parentModelPath.endsWith( "/" ) ) {
                    parentModelPath += "/";
                }

                final String fullModelPath = parentModelPath + path;
                dependencyNode.setProperty( ModelerLexicon.PATH, fullModelPath );
                LOGGER.debug( "Setting dependency path property to '%s'", fullModelPath );

                if ( !exists ) {
                    final MissingDependency md = new MissingDependency( path, count, parentModelPath );
                    pathsToMissingDependencies.add( md );
                }
            }

            if ( dependenciesNode == null ) {
                return null;
            }

            // process any missing dependencies
            if ( !pathsToMissingDependencies.isEmpty() ) {
                uploadMissingDependencies( artifactPath, modelNode, pathsToMissingDependencies, modeler, persistArtifacts );
            }

            modelNode.getSession().save();
            return dependenciesNode.getPath();
        } catch ( final Exception e ) {
            throw new ModelerException( e );
        }
    }

    void uploadMissingDependencies( final String artifactPath,
                                    final Node modelNode,
                                    final List< MissingDependency > missingDependencies,
                                    final Modeler modeler,
                                    final boolean persistArtifacts ) throws Exception {
        assert ( modelNode != null );
        assert ( missingDependencies != null );
        assert ( modeler != null );

        if ( !modelNode.hasProperty( ModelerLexicon.EXTERNAL_LOCATION )
             || !modelNode.hasProperty( ModelerLexicon.MODEL_TYPE )
             || missingDependencies.isEmpty() ) {
            return;
        }

        final String modelName = modelNode.getName();
        final String type = modelNode.getProperty( ModelerLexicon.MODEL_TYPE ).getString();
        final ModelType modelType = modeler.modelTypeManager().modelType( type );

        String externalLocation = modelNode.getProperty( ModelerLexicon.EXTERNAL_LOCATION ).getString();
        externalLocation = externalLocation.substring( 0, ( externalLocation.lastIndexOf( "/" ) ) );

        final String artifactDir = artifactPath.substring( 0, ( artifactPath.lastIndexOf( "/" ) ) );

        for ( final MissingDependency missingDependency : missingDependencies ) {
            String artifactLocation = artifactDir;
            String location = externalLocation;
            int numParentDirs = missingDependency.numParentDirs;

            // navigate up parent dirs if necessary
            while ( numParentDirs > 0 ) {
                location = location.substring( 0, ( externalLocation.lastIndexOf( "/" ) ) );
                artifactLocation = artifactLocation.substring( 0, ( artifactLocation.lastIndexOf( "/" ) ) );
                --numParentDirs;
            }

            // setup external path
            String extPath = location;

            if ( !extPath.endsWith( "/" ) ) {
                extPath += '/';
            }

            extPath += missingDependency.relativePath;

            // setup dependency artifact path
            if ( !artifactLocation.endsWith( "/" ) ) {
                artifactLocation += "/";
            }

            artifactLocation += missingDependency.relativePath;

            try {
                LOGGER.debug( "Importing XSD dependency from external path '%s' for source '%s' and path '%s'", extPath, modelName, artifactLocation );
                final String dependencyArtifactPath = modeler.importArtifact( new URL( extPath ).openStream(), artifactLocation );

                // create model
                final String modelPath = ( missingDependency.modelParentPath + missingDependency.relativePath );
                LOGGER.debug( "Generating model for XSD dependency of model '%s' from path '%s'", modelName, modelPath );
                modeler.generateModel( dependencyArtifactPath, modelPath, modelType, persistArtifacts );
            } catch ( final Exception e ) {
                LOGGER.error( e, XsdModelerI18n.errorImportingXsdDependencyArtifact, extPath, modelName );
            }
        }
    }

    private static class MissingDependency {

        final String modelParentPath;
        final int numParentDirs;
        final String relativePath;

        MissingDependency( final String relativePath,
                           final int numParentDirs,
                           final String modelParentPath ) {
            this.relativePath = relativePath;
            this.numParentDirs = numParentDirs;
            this.modelParentPath = modelParentPath;
        }

    }

}
