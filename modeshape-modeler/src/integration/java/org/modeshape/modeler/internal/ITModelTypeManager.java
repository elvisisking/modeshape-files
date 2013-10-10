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
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import java.net.URL;
import java.util.Collection;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Session;

import org.junit.Test;
import org.modeshape.modeler.ModelType;
import org.modeshape.modeler.ModelTypeManager;
import org.modeshape.modeler.ModelerException;
import org.modeshape.modeler.integration.BaseIntegrationTest;

@SuppressWarnings( "javadoc" )
public class ITModelTypeManager extends BaseIntegrationTest {

    @Test
    public void shouldAllowRequestWhenModelTypeDoesNotHaveDependencyProcessor() throws Exception {
        manager().run( new Task< Void >() {

            @Override
            public Void run( final Session session ) throws Exception {
                final Node rootNode = session.getRootNode();
                final Node modelNode = rootNode.addNode( "elvis" );
                modelNode.addMixin( ModelerLexicon.MODEL_MIXIN );
                assertThat( modelTypeManager().dependencyProcessor( modelNode ), nullValue() );
                return null;
            }
        } );
    }

    @Test( expected = ModelerException.class )
    public void shouldFailToProcessDependenciesWhenNotAModelNode() throws Exception {
        final ModelTypeManagerImpl modelTypeMgr = modelTypeManager();
        modelTypeManager().manager.run( new Task< Void >() {

            @Override
            public Void run( final Session session ) throws Exception {
                final Node rootNode = session.getRootNode();
                modelTypeMgr.dependencyProcessor( rootNode );
                return null;
            }
        } );
    }

    @Test
    public void shouldGetApplicableModelTypeManager() throws Exception {
        modelTypeManager().install( "sramp" );
        modelTypeManager().install( "xsd" );
        final Set< ModelType > types = modelTypeManager().modelTypesForArtifact( importArtifact( XSD_ARTIFACT ) );
        assertThat( types, notNullValue() );
        assertThat( types.isEmpty(), is( false ) );
    }

    @Test
    public void shouldGetModelTypeCategories() throws Exception {
        assertThat( modelTypeManager().installableModelTypeCategories().isEmpty(), is( false ) );
    }

    @Test
    public void shouldIniitializeModelTypeRepositories() throws Exception {
        assertThat( modelTypeManager().modelTypeRepositories().contains( new URL( ModelTypeManager.JBOSS_MODEL_TYPE_REPOSITORY ) ), is( true ) );
        assertThat( modelTypeManager().modelTypeRepositories().contains( new URL( ModelTypeManager.MAVEN_MODEL_TYPE_REPOSITORY ) ), is( true ) );
    }

    @Test
    public void shouldInstallModelTypes() throws Exception {
        final Collection< String > potentialSequencerClassNames = modelTypeManager().install( "java" );
        assertThat( potentialSequencerClassNames.isEmpty(), is( true ) );
        assertThat( modelTypeManager().modelTypes().isEmpty(), is( false ) );
    }

    @Test
    public void shouldOnlyInstallModelTypeCategoryOnce() throws Exception {
        modelTypeManager().install( "java" );
        assertThat( modelTypeManager().modelTypes().isEmpty(), is( false ) );
        final int size = modelTypeManager().modelTypes().size();
        modelTypeManager().install( "java" );
        assertThat( modelTypeManager().modelTypes().size(), is( size ) );
    }

    @Test
    public void shouldReturnUninstantiablePotentialSequencerClassNames() throws Exception {
        Collection< String > potentialSequencerClassNames = modelTypeManager().install( "xsd" );
        assertThat( potentialSequencerClassNames.isEmpty(), is( false ) );
        potentialSequencerClassNames = modelTypeManager().install( "sramp" );
        assertThat( potentialSequencerClassNames.isEmpty(), is( true ) );
    }
}
