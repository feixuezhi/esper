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
package com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.distinctof;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.enummethod.dot.EnumMethodEnum;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotEvalParamLambda;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.base.*;
import com.espertech.esper.common.internal.rettype.ClassMultiValuedEPType;
import com.espertech.esper.common.internal.rettype.EPType;
import com.espertech.esper.common.internal.rettype.EPTypeHelper;

import java.util.function.Function;

public class ExprDotForgeDistinctOf extends ExprDotForgeLambdaThreeForm {

    protected EPType initAndNoParamsReturnType(EventType inputEventType, Class collectionComponentType) {
        return EPTypeHelper.collectionOfSingleValue(collectionComponentType);
    }

    protected ThreeFormNoParamFactory.ForgeFunction noParamsForge(EnumMethodEnum enumMethod, EPType type, StatementCompileTimeServices services) {
        return streamCountIncoming -> new EnumDistinctOfScalarNoParams(streamCountIncoming, ((ClassMultiValuedEPType) type).getComponent());
    }

    protected Function<ExprDotEvalParamLambda, EPType> initAndSingleParamReturnType(EventType inputEventType, Class collectionComponentType) {
        return lambda -> {
            if (inputEventType == null) {
                return EPTypeHelper.collectionOfSingleValue(collectionComponentType);
            }
            return EPTypeHelper.collectionOfEvents(inputEventType);
        };
    }

    protected ThreeFormEventPlainFactory.ForgeFunction singleParamEventPlain(EnumMethodEnum enumMethod) {
        return (lambda, typeInfo, services) -> new EnumDistinctOfEvent(lambda);
    }

    protected ThreeFormEventPlusFactory.ForgeFunction singleParamEventPlus(EnumMethodEnum enumMethod) {
        return (lambda, fieldType, numParameters, typeInfo, services) -> new EnumDistinctOfEventPlus(lambda, fieldType, numParameters);
    }

    protected ThreeFormScalarFactory.ForgeFunction singleParamScalar(EnumMethodEnum enumMethod) {
        return (lambda, eventType, numParams, typeInfo, services) -> new EnumDistinctOfScalar(lambda, eventType, numParams);
    }
}
