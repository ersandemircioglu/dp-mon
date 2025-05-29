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
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.cpu.nativecpu.NDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.learning.config.Sgd;

import ed.dpmon.analyst.model.Snapshot;

public class NeuralNetwork {

    private static int NUM_OF_IN_NODES = 10;

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
                .activation(Activation.IDENTITY)
                .weightInit(WeightInit.ONES)
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

    public void train(List<Snapshot> trainingData) {
        DataSet dataSet = createDataSet(trainingData);
        train(dataSet);
    }

    public void test(Snapshot testData) {
        DataSet dataSet = createDataSet(Arrays.asList(testData));
        int estimatedClass = test(dataSet);
        System.out.println("Actual: " + testData.getAnalysisResult().getQualityClass() + " "
                + testData.getAnalysisResult().getProductSummary().getQuality());
        System.out.println("Estimated: " + estimatedClass);
        System.out.println(testData.getAnalysisResult().getQualityClass() == estimatedClass);
    }

    private DataSet createDataSet(List<Snapshot> trainingData) {
        int numOfSamples = trainingData.size();
        int numOfProductTypes = productInsertionOrder.size();
        double[][] featureArrays = new double[numOfSamples][numOfProductTypes];
        double[][] labelArrays = new double[numOfSamples][numOfOutputs];

        for (int sampleIndex = 0; sampleIndex < numOfSamples; sampleIndex++) {
            Snapshot snapshot = trainingData.get(sampleIndex);
            int arrayIndex = 0;
            for (String productName : productInsertionOrder) {
                Long diff = snapshot.getFeatures().get(productName);
                if (diff != null && diff > 0) {
                    featureArrays[sampleIndex][arrayIndex] = diff.doubleValue();
                } else {
                    featureArrays[sampleIndex][arrayIndex] = 0.0;
                }
                arrayIndex++;
            }
            labelArrays[sampleIndex][snapshot.getAnalysisResult().getQualityClass()] = 1.0;
        }
        return new DataSet(new NDArray(featureArrays), new NDArray(labelArrays));
    }

    private void train(DataSet trainingDataSet) {
        normalizer.fit(trainingDataSet);
        normalizer.transform(trainingDataSet);
        for (int i = 0; i < 1000; i++) {
            model.fit(trainingDataSet);
        }
    }

    private int test(DataSet testData) {
        System.out.println(productInsertionOrder.toString());
        System.out.println(testData.getFeatures().toString());
        normalizer.transform(testData);

        Evaluation eval = new Evaluation(numOfOutputs);
        INDArray output = model.output(testData.getFeatures());
        eval.eval(testData.getLabels(), output);
        // System.out.println(eval.stats());
        System.out.println(testData.getFeatures().toString());
        System.out.println(testData.getLabels().toString());
        System.out.println(output.toString());
        System.out.println("++++++++++++++++++++++");
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
