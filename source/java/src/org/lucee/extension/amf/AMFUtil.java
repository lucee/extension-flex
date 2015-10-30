package org.lucee.extension.amf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import lucee.commons.lang.ClassException;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;

public class AMFUtil {
	
	public static final String TEMPLATE_EXTENSION = "cfm";
	public static final String COMPONENTS_EXTENSION = "cfc";
	
	public static List duplicateList(List list) {
    	List newList;
    	try {
    		newList=(List) CFMLEngineFactory.getInstance().getClassUtil().loadInstance(list.getClass());
		} catch (IOException ioe) {
			newList=new ArrayList();
		}
    	return duplicateList(list, newList);
	}
	
	public static List duplicateList(List list,List newList) {
    	ListIterator it = list.listIterator();	
    	while(it.hasNext()) {
    		newList.add(it.next());
    	}
		return newList;
	}
	
	public static Map duplicateMap(Map map){
    	Map other;
    	CFMLEngine engine = CFMLEngineFactory.getInstance();
    	try {
			other=(Map) engine.getClassUtil().loadInstance(map.getClass());
		} catch (IOException ioe) {
			other=new HashMap();
    	}
		duplicateMap(map,other);
        return other;
    }
    
    public static Map duplicateMap(Map map,Map newMap){
    	Iterator it=map.keySet().iterator();
        while(it.hasNext()) {
            Object key=it.next();
            newMap.put(key,map.get(key));
        }
        return newMap;
    }
}
