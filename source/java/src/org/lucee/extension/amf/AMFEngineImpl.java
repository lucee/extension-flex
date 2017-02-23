/**
 * Copyright (c) 2014, the Railo Company Ltd.
 * Copyright (c) 2015, Lucee Assosication Switzerland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either 
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package org.lucee.extension.amf;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lucee.commons.io.res.Resource;
import lucee.commons.lang.ClassException;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.loader.util.Util;
import lucee.runtime.CFMLFactory;
import lucee.runtime.Mapping;
import lucee.runtime.Page;
import lucee.runtime.PageContext;
import lucee.runtime.PageSource;
import lucee.runtime.config.Config;
import lucee.runtime.config.ConfigWeb;
import lucee.runtime.exp.PageException;
import lucee.runtime.net.amf.AMFEngine;
import lucee.runtime.type.Collection;
import lucee.runtime.type.Collection.Key;
import lucee.runtime.type.Struct;
import lucee.runtime.util.Cast;
import lucee.runtime.util.IO;

import org.lucee.extension.amf.caster.AMFCaster;
import org.lucee.extension.amf.caster.ClassicAMFCaster;
import org.lucee.extension.amf.caster.ModernAMFCaster;
import org.lucee.extension.amf.caster.OpenAMFCaster;
import org.openamf.AMFBody;
import org.openamf.AMFError;
import org.openamf.AMFMessage;
import org.openamf.ServiceRequest;
import org.openamf.io.AMFDeserializer;
import org.openamf.io.AMFSerializer;
import org.openamf.util.OpenAMFUtilsTest;

import flex.messaging.config.ConfigMap;

/**
 * AMF Engine
 */
public final class AMFEngineImpl implements AMFEngine {
	
	private static final int CONFIG_TYPE_XML = 1;
	private static final int CONFIG_TYPE_MANUAL = 2;

	private final Key FLASH;
	private final Key PARAMS;
	private final Key RESULT;
	private final Key AMF_FORWARD;
	
	private CFMLEngine engine;
	private AMFCaster caster;
	private Map<String, String> args;

	private int configType=CONFIG_TYPE_MANUAL;
	
	public AMFEngineImpl(){
		this.engine=CFMLEngineFactory.getInstance();
		Cast caster = engine.getCastUtil();

		FLASH = caster.toKey("flash");
		PARAMS = caster.toKey("params");
		RESULT = caster.toKey("result");
		AMF_FORWARD = caster.toKey("AMF-Forward");
		
	}
	

	@Override
	public void init(ConfigWeb config,Map<String, String> args) throws IOException {
		this.args=args;
		
		// caster
		String str = args.get("caster");
		if(Util.isEmpty(str,true)) str="classic";
		else str=str.trim();
		if("modern".equalsIgnoreCase(str))
			caster=new ModernAMFCaster();
		else if("open".equalsIgnoreCase(str))
			caster=OpenAMFCaster.getInstance();
		else
			caster=new ClassicAMFCaster();
		
		caster.init(args);
		
		// config
		
		str = args.get("configuration");
		if(Util.isEmpty(str,true)) str = args.get("config");
		if(Util.isEmpty(str,true)) str="manual";
		else str=str.trim();
		if("xml".equalsIgnoreCase(str))
			configType=CONFIG_TYPE_XML;
		else
			configType=CONFIG_TYPE_MANUAL;
		
		// init file context
		//createContext(config,configType);
	}
	
	/*private static void createContext(ConfigWeb config, int configType) throws IOException {
		if(configType==CONFIG_TYPE_XML) {
		
			String strPath = config.getServletContext().getRealPath("/WEB-INF");
			File webInf = new File(strPath);
			File flex = new File(webInf,"flex");
			if (!flex.exists()) flex.mkdirs();

			File f = new File(flex,"messaging-config.xml");
			if (!f.exists()) createFile("/org/lucee/extension/amf/resources/messaging-config.xml", f);
			f = new File(flex,"proxy-config.xml");
			if (!f.exists()) createFile("/org/lucee/extension/amf/resources/proxy-config.xml", f);
			f = new File(flex,"remoting-config.xml");
			if (!f.exists()) createFile("/org/lucee/extension/amf/resources/remoting-config.xml", f);
			f = new File(flex,"services-config.xml");
			if (!f.exists()) createFile("/org/lucee/extension/amf/resources/services-config.xml", f);
		}
	}*/
	
	/*private static void createFile(String resource, File file) throws IOException {
		IO util = CFMLEngineFactory.getInstance().getIOUtil();
		InputStream is = AMFUtil.class.getResourceAsStream(resource);
		if(is==null) throw new IOException("file ["+resource+"] does not exist.");
		file.createNewFile();
		util.copy(is, new FileOutputStream(file), true, true);
	}*/
	


	@Override
    public void service(HttpServlet servlet, HttpServletRequest req, HttpServletResponse rsp) throws IOException {
    	AMFMessage requestMessage = null;
        AMFMessage responseMessage = null;
        requestMessage = deserializeAMFMessage(req);
        responseMessage = processMessage(servlet, req, rsp, requestMessage);
        serializeAMFMessage(rsp, responseMessage);
    }

    private AMFMessage deserializeAMFMessage(HttpServletRequest req) throws IOException {
        DataInputStream dis = null;
       	try {
       		dis = new DataInputStream(req.getInputStream());
       		AMFDeserializer deserializer = new AMFDeserializer(dis);
       		AMFMessage message = deserializer.getAMFMessage();
       		return message;
       	}
       	finally {
       		engine.getIOUtil().closeSilent(dis);
       	}
    }

    private void serializeAMFMessage(HttpServletResponse resp, AMFMessage message) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        AMFSerializer serializer = new AMFSerializer(dos);
        serializer.serializeMessage(message);
        resp.setContentType("application/x-amf");
        resp.setContentLength(baos.size());
        ServletOutputStream sos = resp.getOutputStream(); 
        baos.writeTo(sos);
        sos.flush();
    }

    /**
     * Iterates through the request message's bodies, invokes each body and
     * then, builds a message to send as the results
     * @param req 
     * @param rsp 
     * @param message 
     * @return AMFMessage
     * @throws IOException 
     * @throws ServletException 
     */
    private AMFMessage processMessage(HttpServlet servlet, HttpServletRequest req, HttpServletResponse rsp, AMFMessage message)  {
        AMFMessage responseMessage = new AMFMessage();
        for (Iterator bodies = message.getBodies(); bodies.hasNext();) {
            AMFBody requestBody = (AMFBody) bodies.next();
            // invoke
            Object serviceResult = invokeBody(servlet,req, rsp, requestBody);
            String target = getTarget(requestBody, serviceResult);
            AMFBody responseBody = new AMFBody(target, "null", serviceResult);
            responseMessage.addBody(responseBody);
        }
        return responseMessage;
    }

    
    private Object invokeBody(HttpServlet servlet, HttpServletRequest req, HttpServletResponse rsp, AMFBody requestBody) { 
    	try {
	    	ServiceRequest request = new ServiceRequest(requestBody);
	        rsp.getOutputStream();// MUST muss das sein?
	       
	        return _invokeBody(OpenAMFCaster.getInstance(),null,servlet.getServletConfig(), req, rsp, request.getServiceName(), request.getServiceMethodName(), request.getParameters());
		} 
    	catch (Exception e) {
    		e.printStackTrace();
            rsp.setStatus(200);
            AMFError error=new AMFError();
            e.setStackTrace(e.getStackTrace());
            error.setDescription(e.getMessage());
			
			if(e instanceof PageException){
				PageException pe = (PageException)e;
	            error.setCode(pe.getErrorCode());
	            error.setCode(pe.getErrorCode());
	            error.setDetails(pe.getDetail());
			}
			
			return error;
		} 
    }
    
    private Object _invokeBody(AMFCaster caster,ConfigMap configMap,ServletConfig config,HttpServletRequest req, 
			HttpServletResponse rsp,String serviceName,String serviceMethodName,List rawParams) throws ServletException, PageException,IOException { 
    
		//try { 
    	
    	
        // Forward
        CFMLFactory factory = engine.getCFMLFactory(config, req);
        PageContext pc=null;

        // CFC Files
        String cfc;
        Object parameters=null;
        try {
            cfc="/"+serviceName.replace('.','/')+"."+AMFUtil.COMPONENTS_EXTENSION;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            pc=createPageContext(engine,factory,cfc,"method="+serviceMethodName,baos,req,true,-1);
            PageSource source = getPageSourceExisting(pc,cfc);

           
            parameters=caster.toCFMLObject(rawParams);
        	if(source!=null) {
        		print(pc,cfc+"?method="+serviceMethodName);
            	// Map
        			//print.err(parameters);
        		if(parameters instanceof Map){
        			//print.err("map");
            		pc.getHttpServletRequest().setAttribute("argumentCollection", parameters);
            	}
        		// List
            	else if(parameters instanceof List){
            		//print.err("list");
            		pc.getHttpServletRequest().setAttribute("argumentCollection", parameters);
            	}
            	else {
            		ArrayList list = new ArrayList();
            		list.add(parameters);
            		pc.getHttpServletRequest().setAttribute("argumentCollection", list);
            		
            	}
            	
                // Execute
                pc.executeCFML(cfc,true,true);

                // write back response
                writeBackResponse(pc,rsp);
                
                // After
	            return caster.toAMFObject(pc.variablesScope().get(AMF_FORWARD,null));
	            
            }
        }
        finally {
            if(pc!=null)factory.releaseLuceePageContext(pc,true);
        }    
        
     // CFML Files
        String cfml;
        try {
            cfml="/"+(serviceName.replace('.','/')+'/'+serviceMethodName.replace('.','/'))+"."+AMFUtil.TEMPLATE_EXTENSION;
            pc=createPageContext(engine,factory,cfml,"",null,req,true,-1);
            PageSource source = getPageSourceExisting(pc,cfml);
            
            
            
            
            if(source!=null) {
            	print(pc,cfml);
            	// Before
                Struct params=engine.getCreationUtil().createStruct();
                pc.variablesScope().setEL(FLASH,params);
                params.setEL(PARAMS,parameters);
                
                // Execute
                pc.executeCFML(cfml,true,true);
                
                // write back response
                writeBackResponse(pc,rsp);
                
                // After
                Object flash=pc.variablesScope().get(FLASH,null);
                if(flash instanceof Struct) {
                	return caster.toAMFObject(((Struct)flash).get(RESULT,null));
                }
                return null;
            }
        }
        finally {
            if(pc!=null)factory.releaseLuceePageContext(pc,true);
        }
        
        throw new AMFException("can't find cfml ("+cfml+") or cfc ("+cfc+") matching the request");
    }
	

	private static PageContext createPageContext(CFMLEngine engine,CFMLFactory factory,String scriptName,String queryString, OutputStream os, 
			HttpServletRequest formerReq, boolean register,long timeout) throws PageException {
		
    	// context root
    	Resource contextRoot = factory.getConfig().getRootDirectory();
    	if(!(contextRoot instanceof File))
    		throw engine.getExceptionUtil().createApplicationException("context root need to be on the local file system, now it is ["+contextRoot+"].");
		
    	// headers
    	Map<String, Object> headers = cloneHeaders(formerReq);
    	headers.put("AMF-Forward", "true");
    	
    	try {
			return engine.createPageContext(
					(File)contextRoot, 
					"localhost", 
					scriptName, 
					queryString, 
					formerReq.getCookies(), 
					headers, 
					cloneParameters(formerReq), 
					getAttributesAsMap(formerReq), 
					null, 
					timeout, register);
		} catch (ServletException e) {
			throw engine.getCastUtil().toPageException(e);
		}
    	
	}
    
    


	private static void writeBackResponse(PageContext pc, HttpServletResponse rsp) throws PageException {
		HttpServletResponse hsrd=pc.getHttpServletResponse();
        
		// Cookie
		Cookie[] cookies = getCookies(hsrd);
        if(cookies!=null) {
        	for(int i=0;i<cookies.length;i++) {
        		rsp.addCookie(cookies[i]);
        	}
        }
        CFMLEngine engine=CFMLEngineFactory.getInstance();
        
        // header
        Iterator<String> namesIt = hsrd.getHeaderNames().iterator();
        String name;
        while(namesIt.hasNext()){
        	name=namesIt.next();
        	Iterator<String> it = hsrd.getHeaders(name).iterator();
        	while(it.hasNext())
        		rsp.addHeader(name, it.next());
        }
        
        
        /*Pair<String,Object>[] headers = hsrd.getHeaders(hsrd);
        Pair<String,Object> header ;
        Object value;
        if(headers!=null) {
        	for(int i=0;i<headers.length;i++) {
        		header=headers[i];
        		value=header.getValue();
        		if(value instanceof Long)rsp.addDateHeader(header.getName(), ((Long)value).longValue());
        		else if(value instanceof Integer)rsp.addDateHeader(header.getName(), ((Integer)value).intValue());
        		else rsp.addHeader(header.getName(), engine.getCastUtil().toString(header.getValue(),""));
        	}
        }*/
	}
    

	private AMFCaster createCaster(CFMLEngine engine,Config ci, ConfigMap properties) throws ClassException {
		Cast caster = engine.getCastUtil();
		if(properties!=null){
			ConfigMap cases = properties.getPropertyAsMap("property-case", null);
	        if(cases!=null){
	        	if(!args.containsKey("force-cfc-lowercase"))
	        		args.put("force-cfc-lowercase",caster.toString(cases.getPropertyAsBoolean("force-cfc-lowercase", false)));
	        	if(!args.containsKey("force-query-lowercase"))
	        		args.put("force-query-lowercase",caster.toString(cases.getPropertyAsBoolean("force-query-lowercase", false)));
	        	if(!args.containsKey("force-struct-lowercase"))
	        		args.put("force-struct-lowercase",caster.toString(cases.getPropertyAsBoolean("force-struct-lowercase", false)));
	        	
	        }
	        ConfigMap access = properties.getPropertyAsMap("access", null);
	        if(access!=null){
	        	if(!args.containsKey("use-mappings"))
	        		args.put("use-mappings",caster.toString(access.getPropertyAsBoolean("use-mappings", false)));
	        	if(!args.containsKey("method-access-level"))
	        		args.put("method-access-level",access.getPropertyAsString("method-access-level","remote"));
	        }
		}
		
		AMFCaster amfCaster = new ClassicAMFCaster();//  (AMFCaster)engine.getClassUtil().loadInstance(ci.getAMFCasterClass());
		amfCaster.init(args);
		
		return amfCaster;
	}
    
    private static void print(PageContext pc, String str) {
		pc.getConfig().getOutWriter().println( str);
	}

    private String getTarget(AMFBody requestBody, Object serviceResult) {
        String target = "/onResult";
        if (serviceResult instanceof AMFError) {
            target = "/onStatus";
        }
        return requestBody.getResponse() + target;
    }
    
    private static Map<String,Object> cloneHeaders(HttpServletRequest req) {
    	Map<String,Object> headers=new HashMap<String, Object>();
		Enumeration e = req.getHeaderNames(),ee;
		String name,value;
		Set<String> tmp;
		while(e.hasMoreElements()){
			name=(String) e.nextElement();
			ee=req.getHeaders(name);
			tmp=new HashSet<String>();
			value=null;
			while(ee.hasMoreElements()){
				tmp.add(value=e.nextElement().toString());
			}
			if(tmp.size()==1)headers.put(name, value);
			else headers.put(name, tmp);
		}
		return headers;
	}
    
    private static Map<String,String> cloneParameters(HttpServletRequest req) {
		Map<String,String> parameters=new HashMap<String, String>();
		Enumeration e = req.getParameterNames();
		String[] values;
		String name;
		CFMLEngine engine=CFMLEngineFactory.getInstance();
		StringBuilder tmp;
		while(e.hasMoreElements()) {
			name=(String) e.nextElement();
			values=req.getParameterValues(name);
			if(values!=null){
				tmp=new StringBuilder();
				for(int i=0;i<values.length;i++){
					if(tmp.length()>0)tmp.append(',');
					tmp.append(values[i]);
				}
				parameters.put(name, tmp.toString());
			}
		}
		return parameters;
	}
    
    private static Map<String,Object> getAttributesAsMap(HttpServletRequest req) {
    	Map<String,Object> attributes=new HashMap<String, Object>();
		Enumeration e = req.getAttributeNames();
		String name;
		while(e.hasMoreElements()){
			name=(String) e.nextElement();// MUST (hhlhgiug) can throw ConcurrentModificationException
			if(name!=null)attributes.put(name, req.getAttribute(name));
		}
		return attributes;
	}
    

    private static PageSource getPageSourceExisting(PageContext pc, String cfml) throws PageException {
    	try {
    		Method method = pc.getClass().getMethod("getPageSourceExisting", new Class[]{String.class});
    		return (PageSource) method.invoke(pc, new Object[]{cfml});
    	}
    	catch(Exception e){
    		throw CFMLEngineFactory.getInstance().getCastUtil().toPageException(e);
    	}
	}
    

    private static Cookie[] getCookies(HttpServletResponse rsp) throws PageException {
    	// class HttpServletResponseDummy has this method
    	try {
    		Method method = rsp.getClass().getMethod("getCookies", new Class[]{});
    		return (Cookie[]) method.invoke(rsp, new Object[]{});
    	}
    	catch(Exception e){
    		throw CFMLEngineFactory.getInstance().getCastUtil().toPageException(e);
    	}
	}

}