/**
*
* Copyright (c) 2014, the Railo Company Ltd. All rights reserved.
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
**/
package org.lucee.extension.net.flex;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.lucee.extension.amf.caster.AMFCaster;

import lucee.commons.io.log.Log;
import lucee.commons.io.res.Resource;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.CFMLFactory;
import lucee.runtime.PageContext;
import lucee.runtime.PageSource;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.Collection;
import lucee.runtime.type.Struct;
import lucee.runtime.util.Cast;
import lucee.runtime.util.Creation;
import lucee.runtime.util.HTTPUtil;
import flex.messaging.config.ConfigMap;

public class CFMLProxy {
	
	
	private final Collection.Key FLASH;
	private final Collection.Key PARAMS;
	private final Collection.Key RESULT;
	private final Collection.Key AMF_FORWARD;
	
	
	private CFMLEngine engine;

	public CFMLProxy() {
		engine = CFMLEngineFactory.getInstance();
		Cast cast = engine.getCastUtil();
		FLASH=cast.toKey("flash");
		PARAMS=cast.toKey("params");
		RESULT=cast.toKey("result");
		AMF_FORWARD=cast.toKey("AMF-Forward");
	}
	

	public Object invokeBody(AMFCaster caster, ServletContext context,ServletConfig config,HttpServletRequest req, 
			HttpServletResponse rsp,String serviceName,String serviceMethodName,List rawParams) throws ServletException, PageException,IOException { 
   
		
   	
       // Forward
       CFMLFactory factory = engine.getCFMLFactory(config, req);
       PageContext pc=null;

       // CFC Files
       String cfc;
       Object parameters=null;
       try {
           cfc="/"+serviceName.replace('.','/')+".cfc";
           ByteArrayOutputStream baos = new ByteArrayOutputStream();
           
           String qs=req.getQueryString();
           if(qs==null || qs.isEmpty()) qs="";
           else qs+='&';
           qs+="method="+serviceMethodName;
           
           pc=createPageContext(factory,cfc,qs,baos,req);
           PageSource source = getPageSourceExisting(pc,cfc);

           parameters=caster.toCFMLObject(rawParams);
       	if(source!=null) {
       		print(pc,cfc+"?"+qs);
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
               pc.executeCFML(cfc,true,false);

               // write back response
               writeBackResponse(pc,rsp);
               
               // After
	            return caster.toAMFObject(pc.variablesScope().get(AMF_FORWARD,null));
	            
           }
       }
       finally {
           if(pc!=null)factory.releaseLuceePageContext(pc);
       }    
       
    // CFML Files
       String cfml;
       try {
           cfml="/"+(serviceName.replace('.','/')+'/'+serviceMethodName.replace('.','/'))+".cfm";
           String qs=req.getQueryString();
           if(qs==null) qs="";
           
           pc=createPageContext(factory,cfml,qs,null,req);
           PageSource source = getPageSourceExisting(pc,cfml);
           
           if(source!=null) {
           	print(pc,cfml);
           	// Before
               Struct params=engine.getCreationUtil().createStruct();
               pc.variablesScope().setEL(FLASH,params);
               params.setEL(PARAMS,parameters);
               
               // Execute
               pc.executeCFML(cfml,true,false);
               
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
           if(pc!=null)factory.releaseLuceePageContext(pc);
       }
       
       throw new IOException("can't find cfml ("+cfml+") or cfc ("+cfc+") matching the request");
   }
	
	
	
	private void writeBackResponse(PageContext pc, HttpServletResponse rsp) throws PageException {
		Cast caster = engine.getCastUtil();
		HttpServletResponse hsrd = pc.getHttpServletResponse();
       
		// Cookie
		Cookie[] cookies = getCookies(hsrd);
		if(cookies!=null) {
	       	for(int i=0;i<cookies.length;i++) {
	       		rsp.addCookie(cookies[i]);
	       	}
		}
       
       // header
       Map<String,Object> headers = getHeaders(hsrd);
       Iterator<Entry<String, Object>> it = headers.entrySet().iterator();
       Entry<String, Object> e;
       Object value;
       while(it.hasNext()) {
    	   e = it.next();
    	   value=e.getValue();
    	   if(value instanceof Long)rsp.addDateHeader(e.getKey(), ((Long)value).longValue());
    	   else if(value instanceof Integer)rsp.addDateHeader(e.getKey(), ((Integer)value).intValue());
    	   else rsp.addHeader(e.getKey(), caster.toString(e.getValue(),""));
       }
	}

	private Map<String, Object> getHeaders(HttpServletResponse rsp) throws PageException {
		Map<String, Object> map=new HashMap<String, Object>();
		try {
			Method m = rsp.getClass().getMethod("getHeaders", new Class[0]);
			Object[] arr=(Object[]) m.invoke(rsp, new Object[0]);
			for(Object obj:arr) {
				put(map,obj);
			}
		}
		catch(Exception e) {
			throw CFMLEngineFactory.getInstance().getCastUtil().toPageException(e);
		}
		return map;
	}


	private void put(Map<String, Object> map, Object obj) throws PageException {
		try {
			Method m = obj.getClass().getMethod("getName", new Class[0]);
			String n=(String) m.invoke(obj, new Object[0]);
			m = obj.getClass().getMethod("getValue", new Class[0]);
			Object v=m.invoke(obj, new Object[0]);
			map.put(n, v);
		}
		catch(Exception e) {
			throw CFMLEngineFactory.getInstance().getCastUtil().toPageException(e);
		}
	}


	private Cookie[] getCookies(HttpServletResponse rsp) throws PageException {
		try {
			Method m = rsp.getClass().getMethod("getCookies", new Class[0]);
			return (Cookie[]) m.invoke(rsp, new Object[0]);
		}
		catch (Exception e) {
			throw CFMLEngineFactory.getInstance().getCastUtil().toPageException(e);
		}
	}


	private PageSource getPageSourceExisting(PageContext pc, String relpath) throws PageException, RuntimeException {
		try {
			Method m = pc.getClass().getMethod("getPageSourceExisting", new Class[]{String.class});
			return (PageSource) m.invoke(pc, new Object[]{relpath});
		} 
		catch (Exception e) {
			throw CFMLEngineFactory.getInstance().getCastUtil().toPageException(e);
		}
	}


	private PageContext createPageContext(CFMLFactory factory,String scriptName,String queryString, OutputStream os, 
			HttpServletRequest formerReq) {
		Resource root = factory.getConfig().getRootDirectory();
		Creation creator = engine.getCreationUtil();
		
		// Request
		Map<String, Object> headers = HttpUtil.cloneHeaders(formerReq);
		headers.put(AMF_FORWARD.getString(), "true");
		HttpServletRequest req = creator.createHttpServletRequest(
				new File(root.getAbsolutePath()), 
				"localhost", 
				scriptName, 
				queryString, 
				HttpUtil.cloneCookies(formerReq), 
				headers, 
				HttpUtil.cloneParameters(formerReq), 
				HttpUtil.cloneAttributes(formerReq), 
				null);
		
		
		// Response
		if(os==null)os=DevNullOutputStream.DEV_NULL_OUTPUT_STREAM;
		HttpServletResponse rsp = creator.createHttpServletResponse(os);
		
		
		return  factory.getLuceePageContext(factory.getServlet(), req, rsp, null, false, -1, false);
	}
	private void print(PageContext pc, String str) {
		Log log = pc.getConfig().getLog("application");
		log.info("flex", str);
       
	}
}
