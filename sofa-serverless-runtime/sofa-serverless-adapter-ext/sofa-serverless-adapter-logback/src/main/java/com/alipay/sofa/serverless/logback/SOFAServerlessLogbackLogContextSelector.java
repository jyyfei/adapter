/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alipay.sofa.serverless.logback;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.selector.ContextSelector;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 支持将配置转换为 logback context.
 *
 * @author : chenlei3641
 */
public class SOFAServerlessLogbackLogContextSelector implements ContextSelector {
    private static final Map<ClassLoader, LoggerContext> CLASS_LOADER_LOGGER_CONTEXT = new HashMap<>();

    private static final String                          BIZ_CLASS_LOADER            = "com.alipay.sofa.ark.container.service.classloader.BizClassLoader";

    private LoggerContext                                defaultLoggerContext;

    public SOFAServerlessLogbackLogContextSelector(LoggerContext loggerContext) {
        this.defaultLoggerContext = loggerContext;
    }

    private static LoggerContext getContext(ClassLoader cls) {
        LoggerContext loggerContext = CLASS_LOADER_LOGGER_CONTEXT.get(cls);
        if (null == loggerContext) {
            synchronized (SOFAServerlessLogbackLogContextSelector.class) {
                loggerContext = CLASS_LOADER_LOGGER_CONTEXT.get(cls);
                if (null == loggerContext) {
                    loggerContext = new LoggerContext();
                    loggerContext.setName(Integer.toHexString(System.identityHashCode(cls)));
                    CLASS_LOADER_LOGGER_CONTEXT.put(cls, loggerContext);
                }
            }
        }
        return loggerContext;
    }

    public static LoggerContext removeContext(ClassLoader cls) {
        if(cls == null){
            return null;
        }
        return CLASS_LOADER_LOGGER_CONTEXT.remove(cls);
    }

    @Override
    public LoggerContext getLoggerContext() {
        ClassLoader classLoader = this.findClassLoader();
        if (classLoader == null) {
            return defaultLoggerContext;
        }
        return getContext(classLoader);
    }

    private ClassLoader findClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader != null && BIZ_CLASS_LOADER.equals(classLoader.getClass().getName())) {
            return classLoader;
        }
        Class<?>[] context = new SecurityManager() {
            @Override
            public Class<?>[] getClassContext() {
                return super.getClassContext();
            }
        }.getClassContext();
        if (context == null || context.length == 0) {
            return null;
        }
        for (Class<?> cls : context) {
            if (cls.getClassLoader() != null
                && BIZ_CLASS_LOADER.equals(cls.getClassLoader().getClass().getName())) {
                return cls.getClassLoader();
            }
        }

        return null;
    }

    @Override
    public LoggerContext getLoggerContext(String name) {
        if (!StringUtils.hasText(name)) {
            return defaultLoggerContext;
        }
        for (ClassLoader classLoader : CLASS_LOADER_LOGGER_CONTEXT.keySet()) {
            LoggerContext loggerContext = CLASS_LOADER_LOGGER_CONTEXT.get(classLoader);
            if (name.equals(loggerContext.getName())) {
                return loggerContext;
            }
        }
        return defaultLoggerContext;
    }

    @Override
    public LoggerContext getDefaultLoggerContext() {
        return defaultLoggerContext;
    }

    @Override
    public LoggerContext detachLoggerContext(String loggerContextName) {
        if (!StringUtils.hasText(loggerContextName)) {
            return null;
        }
        for (ClassLoader classLoader : CLASS_LOADER_LOGGER_CONTEXT.keySet()) {
            LoggerContext loggerContext = CLASS_LOADER_LOGGER_CONTEXT.get(classLoader);
            if (loggerContextName.equals(loggerContext.getName())) {
                return removeContext(classLoader);
            }
        }
        return null;
    }

    @Override
    public List<String> getContextNames() {
        return CLASS_LOADER_LOGGER_CONTEXT.values().stream().map(LoggerContext::getName).collect(Collectors.toList());
    }
}