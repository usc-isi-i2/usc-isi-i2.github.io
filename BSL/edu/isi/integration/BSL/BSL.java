package edu.isi.integration.BSL;

import java.io.*;
import java.util.*;
import java.sql.*;
import edu.isi.integration.Commons.*;

public class BSL { 
    private String directoryPath; //path to temporary file if it's created...
    public void setPath(String s) {directoryPath = s;}


    public static void main(String args[]) {       
	XMLReader xr = new XMLReader();
	DataSet toMatch = new DataSet();
	DataSet forMatchTrain = new DataSet();
	DataSet forMatchTest = new DataSet();
	DataSet matchTable = new DataSet();
	Vector attributes = new Vector();
	Vector methods = new Vector();
	BSL bsl = new BSL();

	if(args[0].equals("run")) {	    
	    //first we load up the toMatch set...
	    //first parameter is xml file, second is name of the data set
	    bsl.loadDataSet(args[1],args[2],toMatch,xr);
	
	    //now we load up the forMatch set...	
	    bsl.loadDataSet(args[3],args[4],forMatchTrain,xr);

	    //now we load up the forMatch set...	
	    bsl.loadDataSet(args[5],args[6],forMatchTest,xr);

	    //now, we load in the attributes and methods the user wants to use for learning
	    String attrMethodFile = args[7];
	    bsl.loadAttributesAndMethods(attrMethodFile,attributes,methods,xr);

	    //now, if the method requires it, we can load in the matches table
	    bsl.makeMatchTable(forMatchTrain, toMatch, matchTable);

	    //now learn and test a rule
	    bsl.learnRuleAndTest(attributes, methods,forMatchTrain,forMatchTest,toMatch,matchTable);
	}
	if(args[0].equals("setUpRefIndex")) {
	    //first we load up the toMatch set...
	    //first parameter is xml file, second is name of the data set
	    bsl.loadDataSet(args[1],args[2],toMatch,xr);
	
	    //now we load up the forMatch set...	
	    bsl.loadDataSet(args[3],args[4],forMatchTrain,xr);

	    //now, we load in the attributes and methods the user wants to use for learning
	    String attrMethodFile = args[5];
	    bsl.loadAttributesAndMethods(attrMethodFile,attributes,methods,xr);

	    //now, if the method requires it, we can load in the matches table
	    bsl.makeMatchTable(forMatchTrain, toMatch, matchTable);

	    bsl.setUpRefIndex(attributes,methods,matchTable,forMatchTrain,toMatch);
	}
	//this is all if you want it to work on it's own...
	/*
	  
	  if(args.length == 0) {
	  System.out.println("usage: TO BE WRITTEN");
	  System.exit(0);
	  }
	  if(args[1].equals("setUpRefIndex")) {
	  setUpRefIndex(Vector attrs, Vector methods, DataSet matchTable, 
	  DataSet forMatching,DataSet toMatch)
	  }
	  if(args[1].equals("setUpAttrIndexTrain")) {
	  setUpAttrIndex(Vector attrs, Vector methods, DataSet forMatch, DataSet toMatch)
	  }
	  if(args[1].equals("run")) {
	  learnRuleAndTest(Vector attrs, Vector methods, DataSet forMatchTrain,
	  DataSet forMatchTest, DataSet toMatch, DataSet matchTable)
	  }
	  if(args[1].equals("test")) {
	  testBlockingScheme(Vector attrs, Vector methods, Vector blockingScheme,
	  DataSet forMatch, DataSet toMatch, boolean forceFile)
	  }
	  if(args[1].equals("clearAttrIndexes")) {
	  clearForMatchingSetIndexes(Vector attrs, Vector methods, DataSet forMatching) 
	  }
	  //if(args[1].equals("clearRefIndexes")) {
	  clearToMatchIndexes(Vector attrs, Vector methods, DataSet matchTable, 
	  DataSet toMatch)
	  }
	*/
    }

    public static void setUpRefIndex(Vector attrs, Vector methods, DataSet matchTable, 
				     DataSet forMatching,DataSet toMatch) {
	//we always take the set we are matching for (forMatching) and match the records to those from the toMatch set
	MakeIndex m = new MakeIndex();
	m.makeToMatchIndex(toMatch, attrs, methods);
	m.copyMatchesTable(forMatching, toMatch, matchTable);
    }
    
    public static void setUpAttrIndex(Vector attrs, Vector methods, DataSet forMatch, DataSet toMatch) {
	MakeIndex m = new MakeIndex(); 
	m.makeForMatchIndex(toMatch, forMatch,attrs,methods); 
    }

    public Vector learnRule(DataSet forMatchTrain, DataSet toMatch, DataSet matchTable, String attrMethodFile,
			    boolean forceFile) {
	XMLReader xr = new XMLReader();
	Vector attrs = new Vector();
	Vector methods = new Vector();
	
	//now, we load in the attributes and methods the user wants to use for learning
	loadAttributesAndMethods(attrMethodFile,attrs,methods,xr);

	MakeIndex m = new MakeIndex(); 
	m.makeForMatchIndex(toMatch, forMatchTrain,attrs,methods); 
	
	//now learn our blocking rule
	SeqCoveringAlg sca = new SeqCoveringAlg(forMatchTrain, toMatch, matchTable, attrs, methods);
	Vector blockingScheme = sca.sequentialCovering();
	
	//we can write out the cands we would generate on the training set
	m.reloadMatchesTable(forMatchTrain, toMatch, matchTable);
	BlockScheme bs = new BlockScheme(attrs, methods, matchTable, forMatchTrain, toMatch);
	//view scheme
	//bs.printBlockingScheme();
	if(forceFile) {
	    BlockScheme.writeCandsFile(blockingScheme, forMatchTrain, toMatch, directoryPath);
	}
	else {
	    BlockScheme.writeCands(blockingScheme, forMatchTrain, toMatch);
	}
	m.dropAttrMethodTable(forMatchTrain, methods, attrs);
	m.dropCandTables(methods, attrs, forMatchTrain);

	return blockingScheme;
    }

    public static void learnRuleAndTest(Vector attrs, Vector methods, DataSet forMatchTrain,
					DataSet forMatchTest, DataSet toMatch, DataSet matchTable) {
	MakeIndex m = new MakeIndex(); 
	m.makeForMatchIndex(toMatch, forMatchTrain,attrs,methods); 

	//now learn our blocking rule
	SeqCoveringAlg sca = new SeqCoveringAlg(forMatchTrain, toMatch, matchTable, attrs, methods);
	Vector blockingScheme = sca.sequentialCovering();
	
	//we can write out the cands we would generate on the training set
	m.reloadMatchesTable(forMatchTrain, toMatch, matchTable);
	BlockScheme bs = new BlockScheme(attrs, methods, matchTable, forMatchTrain, toMatch);
	BlockScheme.writeCands(blockingScheme, forMatchTrain, toMatch);

	//now clear out the old indexes and reload the matches table (which gets changed)
	m.dropAttrMethodTable(forMatchTrain, methods, attrs);
	m.dropCandTables(methods, attrs, forMatchTrain);

	//now, get the testing data and make it's indexes
	m.makeForMatchIndex(toMatch, forMatchTest, attrs,methods); 

	//now test out the rule we learned
	double ncands = bs.getNumCands(blockingScheme, forMatchTest,toMatch);
	//System.out.println("CANDS: "+ncands);
	double rr = bs.getReductionRatio(blockingScheme,forMatchTest,toMatch,matchTable);
	//System.out.println("FINAL RR: "+rr);
	double pc = bs.getPairsCompleteness(blockingScheme,forMatchTest,toMatch,matchTable);
	//System.out.println("FINAL PC: "+pc);
	BlockScheme.writeCands(blockingScheme, forMatchTest, toMatch);

	//lastly, clear out the old indexes from the testing data
	m.dropAttrMethodTable(forMatchTest, methods, attrs);
	m.dropCandTables(methods, attrs, forMatchTest);
    }
    
    public void runBlockingScheme(Vector blockingScheme,DataSet forMatch, 
					  DataSet toMatch, boolean forceFile) {
	//the blocking scheme can write the candidates to a database or a file, depending on whether or 
	//not the database you are using suports the required query to insert the candidates. 
	//The databases that do are mysql and sql server. If you are using access, then it writes them to a file.
	//However, in some cases you may want to force them to a file, even if you are using sqlserver or mysql
	//In this case, set forceFile to true and it will do this.
 	    MakeIndex m = new MakeIndex(); 
	    System.out.println("Making token tables and indexes..."+new Time(System.currentTimeMillis()));
	    m.makeBlockSchemeIndexes(toMatch, forMatch, blockingScheme);
	    if(forceFile) {
		BlockScheme.writeCandsFile(blockingScheme, forMatch, toMatch, directoryPath);
	    }
	    else {
		BlockScheme.writeCands(blockingScheme, forMatch, toMatch);
	    }
	    //lastly, clear out the old indexes from the testing data
	    m.dropAttrMethodTable(forMatch, blockingScheme);
	    m.dropCandTables(blockingScheme, forMatch);
    }

    public static void clearForMatchingSetIndexes(Vector attrs, Vector methods, DataSet forMatching) {
	MakeIndex m = new MakeIndex();
	m.dropAttrMethodTable(forMatching, methods, attrs);
	m.dropCandTables(methods, attrs, forMatching);
    }

    public static void clearToMatchIndexes(Vector attrs, Vector methods, DataSet matchTable, 
					   DataSet toMatch) {
 	//if(args[1].equals("clearRefIndexes")) {
	MakeIndex m = new MakeIndex();
	m.dropAttrMethodTable(toMatch, methods, attrs);
	m.dropMatchCopyTable(matchTable);
	//System.out.println("DROPPED ALL REFERENCE TABLES...");
    }



    private static Vector getUniqueAttrs(Vector blockingScheme) {
	TreeSet attrs = new TreeSet(); //makes the containment check faster
	for(int z = 0; z < blockingScheme.size(); z++) {
	    String currPair[] =  ((String)blockingScheme.get(z)).split("\\|"); 
	    String attr = currPair[0];
	    if(!attrs.contains((String)attr)) {
		attrs.add((String)attr);
	    }
	}
	return new Vector((Collection)attrs);
    }

    public void setUpTable(String postSetFile, String postSetName, String method) {
	DataSet postSet = new DataSet();
	XMLReader xr = new XMLReader();
	loadDataSet(postSetFile,postSetName,postSet,xr);
	setUpTable(postSet,method);
    }

    public void setUpTable(DataSet postSet, String method) {
	Vector attrs = postSet.getAttributes();
	String q = "";
	try {
	    Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
	    Connection con = DriverManager.getConnection(postSet.getURL(),postSet.getLogin(),postSet.getPass());
	    Statement s = con.createStatement();
	    q = "UPDATE "+postSet.getTableName()+" SET ";
	    for(int x = 0; x < attrs.size(); x++) {
		if(x > 0) {
		    q += ",";
		}
		if(method.equals("blocking")) {
		    q += (String)attrs.get(x)+"=post";
		}
		if(method.equals("phoebus")) {
		    if(!(((String)attrs.get(x)).equals("post"))) {
			q += (String)attrs.get(x)+"=''";
		    }
		}
	    }
	    
	    //all attributes used in the blocking scheme set to be the post
	    q = q.replaceAll("SET ,","SET ");
	    s.executeUpdate(q);
	    con.close();
	}
	catch(Exception e) {
	    System.out.println("FAILED ON: "+q);
	    e.printStackTrace();
	}
    }

    public void setUpTable(DataSet postSet, Vector blockingScheme, String method) {
	//method: blocking|phoebus 
	//Means that we can set the table up for blocking, which means we put the post in each attribute
	//or we can set it up for phoebus, which clears the attributes set up for in blocking

	//first, get all the unique attributes in the blocking scheme
	Vector attrs = getUniqueAttrs(blockingScheme);

	String q = "";
	try {
	    Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
	    Connection con = DriverManager.getConnection(postSet.getURL(),postSet.getLogin(),postSet.getPass());
	    Statement s = con.createStatement();
	    q = "UPDATE "+postSet.getTableName()+" SET ";
	    for(int x = 0; x < attrs.size(); x++) {
		if(x > 0) {
		    q += ",";
		}
		if(method.equals("blocking")) {
		    q += (String)attrs.get(x)+"=post";
		}
		if(method.equals("phoebus")) {
		    q += (String)attrs.get(x)+"=''";
		}
	    }

	    //all attributes used in the blocking scheme set to be the post
	    s.executeUpdate(q);
	    con.close();
	}
	catch(Exception e) {
	    System.out.println("FAILED ON: "+q);
	    e.printStackTrace();
	}
    }

    public void loadDataSet(String file, String setName, DataSet ds, XMLReader xr) {
	xr.retrieveDataSet(file, setName, ds);
	System.out.println(ds);
    }

    public void loadAttributesAndMethods(String file, Vector attributes, Vector methods, XMLReader xr) {
	xr.loadAttributesAndMethods(file,attributes,methods);	
	System.out.println("ATTRS: "+attributes);
	System.out.println("METHS: "+methods);
    }

    public void makeMatchTable(DataSet set1, DataSet set2, DataSet matchTable){
	matchTable.setName("matches");
	matchTable.setTableName(set1.getTableName()+"_"+set2.getTableName()+"_MATCHES");
	matchTable.setURL(set1.getURL());
	matchTable.setLogin(set1.getLogin());
	matchTable.setPass(set1.getPass());
	matchTable.setTableId("notUsed");
	matchTable.addAttribute(set1.getTableId());
	matchTable.addAttribute(set2.getTableId());
	System.out.println(matchTable);
	TableMaker tm = new TableMaker();
	tm.createTable(matchTable);
    }
}