/**
 * 
 */
package org.apache.vysper.xml.sax.impl;

public class Attribute {
	private String localName;
	private String uri;
	private String qname;
	private String value;
	public Attribute(String localName, String uri, String qname,
			String value) {
		this.localName = localName;
		this.uri = uri;
		this.qname = qname;
		this.value = value;
	}
	public String getLocalName() {
		return localName;
	}
	public String getURI() {
		return uri;
	}
	public String getQname() {
		return qname;
	}
	public String getValue() {
		return value;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((localName == null) ? 0 : localName.hashCode());
		result = prime * result + ((qname == null) ? 0 : qname.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Attribute other = (Attribute) obj;
		
		if (localName == null) {
			if (other.localName != null) return false;
		} else if (!localName.equals(other.localName)) return false;
		
		if (qname == null) {
			if (other.qname != null) return false;
		} else if (!qname.equals(other.qname)) return false;
		
		if (uri == null) {
			if (other.uri != null) return false;
		} else if (!uri.equals(other.uri)) return false;
		
		if (value == null) {
			if (other.value != null) return false;
		} else if (!value.equals(other.value)) return false;
		
		return true;
	}
	
	
}