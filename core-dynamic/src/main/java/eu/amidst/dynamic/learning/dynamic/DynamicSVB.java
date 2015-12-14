/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.dynamic.learning.dynamic;

import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.exponentialfamily.EF_LearningBayesianNetwork;
import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;
import eu.amidst.core.exponentialfamily.ParameterVariables;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;

import java.util.List;

/**
 *
 * TODO By iterating several times over the data we can get better approximations. Trick. Initialize the Q's of the parameters variables with the final posterios in the previous iterations.
 *
 *
 * Created by ana@cs.aau.dk on 04/03/15.
 */
public class DynamicSVB implements DynamicBayesianLearningAlgorithm {

    EF_LearningBayesianNetwork ef_extendedBNTime0;
    EF_LearningBayesianNetwork ef_extendedBNTimeT;

    DynamicPlateauStructure dynamicPlateauStructure = new DynamicPlateauStructure();

    DynamicDAG dag;

    ParameterVariables parametersVariablesTime0;
    ParameterVariables parametersVariablesTimeT;

    DataStream<DynamicDataInstance> dataStream;

    double elbo;
    boolean parallelMode=false;
    int windowsSize=100;
    int seed = 0;

    public DynamicPlateauStructure getDynamicPlateauStructure() {
        return dynamicPlateauStructure;
    }

    public DynamicSVB(){
        dynamicPlateauStructure = new DynamicPlateauStructure();
        dynamicPlateauStructure.setNRepetitions(windowsSize);

    }


    //public PlateuVMP getPlateuVMP() {
    //    return plateuVMP;
    //}

    public int getSeed() {
        return seed;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }

    @Override
    public double getLogMarginalProbability() {
        return elbo;
    }

    public void setWindowsSize(int windowsSize) {
        this.windowsSize = windowsSize;
        this.dynamicPlateauStructure.setNRepetitions(windowsSize);
    }

    @Override
    public void runLearning() {
        this.initLearning();
        if (!parallelMode) {
            //this.elbo = this.dataStream.stream().sequential().mapToDouble(this::updateModel).sumNonStateless();
            this.elbo = this.dataStream.streamOfBatches(this.windowsSize).sequential().mapToDouble(this::updateModel).sum();
        }else {
            //Creeat EF_ExtendedBN which returns ParameterVariable object
            //Paremter variable car
            //BatchOutput finalout = this.dataStream.streamOfBatches(100).map(this::updateModelOnBatchParallel).reduce(BatchOutput::sumNonStateless).get();
        }
    }

    public void setParallelMode(boolean parallelMode) {
        this.parallelMode = parallelMode;
    }

    @Override
    public double updateModel(DataOnMemory<DynamicDataInstance> batch) {
        //System.out.println("\n Batch:");

        List<DynamicDataInstance> data = batch.getList();
        double logprob = 0;
        if (batch.getDataInstance(0).getTimeID()==0){
            logprob+=this.updateModelTime0(batch.getDataInstance(0));
            data.remove(0);
            if (data.size()==0)
                return logprob;

        }

        logprob+=this.updateModelTimeT(data);

        return logprob;
    }

    private double updateModelTime0(DynamicDataInstance dataInstance) {
        this.dynamicPlateauStructure.setEvidenceTime0(dataInstance);
        this.dynamicPlateauStructure.runInferenceTime0();

        for (Variable var: dynamicPlateauStructure.getEFLearningBNTime0().getParametersVariables()){
            EF_UnivariateDistribution uni = dynamicPlateauStructure.getEFParameterPosteriorTime0(var).deepCopy();
            dynamicPlateauStructure.getEFLearningBNTime0().setDistribution(var, uni);
            this.dynamicPlateauStructure.getNodeOfVarTime0(var).setPDist(uni);
        }

        //this.plateuVMP.resetQs();
        return this.dynamicPlateauStructure.getLogProbabilityOfEvidenceTime0();
    }

    private double updateModelTimeT(List<DynamicDataInstance> batch) {
        this.dynamicPlateauStructure.setEvidenceTimeT(batch);
        this.dynamicPlateauStructure.runInferenceTimeT();

        for (Variable var: dynamicPlateauStructure.getEFLearningBNTimeT().getParametersVariables()){
            EF_UnivariateDistribution uni = dynamicPlateauStructure.getEFParameterPosteriorTimeT(var).deepCopy();
            dynamicPlateauStructure.getEFLearningBNTimeT().setDistribution(var,uni);
            this.dynamicPlateauStructure.getNodeOfVarTimeT(var,0).setPDist(uni);
        }

        //this.plateuVMPDBN.resetQs();
        return this.dynamicPlateauStructure.getLogProbabilityOfEvidenceTimeT();
    }


    @Override
    public void setDynamicDAG(DynamicDAG dag) {
        this.dag = dag;
    }

    public void initLearning(){
        this.dynamicPlateauStructure.setSeed(seed);
        this.dynamicPlateauStructure.setDBNModel(this.dag);
        this.dynamicPlateauStructure.resetQs();
        this.ef_extendedBNTime0 = this.dynamicPlateauStructure.getEFLearningBNTime0();
        this.ef_extendedBNTimeT = this.dynamicPlateauStructure.getEFLearningBNTimeT();
    }

    @Override
    public void setDataStream(DataStream<DynamicDataInstance> data) {
        this.dataStream=data;
    }

    @Override
    public DynamicBayesianNetwork getLearntDBN() {
        return new DynamicBayesianNetwork(this.dag, this.ef_extendedBNTime0.toConditionalDistribution(), this.ef_extendedBNTimeT.toConditionalDistribution());
    }




}