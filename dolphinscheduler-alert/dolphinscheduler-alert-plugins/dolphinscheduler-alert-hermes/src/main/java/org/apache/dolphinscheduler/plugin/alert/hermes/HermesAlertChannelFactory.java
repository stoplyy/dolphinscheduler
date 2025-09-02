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

package org.apache.dolphinscheduler.plugin.alert.hermes;

import java.util.Arrays;
import java.util.List;

import org.apache.dolphinscheduler.alert.api.AlertChannel;
import org.apache.dolphinscheduler.alert.api.AlertChannelFactory;
import org.apache.dolphinscheduler.alert.api.AlertInputTips;
import org.apache.dolphinscheduler.spi.params.base.DataType;
import org.apache.dolphinscheduler.spi.params.base.PluginParams;
import org.apache.dolphinscheduler.spi.params.base.Validate;
import org.apache.dolphinscheduler.spi.params.input.InputParam;

import com.google.auto.service.AutoService;

@AutoService(AlertChannelFactory.class)
public final class HermesAlertChannelFactory implements AlertChannelFactory {

    @Override
    public String name() {
        return "hermes";
    }

    @Override
    public List<PluginParams> params() {

        InputParam url = InputParam.newBuilder(HermesAlertConstants.NAME_URL, HermesAlertConstants.URL)
                .setPlaceholder(AlertInputTips.URL.getMsg())
                .addValidate(Validate.newBuilder()
                        .setRequired(false)
                        .build())
                .build();

        InputParam token = InputParam.newBuilder(HermesAlertConstants.NAME_TOKEN, HermesAlertConstants.TOKEN)
                .setPlaceholder(AlertInputTips.TOKEN.getMsg())
                .addValidate(Validate.newBuilder()
                        .setType(DataType.STRING.getDataType())
                        .setRequired(false)
                        .build())
                .build();

        InputParam eventCode = InputParam
                .newBuilder(HermesAlertConstants.NAME_EVENT_CODE, HermesAlertConstants.EVENT_CODE)
                .setPlaceholder(AlertInputTips.HERMES_EVENT_CODE.getMsg())
                .addValidate(Validate.newBuilder()
                        .setRequired(false)
                        .build())
                .build();

        InputParam env = InputParam
                .newBuilder(HermesAlertConstants.NAME_ENV, HermesAlertConstants.ENV)
                .setPlaceholder(AlertInputTips.ENV.getMsg())
                .addValidate(Validate.newBuilder()
                        .setRequired(false)
                        .build())
                .build();
        InputParam ignoreProjectCode = InputParam
                .newBuilder(HermesAlertConstants.NAME_IGNORE_PROJECT_CODE, HermesAlertConstants.IGNORE_PROJECT_CODE)
                .setPlaceholder(AlertInputTips.IGNORE_PROJECT_CODE.getMsg())
                .addValidate(Validate.newBuilder()
                        .setRequired(false)
                        .build())
                .build();

        InputParam filterTaskTypeCode = InputParam
                .newBuilder(HermesAlertConstants.NAME_FILTER_TASK_TYPE_CODE, HermesAlertConstants.FILTER_TASK_TYPE_CODE)
                .setPlaceholder(AlertInputTips.FILTER_TASK_TYPE_CODE.getMsg())
                .addValidate(Validate.newBuilder()
                        .setRequired(false)
                        .build())
                .build();

        InputParam solarisUrl = InputParam
                .newBuilder(HermesAlertConstants.NAME_SOLARIS_URL, HermesAlertConstants.SOLARIS_URL)
                .setPlaceholder(AlertInputTips.SOLARIS_URL.getMsg())
                .addValidate(Validate.newBuilder()
                        .setRequired(false)
                        .build())
                .build();

        return Arrays.asList(url, token, eventCode, env, ignoreProjectCode, filterTaskTypeCode, solarisUrl);
    }

    @Override
    public AlertChannel create() {
        return new HermesAlertChannel();
    }
}
