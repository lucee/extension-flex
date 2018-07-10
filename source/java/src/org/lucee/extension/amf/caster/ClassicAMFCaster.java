/**
 *
 * Copyright (c) 2015, Lucee Assosication Switzerland
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
package org.lucee.extension.amf.caster;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.loader.util.Util;
import lucee.runtime.Component;
import lucee.runtime.Page;
import lucee.runtime.PageContext;
import lucee.runtime.PageSource;
import lucee.runtime.component.Property;
import lucee.runtime.config.ConfigWeb;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.Array;
import lucee.runtime.type.Collection;
import lucee.runtime.type.Collection.Key;
import lucee.runtime.type.Query;
import lucee.runtime.type.Struct;
import lucee.runtime.type.UDF;
import lucee.runtime.util.Cast;

import org.lucee.extension.amf.AMFUtil;
import org.w3c.dom.Node;

import flex.messaging.io.amf.ASObject;


/**
 * Cast a CFML object to AMF Objects and the other way
 */
public class ClassicAMFCaster implements AMFCaster {

	
	
	//private static ClassicAMFCaster singelton;
	
	protected boolean forceCFCLower;
	protected boolean forceStructLower;
	protected boolean forceQueryLower;

	// private int methodAccessLevel;  TODO add support for this

	private CFMLEngine engine;

	@Override
	public void init(Map arguments){
		engine = CFMLEngineFactory.getInstance();
		Cast caster = engine.getCastUtil();
		forceCFCLower=caster.toBooleanValue(arguments.get("force-cfc-lowercase"),false);
		forceQueryLower=caster.toBooleanValue(arguments.get("force-query-lowercase"),false);
		forceStructLower=caster.toBooleanValue(arguments.get("force-struct-lowercase"),false);
		// method access level
		/* TODO add support for this
		String str=caster.toString(arguments.get("method-access-level"),"remote");
		
		if("private".equalsIgnoreCase(str))methodAccessLevel=Component.ACCESS_PRIVATE;
		else if("package".equalsIgnoreCase(str))methodAccessLevel=Component.ACCESS_PACKAGE;
		else if("public".equalsIgnoreCase(str))methodAccessLevel=Component.ACCESS_PUBLIC;
		else methodAccessLevel=Component.ACCESS_REMOTE;*/
		
	}
	

	@Override
	public Object toAMFObject(Object cf) throws PageException {
		if(cf instanceof Node) return toAMFObject((Node)cf);
		if(cf instanceof List) return toAMFObject((List)cf);
		if(cf instanceof Array) return toAMFObject(((List)cf)); // TODO Array not necesaarly has to be a List, in that case convert ...
		if(cf instanceof Component)	return toAMFObject((Component)cf);
		if(cf instanceof Query) return toAMFObject((Query)cf);
		//if(cf instanceof Image) return toAMFObject((Image)cf); // TODO add support for this
		if(cf instanceof Map) return toAMFObject((Map)cf);
		if(cf instanceof Object[]) return toAMFObject((Object[])cf);
		
		return cf;
	}

	protected Object toAMFObject(Node node) {
		return node;
	}
	
	protected Object toAMFObject(Query query) throws PageException {
		List<ASObject> result = new ArrayList<ASObject>();
		int len=query.getRecordcount();
        Collection.Key[] columns=query.getColumnNames();
    	ASObject row;
        for(int r=1;r<=len;r++) {
        	result.add(row = new ASObject());
            for(int c=0;c<columns.length;c++) {
                row.put(toString(columns[c],forceQueryLower), toAMFObject(query.getAt(columns[c],r)) ); 
            }
        }
		return result;
	}
	
	/*protected Object toAMFObject(Image img) throws PageException { TODO add suppot for this
		try{
			return img.getImageBytes(null);
		}
		catch(Throwable t){
			if(t instanceof ThreadDeath) throw (ThreadDeath)t;
			return img.getImageBytes("png");
		}
	}*/

	protected ASObject toAMFObject(Component cfc) throws PageException {
		ASObject aso = new ASObject();
		aso.setType(cfc.getCallName());
		
		
		// Component c=ComponentSpecificAccess.toComponentSpecificAccess(methodAccessLevel,cfc);

		Property[] prop = cfc.getProperties(false);
		Object v; UDF udf;
    	if(prop!=null)for(int i=0;i<prop.length;i++) {
    		boolean remotingFetch = engine.getCastUtil().toBooleanValue(prop[i].getDynamicAttributes().get(engine.getCastUtil().toKey("remotingFetch"),Boolean.TRUE),true);
    		if(!remotingFetch) continue;
    		v=cfc.get(prop[i].getName(),null);
    		if(v==null){
    			//v=c.get("get"+prop[i].getName(),null);
    			v=cfc.getMember(Component.ACCESS_REMOTE, engine.getCastUtil().toKey("get"+prop[i].getName()), false, false);
	    		if(v instanceof UDF){
	            	udf=(UDF) v;
	            	if(udf.getReturnType()==15/*CFTypes.TYPE_VOID*/) continue;
	            	if(udf.getFunctionArguments().length>0) continue;
	            	
	            	try {
						v=cfc.call(engine.getThreadPageContext(), udf.getFunctionName(), new Object[]{});
					} catch (PageException e) {
						continue;
					}
	            }
    		}
    		
    		aso.put(toString(prop[i].getName(),forceCFCLower), toAMFObject(v));
    	}
    	return aso;
	}
    
	protected Object toAMFObject(Map map) throws PageException {
    	if(forceStructLower && map instanceof Struct) toAMFObject((Struct)map);
    	
    	map=(Map) AMFUtil.duplicateMap(map);
    	Iterator it = map.entrySet().iterator();
        Map.Entry entry;
        while(it.hasNext()) {
            entry=(Entry) it.next();
            entry.setValue(toAMFObject(entry.getValue()));
        }
        return engine.getCastUtil().toStruct(map);
    }
    
	protected Object toAMFObject(Struct src) throws PageException {
    	Struct trg=engine.getCreationUtil().createStruct();
    	//Key[] keys = src.keys();
    	Iterator<Entry<Key, Object>> it = src.entryIterator();
    	Entry<Key, Object> e;
        while(it.hasNext()) {
        	e = it.next();
            trg.set(engine.getCastUtil().toKey(toString(e.getKey(),forceStructLower)), toAMFObject(e.getValue()));
        }
        return trg;
    }
    
    
	
	protected Object toAMFObject(List list) throws PageException {
		Object[] trg=new Object[list.size()];
		ListIterator it = list.listIterator();
        
        while(it.hasNext()) {
        	trg[it.nextIndex()]=toAMFObject(it.next());
        }
        return trg;
    }
	
	protected Object toAMFObject(Object[] src) throws PageException {
		Object[] trg=new Object[src.length];
		for(int i=0;i<src.length;i++){
			trg[i]=toAMFObject(src[i]);
		}
		return trg;
    }
	

	@Override
	public Object toCFMLObject(Object amf) throws PageException {
		if(amf instanceof Node) return toCFMLObject((Node)amf);
		if(amf instanceof List) {
			List l = (List)amf;
			if(l.size() == 1 && l.get(0) instanceof ASObject){
				return toCFMLObject((ASObject)l.get(0));
			}
			return toCFMLObject((List)amf);
		}
		if(engine.getDecisionUtil().isNativeArray(amf)) {
			if(amf instanceof byte[]) return amf;
			if(amf instanceof char[]) return new String((char[])amf);
			return toCFMLObject(engine.getCastUtil().toNativeArray(amf));
		}
		if(amf instanceof ASObject) return toCFMLObject((ASObject)amf);
		if(amf instanceof Map) return toCFMLObject((Map)amf);
		if(amf instanceof Date) return engine.getCreationUtil().createDateTime(((Date)amf).getTime());
        if(amf == null) return "";
        
		return amf;
	}

	protected Object toCFMLObject(Node node) throws PageException {
		return toXMLStruct(node, true);
    }


	protected Object toCFMLObject(Object[] arr) throws PageException {
		Array trg=engine.getCreationUtil().createArray();
		for(int i=0;i<arr.length;i++){
			trg.setEL(i+1, toCFMLObject(arr[i]));
		}
		return trg;
    }
	
	protected Object toCFMLObject(List list) throws PageException {
        ListIterator it = list.listIterator();
        while(it.hasNext()) {
        	//arr.setE(it.nextIndex()+1, toCFMLObject(it.next()));
            list.set(it.nextIndex(),toCFMLObject(it.next()));
        }
        return engine.getCastUtil().toArray(list);
    }

	protected Object toCFMLObject(Map map) throws PageException {
		Iterator it = map.entrySet().iterator();
        Map.Entry entry;
        while(it.hasNext()) {
            entry=(Entry) it.next();
            entry.setValue(toCFMLObject(entry.getValue()));
        }
        return engine.getCastUtil().toStruct(map);
    }
	
	protected Object toCFMLObject(ASObject aso) throws PageException {
		if(!Util.isEmpty(aso.getType())){
			PageContext pc = engine.getThreadPageContext();
			ConfigWeb config = pc.getConfig();
			
				String name="/"+aso.getType().replace('.', '/')+"."+AMFUtil.COMPONENTS_EXTENSION;

				Page p = loadPage(pc, getPageSources(pc,name), null) ;
				if(p==null)throw engine.getExceptionUtil().createApplicationException("Could not find a Component with name ["+aso.getType()+"]");
				
				Component cfc = loadComponent(pc, p,  aso.getType(), false,false,false,true);
				//ComponentSpecificAccess cw=ComponentSpecificAccess.toComponentSpecificAccess(config.getComponentDataMemberDefaultAccess(),cfc); // TODO is this necessary
				
				Iterator it = aso.entrySet().iterator();
				Map.Entry entry;
				while(it.hasNext()){
					entry = (Entry) it.next();
					cfc.set(engine.getCastUtil().toKey(entry.getKey()), toCFMLObject(entry.getValue()));
				}
				return cfc;
			
			
		}
		return toCFMLObject((Map)aso);
    }
	


	protected String toString(Object key, boolean forceLower) {
		if(key instanceof Key) return toString((Key)key, forceLower);
		return toString(engine.getCastUtil().toString(key,""), forceLower);
	}
	
	protected String toString(Key key, boolean forceLower) {
		if(forceLower) return key.getLowerString();
		return key.getString();
	}
	
	protected String toString(String key, boolean forceLower) {
		if(forceLower) return key.toLowerCase();
		return key;
	}
	
	private PageSource[] getPageSources(PageContext pc, String relPath) throws PageException {
    	// class HttpServletResponseDummy has this method
    	try {
    		Method method = pc.getClass().getMethod("getPageSources", new Class[]{String.class});
    		return (PageSource[]) method.invoke(pc, new Object[]{relPath});
    	}
    	catch(Exception e){
    		throw CFMLEngineFactory.getInstance().getCastUtil().toPageException(e);
    	}
	}
	

    private static Page loadPage(PageContext pc,PageSource[] arr,Page defaultValue) throws PageException {
		if(arr==null || arr.length==0) return null;
		Page p;
		for(int i=0;i<arr.length;i++) {
			p=arr[i].loadPageThrowTemplateException(pc,false,(Page)null);
			if(p!=null) return p;
		}
		return defaultValue;
	}
    


	public static Component loadComponent(PageContext pc,Page page, String callPath, boolean isRealPath, boolean silent,boolean isExtendedComponent, boolean executeConstr) throws PageException  {
		try {
			Class<?> clazz = CFMLEngineFactory.getInstance().getClassUtil().loadClass("lucee.runtime.component.ComponentLoader");
    		Method method = clazz.getMethod("loadComponent", new Class[]{PageContext.class,Page.class, String.class, boolean.class, boolean.class,boolean.class, boolean.class});
    		return (Component) method.invoke(null, new Object[]{pc,page, callPath, isRealPath, silent,isExtendedComponent, executeConstr});
    	}
    	catch(Exception e){
    		throw CFMLEngineFactory.getInstance().getCastUtil().toPageException(e);
    	}
	}

	private static Object toXMLStruct(Node node, boolean caseSensitive) throws PageException {
		try {
			Class<?> clazz = CFMLEngineFactory.getInstance().getClassUtil().loadClass("lucee.runtime.text.xml.XMLCaster");
    		Method method = clazz.getMethod("toXMLStruct", new Class[]{Node.class, boolean.class});
    		return method.invoke(null, new Object[]{node,caseSensitive});
    	}
    	catch(Exception e){
    		throw CFMLEngineFactory.getInstance().getCastUtil().toPageException(e);
    	}
	}
}