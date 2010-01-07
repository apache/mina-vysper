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
package org.apache.vysper.xml.sax.perf;

import org.apache.mina.common.ByteBuffer;
import org.apache.vysper.charset.CharsetUtil;
import org.apache.vysper.xml.sax.impl.DefaultNonBlockingXMLReader;

/**
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class PerfTest  {

	public static void main(String[] args) throws Exception {
		
		ByteBuffer opening = ByteBuffer.wrap("<p:root xmlns:p='http://example.com'>".getBytes("UTF-8"));
		ByteBuffer buffer = ByteBuffer.wrap("<child att='foo' att2='bar' />text".getBytes("UTF-8"));
		
		DefaultNonBlockingXMLReader reader = new DefaultNonBlockingXMLReader();

		StopWatch watch = new StopWatch();
		
		reader.parse(opening, CharsetUtil.UTF8_DECODER);
		for(int i = 0; i<10000; i++) {
			buffer.position(0);
			reader.parse(buffer, CharsetUtil.UTF8_DECODER);
		}
		watch.stop();

		System.out.println(watch);
		
	}

}