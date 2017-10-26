package edu.isi.integration.Commons;

import java.sql.*;
import java.util.*;

public class TableMaker {
    //This class allows you to make tables
    /*basically, you pass in a dataset object (b/c that holds all of the things like the jdbc connection, what the
    table should be named, what it's attributes are, etc.

    Then you fill it up with data from the XML file of posts.

    Lastly, we have a method that cleans up the tables after themselves...
    */
    
    public void createTable(DataSet dataInfo) {
	String query = "";
	try {
	    //First thngs first, we need to make a connection to the database
	    Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
	    Connection con = DriverManager.getConnection(dataInfo.getURL(),dataInfo.getLogin(),dataInfo.getPass());
	    Statement stmt = con.createStatement();

	    //now we build the create statement
	    query += "CREATE TABLE "+dataInfo.getTableName()+" (";

	    DatabaseMetaData dmd = con.getMetaData();
	    String dbType = dmd.getDatabaseProductName();

	    for(int i = 0; i < (dataInfo.getAttributes()).size(); i++) {
		if(dbType.equals("Microsoft SQL Server")) {
		    if(i == 0) {
			query += (String)(dataInfo.getAttributes()).get(i)+" varchar(255)";
		    }
		    else {
			query += ","+(String)(dataInfo.getAttributes()).get(i)+" varchar(255)";
		    }
		}
		else {
		    if(i == 0) {
			query += (String)(dataInfo.getAttributes()).get(i)+" text";
		    }
		    else {
			query += ","+(String)(dataInfo.getAttributes()).get(i)+" text";
		    }
		}
	    }
	    if(dataInfo.getTableId().trim().length() > 0) {
		if(dbType.equals("Microsoft SQL Server")) {
		    query += ","+dataInfo.getTableId()+" varchar(255)";
		}
		else {
		    query += ","+dataInfo.getTableId()+" text";
		}
	    }
	    query += ")";
	
	    //if the table already exists for some reason, we don't want to remake it...
	    ResultSet tables = dmd.getTables(null, null, dataInfo.getTableName(), null);
	    if (tables.next()) {
		// Table exists
		stmt.close();
		con.close();
		return;
	    }
	    else {
		// Table does not exist
		int val = stmt.executeUpdate(query);
	    }
	    stmt.close();
	    con.close();
	}
	catch(Exception e) {
	    e.printStackTrace();
	    System.out.println("FAILED ON: "+query);
	}
    }

    public void fillTableWithPosts(HashMap posts, DataSet dataInfo, String matchTableName, String refIdName) {
	String query = "";
	try {
	    //First thngs first, we need to make a connection to the database
	    Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
	    Connection con = DriverManager.getConnection(dataInfo.getURL(),dataInfo.getLogin(),dataInfo.getPass());
	    Statement stmt = con.createStatement();
	    Iterator iterator = posts.keySet().iterator();
	    while (iterator.hasNext()) {
		String currId = (String)iterator.next();
		String currPost = (String)posts.get(currId);	   
		query = "INSERT INTO "+dataInfo.getTableName()+" (post,"+dataInfo.getTableId()+") VALUES ";
		query += " ('"+currPost+"','"+(String)(currId+"")+"')";
		stmt.executeUpdate(query);
		query = "INSERT INTO "+matchTableName+" ("+dataInfo.getTableId()+","+refIdName+") ";
		query += "VALUES ('"+(String)(currId+"")+"','0')";
		stmt.executeUpdate(query);
	    }
	    stmt.close();
	    con.close();
	}
	catch(Exception e) {
	    e.printStackTrace();
	    System.out.println("FAILED ON: "+query);
	}
    }

    public void fillTableWithPosts(String xmlFile, DataSet dataInfo, String matchTableName, String refIdName) {
	XMLReader xr = new XMLReader();
	//Vector posts = xr.retrievePosts(xmlFile);
	HashMap posts = xr.retrievePosts(xmlFile);//key: record id from XML, value: post

	String query = "";
	try {
	    //First thngs first, we need to make a connection to the database
	    Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
	    Connection con = DriverManager.getConnection(dataInfo.getURL(),dataInfo.getLogin(),dataInfo.getPass());
	    Statement stmt = con.createStatement();
	    Iterator iterator = posts.keySet().iterator();
	    while (iterator.hasNext()) {
		String currId = (String)iterator.next();
		String currPost = (String)posts.get(currId);	   
		query = "INSERT INTO "+dataInfo.getTableName()+" (post,"+dataInfo.getTableId()+") VALUES ";
		query += " ('"+currPost+"','"+(String)(currId+"")+"')";
		stmt.executeUpdate(query);
		query = "INSERT INTO "+matchTableName+" ("+dataInfo.getTableId()+","+refIdName+") ";
		query += "VALUES ('"+(String)(currId+"")+"','0')";
		stmt.executeUpdate(query);
	    }
	    stmt.close();
	    con.close();
	}
	catch(Exception e) {
	    e.printStackTrace();
	    System.out.println("FAILED ON: "+query);
	}
    }

    public void updateField(String attribute, String value, DataSet dataInfo) {
	String query = "";
	try {
	    //First thngs first, we need to make a connection to the database
	    Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
	    Connection con = DriverManager.getConnection(dataInfo.getURL(),dataInfo.getLogin(),dataInfo.getPass());
	    Statement stmt = con.createStatement();
	    query += "UPDATE "+dataInfo.getTableName()+" SET "+attribute+" ='"+value+"'";
	    stmt.executeUpdate(query);
	    stmt.close();
	    con.close();
	}
	catch(Exception e) {
	    e.printStackTrace();
	    System.out.println("FAILED ON: "+query);
	}
    }

    public void cleanUp(DataSet dataInfo) {
	String query = "";
	try {
	    //First thngs first, we need to make a connection to the database
	    Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
	    Connection con = DriverManager.getConnection(dataInfo.getURL(),dataInfo.getLogin(),dataInfo.getPass());
	    Statement stmt = con.createStatement();

	    //now we build the create statement
	    query += "DROP TABLE "+dataInfo.getTableName();
	    stmt.executeUpdate(query);
	    stmt.close();
	    con.close();
	}
	catch(Exception e) {
	    e.printStackTrace();
	    System.out.println("FAILED ON: "+query);
	}
    }
}
