/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dolphinscheduler.plugin.task.remoteshell;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.dolphinscheduler.common.constants.PlatformConstant;
import org.apache.dolphinscheduler.plugin.task.api.enums.ResourceType;
import org.apache.dolphinscheduler.plugin.task.api.model.Property;
import org.apache.dolphinscheduler.plugin.task.api.parameters.AbstractParameters;
import org.apache.dolphinscheduler.plugin.task.api.parameters.resource.ResourceParametersHelper;

import lombok.Data;

@Data
public class RemoteShellParameters extends AbstractParameters {

    private String rawScript;

    private String type;

    /**
     * datasource id
     */
    private int datasource;

    @Override
    public boolean checkParameters() {
        return rawScript != null && !rawScript.isEmpty();
    }

    @Override
    public ResourceParametersHelper getResources() {
        final ResourceParametersHelper resources = super.getResources();
        List<String> inputSource = loadResourceWithTaskParam(resources);
        inputSource.forEach(datasourceId -> resources.put(ResourceType.DATASOURCE, Integer.parseInt(datasourceId)));
        if (!inputSource.contains(datasource + "")) {
            resources.put(ResourceType.DATASOURCE, datasource);
        }
        return resources;
    }

    /**
     * put datasource resource with task param
     * 1. use PlatformConstant.DATASOURCE_PARAM_NAME param to specify the datasource
     * 2. if has multiple datasource,
     * use PlatformConstant.DATASOURCE_PARAM_NAME_SEPARATOR to separate
     * 3. the param value is datasource id
     * 
     * @param resources
     */
    private List<String> loadResourceWithTaskParam(ResourceParametersHelper resources) {
        Set<String> localDatasource = new HashSet<>();
        if (localParams != null && !localParams.isEmpty()) {
            Optional<Property> op = localParams.stream()
                    .filter(property -> property.getProp().equalsIgnoreCase(PlatformConstant.DATASOURCE_PARAM_NAME))
                    .findFirst();
            if (op.isPresent()) {
                String stringValue = op.get().getValue();
                if (stringValue != null && !stringValue.isEmpty()) {
                    if (stringValue.contains(PlatformConstant.DATASOURCE_PARAM_NAME_SEPARATOR)) {
                        String[] values = stringValue.split(PlatformConstant.DATASOURCE_PARAM_NAME_SEPARATOR);
                        for (String value : values) {
                            localDatasource.add(value);
                        }
                    } else {
                        localDatasource.add(stringValue);
                    }
                }
            }
        }
        return new ArrayList<>(localDatasource);
    }

}
