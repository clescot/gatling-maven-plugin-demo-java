import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class GatlingReporter {

    public static final String LANGUAGE_ID = "js";
    public static final String STATS_JS_PATH = "/js/stats.js";
    final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static void main(String[] args) throws IOException {
        GatlingReporter gatlingReporter = new GatlingReporter();
        File lastGatlingTestExecutionDirectory = gatlingReporter.getLastGatlingDirectory();
        System.out.println(getGatlingExecutionDataAsJson(gatlingReporter, lastGatlingTestExecutionDirectory));
    }

    private static String getGatlingExecutionDataAsJson(GatlingReporter gatlingReporter, File lastGatlingTestExecutionDirectory) throws IOException {
        Value root = gatlingReporter.getStatsVariable(lastGatlingTestExecutionDirectory);
        Map<String, Object> data = Maps.newHashMap();
        Map<String, Object> rootAttributes = gatlingReporter.getAttributes(root);
        data.put("root",rootAttributes);

        Value requests = root.getMember("contents");
        Set<String> contentMemberKeys = requests.getMemberKeys();
        Map<String, Object> requestsData = Maps.newHashMap();
        rootAttributes.put("contents",requestsData);
        for (String contentKey : contentMemberKeys) {
            Map<String, Object> contentKeyAttributes = gatlingReporter.getAttributes(root.getMember("contents").getMember(contentKey));
            requestsData.put(contentKey,contentKeyAttributes);
        }

        return OBJECT_MAPPER.writeValueAsString(data);
    }

    private  Map<String,Object> getAttributes(Value root) {
        Map<String,Object> rootAttributes = Maps.newHashMap();

        String type = root.getMember("type").asString();
        rootAttributes.put("type",type);

        String name = root.getMember("name").asString();
        rootAttributes.put("name",name);

        String path = root.getMember("path").asString();
        rootAttributes.put("path",path);

        String pathFormatted = root.getMember("pathFormatted").asString();
        rootAttributes.put("pathFormatted",pathFormatted);

        Map<String,Object> statsAttributes = Maps.newHashMap();
        rootAttributes.put("stats",statsAttributes);

        Value stats = root.getMember("stats");
        List<String> memberKeys = stats.getMemberKeys().stream()
                .filter(key -> !key.startsWith("group"))
                .filter(key -> !key.equals("name"))
                .collect(Collectors.toUnmodifiableList());
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
        statuses.put("total", replaceDashByNull(value.getMember("total").asString()));
        statuses.put("ok", replaceDashByNull(value.getMember("ok").asString()));
        statuses.put("ko", replaceDashByNull(value.getMember("ko").asString()));
        return statuses;
    }

    private Map<String,String> parseGroup(Value value){
        Map<String,String> map = Maps.newHashMap();
        map.put("name",(value.getMember("name").asString()));
        map.put("htmlName",(value.getMember("htmlName").asString()));
        map.put("count",(value.getMember("count").asInt()+""));
        map.put("percentage",(value.getMember("percentage").asInt()+""));
        return map;
    }

    private Map<String,Map<String,String>> parseGroups(Value parent){
        Map<String,Map<String,String>> map = Maps.newHashMap();
        List<String> list = parent.getMemberKeys().stream().filter(name->name.startsWith("group")).collect(Collectors.toUnmodifiableList());
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
