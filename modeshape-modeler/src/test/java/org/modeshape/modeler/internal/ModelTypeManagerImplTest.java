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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.Value;

import org.junit.Test;
import org.modeshape.modeler.ModeShapeModeler;
import org.modeshape.modeler.ModelType;
import org.modeshape.modeler.ModelTypeManager;
import org.modeshape.modeler.Modeler;
import org.modeshape.modeler.TestUtil;
import org.modeshape.modeler.test.BaseTest;

@SuppressWarnings( "javadoc" )
public class ModelTypeManagerImplTest extends BaseTest {

    private ModelTypeManager failingModelTypeManager() throws Exception {
        return new ModelTypeManagerImpl( mock( Manager.class ) );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetApplicableModelTypesIfPathIsEmpty() throws Exception {
        failingModelTypeManager().modelTypesForArtifact( " " );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetApplicableModelTypesIfPathIsNull() throws Exception {
        failingModelTypeManager().modelTypesForArtifact( null );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetDefaultModelTypeIfPathIsEmpty() throws Exception {
        failingModelTypeManager().defaultModelType( " " );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetDefaultModelTypeIfPathIsNull() throws Exception {
        failingModelTypeManager().defaultModelType( null );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetModelTypeIfNameIsEmpty() throws Exception {
        failingModelTypeManager().modelType( " " );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetModelTypeIfNameIsNull() throws Exception {
        failingModelTypeManager().modelType( null );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetModelTypesForCategoryIfCategoryEmpty() throws Exception {
        failingModelTypeManager().modelTypesForCategory( " " );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetModelTypesForCategoryIfCategoryNull() throws Exception {
        failingModelTypeManager().modelTypesForCategory( null );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToInstallModelTypesIfCategoryIsEmpty() throws Exception {
        failingModelTypeManager().install( " " );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToInstallModelTypesIfCategoryIsNull() throws Exception {
        failingModelTypeManager().install( null );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToInstallModelTypesIfCategoryNotFound() throws Exception {
        modelTypeManager().install( "bogus" );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToMoveModelTypeRepositoryDownIfUrlNotFound() throws Exception {
        failingModelTypeManager().moveModelTypeRepositoryDown( MODEL_TYPE_REPOSITORY );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToMoveModelTypeRepositoryDownIfUrlNull() throws Exception {
        failingModelTypeManager().moveModelTypeRepositoryDown( null );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToMoveModelTypeRepositoryUpIfUrlNotFound() throws Exception {
        failingModelTypeManager().moveModelTypeRepositoryUp( MODEL_TYPE_REPOSITORY );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToMoveModelTypeRepositoryUpIfUrlNull() throws Exception {
        failingModelTypeManager().moveModelTypeRepositoryUp( null );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToRegisterModelTypeRepositoryIfUrlIsNull() throws Exception {
        failingModelTypeManager().registerModelTypeRepository( null );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToUninstallIfCategoryEmpty() throws Exception {
        failingModelTypeManager().uninstall( " " );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToUninstallIfCategoryNull() throws Exception {
        failingModelTypeManager().uninstall( null );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToUnregisterModelTypeRepositoryIfUrlIsNull() throws Exception {
        failingModelTypeManager().unregisterModelTypeRepository( null );
    }

    @Test
    public void shouldGetDefaultRegisteredModelTypeRepositories() throws Exception {
        final List< URL > repos = modelTypeManager().modelTypeRepositories();
        assertThat( repos, notNullValue() );
        assertThat( repos.isEmpty(), is( false ) );
    }

    @Test
    public void shouldGetEmptyApplicableModelTypesIfFileHasUknownMimeType() throws Exception {
        final String path = modeler().importArtifact( stream( "stuff" ), ARTIFACT_NAME );
        final Set< ModelType > types = modelTypeManager().modelTypesForArtifact( path );
        assertThat( types, notNullValue() );
        assertThat( types.isEmpty(), is( true ) );
    }

    @Test
    public void shouldGetExistingRegisteredModelTypeRepositoriesIfRegisteringRegisteredUrl() throws Exception {
        final List< URL > origRepos = modelTypeManager().modelTypeRepositories();
        final List< URL > repos = modelTypeManager().registerModelTypeRepository( MODEL_TYPE_REPOSITORY );
        assertThat( repos, notNullValue() );
        assertThat( repos.equals( origRepos ), is( true ) );
    }

    @Test
    public void shouldGetExistingRegisteredModelTypeRepositoriesIfUnregisteringUnregisteredUrl() throws Exception {
        final int size = modelTypeManager().modelTypeRepositories().size();
        final List< URL > repos = modelTypeManager().unregisterModelTypeRepository( new URL( "file:" ) );
        assertThat( repos, notNullValue() );
        assertThat( repos.size(), is( size ) );
    }

    @Test
    public void shouldGetModelType() throws Exception {
        modelTypeManager().registerModelTypeRepository( MODEL_TYPE_REPOSITORY );
        modelTypeManager().install( "xml" );
        assertThat( modelTypeManager().modelType( XML_MODEL_TYPE_NAME ), notNullValue() );
    }

    @Test
    public void shouldGetModelTypeCategories() throws Exception {
        modelTypeManager().registerModelTypeRepository( MODEL_TYPE_REPOSITORY );
        modelTypeManager().install( "java" );
        assertThat( modelTypeManager().modelTypeCategories().size(), is( 1 ) );
        assertThat( modelTypeManager().modelTypeCategories().contains( "java" ), is( true ) );
    }

    @Test
    public void shouldGetNullDefaultModelTypeIfFileHasUknownMimeType() throws Exception {
        final String path = modeler().importArtifact( stream( "stuff" ), ARTIFACT_NAME );
        assertThat( modelTypeManager().defaultModelType( path ), nullValue() );
    }

    @Test
    public void shouldInstallModelTypes() throws Exception {
        modelTypeManager().registerModelTypeRepository( MODEL_TYPE_REPOSITORY );
        final Collection< String > potentialSequencerClassNames = modelTypeManager().install( "java" );
        assertThat( potentialSequencerClassNames.isEmpty(), is( true ) );
        assertThat( modelTypeManager().modelTypes().isEmpty(), is( false ) );
        final ModelTypeImpl type = ( ModelTypeImpl ) modelTypeManager().modelTypes().iterator().next();
        assertThat( type.category(), is( "java" ) );
        assertThat( type.sequencerClass, notNullValue() );
    }

    @Test
    public void shouldLoadState() throws Exception {
        modeler().close();
        int repos;
        try ( Modeler modeler = new ModeShapeModeler( TEST_REPOSITORY_STORE_PARENT_PATH ) ) {
            repos = modelTypeManager().modelTypeRepositories().size();
            final ModelTypeManager modelTypeManager = modeler.modelTypeManager();
            modelTypeManager.registerModelTypeRepository( MODEL_TYPE_REPOSITORY );
            modelTypeManager.install( "java" );
            modelTypeManager.install( "xsd" );
        }
        try ( ModeShapeModeler modeler = new ModeShapeModeler( TEST_REPOSITORY_STORE_PARENT_PATH ) ) {
            final ModelTypeManagerImpl modelTypeManager = ( ModelTypeManagerImpl ) modeler.modelTypeManager();
            assertThat( modelTypeManager.modelTypeRepositories().size(), not( repos ) );
            assertThat( modelTypeManager.modelTypes().isEmpty(), is( false ) );
            assertThat( modelTypeManager.libraryClassLoader.getURLs().length > 0, is( true ) );
            assertThat( modelTypeManager.potentialSequencerClassNamesByCategory.isEmpty(), is( false ) );
            TestUtil.manager( modeler ).run( modelTypeManager, new SystemTask< Void >() {

                @Override
                public Void run( final Session session,
                                 final Node systemNode ) throws Exception {
                    assertThat( systemNode.getProperty( ModelTypeManagerImpl.ZIPS ).getValues().length > 0, is( true ) );
                    return null;
                }
            } );
        }
    }

    @Test
    public void shouldMoveModelTypeRepositoryDown() throws Exception {
        final URL url = new URL( ModelTypeManager.JBOSS_MODEL_TYPE_REPOSITORY );
        final int size = modelTypeManager().modelTypeRepositories().size();
        assertThat( modelTypeManager().modelTypeRepositories().indexOf( url ), is( 0 ) );
        modelTypeManager().moveModelTypeRepositoryDown( url );
        assertThat( modelTypeManager().modelTypeRepositories().indexOf( url ), is( 1 ) );
        assertThat( modelTypeManager().modelTypeRepositories().size(), is( size ) );
    }

    @Test
    public void shouldMoveModelTypeRepositoryUp() throws Exception {
        final URL url = new URL( ModelTypeManager.MAVEN_MODEL_TYPE_REPOSITORY );
        final int size = modelTypeManager().modelTypeRepositories().size();
        assertThat( modelTypeManager().modelTypeRepositories().indexOf( url ), is( 1 ) );
        modelTypeManager().moveModelTypeRepositoryUp( url );
        assertThat( modelTypeManager().modelTypeRepositories().indexOf( url ), is( 0 ) );
        assertThat( modelTypeManager().modelTypeRepositories().size(), is( size ) );
    }

    @Test
    public void shouldNotInstallModelTypeCategoryIfAlreadyInstalled() throws Exception {
        manager().run( modelTypeManager(), new SystemTask< Void >() {

            @Override
            public Void run( final Session session,
                             final Node systemNode ) throws Exception {
                final String version = manager().repository.getDescriptor( Repository.REP_VERSION_DESC );
                final String archiveName = "modeshape-sequencer-test-" + version + "-module-with-dependencies.zip";
                final Value[] vals = new Value[] { session.getValueFactory().createValue( archiveName ) };
                systemNode.setProperty( ModelTypeManagerImpl.ZIPS, vals );
                session.save();
                return null;
            }
        } );
        modelTypeManager().registerModelTypeRepository( MODEL_TYPE_REPOSITORY );
        modelTypeManager().install( "test" );
        assertThat( modelTypeManager().modelTypes().isEmpty(), is( true ) );
    }

    @Test
    public void shouldNotMoveModelTypeRepositoryDownIfUrlLast() throws Exception {
        final List< URL > urls = modelTypeManager().modelTypeRepositories();
        final URL url = new URL( ModelTypeManager.MAVEN_MODEL_TYPE_REPOSITORY );
        modelTypeManager().moveModelTypeRepositoryDown( url );
        assertThat( modelTypeManager().modelTypeRepositories().indexOf( url ), is( 1 ) );
        assertThat( modelTypeManager().modelTypeRepositories().equals( urls ), is( true ) );
    }

    @Test
    public void shouldNotMoveModelTypeRepositoryUpIfUrlFirst() throws Exception {
        final List< URL > urls = modelTypeManager().modelTypeRepositories();
        final URL url = new URL( ModelTypeManager.JBOSS_MODEL_TYPE_REPOSITORY );
        modelTypeManager().moveModelTypeRepositoryUp( url );
        assertThat( modelTypeManager().modelTypeRepositories().equals( urls ), is( true ) );
    }

    @Test
    public void shouldNotReturnNullModelTypeCategories() throws Exception {
        assertThat( modelTypeManager().modelTypeCategories(), notNullValue() );
        assertThat( modelTypeManager().modelTypeCategories().isEmpty(), is( true ) );
    }

    @Test
    public void shouldNotReturnNullModelTypesForCategory() throws Exception {
        assertThat( modelTypeManager().modelTypesForCategory( "bogus" ), notNullValue() );
        assertThat( modelTypeManager().modelTypesForCategory( "bogus" ).isEmpty(), is( true ) );
    }

    @Test
    public void shouldRegisterModelTypeRepository() throws Exception {
        final int size = modelTypeManager().modelTypeRepositories().size();
        final List< URL > repos = modelTypeManager().registerModelTypeRepository( MODEL_TYPE_REPOSITORY );
        assertThat( repos, notNullValue() );
        assertThat( repos.size(), is( size + 1 ) );
        assertThat( repos.contains( MODEL_TYPE_REPOSITORY ), is( true ) );
    }

    @Test
    public void shouldUninstall() throws Exception {
        modelTypeManager().registerModelTypeRepository( MODEL_TYPE_REPOSITORY );
        modelTypeManager().install( "java" );
        assertThat( modelTypeManager().modelTypes().isEmpty(), is( false ) );
        modelTypeManager().uninstall( "java" );
        assertThat( modelTypeManager().modelTypes().isEmpty(), is( true ) );
        manager().run( modelTypeManager(), new SystemTask< Void >() {

            @Override
            public Void run( final Session session,
                             final Node systemNode ) throws Exception {
                assertThat( systemNode.getProperty( ModelTypeManagerImpl.ZIPS ).getValues().length, is( 0 ) );
                return null;
            }
        } );
    }

    @Test
    public void shouldUnregisterModelTypeRepository() throws Exception {
        final int size = modelTypeManager().modelTypeRepositories().size();
        final URL repo = new URL( "file:" );
        modelTypeManager().registerModelTypeRepository( repo );
        final List< URL > repos = modelTypeManager().unregisterModelTypeRepository( repo );
        assertThat( repos, notNullValue() );
        assertThat( repos.size(), is( size ) );
    }
}
