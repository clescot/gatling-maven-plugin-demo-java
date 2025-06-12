Gatling Reporter project
============================================

# goal of this project

expose gatling metrics from tests (aka simulation), to prometheus.

# prerequisites

a push gateway to publish computed metrics by gatling.

# test the gatling reporter


## launch test infrastructure
launch with docker compose prometheus + prometheus push gateway + grafana with this command (from the directory of the project)

`docker compose up -d `


## bound gatling maven plugin to a maven phase

When the gatling-maven-plugin is executed, it needs to be bound to a maven phase (usually `integration-phase`)
you can do that in your pom.xml maven project file with this snippet : 
```xml
 <plugin>
        <groupId>io.gatling</groupId>
        <artifactId>gatling-maven-plugin</artifactId>
        <version>${gatling-maven-plugin.version}</version>
        <configuration>
        </configuration>
        <executions>
          <execution>
            <id>integration-test</id>
            <goals>
              <goal>test</goal>
            </goals>
            <phase>integration-test</phase>
          </execution>
        </executions>
      </plugin>
```

you can launch the test with ` mvn integration-test -Dgatling.simulationClass=example.AdvancedSimulation` (to test your application with the AdvancedSimulation).

## configure GatlingReporter after the previous phase in your pom.xml

The gatling reporter dependency needs to be imported :
```xml

 <dependency>
      <groupId>io.gatling.demo</groupId>
      <artifactId>gatling-reporter</artifactId>
      <version>1.0.0</version>
    </dependency>
```


```xml
 <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>exec-maven-plugin</artifactId>
        <version>3.4.1</version>
      <executions>
        <execution>
          <id>default-cli</id>
          <phase>post-integration-test</phase>
          <goals>
            <goal>java</goal>
          </goals>
        </execution>
      </executions>
      <configuration>
        <mainClass>GatlingReporter</mainClass>
      </configuration>
      </plugin>
```
The gatling reporter will be launched automatically after the gatling simulation.
the gatling simulation reports are located in the `target/gatling/<simulation-name-YYYYMMDDHHmmssSSS>`
The gatling-reporter analyze in the more recent directory, the `js/stats.js` file. 
It parse it, and publish some metrics to the prometheus push gateway, which will be parsed by prometheus.

at the end of the test, you can shutdown docker containers used with :

`docker compose down`