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
package org.modeshape.modeler.java;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import javax.jcr.Node;
import javax.jcr.Session;

import org.junit.Test;
import org.modeshape.modeler.Model;
import org.modeshape.modeler.ModelType;
import org.modeshape.modeler.integration.BaseIntegrationTest;
import org.modeshape.modeler.internal.Task;

@SuppressWarnings( "javadoc" )
public class ITJavaDependencyProcessor extends BaseIntegrationTest {

    @Test
    public void shouldDetermineDependencies() throws Exception {
        assertThat( modelTypeManager().install( "java" ).length == 0, is( true ) );
        final String name = JavaDependencyProcessor.class.getName();
        // final File file = new File( "src/main/java/" + name.replace( '.', '/' ) + ".java" );
        // assertThat( file.exists(), is( true ) );
        // final ModelType modelType = modelTypeManager().modelType( "org.modeshape.modeler.java.JavaFile" );
        final ModelType modelType = modelTypeManager().modelType( "org.modeshape.modeler.java.ClassFile" );
        assertThat( modelType, notNullValue() );
        // final Model model =
        // modeler().generateModel( file, JavaDependencyProcessor.class.getPackage().getName().replace( '.', '/' ), modelType );
        final Model model =
            modeler().generateModel( getClass().getClassLoader().getResourceAsStream( name.replace( '.', '/' ) + ".class" ),
                                     JavaDependencyProcessor.class.getName().replace( '.', '/' ) + ".java", modelType );
        assertThat( model, notNullValue() );
        manager().run( new Task< Void >() {

            @Override
            public Void run( final Session session ) throws Exception {
                final Node modelNode =
                    session.getNode( '/' + JavaDependencyProcessor.class.getName().replace( '.', '/' ) + ".java" );
                final JavaDependencyProcessor processor = new JavaDependencyProcessor();
                processor.process( modelNode, modeler() );
                return null;
            }
        } );
    }
}
