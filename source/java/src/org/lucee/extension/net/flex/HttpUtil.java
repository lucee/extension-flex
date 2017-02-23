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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.util.ListUtil;

public class HttpUtil {

	/**
	 * read all headers from request and return it
	 * @param req
	 * @return
	 */
	public static Map<String, Object> cloneHeaders(HttpServletRequest req) {
		Map<String, Object> headers=new HashMap<String, Object>();
		Enumeration<String> e = req.getHeaderNames(),ee;
		String name;
		while(e.hasMoreElements()){
			name= e.nextElement();
			ee=req.getHeaders(name);
			while(ee.hasMoreElements()){
				headers.put(name,ee.nextElement().toString());
			}
		}
		return headers;
	}

	public static Map<String, Object> cloneAttributes(HttpServletRequest req) {
		Map<String, Object> attributes=new HashMap<String, Object>();
		Enumeration e = req.getAttributeNames();
		String name;
		while(e.hasMoreElements()){
			name=(String) e.nextElement();
			attributes.put(name, req.getAttribute(name));
		}
		return attributes;
	}

	public static Map<String, String> cloneParameters(HttpServletRequest req) {
		ListUtil util = CFMLEngineFactory.getInstance().getListUtil();
		Map<String, String> parameters=new HashMap<String, String>();
		Enumeration e = req.getParameterNames();
		String[] values;
		String name;
		
		while(e.hasMoreElements()){
			name=(String) e.nextElement();
			values=req.getParameterValues(name);
			parameters.put(name, util.toList(values, ","));
		}
		return parameters;
	}
	
	public static Cookie[] cloneCookies(HttpServletRequest req) {
		Cookie[] src=req.getCookies();
		if(src==null)return new Cookie[0];
		
		Cookie[] dest=new Cookie[src.length];
		for(int i=0;i<src.length;i++) {
			dest[i]=(Cookie) src[i].clone();
		}
		return dest;
	}

}