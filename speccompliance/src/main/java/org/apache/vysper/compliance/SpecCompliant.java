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

package org.apache.vysper.compliance;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to mark methods, types or packages that implement a
 * specific RFC or a section of an RFC. Use the <code>status</code> parameter
 * to specify the compliance level.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * 
 */
@Documented
@Target( { ElementType.METHOD, ElementType.TYPE, ElementType.FIELD })
@Retention(RetentionPolicy.SOURCE)
public @interface SpecCompliant {
    public enum ComplianceStatus {
        NOT_STARTED, IN_PROGRESS, FINISHED
    }

    public enum ComplianceCoverage {
        UNKNOWN, UNSUPPORTED, // the spec is not supported
        PARTIAL, // spec is partially covered 
        COMPLETE
        // spec is completely covered
    }

    /**
     * References a RFC or XEP specification document.
     * 
     * @return the RFC or XEP 
     */
    String spec();

    /**
     * References the section in the specificiation document the target implements.
     * 
     * @return the section/appendix number or an empty {@link String} in case
     *         the entire RFC is implemented by the target
     */
    String section() default "";

    /**
     * Specifies the status of the RFC compliance.
     * 
     * @return the compliance status
     */
    ComplianceStatus status() default ComplianceStatus.IN_PROGRESS;

    /**
     * Specifies the level of coverage for the referenced spec/section. If known, the referenced spec extract
     * can be covered fully or partially. In the latter case, other code might reference other parts of the the
     * same extract. 
     * @return coverage level
     */
    ComplianceCoverage coverage() default ComplianceCoverage.UNKNOWN;

    /**
     * short text being more specific on coverage and other implementational aspects 
     */
    String comment() default "";
}
