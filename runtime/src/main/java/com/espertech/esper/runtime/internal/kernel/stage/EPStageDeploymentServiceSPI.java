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
package com.espertech.esper.runtime.internal.kernel.stage;

import com.espertech.esper.runtime.client.stage.EPStageDeploymentService;
import com.espertech.esper.runtime.internal.kernel.service.DeploymentInternal;

import java.util.Map;

public interface EPStageDeploymentServiceSPI extends EPStageDeploymentService {
    Map<String, DeploymentInternal> getDeploymentMap();
}
