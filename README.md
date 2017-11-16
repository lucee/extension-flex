## Vulnerability
There are some known vulnerability with the Flex Framework codebase, please educate yourself about it before using Flex in combination with Lueee.

## Installation

- built the necessary artifacts by executing ant (http://ant.apache.org/) in the root of the project. This will create a zip file in the folder "dist"
- add the following to your web.xml (also named webdefault.xml or server.xml depending on the servlet engine used).

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
- copy the jar files from the zip file (see #1) to your classpath ("libs" folder NOT to lucee-server/bundles!)
- copy the xml files from the zip file (see #1) to WEB-INF/flex of your webroot
- restart the servlet engine
