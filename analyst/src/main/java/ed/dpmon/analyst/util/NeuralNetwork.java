package ed.dpmon.analyst.util;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.cpu.nativecpu.NDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.learning.config.Sgd;

import ed.dpmon.analyst.model.AnalysisResult;

public class NeuralNetwork {

    private static int NUM_OF_IN_NODES = 15;

    private int numOfInputs;
    private int numOfOutputs;

    private LinkedHashSet<String> productInsertionOrder;

    private MultiLayerNetwork model;
    private DataNormalization normalizer;

    public NeuralNetwork(LinkedHashSet<String> productInsertionOrder, int numberOfOutputs) {
        this.productInsertionOrder = productInsertionOrder;
        this.numOfInputs = productInsertionOrder.size();
        this.numOfOutputs = numberOfOutputs;

        long seed = Math.round(Math.random() * 100);
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .activation(Activation.SOFTPLUS)
                .weightInit(WeightInit.UNIFORM)
                .updater(new Sgd(0.1)).l2(1e-4).list()
                .layer(new DenseLayer.Builder().nIn(numOfInputs).nOut(NUM_OF_IN_NODES).build())
                .layer(new OutputLayer.Builder().nIn(NUM_OF_IN_NODES).nOut(numOfOutputs).build())
                .build();
        model = new MultiLayerNetwork(conf);
        model.init();
        // record score once every 100 iterations
        model.setListeners(new ScoreIterationListener(100));

        normalizer = new NormalizerStandardize();
    }

    public void train(List<AnalysisResult> trainingData) {
        DataSet dataSet = createDataSet(trainingData);
        train(dataSet);
    }

    public AnalysisResult test(AnalysisResult testData) {
        DataSet dataSet = createDataSet(Arrays.asList(testData));
        testData.setEstimatedQualityClass(test(dataSet));
        return testData;
    }

    private DataSet createDataSet(List<AnalysisResult> trainingData) {
        int numOfSamples = trainingData.size();
        int numOfProductTypes = productInsertionOrder.size();
        double[][] featureArrays = new double[numOfSamples][numOfProductTypes];
        double[][] labelArrays = new double[numOfSamples][numOfOutputs];

        for (int sampleIndex = 0; sampleIndex < numOfSamples; sampleIndex++) {
            AnalysisResult analysisResult = trainingData.get(sampleIndex);
            int arrayIndex = 0;
            for (String productName : productInsertionOrder) {
                Long diff = analysisResult.getSnapshot().get(productName);
                if (diff != null && diff > 0) {
                    featureArrays[sampleIndex][arrayIndex] = diff.doubleValue();
                } else {
                    featureArrays[sampleIndex][arrayIndex] = 0.0;
                }
                arrayIndex++;
            }
            labelArrays[sampleIndex][analysisResult.getQualityClass()] = 1.0;
        }
        return new DataSet(new NDArray(featureArrays), new NDArray(labelArrays));
    }

    private void train(DataSet trainingDataSet) {
        normalizer.fit(trainingDataSet);
        normalizer.transform(trainingDataSet);
        for (int i = 0; i < 10000; i++) {
            model.fit(trainingDataSet);
        }
    }

    private int test(DataSet testData) {
        normalizer.transform(testData);
        INDArray output = model.output(testData.getFeatures());

        double[] outputArray = output.toDoubleVector();
        int estimatedQualityClass = 0;
        double max = 0;
        for (int i = 0; i < outputArray.length; i++) {
            if (outputArray[i] > max) {
                max = outputArray[i];
                estimatedQualityClass = i;
            }
        }
        return estimatedQualityClass;
    }

}
