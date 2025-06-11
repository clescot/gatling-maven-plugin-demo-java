import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.gatling.charts.stats.LogFileReader;
import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Summary;
import io.prometheus.metrics.exporter.pushgateway.PushGateway;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.Unit;
import org.graalvm.collections.Pair;
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
    public static final String HTML_NAME = "htmlName";
    public static final String COUNT = "count";
    public static final String PERCENTAGE = "percentage";
    private URL gatlingConfigUrl;
    Map<String, Pair<String,String>> indicatorsMapper = getDefaultIndicatorsMapper();
    public GatlingReporter(){}
    public GatlingReporter(URL gatlingConfigUrl) throws IOException, URISyntaxException {
        this.gatlingConfigUrl = gatlingConfigUrl;
        String gatlingConf = Files.readString(Paths.get(gatlingConfigUrl.toURI()));
        this.indicatorsMapper = updateIndicatorsMapper(indicatorsMapper,gatlingConf);
    }


    public static void main(String[] args) throws IOException, URISyntaxException {
        URL gatlingConfigUrl = Thread.currentThread().getContextClassLoader().getResource("gatling.conf");
        GatlingReporter gatlingReporter;
        if(gatlingConfigUrl!=null) {
            gatlingReporter = new GatlingReporter(gatlingConfigUrl);
        }else{
            gatlingReporter = new GatlingReporter();
        }

        PrometheusRegistry prometheusRegistry = new PrometheusRegistry();
        String jobName = "gatling";
        String pushGatewayAddress = "localhost:9091";
        PushGateway pushGateway = PushGateway.builder()
                .address(pushGatewayAddress)
                .registry(prometheusRegistry)// not needed as localhost:9091 is the default
                .job(jobName)
                .build();
        File lastGatlingTestExecutionDirectory = gatlingReporter.getLastGatlingDirectory();
        Map<String, Counter> counters = gatlingReporter.
                getGatlingExecutionMetrics(
                        prometheusRegistry,
                        gatlingReporter,
                        lastGatlingTestExecutionDirectory);
        //counters are already registered into the prometheusRegistry instance
//        pushGateway.push();
    }

    private Map<String, Pair<String,String>> getDefaultIndicatorsMapper() {
        Map<String, Pair<String,String>> indicatorsMapper = Maps.newHashMap();
        indicatorsMapper.put("percentiles1", Pair.create("percentile","50"));
        indicatorsMapper.put("percentiles2", Pair.create("percentile","75"));
        indicatorsMapper.put("percentiles3", Pair.create("percentile","95"));
        indicatorsMapper.put("percentiles4", Pair.create("percentile","99"));
        indicatorsMapper.put("lowerBound", Pair.create("lower_bound","800"));
        indicatorsMapper.put("higherBound", Pair.create("higher_bound","1200"));
        return indicatorsMapper;
    }

    private Map<String, Pair<String,String>> updateIndicatorsMapper(Map<String, Pair<String,String>> defaultIndicatorsMapper, String gatlingConf) {
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
        defaultIndicatorsMapper.put("percentile1", Pair.create("percentile",percentile1));
        defaultIndicatorsMapper.put("percentile2", Pair.create("percentile",percentile2));
        defaultIndicatorsMapper.put("percentile3", Pair.create("percentile",percentile3));
        defaultIndicatorsMapper.put("percentile4", Pair.create("percentile",percentile4));
        defaultIndicatorsMapper.put("lowerBound", Pair.create("lower_bound",lowerBound));
        defaultIndicatorsMapper.put("higherBound", Pair.create("higher_bound",higherBound));
        return defaultIndicatorsMapper;
    }

    private Map<String, Counter> getGatlingExecutionMetrics(PrometheusRegistry prometheusRegistry,
                                                            GatlingReporter gatlingReporter,
                                                            File lastGatlingTestExecutionDirectory) throws IOException {
        Value root = gatlingReporter.getStatsVariable(lastGatlingTestExecutionDirectory);
        Map<String, Counter> counters = gatlingReporter.getAttributes(prometheusRegistry, root);
        Value requests = root.getMember(CONTENTS);
        Set<String> contentMemberKeys = requests.getMemberKeys();
        for (String contentKey : contentMemberKeys) {
            Map<String, Counter> contentCounters = gatlingReporter.getAttributes(prometheusRegistry,
                    root.getMember(CONTENTS).getMember(contentKey)
            );
            counters.putAll(contentCounters);
        }

        return counters;
    }

    private Map<String, Counter> getAttributes(PrometheusRegistry prometheusRegistry, Value root) {
        Map<String, Object> rootAttributes = Maps.newHashMap();

        //String type = root.getMember("type").asString();
        //rootAttributes.put("type",type);

        //String name = root.getMember(NAME).asString();
        //rootAttributes.put(NAME,name);

//        String path = root.getMember("path").asString();
//        rootAttributes.put("path",path);

//        String pathFormatted = root.getMember("pathFormatted").asString();
//        rootAttributes.put("pathFormatted",pathFormatted);


        Value stats = root.getMember("stats");
        Set<String> statsMemberKeys = stats.getMemberKeys();
        List<String> memberKeys = statsMemberKeys.stream()
                .filter(key -> !key.startsWith("group"))
                .filter(key -> !key.equals(NAME))
                .toList();
        String statsName = stats.getMember("name").toString().toLowerCase().replaceAll("\\s", "_");
        Map<String, Counter> counters = registerStatsCounters(prometheusRegistry, statsName,
                stats,
                memberKeys
        );

        counters = parseGroups(prometheusRegistry, counters, stats);
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

    private Value getStatsVariable(File gatlingTestExecutionDirectory) throws IOException {
        String jsContent = new String(Files.readAllBytes(Paths.get(gatlingTestExecutionDirectory.toString() + STATS_JS_PATH)));
        Value contextBindings;
        Context context = Context.newBuilder(LANGUAGE_ID).build();
        context.eval(LANGUAGE_ID, jsContent);
        contextBindings = context.getBindings(LANGUAGE_ID);
        return contextBindings.getMember("stats");
    }

    private File getLastGatlingDirectory() {
        Path absolutePath = Paths.get("").toAbsolutePath();
        Path target = absolutePath.resolve("target/gatling");
        File gatlingDir = new File(target.toString());
        File[] files = gatlingDir.listFiles(File::isDirectory);
        Preconditions.checkNotNull(files, "no gatling directory found");
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());

        return files[0];
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
    private Map<String, Counter> registerStatsCounters(PrometheusRegistry prometheusRegistry,
                                                       String parentName,
                                                       Value parent,
                                                       List<String> keys) {
        Map<String,Counter> counters = Maps.newHashMap();
        for (String key : keys) {
            Value member = parent.getMember(key);
            String labelKey = null;
            String labelValue = null;
            String snakeCaseKey = parentName;
            if(this.indicatorsMapper.containsKey(key)){
                labelKey= indicatorsMapper.get(key).getLeft();
                labelValue= indicatorsMapper.get(key).getRight();
            }else{
                snakeCaseKey = snakeCaseKey + "_" + convertCamelCaseToSnakeRegex(key.replaceAll("\\s", "_"));
            }
            String total = replaceDashByNull(member.getMember(TOTAL).asString());
            Pair<String, String> pair = labelKey!=null?Pair.create(labelKey, labelValue):null;
            String totalCounterName = snakeCaseKey + "_total";
            Counter counterTotal = buildCounter(prometheusRegistry, totalCounterName, total, pair);
            counters.put(totalCounterName,counterTotal);
            String ok = replaceDashByNull(member.getMember(OK).asString());
            String okCounterName = snakeCaseKey + "_ok_total";
            Counter counterOk = buildCounter(prometheusRegistry, okCounterName, ok,pair);
            counters.put(okCounterName,counterOk);
            String ko = replaceDashByNull(member.getMember(KO).asString());
            String koCounterName = snakeCaseKey + "_ko_total";
            Counter counterKo = buildCounter(prometheusRegistry, koCounterName, ko,pair);
            counters.put(koCounterName,counterKo);
        }
        return counters;
    }

    private Counter buildCounter(PrometheusRegistry prometheusRegistry,  String snakeCaseKey, String value, Pair<String,String>... pairs) {
        Counter.Builder builder = Counter.builder().name(snakeCaseKey);
        Pair<String, String> firstPair = null;
        if(pairs != null && pairs.length > 0 && pairs[0]!=null) {
            firstPair = pairs[0];
            if (firstPair.getLeft() != null) {
                builder = builder.labelNames(firstPair.getLeft());
            }
        }
        Counter counter = builder
                //.unit(Unit.SECONDS)
                .register(prometheusRegistry);

        boolean convertMillisToSecondNeeded = false;
        if (!snakeCaseKey.contains("per_second")) {
            convertMillisToSecondNeeded = true;
        }
        if (value != null) {
            double amountAsDouble;
            if (convertMillisToSecondNeeded) {
                amountAsDouble = Unit.millisToSeconds(Long.parseLong(value));
            } else {
                amountAsDouble = Double.parseDouble(value);
            }
            if(pairs != null && pairs.length > 0 && pairs[0]!=null) {
                counter.labelValues(firstPair.getRight()).inc(amountAsDouble);
            } else {
                counter.inc(amountAsDouble);
            }
        }




        return counter;
    }

    public String convertCamelCaseToSnakeRegex(String input) {
        return input
                .replaceAll("([A-Z])(?=[A-Z])", "$1_")
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toLowerCase();
    }


    private Counter parseGroup(PrometheusRegistry prometheusRegistry, String counterName, Value value) {

        Counter counter = Counter.builder()
                .name(counterName)
                .register(prometheusRegistry);
        counter.inc(Long.parseLong(value.getMember(COUNT).asInt() + ""));
        return counter;
    }

    private Map<String,Counter> parseGroups(PrometheusRegistry prometheusRegistry,Map<String,Counter> counters, Value parent) {

        List<String> list = parent.getMemberKeys().stream()
                .filter(name -> name.startsWith("group"))
                .toList();
        String parentName = convertCamelCaseToSnakeRegex(parent.getMember("name").toString()).replaceAll("\\s", "_");
        for (String groupId : list) {
            Value value = parent.getMember(groupId);
            String counterName = parentName + "_" + value.getMember(NAME)
                    .asString()
                    .replaceAll("\\s", "_")
                    .replaceAll("<=", "lower_or_equal_than")
                    .replaceAll("<", "lower_than")
                    .replaceAll(">=", "higher_or_equal_than")
                    .replaceAll(">", "higher_than");
            if(!counters.containsKey(counterName)) {
                counters.put(counterName, parseGroup(prometheusRegistry, counterName, value));
            }
        }
        return counters;
    }

    private String replaceDashByNull(String value) {
        if ("-".equals(value)) {
            return null;
        }
        return value;
    }
}
