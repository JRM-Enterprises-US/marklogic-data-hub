/*
 * Copyright 2012-2019 MarkLogic Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.marklogic.hub.web.service;

import com.marklogic.hub.FlowManager;
import com.marklogic.hub.flow.Flow;
import com.marklogic.hub.flow.FlowRunner;
import com.marklogic.hub.flow.RunFlowResponse;
import com.marklogic.hub.impl.HubConfigImpl;
import com.marklogic.hub.step.Step;
import com.marklogic.hub.util.json.JSONObject;
import com.marklogic.hub.web.exception.DataHubException;
import com.marklogic.hub.web.model.StepModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class FlowManagerService {

    @Autowired
    private FlowManager flowManager;

    @Autowired
    private FlowRunner flowRunner;

    @Autowired
    private HubConfigImpl hubConfig;

    @Autowired
    private StepManagerService stepManagerService;

    public List<Flow> getFlows() {
        return flowManager.getFlows();
    }

    public Flow createFlow(String flowJson, boolean checkExists) {
        Flow flow = flowManager.createFlowFromJSON(flowJson);
        if (flow != null && StringUtils.isEmpty(flow.getName())) {
            return null;
        }
        if (checkExists && flowManager.isFlowExisted(flow.getName())) {
            throw new DataHubException(flow.getName() + " is existed.");
        }
        flowManager.saveFlow(flow);
        return flow;
    }

    public Flow getFlow(String flowName) {
        return flowManager.getFlow(flowName);
    }

    public List<String> getFlowNames() {
        return flowManager.getFlowNames();
    }

    public void deleteFlow(String flowName) {
        flowManager.deleteFlow(flowName);
    }

    public List<StepModel> getSteps(String flowName) {
        Map<String, Step> stepMap = flowManager.getSteps(flowName);

        List<StepModel> stepModelList = new ArrayList<>();
        for (String key : stepMap.keySet()) {
            Step step = stepMap.get(key);
            StepModel stepModel = new StepModel();

            stepModel.setId(step.getName() + "-" + step.getType());

            stepModel.setType(step.getType());
            stepModel.setName(step.getName());
//            stepModel.setDescription(step.get);
            stepModel.setSourceDatabase(step.getSourceDB());
            stepModel.setTargetDatabase(step.getDestDB());
            stepModel.setConfig(step.getConfig());
            stepModel.setLanguage("zxx");
//            stepModel.setValid(step.get);
//            stepModel.setRunning();
            stepModel.setVersion(String.valueOf(step.getVersion()));
//            stepModel.setSourceCollection(step.get);
//            stepModel.setSourceQuery(step.get);
//            stepModel.setTargetEntity(step.get);

            stepModelList.add(stepModel);
        }

        return stepModelList;
    }


    public StepModel createStep(String flowName, String stepJson) {
        StepModel stepModel = null;
        try {
            stepModel = StepModel.fromJson(JSONObject.readInput(stepJson));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Step step = Step.create("dummy", Step.StepType.CUSTOM);

        // TODO: Transform stepModel to step, Save the step on disk, Add the stepJson to Flow
        // NOTE: Only save step if step is of Custom type, for rest use the default steps.

        if (stepManagerService.getStep(step.getName(), step.getType()) != null) {
            stepManagerService.saveStep(step);
        } else {
            stepManagerService.createStep(step);
        }
        Map<String, Step> stepMap = new HashMap<>();
        stepMap.put("<STEP_ORDER>", step);
        Flow flow = flowManager.setSteps(flowName, stepMap);
        flowManager.saveFlow(flow);

        return stepModel;
    }

    public void deleteStep(String flowName, String stepId) {
        // TODO: stepId: stepNum or stepName-stepType
        Step step = flowManager.getStep(flowName, stepId);
        stepManagerService.deleteStep(step);
    }

    public String runFlow(String flowName, String[] steps) {
        RunFlowResponse resp = null;
        if (steps == null) {
            resp = flowRunner.runFlow(flowName);
        } else {
            resp = flowRunner.runFlow(flowName, Arrays.asList(steps));
        }
        return resp.getJobId();
    }
}