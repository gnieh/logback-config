logback-config
==============

[Typesafe config](https://github.com/typesafehub/config/) configurator for Logback.

Installation
------------

If you use maven, add the dependency to your `pom.xml` file:

```xml
<dependency>
  <groupId>org.gnieh</groupId>
  <artifactId>logback-config</artifactId>
  <version>0.4.0</version>
</dependency>
```


If you use sbt, add this dependency to your `build.sbt` file:

```scala
libraryDependencies += "org.gnieh" % "logback-config" % "0.4.0"
```

Typesafe configuration loading
------------------

The configurator first attempts to load the Typesafe configuration through the Java service-provider mechanism. It looks
for a service-provider for the `org.gnieh.logback.config.ConfigLoader` interface, and calls the first one that it find.
If none are found the Typesafe configuration is loaded by a call to ConfigFactory.load().

If the configuration value `scan-period` is assigned a duration value then all regular files (those directly contained in
the file system) encountered in the Typesafe configuration will be checked for changes at that interval, and the
configuration will be reloaded if any have been modified.

Configuration root
------------------

By default the logback configuration keys are all under the top-level `logback` key. You can override this by changing the value of the `logback-root` key.
For instance, let's say you have two different logging configurations, one for testing and one for production. In your `reference.conf` file you can have:

```scala
logback-root = production.logback

production {
  logback = ${logback} {
    // logging configruation, see below
  }
}
```

In your test configuration file you may then have:

```scala
logback-root = test.logback

test {
  logback = ${logback} {
    // logging configruation, see below
  }
}
```

Inheriting from the default `logback` configuration object brings a valid empty configuration, so that only required keys must be defined.
It is not necessary though, if your configuration defines all the required keys (`appenders`, `loggers`, and `root`, see below).

Format
------

The general configuration format is as follows:

```scala
logback {
  scan-period = 30 seconds
  
  properties {
    property1 = "value"
  }

  appenders {
    appender-name {
      // appender configuration
    }
    ...
  }

  loggers {
    logger-name {
      // logger configuration
    }
    ...
  }

  root {
    // logger configuration
  }

}
```

Where appender configuration looks like this:

```scala
{
  class = "my.configuration.Class" // mandatory

  // optional
  encoder {
    class = "my.encoder.Class"

    // optional
    layout {
      class = "my.layout.Class"
      // any other property with name convention and value conversions described below
    }

    // any other property with name convention and value conversions described below
  }

  // optional
  layout {
    class = "my.layout.Class"
    // any other property with name convention and value conversions described below
  }

  // optional
  filter {
    class = "my.filter.Class"
    // any other property with name convention and value conversions described below
  }

  optional
  filters = [
    {
      class = "my.filter.Class"
      // any other property with name convention and value conversions described below
    },
    ...
  ]

  // any other property with name convention and value conversions described below

}
```

Encoder configuration looks like this:

```scala
{

  // optional
  level = DEBUG // or any other level as described at https://logback.qos.ch/manual/architecture.html#effectiveLevel

  // optional
  additivity = true // or any other boolean value (see conversions below) as described at https://logback.qos.ch/manual/architecture.html#additivity

  // optional
  appenders = [ "appender-name", ... ]
}
```

Name convention
---------------

Configuration keys are mangled to a property name with following rule: any dash (-) followed by a letter is mangled into the uppercased letter.
For example, configuration key `my-property-name` is mangled as `myPropertyName`.

Value conversion
----------------

Possible value [types in configuration](https://github.com/typesafehub/config/blob/master/HOCON.md#unchanged-from-json) are translated into equivalent type in Java. Here is the conversion array

Typesafe config | Java
----------------|-----
integer         | `int`
floating point  | `double`
string          | `java.lang.String`
boolean         | `boolean`
null            | `null`
array           | `java.util.List`
object          | `java.util.Map<java.lang.String,java.lang.Object>`
