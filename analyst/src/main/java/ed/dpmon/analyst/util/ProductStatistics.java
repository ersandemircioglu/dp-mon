package ed.dpmon.analyst.util;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.LongStream;

import ed.dpmon.analyst.model.DataStats;
import ed.dpmon.analyst.model.QualityLabel;
import ed.dpmon.analyst.model.Snapshot;

public class ProductStatistics {

    private LinkedHashSet<String> productInsertionOrder;
    private List<Snapshot> trainingData;

    private DataStats productDataStats;
    private LinkedHashMap<String, DataStats> featuresDataStats = new LinkedHashMap<String, DataStats>();

    public ProductStatistics(LinkedHashSet<String> productInsertionOrder, List<Snapshot> trainingData) {
        super();
        this.productInsertionOrder = productInsertionOrder;
        this.trainingData = trainingData;
        calculateDataDistributions();
    }

    public void estimateQuality(NeuralNetwork nn) {
        for (String productName : productInsertionOrder) {
            estimateQualityForFeature(nn, productName);
            System.out.println(featuresDataStats.get(productName));
        }
    }

    private void estimateQualityForFeature(NeuralNetwork nn, String featureName) {
        Snapshot tempSnapshot = new Snapshot();
        for (String name : productInsertionOrder) {
            if (!name.equals(featureName)) {
                DataStats dataStats = featuresDataStats.get(name);
                tempSnapshot.getFeatures().put(name, dataStats.getMean());
            }
        }
        DataStats featureDataStats = featuresDataStats.get(featureName);
        for (int distributionIndex = DataStats.MINUS_2_SD; distributionIndex <= DataStats.PLUS_2_SD; distributionIndex++) {
            tempSnapshot.getFeatures().put(featureName, featureDataStats.getDistribution()[distributionIndex]);
            int estimatedQualityClass = nn.test(tempSnapshot);
            QualityLabel qualityLabel = QualityLabel.getInstance(estimatedQualityClass);
            featureDataStats.getQualityLabels()[distributionIndex] = qualityLabel;
        }
    }

    private void calculateDataDistributions() {
        int dataSize = trainingData.size();
        int featureSize = productInsertionOrder.size();
        long[] productGenerationFrequency = new long[dataSize];
        long[][] features = new long[featureSize][dataSize];

        long previousProductGenerationTime = 0;
        for (int dataIndex = 0; dataIndex < dataSize; dataIndex++) {
            Snapshot snapshot = trainingData.get(dataIndex);
            productGenerationFrequency[dataIndex] = snapshot.getProductGenerationTime()
                    - previousProductGenerationTime;
            previousProductGenerationTime = snapshot.getProductGenerationTime();

            int featureIndex = 0;
            for (String featureName : productInsertionOrder) {
                if (snapshot.getFeatures().containsKey(featureName)) {
                    features[featureIndex][dataIndex] = snapshot.getFeatures().get(featureName);
                }
                featureIndex++;
            }
        }

        long mean = calculateMean(productGenerationFrequency);
        long standartDeviation = calculateStandartDeviation(productGenerationFrequency, mean);
        productDataStats = new DataStats(mean, standartDeviation);

        int featureIndex = 0;
        for (String featureName : productInsertionOrder) {
            mean = calculateMean(features[featureIndex]);
            standartDeviation = calculateStandartDeviation(features[featureIndex], mean);
            featuresDataStats.put(featureName, new DataStats(mean, standartDeviation));
            featureIndex++;
        }

    }

    private long calculateMean(long[] numArray) {
        return (long) LongStream.of(numArray).average().getAsDouble();
    }

    private long calculateStandartDeviation(long[] numArray, long mean) {
        long sumOfSquaredDiffs = LongStream.of(numArray)
                .map(x -> (x - mean) * (x - mean))
                .sum();
        return (long) Math.sqrt(sumOfSquaredDiffs / numArray.length);
    }

    public DataStats getProductDataStats() {
        return productDataStats;
    }

    public LinkedHashMap<String, DataStats> getFeaturesDataStats() {
        return featuresDataStats;
    }

}
