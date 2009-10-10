/**
Copyright 2008, 2009 Mark Hooijkaas

This file is part of the Caas tool.

The Caas tool is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

The Caas tool is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with the Caas tool.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.kisst.cordys.caas;

import java.util.ArrayList;
import java.util.List;

import org.kisst.cordys.caas.util.XmlNode;

public class Organization extends CordysLdapObject {

	protected Organization(LdapObject parent, String dn) {
		super(parent, dn);
	}

	public NamedObjectList<User> getUser() {
		return new NamedObjectList<User>(getUsers()); 
	}
	public List<User> getUsers() {	
		XmlNode method=new XmlNode("GetOrganizationalUsers", xmlns_ldap);
		method.add("dn").setText(dn);
		return getObjectsFromEntries(soapCall(method));
	}


	public NamedObjectList<MethodSet> getMs() { 
		return new NamedObjectList<MethodSet>(getMethodSets());
	}
	public List<MethodSet> getMethodSets() {	
		XmlNode method=new XmlNode("GetMethodSets", xmlns_ldap);
		method.add("dn").setText(dn);
		method.add("labeleduri").setText("*");
		return getObjectsFromEntries(soapCall(method));
	}
	
	public NamedObjectList<Role> getRole() {
		return new NamedObjectList<Role>(getRoles());
	}
	public List<Role> getRoles() {	
		XmlNode method=new XmlNode("GetRolesForOrganization", xmlns_ldap);
		method.add("dn").setText(dn);
		return getObjectsFromEntries(soapCall(method));
	}

	public NamedObjectList<SoapNode> getSn() { 
		return new NamedObjectList<SoapNode>(getSoapNodes()); 
	}	
	public List<SoapNode> getSoapNodes() {	
		XmlNode method=new XmlNode("GetSoapNodes", xmlns_ldap);
		method.add("dn").setText(dn);
		method.add("namespace").setText("*");
		return getObjectsFromEntries(soapCall(method));
	}

	public NamedObjectList<SoapProcessor> getSp() { 
		return new NamedObjectList<SoapProcessor>(getSoapProcessors());
	}

	public List<SoapProcessor> getSoapProcessors() {
		ArrayList<SoapProcessor> result= new ArrayList<SoapProcessor>();
		for (Object sn : getSoapNodes()) {
			for (Object sp : ((SoapNode) sn).getSoapProcessors()) {
				result.add((SoapProcessor) sp); // using dn, to prevent duplicate names over organizations
			}
		}
		return result;
	}
}
