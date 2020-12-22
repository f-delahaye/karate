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
package com.intuit.karate.job;

import com.intuit.karate.FileUtils;
import com.intuit.karate.Json;
import com.intuit.karate.StringUtils;
import com.intuit.karate.core.FeatureResult;
import com.intuit.karate.core.Scenario;
import com.intuit.karate.core.ScenarioResult;
import com.intuit.karate.core.ScenarioRuntime;
import com.intuit.karate.http.ResourceType;
import static com.intuit.karate.job.JobConfigBase.logger;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 * @author pthomas3
 */
public class MavenJobConfig extends JobConfigBase<ScenarioRuntime> {

    public MavenJobConfig(int executorCount, String host, int port) {
        super(executorCount, host, port);
    }

    @Override
    public List<JobCommand> getMainCommands(JobChunk<ScenarioRuntime> chunk) {
        Scenario scenario = chunk.getValue().scenario;
        String path = scenario.getFeature().getResource().getPrefixedPath();
        int line = scenario.getLine();
        String temp = "mvn exec:java -Dexec.mainClass=com.intuit.karate.Main -Dexec.classpathScope=test"
                + " \"-Dexec.args=-f karate " + path + ":" + line + "\"";
        for (String k : sysPropKeys) {
            String v = StringUtils.trimToEmpty(System.getProperty(k));
            if (!v.isEmpty()) {
                temp = temp + " -D" + k + "=" + v;
            }
        }
        return Collections.singletonList(new JobCommand(temp));
    }

    @Override
    public ScenarioRuntime handleUpload(JobChunk<ScenarioRuntime> chunk, File upload) {
        ScenarioRuntime runtime = chunk.getValue();
        File jsonFile = JobUtils.getFirstFileWithExtension(upload, "json");
        if (jsonFile == null) {
            logger.warn("no karate json found in job executor result");
            return runtime;
        }        
        String json = FileUtils.toString(jsonFile);
        Map<String, Object> map = Json.of(json).asMap();
        FeatureResult fr = FeatureResult.fromKarateJson(map);
        if (fr.getScenarioResults().isEmpty()) {
            logger.warn("executor feature result is empty");
            return runtime;
        }
        ScenarioResult sr = fr.getScenarioResults().get(0);
        sr.setExecutorName(chunk.getExecutorId());
        sr.setStartTime(chunk.getStartTime());
        sr.setEndTime(System.currentTimeMillis());        
        synchronized (runtime.featureRuntime) {
            runtime.featureRuntime.result.addResult(sr);
        }
        File videoFile = JobUtils.getFirstFileWithExtension(upload, "mp4");
        if (videoFile != null) {
            runtime.embed(FileUtils.toBytes(videoFile), ResourceType.MP4);
        }
        return runtime;
    }

}
