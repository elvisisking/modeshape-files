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

import org.junit.Test;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.modeler.Model;
import org.modeshape.modeler.ModelObject;
import org.modeshape.modeler.ModelType;

@SuppressWarnings( "javadoc" )
public class ModelImplTest extends BaseModelObjectImplTest {

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetStringValueIfMultiValuedProperty() throws Exception {
        modelObject().stringValue( JcrLexicon.MIXIN_TYPES.toString() );
    }

    @Test
    public void shouldGetAbsolutePath() throws Exception {
        assertThat( modelObject().absolutePath(), is( '/' + MODEL_NAME ) );
    }

    @Test
    public void shouldGetChild() throws Exception {
        final ModelObject child = modelObject().child( XML_ROOT );
        assertThat( child, notNullValue() );
        assertThat( child.name(), is( XML_ROOT ) );
    }

    @Test
    public void shouldGetChildren() throws Exception {
        final ModelObject modelObject = modelObject();
        assertThat( modelObject.children(), notNullValue() );
        assertThat( modelObject.children().length, is( 1 ) );
    }

    @Test
    public void shouldGetChildrenMatchingPattern() throws Exception {
        final ModelObject[] children = modelObject().children( XML_ROOT );
        assertThat( children, notNullValue() );
        assertThat( children.length, is( 1 ) );
        assertThat( children[ 0 ].name(), is( XML_ROOT ) );
    }

    @Test
    public void shouldGetExternalLocation() throws Exception {
        modelTypeManager().install( XML_MODEL_TYPE_CATEGORY );
        final Model model = modeler().generateModel( new URL( "file:src/test/resources/Books.xsd" ),
                                                     null,
                                                     modelTypeManager().modelType( XML_MODEL_TYPE_NAME ) );
        assertThat( model, notNullValue() );
    }

    @Test
    public void shouldGetIndex() throws Exception {
        assertThat( modelObject().index(), is( -1 ) );
    }

    @Test
    public void shouldGetMixinTypes() throws Exception {
        final String[] types = modelObject().mixinTypes();
        assertThat( types, notNullValue() );
        assertThat( types.length > 0, is( true ) );
        assertThat( types[ 0 ], is( ModelerLexicon.MODEL_MIXIN ) );
    }

    @Test
    public void shouldGetModel() throws Exception {
        final ModelObject modelObject = modelObject();
        assertThat( modelObject.model(), notNullValue() );
        assertThat( modelObject.model(), is( modelObject ) );
    }

    @Test
    public void shouldGetModelRelativePath() throws Exception {
        assertThat( modelObject().modelRelativePath(), is( "" ) );
    }

    @Test
    public void shouldGetModelType() throws Exception {
        final ModelType type = ( ( Model ) modelObject() ).modelType();
        assertThat( type, notNullValue() );
        assertThat( type.name(), is( XML_MODEL_TYPE_NAME ) );
    }

    @Test
    public void shouldGetName() throws Exception {
        assertThat( modelObject().name(), is( MODEL_NAME ) );
    }

    @Test
    public void shouldGetNullExternalLocation() throws Exception {
        assertThat( ( ( Model ) modelObject() ).externalLocation(), nullValue() );
    }

    @Test
    public void shouldGetPrimaryType() throws Exception {
        assertThat( modelObject().primaryType(), is( "modexml:document" ) );
    }

    @Test
    public void shouldGetPropertyNames() throws Exception {
        final ModelObject modelObject = modelObject();
        assertThat( modelObject.propertyNames(), notNullValue() );
        assertThat( modelObject.propertyNames().length, is( 0 ) );
    }

    @Test
    public void shouldGetStringValue() throws Exception {
        assertThat( modelObject().stringValue( JcrLexicon.PRIMARY_TYPE.toString() ), is( "modexml:document" ) );
    }

    @Test
    public void shouldIndicateIfChildHasSameNameSiblings() throws Exception {
        final ModelObject modelObject = modelObject();
        assertThat( modelObject.childHasSameNameSiblings( XML_ROOT ), is( false ) );
        assertThat( modelObject.childHasSameNameSiblings( "bogus" ), is( false ) );
    }

    @Test
    public void shouldIndicateIfItHasChild() throws Exception {
        final ModelObject modelObject = modelObject();
        assertThat( modelObject.hasChild( XML_ROOT ), is( true ) );
        assertThat( modelObject.hasChild( "bogus" ), is( false ) );
    }

    @Test
    public void shouldIndicateIfItHasChildren() throws Exception {
        assertThat( modelObject().hasChildren(), is( true ) );
    }

    @Test
    public void shouldIndicateIfItHasProperties() throws Exception {
        assertThat( modelObject().hasProperties(), is( false ) );
    }

    @Test
    public void shouldIndicateIfItHasProperty() throws Exception {
        final ModelObject modelObject = modelObject();
        assertThat( modelObject.hasProperty( ModelerLexicon.MODEL_TYPE ), is( true ) );
        assertThat( modelObject.hasProperty( "bogus" ), is( false ) );
    }

    @Test
    public void shouldIndicateIfPropertyHasMultipleValues() throws Exception {
        final ModelObject modelObject = modelObject();
        assertThat( modelObject.propertyHasMultipleValues( JcrLexicon.PRIMARY_TYPE.toString() ), is( false ) );
        assertThat( modelObject.propertyHasMultipleValues( JcrLexicon.MIXIN_TYPES.toString() ), is( true ) );
        assertThat( modelObject.propertyHasMultipleValues( "bogus" ), is( false ) );
    }
}
