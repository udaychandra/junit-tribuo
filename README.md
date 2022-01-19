## Introduction
A playground to experiment with [Tribuo](https://tribuo.org) ML library within the scope of a [JUnit 5](https://junit.org) custom extension.  

## Concepts
JUnit provides extension points to hook into its lifecycle and add custom features to it. Tribuo is an open source machine learning Java library that provides tools for classification, regression, clustering etc. 

In this thought experiment, we leverage Tribuo within a custom JUnit extension to see the feasibility of using ML to potentially gain useful QA insights for a given service or product. 

The custom [extension](src/main/java/io/github/udaychandra/jt/JTExtension.java) computes the total time taken to execute a test class. The data is recorded in a CSV file. For simplicity, all the data that gets collected is naively interpreted as an `EXPECTED` observation. Once we collect enough data, Tribuo's anomaly detection will be used to find any unusual observations when the test classes are run. For instance, say a given test class usually takes somewhere between 2-3 seconds to successfully run all the tests in it. Now a relevant product change gets introduced but no changes are made to the test class and the execution environment it runs in. It now takes 10 seconds to successfully run the tests. This could point to a potential performance regression (an over simplification, of course). If the trained model is doing its job right, this unusual behavior should now be marked as a problem.  

Take a look at [HashUtilsTest.java](src/test/java/io/github/udaychandra/jt/HashUtilsTest.java) to see how the `AnomalyDetector` annotation gets used.

When the timing of the test class runs are as expected, we see something like this from the model's prediction:
```bash
Prediction(maxLabel=(EXPECTED,...
```

When the model detects an anomaly, we see something like this:
```bash
Prediction(maxLabel=(ANOMALOUS,...
```

This is just a start. There might be more useful applications of leveraging ML within the scope of analyzing tests for semi-automated quality assurance. Thanks to the excellent open source libraries like JUnit and Tribuo!   

## Development
This is an experimental community project. All contributions are welcome.

To start contributing, do the following:
* Install JDK 11+
* Fork or clone the source code
* Run the build using maven or your IDE of choice

## License
Apache License 2.0
