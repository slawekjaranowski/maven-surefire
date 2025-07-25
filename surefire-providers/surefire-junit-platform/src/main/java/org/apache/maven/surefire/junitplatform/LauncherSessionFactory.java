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
package org.apache.maven.surefire.junitplatform;

import org.apache.maven.surefire.api.util.ReflectionUtils;
import org.apache.maven.surefire.api.util.SurefireReflectionException;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.core.LauncherFactory;

interface LauncherSessionFactory {

    LauncherSessionFactory DEFAULT = () -> {
        try {
            Class<?> sessionClass = Class.forName("org.junit.platform.launcher.LauncherSession");
            AutoCloseable launcherSession = ReflectionUtils.invokeGetter(LauncherFactory.class, null, "openSession");
            return new SupportedLauncherSessionAdapter(sessionClass, launcherSession);
        } catch (ClassNotFoundException e) {
            return new LegacyLauncherSessionAdapter(LauncherFactory.create());
        }
    };

    LauncherSessionAdapter openSession();

    class SupportedLauncherSessionAdapter implements LauncherSessionAdapter {

        private final Class<?> sessionClass;
        private final AutoCloseable launcherSession;

        SupportedLauncherSessionAdapter(Class<?> sessionClass, AutoCloseable launcherSession) {
            this.sessionClass = sessionClass;
            this.launcherSession = launcherSession;
        }

        @Override
        public Launcher getLauncher() {
            return ReflectionUtils.invokeGetter(sessionClass, launcherSession, "getLauncher");
        }

        @Override
        public void close() {
            try {
                launcherSession.close();
            } catch (Exception e) {
                throw new SurefireReflectionException(e);
            }
        }
    }

    class LegacyLauncherSessionAdapter implements LauncherSessionAdapter {

        private final Launcher launcher;

        LegacyLauncherSessionAdapter(Launcher launcher) {
            this.launcher = launcher;
        }

        @Override
        public Launcher getLauncher() {
            return launcher;
        }

        @Override
        public void close() {
            // do nothing
        }
    }
}
