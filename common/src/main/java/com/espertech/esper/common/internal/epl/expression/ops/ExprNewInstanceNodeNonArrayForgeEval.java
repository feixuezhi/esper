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
package com.espertech.esper.common.internal.epl.expression.ops;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.event.bean.manufacturer.InstanceManufacturer;

/**
 * Represents the "new Class(...)" operator in an expression tree.
 */
public class ExprNewInstanceNodeNonArrayForgeEval implements ExprEvaluator {

    private final InstanceManufacturer manufacturer;

    public ExprNewInstanceNodeNonArrayForgeEval(InstanceManufacturer manufacturer) {
        this.manufacturer = manufacturer;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return manufacturer.make(eventsPerStream, isNewData, exprEvaluatorContext);
    }
}
