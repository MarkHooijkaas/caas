package org.kisst.cordys.caas.support;

import org.kisst.cordys.caas.CordysSystem;
import org.kisst.cordys.caas.support.LdapObject.AbstractProperty;
import org.kisst.cordys.caas.support.LdapObject.XmlProperty;
import org.kisst.cordys.caas.util.XmlNode;


public abstract class CordysObject implements Comparable<CordysObject> {
	public final static String xmlns_monitor= "http://schemas.cordys.com/1.0/monitor";
	public final static String xmlns_ldap   = "http://schemas.cordys.com/1.0/ldap";
	public final static String xmlns_isv    = "http://schemas.cordys.com/1.0/isvpackage";
	public final static String xmlns_xmlstore="http://schemas.cordys.com/1.0/xmlstore";

	abstract public CordysSystem getSystem();
	public String getName() { return null; } 
	abstract public String getVarName();
	protected String prefix() { return ""; }

	abstract public String getKey(); 

	public int compareTo(CordysObject other) {
		if (other==null)
			return -1;
		return getKey().compareTo(other.getKey());
	}

	public boolean useCache() { return getSystem().useCache();}

	public Props<Object> getProps() { return new Props<Object>(this,null);}

	public void log(String msg) { System.out.println(msg); }
	public void myclear() {}
	public void clear() {
		for (CordysObjectList o: new Props<CordysObjectList>(this, CordysObjectList.class)) {
			o.clear();
		}
		myclear();
	}
	
	public Differences deepdiff(CordysObject other) { return diff(other,100); }
	public Differences diff(CordysObject other) { return diff(other,0); }
	public Differences diff(CordysObject other, int depth) { return diff(null,other,depth);}
	public Differences diff(Differences parent, CordysObject other, int depth) {
		if (this.getClass()!=other.getClass())
			throw new RuntimeException("Cannot diff two different classes "
					+this.getClass().getName()+" and "+other.getClass().getName());
		Differences diffs=new Differences(parent, "[\""+getName()+"\"]", this, other);
		Props<Object> p1=new Props<Object>(this, Object.class);
		Props<Object> p2=new Props<Object>(other, Object.class);
		for (String key : p1.keys()) {
			if (key.equals("dn") 
					|| key.equals("key") 
					|| key.equals("parent") 
					|| key.equals("system") 
					|| key.equals("varName"))
				continue;
			Object v1=p1.get(key);
			Object v2=p2.get(key);
			if (v1 instanceof XmlProperty)
				continue;
			if (v1 instanceof AbstractProperty) {
				v1=((AbstractProperty)v1).get();
				v2=((AbstractProperty)v2).get();
			}
			if (v1==v2) 
				continue;
			else if (v1 instanceof XmlNode) 
				continue;
			else if (v1 instanceof ChildList) {
				if (depth>0)
					diffs.addChildDiffs(((CordysObjectList)v1).diff(diffs, (CordysObjectList)v2, depth-1));
			}
			else if (v1 instanceof CordysObjectList)
				continue;
			else if (v1==null || ! v1.equals(v2))
				diffs.attributeDiffers(key, v1, v2);
		}
		return diffs;
	}

}
