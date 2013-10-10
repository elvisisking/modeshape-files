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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;

import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.api.ValueFactory;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.modeler.internal.DependencyProcessor;
import org.modeshape.modeler.internal.Manager;
import org.modeshape.modeler.internal.ModelImpl;
import org.modeshape.modeler.internal.ModelTypeImpl;
import org.modeshape.modeler.internal.ModelerLexicon;
import org.modeshape.modeler.internal.Task;
import org.polyglotter.common.Logger;

/**
 * 
 */
public final class Modeler implements AutoCloseable {

    /**
     * The path to the default ModeShape configuration, which uses a file-based repository
     */
    public static final String DEFAULT_MODESHAPE_CONFIGURATION_PATH = "jcr/modeShapeConfig.json";

    final Manager manager;

    /**
     * Uses a default ModeShape configuration.
     * 
     * @param repositoryStoreParentPath
     *        the path to the folder that should contain the ModeShape repository store
     * @throws ModelerException
     *         if any error occurs
     */
    public Modeler( final String repositoryStoreParentPath ) throws ModelerException {
        this( repositoryStoreParentPath, DEFAULT_MODESHAPE_CONFIGURATION_PATH );
    }

    /**
     * @param repositoryStoreParentPath
     *        the path to the folder that should contain the ModeShape repository store
     * @param modeShapeConfigurationPath
     *        the path to a ModeShape configuration file
     * @throws ModelerException
     *         if any error occurs
     */
    public Modeler( final String repositoryStoreParentPath,
                    final String modeShapeConfigurationPath ) throws ModelerException {
        manager = new Manager( repositoryStoreParentPath, modeShapeConfigurationPath );
    }

    String absolutePath( String path ) {
        if ( path == null ) return "/";
        path = path.trim();
        if ( path.isEmpty() ) return "/";
        if ( path.charAt( 0 ) == '/' ) return path;
        return '/' + path;
    }

    String absolutePath( String path,
                         final String name ) {
        path = absolutePath( path );
        return path.endsWith( "/" ) ? path + name : path + '/' + name;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ModelerException
     *         if any problem occurs
     * @see java.lang.AutoCloseable#close()
     */
    @Override
    public void close() throws ModelerException {
        manager.close();
    }

    /**
     * @param artifactPath
     *        the workspace path to an artifact; must not be empty.
     * @param modelPath
     *        the path where the model should be created
     * @return a new model of the default type, determined by the artifact's content, and if the artifact is a file, its file
     *         extension; never <code>null</code>
     * @throws ModelerException
     *         if any problem occurs
     */
    public Model generateDefaultModel( final String artifactPath,
                                       final String modelPath ) throws ModelerException {
        return generateModel( artifactPath, modelPath, null );
    }

    /**
     * Creates a model with the name of the supplied file.
     * 
     * @param file
     *        the file to be imported. Must not be <code>null</code>.
     * @param modelFolder
     *        the parent path where the model should be created
     * @param modelType
     *        the type of model to be created for the supplied artifact; may be <code>null</code>.
     * @return a new model of the supplied type; never <code>null</code>
     * @throws ModelerException
     *         if any problem occurs
     */
    public Model generateModel( final File file,
                                final String modelFolder,
                                final ModelType modelType ) throws ModelerException {
        return generateModel( file, modelFolder, null, modelType );
    }

    /**
     * Creates a model with the name of the supplied file.
     * 
     * @param file
     *        the file to be imported. Must not be <code>null</code>.
     * @param modelFolder
     *        the parent path where the model should be created
     * @param modelName
     *        the name of the model. If <code>null</code> or empty, the name of the supplied file will be used.
     * @param modelType
     *        the type of model to be created for the supplied artifact; may be <code>null</code>.
     * @return a new model of the supplied type; never <code>null</code>
     * @throws ModelerException
     *         if any problem occurs
     */
    public Model generateModel( final File file,
                                final String modelFolder,
                                final String modelName,
                                final ModelType modelType ) throws ModelerException {
        CheckArg.isNotNull( file, "file" );
        try {
            return generateModel( file.toURI().toURL(), modelFolder, modelName, modelType );
        } catch ( final MalformedURLException e ) {
            throw new ModelerException( e );
        }
    }

    /**
     * @param stream
     *        the artifact's content to be imported. Must not be <code>null</code>.
     * @param modelPath
     *        the path where the model should be created
     * @param modelType
     *        the type of model to be created for the supplied artifact; may be <code>null</code>.
     * @return a new model of the supplied type; never <code>null</code>
     * @throws ModelerException
     *         if any problem occurs
     */
    public Model generateModel( final InputStream stream,
                                final String modelPath,
                                final ModelType modelType ) throws ModelerException {
        final String artifactPath = importArtifact( stream, ModelerLexicon.TEMP_FOLDER + "/file" );
        final Model model = generateModel( artifactPath, modelPath, modelType );
        removeTemporaryArtifact( artifactPath );
        return model;
    }

    /**
     * @param artifactPath
     *        the workspace path to an artifact; must not be empty.
     * @param modelPath
     *        the path where the model should be created
     * @param modelType
     *        the type of model to be created for the supplied artifact; may be <code>null</code>.
     * @return a new model of the supplied type; never <code>null</code>
     * @throws ModelerException
     *         if any problem occurs
     */
    public Model generateModel( final String artifactPath,
                                final String modelPath,
                                final ModelType modelType ) throws ModelerException {
        CheckArg.isNotEmpty( artifactPath, "artifactPath" );
        CheckArg.isNotEmpty( modelPath, "modelPath" );
        return manager.run( new Task< Model >() {

            @Override
            public Model run( final Session session ) throws Exception {
                final Node artifactNode = manager.artifactNode( session, artifactPath );
                ModelType type = modelType;
                if ( modelType == null ) {
                    // If no model type supplied, use default model type if one exists
                    type = manager.modelTypeManager.defaultModelType( artifactNode,
                                                                      manager.modelTypeManager.modelTypes( artifactNode ) );
                    if ( type == null )
                        throw new IllegalArgumentException( ModelerI18n.unableToDetermineDefaultModelType.text( artifactPath ) );
                    throw new UnsupportedOperationException( "Not yet implemented" );
                }
                // Build the model
                final ValueFactory valueFactory = ( ValueFactory ) session.getValueFactory();
                final Calendar cal = Calendar.getInstance();
                final ModelTypeImpl modelType = ( ModelTypeImpl ) type;
                final Node modelNode = new JcrTools().findOrCreateNode( session, absolutePath( modelPath ) );
                modelNode.addMixin( ModelerLexicon.MODEL_MIXIN );
                if ( artifactNode.hasProperty( ModelerLexicon.EXTERNAL_LOCATION ) )
                    modelNode.setProperty( ModelerLexicon.EXTERNAL_LOCATION,
                                           artifactNode.getProperty( ModelerLexicon.EXTERNAL_LOCATION ).getString() );
                final boolean save = modelType.sequencer().execute( artifactNode.getNode( JcrLexicon.CONTENT.getString() )
                                                                                .getProperty( JcrLexicon.DATA.getString() ),
                                                                    modelNode,
                                                                    new Sequencer.Context() {

                                                                        @Override
                                                                        public Calendar getTimestamp() {
                                                                            return cal;
                                                                        }

                                                                        @Override
                                                                        public ValueFactory valueFactory() {
                                                                            return valueFactory;
                                                                        }
                                                                    } );
                if ( save ) {
                    modelNode.setProperty( ModelerLexicon.MODEL_TYPE, modelType.name() );
                    processDependencies( modelNode, modelType );
                    session.save();
                    return new ModelImpl( manager, modelNode.getPath() );
                }
                throw new ModelerException( ModelerI18n.sessionNotSavedWhenCreatingModel, artifactPath );
            }
        } );
    }

    /**
     * @param artifactUrl
     *        the URL of an artifact; must not be <code>null</code>.
     * @param modelFolder
     *        the parent path where the model should be created
     * @param modelType
     *        the type of model to be created for the supplied artifact; may be <code>null</code>.
     * @return a new model of the supplied type; never <code>null</code>
     * @throws ModelerException
     *         if any problem occurs
     */
    public Model generateModel( final URL artifactUrl,
                                final String modelFolder,
                                final ModelType modelType ) throws ModelerException {
        return generateModel( artifactUrl, modelFolder, null, modelType );
    }

    /**
     * @param artifactUrl
     *        the URL of an artifact; must not be <code>null</code>.
     * @param modelFolder
     *        the parent path where the model should be created
     * @param modelName
     *        the name of the model. If <code>null</code> or empty, the name of the supplied file will be used.
     * @param modelType
     *        the type of model to be created for the supplied artifact; may be <code>null</code>.
     * @return a new model of the supplied type; never <code>null</code>
     * @throws ModelerException
     *         if any problem occurs
     */
    public Model generateModel( final URL artifactUrl,
                                final String modelFolder,
                                final String modelName,
                                final ModelType modelType ) throws ModelerException {
        final String artifactPath = importArtifact( artifactUrl, ModelerLexicon.TEMP_FOLDER );
        final Model model = generateModel( artifactPath, absolutePath( modelFolder, name( modelName, artifactUrl ) ), modelType );
        removeTemporaryArtifact( artifactPath );
        return model;
    }

    /**
     * @param stream
     *        the artifact's content to be imported. Must not be <code>null</code>.
     * @param workspacePath
     *        the path where the artifact should be imported
     * @return the workspace path the to imported artifact
     * @throws ModelerException
     *         if any problem occurs
     */
    public String importArtifact( final InputStream stream,
                                  final String workspacePath ) throws ModelerException {
        CheckArg.isNotNull( stream, "stream" );
        CheckArg.isNotEmpty( workspacePath, "workspacePath" );
        return manager.run( new Task< String >() {

            @Override
            public String run( final Session session ) throws Exception {
                // Ensure the path is non-null, absolute, and ends with a slash
                final Node node = new JcrTools().uploadFile( session, absolutePath( workspacePath ), stream );
                // Add unstructured mix-in to allow node to contain anything else, like models created later
                node.addMixin( ModelerLexicon.UNSTRUCTURED_MIXIN );
                session.save();
                return node.getPath();
            }
        } );
    }

    /**
     * @param url
     *        the name of the artifact as it should be stored in the workspace. Must not be empty.
     * @param workspaceFolder
     *        the parent path where the artifact should be imported
     * @return the workspace path the to imported artifact
     * @throws ModelerException
     *         if any problem occurs
     */
    public String importArtifact( final URL url,
                                  final String workspaceFolder ) throws ModelerException {
        return importArtifact( url, workspaceFolder, null );
    }

    /**
     * @param url
     *        the name of the artifact as it should be stored in the workspace. Must not be empty.
     * @param workspaceFolder
     *        the parent path where the artifact should be imported
     * @param workspaceName
     *        the name of the artifact in the workspace. If <code>null</code> or empty, the last segment of the supplied URL will be
     *        used.
     * @return the workspace path the to imported artifact
     * @throws ModelerException
     *         if any problem occurs
     */
    public String importArtifact( final URL url,
                                  final String workspaceFolder,
                                  final String workspaceName ) throws ModelerException {
        CheckArg.isNotNull( url, "url" );
        try {
            final String path = importArtifact( url.openStream(), absolutePath( workspaceFolder, name( workspaceName, url ) ) );
            saveExternalLocation( path, url.toString() );
            return path;
        } catch ( final FileNotFoundException e ) {
            throw new IllegalArgumentException( e );
        } catch ( final IOException e ) {
            throw new ModelerException( e );
        }
    }

    /**
     * @param file
     *        the file to be imported. Must not be <code>null</code>.
     * @param workspaceFolder
     *        the parent path where the file should be imported
     * @return the workspace path the to imported artifact
     * @throws ModelerException
     *         if any problem occurs
     */
    public String importFile( final File file,
                              final String workspaceFolder ) throws ModelerException {
        return importFile( file, workspaceFolder, null );
    }

    /**
     * @param file
     *        the file to be imported. Must not be <code>null</code>.
     * @param workspaceFolder
     *        the parent path where the file should be imported
     * @param workspaceName
     *        the name of the file in the workspace. If <code>null</code> or empty, the name of the supplied file will be used.
     * @return the workspace path the to imported artifact
     * @throws ModelerException
     *         if any problem occurs
     */
    public String importFile( final File file,
                              final String workspaceFolder,
                              final String workspaceName ) throws ModelerException {
        CheckArg.isNotNull( file, "file" );
        try {
            return importArtifact( file.toURI().toURL(), workspaceFolder, workspaceName );
        } catch ( final MalformedURLException e ) {
            throw new ModelerException( e );
        }
    }

    /**
     * @param path
     *        a workspace path for a model
     * @return the model at the supplied path, or <code>null</code> if not found
     * @throws ModelerException
     *         if any error occurs
     */
    public Model model( final String path ) throws ModelerException {
        CheckArg.isNotEmpty( path, "path" );
        return manager.run( new Task< Model >() {

            @Override
            public Model run( final Session session ) throws Exception {
                try {
                    final String absPath = absolutePath( path );
                    final Node node = session.getNode( absPath );
                    if ( !node.isNodeType( ModelerLexicon.MODEL_MIXIN ) )
                        throw new IllegalArgumentException( ModelerI18n.notModelPath.text( absPath ) );
                    return new ModelImpl( manager, absPath );
                } catch ( final PathNotFoundException e ) {
                    return null;
                }
            }
        } );
    }

    /**
     * @return the model type manager
     */
    public ModelTypeManager modelTypeManager() {
        return manager.modelTypeManager;
    }

    /**
     * @return the path to the configuration for the embedded ModeShape repository supplied when this Modeler was instantiated.
     */
    public String modeShapeConfigurationPath() {
        return manager.modeShapeConfigurationPath;
    }

    private String name( String workspaceName,
                         final URL url ) {
        if ( workspaceName != null && !workspaceName.trim().isEmpty() ) return workspaceName;
        workspaceName = url.getPath();
        workspaceName = workspaceName.substring( workspaceName.lastIndexOf( '/' ) + 1 );
        return workspaceName;
    }

    /**
     * @param modelNode
     *        the model node whose dependency processing is being requested (cannot be <code>null</code>)
     * @param modelType
     *        the model type of the model node (cannot be <code>null</code>)
     * @return the path to the dependencies child node or <code>null</code> if no dependencies are found
     * @throws ModelerException
     *         if node is not a model nodel or if an error occurs
     */
    String processDependencies( final Node modelNode,
                                final ModelType modelType ) throws ModelerException {
        CheckArg.isNotNull( modelNode, "modelNode" );
        CheckArg.isNotNull( modelType, "modelType" );

        return manager.run( new Task< String >() {

            @Override
            public String run( final Session session ) throws Exception {
                final DependencyProcessor dependencyProcessor = manager.modelTypeManager.dependencyProcessor( modelNode );

                if ( dependencyProcessor == null ) {
                    Logger.getLogger( getClass() ).debug( "No dependency processor found for model '" + modelNode.getName() + '\'' );
                    return null;
                }

                return dependencyProcessor.process( modelNode, modelType, Modeler.this );
            }
        } );
    }

    private void removeTemporaryArtifact( final String artifactPath ) throws ModelerException {
        manager.run( new Task< Void >() {

            @Override
            public Void run( final Session session ) throws Exception {
                session.getNode( artifactPath ).remove();
                session.save();
                return null;
            }
        } );
    }

    /**
     * @return the path to the folder that should contain the ModeShape repository store
     */
    public String repositoryStoreParentPath() {
        return System.getProperty( Manager.REPOSITORY_STORE_PARENT_PATH_PROPERTY );
    }

    private void saveExternalLocation( final String path,
                                       final String location ) throws ModelerException {
        manager.run( new Task< Void >() {

            @Override
            public Void run( final Session session ) throws Exception {
                session.getNode( path ).setProperty( ModelerLexicon.EXTERNAL_LOCATION, location );
                session.save();
                return null;
            }
        } );
    }
}
