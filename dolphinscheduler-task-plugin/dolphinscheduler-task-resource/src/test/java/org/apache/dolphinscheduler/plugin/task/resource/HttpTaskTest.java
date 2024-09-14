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

package org.apache.dolphinscheduler.plugin.task.resource;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import okhttp3.mockwebserver.MockWebServer;

/**
 * Test HttpTask
 */
@ExtendWith(MockitoExtension.class)
public class HttpTaskTest {

    private static final String CONTENT_TYPE = "Content-Type";

    private static final String APPLICATION_JSON_VALUE = "application/json";

    private static final String MOCK_DISPATCH_PATH_REQ_BODY_TO_RES_BODY = "/requestBody/to/responseBody";

    private static final String MOCK_DISPATCH_PATH_REQ_PARAMS_TO_RES_BODY = "/requestParams/to/responseBody";

    private final List<MockWebServer> mockWebServers = new ArrayList<>();

    @AfterEach
    public void after() {
        mockWebServers.forEach(IOUtils::closeQuietly);
        mockWebServers.clear();
    }

}
