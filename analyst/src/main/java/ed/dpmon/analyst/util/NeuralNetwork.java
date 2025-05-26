package ed.dpmon.analyst.util;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.learning.config.Sgd;

public class NeuralNetwork {

    private static int NUM_OF_OUTPUT = 10;
    private static int NUM_OF_IN_NODES = 10;
    private MultiLayerNetwork model;
    private DataNormalization normalizer;

    public NeuralNetwork(int numInputs) {
        ;
        long seed = Math.round(Math.random() * 100);
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().seed(seed).activation(Activation.IDENTITY)
                .weightInit(WeightInit.ONES).updater(new Sgd(0.1)).l2(1e-4).list()
                .layer(new DenseLayer.Builder().nIn(numInputs).nOut(NUM_OF_IN_NODES).build())
                .layer(new OutputLayer.Builder().nIn(NUM_OF_IN_NODES).nOut(NUM_OF_OUTPUT).build()).build();
        model = new MultiLayerNetwork(conf);
        model.init();
        // record score once every 100 iterations
        model.setListeners(new ScoreIterationListener(100));

        normalizer = new NormalizerStandardize();
    }

    public void train(DataSetIterator trainingData) {
        for (int i = 0; i < 1000; i++) {
            model.fit(trainingData);
        }
    }

}
