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

public class GatlingReporter {
    public static void main(String[] args) throws IOException {
        GatlingReporter gatlingReporter = new GatlingReporter();
        Value root = gatlingReporter.getStatsVariable();
        Map<String, Object> data = Maps.newHashMap();
        Map<String, Object> rootAttributes = gatlingReporter.getAttributes(root);
        data.put("root",rootAttributes);

        Value requests = root.getMember("contents");
        Set<String> contentMemberKeys = requests.getMemberKeys();
        Map<String, Object> requestsData = Maps.newHashMap();
        data.put("contents",requestsData);
        for (String contentKey : contentMemberKeys) {
            Map<String, Object> contentKeyAttributes = gatlingReporter.getAttributes(root.getMember("contents").getMember(contentKey));
            requestsData.put(contentKey,contentKeyAttributes);
        }
        System.out.println(data);
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
        Map<String,Map<String,String>> statsValues = getStats(
                stats,
                "numberOfRequests",
                "minResponseTime",
                "maxResponseTime",
                "meanResponseTime",
                "standardDeviation",
                "percentiles1",
                "percentiles2",
                "percentiles3",
                "percentiles4",
                "meanNumberOfRequestsPerSecond"
        );

        statsAttributes.putAll(statsValues);
        Map<String, Map<String, String>> groupAttributes = parseGroups(
                stats,
                "group1",
                "group2",
                "group3",
                "group4");

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

    private  Value getStatsVariable() throws IOException {
        File lastGatlingDirectory = getLastGatlingDirectory();
        String jsContent = new String(Files.readAllBytes(Paths.get(lastGatlingDirectory.toString() + "/js/stats.js")));
        Context context = Context.newBuilder("js").build();
        context.eval("js", jsContent);
        Value contextBindings = context.getBindings("js");
        return contextBindings.getMember("stats");
    }

    private File getLastGatlingDirectory() {
        Path absolutePath = Paths.get("").toAbsolutePath();
        Path target = absolutePath.resolve("target/gatling");
        File gatlingDir = new File(target.toString());
        File[] files = gatlingDir.listFiles(File::isDirectory);

        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());

        return files[0];
    }

    private Map<String,Map<String,String>> getStats(Value parent, String... keys){
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

    private Map<String,Map<String,String>> parseGroups(Value parent,String... groupIds){
        Map<String,Map<String,String>> map = Maps.newHashMap();
        List<String> list = Lists.newArrayList(groupIds);
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
