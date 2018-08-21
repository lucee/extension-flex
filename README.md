## Vulnerability
There are some known vulnerability with the Flex Framework codebase, please educate yourself about it before using Flex in combination with Lueee.

## Installation

- Download the prebuilt ZIP file from [releases/tag/1.0.0.3](releases/tag/1.0.0.3).  Alternatively, you can build the artifacts yourself by running `ant` (http://ant.apache.org/) in the root of the project, which will create the ZIP file in the `dist` directory.

- Add the following to your deployment descriptor file, web.xml (named webdefault.xml in Jetty).

```xml
<!-- ===================================================================== -->
<!-- Lucee MessageBroker Servlet - Flex Gateway                            -->
<!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
<!-- * ATTENTION - ATENCION - ACHTUNG  -  ATTENTION - ATENCION - ACHTUNG * -->
<!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
<!-- messageBrokerId must be unique for each defintion of the MessageBroker-->
<!-- Servlet.  if you use the MessageBroker Servlet and define it in more  -->
<!-- than one xml file, you must uncomment the messageBrokerId init-param  -->
<!-- and set a different value in each definition.                         -->
<!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
<servlet id="MessageBrokerServlet">
  <description>Lucee Servlet for Flex Gateway</description>
  <servlet-name>MessageBrokerServlet</servlet-name>
  <display-name>MessageBrokerServlet</display-name>
  <servlet-class>flex.messaging.MessageBrokerServlet</servlet-class>
  <init-param>
    <param-name>services.configuration.file</param-name>
    <param-value>/WEB-INF/flex/services-config.xml</param-value>
  </init-param>
  <init-param>
    <param-name>messageBrokerId</param-name>
    <param-value>_default_</param-value>
  </init-param>
  <load-on-startup>3</load-on-startup>
</servlet>

<servlet-mapping>
  <servlet-name>MessageBrokerServlet</servlet-name>
  <url-pattern>/flex2gateway/*</url-pattern>
  <url-pattern>/flashservices/gateway/*</url-pattern>
  <url-pattern>/messagebroker/*</url-pattern>
</servlet-mapping>
```

- Copy the JAR files from the ZIP file jars directory to your classpath, e.g. the same directory where the primary Lucee JAR file is saved (NOT to the bundles directory!)

- Copy the XML files from the ZIP file resources directory to WEB-INF/flex of your webroot

- Restart the servlet container (e.g. Tomcat, Jetty)
