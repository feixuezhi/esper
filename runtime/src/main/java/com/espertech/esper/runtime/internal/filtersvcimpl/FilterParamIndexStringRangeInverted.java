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
package com.espertech.esper.runtime.internal.filtersvcimpl;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.common.internal.filterspec.FilterOperator;
import com.espertech.esper.common.internal.filterspec.StringRange;
import com.espertech.esper.common.internal.filtersvc.FilterHandle;
import com.espertech.esper.runtime.internal.metrics.instrumentation.InstrumentationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Index for filter parameter constants for the not range operators (range open/closed/half).
 * The implementation is based on the SortedMap implementation of TreeMap and stores only expression
 * parameter values of type StringRange.
 */
public final class FilterParamIndexStringRangeInverted extends FilterParamIndexStringRangeBase {
    public FilterParamIndexStringRangeInverted(ExprFilterSpecLookupable lookupable, ReadWriteLock readWriteLock, FilterOperator filterOperator) {
        super(lookupable, readWriteLock, filterOperator);
        if (!(filterOperator.isInvertedRangeOperator())) {
            throw new IllegalArgumentException("Invalid filter operator " + filterOperator);
        }
    }

    public final void matchEvent(EventBean theEvent, Collection<FilterHandle> matches, ExprEvaluatorContext ctx) {
        Object objAttributeValue = lookupable.getEval().eval(theEvent, ctx);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qFilterReverseIndex(this, objAttributeValue);
        }

        if (objAttributeValue == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aFilterReverseIndex(false);
            }
            return;
        }

        String attributeValue = (String) objAttributeValue;

        if (this.getFilterOperator() == FilterOperator.NOT_RANGE_CLOSED) {
            // include all endpoints
            for (Map.Entry<StringRange, EventEvaluator> entry : ranges.entrySet()) {
                if (entry.getKey().getMin().compareTo(attributeValue) > 0 || entry.getKey().getMax().compareTo(attributeValue) < 0) {
                    entry.getValue().matchEvent(theEvent, matches, ctx);
                }
            }
        } else if (this.getFilterOperator() == FilterOperator.NOT_RANGE_OPEN) {  // include neither endpoint
            for (Map.Entry<StringRange, EventEvaluator> entry : ranges.entrySet()) {
                if (entry.getKey().getMin().compareTo(attributeValue) >= 0 || entry.getKey().getMax().compareTo(attributeValue) <= 0) {
                    entry.getValue().matchEvent(theEvent, matches, ctx);
                }
            }
        } else if (this.getFilterOperator() == FilterOperator.NOT_RANGE_HALF_CLOSED) {
            // include high endpoint not low endpoint
            for (Map.Entry<StringRange, EventEvaluator> entry : ranges.entrySet()) {
                if (entry.getKey().getMin().compareTo(attributeValue) >= 0 || entry.getKey().getMax().compareTo(attributeValue) < 0) {
                    entry.getValue().matchEvent(theEvent, matches, ctx);
                }
            }
        } else if (this.getFilterOperator() == FilterOperator.NOT_RANGE_HALF_OPEN) {
            // include low endpoint not high endpoint
            for (Map.Entry<StringRange, EventEvaluator> entry : ranges.entrySet()) {
                if (entry.getKey().getMin().compareTo(attributeValue) > 0 || entry.getKey().getMax().compareTo(attributeValue) <= 0) {
                    entry.getValue().matchEvent(theEvent, matches, ctx);
                }
            }
        } else {
            throw new IllegalStateException("Invalid filter operator " + this.getFilterOperator());
        }

        if (rangesNullEndpoints != null) {
            rangesNullEndpoints.matchEvent(theEvent, matches, ctx);
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aFilterReverseIndex(null);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(FilterParamIndexStringRangeInverted.class);
}
