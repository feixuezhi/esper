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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoCast;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.rettype.EPType;
import com.espertech.esper.common.internal.rettype.EPTypeHelper;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprDotForgeGetCollectionEval implements ExprDotEval {
    private final ExprDotForgeGetCollection forge;
    private final ExprEvaluator indexExpression;

    public ExprDotForgeGetCollectionEval(ExprDotForgeGetCollection forge, ExprEvaluator indexExpression) {
        this.forge = forge;
        this.indexExpression = indexExpression;
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (target == null) {
            return null;
        }
        Object index = indexExpression.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (!(index instanceof Integer)) {
            return null;
        }
        int indexNum = (Integer) index;
        return collectionElementAt(target, indexNum);

    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param target collection
     * @return frequence params
     */
    public static Object collectionElementAt(Object target, int indexNum) {
        Collection<Object> collection = (Collection<Object>) target;
        if (collection.size() <= indexNum) {
            return null;
        }

        if (collection instanceof List) {
            return ((List<Object>) collection).get(indexNum);
        }
        int count = 0;
        Iterator<Object> it = collection.iterator();
        while (count < indexNum && it.hasNext()) {
            it.next();
            count++;
        }
        if (it.hasNext()) {
            return it.next();
        }
        return null;
    }

    public EPType getTypeInfo() {
        return forge.getTypeInfo();
    }

    public ExprDotForge getDotForge() {
        return forge;
    }

    public static CodegenExpression codegen(ExprDotForgeGetCollection forge, CodegenExpression inner, Class innerType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(EPTypeHelper.getNormalizedClass(forge.getTypeInfo()), ExprDotForgeGetCollectionEval.class, codegenClassScope).addParam(innerType, "target");

        CodegenBlock block = methodNode.getBlock();
        if (!innerType.isPrimitive()) {
            block.ifRefNullReturnNull("target");
        }
        Class targetType = EPTypeHelper.getCodegenReturnType(forge.getTypeInfo());
        block.declareVar(int.class, "index", forge.getIndexExpression().evaluateCodegen(int.class, methodNode, exprSymbol, codegenClassScope))
            .methodReturn(CodegenLegoCast.castSafeFromObjectType(targetType, staticMethod(ExprDotForgeGetCollectionEval.class, "collectionElementAt", ref("target"), ref("index"))));
        return localMethod(methodNode, inner);
    }
}
