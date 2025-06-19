import com.google.common.collect.Maps;
import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class UpdateIndicatorsMapperTest {

    @Test
    public void testUpdateIndicatorsMapper() {
        // Given
        GatlingReporter gatlingReporter = new GatlingReporter();
        String gatlingConf="#########################\n" +
                "# Gatling Configuration #\n" +
                "#########################\n" +
                "\n" +
                "# This file contains all the settings configurable for Gatling with their default values\n" +
                "\n" +
                "gatling {\n" +
                "  core {\n" +
                "    #encoding = \"utf-8\"                      # Encoding to use throughout Gatling for file and string manipulation\n" +
                "    #elFileBodiesCacheMaxCapacity = 200      # Cache size for request body EL templates, set to 0 to disable\n" +
                "    #rawFileBodiesCacheMaxCapacity = 200     # Cache size for request body raw files, set to 0 to disable\n" +
                "    #rawFileBodiesInMemoryMaxSize = 10240    # Max bite size of raw files to be cached in memory\n" +
                "    #pebbleFileBodiesCacheMaxCapacity = 200  # Cache size for request body Pebble templates, set to 0 to disable\n" +
                "    #feederAdaptiveLoadModeThreshold = 100   # File size threshold (in MB). Below load eagerly in memory, above use batch mode with default buffer size\n" +
                "    #shutdownTimeout = 10000                 # Milliseconds to wait for the actor system to shutdown\n" +
                "    extract {\n" +
                "      regex {\n" +
                "        #cacheMaxCapacity = 200              # Cache size for the compiled regexes, set to 0 to disable caching\n" +
                "      }\n" +
                "      xpath {\n" +
                "        #cacheMaxCapacity = 200              # Cache size for the compiled XPath queries,  set to 0 to disable caching\n" +
                "      }\n" +
                "      jsonPath {\n" +
                "        #cacheMaxCapacity = 200              # Cache size for the compiled jsonPath queries, set to 0 to disable caching\n" +
                "      }\n" +
                "      css {\n" +
                "        #cacheMaxCapacity = 200              # Cache size for the compiled CSS selectors queries,  set to 0 to disable caching\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "  socket {\n" +
                "    #connectTimeout = 10000                  # Timeout in millis for establishing a TCP socket\n" +
                "    #tcpNoDelay = true\n" +
                "    #soKeepAlive = false                     # if TCP keepalive configured at OS level should be used\n" +
                "    #soReuseAddress = false\n" +
                "  }\n" +
                "  netty {\n" +
                "    #useNativeTransport = true               # if Netty Linux native transport should be used instead of Java NIO\n" +
                "    #useIoUring = false                      # if io_uring should be used instead of epoll if available\n" +
                "    #allocator = \"pooled\"                    # switch to unpooled for unpooled ByteBufAllocator\n" +
                "    #maxThreadLocalCharBufferSize = 200000   # Netty's default is 16k\n" +
                "  }\n" +
                "  ssl {\n" +
                "    #useOpenSsl = true                       # if OpenSSL should be used instead of JSSE (only the latter can be debugged with -Djavax.net.debug=ssl)\n" +
                "    #useOpenSslFinalizers = false            # if OpenSSL contexts should be freed with Finalizer or if using RefCounted is fine\n" +
                "    #handshakeTimeout = 10000                # TLS handshake timeout in millis\n" +
                "    #useInsecureTrustManager = true          # Use an insecure TrustManager that trusts all server certificates\n" +
                "    #enabledProtocols = []                   # Array of enabled protocols for HTTPS, if empty use Netty's defaults\n" +
                "    #enabledCipherSuites = []                # Array of enabled cipher suites for HTTPS, if empty enable all available ciphers\n" +
                "    #sessionCacheSize = 0                    # SSLSession cache size, set to 0 to use JDK's default\n" +
                "    #sessionTimeout = 0                      # SSLSession timeout in seconds, set to 0 to use JDK's default (24h)\n" +
                "    #enableSni = true                        # When set to true, enable Server Name indication (SNI)\n" +
                "    keyStore {\n" +
                "      #type = \"\"                             # Type of SSLContext's KeyManagers store, possible values are jks and p12\n" +
                "      #file = \"\"                             # Location of SSLContext's KeyManagers store\n" +
                "      #password = \"\"                         # Password for SSLContext's KeyManagers store\n" +
                "      #algorithm = \"\"                        # Algorithm used SSLContext's KeyManagers store, typically RSA\n" +
                "    }\n" +
                "    trustStore {\n" +
                "      #type = \"\"                             # Type of SSLContext's TrustManagers store, possible values are jks and p12\n" +
                "      #file = \"\"                             # Location of SSLContext's TrustManagers store\n" +
                "      #password = \"\"                         # Password for SSLContext's TrustManagers store\n" +
                "      #algorithm = \"\"                        # Algorithm used by SSLContext's TrustManagers store, typically RSA\n" +
                "    }\n" +
                "  }\n" +
                "  charting {\n" +
                "    #maxPlotPerSeries = 1000                 # Number of points per graph in Gatling reports\n" +
                "    #useGroupDurationMetric = false          # Switch group timings from cumulated response time to group duration.\n" +
                "    indicators {\n" +
                "      lowerBound = 900                      # Lower bound for the requests' response time to track in the reports and the console summary\n" +
                "      higherBound = 1600                    # Higher bound for the requests' response time to track in the reports and the console summary\n" +
                "      percentile1 = 56                      # Value for the 1st percentile to track in the reports, the console summary and Graphite\n" +
                "      percentile2 = 77                      # Value for the 2nd percentile to track in the reports, the console summary and Graphite\n" +
                "      percentile3 = 96                      # Value for the 3rd percentile to track in the reports, the console summary and Graphite\n" +
                "      percentile4 = 98                      # Value for the 4th percentile to track in the reports, the console summary and Graphite\n" +
                "    }\n" +
                "  }\n" +
                "  http {\n" +
                "    #fetchedCssCacheMaxCapacity = 200        # Cache size for CSS parsed content, set to 0 to disable\n" +
                "    #fetchedHtmlCacheMaxCapacity = 200       # Cache size for HTML parsed content, set to 0 to disable\n" +
                "    #perUserCacheMaxCapacity = 200           # Per virtual user cache size, set to 0 to disable\n" +
                "    #warmUpUrl = \"https://gatling.io\"        # The URL to use to warm-up the HTTP stack (blank means disabled)\n" +
                "    #pooledConnectionIdleTimeout = 60000     # Timeout in millis for a connection to stay idle in the pool\n" +
                "    #requestTimeout = 60000                  # Timeout in millis for performing an HTTP request\n" +
                "    #enableHostnameVerification = false      # When set to true, enable hostname verification: SSLEngine.setHttpsEndpointIdentificationAlgorithm(\"HTTPS\")\n" +
                "    dns {\n" +
                "      #queryTimeout = 5000                   # Timeout in millis of each DNS query in millis\n" +
                "      #maxQueriesPerResolve = 6              # Maximum allowed number of DNS queries for a given name resolution\n" +
                "    }\n" +
                "  }\n" +
                "  jms {\n" +
                "    #replyTimeoutScanPeriod = 1000           # scan period for timed out reply messages\n" +
                "  }\n" +
                "  data {\n" +
                "    #writers = [console, file]               # The list of DataWriters to which Gatling write simulation data (currently supported : console, file, graphite)\n" +
                "    #utcDateTime = true                      # Print date-times with the UTC zone instead of the System's default\n" +
                "    console {\n" +
                "      #light = false                         # When set to true, displays a light version without detailed request stats\n" +
                "      #writePeriod = 5                       # Write interval, in seconds\n" +
                "    }\n" +
                "    file {\n" +
                "      #bufferSize = 8192                     # FileDataWriter's internal data buffer size, in bytes\n" +
                "    }\n" +
                "    leak {\n" +
                "      #noActivityTimeout = 30                # Period, in seconds, for which Gatling may have no activity before considering a leak may be happening\n" +
                "    }\n" +
                "    graphite {\n" +
                "      #light = false                         # only send the all* stats\n" +
                "      #host = \"localhost\"                    # The host where the Carbon server is located\n" +
                "      #port = 2003                           # The port to which the Carbon server listens to (2003 is default for plaintext, 2004 is default for pickle)\n" +
                "      #protocol = \"tcp\"                      # The protocol used to send data to Carbon (currently supported : \"tcp\", \"udp\")\n" +
                "      #rootPathPrefix = \"gatling\"            # The common prefix of all metrics sent to Graphite\n" +
                "      #bufferSize = 8192                     # Internal data buffer size, in bytes\n" +
                "      #writePeriod = 1                       # Write period, in seconds\n" +
                "    }\n" +
                "    #enableAnalytics = true                  # Anonymous Usage Analytics (no tracking), please support\n" +
                "  }\n" +
                "}\n";

        Map<String, String> defaultIndicatorsMapper = Maps.newHashMap();
        defaultIndicatorsMapper.put("percentiles1", "p50");
        defaultIndicatorsMapper.put("percentiles2", "p75");
        defaultIndicatorsMapper.put("percentiles3", "p95");
        defaultIndicatorsMapper.put("percentiles4", "p99");
        defaultIndicatorsMapper.put("lowerBound", "lower_bound_800");
        defaultIndicatorsMapper.put("higherBound", "higher_bound_1200");
        // When
        Map<String, String> updatedIndicatorsMapper = gatlingReporter.updateIndicatorsMapper(defaultIndicatorsMapper, gatlingConf);
        Map<String, String> expectedIndicatorsMapper = Maps.newHashMap();
        expectedIndicatorsMapper.put("percentiles1", "p56");
        expectedIndicatorsMapper.put("percentiles2", "p77");
        expectedIndicatorsMapper.put("percentiles3", "p96");
        expectedIndicatorsMapper.put("percentiles4", "p98");
        expectedIndicatorsMapper.put("lowerBound", "lower_bound_900");
        expectedIndicatorsMapper.put("higherBound", "higher_bound_1600");

        // Then
        assertThat(updatedIndicatorsMapper).containsAllEntriesOf(expectedIndicatorsMapper);
    }
}
