/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.idea.reference;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PsiJavaElementPattern;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;
import org.apache.camel.idea.util.CamelIdeaUtils;
import org.apache.camel.idea.util.JavaClassUtils;
import org.apache.camel.idea.util.JavaMethodUtils;
import org.apache.camel.idea.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import static com.intellij.patterns.PsiJavaPatterns.psiLiteral;
import static com.intellij.patterns.PsiJavaPatterns.psiMethod;

/**
 * Create a link between the Camel DSL {@Code bean(MyClass.class,"myMethod")} and the specific method
 * in it's destination bean.
 */
public class CamelBeanReferenceContributor extends PsiReferenceContributor {

    public static final PsiJavaElementPattern.Capture<PsiLiteral> BEAN_CLASS_METHOD_PATTERN = psiLiteral().methodCallParameter(
        psiMethod()
            .withName("bean")
            .withParameters("java.lang.Class", "java.lang.String")
    );

    public static final PsiJavaElementPattern.Capture<PsiLiteral> BEAN_OBJECT_STRING_PATTERN = psiLiteral().methodCallParameter(
        psiMethod()
            .withName("bean")
            .withParameters("java.lang.Object", "java.lang.String")
    );

    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(BEAN_CLASS_METHOD_PATTERN, new CamelPsiReferenceProvider() {
            @NotNull
            @Override
            public PsiReference[] getCamelReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
                return createCamelBeanMethodReference(element);
            }
        });

        registrar.registerReferenceProvider(BEAN_OBJECT_STRING_PATTERN, new CamelPsiReferenceProvider() {
            @NotNull
            @Override
            public PsiReference[] getCamelReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
                return createCamelDefaultBeanMethodReference(element);
            }
        });
    }

    private PsiReference[] createCamelBeanMethodReference(@NotNull PsiElement element) {

        if (element.getText().contains("IntellijIdeaRulezzz")) {
            return PsiReference.EMPTY_ARRAY;
        }

        PsiClass psiClass = getCamelIdeaUtils().getBean(element);
        if (psiClass != null) {
            //final PsiLiteral beanNameElement = PsiTreeUtil.findChildOfType(PsiTreeUtil.getParentOfType(beanClassElement, PsiExpressionList.class), PsiLiteral.class);
            String methodName = StringUtils.stripDoubleQuotes(element.getText());
            if (!methodName.isEmpty()) {
                return new PsiReference[] {new CamelBeanMethodReference(element, psiClass, methodName, new TextRange(1, methodName.length() + 1))};
            }
        }
        return PsiReference.EMPTY_ARRAY;
    }

    private PsiReference[] createCamelDefaultBeanMethodReference(@NotNull PsiElement element) {

        if (element.getText().contains("IntellijIdeaRulezzz")) {
            return PsiReference.EMPTY_ARRAY;
        }

        PsiClass psiClass = getCamelIdeaUtils().getBean(element);
        if (psiClass == null) {
           return PsiReference.EMPTY_ARRAY;
        }

        final String beanName = getJavaClassUtils().getBeanName(psiClass);
        final String methodName = StringUtils.stripDoubleQuotes(element.getText());

        if (methodName.equals(beanName)) {
            return PsiReference.EMPTY_ARRAY;
        }

        return getJavaMethodUtils().getHandleMethod(psiClass)
            .map(psiMethod -> new PsiReference[] {new CamelBeanMethodReference(element, psiClass, methodName, new TextRange(1, methodName.length() + 1))})
            .orElse(PsiReference.EMPTY_ARRAY);

    }

    private CamelIdeaUtils getCamelIdeaUtils() {
        return ServiceManager.getService(CamelIdeaUtils.class);
    }

    private JavaMethodUtils getJavaMethodUtils() {
        return ServiceManager.getService(JavaMethodUtils.class);
    }

    private JavaClassUtils getJavaClassUtils() {
        return ServiceManager.getService(JavaClassUtils.class);
    }

}
