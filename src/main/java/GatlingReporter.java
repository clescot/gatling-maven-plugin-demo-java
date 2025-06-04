import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.exporter.pushgateway.PushGateway;
import io.prometheus.metrics.model.snapshots.Unit;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class GatlingReporter {

    public static final String LANGUAGE_ID = "js";
    public static final String STATS_JS_PATH = "/js/stats.js";
    final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final String CONTENTS = "contents";
    public static final String OK = "ok";
    public static final String KO = "ko";
    public static final String TOTAL = "total";
    public static final String NAME = "name";
    public static final String HTML_NAME = "htmlName";
    public static final String COUNT = "count";
    public static final String PERCENTAGE = "percentage";

    public static void main(String[] args) throws IOException {

        GatlingReporter gatlingReporter = new GatlingReporter();
        PushGateway pushGateway = PushGateway.builder()
                .address("localhost:9091") // not needed as localhost:9091 is the default
                .job("gatling")
                .build();
        File lastGatlingTestExecutionDirectory = gatlingReporter.getLastGatlingDirectory();
        String gatlingExecutionDataAsJson = gatlingReporter.getGatlingExecutionDataAsJson(gatlingReporter, lastGatlingTestExecutionDirectory);
        System.out.println(gatlingExecutionDataAsJson);
        Gauge dataProcessedInBytes = Gauge.builder()
                .name("data_processed")
                .help("data processed in the last batch job run")
                .unit(Unit.BYTES)
                .register();
        pushGateway.push();
    }

    private String getGatlingExecutionDataAsJson(GatlingReporter gatlingReporter, File lastGatlingTestExecutionDirectory) throws IOException {
        Value root = gatlingReporter.getStatsVariable(lastGatlingTestExecutionDirectory);
        Map<String, Object> data = Maps.newHashMap();
        Map<String, Object> rootAttributes = gatlingReporter.getAttributes(root);
        data.put("root",rootAttributes);

        Value requests = root.getMember(CONTENTS);
        Set<String> contentMemberKeys = requests.getMemberKeys();
        Map<String, Object> requestsData = Maps.newHashMap();
        rootAttributes.put(CONTENTS,requestsData);
        for (String contentKey : contentMemberKeys) {
            Map<String, Object> contentKeyAttributes = gatlingReporter.getAttributes(root.getMember(CONTENTS).getMember(contentKey));
            requestsData.put(contentKey,contentKeyAttributes);
        }

        return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(data);
    }

    private  Map<String,Object> getAttributes(Value root) {
        Map<String,Object> rootAttributes = Maps.newHashMap();

        String type = root.getMember("type").asString();
        rootAttributes.put("type",type);

        String name = root.getMember(NAME).asString();
        rootAttributes.put(NAME,name);

        String path = root.getMember("path").asString();
        rootAttributes.put("path",path);

        String pathFormatted = root.getMember("pathFormatted").asString();
        rootAttributes.put("pathFormatted",pathFormatted);

        Map<String,Object> statsAttributes = Maps.newHashMap();
        rootAttributes.put("stats",statsAttributes);

        Value stats = root.getMember("stats");
        List<String> memberKeys = stats.getMemberKeys().stream()
                .filter(key -> !key.startsWith("group"))
                .filter(key -> !key.equals(NAME))
                .toList();
        Map<String,Map<String,String>> statsValues = getStats(
                stats,
                memberKeys
        );

        statsAttributes.putAll(statsValues);
        Map<String, Map<String, String>> groupAttributes = parseGroups(stats);

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

        statsAttributes.putAll(groupAttributes);
        return rootAttributes;
    }

    private  Value getStatsVariable(File gatlingTestExecutionDirectory) throws IOException {
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
        Preconditions.checkNotNull(files,"no gatling directory found");
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());

        return files[0];
    }

    private Map<String,Map<String,String>> getStats(Value parent, List<String> keys){
        Map<String,Map<String,String>> statusesWithKeys = Maps.newHashMap();
        for (String key : keys) {
            Value member = parent.getMember(key);
            statusesWithKeys.put(key, getStats(member));
        }
        return statusesWithKeys;
    }

    private Map<String,String> getStats(Value value){
        Map<String,String> statuses = Maps.newHashMap();
        statuses.put(TOTAL, replaceDashByNull(value.getMember(TOTAL).asString()));
        statuses.put(OK, replaceDashByNull(value.getMember(OK).asString()));
        statuses.put(KO, replaceDashByNull(value.getMember(KO).asString()));
        return statuses;
    }

    private Map<String,String> parseGroup(Value value){
        Map<String,String> map = Maps.newHashMap();
        map.put(NAME,(value.getMember(NAME).asString()));
        map.put(HTML_NAME,(value.getMember(HTML_NAME).asString()));
        map.put(COUNT,(value.getMember(COUNT).asInt()+""));
        map.put(PERCENTAGE,(value.getMember(PERCENTAGE).asInt()+""));
        return map;
    }

    private Map<String,Map<String,String>> parseGroups(Value parent){
        Map<String,Map<String,String>> map = Maps.newHashMap();
        List<String> list = parent.getMemberKeys().stream()
                .filter(name->name.startsWith("group"))
                .toList();
        for (String groupId : list) {
            map.put(groupId,parseGroup(parent.getMember(groupId)));
        }
        return map;
    }

    private String replaceDashByNull(String value){
        if("-".equals(value)){
            return null;
        }
        return value;
    }
}
