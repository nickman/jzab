/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2007, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package org.helios.jzab.agent.api.zobjects;

import org.json.JSONObject;

/**
 * <p>Title: ZItem</p>
 * <p>Description: Defines a Zabbix API Item</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.agent.api.ZItem</code></p>
 */

public class ZItem implements IZObject {
	/** The Item ID value */
	protected int itemId = -1;
	/** The Type value */
	protected int type = -1;
	/** The SNMP Community name value */
	protected String snmpCommunity = null;
	/** The SNMP OID value */
	protected String snmpoId = null;
	/** The Item custom port value */
	protected int port = -1;
	/** The Host ID value */
	protected int hostId = -1;
	/** The Item name value */
	protected String name = null;
	/** The Item key value */
	protected String key = null;
	/** The Check interval value */
	protected int delay = -1;
	/** The How long to keep item history (days) value */
	protected int history = -1;
	/** The How long to keep item trends (days) value */
	protected int trends = -1;
	/** The Last value value */
	protected String lastValue = null;
	/** The Last check value */
	protected int lastclock = -1;
	/** The Previous value value */
	protected String prevValue = null;
	/** The Item status value */
	protected int status = -1;
	/** The Value type value */
	protected int valueType = -1;
	/** The  value */
	protected String trapperHosts = null;
	/** The Value units value */
	protected String units = null;
	/** The Value multiplier value */
	protected int multiplier = -1;
	/** The Store values as delta value */
	protected int delta = -1;
	/** The  value */
	protected String prevorgValue = null;
	/** The SNMPv3 security name value */
	protected String snmpv3SecurityName = null;
	/** The SNMPv3 security level value */
	protected int snmpv3Securitylevel = -1;
	/** The SNMPv3 authentication phrase value */
	protected String snmpv3authpassphrase = null;
	/** The SNMPv3 private phrase value */
	protected String snmpv3privpassphrase = null;
	/** The  value */
	protected String formula = null;
	/** The Item check error value */
	protected String error = null;
	/** The Last log size value */
	protected int lastlogSize = -1;
	/** The Log time format value */
	protected String logTimefmt = null;
	/** The Parent item ID value */
	protected int templateId = -1;
	/** The Value map ID value */
	protected int valueMapId = -1;
	/** The Flexible delay value */
	protected String delayflex = null;
	/** The  value */
	protected String params = null;
	/** The IPMI sensor value */
	protected String ipmiSensor = null;
	/** The  value */
	protected int dataType = -1;
	/** The  value */
	protected int authType = -1;
	/** The  value */
	protected String userName = null;
	/** The  value */
	protected String password = null;
	/** The  value */
	protected String publicKey = null;
	/** The  value */
	protected String privateKey = null;
	/** The Micro time value */
	protected int mTime = -1;
	/** The Host interface ID value */
	protected int interfaceId = -1;
	/** The Item description value */
	protected String description = null;
	/** The Host inventory field number, that will be updated with the value returned by the item value */
	protected int inventoryLink = -1;


	/**
	 * Sets the Item ID 
	 * @param itemId the Item ID 
	 * @return this ZItem 
	 */ 
	public ZItem setItemId(int itemId) { 
		this.itemId = itemId;
		return this;
	}

	/**
	 * Gets the Item ID 
	 * @return the Item ID 
	 */
	public int getItemId() {
		return itemId;
	}

	/**
	 * Sets the Type 
	 * @param type the Type 
	 * @return this ZItem 
	 */ 
	public ZItem setType(int type) { 
		this.type = type;
		return this;
	}

	/**
	 * Gets the Type 
	 * @return the Type 
	 */
	public int getType() {
		return type;
	}

	/**
	 * Sets the SNMP Community name 
	 * @param snmpCommunity the SNMP Community name 
	 * @return this ZItem 
	 */ 
	public ZItem setSnmpCommunity(String snmpCommunity) { 
		this.snmpCommunity = snmpCommunity;
		return this;
	}

	/**
	 * Gets the SNMP Community name 
	 * @return the SNMP Community name 
	 */
	public String getSnmpCommunity() {
		return snmpCommunity;
	}

	/**
	 * Sets the SNMP OID 
	 * @param snmpoId the SNMP OID 
	 * @return this ZItem 
	 */ 
	public ZItem setSnmpoId(String snmpoId) { 
		this.snmpoId = snmpoId;
		return this;
	}

	/**
	 * Gets the SNMP OID 
	 * @return the SNMP OID 
	 */
	public String getSnmpoId() {
		return snmpoId;
	}

	/**
	 * Sets the Item custom port 
	 * @param port the Item custom port 
	 * @return this ZItem 
	 */ 
	public ZItem setPort(int port) { 
		this.port = port;
		return this;
	}

	/**
	 * Gets the Item custom port 
	 * @return the Item custom port 
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Sets the Host ID 
	 * @param hostId the Host ID 
	 * @return this ZItem 
	 */ 
	public ZItem setHostId(int hostId) { 
		this.hostId = hostId;
		return this;
	}

	/**
	 * Gets the Host ID 
	 * @return the Host ID 
	 */
	public int getHostId() {
		return hostId;
	}

	/**
	 * Sets the Item name 
	 * @param name the Item name 
	 * @return this ZItem 
	 */ 
	public ZItem setName(String name) { 
		this.name = name;
		return this;
	}

	/**
	 * Gets the Item name 
	 * @return the Item name 
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the Item key 
	 * @param key the Item key 
	 * @return this ZItem 
	 */ 
	public ZItem setKey(String key) { 
		this.key = key;
		return this;
	}

	/**
	 * Gets the Item key 
	 * @return the Item key 
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Sets the Check interval 
	 * @param delay the Check interval 
	 * @return this ZItem 
	 */ 
	public ZItem setDelay(int delay) { 
		this.delay = delay;
		return this;
	}

	/**
	 * Gets the Check interval 
	 * @return the Check interval 
	 */
	public int getDelay() {
		return delay;
	}

	/**
	 * Sets the How long to keep item history (days) 
	 * @param history the How long to keep item history (days) 
	 * @return this ZItem 
	 */ 
	public ZItem setHistory(int history) { 
		this.history = history;
		return this;
	}

	/**
	 * Gets the How long to keep item history (days) 
	 * @return the How long to keep item history (days) 
	 */
	public int getHistory() {
		return history;
	}

	/**
	 * Sets the How long to keep item trends (days) 
	 * @param trends the How long to keep item trends (days) 
	 * @return this ZItem 
	 */ 
	public ZItem setTrends(int trends) { 
		this.trends = trends;
		return this;
	}

	/**
	 * Gets the How long to keep item trends (days) 
	 * @return the How long to keep item trends (days) 
	 */
	public int getTrends() {
		return trends;
	}

	/**
	 * Sets the Last value 
	 * @param lastValue the Last value 
	 * @return this ZItem 
	 */ 
	public ZItem setLastValue(String lastValue) { 
		this.lastValue = lastValue;
		return this;
	}

	/**
	 * Gets the Last value 
	 * @return the Last value 
	 */
	public String getLastValue() {
		return lastValue;
	}

	/**
	 * Sets the Last check 
	 * @param lastclock the Last check 
	 * @return this ZItem 
	 */ 
	public ZItem setLastclock(int lastclock) { 
		this.lastclock = lastclock;
		return this;
	}

	/**
	 * Gets the Last check 
	 * @return the Last check 
	 */
	public int getLastclock() {
		return lastclock;
	}

	/**
	 * Sets the Previous value 
	 * @param prevValue the Previous value 
	 * @return this ZItem 
	 */ 
	public ZItem setPrevValue(String prevValue) { 
		this.prevValue = prevValue;
		return this;
	}

	/**
	 * Gets the Previous value 
	 * @return the Previous value 
	 */
	public String getPrevValue() {
		return prevValue;
	}

	/**
	 * Sets the Item status 
	 * @param status the Item status 
	 * @return this ZItem 
	 */ 
	public ZItem setStatus(int status) { 
		this.status = status;
		return this;
	}

	/**
	 * Gets the Item status 
	 * @return the Item status 
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * Sets the Value type 
	 * @param valueType the Value type 
	 * @return this ZItem 
	 */ 
	public ZItem setValueType(int valueType) { 
		this.valueType = valueType;
		return this;
	}

	/**
	 * Gets the Value type 
	 * @return the Value type 
	 */
	public int getValueType() {
		return valueType;
	}

	/**
	 * Sets the  
	 * @param trapperHosts the  
	 * @return this ZItem 
	 */ 
	public ZItem setTrapperHosts(String trapperHosts) { 
		this.trapperHosts = trapperHosts;
		return this;
	}

	/**
	 * Gets the  
	 * @return the  
	 */
	public String getTrapperHosts() {
		return trapperHosts;
	}

	/**
	 * Sets the Value units 
	 * @param units the Value units 
	 * @return this ZItem 
	 */ 
	public ZItem setUnits(String units) { 
		this.units = units;
		return this;
	}

	/**
	 * Gets the Value units 
	 * @return the Value units 
	 */
	public String getUnits() {
		return units;
	}

	/**
	 * Sets the Value multiplier 
	 * @param multiplier the Value multiplier 
	 * @return this ZItem 
	 */ 
	public ZItem setMultiplier(int multiplier) { 
		this.multiplier = multiplier;
		return this;
	}

	/**
	 * Gets the Value multiplier 
	 * @return the Value multiplier 
	 */
	public int getMultiplier() {
		return multiplier;
	}

	/**
	 * Sets the Store values as delta 
	 * @param delta the Store values as delta 
	 * @return this ZItem 
	 */ 
	public ZItem setDelta(int delta) { 
		this.delta = delta;
		return this;
	}

	/**
	 * Gets the Store values as delta 
	 * @return the Store values as delta 
	 */
	public int getDelta() {
		return delta;
	}

	/**
	 * Sets the  
	 * @param prevorgValue the  
	 * @return this ZItem 
	 */ 
	public ZItem setPrevorgValue(String prevorgValue) { 
		this.prevorgValue = prevorgValue;
		return this;
	}

	/**
	 * Gets the  
	 * @return the  
	 */
	public String getPrevorgValue() {
		return prevorgValue;
	}

	/**
	 * Sets the SNMPv3 security name 
	 * @param snmpv3SecurityName the SNMPv3 security name 
	 * @return this ZItem 
	 */ 
	public ZItem setSnmpv3SecurityName(String snmpv3SecurityName) { 
		this.snmpv3SecurityName = snmpv3SecurityName;
		return this;
	}

	/**
	 * Gets the SNMPv3 security name 
	 * @return the SNMPv3 security name 
	 */
	public String getSnmpv3SecurityName() {
		return snmpv3SecurityName;
	}

	/**
	 * Sets the SNMPv3 security level 
	 * @param snmpv3Securitylevel the SNMPv3 security level 
	 * @return this ZItem 
	 */ 
	public ZItem setSnmpv3Securitylevel(int snmpv3Securitylevel) { 
		this.snmpv3Securitylevel = snmpv3Securitylevel;
		return this;
	}

	/**
	 * Gets the SNMPv3 security level 
	 * @return the SNMPv3 security level 
	 */
	public int getSnmpv3Securitylevel() {
		return snmpv3Securitylevel;
	}

	/**
	 * Sets the SNMPv3 authentication phrase 
	 * @param snmpv3authpassphrase the SNMPv3 authentication phrase 
	 * @return this ZItem 
	 */ 
	public ZItem setSnmpv3authpassphrase(String snmpv3authpassphrase) { 
		this.snmpv3authpassphrase = snmpv3authpassphrase;
		return this;
	}

	/**
	 * Gets the SNMPv3 authentication phrase 
	 * @return the SNMPv3 authentication phrase 
	 */
	public String getSnmpv3authpassphrase() {
		return snmpv3authpassphrase;
	}

	/**
	 * Sets the SNMPv3 private phrase 
	 * @param snmpv3privpassphrase the SNMPv3 private phrase 
	 * @return this ZItem 
	 */ 
	public ZItem setSnmpv3privpassphrase(String snmpv3privpassphrase) { 
		this.snmpv3privpassphrase = snmpv3privpassphrase;
		return this;
	}

	/**
	 * Gets the SNMPv3 private phrase 
	 * @return the SNMPv3 private phrase 
	 */
	public String getSnmpv3privpassphrase() {
		return snmpv3privpassphrase;
	}

	/**
	 * Sets the  
	 * @param formula the  
	 * @return this ZItem 
	 */ 
	public ZItem setFormula(String formula) { 
		this.formula = formula;
		return this;
	}

	/**
	 * Gets the  
	 * @return the  
	 */
	public String getFormula() {
		return formula;
	}

	/**
	 * Sets the Item check error 
	 * @param error the Item check error 
	 * @return this ZItem 
	 */ 
	public ZItem setError(String error) { 
		this.error = error;
		return this;
	}

	/**
	 * Gets the Item check error 
	 * @return the Item check error 
	 */
	public String getError() {
		return error;
	}

	/**
	 * Sets the Last log size 
	 * @param lastlogSize the Last log size 
	 * @return this ZItem 
	 */ 
	public ZItem setLastlogSize(int lastlogSize) { 
		this.lastlogSize = lastlogSize;
		return this;
	}

	/**
	 * Gets the Last log size 
	 * @return the Last log size 
	 */
	public int getLastlogSize() {
		return lastlogSize;
	}

	/**
	 * Sets the Log time format 
	 * @param logTimefmt the Log time format 
	 * @return this ZItem 
	 */ 
	public ZItem setLogTimefmt(String logTimefmt) { 
		this.logTimefmt = logTimefmt;
		return this;
	}

	/**
	 * Gets the Log time format 
	 * @return the Log time format 
	 */
	public String getLogTimefmt() {
		return logTimefmt;
	}

	/**
	 * Sets the Parent item ID 
	 * @param templateId the Parent item ID 
	 * @return this ZItem 
	 */ 
	public ZItem setTemplateId(int templateId) { 
		this.templateId = templateId;
		return this;
	}

	/**
	 * Gets the Parent item ID 
	 * @return the Parent item ID 
	 */
	public int getTemplateId() {
		return templateId;
	}

	/**
	 * Sets the Value map ID 
	 * @param valueMapId the Value map ID 
	 * @return this ZItem 
	 */ 
	public ZItem setValueMapId(int valueMapId) { 
		this.valueMapId = valueMapId;
		return this;
	}

	/**
	 * Gets the Value map ID 
	 * @return the Value map ID 
	 */
	public int getValueMapId() {
		return valueMapId;
	}

	/**
	 * Sets the Flexible delay 
	 * @param delayflex the Flexible delay 
	 * @return this ZItem 
	 */ 
	public ZItem setDelayflex(String delayflex) { 
		this.delayflex = delayflex;
		return this;
	}

	/**
	 * Gets the Flexible delay 
	 * @return the Flexible delay 
	 */
	public String getDelayflex() {
		return delayflex;
	}

	/**
	 * Sets the  
	 * @param params the  
	 * @return this ZItem 
	 */ 
	public ZItem setParams(String params) { 
		this.params = params;
		return this;
	}

	/**
	 * Gets the  
	 * @return the  
	 */
	public String getParams() {
		return params;
	}

	/**
	 * Sets the IPMI sensor 
	 * @param ipmiSensor the IPMI sensor 
	 * @return this ZItem 
	 */ 
	public ZItem setIpmiSensor(String ipmiSensor) { 
		this.ipmiSensor = ipmiSensor;
		return this;
	}

	/**
	 * Gets the IPMI sensor 
	 * @return the IPMI sensor 
	 */
	public String getIpmiSensor() {
		return ipmiSensor;
	}

	/**
	 * Sets the  
	 * @param dataType the  
	 * @return this ZItem 
	 */ 
	public ZItem setDataType(int dataType) { 
		this.dataType = dataType;
		return this;
	}

	/**
	 * Gets the  
	 * @return the  
	 */
	public int getDataType() {
		return dataType;
	}

	/**
	 * Sets the  
	 * @param authType the  
	 * @return this ZItem 
	 */ 
	public ZItem setAuthType(int authType) { 
		this.authType = authType;
		return this;
	}

	/**
	 * Gets the  
	 * @return the  
	 */
	public int getAuthType() {
		return authType;
	}

	/**
	 * Sets the  
	 * @param userName the  
	 * @return this ZItem 
	 */ 
	public ZItem setUserName(String userName) { 
		this.userName = userName;
		return this;
	}

	/**
	 * Gets the  
	 * @return the  
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * Sets the  
	 * @param password the  
	 * @return this ZItem 
	 */ 
	public ZItem setPassword(String password) { 
		this.password = password;
		return this;
	}

	/**
	 * Gets the  
	 * @return the  
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Sets the  
	 * @param publicKey the  
	 * @return this ZItem 
	 */ 
	public ZItem setPublicKey(String publicKey) { 
		this.publicKey = publicKey;
		return this;
	}

	/**
	 * Gets the  
	 * @return the  
	 */
	public String getPublicKey() {
		return publicKey;
	}

	/**
	 * Sets the  
	 * @param privateKey the  
	 * @return this ZItem 
	 */ 
	public ZItem setPrivateKey(String privateKey) { 
		this.privateKey = privateKey;
		return this;
	}

	/**
	 * Gets the  
	 * @return the  
	 */
	public String getPrivateKey() {
		return privateKey;
	}

	/**
	 * Sets the Micro time 
	 * @param mTime the Micro time 
	 * @return this ZItem 
	 */ 
	public ZItem setMTime(int mTime) { 
		this.mTime = mTime;
		return this;
	}

	/**
	 * Gets the Micro time 
	 * @return the Micro time 
	 */
	public int getMTime() {
		return mTime;
	}

	/**
	 * Sets the Host interface ID 
	 * @param interfaceId the Host interface ID 
	 * @return this ZItem 
	 */ 
	public ZItem setInterfaceId(int interfaceId) { 
		this.interfaceId = interfaceId;
		return this;
	}

	/**
	 * Gets the Host interface ID 
	 * @return the Host interface ID 
	 */
	public int getInterfaceId() {
		return interfaceId;
	}

	/**
	 * Sets the Item description 
	 * @param description the Item description 
	 * @return this ZItem 
	 */ 
	public ZItem setDescription(String description) { 
		this.description = description;
		return this;
	}

	/**
	 * Gets the Item description 
	 * @return the Item description 
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the Host inventory field number, that will be updated with the value returned by the item 
	 * @param inventoryLink the Host inventory field number, that will be updated with the value returned by the item 
	 * @return this ZItem 
	 */ 
	public ZItem setInventoryLink(int inventoryLink) { 
		this.inventoryLink = inventoryLink;
		return this;
	}

	/**
	 * Gets the Host inventory field number, that will be updated with the value returned by the item 
	 * @return the Host inventory field number, that will be updated with the value returned by the item 
	 */
	public int getInventoryLink() {
		return inventoryLink;
	}

	/**
	 * Creates a ZItem from the passed json string
	 * @param json the json string 
	 * @return the created ZItem
	 */
	public static ZItem fromJSON(CharSequence json) {
		if(json==null || json.toString().trim().isEmpty()) throw new IllegalArgumentException("The passed JSON was null or empty", new Throwable());
		ZItem zitem = new ZItem();
		try {
			JSONObject jsonObj = new JSONObject(json.toString());
			if(jsonObj.has("itemid"))zitem.setItemId(jsonObj.getInt("itemid"));
			if(jsonObj.has("type"))zitem.setType(jsonObj.getInt("type"));
			if(jsonObj.has("snmp_community"))zitem.setSnmpCommunity(jsonObj.getString("snmp_community"));
			if(jsonObj.has("snmp_oid"))zitem.setSnmpoId(jsonObj.getString("snmp_oid"));
			if(jsonObj.has("port"))zitem.setPort(jsonObj.getInt("port"));
			if(jsonObj.has("hostid"))zitem.setHostId(jsonObj.getInt("hostid"));
			if(jsonObj.has("name"))zitem.setName(jsonObj.getString("name"));
			if(jsonObj.has("key_"))zitem.setKey(jsonObj.getString("key_"));
			if(jsonObj.has("delay"))zitem.setDelay(jsonObj.getInt("delay"));
			if(jsonObj.has("history"))zitem.setHistory(jsonObj.getInt("history"));
			if(jsonObj.has("trends"))zitem.setTrends(jsonObj.getInt("trends"));
			if(jsonObj.has("lastvalue"))zitem.setLastValue(jsonObj.getString("lastvalue"));
			if(jsonObj.has("lastclock"))zitem.setLastclock(jsonObj.getInt("lastclock"));
			if(jsonObj.has("prevvalue"))zitem.setPrevValue(jsonObj.getString("prevvalue"));
			if(jsonObj.has("status"))zitem.setStatus(jsonObj.getInt("status"));
			if(jsonObj.has("value_type"))zitem.setValueType(jsonObj.getInt("value_type"));
			if(jsonObj.has("trapper_hosts"))zitem.setTrapperHosts(jsonObj.getString("trapper_hosts"));
			if(jsonObj.has("units"))zitem.setUnits(jsonObj.getString("units"));
			if(jsonObj.has("multiplier"))zitem.setMultiplier(jsonObj.getInt("multiplier"));
			if(jsonObj.has("delta"))zitem.setDelta(jsonObj.getInt("delta"));
			if(jsonObj.has("prevorgvalue"))zitem.setPrevorgValue(jsonObj.getString("prevorgvalue"));
			if(jsonObj.has("snmpv3_securityname"))zitem.setSnmpv3SecurityName(jsonObj.getString("snmpv3_securityname"));
			if(jsonObj.has("snmpv3_securitylevel"))zitem.setSnmpv3Securitylevel(jsonObj.getInt("snmpv3_securitylevel"));
			if(jsonObj.has("snmpv3_authpassphrase"))zitem.setSnmpv3authpassphrase(jsonObj.getString("snmpv3_authpassphrase"));
			if(jsonObj.has("snmpv3_privpassphrase"))zitem.setSnmpv3privpassphrase(jsonObj.getString("snmpv3_privpassphrase"));
			if(jsonObj.has("formula"))zitem.setFormula(jsonObj.getString("formula"));
			if(jsonObj.has("error"))zitem.setError(jsonObj.getString("error"));
			if(jsonObj.has("lastlogsize"))zitem.setLastlogSize(jsonObj.getInt("lastlogsize"));
			if(jsonObj.has("logtimefmt"))zitem.setLogTimefmt(jsonObj.getString("logtimefmt"));
			if(jsonObj.has("templateid"))zitem.setTemplateId(jsonObj.getInt("templateid"));
			if(jsonObj.has("valuemapid"))zitem.setValueMapId(jsonObj.getInt("valuemapid"));
			if(jsonObj.has("delay_flex"))zitem.setDelayflex(jsonObj.getString("delay_flex"));
			if(jsonObj.has("params"))zitem.setParams(jsonObj.getString("params"));
			if(jsonObj.has("ipmi_sensor"))zitem.setIpmiSensor(jsonObj.getString("ipmi_sensor"));
			if(jsonObj.has("data_type"))zitem.setDataType(jsonObj.getInt("data_type"));
			if(jsonObj.has("authtype"))zitem.setAuthType(jsonObj.getInt("authtype"));
			if(jsonObj.has("username"))zitem.setUserName(jsonObj.getString("username"));
			if(jsonObj.has("password"))zitem.setPassword(jsonObj.getString("password"));
			if(jsonObj.has("publickey"))zitem.setPublicKey(jsonObj.getString("publickey"));
			if(jsonObj.has("privatekey"))zitem.setPrivateKey(jsonObj.getString("privatekey"));
			if(jsonObj.has("mtime"))zitem.setMTime(jsonObj.getInt("mtime"));
			if(jsonObj.has("interfaceid"))zitem.setInterfaceId(jsonObj.getInt("interfaceid"));
			if(jsonObj.has("description"))zitem.setDescription(jsonObj.getString("description"));
			if(jsonObj.has("inventory_link"))zitem.setInventoryLink(jsonObj.getInt("inventory_link"));			
			return zitem;
		} catch (Exception e) {
			throw new RuntimeException("Failed to create a ZItem from the passed JSON string", e);
		}		
	}
	
	/**
	 * Generates a JSON string for this ZItem
	 * @return a JSON string for this ZItem
	 */
	public String toJSON() {
		return toJSON(false, 0);
	}
	
	/**
	 * Generates a JSON string for this ZItem
	 * @param prettyprint true to pretty print, false to compress
	 * @param indent If pretty print is true, this is the indent factor
	 * @return @return a JSON string for this ZItem
	 */
	public String toJSON(boolean prettyprint, int indent) {
		try {
			JSONObject jsonObj = new JSONObject();
			if(itemId!=-1) jsonObj.put("itemid", itemId);
			if(type!=-1) jsonObj.put("type", type);
			if(snmpCommunity!=null) jsonObj.put("snmp_community", snmpCommunity);
			if(snmpoId!=null) jsonObj.put("snmp_oid", snmpoId);
			if(port!=-1) jsonObj.put("port", port);
			if(hostId!=-1) jsonObj.put("hostid", hostId);
			if(name!=null) jsonObj.put("name", name);
			if(key!=null) jsonObj.put("key_", key);
			if(delay!=-1) jsonObj.put("delay", delay);
			if(history!=-1) jsonObj.put("history", history);
			if(trends!=-1) jsonObj.put("trends", trends);
			if(lastValue!=null) jsonObj.put("lastvalue", lastValue);
			if(lastclock!=-1) jsonObj.put("lastclock", lastclock);
			if(prevValue!=null) jsonObj.put("prevvalue", prevValue);
			if(status!=-1) jsonObj.put("status", status);
			if(valueType!=-1) jsonObj.put("value_type", valueType);
			if(trapperHosts!=null) jsonObj.put("trapper_hosts", trapperHosts);
			if(units!=null) jsonObj.put("units", units);
			if(multiplier!=-1) jsonObj.put("multiplier", multiplier);
			if(delta!=-1) jsonObj.put("delta", delta);
			if(prevorgValue!=null) jsonObj.put("prevorgvalue", prevorgValue);
			if(snmpv3SecurityName!=null) jsonObj.put("snmpv3_securityname", snmpv3SecurityName);
			if(snmpv3Securitylevel!=-1) jsonObj.put("snmpv3_securitylevel", snmpv3Securitylevel);
			if(snmpv3authpassphrase!=null) jsonObj.put("snmpv3_authpassphrase", snmpv3authpassphrase);
			if(snmpv3privpassphrase!=null) jsonObj.put("snmpv3_privpassphrase", snmpv3privpassphrase);
			if(formula!=null) jsonObj.put("formula", formula);
			if(error!=null) jsonObj.put("error", error);
			if(lastlogSize!=-1) jsonObj.put("lastlogsize", lastlogSize);
			if(logTimefmt!=null) jsonObj.put("logtimefmt", logTimefmt);
			if(templateId!=-1) jsonObj.put("templateid", templateId);
			if(valueMapId!=-1) jsonObj.put("valuemapid", valueMapId);
			if(delayflex!=null) jsonObj.put("delay_flex", delayflex);
			if(params!=null) jsonObj.put("params", params);
			if(ipmiSensor!=null) jsonObj.put("ipmi_sensor", ipmiSensor);
			if(dataType!=-1) jsonObj.put("data_type", dataType);
			if(authType!=-1) jsonObj.put("authtype", authType);
			if(userName!=null) jsonObj.put("username", userName);
			if(password!=null) jsonObj.put("password", password);
			if(publicKey!=null) jsonObj.put("publickey", publicKey);
			if(privateKey!=null) jsonObj.put("privatekey", privateKey);
			if(mTime!=-1) jsonObj.put("mtime", mTime);
			if(interfaceId!=-1) jsonObj.put("interfaceid", interfaceId);
			if(description!=null) jsonObj.put("description", description);
			if(inventoryLink!=-1) jsonObj.put("inventory_link", inventoryLink);
			return jsonObj.toString();
		} catch (Exception e) {
			throw new RuntimeException("Failed to generate JSON for ZItem", e);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ZItem [itemId=");
		builder.append(itemId);
		builder.append(", type=");
		builder.append(type);
		builder.append(", ");
		if (snmpCommunity != null) {
			builder.append("snmpCommunity=");
			builder.append(snmpCommunity);
			builder.append(", ");
		}
		if (snmpoId != null) {
			builder.append("snmpoId=");
			builder.append(snmpoId);
			builder.append(", ");
		}
		builder.append("port=");
		builder.append(port);
		builder.append(", hostId=");
		builder.append(hostId);
		builder.append(", ");
		if (name != null) {
			builder.append("name=");
			builder.append(name);
			builder.append(", ");
		}
		if (key != null) {
			builder.append("key=");
			builder.append(key);
			builder.append(", ");
		}
		builder.append("delay=");
		builder.append(delay);
		builder.append(", history=");
		builder.append(history);
		builder.append(", trends=");
		builder.append(trends);
		builder.append(", ");
		if (lastValue != null) {
			builder.append("lastValue=");
			builder.append(lastValue);
			builder.append(", ");
		}
		builder.append("lastclock=");
		builder.append(lastclock);
		builder.append(", ");
		if (prevValue != null) {
			builder.append("prevValue=");
			builder.append(prevValue);
			builder.append(", ");
		}
		builder.append("status=");
		builder.append(status);
		builder.append(", valueType=");
		builder.append(valueType);
		builder.append(", ");
		if (trapperHosts != null) {
			builder.append("trapperHosts=");
			builder.append(trapperHosts);
			builder.append(", ");
		}
		if (units != null) {
			builder.append("units=");
			builder.append(units);
			builder.append(", ");
		}
		builder.append("multiplier=");
		builder.append(multiplier);
		builder.append(", delta=");
		builder.append(delta);
		builder.append(", ");
		if (prevorgValue != null) {
			builder.append("prevorgValue=");
			builder.append(prevorgValue);
			builder.append(", ");
		}
		if (snmpv3SecurityName != null) {
			builder.append("snmpv3SecurityName=");
			builder.append(snmpv3SecurityName);
			builder.append(", ");
		}
		builder.append("snmpv3Securitylevel=");
		builder.append(snmpv3Securitylevel);
		builder.append(", ");
		if (snmpv3authpassphrase != null) {
			builder.append("snmpv3authpassphrase=");
			builder.append(snmpv3authpassphrase);
			builder.append(", ");
		}
		if (snmpv3privpassphrase != null) {
			builder.append("snmpv3privpassphrase=");
			builder.append(snmpv3privpassphrase);
			builder.append(", ");
		}
		if (formula != null) {
			builder.append("formula=");
			builder.append(formula);
			builder.append(", ");
		}
		if (error != null) {
			builder.append("error=");
			builder.append(error);
			builder.append(", ");
		}
		builder.append("lastlogSize=");
		builder.append(lastlogSize);
		builder.append(", ");
		if (logTimefmt != null) {
			builder.append("logTimefmt=");
			builder.append(logTimefmt);
			builder.append(", ");
		}
		builder.append("templateId=");
		builder.append(templateId);
		builder.append(", valueMapId=");
		builder.append(valueMapId);
		builder.append(", ");
		if (delayflex != null) {
			builder.append("delayflex=");
			builder.append(delayflex);
			builder.append(", ");
		}
		if (params != null) {
			builder.append("params=");
			builder.append(params);
			builder.append(", ");
		}
		if (ipmiSensor != null) {
			builder.append("ipmiSensor=");
			builder.append(ipmiSensor);
			builder.append(", ");
		}
		builder.append("dataType=");
		builder.append(dataType);
		builder.append(", authType=");
		builder.append(authType);
		builder.append(", ");
		if (userName != null) {
			builder.append("userName=");
			builder.append(userName);
			builder.append(", ");
		}
		if (password != null) {
			builder.append("password=");
			builder.append(password);
			builder.append(", ");
		}
		if (publicKey != null) {
			builder.append("publicKey=");
			builder.append(publicKey);
			builder.append(", ");
		}
		if (privateKey != null) {
			builder.append("privateKey=");
			builder.append(privateKey);
			builder.append(", ");
		}
		builder.append("mTime=");
		builder.append(mTime);
		builder.append(", interfaceId=");
		builder.append(interfaceId);
		builder.append(", ");
		if (description != null) {
			builder.append("description=");
			builder.append(description);
			builder.append(", ");
		}
		builder.append("inventoryLink=");
		builder.append(inventoryLink);
		builder.append("]");
		return builder.toString();
	}

	

}
