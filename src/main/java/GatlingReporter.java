import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.prometheus.metrics.core.metrics.Counter;
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
    private Map<String,String> indicatorsMapper = getDefaultIndicatorsMapper();

    public GatlingReporter(URL gatlingConfigUrl) throws IOException, URISyntaxException {
        this.gatlingConfigUrl = gatlingConfigUrl;
        String gatlingConf = Files.readString(Paths.get(gatlingConfigUrl.toURI()));
    }

    public GatlingReporter() {}


    public static void main(String[] args) throws IOException, URISyntaxException {
        URL gatlingConfigUrl = Thread.currentThread().getContextClassLoader().getResource("gatling.conf");
        GatlingReporter gatlingReporter;
        if(gatlingConfigUrl!=null) {
            gatlingReporter = new GatlingReporter(gatlingConfigUrl);
        }else{
            gatlingReporter = new GatlingReporter();
        }

        PrometheusRegistry prometheusRegistry = new PrometheusRegistry();
        String pushGatewayAddress = "localhost:9091";
        String jobName = "gatling";
        PushGateway pushGateway = PushGateway.builder()
                .address(pushGatewayAddress)
                .registry(prometheusRegistry)// not needed as localhost:9091 is the default
                .job(jobName)
                .build();
        File lastGatlingTestExecutionDirectory = gatlingReporter.getLastGatlingDirectory();
        List<Counter> counters = gatlingReporter.
                getGatlingExecutionMetrics(
                prometheusRegistry,
                gatlingReporter,
                lastGatlingTestExecutionDirectory);
        pushGateway.push();
    }

    private List<Counter> getGatlingExecutionMetrics(PrometheusRegistry prometheusRegistry,
                                                     GatlingReporter gatlingReporter,
                                                     File lastGatlingTestExecutionDirectory) throws IOException {
        Value root = gatlingReporter.getStatsVariable(lastGatlingTestExecutionDirectory);
        List<Counter> counters = Lists.newArrayList();
        List<Counter> rootCounters = gatlingReporter.getAttributes(prometheusRegistry, root);
        counters.addAll(rootCounters);
        Value requests = root.getMember(CONTENTS);
        Set<String> contentMemberKeys = requests.getMemberKeys();
        for (String contentKey : contentMemberKeys) {
            List<Counter> contentCounters = gatlingReporter.getAttributes(prometheusRegistry,
                    root.getMember(CONTENTS).getMember(contentKey)
            );
            counters.addAll(contentCounters);
        }

        return counters;
    }

    private List<Counter> getAttributes(PrometheusRegistry prometheusRegistry, Value root) {
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
        List<Counter> counters = registerStatsCounters(prometheusRegistry, statsName,
                stats,
                memberKeys
        );

        List<Counter> groupCounters = parseGroups(prometheusRegistry,stats);
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

    private Map<String, String> getDefaultIndicatorsMapper() {
        Map<String, String> indicatorsMapper = Maps.newHashMap();
        indicatorsMapper.put("percentiles1", "p50");
        indicatorsMapper.put("percentiles2", "p75");
        indicatorsMapper.put("percentiles3", "p95");
        indicatorsMapper.put("percentiles4", "p99");
        indicatorsMapper.put("lowerBound", "lower_bound_800");
        indicatorsMapper.put("higherBound", "higher_bound_1200");
        return indicatorsMapper;
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

            String snakeCaseKey = statsName + "_" + convertCamelCaseToSnakeRegex(myKey.replaceAll("\\s", "_"));
            String total = replaceDashByNull(member.getMember(TOTAL).asString());
            Counter counterTotal = buildCounter(prometheusRegistry, snakeCaseKey + "_total", total);
            counters.add(counterTotal);
            String ok = replaceDashByNull(member.getMember(OK).asString());
            Counter counterOk = buildCounter(prometheusRegistry, snakeCaseKey + "_ok_total", ok);
            counters.add(counterOk);
            String ko = replaceDashByNull(member.getMember(KO).asString());
            Counter counterKo = buildCounter(prometheusRegistry, snakeCaseKey + "_ko_total", ko);
            counters.add(counterKo);
        }
        return counters;
    }

    private Counter buildCounter(PrometheusRegistry prometheusRegistry, String snakeCaseKey, String value) {
        Counter counter = Counter.builder().name(snakeCaseKey)
                //.unit(Unit.SECONDS)
                .register(prometheusRegistry);
        boolean convertMillisToSecondNeeded = false;
        if (!snakeCaseKey.contains("per_second")) {
            convertMillisToSecondNeeded = true;
        }
        if (value != null) {
            if (convertMillisToSecondNeeded) {
                double amountAsDouble = Unit.millisToSeconds(Long.parseLong(value));
                counter.inc(amountAsDouble);
            } else {
                counter.inc(Double.parseDouble(value));
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


    private Counter parseGroup(PrometheusRegistry prometheusRegistry,String parentName, Value value) {
        String string = parentName+"_"+value.getMember(NAME)
                .asString()
                .replaceAll("\\s","_")
                .replaceAll("<=","lower_or_equal_than")
                .replaceAll("<","lower_than")
                .replaceAll(">=","higher_or_equal_than")
                .replaceAll(">","higher_than")
                ;
        Counter counter = Counter.builder()
                .name(string)
                .register(prometheusRegistry);
        counter.inc(Long.parseLong(value.getMember(COUNT).asInt() + ""));
        return counter;
    }

    private List<Counter> parseGroups(PrometheusRegistry prometheusRegistry, Value parent) {
        List<Counter> groupCounters = Lists.newArrayList();
        List<String> list = parent.getMemberKeys().stream()
                .filter(name -> name.startsWith("group"))
                .toList();
        String parentName = convertCamelCaseToSnakeRegex(parent.getMember("name").toString()).replaceAll("\\s","_");
        for (String groupId : list) {
            groupCounters.add(parseGroup(prometheusRegistry, parentName, parent.getMember(groupId)));
        }
        return groupCounters;
    }

    private String replaceDashByNull(String value) {
        if ("-".equals(value)) {
            return null;
        }
        return value;
    }
}
