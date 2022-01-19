/*
 * Copyright 2022 Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.udaychandra.jt;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.tribuo.MutableDataset;
import org.tribuo.Prediction;
import org.tribuo.anomaly.AnomalyFactory;
import org.tribuo.anomaly.Event;
import org.tribuo.anomaly.libsvm.LibSVMAnomalyModel;
import org.tribuo.anomaly.libsvm.LibSVMAnomalyTrainer;
import org.tribuo.anomaly.libsvm.SVMAnomalyType;
import org.tribuo.common.libsvm.KernelType;
import org.tribuo.common.libsvm.SVMParameters;
import org.tribuo.data.columnar.ColumnarIterator;
import org.tribuo.data.columnar.FieldExtractor;
import org.tribuo.data.columnar.FieldProcessor;
import org.tribuo.data.columnar.RowProcessor;
import org.tribuo.data.columnar.extractors.IdentityExtractor;
import org.tribuo.data.columnar.processors.field.DoubleFieldProcessor;
import org.tribuo.data.columnar.processors.response.FieldResponseProcessor;
import org.tribuo.data.csv.CSVDataSource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * An experimental JUnit extension that times test class execution and applies Tribuo's LibSVM anomaly detection.
 * This is just for fun. In reality, JUnit provides enough information and extension points to do such calculations
 * outside the framework--like a Jenkins plugin, a standalone analyzer etc.
 */
public class JTExtension
    implements BeforeAllCallback, AfterAllCallback {

  // =================== BEGIN CONFIG ===================
  // TODO: Allow all configuration knobs to be passed dynamically via annotations or by some other means.

  // The folder path where testing data is to be collected.
  private static final File TESTING_DS_FOLDER = new File("jt-ds");

  // The number of examples to be collected before a model can be trained.
  private static final int MIN_EXAMPLES = 1950;

  private static final double MODEL_GAMMA = 1.0;

  // One class SVM parameter: 0.1 indicates that at most 10% of the training examples are allowed to be wrongly
  // classified. And at least 10% of the training examples will act as support vectors.
  private static final double MODEL_NU = 0.1;

  // =================== END CONFIG ===================

  private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(JTExtension.class);

  private static final Path sTrainingDSPath;
  private static final RowProcessor<Event> sRowProcessor;

  static {
    if (!TESTING_DS_FOLDER.exists()) {
      TESTING_DS_FOLDER.mkdirs();
    }

    var trainingDSFile = new File(TESTING_DS_FOLDER, "testing-ds.csv");
    sTrainingDSPath = trainingDSFile.toPath();
    if (!trainingDSFile.exists()) {
      try {
        Files.writeString(sTrainingDSPath, "name,duration,status\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
      } catch (IOException ex) {
        throw new RuntimeException("Unable to create CSV file for saving training data", ex);
      }
    }

    var fieldProcessors = new HashMap<String, FieldProcessor>();
    fieldProcessors.put("duration", new DoubleFieldProcessor("duration"));

    var metadataExtractors = new ArrayList<FieldExtractor<?>>();
    metadataExtractors.add(new IdentityExtractor("name"));

    var responseProcessor = new FieldResponseProcessor<>("status","UNKNOWN", new AnomalyFactory());
    sRowProcessor = new RowProcessor<>(metadataExtractors, null, responseProcessor, fieldProcessors, Map.of(), Set.of());
  }

  @Override
  public void beforeAll(ExtensionContext pContext) {
    Class<?> clazz = pContext.getRequiredTestClass();
    pContext.getStore(NAMESPACE).put(clazz.getName(), System.currentTimeMillis());
  }

  @Override
  public void afterAll(ExtensionContext pContext)
      throws Exception {
    Class<?> clazz = pContext.getRequiredTestClass();
    long startTime = pContext.getStore(NAMESPACE).remove(clazz.getName(), long.class);
    long duration = System.currentTimeMillis() - startTime;
    double durationInSec = duration / 1000.0;
    Prediction<Event> prediction = null;

    try {
      var dataSource = new CSVDataSource<>(sTrainingDSPath, sRowProcessor, false);
      var trainingDataset = new MutableDataset<>(dataSource);

      if (trainingDataset.size() >= MIN_EXAMPLES) {
        var params = new SVMParameters<>(new SVMAnomalyType(SVMAnomalyType.SVMMode.ONE_CLASS), KernelType.RBF);
        params.setGamma(MODEL_GAMMA);
        params.setNu(MODEL_NU);
        var trainer = new LibSVMAnomalyTrainer(params);

        // The LibSVM anomaly detection algorithm requires that there be no anomalies in the training data.
        // When building a real extension, we have to handle this requirement.
        var model = trainer.train(trainingDataset);

        var newRow = Map.of(
            "name", clazz.getName(),
            "duration", String.valueOf(durationInSec)
        );
        var headers = java.util.List.copyOf(newRow.keySet());
        var row = new ColumnarIterator.Row(trainingDataset.size(), headers, newRow);

        var example = sRowProcessor.generateExample(row,false).get();

        // That's right, no testing, no cross-validation, just predict (for fun).
        prediction = model.predict(example);

        // This is where you would generate an actual report
        System.out.println(example);
        System.out.println(prediction);
      }

    } finally {
      // Naive logic to auto mark examples as expected.
      if (prediction == null || prediction.getOutput().getType() != Event.EventType.ANOMALOUS) {
        Files.writeString(
            sTrainingDSPath,
            clazz.getName() + "," + durationInSec + ",EXPECTED\n",
            StandardCharsets.UTF_8,
            StandardOpenOption.APPEND
        );
      }
    }
  }
}
