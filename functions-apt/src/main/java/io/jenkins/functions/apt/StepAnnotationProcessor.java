/**
 * Copyright (C) Original Authors 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.jenkins.functions.apt;


import io.jenkins.functions.Argument;
import io.jenkins.functions.Step;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.beans.Introspector;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Processes all {@link Step} functions and generates the metadata for them
 */
@SupportedAnnotationTypes({"*"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class StepAnnotationProcessor extends AbstractAnnotationProcessor {

    public boolean process(Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return true;
        }
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Step.class);
        if (!elements.isEmpty()) {
            Properties properties = new Properties();
            for (Element element : elements) {
                if (element instanceof TypeElement) {
                    processStepClass(roundEnv, (TypeElement) element, properties);
                }
            }

            if (!properties.isEmpty()) {
                StringWriter writer = new StringWriter();
                try {
                    properties.store(writer, "Generated by functions-apt");
                    String text = writer.toString();

                    writeFile("io.jenkins.functions", "steps.properties", text);
                } catch (IOException e) {
                    log(e);
                }
            }
        }
        return true;
    }

    protected void processStepClass(final RoundEnvironment roundEnv, final TypeElement element, Properties properties) {
        final Step property = element.getAnnotation(Step.class);
        if (property != null) {
            String name = property.name();
            if (Strings.isNullOrEmpty(name)) {
                name = Introspector.decapitalize(element.getSimpleName().toString());
            }
            String javaTypeName = javaTypeName(element);
            properties.put(name, javaTypeName);

            // TODO write the step file!

            StringWriter writer = new StringWriter();
            writer.append("step {\n" +
                    "  metadata {\n" +
                    "    name '" + name + "'\n" +
                    "  }\n" +
                    "  args {\n");

            List<VariableElement> fields = findAllFields(element);
            for (VariableElement fieldElement : fields) {
                Argument argument = fieldElement.getAnnotation(Argument.class);
                if (argument != null) {
                    String argName = argument.name();
                    String description = argument.description();
                    if (Strings.isNullOrEmpty(argName)) {
                        argName = fieldElement.getSimpleName().toString();
                    }

                    if (Strings.notEmpty(argName)) {
                        writer.append("    arg {\n" +
                                "      name '" + argName + "'\n");

                        if (Strings.notEmpty(description)) {
                            writer.append("      description '" + description + "'\n");
                        }
                        String argTypeName = javaTypeName(fieldElement);
                        if (Strings.notEmpty(argTypeName)) {
                            writer.append("      className '" + argTypeName + "'\n");
                        }
                        writer.append("    }\n");
                    }
                }
            }
            writer.append("  }\n" +
                    "  steps {\n" +
                    "    javaStepFunction  '" + name + " ${args}'\n" +
                    "  }\n" +
                    "}\n");

            String stepMarkup = writer.toString();
            writeFile("io.jenkins.functions", name + ".step", stepMarkup);
        }
    }

    protected List<VariableElement> findAllFields(TypeElement element) {
        List<VariableElement> allFieldElements = new ArrayList<>();
        TypeElement e = element;
        while (true) {
            List<VariableElement> fieldElements = ElementFilter.fieldsIn(e.getEnclosedElements());
            allFieldElements.addAll(fieldElements);

            TypeMirror superclass = e.getSuperclass();
            if (superclass instanceof DeclaredType) {
                DeclaredType declaredType = (DeclaredType) superclass;
                Element typeElement = declaredType.asElement();
                String superclassName = javaTypeName(typeElement);
                if (Strings.isNullOrEmpty(superclassName) || superclassName.equals("java.lang.Object")) {
                    break;
                }
                if (typeElement instanceof TypeElement) {
                    e = (TypeElement) typeElement;
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        return allFieldElements;
    }
}
