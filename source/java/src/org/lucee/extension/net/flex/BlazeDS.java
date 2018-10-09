/**
*
* Copyright (c) 2014, the Railo Company Ltd. All rights reserved.
* Copyright (c) 2017, Lucee Assosication Switzerland
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

import org.lucee.extension.amf.caster.AMFCaster;

import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.exp.PageException;
import lucee.runtime.util.Cast;
import flex.messaging.FlexContext;
import flex.messaging.MessageException;
import flex.messaging.config.ConfigMap;
import flex.messaging.messages.Message;
import flex.messaging.messages.RemotingMessage;
import flex.messaging.services.ServiceAdapter;

public class BlazeDS {
	

	private AMFCaster amfCaster;
	
	public BlazeDS(AMFCaster amfCaster) {
		this.amfCaster=amfCaster;
	}


	public Object invoke(ServiceAdapter serviceAdapter, Message message){
		RemotingMessage remotingMessage = (RemotingMessage)message;
    
		try {
			Object rtn = new CFMLProxy().invokeBody(
					amfCaster, 
					FlexContext.getServletContext(),
					FlexContext.getServletConfig(), 
					FlexContext.getHttpRequest(), 
					FlexContext.getHttpResponse(), 
					remotingMessage.getDestination(), 
					remotingMessage.getOperation(), 
					remotingMessage.getParameters());
			
	        return rtn;
		} 
       catch (Exception e) {
       	e.printStackTrace();// TODO
       	String msg=e.getMessage();
       	
       	MessageException me = new MessageException( e.getClass().getName() + " : " + msg);
       	me.setRootCause(e);
           me.setCode("Server.Processing");
           me.setRootCause(e);
           
           if(e instanceof PageException){
           	PageException pe=(PageException) e;
           	me.setDetails(pe.getDetail());
           	me.setMessage(pe.getMessage());
           	me.setCode(pe.getErrorCode());
           }
           
           throw me;
		}
   }
}