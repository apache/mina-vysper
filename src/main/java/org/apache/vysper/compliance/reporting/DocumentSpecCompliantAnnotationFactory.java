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
package org.apache.vysper.compliance.reporting;

import com.sun.mirror.apt.AnnotationProcessor;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.apt.AnnotationProcessorFactory;
import com.sun.mirror.apt.Filer;
import com.sun.mirror.declaration.*;
import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.compliance.SpecCompliance;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableCollection;
import java.util.*;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.File;

/**
 * this generates from all SpecCompliant annotations a HTML doc to show what parts of the spec are covered.
 * The table is sorted by implemented spec section and links out to the spec and the apidocs.
 *
 * apt can be invoked like this (tools.jar must be on the classpath)
 * apt -cp build/ant/classes;lib/* -s src/main/java  -factory org.apache.vysper.compliance.reporting.DocumentSpecCompliantAnnotationFactory
 * it generates a file spec_compliance.html in src/main/java
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev: 671827 $, $Date: 2008-06-26 14:19:48 +0530 (Thu, 26 Jun 2008) $
 */
public class DocumentSpecCompliantAnnotationFactory implements AnnotationProcessorFactory {
    // Process any set of annotations
    private static final Collection<String> supportedAnnotations
        = unmodifiableCollection(Arrays.asList("org.apache.vysper.compliance.*"));

    // No supported options
    private static final Collection<String> supportedOptions = emptySet();

    public Collection<String> supportedAnnotationTypes() {
        return supportedAnnotations;
    }

    public Collection<String> supportedOptions() {
        return supportedOptions;
    }

    public AnnotationProcessor getProcessorFor(
            Set<AnnotationTypeDeclaration> atds,
            AnnotationProcessorEnvironment env) {
        return new SpecCompliantClassAP(env);
    }

    private static class SpecCompliantClassAP implements AnnotationProcessor {

        protected final AnnotationProcessorEnvironment env;
        protected AnnotationTypeDeclaration specCompliantAnnotation;
        protected AnnotationTypeDeclaration specCompliantCollectionAnnotation;

        protected Map<String, SpecDoc> map = new TreeMap<String, SpecDoc>();
        protected PrintWriter fileWriter;

        SpecCompliantClassAP(AnnotationProcessorEnvironment env) {
            this.env = env;
            specCompliantAnnotation = (AnnotationTypeDeclaration) env.getTypeDeclaration("org.apache.vysper.compliance.SpecCompliant");
            specCompliantCollectionAnnotation = (AnnotationTypeDeclaration) env.getTypeDeclaration("org.apache.vysper.compliance.SpecCompliance");

            try {
                fileWriter = env.getFiler().createTextFile(Filer.Location.SOURCE_TREE, "", new File("spec_compliance.html"), null);
            } catch (IOException e) {
                throw new RuntimeException("could not write to output file", e);
            }
        }

        public void process() {

            // Retrieve all declarations with SpecCompliant annotations
            Collection<Declaration> declarations = env.getDeclarationsAnnotatedWith(specCompliantAnnotation);
            System.out.println("number of solitairy @SpecCompliant: " + declarations.size());
            for (Declaration declaration : declarations) {
                final SpecDoc doc = new SpecDoc(declaration, declaration.getAnnotation(SpecCompliant.class));
                map.put(doc.getKey(), doc);
            }

            // Retrieve all SpecCompliance-typed declarations and extract SpecCompliant annotations 
            Collection<Declaration> moreDeclarations = env.getDeclarationsAnnotatedWith(specCompliantCollectionAnnotation);
            System.out.println("number of @SpecCompliance: " + moreDeclarations.size());
            for (Declaration declaration : moreDeclarations) {
                final SpecCompliance compliance = declaration.getAnnotation(SpecCompliance.class);
                final SpecCompliant[] specCompliants = compliance.compliant();
                for (SpecCompliant specCompliant : specCompliants) {
                    if (specCompliant == null) continue;

                    final SpecDoc doc = new SpecDoc(declaration, specCompliant);
                    map.put(doc.getKey(), doc);
                }
            }

            // visit every annotation fromt the now sorted map

            // write the HTML file
            fileWriter.println("<html><head>" +
                    "<link rel='stylesheet' type='text/css' href='http://yui.yahooapis.com/2.7.0/build/reset-fonts-grids/reset-fonts-grids.css'>\n" +
                    "<link rel='stylesheet' type='text/css' href='http://yui.yahooapis.com/2.7.0/build/base/base-min.css'>\n" + 
                    "</head><body><table><thead><th>spec</th><th>section</th><th>package</th><th>class</th><th>field/method/etc.</th><th>coverage</th><th>implementation</th><th>comment</th></thead>");
            for (String key : map.keySet()) {
                fileWriter.print("<tr>");
                System.out.println(key);
                final SpecDoc doc = map.get(key);

                // spec
                fileWriter.print("<td>");
                final String specURL = doc.getSpecDocURL();
                if (specURL != null) fileWriter.print("<a href='" + specURL + "'>");
                final String specDoc = doc.getSpecDoc();
                if (specDoc != null) fileWriter.print(specDoc);
                if (specURL != null) fileWriter.print("</a>"); 
                fileWriter.print("</td>");


                // spec section
                fileWriter.print("<td>");
                String specSection = doc.getSpecSection();
                if (specSection != null && specSection.length() > 0) {
                    if (specSection.endsWith(".")) specSection = specSection.substring(0, specSection.length() - 1);
                    String anchorLink = Character.isDigit(specSection.charAt(0)) ? "#section-" : "#appendix-";  
                    if (specURL != null) fileWriter.print("<a href='" + specURL + anchorLink + specSection + "'>");
                    fileWriter.print(specSection);
                    if (specURL != null) fileWriter.print("</a>");
                }
                fileWriter.print("</td>");


                // package
                fileWriter.print("<td>");
                final String packageName = doc.getPackage();
                if (packageName != null) {
                    fileWriter.print(o_a_v_shortened(packageName));
                }
                fileWriter.print("</td>");

                // class
                fileWriter.print("<td>");
                final String className = doc.getClassName();
                if (className != null) {
                    fileWriter.print("<a href='" + doc.getFQClassName().replace(".", "/") + ".html'>");
                    fileWriter.print(o_a_v_cut(packageName, className));
                    fileWriter.print("</a>");
                }
                fileWriter.print("</td>");


                // class element
                fileWriter.print("<td>");
                final String member = doc.getMember();
                if (member != null) {
                    if (className != null) {
                        fileWriter.print("<a href='" + className.replace(".", "/") + ".html#" + doc.getMemberAnchor().replace(",", ",%20") + "'>");
                    }
                    fileWriter.print(o_a_v_cut(packageName, member));
                    if (className != null) {
                        fileWriter.print("</a>");
                    }
                }
                fileWriter.print("</td>");


                // coverage
                fileWriter.print("<td>");
                final SpecCompliant.ComplianceCoverage coverage = doc.getCoverageLevel();
                if (coverage != null) fileWriter.print(coverage.toString().toLowerCase());
                fileWriter.print("</td>");


                // status
                fileWriter.print("<td>");
                final SpecCompliant.ComplianceStatus complianceStatus = doc.getComplianceStatus();
                if (complianceStatus != null) fileWriter.print(complianceStatus.toString().toLowerCase());
                fileWriter.print("</td>");


                // comment
                fileWriter.print("<td>");
                final String comment = doc.getComment();
                if (comment != null) fileWriter.print(comment);
                fileWriter.print("</td>");

                fileWriter.println("</tr>");

            }
            fileWriter.println("</table></body>");
        }

        private String o_a_v_shortened(String packageString) {
            if (packageString != null && packageString.contains("org.apache.vysper.")) return packageString.replace("org.apache.vysper.", "o.a.v.");
            return packageString;
        }

        private String o_a_v_cut(String packageString, String memberString) {
            if (memberString != null && memberString.contains(packageString + ".")) return memberString.replace(packageString + ".", "");
            return memberString;
        }

    }

    static class SpecDoc {

        Declaration declaration;
        SpecCompliant specCompliant;

        SpecDoc(Declaration declaration, SpecCompliant specCompliant) {
            this.declaration = declaration;
            this.specCompliant = specCompliant;
        }

        public String getClassName() {
            if (declaration instanceof FieldDeclaration || declaration instanceof MethodDeclaration) {
                MemberDeclaration memberDeclaration = (MemberDeclaration) declaration;
                return memberDeclaration.getDeclaringType().getQualifiedName();
            } else {
                return declaration.getSimpleName();
            }
        }

        public String getFQClassName() {
            if (declaration instanceof FieldDeclaration || declaration instanceof MethodDeclaration) {
                MemberDeclaration memberDeclaration = (MemberDeclaration) declaration;
                return memberDeclaration.getDeclaringType().getQualifiedName();
            } else if(declaration instanceof ClassDeclaration) {
                ClassDeclaration classDeclaration = (ClassDeclaration) declaration;
                return classDeclaration.getPackage() + "." + classDeclaration.getSimpleName();
            } else {
                return declaration.getSimpleName();
            }
        }

        public String getPackage() {
            if (declaration instanceof TypeDeclaration ) {
                TypeDeclaration typeDeclaration = (TypeDeclaration) declaration;
                return typeDeclaration.getPackage().getQualifiedName(); 
            } else if (declaration instanceof FieldDeclaration || declaration instanceof MethodDeclaration) {
                MemberDeclaration memberDeclaration = (MemberDeclaration) declaration;
                return memberDeclaration.getDeclaringType().getPackage().getQualifiedName();
            } else {
                return null;
            }
        }

        public String getMember() {
            if (declaration instanceof FieldDeclaration || declaration instanceof MethodDeclaration) {
                MemberDeclaration memberDeclaration = (MemberDeclaration) declaration;
                return declaration.getSimpleName();
            } else {
                return null;
            }
        }

        public String getMemberAnchor() {
            if (declaration instanceof FieldDeclaration || declaration instanceof MethodDeclaration) {
                MemberDeclaration memberDeclaration = (MemberDeclaration) declaration;
                return declaration.toString();
            } else {
                return null;
            }
        }

        public String getKey() {
            return getSpecDoc() + " " + getSpecSection() + " " + getClassName() + " " + getMember() + " " + getCoverageLevel() + " " + getComplianceStatus();
        }

        public String getSpecSection() {
            return specCompliant.section();
        }

        public String getSpecDoc() {
            final String specRaw = specCompliant.spec();
            if (specRaw == null) return null;
            return specRaw.toLowerCase();
        }

        public String getSpecDocURL() {
            final String spec = getSpecDoc();
            if (spec == null) return null;

            if (spec.startsWith("xep")) return "http://xmpp.org/extensions/" + spec + ".html";
            if (spec.startsWith("rfc")) {
                if (!spec.contains("bis")) {
                    return "http://tools.ietf.org/html/" + spec;
                } else {
                    return "http://tools.ietf.org/html/draft-saintandre-" + spec;
                }
            }
            return null;
        }

        public SpecCompliant.ComplianceCoverage getCoverageLevel() {
            return specCompliant.coverage();
        }

        public SpecCompliant.ComplianceStatus getComplianceStatus() {
            return specCompliant.status();
        }

        public String getComment() {
            return specCompliant.comment();
        }
    }
}
