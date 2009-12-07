/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.vysper.xml.decoder;

import junit.framework.TestCase;

import java.util.List;
import java.util.ArrayList;

import org.apache.vysper.xml.decoder.DecodingException;
import org.apache.vysper.xml.decoder.XMLParticle;
import org.apache.vysper.xml.decoder.XMLRawToFragmentConverter;

/**
 */
public class XMLRawToFragmentConverterBalancedTestCase extends TestCase {

    public void testSimple1() throws DecodingException {
        List<XMLParticle> particles = new ArrayList<XMLParticle>();

        particles.add(new XMLParticle("<balanced>"));
        particles.add(new XMLParticle("</balanced>"));
        
        assertTrue(new XMLRawToFragmentConverter().isBalanced(particles));
    }
    public void testSimple2() throws DecodingException {
        List<XMLParticle> particles = new ArrayList<XMLParticle>();

        particles.add(new XMLParticle("<balanced>"));
        particles.add(new XMLParticle("plain"));
        particles.add(new XMLParticle("</balanced>"));
        
        assertTrue(new XMLRawToFragmentConverter().isBalanced(particles));
    }
    public void testSimple3() throws DecodingException {
        List<XMLParticle> particles = new ArrayList<XMLParticle>();

        particles.add(new XMLParticle("<balanced/>"));
        
        assertTrue(new XMLRawToFragmentConverter().isBalanced(particles));
    }
    public void testSimple4() throws DecodingException {
        List<XMLParticle> particles = new ArrayList<XMLParticle>();

        particles.add(new XMLParticle("balanced"));
        
        assertTrue(new XMLRawToFragmentConverter().isBalanced(particles));
    }
    public void testSimple5() throws DecodingException {
        List<XMLParticle> particles = new ArrayList<XMLParticle>();

        particles.add(new XMLParticle("<unbalanced>"));
        
        assertFalse(new XMLRawToFragmentConverter().isBalanced(particles));
    }
    public void testSimple6() throws DecodingException {
        List<XMLParticle> particles = new ArrayList<XMLParticle>();

        particles.add(new XMLParticle("<unbalanced>"));
        particles.add(new XMLParticle("<unbalanced2>"));
        
        assertFalse(new XMLRawToFragmentConverter().isBalanced(particles));
    }
    public void testSimple7() throws DecodingException {
        List<XMLParticle> particles = new ArrayList<XMLParticle>();

        particles.add(new XMLParticle("<balanced>"));
        particles.add(new XMLParticle("</balanced>"));
        particles.add(new XMLParticle("<unbalanced>"));
        
        assertFalse(new XMLRawToFragmentConverter().isBalanced(particles));
    }


    public void testNested1() throws DecodingException {
        List<XMLParticle> particles = new ArrayList<XMLParticle>();

        particles.add(new XMLParticle("<balanced>"));
        particles.add(new XMLParticle("<inner/>"));
        particles.add(new XMLParticle("</balanced>"));
        
        assertTrue(new XMLRawToFragmentConverter().isBalanced(particles));
    }
    public void testNested2() throws DecodingException {
        List<XMLParticle> particles = new ArrayList<XMLParticle>();

        particles.add(new XMLParticle("<balanced>"));
        particles.add(new XMLParticle("inner"));
        particles.add(new XMLParticle("</balanced>"));
        
        assertTrue(new XMLRawToFragmentConverter().isBalanced(particles));
    }
    public void testNested3() throws DecodingException {
        List<XMLParticle> particles = new ArrayList<XMLParticle>();

        particles.add(new XMLParticle("<balanced>"));
        particles.add(new XMLParticle("<inner>"));
        particles.add(new XMLParticle("</inner>"));
        particles.add(new XMLParticle("</balanced>"));
        
        assertTrue(new XMLRawToFragmentConverter().isBalanced(particles));
    }
    public void testNested4() throws DecodingException {
        List<XMLParticle> particles = new ArrayList<XMLParticle>();

        particles.add(new XMLParticle("<unbalanced>"));
        particles.add(new XMLParticle("<inner>"));
        particles.add(new XMLParticle("</inner>"));
        
        assertFalse(new XMLRawToFragmentConverter().isBalanced(particles));
    }
}
