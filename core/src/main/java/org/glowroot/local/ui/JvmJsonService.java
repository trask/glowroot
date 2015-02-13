/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glowroot.local.ui;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.Nullable;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.base.StandardSystemProperty;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.io.CharStreams;
import com.google.common.primitives.Ints;
import org.immutables.common.marshal.Marshaling;
import org.immutables.value.Json;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.glowroot.collector.GaugePoint;
import org.glowroot.config.ConfigService;
import org.glowroot.config.GaugeConfig;
import org.glowroot.jvm.GcEvents;
import org.glowroot.jvm.HeapDumps;
import org.glowroot.jvm.ImmutableAvailability;
import org.glowroot.jvm.LazyPlatformMBeanServer;
import org.glowroot.jvm.OptionalService;
import org.glowroot.jvm.OptionalService.Availability;
import org.glowroot.jvm.ThreadAllocatedBytes;
import org.glowroot.local.store.GaugePointDao;
import org.glowroot.local.store.GcEventDao;
import org.glowroot.markers.UsedByJsonBinding;

import static com.google.common.base.Preconditions.checkNotNull;

@JsonService
class JvmJsonService {

    private static final Logger logger = LoggerFactory.getLogger(JvmJsonService.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final Ordering<ThreadInfo> threadInfoOrdering =
            new Ordering<ThreadInfo>() {
                @Override
                public int compare(@Nullable ThreadInfo left, @Nullable ThreadInfo right) {
                    checkNotNull(left);
                    checkNotNull(right);
                    if (left.getThreadId() == Thread.currentThread().getId()) {
                        return 1;
                    } else if (right.getThreadId() == Thread.currentThread().getId()) {
                        return -1;
                    }
                    int result =
                            Ints.compare(right.getStackTrace().length, left.getStackTrace().length);
                    if (result == 0) {
                        return left.getThreadName().compareToIgnoreCase(right.getThreadName());
                    }
                    return result;
                }
            };

    private final LazyPlatformMBeanServer lazyPlatformMBeanServer;
    private final GaugePointDao gaugePointDao;
    private final GcEventDao gcEventDao;
    private final ConfigService configService;

    private final OptionalService<ThreadAllocatedBytes> threadAllocatedBytes;
    private final OptionalService<HeapDumps> heapDumps;
    private final OptionalService<GcEvents> gcEvents;
    private final @Nullable String processId;

    private final long fixedGaugeIntervalMillis;
    private final long fixedGaugeRollupMillis;

    JvmJsonService(LazyPlatformMBeanServer lazyPlatformMBeanServer, GaugePointDao gaugePointDao,
            GcEventDao gcEventDao, ConfigService configService,
            OptionalService<ThreadAllocatedBytes> threadAllocatedBytes,
            OptionalService<HeapDumps> heapDumps, OptionalService<GcEvents> gcEvents,
            @Nullable String processId, long fixedGaugeIntervalSeconds,
            long fixedGaugeRollupSeconds) {
        this.lazyPlatformMBeanServer = lazyPlatformMBeanServer;
        this.gaugePointDao = gaugePointDao;
        this.gcEventDao = gcEventDao;
        this.configService = configService;
        this.threadAllocatedBytes = threadAllocatedBytes;
        this.heapDumps = heapDumps;
        this.gcEvents = gcEvents;
        this.processId = processId;
        this.fixedGaugeIntervalMillis = fixedGaugeIntervalSeconds * 1000;
        this.fixedGaugeRollupMillis = fixedGaugeRollupSeconds * 1000;
    }

    @GET("/backend/jvm/process-info")
    String getProcess() throws Exception {
        String command = System.getProperty("sun.java.command");
        String mainClass = null;
        List<String> arguments = ImmutableList.of();
        if (command != null) {
            int index = command.indexOf(' ');
            if (index > 0) {
                mainClass = command.substring(0, index);
                arguments = Lists.newArrayList(
                        Splitter.on(' ').split(command.substring(index + 1)));
            }
        }
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        String jvm = StandardSystemProperty.JAVA_VM_NAME.value() + " ("
                + StandardSystemProperty.JAVA_VM_VERSION.value() + ", "
                + System.getProperty("java.vm.info") + ")";
        String java = "version " + StandardSystemProperty.JAVA_VERSION.value() + ", vendor "
                + StandardSystemProperty.JAVA_VM_VENDOR.value();
        String javaHome = StandardSystemProperty.JAVA_HOME.value();

        StringBuilder sb = new StringBuilder();
        JsonGenerator jg = mapper.getFactory().createGenerator(CharStreams.asWriter(sb));
        jg.writeStartObject();
        jg.writeNumberField("startTime", runtimeMXBean.getStartTime());
        jg.writeNumberField("uptime", runtimeMXBean.getUptime());
        jg.writeStringField("pid", MoreObjects.firstNonNull(processId, "<unknown>"));
        jg.writeStringField("mainClass", mainClass);
        jg.writeFieldName("mainClassArguments");
        mapper.writeValue(jg, arguments);
        jg.writeStringField("jvm", jvm);
        jg.writeStringField("java", java);
        jg.writeStringField("javaHome", javaHome);
        jg.writeFieldName("jvmArguments");
        mapper.writeValue(jg, runtimeMXBean.getInputArguments());
        jg.writeEndObject();
        jg.close();
        return sb.toString();
    }

    @GET("/backend/jvm/gauge-points")
    String getGaugePoints(String queryString) throws Exception {
        GaugePointRequest request = QueryStrings.decode(queryString, GaugePointRequest.class);
        double gapMillis;
        if (request.rollupLevel() == 0) {
            gapMillis = fixedGaugeIntervalMillis * 1.5;
        } else {
            gapMillis = fixedGaugeRollupMillis * 1.5;
        }
        List<List<Number /*@Nullable*/[]>> series = Lists.newArrayList();
        for (String gaugeName : request.gaugeNames()) {
            ImmutableList<GaugePoint> gaugePoints = gaugePointDao.readGaugePoints(gaugeName,
                    request.from(), request.to(), request.rollupLevel());
            series.add(convertToDataSeriesWithGaps(gaugePoints, gapMillis));
        }
        return mapper.writeValueAsString(series);
    }

    @GET("/backend/jvm/all-gauge-names")
    String getAllGaugeNames() throws IOException {
        List<String> gaugeNames = Lists.newArrayList();
        for (GaugeConfig gaugeConfig : configService.getGaugeConfigs()) {
            for (String mbeanAttributeName : gaugeConfig.mbeanAttributeNames()) {
                gaugeNames.add(gaugeConfig.name() + "/" + mbeanAttributeName);
            }
        }
        ImmutableList<String> sortedGaugeNames =
                Ordering.from(String.CASE_INSENSITIVE_ORDER).immutableSortedCopy(gaugeNames);
        return mapper.writeValueAsString(sortedGaugeNames);
    }

    @GET("/backend/jvm/system-properties")
    String getSystemProperties() throws IOException {
        Properties properties = System.getProperties();
        // can't use Maps.newTreeMap() because of OpenJDK6 type inference bug
        // see https://code.google.com/p/guava-libraries/issues/detail?id=635
        Map<String, String> sortedProperties =
                new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        for (Enumeration<?> e = properties.propertyNames(); e.hasMoreElements();) {
            Object obj = e.nextElement();
            if (obj instanceof String) {
                String propertyName = (String) obj;
                String propertyValue = properties.getProperty(propertyName);
                if (propertyValue != null) {
                    sortedProperties.put(propertyName, propertyValue);
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        JsonGenerator jg = mapper.getFactory().createGenerator(CharStreams.asWriter(sb));
        jg.writeStartArray();
        for (Entry<String, String> entry : sortedProperties.entrySet()) {
            jg.writeStartObject();
            jg.writeStringField("name", entry.getKey());
            jg.writeStringField("value", entry.getValue());
            jg.writeEndObject();
        }
        jg.writeEndArray();
        jg.close();
        return sb.toString();
    }

    @GET("/backend/jvm/mbean-tree")
    String getMBeanTree(String queryString) throws Exception {
        MBeanTreeRequest request = QueryStrings.decode(queryString, MBeanTreeRequest.class);
        Set<ObjectName> objectNames = lazyPlatformMBeanServer.queryNames(null, null);
        // can't use Maps.newTreeMap() because of OpenJDK6 type inference bug
        // see https://code.google.com/p/guava-libraries/issues/detail?id=635
        Map<String, MBeanTreeInnerNode> sortedRootNodes =
                new TreeMap<String, MBeanTreeInnerNode>(String.CASE_INSENSITIVE_ORDER);
        for (ObjectName objectName : objectNames) {
            String domain = objectName.getDomain();
            MBeanTreeInnerNode node = sortedRootNodes.get(domain);
            if (node == null) {
                node = new MBeanTreeInnerNode(domain);
                sortedRootNodes.put(domain, node);
            }
            List<String> propertyValues = ObjectNames.getPropertyValues(objectName);
            for (int i = 0; i < propertyValues.size() - 1; i++) {
                node = node.getOrCreateNode(propertyValues.get(i));
            }
            String name = objectName.toString();
            String value = propertyValues.get(propertyValues.size() - 1);
            if (request.expanded().contains(name)) {
                Map<String, /*@Nullable*/Object> sortedAttributeMap =
                        getMBeanSortedAttributeMap(objectName);
                node.addLeafNode(
                        new MBeanTreeLeafNode(value, name, true, sortedAttributeMap));
            } else {
                node.addLeafNode(new MBeanTreeLeafNode(value, name, false, null));
            }
        }
        return mapper.writeValueAsString(sortedRootNodes);
    }

    @GET("/backend/jvm/mbean-attribute-map")
    String getMBeanAttributeMap(String queryString) throws Exception {
        MBeanAttributeMapRequest request =
                QueryStrings.decode(queryString, MBeanAttributeMapRequest.class);
        ObjectName objectName = ObjectName.getInstance(request.objectName());
        Map<String, /*@Nullable*/Object> attributeMap = getMBeanSortedAttributeMap(objectName);
        return mapper.writeValueAsString(attributeMap);
    }

    @POST("/backend/jvm/perform-gc")
    void performGC() throws IOException {
        // using MemoryMXBean.gc() instead of System.gc() in hope that it will someday bypass
        // -XX:+DisableExplicitGC (see https://bugs.openjdk.java.net/browse/JDK-6396411)
        ManagementFactory.getMemoryMXBean().gc();
    }

    @GET("/backend/jvm/thread-dump")
    String getThreadDump() throws IOException {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos =
                threadBean.getThreadInfo(threadBean.getAllThreadIds(), Integer.MAX_VALUE);
        List<ThreadInfo> sortedThreadInfos =
                threadInfoOrdering.immutableSortedCopy(Arrays.asList(threadInfos));
        StringBuilder sb = new StringBuilder();
        JsonGenerator jg = mapper.getFactory().createGenerator(CharStreams.asWriter(sb));
        jg.writeStartArray();
        long currentThreadId = Thread.currentThread().getId();
        for (ThreadInfo threadInfo : sortedThreadInfos) {
            jg.writeStartObject();
            jg.writeStringField("name", threadInfo.getThreadName());
            jg.writeStringField("state", threadInfo.getThreadState().name());
            jg.writeStringField("lockName", threadInfo.getLockName());
            jg.writeArrayFieldStart("stackTrace");
            boolean trimCurrentThreadStack = threadInfo.getThreadId() == currentThreadId;
            for (StackTraceElement stackTraceElement : threadInfo.getStackTrace()) {
                if (trimCurrentThreadStack && !stackTraceElement.getClassName().equals(
                        JvmJsonService.class.getName())) {
                    // just cleaning current thread's stack trace a bit to make it more obvious
                    // that it is just the current thread
                    continue;
                }
                trimCurrentThreadStack = false;
                jg.writeString(stackTraceElement.toString());
            }
            jg.writeEndArray();
            jg.writeEndObject();
        }
        jg.writeEndArray();
        jg.close();
        return sb.toString();
    }

    @GET("/backend/jvm/heap-dump-defaults")
    String getHeapDumpDefaults() throws Exception {
        String heapDumpPath = getHeapDumpPathFromCommandLine();
        if (Strings.isNullOrEmpty(heapDumpPath)) {
            String javaTempDir = MoreObjects.firstNonNull(
                    StandardSystemProperty.JAVA_IO_TMPDIR.value(), ".");
            heapDumpPath = new File(javaTempDir).getAbsolutePath();
        }
        StringBuilder sb = new StringBuilder();
        JsonGenerator jg = mapper.getFactory().createGenerator(CharStreams.asWriter(sb));
        jg.writeStartObject();
        jg.writeStringField("directory", heapDumpPath);
        jg.writeEndObject();
        jg.close();
        return sb.toString();
    }

    @POST("/backend/jvm/check-disk-space")
    String checkDiskSpace(String content) throws IOException {
        RequestWithDirectory request = Marshaling.fromJson(content, RequestWithDirectory.class);
        File dir = new File(request.directory());
        if (!dir.exists()) {
            return "{\"error\": \"Directory doesn't exist\"}";
        }
        if (!dir.isDirectory()) {
            return "{\"error\": \"Path is not a directory\"}";
        }
        long diskSpace = new File(request.directory()).getFreeSpace();
        return Long.toString(diskSpace);
    }

    @POST("/backend/jvm/dump-heap")
    String dumpHeap(String content) throws Exception {
        // this command is filtered out of the UI when service is null
        HeapDumps service = checkNotNull(heapDumps.getService(),
                "Heap dump service is not available: %s", heapDumps.getAvailability().getReason());
        RequestWithDirectory request = Marshaling.fromJson(content, RequestWithDirectory.class);
        File dir = new File(request.directory());
        if (!dir.exists()) {
            return "{\"error\": \"Directory doesn't exist\"}";
        }
        if (!dir.isDirectory()) {
            return "{\"error\": \"Path is not a directory\"}";
        }
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        File file = new File(dir, "heap-dump-" + timestamp + ".hprof");
        int i = 1;
        while (file.exists()) {
            // this seems unlikely now that timestamp is included in filename
            i++;
            file = new File(dir, "heap-dump-" + timestamp + "-" + i + ".hprof");
        }
        service.dumpHeap(file.getAbsolutePath());

        StringBuilder sb = new StringBuilder();
        JsonGenerator jg = mapper.getFactory().createGenerator(CharStreams.asWriter(sb));
        jg.writeStartObject();
        jg.writeStringField("filename", file.getAbsolutePath());
        jg.writeNumberField("size", file.length());
        jg.writeEndObject();
        jg.close();
        return sb.toString();
    }

    @GET("/backend/jvm/capabilities")
    String getCapabilities() throws IOException {
        StringBuilder sb = new StringBuilder();
        JsonGenerator jg = mapper.getFactory().createGenerator(CharStreams.asWriter(sb));
        jg.writeStartObject();
        jg.writeFieldName("threadCpuTime");
        mapper.writeValue(jg, getThreadCpuTimeAvailability());
        jg.writeFieldName("threadContentionTime");
        mapper.writeValue(jg, getThreadContentionAvailability());
        jg.writeFieldName("threadAllocatedBytes");
        mapper.writeValue(jg, threadAllocatedBytes.getAvailability());
        jg.writeFieldName("heapDump");
        mapper.writeValue(jg, heapDumps.getAvailability());
        jg.writeFieldName("gcEvents");
        mapper.writeValue(jg, gcEvents.getAvailability());
        jg.writeEndObject();
        jg.close();
        return sb.toString();
    }

    private static List<Number /*@Nullable*/[]> convertToDataSeriesWithGaps(
            ImmutableList<GaugePoint> gaugePoints, double gapMillis) {
        List<Number /*@Nullable*/[]> points = Lists.newArrayList();
        GaugePoint lastGaugePoint = null;
        for (GaugePoint gaugePoint : gaugePoints) {
            if (lastGaugePoint != null
                    && gaugePoint.captureTime() - lastGaugePoint.captureTime() > gapMillis) {
                points.add(null);
            }
            points.add(new Number[] {gaugePoint.captureTime(), gaugePoint.value()});
            lastGaugePoint = gaugePoint;
        }
        return points;
    }

    private static Availability getThreadCpuTimeAvailability() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        if (!threadMXBean.isThreadCpuTimeSupported()) {
            return ImmutableAvailability.of(false, "java.lang.management.ThreadMXBean"
                    + ".isThreadCpuTimeSupported() returned false");
        }
        if (!threadMXBean.isThreadCpuTimeEnabled()) {
            return ImmutableAvailability.of(false, "java.lang.management.ThreadMXBean"
                    + ".isThreadCpuTimeEnabled() returned false");
        }
        return ImmutableAvailability.of(true, "");
    }

    private static Availability getThreadContentionAvailability() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        if (!threadMXBean.isThreadContentionMonitoringSupported()) {
            return ImmutableAvailability.of(false, "java.lang.management.ThreadMXBean"
                    + ".isThreadContentionMonitoringSupported() returned false");
        }
        if (!threadMXBean.isThreadContentionMonitoringEnabled()) {
            return ImmutableAvailability.of(false, "java.lang.management.ThreadMXBean"
                    + ".isThreadContentionMonitoringEnabled() returned false");
        }
        return ImmutableAvailability.of(true, "");
    }

    private @Nullable static String getHeapDumpPathFromCommandLine() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        for (String arg : runtimeMXBean.getInputArguments()) {
            if (arg.startsWith("-XX:HeapDumpPath=")) {
                return arg.substring("-XX:HeapDumpPath=".length());
            }
        }
        return null;
    }

    private Map<String, /*@Nullable*/Object> getMBeanSortedAttributeMap(ObjectName objectName)
            throws Exception {
        MBeanInfo mBeanInfo = lazyPlatformMBeanServer.getMBeanInfo(objectName);
        // can't use Maps.newTreeMap() because of OpenJDK6 type inference bug
        // see https://code.google.com/p/guava-libraries/issues/detail?id=635
        Map<String, /*@Nullable*/Object> sortedAttributeMap =
                new TreeMap<String, /*@Nullable*/Object>(String.CASE_INSENSITIVE_ORDER);
        for (MBeanAttributeInfo attribute : mBeanInfo.getAttributes()) {
            Object value;
            try {
                value = lazyPlatformMBeanServer.getAttribute(objectName, attribute.getName());
            } catch (Exception e) {
                // log exception at debug level
                logger.debug(e.getMessage(), e);
                Throwable rootCause = getRootCause(e);
                value = "<Unavailable due to " + rootCause.getClass().getName() + ": "
                        + rootCause.getMessage() + ">";
            }
            sortedAttributeMap.put(attribute.getName(), getMBeanAttributeValue(value));
        }
        return sortedAttributeMap;
    }

    private Throwable getRootCause(Throwable t) {
        Throwable cause = t.getCause();
        if (cause == null) {
            return t;
        } else {
            return getRootCause(cause);
        }
    }

    // see list of allowed attribute value types:
    // http://docs.oracle.com/javase/7/docs/api/javax/management/openmbean/OpenType.html
    // #ALLOWED_CLASSNAMES_LIST
    private @Nullable Object getMBeanAttributeValue(@Nullable Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof CompositeData) {
            return getCompositeDataValue((CompositeData) value);
        } else if (value instanceof TabularData) {
            return getTabularDataValue((TabularData) value);
        } else if (value.getClass().isArray()) {
            return getArrayValue(value);
        } else if (value instanceof Number) {
            return value;
        } else {
            return value.toString();
        }
    }

    private Object getCompositeDataValue(CompositeData compositeData) {
        // linked hash map used to preserve attribute ordering
        Map<String, /*@Nullable*/Object> valueMap = Maps.newLinkedHashMap();
        for (String key : compositeData.getCompositeType().keySet()) {
            valueMap.put(key, getMBeanAttributeValue(compositeData.get(key)));
        }
        return valueMap;
    }

    private Object getTabularDataValue(TabularData tabularData) {
        // linked hash map used to preserve row ordering
        Map<String, Map<String, /*@Nullable*/Object>> rowMap = Maps.newLinkedHashMap();
        Set<String> attributeNames = tabularData.getTabularType().getRowType().keySet();
        for (Object key : tabularData.keySet()) {
            // TabularData.keySet() returns "Set<List<?>> but is declared Set<?> for
            // compatibility reasons" (see javadocs) so safe to cast to List<?>
            List<?> keyList = (List<?>) key;
            String keyString = Joiner.on(", ").join(keyList);
            CompositeData compositeData = tabularData.get(keyList.toArray());
            // linked hash map used to preserve attribute ordering
            Map<String, /*@Nullable*/Object> valueMap = Maps.newLinkedHashMap();
            for (String attributeName : attributeNames) {
                valueMap.put(attributeName,
                        getMBeanAttributeValue(compositeData.get(attributeName)));
            }
            rowMap.put(keyString, valueMap);
        }
        return rowMap;
    }

    private Object getArrayValue(Object value) {
        int length = Array.getLength(value);
        List</*@Nullable*/Object> valueList = Lists.newArrayListWithCapacity(length);
        for (int i = 0; i < length; i++) {
            Object val = Array.get(value, i);
            valueList.add(getMBeanAttributeValue(val));
        }
        return valueList;
    }

    private interface MBeanTreeNode {

        static final Ordering<MBeanTreeNode> ordering = new Ordering<MBeanTreeNode>() {
            @Override
            public int compare(@Nullable MBeanTreeNode left, @Nullable MBeanTreeNode right) {
                checkNotNull(left);
                checkNotNull(right);
                return left.getNodeName().compareToIgnoreCase(right.getNodeName());
            }
        };

        String getNodeName();
    }

    @UsedByJsonBinding
    static class MBeanTreeInnerNode implements MBeanTreeNode {

        private final String name;

        // not using Map here since its possible for multiple leafs with same name
        // e.g. d:type=Foo,name=Bar and d:type=Foo,nonsense=Bar
        // both translate to a leaf named Bar under d/Foo
        private final List<MBeanTreeNode> childNodes = Lists.newArrayList();

        private final Map<String, MBeanTreeInnerNode> innerNodes = Maps.newHashMap();

        private MBeanTreeInnerNode(String name) {
            this.name = name;
        }

        @Override
        public String getNodeName() {
            return name;
        }

        public List<MBeanTreeNode> getChildNodes() {
            return MBeanTreeNode.ordering.sortedCopy(childNodes);
        }

        private MBeanTreeInnerNode getOrCreateNode(String name) {
            MBeanTreeInnerNode innerNode = innerNodes.get(name);
            if (innerNode == null) {
                innerNode = new MBeanTreeInnerNode(name);
                innerNodes.put(name, innerNode);
                childNodes.add(innerNode);
            }
            return innerNode;
        }

        private void addLeafNode(MBeanTreeLeafNode leafNode) {
            childNodes.add(leafNode);
        }
    }

    @UsedByJsonBinding
    static class MBeanTreeLeafNode implements MBeanTreeNode {

        // nodeName may not be unique
        private final String nodeName;
        private final String objectName;
        private final boolean expanded;
        private final @Nullable Map<String, /*@Nullable*/Object> attributeMap;

        private MBeanTreeLeafNode(String nodeName, String objectName, boolean expanded,
                @Nullable Map<String, /*@Nullable*/Object> attributeMap) {
            this.nodeName = nodeName;
            this.objectName = objectName;
            this.expanded = expanded;
            this.attributeMap = attributeMap;
        }

        @Override
        public String getNodeName() {
            return nodeName;
        }

        public String getObjectName() {
            return objectName;
        }

        public boolean isExpanded() {
            return expanded;
        }

        public @Nullable Map<String, /*@Nullable*/Object> getAttributeMap() {
            return attributeMap;
        }
    }

    @Value.Immutable
    @Json.Marshaled
    abstract static class GaugePointRequest {
        abstract long from();
        abstract long to();
        abstract List<String> gaugeNames();
        abstract int rollupLevel();
    }

    @Value.Immutable
    @Json.Marshaled
    abstract static class RequestWithDirectory {
        abstract String directory();
    }

    @Value.Immutable
    @Json.Marshaled
    abstract static class MBeanTreeRequest {
        abstract public List<String> expanded();
    }

    @Value.Immutable
    @Json.Marshaled
    abstract static class MBeanAttributeMapRequest {
        abstract String objectName();
    }
}
