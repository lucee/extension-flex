This extension can be built using ant (http://ant.apache.org/). It is not working yet out of the box. it still needs a manual installation of the servlet by updating the web.xml file.

Sample addition to web.xml

MessageBroker Servlet - Flex Gateway
<![CDATA[
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
  <!-- init-param>
    <param-name>services.configuration.file</param-name>
    <param-value>/WEB-INF/flex/services-config.xml</param-value>
  </init-param !-->
  <!-- init-param>
    <param-name>messageBrokerId</param-name>
    <param-value>_default_</param-value>
  </init-param !-->
  <!-- load-on-startup>2</load-on-startup !-->
</servlet>

<servlet-mapping>
  <servlet-name>MessageBrokerServlet</servlet-name>
  <url-pattern>/flex2gateway/*</url-pattern>
  <url-pattern>/flashservices/gateway/*</url-pattern>
  <url-pattern>/messagebroker/*</url-pattern>
</servlet-mapping>
]]>