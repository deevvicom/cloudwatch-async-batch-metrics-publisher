[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.deevvi/cloudwatch-async-batch-metrics-publisher/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.deevvi/cloudwatch-async-batch-metrics-publisher)

# AWS CloudWatch Async&Batch Metrics Publisher #

### Description ###

This library is an abstraction over AWS CloudWatch, but also it comes with 2 relevant improvements:

* **Asynchronous metrics publishing**: Ideally, code orchestration (metrics and logging) must be achieved without impacting its performance. 
When this orchestration is done using a remote service, an implicit latency is added when this service is called. In order to avoid this, we offer 2 solutions: using blocking queue or using a file. The first one has the advantage of being simpler, while the second one ensures data integrity: If AWS CloudWatch is down for a while, metrics are going to accumulate on a file and, when CW becomes up are published.
* **Batch publishing**: AWS CloudWatch changes you per number of calls you make. Any call could push up to 20 metrics or up to 40Kb of data. If 
the former condition is harder to achieve, the first one could bring an important cost reduction if for example we buffer multiple metrics per AWS call.

### How to use it ###
i. Declare the dependency in your _pom.xml_ file.
<dependency>
  <groupId>com.deevvi</groupId>
  <artifactId>cloudwatch-async-batch-metrics-publisher</artifactId>
  <version>1.0.0</version>
</dependency>

ii. Create a _MetricsPublisher_ object.

Version **1.0.0** offers 2 types of _MetricsPublisher_:

* **QueueBasedMetricsPublisher** - this publisher uses an internal queue to batch metrics. It has the advantage of being very fast, but if the call to AWS CloudWatch fails for whatever reason, **all metrics in that batch are lost**.

```java
 MetricsPublisher publisher = new QueueBasedMetricsPublisher(awsCloudWatchClient, "my-service-namespace", 5000);
```
* **FileBasedMetricsPublisher** - this publisher writes metrics in a file that is rotated after each hour and from there the metrics are read, batched and send to AWS CloudWatch. If call to CW fails, metrics are send again until the call is done with success. Use this one if your application cannot afford to lose any metric.

```java
 MetricsPublisher publisher =  new FileBasedMetricsPublisher(awsCloudWatchClient, "/tmp/cw-experiment/", "test", 5000);
```
**Observation:** the last parameter is the maximum time interval in millis while publisher waits for the metrics. If you put this parameter very high (more than 24 hours), it could take a long time - up to 2 days - to see these metrics. So, our advice is to set this parameter high enough to accumulate some metrics, but not too big because it can delay your metrics.

iii. Create a _MetricsFactory_ using the _MetricsPublisher_ defined above.
```java
 MetricsFactory metricsFactory = new AWSCloudWatchMetricsFactory(publisher);
```
iv. Use the _MetricsFactory_ in your code.
```java
public class DemoService {

    private final MetricsFactory metricsFactory;
    private final UserDataStore dataStore;

    public DemoService(MetricsFactory metricsFactory, UserDataStore dataStore) {

        this.metricsFactory = metricsFactory;
        this.dataStore = dataStore;
    }

    public UserDetails getUserDetails(String userId) throws IOException {

        boolean success = true;
        Metric metric = metricsFactory.newMetric("GetUserDetails");
        metric.open();
        try {
            List<User> users = dataStore.getUserDetails(userId);
            metric.addMeasure("UsersReturned", users.size());
            return users;
        } catch (IOException e) {
            success = false;
            throw e;
        } finally {
            metric.addMeasure("Success", success ? 1.0 : 0.0);
            metric.addMeasure("Failure", !success ? 1.0 : 0.0);
            metric.close();
        }
    }
}
```
The above code produces 4 metrics for method call: duration, number of users loaded from data store, success and failure. 
If you have any question or if you want to contribute, please contact us: https://deevvi.com/#contact
