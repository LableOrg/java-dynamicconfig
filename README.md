Dynamic Config
==============

Dynamic Config is a Java configuration library designed to provide 
[Apache Commons Configuration](https://commons.apache.org/proper/commons-configuration/index.html) 
instances that can be updated at runtime.

## Goals

This library is designed to provide a dynamic configuration resource suitable for use in multiple
concurrent Java (web) applications running on multiple servers, where the configuration is provided 
from a central source. Although the core library is implementation agnostic, this project 
was designed to work with [https://zookeeper.apache.org/](Apache ZooKeeper).

We created this library to:

* provide a simple way to manage runtime configuration parameters for Java web applications 
  deployed on several Tomcat-servers from a single place
* be able to update runtime configurations without the need to redeploy WAR archives

To create and update configuration resources we use a 
[simple command line script](https://github.com/LableOrg/mrconfig) deployed on a server accessible 
via SSH, but any tool or service that can update ZooKeeper nodes should work.

## Usage examples

A set of minimal working examples is 
[available here](https://github.com/jdhoek/java-dynamicconfig-examples).

## Bootstrapping Dynamic Config

Dynamic Config is designed to to be used in situations where multiple web applications are run on 
multiple Tomcat servers. In this distributed scenario Dynamic Config can be used with Apache 
ZooKeeper to provide the configuration sources. Alternatively, a local configuration file may be
used. This approach implies that you have only one machine running a single instance of your 
application, or that you take care of distributing the configuration files yourself (e.g., via SSH).

### Dynamic configuration with a YAML file on the local file system

If your (web) application runs on a single machine, configuration may be provided through a local 
[YAML](http://yaml.org/) file. Add these Maven dependencies:

```xml
<properties>
  <dynamic.config.version>VERSION_HERE</dynamic.config.version>
</properties>

<dependencies>
  <dependency>
    <!-- Useful i -->
    <groupId>org.lable.oss.dynamicconfig</groupId>
    <artifactId>dynamic-config-servlet-util</artifactId>
    <version>${dynamic.config.version}</version>
  </dependency>
  <dependency>
    <groupId>org.lable.oss.dynamicconfig</groupId>
    <artifactId>dynamic-config-core</artifactId>
    <version>${dynamic.config.version}</version>
  </dependency>
  <dependency>
    <groupId>org.lable.oss.dynamicconfig.serialization</groupId>
    <artifactId>dynamic-config-serialization-yaml</artifactId>
    <version>${dynamic.config.version}</version>
  </dependency>
</dependencies>
```

To configure Dynamic Config, have your web server(s) provide these system properties to the web applications it serves:

| System Property                                      | Example value         |
|------------------------------------------------------|-----------------------|
| org.lable.oss.dynamicconfig.type                     | file                  |
| org.lable.oss.dynamicconfig.appname                  | config-file-name.yaml |

Instead of statically configuring `org.lable.oss.dynamicconfig.appname`, you can also implement a 
`ServletContextListener` to use the name of the current web application context instead. This is documented below in 
the section on ZooKeeper.

### Distributed configuration with YAML configuration resources stored in ZooKeeper

To use the ZooKeeper provider with YAML configuration resources, add the following Maven dependencies to your project. 
Set property `dynamic.config.version` to the current version of Dynamic Config:

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

The simplest way to configure Dynamic Config when used from within a web application with the ZooKeeper provider, is to 
configure your web server(s) **once** to provide these system properties to the web applications it serves:

| System Property                                      | Example value      |
|------------------------------------------------------|--------------------|
| org.lable.oss.dynamicconfig.zookeeper.znode          | /dynamicconfig     |
| org.lable.oss.dynamicconfig.type                     | zookeeper          |
| org.lable.oss.dynamicconfig.zookeeper.quorum         | zk1,zk2,zk3        |
| org.lable.oss.dynamicconfig.zookeeper.copy.quorum.to | zookeeper.quorum   |

In addition to these properties, Dynamic Config needs to know the name of the specific configuration resource it 
should load. For a web application running in a servlet container such as Apache Tomcat, you can tell Dynamic Config 
to use the name of the current application context (which usually based on the name of the WAR archive).

Implement a `ServletContextListener`, register it in your `web.xml`, and call this 
[ServletUtil](/servlet-util/src/main/java/org/lable/oss/dynamicconfig/servletutil/ServletUtil.java) method:

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
In order to benefit from the updated configuration, consumers of the configuration instance should take care not to  
cache the values retrieved from it.

#### Security

This approach assumes that all web applications on the web servers are managed by the same trusted group of people. 
Any  web application that can reach your private ZooKeeper quorum can request any configuration source available 
there.  If you have a ZooKeeper quorum running in your computing environment this is usually the case.

## Organizing configuration files

This library currently supports only YAML as a configuration language (other languages that 
represent a tree of configuration parameters can be added by implementing 
`HierarchicalConfigurationDeserializer`). To provide a way to split up large configuration files,
a custom YAML tag is supported.

For example, if the main configuration file is `config/config.yaml`:

```yaml
extends:
  - defaults.yaml

settings:
  coffee: yes
  foo:
    number-of-foos: 5
    enabled: yes
  bar: !include bar/bar-settings.yaml
```

And these files (or nodes in the case of ZooKeeper) exist:

`defaults.yaml`:

```yaml
settings:
  tea: yes
  milk: no
  coffee: no
```

`bar/bar-settings.yaml`:

```yaml
number-of-bars: 3
enabled: yes
```

Then the resulting configuration tree looks like this:

```yaml
extends:
  - defaults.yaml

settings:
  tea: yes
  milk: no
  coffee: yes
  foo:
    number-of-foos: 5
    enabled: yes
  bar:
    number-of-bars: 3
    enabled: yes
```
