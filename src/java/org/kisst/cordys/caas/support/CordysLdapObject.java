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

package org.kisst.cordys.caas.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.kisst.cordys.caas.AuthenticatedUser;
import org.kisst.cordys.caas.ConnectionPoint;
import org.kisst.cordys.caas.Isvp;
import org.kisst.cordys.caas.Method;
import org.kisst.cordys.caas.MethodSet;
import org.kisst.cordys.caas.Organization;
import org.kisst.cordys.caas.Role;
import org.kisst.cordys.caas.SoapNode;
import org.kisst.cordys.caas.SoapProcessor;
import org.kisst.cordys.caas.User;
import org.kisst.cordys.caas.util.ReflectionUtil;
import org.kisst.cordys.caas.util.XmlNode;


public abstract class CordysLdapObject extends CordysObject {
	public abstract class AbstractProperty {
		abstract public Object get();
		public String toString() { return ""+get(); }
	}
	public class StringProperty extends AbstractProperty {
		private final String path;
		private final int startPos;
		public StringProperty(String path) { this(path,0);}
		public StringProperty(String path, int startPos) {
			this.startPos=startPos;
			this.path=path+"/string";
		}
		public String get() { 
			String s= getEntry().getChildText(path);
			if (s!=null && startPos>0)
				return s.substring(startPos);
			return s;
		}
		public void set(String value) {
			checkIfMayBeModified(); 
			XmlNode newEntry=getEntry().clone();
			newEntry.getChild(path).setText(value);
			updateLdap(newEntry);
		}
	}
	public class XmlProperty extends StringProperty {
		public XmlProperty(String path) {super(path);}
		public XmlNode getXml() { return new XmlNode(get()); }
		public void set(XmlNode value) { set(value.toString()); }
	}
	public class XmlSubProperty extends AbstractProperty {
		private final XmlProperty xml;
		private final String path;
		public XmlSubProperty(XmlProperty xml, String path) {
			this.xml=xml;
			this.path=path;
		}
		public String get() {return xml.getXml().getChildText(path); }
		public void set(String value) {
			XmlNode newnode=xml.getXml().clone();
			newnode.setChildText(path, value);
			xml.set(newnode);
		}
	}

	public class BooleanProperty extends StringProperty {
		public BooleanProperty(String path) {super(path);}
		public Boolean getBool() { return "true".equals(get()); }
		public void set(boolean value) { set(""+value);}
	}
	public class RefProperty<T extends CordysLdapObject> extends StringProperty {
		public RefProperty(String path) {super(path);}
		@SuppressWarnings("unchecked")
		public T getRef() { return (T) getSystem().getLdap(get()); }
		public void set(T value) { set(value.getDn()); }
	}
	public class StringList extends AbstractProperty {
		// TODO: cache this?
		private final String path;
		public StringList(String path) {this.path=path;}
		public List<String> get() { 
			ArrayList<String> result=new ArrayList<String>();
			XmlNode start=getEntry().getChild(path);
			if (start!=null)
				for (XmlNode child: start.getChildren("string")) 
					result.add(child.getText());
			return result;
		}
		public void add(String value) {
			checkIfMayBeModified(); 
			XmlNode newEntry=getEntry().clone();
			newEntry.getChild(path).add("string").setText(value);
			updateLdap(newEntry);
		}
		public void remove(String value) {
			checkIfMayBeModified(); 
			XmlNode newEntry= getEntry().clone();
			XmlNode list=newEntry.getChild(path);
			for(XmlNode e: list.getChildren()) {
				if (value.equals(e.getText())) {
					list.remove(e);
				}
			}
			updateLdap(newEntry);
		}

	}

	public final StringProperty description = new StringProperty("description");
	public final StringProperty desc = description;

	private final CordysSystem system;
	private final CordysObject parent; 
	private final String dn;
	private XmlNode entry;

	protected CordysLdapObject(CordysSystem system, String dn) {
		this.system=system;
		this.parent=null;
		this.dn=dn;
	}
	
	protected CordysLdapObject(CordysObject parent, String dn) {
		this.system=parent.getSystem();
		this.parent=parent;
		this.dn=dn;
	}
	public void refresh() { 
		entry=null;
		for (Object o: getProps().values()) {
			if (o instanceof CordysObject)
				((CordysObject) o).refresh();
		}
	}
	public CordysObject getParent() { return parent; }
	public CordysSystem getSystem() { return system; }
	public XmlNode call(XmlNode method) { return getSystem().call(method); }
	
	public String getKey() { return "ldap:"+dn; }
	public String getDn() { return dn; }
	public String getName() {
		int pos=dn.indexOf("=");
		int pos2=dn.indexOf(",",pos);
		return dn.substring(pos+1,pos2);
	}
	public String toString() {
		//if (getSystem().displayFormat==0) {
		if (true) {
			String c=this.getClass().getSimpleName()+"("+getName()+")";
			if (parent!=null && (parent instanceof CordysLdapObject))
				c=parent.toString()+"."+c;
			return c; 
		}
		else {
			if (parent!=null && (parent instanceof CordysLdapObject))
				return parent.toString()+"."+getName();
			else
				return getName();
		}
	}
	public boolean equals(Object o) {
		if (o instanceof CordysLdapObject)
			return dn.equals(((CordysLdapObject)o).getDn());
		return false;
	}
	public int compareTo(CordysObject o) {
		if (o==this)
			return 0;
		if (o==null)
			return -1;
		String[] d1=getKey().split(",");
		String[] d2=o.getKey().split(",");
		int p1=d1.length-1;
		int p2=d2.length-1;
		while (p1>=0 && p2>=0 ) {
			int comp=d1[p1--].compareTo(d2[p2--]);
			if (comp!=0)
				return comp;
		}
		return 0;
	}


	void setEntry(XmlNode entry) {
		this.entry=entry;
		entry.detach();
	}
	public XmlNode getEntry() {
		if (entry!=null && useCache())
			return entry;
		XmlNode  method=new XmlNode("GetLDAPObject", xmlns_ldap);
		method.add("dn").setText(dn);
		XmlNode response = call(method);
		setEntry(response.getChild("tuple/old/entry"));
		return entry;
	}

	protected XmlNode newEntryXml(String prefix, String name, String ... types) {
		XmlNode newEntry = new XmlNode("entry",xmlns_ldap);
		newEntry.setAttribute("dn", "cn="+name+","+prefix+dn);
		XmlNode child = newEntry.add("objectclass");
		child.add("string").setText("top");
		for (String t: types)
			child.add("string").setText(t);
		newEntry.add("cn").add("string").setText(name);
		return newEntry;
	}
	protected void createInLdap(XmlNode newEntry) { updateLdap(null, newEntry); }
	protected void updateLdap(XmlNode newEntry) { updateLdap(entry.clone(), newEntry); }
	protected void updateLdap(XmlNode oldEntry, XmlNode newEntry) {
		XmlNode method=new XmlNode("Update", xmlns_ldap);
		XmlNode tuple=method.add("tuple");
		if (oldEntry!=null)
			tuple.add("old").add(oldEntry);
		if (newEntry!=null)
			tuple.add("new").add(newEntry);
		call(method);
		setEntry(newEntry);
	}
	protected void preDeleteHook() {}
	public void checkIfMayBeModified() {
		CordysObject obj=this;
		while (obj!=null && (obj instanceof CordysLdapObject)) {
			if (obj instanceof Isvp)
				throw new RuntimeException("It is not allowed to delete any part of an ISVP");
			obj=((CordysLdapObject)obj).getParent();
		}
	}
	public void delete() {
		checkIfMayBeModified(); 
		preDeleteHook();
		XmlNode method=new XmlNode("DeleteRecursive", xmlns_ldap);
		XmlNode tuple=method.add("tuple");
		tuple.add("old").add(entry.clone());
		call(method);
		getParent().refresh();
		getSystem().remove(getDn());
	}
	
	
	private final static HashMap<String,Class> ldapObjectTypes=new HashMap<String,Class>();
	static {
		ldapObjectTypes.put("busauthenticationuser", AuthenticatedUser.class);
		//ldapObjectTypes.put("groupOfNames", Isvp.class); this one is not unique
		ldapObjectTypes.put("busmethod", Method.class);
		ldapObjectTypes.put("busmethodset", MethodSet.class);
		ldapObjectTypes.put("organization", Organization.class);
		ldapObjectTypes.put("busorganizationalrole", Role.class);
		ldapObjectTypes.put("bussoapnode", SoapNode.class);
		ldapObjectTypes.put("bussoapprocessor", SoapProcessor.class);
		ldapObjectTypes.put("busorganizationaluser", User.class);
		ldapObjectTypes.put("busconnectionpoint", ConnectionPoint.class);
	}
	static private Class determineClass(CordysSystem system, XmlNode entry) {
		XmlNode objectclass=entry.getChild("objectclass");
		for(XmlNode o:objectclass.getChildren("string")) {
			Class c=ldapObjectTypes.get(o.getText());
			if (c!=null)
				return c;
		}
		String dn=entry.getAttribute("dn");
		if (dn.substring(dn.indexOf(",")+1).equals(system.getDn()) && dn.startsWith("cn="))
			return Isvp.class;
		return null;
	}
	static CordysObject createObject(CordysSystem system, String dn) {
		XmlNode method=new XmlNode("GetLDAPObject", CordysObject.xmlns_ldap);
		method.add("dn").setText(dn);
		XmlNode response = system.call(method);
		XmlNode entry=response.getChild("tuple/old/entry");
		return CordysLdapObject.createObject(system, entry);
	}

	static CordysObject createObject(CordysSystem system, XmlNode entry) {
		if (entry==null)
			return null;
		String newdn=entry.getAttribute("dn");
		CordysObject parent = calcParent(system, entry);
		Class resultClass = determineClass(system, entry);
		if (resultClass==null)
			return null;
		Constructor cons=ReflectionUtil.getConstructor(resultClass, new Class[] {CordysObject.class, String.class});
		cons.setAccessible(true);
		try {
			CordysLdapObject result = (CordysLdapObject) cons.newInstance(new Object[]{parent, newdn});
			result.setEntry(entry);
			//tree.put(newdn, result);
			return result;
		}
		catch (IllegalArgumentException e) { throw new RuntimeException(e); }
		catch (InstantiationException e) { throw new RuntimeException(e); }
		catch (IllegalAccessException e) { throw new RuntimeException(e); }
		catch (InvocationTargetException e) { throw new RuntimeException(e); }
	}
	static private CordysObject calcParent(CordysSystem system, XmlNode entry) {
		String dn=entry.getAttribute("dn");
		//System.out.println("calcParent ["+dn+"]");
		if (dn.equals(system.getDn())) // Safeguard
			return system;
		if (dn.length()<=system.getDn().length()) // Safeguard
			return null;
		do {
			int pos=dn.indexOf(",");
			dn=dn.substring(pos+1);
			CordysObject parent=system.getLdap(dn);
			if (parent!=null)
				return parent;
		} while (dn.length()>0);
		throw new RuntimeException("Could not find a parent for "+dn);
	}
}
