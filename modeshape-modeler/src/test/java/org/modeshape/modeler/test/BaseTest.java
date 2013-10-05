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
package org.modeshape.modeler.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.modeshape.modeler.Modeler;
import org.modeshape.modeler.TestUtil;
import org.modeshape.modeler.internal.Manager;
import org.modeshape.modeler.internal.ModelTypeManagerImpl;

/**
 * Superclass for all test classes
 */
@RunWith( TestRunner.class )
@SuppressWarnings( "javadoc" )
public abstract class BaseTest {
    
    protected static final String TEST_MODESHAPE_CONFIGURATION_PATH = "testModeShapeConfig.json";
    protected static final String TEST_REPOSITORY_STORE_PARENT_PATH;
    protected static final URL MODEL_TYPE_REPOSITORY;
    
    protected static final String XML_ROOT = "root";
    protected static final String XML_LEAF = "child";
    protected static final String XML_STRING_VALUE = "string";
    protected static final String XML_SAME_NAME_SIBLING = "sameNameSibling";
    protected static final String XML_DECLARATION = "<?xml version='1.0' encoding='UTF-8'?>";
    protected static final String XML_ARTIFACT = XML_DECLARATION
                                                 + '<' + XML_ROOT + " property='" + XML_STRING_VALUE + "'>"
                                                 + "<" + XML_LEAF + "></" + XML_LEAF + ">"
                                                 + "<" + XML_SAME_NAME_SIBLING + "></" + XML_SAME_NAME_SIBLING + ">"
                                                 + "<" + XML_SAME_NAME_SIBLING + "></" + XML_SAME_NAME_SIBLING + ">"
                                                 + "</" + XML_ROOT + ">";
    protected static final String XSD_ARTIFACT = XML_DECLARATION + "<schema></schema>";
    protected static final String XML_MODEL_TYPE_NAME = "org.modeshape.modeler.xml.Xml";
    
    static {
        try {
            MODEL_TYPE_REPOSITORY = new URL( "file:src/test/resources/" );
            final Path path = Files.createTempDirectory( null );
            path.toFile().deleteOnExit();
            TEST_REPOSITORY_STORE_PARENT_PATH = path.toString();
        } catch ( final IOException e ) {
            throw new RuntimeException( e );
        }
    }
    
    public Modeler modeler;
    public Manager manager;
    public ModelTypeManagerImpl modelTypeManager;
    
    @After
    public void after() throws Exception {
        modeler.close();
        deleteFolder( TEST_REPOSITORY_STORE_PARENT_PATH + "/modelerRepository" );
        deleteFolder( System.getProperty( "java.io.tmpdir" ) + "/modeshape-binary-store" );
    }
    
    @Before
    public void before() throws Exception {
        modeler = new Modeler( TEST_REPOSITORY_STORE_PARENT_PATH, TEST_MODESHAPE_CONFIGURATION_PATH );
        manager = TestUtil.manager( modeler );
        modelTypeManager = ( ModelTypeManagerImpl ) modeler.modelTypeManager();
    }
    
    private void deleteFolder( final String folder ) throws Exception {
        final File file = new File( folder );
        if ( file.exists() )
            Files.walkFileTree( FileSystems.getDefault().getPath( file.toString() ), new SimpleFileVisitor< Path >() {
                
                @Override
                public FileVisitResult postVisitDirectory( final Path folder,
                                                           final IOException e ) throws IOException {
                    if ( e != null ) throw e;
                    Files.delete( folder );
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult visitFile( final Path file,
                                                  final BasicFileAttributes attrs ) throws IOException {
                    Files.delete( file );
                    return FileVisitResult.CONTINUE;
                }
            } );
    }
    
    protected String importArtifact( final String artifactPath ) throws Exception {
        return modeler.importArtifact( "stuff", new ByteArrayInputStream( artifactPath.getBytes() ), null );
    }
    
    protected InputStream stream( final String artifactPath ) {
        return new ByteArrayInputStream( artifactPath.getBytes() );
    }
}
