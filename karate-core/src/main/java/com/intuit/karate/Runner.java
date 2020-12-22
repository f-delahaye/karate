/*
 * The MIT License
 *
 * Copyright 2020 Intuit Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.intuit.karate;

import com.intuit.karate.core.Feature;
import com.intuit.karate.core.FeatureResult;
import com.intuit.karate.core.FeatureRuntime;
import com.intuit.karate.core.RuntimeHookFactory;
import com.intuit.karate.http.HttpClientFactory;
import com.intuit.karate.job.JobConfig;
import com.intuit.karate.resource.ResourceUtils;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pthomas3
 */
public class Runner {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Runner.class);

    public static Map<String, Object> runFeature(Feature feature, Map<String, Object> vars, boolean evalKarateConfig) {
        Suite suite = new Suite();
        FeatureRuntime featureRuntime = FeatureRuntime.of(suite, feature, vars);
        featureRuntime.caller.setKarateConfigDisabled(!evalKarateConfig);
        featureRuntime.run();
        FeatureResult result = featureRuntime.result;
        if (result.isFailed()) {
            throw result.getErrorsCombined();
        }
        return result.getVariables();
    }

    public static Map<String, Object> runFeature(File file, Map<String, Object> vars, boolean evalKarateConfig) {
        Feature feature = Feature.read(file);
        return runFeature(feature, vars, evalKarateConfig);
    }

    public static Map<String, Object> runFeature(Class relativeTo, String path, Map<String, Object> vars, boolean evalKarateConfig) {
        File file = ResourceUtils.getFileRelativeTo(relativeTo, path);
        return runFeature(file, vars, evalKarateConfig);
    }

    public static Map<String, Object> runFeature(String path, Map<String, Object> vars, boolean evalKarateConfig) {
        Feature feature = Feature.read(path);
        return runFeature(feature, vars, evalKarateConfig);
    }

    // this is called by karate-gatling !
    public static void callAsync(String path, List<String> tags, Map<String, Object> arg, PerfHook perfHook) {
        Builder builder = new Builder();
        builder.tags = tags;
        Suite suite = new Suite(builder); // sets tag selector
        Feature feature = FileUtils.parseFeatureAndCallTag(path);
        FeatureRuntime featureRuntime = FeatureRuntime.of(suite, feature, arg, perfHook);
        featureRuntime.setNext(() -> perfHook.afterFeature(featureRuntime.result));
        perfHook.submit(featureRuntime);
    }

    //==========================================================================
    //
    /**
     * @see com.intuit.karate.Runner#builder()
     * @deprecated
     */
    @Deprecated
    public static Results parallel(Class<?> clazz, int threadCount) {
        return parallel(clazz, threadCount, null);
    }

    /**
     * @see com.intuit.karate.Runner#builder()
     * @deprecated
     */
    @Deprecated
    public static Results parallel(Class<?> clazz, int threadCount, String reportDir) {
        return builder().fromKarateAnnotation(clazz).reportDir(reportDir).parallel(threadCount);
    }

    /**
     * @see com.intuit.karate.Runner#builder()
     * @deprecated
     */
    @Deprecated
    public static Results parallel(List<String> tags, List<String> paths, int threadCount, String reportDir) {
        return parallel(tags, paths, null, null, threadCount, reportDir);
    }

    /**
     * @see com.intuit.karate.Runner#builder()
     * @deprecated
     */
    @Deprecated
    public static Results parallel(int threadCount, String... tagsOrPaths) {
        return parallel(null, threadCount, tagsOrPaths);
    }

    /**
     * @see com.intuit.karate.Runner#builder()
     * @deprecated
     */
    @Deprecated
    public static Results parallel(String reportDir, int threadCount, String... tagsOrPaths) {
        List<String> tags = new ArrayList();
        List<String> paths = new ArrayList();
        for (String s : tagsOrPaths) {
            s = StringUtils.trimToEmpty(s);
            if (s.startsWith("~") || s.startsWith("@")) {
                tags.add(s);
            } else {
                paths.add(s);
            }
        }
        return parallel(tags, paths, threadCount, reportDir);
    }

    /**
     * @see com.intuit.karate.Runner#builder()
     * @deprecated
     */
    @Deprecated
    public static Results parallel(List<String> tags, List<String> paths, String scenarioName,
            List<RuntimeHook> hooks, int threadCount, String reportDir) {
        Builder options = new Builder();
        options.tags = tags;
        options.paths = paths;
        options.scenarioName = scenarioName;
        if (hooks != null) {
            options.hooks.addAll(hooks);
        }
        options.reportDir = reportDir;
        return options.parallel(threadCount);
    }

    //==========================================================================
    //
    public static class Builder {

        ClassLoader classLoader;
        Class optionsClass;
        String env;
        File workingDir;
        String buildDir;
        String configDir;
        int threadCount;
        int timeoutMinutes;
        String reportDir;
        String scenarioName;
        List<String> tags;
        List<String> paths;
        List<Feature> features;
        String relativeTo;
        final Collection<RuntimeHook> hooks = new ArrayList();
        RuntimeHookFactory hookFactory;
        HttpClientFactory clientFactory;
        boolean forTempUse;
        boolean outputHtmlReport = true;
        boolean outputJunitXml;
        boolean outputCucumberJson;
        boolean outputKarateJson;
        boolean dryRun;
        Map<String, String> systemProperties;
        JobConfig jobConfig;

        public List<Feature> resolveAll() {
            if (classLoader == null) {
                classLoader = Thread.currentThread().getContextClassLoader();
            }
            if (clientFactory == null) {
                clientFactory = HttpClientFactory.DEFAULT;
            }
            if (systemProperties == null) {
                systemProperties = new HashMap(System.getProperties());
            } else {
                systemProperties.putAll(new HashMap(System.getProperties()));
            }
            // env
            String tempOptions = StringUtils.trimToNull(systemProperties.get(Constants.KARATE_OPTIONS));
            if (tempOptions != null) {
                LOGGER.info("using system property '{}': {}", Constants.KARATE_OPTIONS, tempOptions);
                Main ko = Main.parseKarateOptions(tempOptions);
                if (ko.tags != null) {
                    tags = ko.tags;
                }
                if (ko.paths != null) {
                    paths = ko.paths;
                }
                dryRun = ko.dryRun || dryRun;
            }
            String tempEnv = StringUtils.trimToNull(systemProperties.get(Constants.KARATE_ENV));
            if (tempEnv != null) {
                LOGGER.info("using system property '{}': {}", Constants.KARATE_ENV, tempEnv);
                env = tempEnv;
            } else if (env != null) {
                LOGGER.info("karate.env is: '{}'", env);
            }
            // config dir
            String tempConfig = StringUtils.trimToNull(systemProperties.get(Constants.KARATE_CONFIG_DIR));
            if (tempConfig != null) {
                LOGGER.info("using system property '{}': {}", Constants.KARATE_CONFIG_DIR, tempConfig);
                configDir = tempConfig;
            }
            if (workingDir == null) {
                workingDir = FileUtils.WORKING_DIR;
            }
            if (configDir == null) {
                try {
                    ResourceUtils.getResource(workingDir, "classpath:karate-config.js");
                    configDir = "classpath:"; // default mode
                } catch (Exception e) {
                    configDir = workingDir.getPath();
                }
            }
            if (configDir.startsWith("file:") || configDir.startsWith("classpath:")) {
                // all good
            } else {
                configDir = "file:" + configDir;
            }
            if (configDir.endsWith(":") || configDir.endsWith("/") || configDir.endsWith("\\")) {
                // all good
            } else {
                configDir = configDir + File.separator;
            }
            if (buildDir == null) {
                buildDir = FileUtils.getBuildDir();
            }
            if (reportDir == null) {
                reportDir = buildDir + File.separator + Constants.KARATE_REPORTS;
            }
            // hooks
            if (hookFactory != null) {
                hook(hookFactory.create());
            }
            // features
            if (features == null) {
                if (paths != null && !paths.isEmpty()) {
                    if (relativeTo != null) {
                        paths = paths.stream().map(p -> {
                            if (p.startsWith("classpath:")) {
                                return p;
                            }
                            if (!p.endsWith(".feature")) {
                                p = p + ".feature";
                            }
                            return relativeTo + "/" + p;
                        }).collect(Collectors.toList());
                    }
                } else if (relativeTo != null) {
                    paths = new ArrayList();
                    paths.add(relativeTo);
                }
                features = ResourceUtils.findFeatureFiles(workingDir, paths);
            }
            if (scenarioName != null) {
                for (Feature feature : features) {
                    feature.setCallName(scenarioName);
                }
            }
            if (jobConfig != null) {
                reportDir = jobConfig.getExecutorDir();
                threadCount = jobConfig.getExecutorCount();
                timeoutMinutes = jobConfig.getTimeoutMinutes();                
            }
            if (threadCount < 1) {
                threadCount = 1;
            }
            return features;
        }

        protected Builder forTempUse() {
            forTempUse = true;
            return this;
        }

        //======================================================================
        //
        public Builder configDir(String dir) {
            this.configDir = dir;
            return this;
        }

        public Builder karateEnv(String env) {
            this.env = env;
            return this;
        }

        public Builder systemProperty(String key, String value) {
            if (systemProperties == null) {
                systemProperties = new HashMap();
            }
            systemProperties.put(key, value);
            return this;
        }

        public Builder workingDir(File value) {
            if (value != null) {
                this.workingDir = value;
            }
            return this;
        }

        public Builder buildDir(String value) {
            if (value != null) {
                this.buildDir = value;
            }
            return this;
        }

        public Builder classLoader(ClassLoader value) {
            classLoader = value;
            return this;
        }

        public Builder relativeTo(Class clazz) {
            relativeTo = "classpath:" + ResourceUtils.toPathFromClassPathRoot(clazz);
            return this;
        }

        /**
         * @see com.intuit.karate.Runner#builder()
         * @deprecated
         */
        @Deprecated
        public Builder fromKarateAnnotation(Class<?> clazz) {
            KarateOptions ko = clazz.getAnnotation(KarateOptions.class);
            if (ko != null) {
                LOGGER.warn("the @KarateOptions annotation is deprecated, please use Runner.builder()");
                if (ko.tags().length > 0) {
                    tags = Arrays.asList(ko.tags());
                }
                if (ko.features().length > 0) {
                    paths = Arrays.asList(ko.features());
                }
            }
            return relativeTo(clazz);
        }

        public Builder path(String... value) {
            path(Arrays.asList(value));
            return this;
        }

        public Builder path(List<String> value) {
            if (value != null) {
                if (paths == null) {
                    paths = new ArrayList();
                }
                paths.addAll(value);
            }
            return this;
        }

        public Builder tags(List<String> value) {
            if (value != null) {
                if (tags == null) {
                    tags = new ArrayList();
                }
                tags.addAll(value);
            }
            return this;
        }

        public Builder tags(String... tags) {
            tags(Arrays.asList(tags));
            return this;
        }

        public Builder features(Collection<Feature> value) {
            if (value != null) {
                if (features == null) {
                    features = new ArrayList();
                }
                features.addAll(value);
            }
            return this;
        }

        public Builder features(Feature... value) {
            return features(Arrays.asList(value));
        }

        public Builder reportDir(String value) {
            if (value != null) {
                this.reportDir = value;
            }
            return this;
        }

        public Builder scenarioName(String name) {
            this.scenarioName = name;
            return this;
        }

        public Builder timeoutMinutes(int timeoutMinutes) {
            this.timeoutMinutes = timeoutMinutes;
            return this;
        }

        public Builder hook(RuntimeHook hook) {
            if (hook != null) {
                hooks.add(hook);
            }
            return this;
        }

        public Builder hooks(Collection<RuntimeHook> hooks) {
            if (hooks != null) {
                this.hooks.addAll(hooks);
            }
            return this;
        }


        public Builder hookFactory(RuntimeHookFactory hookFactory) {
            this.hookFactory = hookFactory;
            return this;
        }

        public Builder clientFactory(HttpClientFactory clientFactory) {
            this.clientFactory = clientFactory;
            return this;
        }

        public Builder threads(int value) {
            threadCount = value;
            return this;
        }

        public Builder outputHtmlReport(boolean value) {
            outputHtmlReport = value;
            return this;
        }

        public Builder outputCucumberJson(boolean value) {
            outputCucumberJson = value;
            return this;
        }
        
        public Builder outputKarateJson(boolean value) {
            outputKarateJson = value;
            return this;
        }        

        public Builder outputJunitXml(boolean value) {
            outputJunitXml = value;
            return this;
        }

        public Builder dryRun(boolean value) {
            dryRun = value;
            return this;
        }
        
        public Results jobManager(JobConfig value) {
            jobConfig = value;
            Suite suite = new Suite(this);
            suite.run();
            return suite.results;
        }

        public Results parallel(int threadCount) {
            threads(threadCount);
            Suite suite = new Suite(this);
            suite.run();
            return suite.results;
        }

        @Override
        public String toString() {
            return paths + "";
        }

    }

    public static Builder path(String... paths) {
        Builder builder = new Builder();
        return builder.path(paths);
    }

    public static Builder path(List<String> paths) {
        Builder builder = new Builder();
        return builder.path(paths);
    }

    public static Builder builder() {
        return new Runner.Builder();
    }

}
