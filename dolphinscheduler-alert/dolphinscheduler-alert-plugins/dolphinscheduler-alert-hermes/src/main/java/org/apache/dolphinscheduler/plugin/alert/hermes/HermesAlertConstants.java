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

public final class HermesAlertConstants {

    public static final String URL = "$t('url')";
    public static final String NAME_URL = "url";

    public static final String TOKEN = "$t('token')";
    public static final String NAME_TOKEN = "token";

    public static final String ENV = "$t('env')";
    public static final String NAME_ENV = "env";

    public static final String EVENT_CODE = "$t('eventCode')";
    public static final String NAME_EVENT_CODE = "eventCode";

    public static final String IGNORE_PROJECT_CODE = "$t('ignoreProjectCode')";
    public static final String NAME_IGNORE_PROJECT_CODE = "ignoreProjectCode";

    public static final String FILTER_TASK_TYPE_CODE = "$t('filterTaskTypeCode')";
    public static final String NAME_FILTER_TASK_TYPE_CODE = "filterTaskTypeCode";

    public static final String TIMEOUT = "$t('timeout')";
    public static final String NAME_TIMEOUT = "timeout";

    public static final int DEFAULT_TIMEOUT = 120;

    private HermesAlertConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}