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
package org.modeshape.modeler.extensions;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;

/**
 * A test for the {@link Dependency} class.
 */
@SuppressWarnings( "javadoc" )
public class DependencyTest {

    @Test
    public void shouldAddSourceReference() {
        final Dependency dep = new Dependency( "/my/path", false );
        final String input = "input";
        dep.addSourceReference( input );

        final List< String > srcRefs = dep.sourceReferences();
        assertThat( srcRefs.size(), is( 1 ) );
        assertThat( srcRefs.get( 0 ), is( input ) );
    }

    @SuppressWarnings( "unused" )
    @Test
    public void shouldAllowEmptyPath() {
        new Dependency( "", false );
    }

    @SuppressWarnings( "unused" )
    @Test
    public void shouldAllowNullPath() {
        new Dependency( null, false );
    }

    @Test
    public void shouldHaveCorrectDependencyExists() {
        { // exists = true
            final boolean exists = true;
            final Dependency dep = new Dependency( "/my/path", exists );
            assertThat( dep.exists(), is( exists ) );
        }

        { // exists = false
            final boolean exists = false;
            final Dependency dep = new Dependency( "/my/path", exists );
            assertThat( dep.exists(), is( exists ) );
        }
    }

    @Test
    public void shouldHaveCorrectDependencyPath() {
        final String path = "/my/path";
        final Dependency dep = new Dependency( path, false );
        assertThat( dep.path(), is( path ) );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowEmptySourceReference() {
        final Dependency dep = new Dependency( "/my/path", false );
        dep.addSourceReference( "" );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullSourceReference() {
        final Dependency dep = new Dependency( "/my/path", false );
        dep.addSourceReference( null );
    }

    @Test
    public void shouldNotHaveSourceReferencesAfterConstruction() {
        final Dependency dep = new Dependency( "/my/path", false );
        assertThat( dep.sourceReferences(), is( notNullValue() ) );
        assertThat( dep.sourceReferences().size(), is( 0 ) );
    }

}
