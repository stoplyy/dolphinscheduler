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

package org.apache.dolphinscheduler.api.service;

import java.util.List;

import org.apache.dolphinscheduler.api.utils.Result;
import org.apache.dolphinscheduler.dao.entity.ProjectNodeParameter;
import org.apache.dolphinscheduler.dao.entity.User;

public interface ProjectNodeParameterService {

    Result<ProjectNodeParameter> createParameter(User loginUser, long projectCode, Integer nodeCode,
            String parameterName,
            String parameterValue);

    Result<ProjectNodeParameter> updateParameter(User loginUser, long projectCode,
            Integer nodeCode,
            Integer code, String parameterName,
            String parameterValue, String des);

    Result<Boolean> deleteParametersByCode(User loginUser, long projectCode, Integer nodeCode, Integer code);

    Result<Boolean> deleteParametersByNodeCode(User loginUser, long projectCode, Integer nodeCode);

    Result<Boolean> batchDeleteParametersByCodes(User loginUser, long projectCode, Integer nodeCode, String codes);

    Result<List<ProjectNodeParameter>> queryParameterList(User loginUser, long projectCode, Integer nodeCode);

    Result<ProjectNodeParameter> queryParameterByCode(User loginUser, long projectCode, Integer code);
}
