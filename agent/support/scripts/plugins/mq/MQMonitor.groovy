/**
MQ Monitoring Script for jZab Scripting Plugin
Whitehead, Aug 15, 2012
Requires the following MQ jars:
	com.ibm.msg.client.commonservices.j2se.jar
	com.ibm.mq.jar
	com.ibm.mq.jmqi.jar
	com.ibm.msg.client.wmq.common.jar
	com.ibm.mq.jmqi.remote.jar
	com.ibm.mqjms.jar
	com.ibm.mq.commonservices.jar
	com.ibm.mq.jmqi.system.jar
	com.ibm.mq.headers.jar
	com.ibm.msg.client.commonservices.jar
	com.ibm.mq.pcf.jar
	com.ibm.msg.client.jms.internal.jar
	com.ibm.msg.client.wmq.factories.jar

*/

import com.ibm.mq.constants.MQConstants
import java.text.SimpleDateFormat
import com.ibm.mq.pcf.*

/**

*/
public List request(byName, agent, type, parameters) {
    def responses = [];
    def PCFMessage request = new PCFMessage(type);
    parameters.each() { name, value ->
        request.addParameter(name, value);
    }
    try {
      agent.send(request).each() {
          def responseValues = [:];
          it.getParameters().toList().each() { pcfParam ->
              def value = pcfParam.getValue();
              if(value instanceof String) value = value.trim();
              responseValues.put(byName ? pcfParam.getParameterName() : pcfParam.getParameter(), value);
          }
          responses.add(responseValues);
      }
      return responses;
    } catch (e) {
    		println "Exception Request Type:${type}, Parameters:\n${parameters}";
    		e.printStackTrace(System.err);
    		throw e;
    }
}
