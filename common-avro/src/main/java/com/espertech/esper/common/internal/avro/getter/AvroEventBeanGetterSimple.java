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
package com.espertech.esper.common.internal.avro.getter;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.avro.core.AvroEventPropertyGetter;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactoryCodegenField;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import org.apache.avro.generic.GenericData;

import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class AvroEventBeanGetterSimple implements AvroEventPropertyGetter {
    private final int propertyIndex;
    private final EventType fragmentType;
    private final EventBeanTypedEventFactory eventAdapterService;
    private final Class propertyType;

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param value               value
     * @param eventAdapterService svc
     * @param fragmentType        type
     * @return fragment
     */
    public static Object getFragmentAvro(Object value, EventBeanTypedEventFactory eventAdapterService, EventType fragmentType) {
        if (fragmentType == null) {
            return null;
        }
        if (value instanceof GenericData.Record) {
            return eventAdapterService.adapterForTypedAvro(value, fragmentType);
        }
        if (value instanceof Collection) {
            Collection coll = (Collection) value;
            EventBean[] events = new EventBean[coll.size()];
            int index = 0;
            for (Object item : coll) {
                events[index++] = eventAdapterService.adapterForTypedAvro(item, fragmentType);
            }
            return events;
        }
        return null;
    }

    public AvroEventBeanGetterSimple(int propertyIndex, EventType fragmentType, EventBeanTypedEventFactory eventAdapterService, Class propertyType) {
        this.propertyIndex = propertyIndex;
        this.fragmentType = fragmentType;
        this.eventAdapterService = eventAdapterService;
        this.propertyType = propertyType;
    }

    public Object getAvroFieldValue(GenericData.Record record) throws PropertyAccessException {
        return record.get(propertyIndex);
    }

    public Object get(EventBean theEvent) {
        return getAvroFieldValue((GenericData.Record) theEvent.getUnderlying());
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public boolean isExistsPropertyAvro(GenericData.Record record) {
        return true;
    }

    public Object getFragment(EventBean obj) {
        Object value = get(obj);
        return getFragmentAvro(value, eventAdapterService, fragmentType);
    }

    public Object getAvroFragment(GenericData.Record record) {
        Object value = getAvroFieldValue(record);
        return getFragmentAvro(value, eventAdapterService, fragmentType);
    }

    private CodegenMethod getAvroFragmentCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenExpressionField factory = codegenClassScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
        CodegenExpressionField type = codegenClassScope.addFieldUnshared(true, EventType.class, EventTypeUtility.resolveTypeCodegen(fragmentType, EPStatementInitServices.REF));
        return codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(GenericData.Record.class, "record").getBlock()
                .declareVar(Object.class, "value", underlyingGetCodegen(ref("record"), codegenMethodScope, codegenClassScope))
                .methodReturn(staticMethod(this.getClass(), "getFragmentAvro", ref("value"), factory, type));
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(GenericData.Record.class, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingExistsCodegen(castUnderlying(GenericData.Record.class, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingFragmentCodegen(castUnderlying(GenericData.Record.class, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return cast(propertyType, exprDotMethod(underlyingExpression, "get", constant(propertyIndex)));
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantTrue();
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        if (fragmentType == null) {
            return constantNull();
        }
        return localMethod(getAvroFragmentCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }
}

