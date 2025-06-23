import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.exporter.pushgateway.PushGateway;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.Unit;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class GatlingReporter {

    public static final String LANGUAGE_ID = "js";
    public static final String STATS_JS_PATH = "/js/stats.js";
    public static final String CONTENTS = "contents";
    public static final String OK = "ok";
    public static final String KO = "ko";
    public static final String TOTAL = "total";
    public static final String NAME = "name";
    public static final String COUNT = "count";
    private final Map<String,String> indicatorsMapper = getIndicatorsMapper();
    private final PrometheusRegistry prometheusRegistry = new PrometheusRegistry();


    public static void main(String[] args) throws IOException, URISyntaxException {
        Map<String, String> env = System.getenv();
        GatlingReporter gatlingReporter = getGatlingReporter(env);
        List<Counter> counters;
        Optional<String> directory = Optional.ofNullable(env.get("gatlingDirectory"));
        counters = gatlingReporter.getMetrics(directory);
        String pushGatewayAddress = Optional.ofNullable(env.get("pushGatewayAddress")).orElse("localhost:9091");
        String jobName = Optional.ofNullable(env.get("jobName")).orElse("gatling");
        gatlingReporter.pushToGateway(pushGatewayAddress,jobName);
    }

    private List<Counter> getMetrics(Optional<String> directory) throws IOException {
        List<Counter> counters;
        if(directory.isEmpty()) {
            counters = extractGatlingMetricsFromLastDirectory();
        }else {
            URL resource = Thread.currentThread().getContextClassLoader().getResource(".");
            if(resource == null) {
                throw new IOException("Resource not found: .");
            }
            File gatlingDirectoryRelativePath = new File(resource.getPath()+"../gatling/"+ directory.get());
            if(!gatlingDirectoryRelativePath.exists()){
                throw new IOException("Gatling directory does not exist: " + gatlingDirectoryRelativePath.getAbsolutePath());
            }
            counters = getGatlingExecutionMetrics(gatlingDirectoryRelativePath);
        }
        return counters;
    }

    private static GatlingReporter getGatlingReporter(Map<String, String> env) throws IOException, URISyntaxException {
        String gatlingConfPath = env.get("gatlingConfPath");
        URL gatlingConfigUrl = Thread.currentThread().getContextClassLoader().getResource(Optional.ofNullable(gatlingConfPath).orElse("gatling.conf"));

        GatlingReporter gatlingReporter;
        if(gatlingConfigUrl!=null) {
            gatlingReporter = new GatlingReporter(gatlingConfigUrl);
        }else{
            gatlingReporter = new GatlingReporter();
        }
        return gatlingReporter;
    }


    public GatlingReporter(URL gatlingConfigUrl) throws IOException, URISyntaxException {
        String gatlingConf = Files.readString(Paths.get(gatlingConfigUrl.toURI()));
        updateIndicatorsMapper(this.indicatorsMapper,gatlingConf);
    }

    public GatlingReporter() {}

    protected List<Counter> extractGatlingMetricsFromLastDirectory() throws IOException {
        File lastGatlingTestExecutionDirectory = getLastGatlingDirectory();
        return getGatlingExecutionMetrics(lastGatlingTestExecutionDirectory);
    }

    protected List<Counter> getGatlingExecutionMetrics(
                                                     File lastGatlingTestExecutionDirectory) throws IOException {
        Value statsFromRoot = getStatsFromRootValue(lastGatlingTestExecutionDirectory);
        String run  = lastGatlingTestExecutionDirectory.getName().replaceAll("-","_");
        List<Counter> counters = Lists.newArrayList();
        List<Counter> rootCounters = getCountersFromStats(statsFromRoot,run,null);
        counters.addAll(rootCounters);
        Value contents = statsFromRoot.getMember(CONTENTS);
        List<Counter> countersFromContent = getCountersFromContent(contents, run,null, counters);
        counters.addAll(countersFromContent);
        return counters;
    }

    private List<Counter> getCountersFromContent(Value contents, String run, String prefix,List<Counter> counters) {
        Set<String> contentMemberKeys = contents.getMemberKeys();
        for (String contentKey : contentMemberKeys) {
            Value member = contents.getMember(contentKey);
            String prefixKey = Optional.ofNullable(prefix).orElse("root") + "_";
            List<Counter> contentCounters = getCountersFromStats(
                    member,
                    run,
                    prefixKey + contentKey
            );
            counters.addAll(contentCounters);
            if(member.getMember("contents") != null) {
                Value subContents = member.getMember("contents");

                List<Counter> subContentCounters = getCountersFromContent(subContents, run,prefixKey+"_"+contentKey, counters);
                counters.addAll(subContentCounters);
            }
        }
        return counters;
    }

    private List<Counter> getCountersFromStats(Value statsValue, String run,String prefix) {

        Value stats = statsValue.getMember("stats");
        Set<String> statsMemberKeys = stats.getMemberKeys();
        List<String> memberKeys = statsMemberKeys.stream()
                .filter(key -> !key.startsWith("group"))
                .filter(key -> !key.equals(NAME))
                .toList();
        String statsName = Optional.ofNullable(prefix).orElse("root")+"_"+stats.getMember("name")
                .toString()
                .toLowerCase()
                .replaceAll("-", "_")
                .replaceAll("\\s", "_")
                ;
        List<Counter> counters = registerStatsCounters(prometheusRegistry, run, statsName,
                stats,
                memberKeys
        );

        List<Counter> groupCounters = parseGroups(prometheusRegistry,stats,statsName);
        counters.addAll(groupCounters);
        //col-2 => total
        //col-3 => nombre de OK
        //col-4 => nombre de KO
        //col-5 => % KO
        //col-6 => counts/sec
        //col-7 => min%
        //col-8 => 50%
        //col-9 => 75%
        //col-10 => 95%
        //col-11 => 99%
        //col-12 => max
        //col-13 => mean
        //col-14 => standard deviation

        return counters;
    }

    protected Value getStatsFromRootValue(File gatlingTestExecutionDirectory) throws IOException {
        String jsContent = new String(Files.readAllBytes(Paths.get(gatlingTestExecutionDirectory.toString() + STATS_JS_PATH)));
        return getJavascriptValueBoundToKey(jsContent, "stats");
    }

    Value getJavascriptValueBoundToKey(String jsContent, String key) {
        Value contextBindings;
        Context context = Context.newBuilder(LANGUAGE_ID).build();
        context.eval(LANGUAGE_ID, jsContent);
        contextBindings = context.getBindings(LANGUAGE_ID);
        return contextBindings.getMember(key);
    }

    protected File getLastGatlingDirectory() {
        Path absolutePath = Paths.get("").toAbsolutePath();
        Path target = absolutePath.resolve("target/gatling");
        File gatlingDir = new File(target.toString());
        File[] files = gatlingDir.listFiles(File::isDirectory);
        Preconditions.checkNotNull(files, "no gatling directory found");
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());

        return files[0];
    }

    private Map<String, String> getIndicatorsMapper() {
        Map<String, String> indicatorsMapper = Maps.newHashMap();
        indicatorsMapper.put("percentiles1", "p50");
        indicatorsMapper.put("percentiles2", "p75");
        indicatorsMapper.put("percentiles3", "p95");
        indicatorsMapper.put("percentiles4", "p99");
        indicatorsMapper.put("lowerBound", "lower_bound_800");
        indicatorsMapper.put("higherBound", "higher_bound_1200");
        return indicatorsMapper;
    }
    Map<String, String> updateIndicatorsMapper(Map<String, String> defaultIndicatorsMapper, String gatlingConf) {
        String percentile1 = "50";
        String percentile2 = "75";
        String percentile3 = "95";
        String percentile4 = "99";
        String lowerBound = "800";
        String higherBound = "1200";
        Config indicatorsConfig = ConfigFactory.parseString(gatlingConf).getConfig("gatling.charting.indicators");
        if (indicatorsConfig.hasPath("percentile1")) {
            percentile1 = ""+indicatorsConfig.getInt("percentile1");
        }
        if (indicatorsConfig.hasPath("percentile2")) {
            percentile2 = ""+indicatorsConfig.getInt("percentile2");
        }
        if (indicatorsConfig.hasPath("percentile3")) {
            percentile3 = ""+indicatorsConfig.getInt("percentile3");
        }
        if (indicatorsConfig.hasPath("percentile4")) {
            percentile4 = ""+indicatorsConfig.getInt("percentile4");
        }
        if (indicatorsConfig.hasPath("lowerBound")) {
            lowerBound = ""+indicatorsConfig.getInt("lowerBound");
        }
        if (indicatorsConfig.hasPath("higherBound")) {
            higherBound = ""+indicatorsConfig.getInt("higherBound");
        }
        defaultIndicatorsMapper.put("percentiles1", "p"+percentile1);
        defaultIndicatorsMapper.put("percentiles2", "p"+percentile2);
        defaultIndicatorsMapper.put("percentiles3", "p"+percentile3);
        defaultIndicatorsMapper.put("percentiles4", "p"+percentile4);
        defaultIndicatorsMapper.put("lowerBound", "lower_bound_"+lowerBound);
        defaultIndicatorsMapper.put("higherBound", "higher_bound_"+higherBound);
        return defaultIndicatorsMapper;
    }



    /**
     * keys
     * 0 = "numberOfRequests" => Counter
     * 1 = "minResponseTime" =>
     * 2 = "maxResponseTime"
     * 3 = "meanResponseTime"
     * 4 = "standardDeviation"
     * 5 = "percentiles1"
     * 6 = "percentiles2"
     * 7 = "percentiles3"
     * 8 = "percentiles4"
     * 9 = "meanNumberOfRequestsPerSecond"
     *
     * @param prometheusRegistry
     * @param parent
     * @param keys
     * @return
     */
    private List<Counter> registerStatsCounters(PrometheusRegistry prometheusRegistry,
                                                String run,
                                                String statsName,
                                                Value parent,
                                                List<String> keys) {
        List<Counter> counters = Lists.newArrayList();
        for (String key : keys) {
            Value member = parent.getMember(key);
            String myKey = key;
            if(indicatorsMapper.containsKey(key)){
                myKey = indicatorsMapper.get(key);
            }

            String snakeCaseKey = fixPrometheusMetricName(statsName + "_" + convertCamelCaseToSnake(fixPrometheusMetricName(myKey)));
            String total = replaceDashByNull(member.getMember(TOTAL).asString());
            Counter counterTotal = buildCounter(prometheusRegistry, run, snakeCaseKey , total);
            counters.add(counterTotal);
            String ok = replaceDashByNull(member.getMember(OK).asString());
            Counter counterOk = buildCounter(prometheusRegistry, run, snakeCaseKey + "_ok", ok);
            counters.add(counterOk);
            String ko = replaceDashByNull(member.getMember(KO).asString());
            Counter counterKo = buildCounter(prometheusRegistry, run, snakeCaseKey + "_ko", ko);
            counters.add(counterKo);
        }
        return counters;
    }

    protected String fixPrometheusMetricName(String metricName) {
        Preconditions.checkNotNull(metricName,"metricName is null");
        return metricName
                .replaceAll("\\s", "_")
                .replaceAll("-", "_")
                .toLowerCase();
    }

    protected Counter buildCounter(PrometheusRegistry prometheusRegistry,String run, String snakeCaseKey,  String value) {
        Counter.Builder builder = Counter.builder()
                .name(snakeCaseKey)
                .labelNames("run");
        boolean convertMillisToSecondNeeded = false;

        if(snakeCaseKey.contains("number_of_requests")){
            builder = builder.help("Number of requests for run " + run);
            //no unit
        } else if (snakeCaseKey.contains("mean_number_of_requests_per_second")) {
            builder = builder.help("Mean number of requests per second for run " + run);
            builder = builder.unit(Unit.SECONDS);
        } else if (snakeCaseKey.contains("min_response_time")) {
            builder = builder.help("Minimum response time for run " + run);
            builder = builder.unit(Unit.SECONDS);
            convertMillisToSecondNeeded = true;
        } else if (snakeCaseKey.contains("max_response_time")) {
            builder = builder.help("Maximum response time for run " + run);
            builder = builder.unit(Unit.SECONDS);
            convertMillisToSecondNeeded = true;
        } else if (snakeCaseKey.contains("mean_response_time")) {
            builder = builder.help("Mean response time for run " + run);
            builder = builder.unit(Unit.SECONDS);
            convertMillisToSecondNeeded = true;
        } else if (snakeCaseKey.contains("standard_deviation")) {
            builder = builder.help("Standard deviation of response time for run " + run);
            builder = builder.unit(Unit.SECONDS);
            convertMillisToSecondNeeded = true;
        } else if (snakeCaseKey.contains("percentiles")) {
            builder = builder.help("Percentiles of response time for run " + run);
            builder = builder.unit(Unit.SECONDS);
            convertMillisToSecondNeeded = true;
        }
        Counter counter = builder
                .register(prometheusRegistry);


        if (value != null) {
            double amountAsDouble;
            if (convertMillisToSecondNeeded) {
                amountAsDouble = Unit.millisToSeconds(Long.parseLong(value));
            } else {
                amountAsDouble = Double.parseDouble(value);
            }
            counter.labelValues(run).inc(amountAsDouble);
        }
        return counter;
    }

    public String convertCamelCaseToSnake(String metricName) {
        Preconditions.checkNotNull(metricName,"metricName is null");
        return metricName
                .replaceAll("([A-Z])(?=[A-Z])", "$1_")
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toLowerCase();
    }


     Counter parseGroup(PrometheusRegistry prometheusRegistry, String parentName, String memberAsString, long counterValue) {
        Preconditions.checkNotNull(prometheusRegistry,"prometheusRegistry is null");
        Preconditions.checkNotNull(parentName,"parentName is null");
        Preconditions.checkNotNull(memberAsString,"memberAsString is null");
        String string = parentName+"_"+ memberAsString
                .replaceAll("(\\d*) ms <= t < (\\d*) ms","t_between_$1_and_$2_ms_count")
                .replaceAll("<= (\\d*) ms","lower_or_equal_than_$1_ms_count")
                .replaceAll("< (\\d*) ms","lower_than_$1_ms_count")
                .replaceAll(">= (\\d*) ms","higher_or_equal_than_$1_ms_count")
                .replaceAll("> (\\d*) ms","higher_than_$1_ms_count")
                .replaceAll("\\s","_")
                ;
        Counter counter = Counter.builder()
                .name(string)
                .register(prometheusRegistry);
        counter.inc(counterValue);
        return counter;
    }

    List<Counter> parseGroups(PrometheusRegistry prometheusRegistry, Value parent,String statsName) {
        List<Counter> groupCounters = Lists.newArrayList();
        List<String> list = parent.getMemberKeys().stream()
                .filter(name -> name.startsWith("group"))
                .toList();
        String parentName = convertCamelCaseToSnake(fixPrometheusMetricName(statsName));
        for (String groupId : list) {
            Value member = parent.getMember(groupId);
            long counterValue = member.getMember(COUNT).asLong();
            String memberNameAsString = member.getMember(NAME).asString();
            groupCounters.add(
                    parseGroup(prometheusRegistry, parentName, memberNameAsString, counterValue)
            );
        }
        return groupCounters;
    }

    String replaceDashByNull(String value) {
        if ("-".equals(value)) {
            return null;
        }
        return value;
    }

    private void pushToGateway(String pushGatewayAddress, String jobName) throws IOException {

        PushGateway pushGateway = PushGateway.builder()
                .address(pushGatewayAddress)
                .registry(prometheusRegistry)// not needed as localhost:9091 is the default
                .job(jobName)
                .instanceIpGroupingKey()
                .build();

        pushGateway.push();
    }
}
