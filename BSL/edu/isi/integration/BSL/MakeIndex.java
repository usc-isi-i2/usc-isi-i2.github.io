package edu.isi.integration.BSL;

import java.io.*;
import java.sql.*;
import java.util.*;
import edu.isi.integration.Commons.DataSet;

public class MakeIndex {

    public void makeToMatchIndex(DataSet set1, Vector attributes, Vector methods) {
	//first thing is to make the attribute/method tables for the first dataset
	//in the indexTable and make indexes on these tables
	createAttrMethodTables(methods, set1);
	buildAttrMethodIndexTables(set1,methods);
    }

    public void makeBlockSchemeIndexes(DataSet set1, DataSet set2, Vector blockingScheme) {
	//we only need to make the indexes for the stuff in the blocking scheme...
	//next, we make the attribute/method tables for the second dataset in the index table
	//and make indexes on these tables
	Vector attrMethPairs = getUniqueAttrMethodPairs(blockingScheme);
	createAttrMethodTablesUsingScheme(attrMethPairs, set2);
	buildAttrMethodIndexTablesUsingScheme(set2,attrMethPairs);

	//now we construct tables of candidates using the matches between attribute/method indexes
	//and we make indexes on these candidate tables
	//System.out.println("START BUILDING CANDIDATE INDEXES...");
	//createCandidateIndexTables(set1, set2, blockingScheme);
	createCandidateIndexTables(set1, set2, attrMethPairs);
	//System.out.println("FINISHED BUILDING CANDIDATE INDEXES...");
    }

    private void createAttrMethodTablesUsingScheme(Vector scheme, DataSet dataTable) {
	String q = "";
	try {
	    dataTable.openConnection();
	    for(int x = 0; x < scheme.size(); x++) {		
		String currPair[] = ((String)scheme.get(x)).split("\\|");
		String currAttr = currPair[0];
		String currMeth = currPair[1];
		String tbName = dataTable.getTableName()+"_"+currAttr+"_"+currMeth;
		DatabaseMetaData dmd = (dataTable.getConnection()).getMetaData();
		String dbType = dmd.getDatabaseProductName();
		if(dbType.equals("Microsoft SQL Server")) {
		    q = "CREATE TABLE "+tbName+" (token varchar(255), "+dataTable.getTableId()+" varchar(255))";
		}
		else {
		    q = "CREATE TABLE "+tbName+" (token text, "+dataTable.getTableId()+" text)";
		}
		//System.out.println("CREATED TABLE: "+q);
		(dataTable.getStatement()).executeUpdate(q);
	    }
	    dataTable.closeConnection();	    
	}
	catch(Exception e) {
	    e.printStackTrace();
	    System.out.println("FAILED ON: "+q);
	}
    }

    private void buildAttrMethodIndexTablesUsingScheme(DataSet data, Vector scheme) {
	data.openConnection();
	//now, iterate through records and apply blocking keys - (methd, attr)
	//to each record...
	String query = "SELECT "+data.getTableId()+",";
	Vector dataTableAttrs = new Vector();
	HashMap attrMethods = new HashMap(); //this way, we hold the methods associated with each attr, for easy access later
	for(int i = 0; i < scheme.size(); i++) {	    
	    String currPair[] = ((String)scheme.get(i)).split("\\|");
	    String currAttr = currPair[0];
	    String currMeth = currPair[1];
	    if(!dataTableAttrs.contains((String)currAttr)) {
		dataTableAttrs.add((String)currAttr);
		query += currAttr+",";
		Vector v = new Vector();
		v.add((String)currMeth);
		attrMethods.put((String)currAttr, (Vector)v);
	    }
	    else {
		Vector v = (Vector)attrMethods.get((String)currAttr);
		v.add((String)currMeth);
		attrMethods.remove((String)currAttr);
		attrMethods.put((String)currAttr, (Vector)v);
	    }
	}
	query += " FROM "+data.getTableName();
	query = query.replaceAll(", FROM"," FROM");
	//System.out.println("BEGIN BUILDING INDEX...");
	try {
	    ResultSet rs = (data.getStatement()).executeQuery(query);
	    Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
	    Connection tempCon = DriverManager.getConnection(data.getURL(),data.getLogin(),data.getPass());
	    while (rs.next()) {
		String id = rs.getString(data.getTableId());
		for(int x = 0; x < dataTableAttrs.size(); x++) {
		    String currAttr = (String)dataTableAttrs.get(x);
		    //System.out.print(currAttr+" -- ");
		    String s = rs.getString(currAttr);
		    //System.out.println(s);
		    Vector methods = (Vector)attrMethods.get((String)currAttr);
		    for(int z = 0; z < methods.size(); z++) {
			String m = (String)methods.get(z);
			//now insert these into the index
			Vector res = TokenMethod.applyMethod(m, s);
			//now insert them
			for(int i = 0; i < res.size(); i++) {
			    insertIndex((String)res.get(i),id,currAttr,m,data,tempCon);
			}
		    }
		}
	    }
	    tempCon.close();
	}
	catch(Exception e) {
	    System.out.println("FAILED ON "+query);
	    e.printStackTrace();
	}

	//now create an index on them for fast searching
	makeAttributeIndex(data, attrMethods);
	//System.out.println("INDEX HAS BEEN BUILT ...");
	//then close all database connections
	data.closeConnection();
    }

    public void dropAttrMethodTable(DataSet dataTable, Vector blockingScheme) {
	Vector parts = getUniqueAttrMethodPairs(blockingScheme);
	try {
	    dataTable.openConnection();

	    for(int x = 0; x < parts.size(); x++) {
		String currPair[] = ((String)parts.get(x)).split("\\|");
		String attr = currPair[0];
		String meth = currPair[1];
		String tbName = dataTable.getTableName()+"_"+attr+"_"+meth;
		String q = "DROP TABLE "+tbName;
		//System.out.println("DROP TABLE: "+q);
		(dataTable.getStatement()).executeUpdate(q);
	    }
	    dataTable.closeConnection();	    
	}
	catch(Exception e) {
	    e.printStackTrace();
	}
    }

    public void dropCandTables(Vector blockingScheme, DataSet dataTable) {
	Vector parts = getUniqueAttrMethodPairs(blockingScheme);
	try {
	    dataTable.openConnection();
	    for(int x = 0; x < parts.size(); x++) {		
		String currPair[] = ((String)parts.get(x)).split("\\|");
		String attr = currPair[0];
		String meth = currPair[1];
		String tbName = attr+"_"+meth+"_CANDS ";
		String q = "DROP TABLE "+tbName;
		//System.out.println("DROP TABLE: "+q);
		(dataTable.getStatement()).executeUpdate(q);
	    }
	    dataTable.closeConnection();	    
	}
	catch(Exception e) {
	    e.printStackTrace();
	}
    }

    public void makeForMatchIndex(DataSet set1, DataSet set2,Vector attributes, Vector methods) {
	//next, we make the attribute/method tables for the second dataset in the index table
	//and make indexes on these tables
	createAttrMethodTables(methods, set2);
	buildAttrMethodIndexTables(set2,methods);

	//now we construct tables of candidates using the matches between attribute/method indexes
	//and we make indexes on these candidate tables
	//System.out.println("START BUILDING CANDIDATE INDEXES...");
	createCandidateIndexTables(set1, set2, attributes, methods);
	//System.out.println("FINISHED BUILDING CANDIDATE INDEXES...");

	//we can delete the attribute/method tables now

	//now we have all of the data so we can just do joins to get candidates for a rule.

    }

    private void buildAttrMethodIndexTables(DataSet data, Vector methods) {
	data.openConnection();
	//now, iterate through records and apply blocking keys - (methd, attr)
	//to each record...
	String query = "SELECT "+data.getTableId()+",";
	Vector dataTableAttrs = data.getAttributes();
	for(int i = 0; i < dataTableAttrs.size(); i++) {
	    query += (String)dataTableAttrs.get(i)+",";
	}
	query += " FROM "+data.getTableName();
	query = query.replaceAll(", FROM"," FROM");
	//System.out.println("BEGIN BUILDING INDEX...");
	try {
	    ResultSet rs = (data.getStatement()).executeQuery(query);
	    Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
	    Connection tempCon = DriverManager.getConnection(data.getURL(),data.getLogin(),data.getPass());
	    while (rs.next()) {
		String id = rs.getString(data.getTableId());
		for(int x = 0; x < dataTableAttrs.size(); x++) {
		    String currAttr = (String)dataTableAttrs.get(x);
		    //System.out.print(currAttr+" -- ");
		    String s = rs.getString(currAttr);
		    //System.out.println(s);
		    for(int z = 0; z < methods.size(); z++) {
			String m = (String)methods.get(z);
			//now insert these into the index
			Vector res = TokenMethod.applyMethod(m, s);
			//now insert them
			for(int i = 0; i < res.size(); i++) {
			    insertIndex((String)res.get(i),id,currAttr,m,data,tempCon);
			}
		    }
		}
	    }
	    tempCon.close();
	}
	catch(Exception e) {
	    System.out.println(query);
	    e.printStackTrace();
	}

	//now create an index on them for fast searching
	makeAttributeIndex(data,methods);
	//System.out.println("INDEX HAS BEEN BUILT ...");
	//then close all database connections
	data.closeConnection();
    }

    private void createAttrMethodTables(Vector methods, DataSet dataTable) {
	try {
	    dataTable.openConnection();
	    Vector dataTableAttrs = dataTable.getAttributes();
	    for(int x = 0; x < dataTableAttrs.size(); x++) {
		for(int y = 0; y < methods.size(); y++) {		
		    String tbName = dataTable.getTableName()+"_"+(String)dataTableAttrs.get(x)+"_"+(String)methods.get(y);
		    String q = "";
		    DatabaseMetaData dmd = (dataTable.getConnection()).getMetaData();
		    String dbType = dmd.getDatabaseProductName();
		    if(dbType.equals("Microsoft SQL Server")) {
			q = "CREATE TABLE "+tbName+" (token varchar(255), "+dataTable.getTableId()+" varchar(255))";
		    }
		    else {
			q = "CREATE TABLE "+tbName+" (token text, "+dataTable.getTableId()+" text)";
		    }
		    //System.out.println("CREATED TABLE: "+q);
		    (dataTable.getStatement()).executeUpdate(q);
		}
	    }
	    dataTable.closeConnection();	    
	}
	catch(Exception e) {
	    e.printStackTrace();
	}
    }

    private void insertIndex(String input, String id, String attr, String meth, DataSet dataTable, Connection con) { 
	String q = "";
	try {
	    DatabaseMetaData dmd = (dataTable.getConnection()).getMetaData();
	    String dbType = dmd.getDatabaseProductName();
	    if(dbType.equalsIgnoreCase("access") || dbType.equals("Microsoft SQL Server")) {
		input = input.replaceAll("'","''");
	    }
	    if(dbType.equalsIgnoreCase("mysql")) {
		input = input.replaceAll("'","\\\\'");
	    }
	    input = input.toLowerCase();
	    String tbName = dataTable.getTableName()+"_"+attr+"_"+meth;
	    q = "INSERT INTO "+tbName+"(token,"+dataTable.getTableId()+") VALUES ";
	    q += "('"+input+"','"+id+"')";
	    Statement st = con.createStatement();
	    st.executeUpdate(q);
	}
	catch(Exception e) {
	    e.printStackTrace();
	    System.out.println("FAILED ON: "+q);
	}
    }

    private void makeAttributeIndex(DataSet dataTable, HashMap attrMeths) {
	//need to make an index on token, attribute so we can search them fast
	String q = "";
	try {
	    DatabaseMetaData dmd = (dataTable.getConnection()).getMetaData();
	    String dbType = dmd.getDatabaseProductName();
	    Iterator iterator = attrMeths.keySet().iterator();
	    while (iterator.hasNext()) {
		String attr = (String)iterator.next();
		Vector methods = (Vector)attrMeths.get(attr);	 
		for(int y = 0;  y < methods.size(); y++) {
		    String tbMA = dataTable.getTableName()+"_"+attr+"_"+(String)methods.get(y);
		    if(dbType.equalsIgnoreCase("access") || dbType.equals("Microsoft SQL Server")) {
			//if(dbType.equalsIgnoreCase("access")) {
			q = "CREATE INDEX "+tbMA+"indexTbl ON "+tbMA+" (token)";
		    }
		    if(dbType.equalsIgnoreCase("mysql")) {
			q = "CREATE INDEX "+tbMA+"indexTbl ON "+tbMA+" (token(50))";
		    }
		    (dataTable.getStatement()).executeUpdate(q);
		}
	    }
	}
	catch(Exception e) {
	    e.printStackTrace();
	    System.out.println("FAILED ON: "+q);
	}
    }

    private void makeAttributeIndex(DataSet dataTable, Vector methods) {
	//need to make an index on token, attribute so we can search them fast
	String q = "";
	try {
	    DatabaseMetaData dmd = (dataTable.getConnection()).getMetaData();
	    String dbType = dmd.getDatabaseProductName();
	    Vector dataTableAttrs = dataTable.getAttributes();
	    for(int x = 0; x < dataTableAttrs.size(); x++) {
		for(int y = 0;  y < methods.size(); y++) {
		    String tbMA = dataTable.getTableName()+"_"+(String)dataTableAttrs.get(x)+"_"+(String)methods.get(y);
		    if(dbType.equalsIgnoreCase("access") || dbType.equals("Microsoft SQL Server")) {
			//if(dbType.equalsIgnoreCase("access")) {
			q = "CREATE INDEX "+tbMA+"indexTbl ON "+tbMA+" (token)";
		    }
		    if(dbType.equalsIgnoreCase("mysql")) {
			q = "CREATE INDEX "+tbMA+"indexTbl ON "+tbMA+" (token(50))";
		    }
		    (dataTable.getStatement()).executeUpdate(q);
		}
	    }
	}
	catch(Exception e) {
	    e.printStackTrace();
	    System.out.println("FAILED ON: "+q);
	}
    }

    private void createIndexTables(Vector blockingScheme, DataSet set1, DataSet set2) {
	try {
	    set1.openConnection();
	    String q = "";
	    for(int x = 0; x < blockingScheme.size(); x++) {
		String currPair[] = ((String)blockingScheme.get(x)).split("\\|");
		String attr = currPair[0];
		String method = currPair[1];
		String tbName = attr+"_"+method+"_CANDS ";
		DatabaseMetaData dmd = (set1.getConnection()).getMetaData();
		String dbType = dmd.getDatabaseProductName();
		if(dbType.equals("Microsoft SQL Server")) {
		    q = "CREATE TABLE "+tbName+" ("+set1.getTableId()+" varchar(255),"+set2.getTableId()+" varchar(255))";
		}
		else {
		    q = "CREATE TABLE "+tbName+" ("+set1.getTableId()+" text,"+set2.getTableId()+" text)";
		}
		(set1.getStatement()).executeUpdate(q);
	    }
	    set1.closeConnection();
	}
	catch(Exception e) {
	    e.printStackTrace();
	}
    }

    private void createIndexTables(Vector methods, Vector attributes, DataSet set1, DataSet set2) {
	try {
	    set1.openConnection();
	    String q = "";
	    for(int x = 0; x < attributes.size(); x++) {
		for(int y = 0; y < methods.size(); y++) {		
		    String tbName = (String)attributes.get(x)+"_"+(String)methods.get(y)+"_CANDS ";
		    DatabaseMetaData dmd = (set1.getConnection()).getMetaData();
		    String dbType = dmd.getDatabaseProductName();
		    if(dbType.equals("Microsoft SQL Server")) {
			q = "CREATE TABLE "+tbName+" ("+set1.getTableId()+" varchar(255),"+set2.getTableId()+" varchar(255))";
		    }
		    else {
			q = "CREATE TABLE "+tbName+" ("+set1.getTableId()+" text,"+set2.getTableId()+" text)";
		    }
		    //System.out.println("CREATED TABLE: "+q);
		    (set1.getStatement()).executeUpdate(q);
		}
	    }
	    set1.closeConnection();	    
	}
	catch(Exception e) {
	    e.printStackTrace();
	}
    }

    private void makeCandIndexes(DataSet set1, DataSet set2, Vector blockingScheme) {
	//need to make an index on token, attribute so we can search them fast
	String q = "";
	try {
	    DatabaseMetaData dmd = (set1.getConnection()).getMetaData();
	    String dbType = dmd.getDatabaseProductName();
	    for(int x = 0; x < blockingScheme.size(); x++) {
		String currPair[] = ((String)blockingScheme.get(x)).split("\\|");
		String attr = currPair[0];
		String meth = currPair[1];
		String tbMA = attr+"_"+meth+"_CANDS";
		if(dbType.equalsIgnoreCase("access") || dbType.equals("Microsoft SQL Server")) {
		    //if(dbType.equalsIgnoreCase("access")) {
		    q = "CREATE INDEX "+tbMA+"indexTbl ON "+tbMA+" ("+set1.getTableId()+","+set2.getTableId()+")";
		}
		if(dbType.equalsIgnoreCase("mysql")) {
		    q = "CREATE INDEX "+tbMA+"indexTbl ON "+tbMA+" ("+set1.getTableId()+"(50),"+set2.getTableId()+"(50))";
		}
		(set1.getStatement()).executeUpdate(q);
	    }
	}
	catch(Exception e) {
	    e.printStackTrace();
	    System.out.println("FAILED ON: "+q);
	}
    }

    private void makeCandIndexes(DataSet set1, DataSet set2,  
				 Vector attributes, Vector methods) {
	//need to make an index on token, attribute so we can search them fast
	String q = "";
	try {
	    DatabaseMetaData dmd = (set1.getConnection()).getMetaData();
	    String dbType = dmd.getDatabaseProductName();
	    for(int x = 0; x < attributes.size(); x++) {
		for(int y = 0;  y < methods.size(); y++) {
		    String tbMA = (String)attributes.get(x)+"_"+(String)methods.get(y)+"_CANDS";
		    if(dbType.equalsIgnoreCase("access") || dbType.equals("Microsoft SQL Server")) {
			//if(dbType.equalsIgnoreCase("access")) {
			q = "CREATE INDEX "+tbMA+"indexTbl ON "+tbMA+" ("+set1.getTableId()+","+set2.getTableId()+")";
		    }
		    if(dbType.equalsIgnoreCase("mysql")) {
			q = "CREATE INDEX "+tbMA+"indexTbl ON "+tbMA+" ("+set1.getTableId()+"(50),"+set2.getTableId()+"(50))";
		    }
		    (set1.getStatement()).executeUpdate(q);
		}
	    }
	}
	catch(Exception e) {
	    e.printStackTrace();
	    System.out.println("FAILED ON: "+q);
	}
    }

    private void createCandidateIndexTables(DataSet set1, DataSet set2, Vector blockingScheme) {
	String q = "";
	try {	    
	    //first we create the tables
	    createIndexTables(blockingScheme, set1, set2);

	    //now we fill em up!
	    set1.openConnection();
	    for(int i = 0; i < blockingScheme.size(); i++) {
		String currPair[] = ((String)blockingScheme.get(i)).split("\\|");
		String attribute = currPair[0];
		String method = currPair[1];
		String attrMeth = attribute+"_"+method;
		q = "INSERT INTO "+attrMeth+"_CANDS ";
		q += "SELECT DISTINCT "+set1.getTableName()+"_"+attrMeth+"."+set1.getTableId()+", ";
		q += set2.getTableName()+"_"+attrMeth+"."+set2.getTableId()+" FROM ";
		q += set1.getTableName()+"_"+attrMeth+", "+set2.getTableName()+"_"+attrMeth+" ";
		q += " WHERE "+set1.getTableName()+"_"+attrMeth+".token="+set2.getTableName()+"_"+attrMeth+".token";
		//System.out.println(q);
		(set1.getStatement()).executeUpdate(q);
	    }

	    makeCandIndexes(set1, set2, blockingScheme);

	    //now we need to make the indexes on them
	    set1.closeConnection();
	}
	catch(Exception e) {
	    e.printStackTrace();
	    System.out.println("FAILED ON: "+q);
	}
    }
    
    private void createCandidateIndexTables(DataSet set1, DataSet set2, 
					    Vector attributes, Vector methods) {
	String q = "";
	try {	    
	    //first we create the tables
	    createIndexTables(methods, attributes, set1, set2);
	    
	    //now we fill em up!
	    set1.openConnection();
	    for(int i = 0; i < attributes.size(); i++) {
		for(int j = 0; j < methods.size(); j++) {
		    String method = (String)methods.get(j);
		    String attribute = (String)attributes.get(i);
		    String attrMeth = attribute+"_"+method;
		    q = "INSERT INTO "+attrMeth+"_CANDS ";
		    q += "SELECT DISTINCT "+set1.getTableName()+"_"+attrMeth+"."+set1.getTableId()+", ";
		    q += set2.getTableName()+"_"+attrMeth+"."+set2.getTableId()+" FROM ";
		    q += set1.getTableName()+"_"+attrMeth+", "+set2.getTableName()+"_"+attrMeth+" ";
		    q += " WHERE "+set1.getTableName()+"_"+attrMeth+".token="+set2.getTableName()+"_"+attrMeth+".token";
		    (set1.getStatement()).executeUpdate(q);
		}
	    }

	    makeCandIndexes(set1, set2, attributes, methods);

	    //now we need to make the indexes on them
	    set1.closeConnection();
	}
	catch(Exception e) {
	    e.printStackTrace();
	    System.out.println("FAILED ON: "+q);
	}
    }


    public void dropAttrMethodTable(DataSet dataTable,
				      Vector methods, Vector attrs) {
	try {
	    dataTable.openConnection();
	    Vector dataTableAttrs = dataTable.getAttributes();
	    for(int x = 0; x < dataTableAttrs.size(); x++) {
		for(int y = 0; y < methods.size(); y++) {		
		    String tbName = dataTable.getTableName()+"_"+(String)dataTableAttrs.get(x)+"_"+(String)methods.get(y);
		    String q = "DROP TABLE "+tbName;
		    //System.out.println("DROP TABLE: "+q);
		    (dataTable.getStatement()).executeUpdate(q);
		}
	    }
	    dataTable.closeConnection();	    
	}
	catch(Exception e) {
	    e.printStackTrace();
	}
    }

    public void dropCandTables(Vector methods, Vector attributes, DataSet dataTable) {
	try {
	    dataTable.openConnection();
	    for(int x = 0; x < attributes.size(); x++) {
		for(int y = 0; y < methods.size(); y++) {		
		    String tbName = (String)attributes.get(x)+"_"+(String)methods.get(y)+"_CANDS ";
		    String q = "DROP TABLE "+tbName;
		    //System.out.println("DROP TABLE: "+q);
		    (dataTable.getStatement()).executeUpdate(q);
		}
	    }
	    dataTable.closeConnection();	    
	}
	catch(Exception e) {
	    e.printStackTrace();
	}
    }

    //since we remove true positives that we cover from the matches table, we need to make a copy of it
    //then, when we need to, we can just reload the copied table into the modified one
    public void copyMatchesTable(DataSet set1, DataSet set2, DataSet matchTable) {
	String q = "";
	try {
	    matchTable.openConnection();
	    DatabaseMetaData dmd = (matchTable.getConnection()).getMetaData();
	    String dbType = dmd.getDatabaseProductName();
	    if(dbType.equals("Microsoft SQL Server")) {
		q = "CREATE TABLE "+matchTable.getTableName()+"_COPY ("+set1.getTableId()+" varchar(255), "+set2.getTableId()+" varchar(255))";		
	    }
	    else {
		q = "CREATE TABLE "+matchTable.getTableName()+"_COPY ("+set1.getTableId()+" text, "+set2.getTableId()+" text)";		
	    }
	    (matchTable.getStatement()).executeUpdate(q);
	    q = "INSERT INTO "+matchTable.getTableName()+"_COPY SELECT "+set1.getTableId()+","+set2.getTableId()+" FROM "+matchTable.getTableName();
	    (matchTable.getStatement()).executeUpdate(q);
	    matchTable.closeConnection();	    
	}
	catch(Exception e) {
	    e.printStackTrace();
	    System.out.println("FAILED ON: "+q);
	}
    }

    public void reloadMatchesTable(DataSet set1, DataSet set2, DataSet matchTable) {
	String q = "DELETE FROM "+matchTable.getTableName();
	try {
	    matchTable.openConnection();
	    (matchTable.getStatement()).executeUpdate(q);
	    q = "INSERT INTO "+matchTable.getTableName()+" SELECT "+set1.getTableId()+","+set2.getTableId()+" FROM "+matchTable.getTableName()+"_COPY";
	    (matchTable.getStatement()).executeUpdate(q);
	    matchTable.closeConnection();	    
	}
	catch(Exception e) {
	    e.printStackTrace();
	    System.out.println("FAILED ON: "+q);
	}
    }
 
    public void dropMatchCopyTable(DataSet matchTable) {
	String q = "DROP TABLE "+matchTable.getTableName()+"_COPY";
	try {
	    matchTable.openConnection();
	    (matchTable.getStatement()).executeUpdate(q);
	    matchTable.closeConnection();	    
	}
	catch(Exception e) {
	    e.printStackTrace();
	    System.out.println("FAILED ON: "+q);
	}
    }

    private Vector getUniqueAttrMethodPairs(Vector blockingScheme) {
	TreeSet uPairs = new TreeSet(); //makes the contains lookup faster...
	for(int i = 0; i < blockingScheme.size(); i++) {
	    String toks[] = ((String)blockingScheme.get(i)).split("&");
	    for(int j = 0; j < toks.length; j++) {
		if(!uPairs.contains((String)toks[j])) {
		    uPairs.add((String)toks[j]);
		}
	    }
	}
	return new Vector((Collection)uPairs);
    }

}