import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.graalvm.polyglot.Value;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ParseGroupsTest {

    @Test
    public void testParseGroups() {
        // Given
        GatlingReporter gatlingReporter = new GatlingReporter();
        PrometheusRegistry prometheusRegistry = new PrometheusRegistry();
        String javascriptContent= """
                var stats = {
                    type: "GROUP",
                name: "All Requests",
                path: "",
                pathFormatted: "group_missing-name--1146707516",
                stats: {
                    "name": "All Requests",
                    "numberOfRequests": {
                        "total": "1",
                        "ok": "1",
                        "ko": "0"
                    },
                    "minResponseTime": {
                        "total": "732",
                        "ok": "732",
                        "ko": "-"
                    },
                    "maxResponseTime": {
                        "total": "732",
                        "ok": "732",
                        "ko": "-"
                    },
                    "meanResponseTime": {
                        "total": "732",
                        "ok": "732",
                        "ko": "-"
                    },
                    "standardDeviation": {
                        "total": "0",
                        "ok": "0",
                        "ko": "-"
                    },
                    "percentiles1": {
                        "total": "732",
                        "ok": "732",
                        "ko": "-"
                    },
                    "percentiles2": {
                        "total": "732",
                        "ok": "732",
                        "ko": "-"
                    },
                    "percentiles3": {
                        "total": "732",
                        "ok": "732",
                        "ko": "-"
                    },
                    "percentiles4": {
                        "total": "732",
                        "ok": "732",
                        "ko": "-"
                    },
                    "group1": {
                    "name": "t < 800 ms",
                    "htmlName": "t < 800 ms",
                    "count": 1,
                    "percentage": 100.0
                },
                    "group2": {
                    "name": "800 ms <= t < 1200 ms",
                    "htmlName": "t >= 800 ms <br> t < 1200 ms",
                    "count": 0,
                    "percentage": 0.0
                },
                    "group3": {
                    "name": "t >= 1200 ms",
                    "htmlName": "t >= 1200 ms",
                    "count": 0,
                    "percentage": 0.0
                },
                    "group4": {
                    "name": "failed",
                    "htmlName": "failed",
                    "count": 0,
                    "percentage": 0.0
                },
                    "meanNumberOfRequestsPerSecond": {
                        "total": "1",
                        "ok": "1",
                        "ko": "-"
                    }
                },
                contents: {
                "req_session--645326218": {
                        type: "REQUEST",
                        name: "Session",
                path: "Session",
                pathFormatted: "req_session--645326218",
                stats: {
                    "name": "Session",
                    "numberOfRequests": {
                        "total": "1",
                        "ok": "1",
                        "ko": "0"
                    },
                    "minResponseTime": {
                        "total": "732",
                        "ok": "732",
                        "ko": "-"
                    },
                    "maxResponseTime": {
                        "total": "732",
                        "ok": "732",
                        "ko": "-"
                    },
                    "meanResponseTime": {
                        "total": "732",
                        "ok": "732",
                        "ko": "-"
                    },
                    "standardDeviation": {
                        "total": "0",
                        "ok": "0",
                        "ko": "-"
                    },
                    "percentiles1": {
                        "total": "732",
                        "ok": "732",
                        "ko": "-"
                    },
                    "percentiles2": {
                        "total": "732",
                        "ok": "732",
                        "ko": "-"
                    },
                    "percentiles3": {
                        "total": "732",
                        "ok": "732",
                        "ko": "-"
                    },
                    "percentiles4": {
                        "total": "732",
                        "ok": "732",
                        "ko": "-"
                    },
                    "group1": {
                    "name": "t < 800 ms",
                    "htmlName": "t < 800 ms",
                    "count": 1,
                    "percentage": 100.0
                },
                    "group2": {
                    "name": "800 ms <= t < 1200 ms",
                    "htmlName": "t >= 800 ms <br> t < 1200 ms",
                    "count": 0,
                    "percentage": 0.0
                },
                    "group3": {
                    "name": "t >= 1200 ms",
                    "htmlName": "t >= 1200 ms",
                    "count": 0,
                    "percentage": 0.0
                },
                    "group4": {
                    "name": "failed",
                    "htmlName": "failed",
                    "count": 0,
                    "percentage": 0.0
                },
                    "meanNumberOfRequestsPerSecond": {
                        "total": "1",
                        "ok": "1",
                        "ko": "-"
                    }
                }
                    }
                }
                
                }
                
                function fillStats(stat){
                    $("#numberOfRequests").append(stat.numberOfRequests.total);
                    $("#numberOfRequestsOK").append(stat.numberOfRequests.ok);
                    $("#numberOfRequestsKO").append(stat.numberOfRequests.ko);
                
                    $("#minResponseTime").append(stat.minResponseTime.total);
                    $("#minResponseTimeOK").append(stat.minResponseTime.ok);
                    $("#minResponseTimeKO").append(stat.minResponseTime.ko);
                
                    $("#maxResponseTime").append(stat.maxResponseTime.total);
                    $("#maxResponseTimeOK").append(stat.maxResponseTime.ok);
                    $("#maxResponseTimeKO").append(stat.maxResponseTime.ko);
                
                    $("#meanResponseTime").append(stat.meanResponseTime.total);
                    $("#meanResponseTimeOK").append(stat.meanResponseTime.ok);
                    $("#meanResponseTimeKO").append(stat.meanResponseTime.ko);
                
                    $("#standardDeviation").append(stat.standardDeviation.total);
                    $("#standardDeviationOK").append(stat.standardDeviation.ok);
                    $("#standardDeviationKO").append(stat.standardDeviation.ko);
                
                    $("#percentiles1").append(stat.percentiles1.total);
                    $("#percentiles1OK").append(stat.percentiles1.ok);
                    $("#percentiles1KO").append(stat.percentiles1.ko);
                
                    $("#percentiles2").append(stat.percentiles2.total);
                    $("#percentiles2OK").append(stat.percentiles2.ok);
                    $("#percentiles2KO").append(stat.percentiles2.ko);
                
                    $("#percentiles3").append(stat.percentiles3.total);
                    $("#percentiles3OK").append(stat.percentiles3.ok);
                    $("#percentiles3KO").append(stat.percentiles3.ko);
                
                    $("#percentiles4").append(stat.percentiles4.total);
                    $("#percentiles4OK").append(stat.percentiles4.ok);
                    $("#percentiles4KO").append(stat.percentiles4.ko);
                
                    $("#meanNumberOfRequestsPerSecond").append(stat.meanNumberOfRequestsPerSecond.total);
                    $("#meanNumberOfRequestsPerSecondOK").append(stat.meanNumberOfRequestsPerSecond.ok);
                    $("#meanNumberOfRequestsPerSecondKO").append(stat.meanNumberOfRequestsPerSecond.ko);
                }
                
                """;
        Value stats = gatlingReporter.getJavascriptValueBoundToKey(javascriptContent, "stats");
        List<Counter> countersFromGroups = gatlingReporter.parseGroups(prometheusRegistry, stats.getMember("stats"));
        assertThat(countersFromGroups.size()).isEqualTo(4);
    }
}
