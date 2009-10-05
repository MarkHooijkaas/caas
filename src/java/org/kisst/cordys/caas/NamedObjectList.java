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
import java.util.HashMap;
import java.util.Iterator;

public class NamedObjectList<T extends LdapObject> implements Iterable {
	private static final long serialVersionUID = 1L;
	private final ArrayList<T> list=new ArrayList<T>();
	private final HashMap<String,T> dnIndex=new HashMap<String,T>(); 
	private final HashMap<String,T> nameIndex=new HashMap<String,T>(); 
	
	public boolean add(T obj) {
		list.add(obj);
		dnIndex.put(obj.getDn(), obj);
		nameIndex.put(obj.getName(), obj);
		return true;
	}
	
	public String toString() { return toString("[\t",",\n\t","]"); }
	public String toString(String begin, String middle, String end) {
		StringBuffer result=new StringBuffer(begin);
		boolean first=true;
		for(Object o: this) {
			if (! first)
				result.append(middle);
			else
				first=false;
			result.append(o.toString());
		}
		result.append(end);
		return result.toString();
	}
	public Iterator<T> iterator() { return list.iterator(); }
	public T getAt(int index) {
		for(T obj: list) {
			if (index--<=0)
				return obj;
		}
		throw new IndexOutOfBoundsException("Index out of bounds");	
	}
	public T propertyMissing(String name) {	return get((Object)name); }

	public NamedObjectList <T> like(String expr) {
		expr=expr.toLowerCase();
		NamedObjectList <T> result=new NamedObjectList <T>();
		for(T obj: list) {
			if (obj.getName().toLowerCase().indexOf(expr)>=0)
				result.add(obj);
		}
		return result;	
	}
	@SuppressWarnings("unchecked")
	public T get(Object key) {
		//if (! (key instanceof String))
		//	return super.get(key);
		String name=((String) key);
		T result=dnIndex.get(name);
		if (result!=null)
			return result;
		result=nameIndex.get(name);
		if (result!=null)
			return result;
		name=name.toLowerCase();
		if (result==null) {
			for(T obj: list) {
				if (obj.getName().toLowerCase().indexOf(name)>=0)
					return obj;
			}
		}
		return null;
	}
}
