/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.common.internal.epl.expression.dot.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.dot.inner.*;
import com.espertech.esper.common.internal.epl.join.analyze.FilterExprAnalyzerAffector;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationBuilderExpr;
import com.espertech.esper.common.internal.rettype.*;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constantNull;

public class ExprDotNodeForgeRootChild extends ExprDotNodeForge implements ExprEnumerationForge {

    private final ExprDotNodeImpl parent;
    private final FilterExprAnalyzerAffector filterExprAnalyzerAffector;
    private final Integer streamNumReferenced;
    private final String rootPropertyName;
    protected final ExprDotEvalRootChildInnerForge innerForge;
    protected final ExprDotForge[] forgesIteratorEventBean;
    protected final ExprDotForge[] forgesUnpacking;

    public ExprDotNodeForgeRootChild(ExprDotNodeImpl parent, FilterExprAnalyzerAffector filterExprAnalyzerAffector, Integer streamNumReferenced, String rootPropertyName, boolean hasEnumerationMethod, ExprForge rootNodeForge, ExprEnumerationForge rootLambdaEvaluator, EPType typeInfo, ExprDotForge[] forgesIteratorEventBean, ExprDotForge[] forgesUnpacking, boolean checkedUnpackEvent) {
        if (forgesUnpacking.length == 0) {
            throw new IllegalArgumentException("Empty forges-unpacking");
        }
        this.parent = parent;
        this.filterExprAnalyzerAffector = filterExprAnalyzerAffector;
        this.streamNumReferenced = streamNumReferenced;
        this.rootPropertyName = rootPropertyName;
        if (rootLambdaEvaluator != null) {
            if (typeInfo instanceof EventMultiValuedEPType) {
                innerForge = new InnerDotEnumerableEventCollectionForge(rootLambdaEvaluator, ((EventMultiValuedEPType) typeInfo).getComponent());
            } else if (typeInfo instanceof EventEPType) {
                innerForge = new InnerDotEnumerableEventBeanForge(rootLambdaEvaluator, ((EventEPType) typeInfo).getType());
            } else {
                innerForge = new InnerDotEnumerableScalarCollectionForge(rootLambdaEvaluator, ((ClassMultiValuedEPType) typeInfo).getComponent());
            }
        } else {
            if (checkedUnpackEvent) {
                innerForge = new InnerDotScalarUnpackEventForge(rootNodeForge);
            } else {
                Class returnType = rootNodeForge.getEvaluationType();
                if (hasEnumerationMethod && returnType.isArray()) {
                    if (returnType.getComponentType().isPrimitive()) {
                        innerForge = new InnerDotArrPrimitiveToCollForge(rootNodeForge);
                    } else {
                        innerForge = new InnerDotArrObjectToCollForge(rootNodeForge);
                    }
                } else if (hasEnumerationMethod && JavaClassHelper.isImplementsInterface(returnType, Collection.class)) {
                    innerForge = new InnerDotCollForge(rootNodeForge);
                } else {
                    innerForge = new InnerDotScalarForge(rootNodeForge);
                }
            }
        }
        this.forgesUnpacking = forgesUnpacking;
        this.forgesIteratorEventBean = forgesIteratorEventBean;
    }

    public ExprDotNodeForgeRootChildEval getExprEvaluator() {
        return new ExprDotNodeForgeRootChildEval(this, innerForge.getInnerEvaluator(), ExprDotNodeUtility.getEvaluators(forgesIteratorEventBean), ExprDotNodeUtility.getEvaluators(forgesUnpacking));
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return new InstrumentationBuilderExpr(this.getClass(), this, "ExprDot", requiredType, codegenMethodScope, exprSymbol, codegenClassScope).build();
    }

    public CodegenExpression evaluateCodegenUninstrumented(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return ExprDotNodeForgeRootChildEval.codegen(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public CodegenExpression evaluateGetROCollectionEventsCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return ExprDotNodeForgeRootChildEval.codegenEvaluateGetROCollectionEvents(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public CodegenExpression evaluateGetROCollectionScalarCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return ExprDotNodeForgeRootChildEval.codegenEvaluateGetROCollectionScalar(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public CodegenExpression evaluateGetEventBeanCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public Class getEvaluationType() {
        return EPTypeHelper.getNormalizedClass(forgesUnpacking[forgesUnpacking.length - 1].getTypeInfo());
    }

    public ExprDotNodeImpl getParent() {
        return parent;
    }

    public boolean isReturnsConstantResult() {
        return false;
    }

    public FilterExprAnalyzerAffector getFilterExprAnalyzerAffector() {
        return filterExprAnalyzerAffector;
    }

    public Integer getStreamNumReferenced() {
        return streamNumReferenced;
    }

    public String getRootPropertyName() {
        return rootPropertyName;
    }

    public EventType getEventTypeCollection(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        return innerForge.getEventTypeCollection();
    }

    public Class getComponentTypeCollection() throws ExprValidationException {
        return innerForge.getComponentTypeCollection();
    }

    public EventType getEventTypeSingle(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        return null;
    }

    public ExprEnumerationEval getExprEvaluatorEnumeration() {
        return getExprEvaluator();
    }

    public ExprNodeRenderable getForgeRenderable() {
        return parent;
    }

    public boolean isLocalInlinedClass() {
        return false;
    }
}

