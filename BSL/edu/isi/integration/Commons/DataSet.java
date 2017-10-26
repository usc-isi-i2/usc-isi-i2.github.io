package edu.isi.integration.Commons;

import java.io.*;
import java.util.*;
import java.sql.*;

public class DataSet {
    private String name; //way in which we reference the table
    private String url; //holds the url for the dataset
    private String login; //holds the login for the dataset
    private String pass;//holds hte password for the dataset
    private String tableName; //holds the name of the database table for the dataset
    private String tableId; //holds the identifying key for the table
    private Vector attributes;//holds teh names of all of the attributes
    private HashMap tableVals; //hashmap where key is tableId and value is the hashmap of attributes; This second hashmap uses the attribute name as key, and value is the attr value
    private String whereClause;//holds where clause if you need it
    private boolean unique; //done for selecting distinct attributes
    private String qString;//if you want to define your own query to retrieve the data
    private String recordCutOff; //for setting TOP 1000 records, or whatever
    private int size; //number of records...we keep it around so we don't have to keep figuring it out
    private Connection conn;
    private Statement stmt;

    public DataSet() {
	name = "";
	url = "";
	login = "";
	pass = "";
	tableName = "";
	tableId = "";
	whereClause = "";
	unique = false;
	qString = "";
	recordCutOff = "";
	attributes = new Vector();
	tableVals = new HashMap();
    }

    public boolean containsRecordId(String id) {
	if(tableVals.containsKey((String)id)) {
	    return true;
	}
	return false;
    }

    public void clear() {
	//System.out.println("CLEARING REF SET!!!");
	//attributes.clear();
	//blockingAttrs.clear();
	tableVals.clear();
    }

    public void reset() {
	tableVals.clear();
	setTableVals();
    }

    //accessor functs: set functs
    public void setName(String s) { name = s; }
    public void setURL(String s) { url = s; }
    public void setLogin(String s) { login = s; }
    public void setPass(String s) { pass = s; }
    public void setTableName(String s) { tableName = s; }
    public void setTableId(String s) { tableId = s; }
    public void setWhereClause(String s) {whereClause = s;}
    public void setUnique(boolean b) {unique = b;}
    public void setQString(String s) {qString = s;}
    public void setRecordCutOff(String s) {recordCutOff = s;}
    public void setAllAttrs(String input) {
	String toks[] = input.split("\\|");
	for(int i = 0; i < toks.length; i++) {
	    attributes.add((String)toks[i].trim());
	}
    }
    public void setAllAttrs(Vector input) {
	attributes = input;
    }

    public void addAttribute(String attrName) {
	attributes.add((String)attrName);
    }

    public void addAllAttributes(Vector input) {
	attributes.addAll((Vector)input);
    }

    //get functions
    public String getName() { return name; }
    public String getURL() { return url; }
    public String getPass() { return pass; }
    public String getLogin() { return login; }
    public String getTableName() { return tableName; }
    public String getTableId() { return tableId; }
    public String getWhereClause() { return whereClause;}
    public boolean getUnique() {return unique;}
    public String getQString() { return qString;}
    public String getRecordCutOff() { return recordCutOff; }
    public Vector getAttributes() { return attributes; }
    public HashMap getTableVals() { return tableVals; }
    public Connection getConnection() { return conn; }

    public Statement getStatement() { 
	/*try {
	    if(conn.isClosed()) {
		openConnection();
	    }
	}
	catch(Exception e) {
	    e.printStackTrace();
	    }*/
	return stmt; 
    }

    public void setSizeFromTable() {
	try {
	    Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
	    Connection con = DriverManager.getConnection(url,login,pass);
	    Statement stmt = con.createStatement();
	    String query = "SELECT COUNT (";
	    if(unique) {
		query += " DISTINCT ";
	    }
	    query += tableId+") as cnt FROM "+tableName;
	    if(whereClause.length() > 0) {
		query += " "+whereClause;
	    }
	    //System.out.println(query);
	    ResultSet rs = stmt.executeQuery(query);
	    while (rs.next()) {		
		int cnt = rs.getInt("cnt");
		size = cnt;
	    }
	    con.close();
	}
	catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public int getSize() {
	//return tableVals.size();
	return size;
    }

    public void insertAttributesFromOtherSet(DataSet otherSet) {
	String query = "";
	try {
	    Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
	    Connection con = DriverManager.getConnection(url,login,pass);
	    Statement stmt = con.createStatement();
	    query += "INSERT INTO "+tableName+" ("+tableId;
	    Vector attrs = otherSet.getAttributes();
	    for(int x = 0; x < attrs.size(); x++) {
		query += ","+(String)attrs.get(x);
	    }
	    query += ") SELECT "+otherSet.getTableId();
	    for(int z = 0; z < attrs.size(); z++) {
		query += ","+(String)attrs.get(z);
	    }
	    query += " FROM "+otherSet.getTableName();
	    stmt.executeUpdate(query);
	    //System.out.println(query);
	}
	catch (Exception e) {
	    System.out.println("FAILED: "+query);
	    e.printStackTrace();
	}
    }

    public void updateAttributesFromOtherSet(DataSet otherSet) {
	String query = "";
	try {
	    Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
	    Connection con = DriverManager.getConnection(url,login,pass);
	    Statement stmt = con.createStatement();

	    //UPDATE postsCars_TEMPTEST_FINAL_SET 
	    //set make = (select make from postsCars_TEMPTEST o where o.recordid=postsCars_TEMPTEST_FINAL_SET.recordid),
	    //make = (select make from postsCars_TEMPTEST o where o.recordid=postsCars_TEMPTEST_FINAL_SET.recordid)


	    query += "UPDATE "+tableName+" SET ";
	    Vector attrs = otherSet.getAttributes();
	    for(int x = 0; x < attrs.size(); x++) {
		String cAtt = (String)attrs.get(x);
		query += cAtt+" = (SELECT DISTINCT "+cAtt+" FROM "+otherSet.getTableName()+" WHERE "+otherSet.getTableName()+"."+otherSet.getTableId()+"="+tableName+"."+tableId+")";
		if(x < attrs.size() - 1) {
		    query += ",";
		}
	    }
	    stmt.executeUpdate(query);
	    // System.out.println(query);
	}
	catch (Exception e) {
	    System.out.println("FAILED: "+query);
	    e.printStackTrace();
	}
    }

    public void setTableVals() {
	String query = "";
	tableVals.clear();
    	try {
	    Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
	    //System.out.println("URL IS: "+url);
	    Connection con = DriverManager.getConnection(url.trim(),login,pass);
	    Statement stmt = con.createStatement();
	    query = "SELECT ";
	    if(unique) {
		query += " DISTINCT ";
	    }
	    if(recordCutOff.length() > 0) {
		query += " "+recordCutOff+" ";
	    }
	    query +=tableId+", ";
	    for(int i = 0; i < attributes.size(); i++) {
		query += (String)attributes.get(i)+", ";
	    }
	    query += " FROM "+tableName;
	    query = query.replaceAll(",  FROM", " FROM");
	    if(whereClause.length() > 0) {
		query += " "+whereClause;
	    }
	    //System.out.println(query);
	    ResultSet rs = stmt.executeQuery(query);
	    while (rs.next()) {
		HashMap hm = new HashMap();
		String id = rs.getString(tableId);
		for(int j = 0; j < attributes.size(); j++) {
		    String s = (String)attributes.get(j);
		    String t = rs.getString(s);
		    if(t == null) { t = ""; }
		    hm.put((String)s, (String)t);
		}
		tableVals.put((String)id, (HashMap)hm);
	    }
	    con.close();
	}
	catch (Exception e) {
	    e.printStackTrace();
	    System.out.println("FAILED ON THIS CRAPPY QUERY: "+query);
	}
	size = tableVals.size();
    }

    public int countNumRefs() {
	int count = 0;
   	try {
	    Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
	    Connection con = DriverManager.getConnection(url,login,pass);
	    Statement stmt = con.createStatement();
	    String query = "SELECT COUNT(";
	    if(unique) {
		query += "DISTINCT ";
	    }
	    query += tableId+") as cnt ";
	    query += " FROM "+tableName;
	    if(recordCutOff.length() > 0) {
		query = "SELECT "+recordCutOff+" ("+tableId+") as cnt ";
		query += " FROM "+tableName;
	    }
	    query = query.replaceAll(",  FROM", " FROM");
	    if(whereClause.length() > 0) {
		query += " "+whereClause;
	    }

	    ResultSet rs = stmt.executeQuery(query);	    
	    while (rs.next()) {
		String c = rs.getString("cnt");
		count = (new Integer(c)).intValue();
	    }
	    con.close();
	}
	catch (Exception e) {
	    e.printStackTrace();
	}
	return count;
    }

    public void removeTableVals(Vector ids) {
	//method to remove from the hashmap the ids in the vector
	for(int i = 0; i < ids.size(); i++) {
	    String id = (String)ids.get(i);
	    tableVals.remove((String)id);
	}
    }

    public void removeRecord(String id) {
	tableVals.remove((String)id);
    }
    
    public Vector getRecordIds() {
	Vector ids = new Vector();
	Set keys = tableVals.keySet();
	Iterator iter = keys.iterator();
	while (iter.hasNext()) {
	    ids.add((String)iter.next());
	}
	return ids;
    }

    public HashMap getRecord(String id) {
	return (HashMap)tableVals.get((String)id);
    }

    public String toString() {
	String s = "";
	s += "NAME: "+name+"\n";
	s += "URL: "+url+"\n";
	s += "LOGIN: "+login+"\n";
	s += "PASS: "+pass+"\n";
	s += "TABLE NAME: "+tableName+"\n";
	s += "TABLE ID: "+tableId+"\n";
	s += "ATTRS: "+attributes+"\n";
	s += "THERE ARE "+size+" RECORDS IN THIS DATA SET\n";
	return s;
    }

    public void openConnection() {
	try {
	    Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
	    //if(conn.isClosed()) {
	    conn = DriverManager.getConnection(url,login,pass);
	    stmt = conn.createStatement();
		// }
	}
	catch(Exception e) {
	    e.printStackTrace();
	}
    }

    public void closeConnection() {
	try {
	    Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
	    //if(!conn.isClosed()) {
	    
	    //conn.commit();
	    stmt.close();
	    conn.close();
		// }
	}
	catch(Exception e) {
	    e.printStackTrace();
	}
    }

}