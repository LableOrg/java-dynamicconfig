Dynamic Config
==============

In short, a configuration library designed as a provider of [Apache Commons Configuration](https://commons.apache.org/proper/commons-configuration/index.html) instances that can be updated at runtime.

## Goals

Dynamic Config is designed to provide a dynamic configuration resource to multiple Java web applications running on multiple webservers from a central source. Although the core library is implementation agnostic, this project was designed to work with [https://zookeeper.apache.org/](Apache ZooKeeper).

We created this library to:

* provide a simple way to manage runtime configuration parameters for Java web applications deployed on several Tomcat-servers from a single place
* be able to update runtime configuration without the need to redeploy WAR archives

To create and update configuration resources we use a [simple command line script](https://github.com/LableOrg/mrconfig) deployed on a server accessible via SSH, but any tool or service that can update ZooKeeper nodes should work.

## Distributed configuration with YAML configuration resources stored in ZooKeeper

To use the ZooKeeper provider with [YAML](http://yaml.org/) configuration resources, add the following Maven dependencies to your project. Set property `dynamic.config.version` to the current version of Dynamic Config:

```xml
<properties>
  <dynamic.config.version>VERSION_HERE</dynamic.config.version>
</properties>

<dependencies>
  <dependency>
    <groupId>org.lable.oss.dynamicconfig</groupId>
    <artifactId>servlet-util</artifactId>
    <version>${dynamic.config.version}</version>
  </dependency>
  <dependency>
    <groupId>org.lable.oss.dynamicconfig.provider</groupId>
    <artifactId>zookeeper</artifactId>
    <version>${dynamic.config.version}</version>
  </dependency>
  <dependency>
    <groupId>org.lable.oss.dynamicconfig.serialization</groupId>
    <artifactId>yaml</artifactId>
    <version>${dynamic.config.version}</version>
  </dependency>
</dependencies>
```

The simplest way to configure Dynamic Config when used from within a web application with the ZooKeeper provider, is to configure your webserver(s) **once** to provide these system properties to the web applications it serves:

| System Property | Example value |
|-----------------|---------------|
| org.lable.oss.dynamicconfig.zookeeper.znode | /dynamicconfig |
| org.lable.oss.dynamicconfig.type | zookeeper |
| org.lable.oss.dynamicconfig.zookeeper.quorum | zk1,zk2,zk3 |
| org.lable.oss.dynamicconfig.zookeeper.copy.quorum.to | zookeeper.quorum |

In addition to these properties, Dynamic Config needs to know the name of the specific configuration resource it should load. For a web applaction running in a servlet container such as Apache Tomcat, you can tell Dynamic Config to use the name of the current application context (which usually based on the name of the WAR archive).

Implement a `ServletContextListener`, register it in your `web.xml`, and call this [ServletUtil](/servlet-util/src/main/java/org/lable/oss/dynamicconfig/servletutil/ServletUtil.java) method:

```java
@Override
public void contextInitialized(ServletContextEvent event) {
    // [...]
    
    ServletUtil.setApplicationNameFromContext(event);

    // [...]
}

```

Now you can load the configuration instance by calling:

```java
Configuration configuration = ConfigurationInitializer.configureFromProperties(new YamlDeserializer());
```

When the configuration resource is updated in the ZooKeeper quorum, this configuration instance will also be updated.
In order to benefit from the updated configuration, consumers of the configuration instance should take care not to cache the values retrieved from it.

### Security

This approach assumes that all web applications on the webservers are managed by the same trusted group of people. Any web application that can reach your private ZooKeeper quorum can request any configuration source available there. If you have a ZooKeeper quorum running in your computing environment this is usually the case.
