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

import org.kisst.cordys.caas.util.XmlNode;


public abstract class CordysLdapObject extends CordysObject implements LdapObject {
	protected abstract class AbstractProperty extends CordysObject {
		public void clearCache() {}
		public CordysSystem getSystem() { return CordysLdapObject.this.getSystem();	}
	}
	protected class StringProperty extends AbstractProperty {
		private final String path;
		protected StringProperty(String path) {this.path=path;}
		public String get() { return getEntry().getChildText(path); }
	}
	protected class BooleanProperty extends AbstractProperty {
		private final String path;
		protected BooleanProperty(String path) {this.path=path;}
		public Boolean get() { return "true".equals(getEntry().getChildText(path)); }
		public void set(boolean value) { System.out.println("setting ");}
	}
	protected class RefProperty<T extends LdapObject> extends AbstractProperty {
		private final String path;
		protected RefProperty(String path) {this.path=path;}
		@SuppressWarnings("unchecked")
		public T get() { 
			String dn= getEntry().getChildText(path);
			return (T) getSystem().getObject(dn);
		}
	}

	protected final CordysSystem system;
	private final LdapObject parent; 
	protected final String dn;
	private XmlNode entry;

	public boolean cache;
	
	protected CordysLdapObject(CordysSystem system, String dn) {
		this.system=system;
		this.parent=null;
		this.dn=dn;
	}
	
	protected CordysLdapObject(LdapObject parent, String dn) {
		this.system=parent.getSystem();
		this.parent=parent;
		this.dn=dn;
	}
	public void clearCache() { 
		entry=null;
		for (CordysObject o: getProps().values()) 
			o.clearCache();
	}
	public LdapObject getParent() { return parent; }
	public CordysSystem getSystem() { return system; }
	public XmlNode call(XmlNode method) { return getSystem().call(method); }
	
	public String getDn() { return dn; }
	public String getName() {
		int pos=dn.indexOf("=");
		int pos2=dn.indexOf(",",pos);
		return dn.substring(pos+1,pos2);
	}
	public String toString() {
		if (getSystem().displayFormat==0) {
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
		if (o instanceof LdapObject)
			return dn.equals(((LdapObject)o).getDn());
		return false;
	}
	public int compareTo(LdapObject o) {
		if (o==this)
			return 0;
		String[] d1=dn.split(",");
		String[] d2=o.getDn().split(",");
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

	protected void addLdapString(String group, String value) {
		XmlNode newEntry=getEntry().clone();
		newEntry.getChild(group).add("string").setText(value);
		updateLdap(newEntry);
	}
	protected void removeLdapString(String group, String value) {
		XmlNode newEntry= getEntry().clone();
		for(XmlNode e: newEntry.getChild(group).getChildren()) {
			if (e.getText().equals(value))
				newEntry.remove(e);
		}
		updateLdap(newEntry);
	}

	protected void updateLdap(XmlNode newEntry) {
		XmlNode method=new XmlNode("Update", xmlns_ldap);
		XmlNode tuple=method.add("tuple");
		tuple.add("old").add(entry.clone());
		tuple.add("new").add(newEntry);
		call(method);
		setEntry(newEntry);
	}
	
	public void deepdiff(LdapObject other) { diff(other,100); }
	public void diff(LdapObject other) { diff(other,0); }
	public abstract void diff(LdapObject other, int depth);

}
