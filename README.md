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
  <version>0.1.0</version>
</dependency>
```


If you use sbt, add this dependency to your `build.sbt` file:

```scala
libraryDependencies += "org.gnieh" % "logback.config" % "0.1.0"
```

Format
------

The general configuration format is as follows:

```scala
logback {

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
