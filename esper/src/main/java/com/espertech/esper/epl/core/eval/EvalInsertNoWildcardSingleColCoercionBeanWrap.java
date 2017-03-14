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
package com.espertech.esper.epl.core.eval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.core.SelectExprProcessor;
import com.espertech.esper.event.WrapperEventType;

import java.util.Collections;

public class EvalInsertNoWildcardSingleColCoercionBeanWrap extends EvalBaseFirstPropFromWrap implements SelectExprProcessor {

    public EvalInsertNoWildcardSingleColCoercionBeanWrap(SelectExprContext selectExprContext, WrapperEventType wrapper) {
        super(selectExprContext, wrapper);
    }

    public EventBean processFirstCol(Object result) {
        EventBean wrappedEvent = super.getEventAdapterService().adapterForTypedBean(result, wrapper.getUnderlyingEventType());
        return super.getEventAdapterService().adapterForTypedWrapper(wrappedEvent, Collections.EMPTY_MAP, wrapper);
    }
}
