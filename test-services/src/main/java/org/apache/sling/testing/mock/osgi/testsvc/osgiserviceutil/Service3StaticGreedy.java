/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.testing.mock.osgi.testsvc.osgiserviceutil;

import java.util.List;
import java.util.Map;

public interface Service3StaticGreedy {

    ServiceInterface1 getReference1();

    ServiceInterface1Optional getReference1Optional();

    List<ServiceInterface2> getReferences2();

    List<ServiceSuperInterface3> getReferences3();

    List<Map<String, Object>> getReference3Configs();

    List<ServiceSuperInterface3> getReferences3Filtered();
}
