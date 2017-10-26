package edu.isi.integration.BSL;

import java.util.*;
import java.io.*;
import java.sql.*;
import edu.isi.integration.Commons.DataSet;
import edu.isi.integration.Commons.TableMaker;

public class BlockScheme {
    private Vector attrs;
    private Vector methods;
    private DataSet matchTable;
    private DataSet toMatchSet;//this is the set we are matching to
    private DataSet forMatchSet;//this is the set we are using, either training or testing set
    private Vector blockingScheme;//we might be testing out a scheme already learned.
    private String cachedRule;//we cache the candidate rule (which is the rule used as the base which we add a conjunction to each time in LearnOneRule)

    BlockScheme(Vector a, Vector m, DataSet mt, DataSet fm, DataSet tm) {
	attrs = a;
	methods = m;
	matchTable = mt;
	toMatchSet = tm;
	forMatchSet = fm;
	cachedRule = "NoRuleSet";
    }

    public void printBlockingScheme() {
	System.out.print("BS: ");
	for(int i = 0; i < blockingScheme.size(); i++) {
	    String conj = (String)blockingScheme.get(i);
	    conj = conj.replaceAll("&"," AND ");
	    conj = "("+conj+")";
	    if(i > 0) {
		conj = " OR "+conj;
	    }
	    System.out.println(conj);
	}
    }

    public void setBlockingScheme(Vector bs) {
	blockingScheme = bs;
    }

    public double getReductionRatio(Vector rule, DataSet set1, DataSet set2,DataSet matchTable) {
	int totNumMatches = getTotalMatches(set1, set2, matchTable);
	int numCands = getNumCands(rule, set1, set2);
	double crossProd = getCrossProdSite(set1, set2);	
	double reductionRatio = 1.0 - (double)numCands / crossProd;
	return reductionRatio;
    }
    public double getPairsCompleteness(Vector rule, DataSet set1, DataSet set2,DataSet matchTable) {
	int totNumMatches = getTotalMatches(set1,set2, matchTable);
	int numCorr = getNumCorrectCands(rule,set1,set2,matchTable);
	double pairsCompleteness = (double)numCorr / (double)totNumMatches;
	return pairsCompleteness;
    }

    public void performance(Vector rule, DataSet set1, DataSet set2,DataSet matchTable) {
	int totNumMatches = getTotalMatches(set1, set2, matchTable);
	int numCands = getNumCands(rule, set1, set2);
	double crossProd = getCrossProdSite(set1, set2);	
	double reductionRatio = 1.0 - (double)numCands / crossProd;
	//System.out.println("NC: "+numCands);
	//System.out.println("CP: "+crossProd);
	//System.out.println("RR: "+reductionRatio);
	//System.out.println("TOTAL NUM MATCHES: "+totNumMatches);
	int numCorr = getNumCorrectCands(rule,set1,set2,matchTable);
	//System.out.println("NCorr "+numCorr);
	double pairsCompleteness = (double)numCorr / (double)totNumMatches;
	//System.out.println("PC: "+pairsCompleteness);
    }

    private int getNumCorrectCands(Vector rule,DataSet set1, DataSet set2, DataSet matchTable) {
	String q = "";
	int numCorr = 0;
	for(int i = 0; i < rule.size(); i++) {
	    String cRule = (String)rule.get(i);
	    String[] conjs = cRule.split("&");
	    int foundCands = 0;
	    //build up the select and the from
	    for(int j = 0; j < conjs.length; j++) {
		String conjElement = conjs[j];
		String[] parts = conjElement.split("\\|");
		String tbl = parts[0]+"_"+parts[1]+"_CANDS";
		if(j == 0) {
		    //we should probably just make this a count, b/c we just need the number in this query
		q += "SELECT DISTINCT "+tbl+"."+set1.getTableId()+", "+tbl+"."+set2.getTableId()+" FROM "+tbl+" ";
		}
		else {
		    q += ","+tbl;
		}
	    }
	    
	    q += ","+matchTable.getTableName()+" WHERE ";
	    
	    if(conjs.length > 1) {
		//q += " WHERE ";
		//build up the where clause, but only if we have more than one conj
		for(int x = 0; x < conjs.length-1; x++) {
		    //  make_firstn_1_cands.record_id=model_token_cands.record_id AND
		    //  make_firstn_1_cands.refid=model_token_cands.refid
		    String prevConjElement = conjs[x];
		    String[] prevParts = prevConjElement.split("\\|");
		    String prevTbl = prevParts[0]+"_"+prevParts[1]+"_CANDS";
		    
		    String nextConjElement = conjs[x+1];
		    String[] nextParts = nextConjElement.split("\\|");
		    String nextTbl = nextParts[0]+"_"+nextParts[1]+"_CANDS";
		    
		    q += prevTbl+"."+set1.getTableId()+"="+nextTbl+"."+set1.getTableId()+" AND ";
		    q += prevTbl+"."+set2.getTableId()+"="+nextTbl+"."+set2.getTableId();
		    if(x+1 < conjs.length-1) {
			q += " AND ";
		    }
		}
	    }
	    if(conjs.length > 1) {
		q += " AND ";
	    }
	    String firstConj = conjs[0];
	    String firstParts[] = firstConj.split("\\|");
	    String firstTbl = firstParts[0]+"_"+firstParts[1]+"_CANDS";
	    q += firstTbl+"."+set1.getTableId()+"="+matchTable.getTableName()+"."+set1.getTableId()+" AND ";
	    q += firstTbl+"."+set2.getTableId()+"="+matchTable.getTableName()+"."+set2.getTableId();

	    if(rule.size() > 1 && i < rule.size() - 1) {
		q += " UNION ";
	    }
	}
	//System.out.println(q);	
	try {
	    set1.openConnection();
	    ResultSet rs = (set1.getStatement()).executeQuery(q);
	    while (rs.next()) {
		numCorr++;
	    }
	    set1.closeConnection();
	}
	catch(Exception e) {
	    e.printStackTrace();
	    System.out.println("FAILED ON: "+q);
	}
	return numCorr;
    }

    public int getTotalMatches(DataSet set1, DataSet set2, DataSet matchTable) {
	String q = "SELECT "+matchTable.getTableName()+"."+set1.getTableId()+","+matchTable.getTableName()+"."+set2.getTableId();
	q += " FROM "+matchTable.getTableName()+","+set1.getTableName();
	q += " WHERE "+set1.getTableName()+"."+set1.getTableId()+"="+matchTable.getTableName()+"."+set1.getTableId();
	int numMatches = 0;
	try {
	    set1.openConnection();
	    //we don't do count distinct b/c access is grumpy about it
	    ResultSet rs = (set1.getStatement()).executeQuery(q);
	    while (rs.next()) {
		numMatches++;
	    }
	    set1.closeConnection();
	}
	catch(Exception e) {
	    e.printStackTrace();
	    System.out.println("FAILED ON: "+q);
	}
	return numMatches;
    }

    private double getCrossProdSite(DataSet set1, DataSet set2) {
	//since we remove covered true positives as we go, we use the temprorary table of set2
	//that holds the ids
	String set1Q = "SELECT DISTINCT "+set1.getTableId()+" AS cnt1 FROM "+set1.getTableName();
	String q = set1Q;
	int set1size = 0;
	int set2size = 0;
	try {
	    //we don't do count distinct b/c access is grumpy about it
	    set1.openConnection();
	    ResultSet rs = (set1.getStatement()).executeQuery(set1Q);
	    while (rs.next()) {
		set1size++;
	    }

	    String set2Q = "SELECT DISTINCT "+set2.getTableId()+" AS cnt2 FROM "+set2.getTableName();
	    //similar grumpiness...
	    rs = (set1.getStatement()).executeQuery(set2Q);
	    while (rs.next()) {
		set2size++;
	    }

	    set1.closeConnection();
	}
	catch(Exception e) {
	    e.printStackTrace();
	    System.out.println("FAILED ON: "+q);
	}
	return (double)set1size*set2size;
    }

    public int getNumCandsNoCache(Vector rule, DataSet set1, DataSet set2) {
	//System.out.println("IN GNCNC");
	String q = "";
	int foundCands = 0;
	String cRule = "";

	for(int i = 0; i < rule.size(); i++) {
	    cRule = (String)rule.get(i);

	    String[] conjs = cRule.split("&");
	    
	    //since both cases above can use the cached rule, they do the same query here...
	    //build up the select and the from
	    for(int j = 0; j < conjs.length; j++) {
		String conjElement = conjs[j];
		String[] parts = conjElement.split("\\|");
		String tbl = parts[0]+"_"+parts[1]+"_CANDS";
		if(j == 0) {
		    //we should probably just make this a count, b/c we just need the number in this query
		    //q += "SELECT COUNT(DISTINCT "+tbl+"."+set1.getTableId()+", "+tbl+"."+set2.getTableId()+") as cnt FROM "+tbl+" ";
		    q += "SELECT DISTINCT "+tbl+"."+set1.getTableId()+", "+tbl+"."+set2.getTableId()+" FROM "+tbl+" ";
		}
		else {
		    q += ","+tbl;
		}
	    }
	    
	    if(conjs.length > 1) {
		q += " WHERE ";
		//build up the where clause, but only if we have more than one conj
		for(int x = 0; x < conjs.length-1; x++) {
		    //  make_firstn_1_cands.record_id=model_token_cands.record_id AND
		    //  make_firstn_1_cands.refid=model_token_cands.refid
		    String prevConjElement = conjs[x];
		    String[] prevParts = prevConjElement.split("\\|");
		    String prevTbl = prevParts[0]+"_"+prevParts[1]+"_CANDS";
		    
		    String nextConjElement = conjs[x+1];
		    String[] nextParts = nextConjElement.split("\\|");
		    String nextTbl = nextParts[0]+"_"+nextParts[1]+"_CANDS";
		    
		    q += prevTbl+"."+set1.getTableId()+"="+nextTbl+"."+set1.getTableId()+" AND ";
		    q += prevTbl+"."+set2.getTableId()+"="+nextTbl+"."+set2.getTableId();
		    if(x+1 < conjs.length-1) {
			q += " AND ";
		    }
		}
		if(i < rule.size()-1 && rule.size() > 1) {
		    q += " UNION ";
		}
	    }
	}
	/*
	System.out.println("______________");
	System.out.println(q);	
	System.out.println("______________");
	*/
	try {
	    set1.openConnection();
	    ResultSet rs = (set1.getStatement()).executeQuery(q);
	    //access is grumpy about the count distinct, who knew?
	    while (rs.next()) {
		foundCands++;
	    }
	    
	    set1.closeConnection();
	}
	catch(Exception e) {
	    e.printStackTrace();
	    System.out.println("FAILED ON: "+q);
	}
	return foundCands;
    }

    public int getNumCands(Vector rule, DataSet set1, DataSet set2) {
	//System.out.println("IN GNC");
	if(rule.size() > 1) {
	    //means we have a disjunction with > 1 clauses, so we union, meaning, no caching
	    return getNumCandsNoCache(rule, set1, set2);
	}

	String q = "";
	int foundCands = 0;
	String cRule = (String)rule.get(0); //only a single clause to process

	//first we should check to see if our clause has a conjunction with at least 3 conjuncts
	//b/c otherwise we won't cache it
	String[] conjs = cRule.split("&");
	if(conjs.length <= 2) {
	    return getNumCandsNoCache(rule, set1, set2);
	}

	//first we should see if the candidate rule that generated this current rule (which is the rule minus the last conjunct) is already cached...if it is, then we can just join the last conjunct to the cached rule
	//if it isn't, we need to cache the candidate rule and then get the current rule...
	
	//first we check if the rule is cached
	int lastConjIdx = cRule.lastIndexOf("&");
	String ruleAllButLast = cRule.substring(0, lastConjIdx);
	if(!cachedRule.equals(ruleAllButLast)) {
	    //means we have a new rule to cache
	    cacheRule(ruleAllButLast,set1,set2);
	    
	    //now update which rule we cache
	    cachedRule = ruleAllButLast;
	    System.out.println("CACHE RULE CHANGE: "+cachedRule);
	}
	String lastConj = cRule.substring(lastConjIdx, cRule.length());
	lastConj = lastConj.replaceAll("&",""); //it has a conjunct sign in it, so remove it...
	String[] parts = lastConj.split("\\|");
	String lastConjTbl = parts[0]+"_"+parts[1]+"_CANDS";
	//since both cases above can use the cached rule, they do the same query here...
	String query = "SELECT DISTINCT ruleCacheTable."+set1.getTableId()+" as id1, ruleCacheTable."+set2.getTableId()+" as id2 FROM ruleCacheTable,"+lastConjTbl;
	query += " WHERE ruleCacheTable."+set1.getTableId()+"="+lastConjTbl+"."+set1.getTableId()+" AND ";
	query += " ruleCacheTable."+set2.getTableId()+"="+lastConjTbl+"."+set2.getTableId();
	//System.out.println(query);
	try {
	    set1.openConnection();
	    ResultSet rs = (set1.getStatement()).executeQuery(query);
	    //access is grumpy about the count distinct, who knew?
	    while (rs.next()) {
		foundCands++;
	    }
	    
	    set1.closeConnection();
	}
	catch(Exception e) {
	    e.printStackTrace();
	    System.out.println("FAILED ON: "+query);
	    System.out.flush();
	    System.exit(0);
	}
	return foundCands;
    }

    public void removeCoveredTPs(String rule,DataSet set1, DataSet set2, DataSet matchTable) {
	String q = "";
	//System.out.println("REMOVE COVERED TPS");
	String[] conjs = rule.split("&");
	//build up the select and the from
	for(int j = 0; j < conjs.length; j++) {
	    String conjElement = conjs[j];
	    String[] parts = conjElement.split("\\|");
	    String tbl = parts[0]+"_"+parts[1]+"_CANDS";
	    if(j == 0) {
		//we should probably just make this a count, b/c we just need the number in this query
		q += "SELECT DISTINCT "+tbl+"."+set1.getTableId()+" as id1, "+tbl+"."+set2.getTableId()+" as id2 FROM "+tbl+" ";
	    }
	    else {
		q += ","+tbl;
	    }
	}
	    
	q += ","+matchTable.getTableName()+"_copy WHERE ";
	
	if(conjs.length > 1) {
	    //build up the where clause, but only if we have more than one conj
	    for(int x = 0; x < conjs.length-1; x++) {
		String prevConjElement = conjs[x];
		String[] prevParts = prevConjElement.split("\\|");
		String prevTbl = prevParts[0]+"_"+prevParts[1]+"_CANDS";
		
		String nextConjElement = conjs[x+1];
		String[] nextParts = nextConjElement.split("\\|");
		String nextTbl = nextParts[0]+"_"+nextParts[1]+"_CANDS";
		
		q += prevTbl+"."+set1.getTableId()+"="+nextTbl+"."+set1.getTableId()+" AND ";
		q += prevTbl+"."+set2.getTableId()+"="+nextTbl+"."+set2.getTableId();
		if(x+1 < conjs.length-1) {
		    q += " AND ";
		}
	    }
	}
	if(conjs.length > 1) {
	    q += " AND ";
	}

	String firstConj = conjs[0];
	String firstParts[] = firstConj.split("\\|");
	String firstTbl = firstParts[0]+"_"+firstParts[1]+"_CANDS";
	q += firstTbl+"."+set1.getTableId()+"="+matchTable.getTableName()+"_copy."+set1.getTableId()+" AND ";
	q += firstTbl+"."+set2.getTableId()+"="+matchTable.getTableName()+"_copy."+set2.getTableId();

	//System.out.println(q);
	try {
	    set1.openConnection();
	    ResultSet rs = (set1.getStatement()).executeQuery(q);
	    Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
	    Connection conT = DriverManager.getConnection(set1.getURL(),set1.getLogin(),set1.getPass());
	    //it proved faster to do 2 queries intead of 1 nested query with an "in"...

	    while(rs.next()) {
		String id1 = rs.getString("id1");
		String id2 = rs.getString("id2");
		Statement delSt = conT.createStatement();
		String qDel = "DELETE FROM "+matchTable.getTableName()+" WHERE "+set1.getTableId()+"='"+id1+"' AND "+set2.getTableId()+"='"+id2+"'";
		delSt.executeUpdate(qDel);
		delSt.close();
	    }
	    conT.close();
	    set1.closeConnection();
	}
	catch(Exception e) {
	    e.printStackTrace();
	    System.out.println("FAILED ON: "+q);
	}
    }

    public static void writeCands(Vector rule, DataSet set1, DataSet set2) {
	try {
	    set1.openConnection();
	    DatabaseMetaData dmd = (set1.getConnection()).getMetaData();
	    String dbType = dmd.getDatabaseProductName();
	    set1.closeConnection();
	    if(dbType.equalsIgnoreCase("access")) {
		writeCandsFile(rule,set1,set2,"");
	    }
	    else {
		writeCandsTable(rule, set1, set2);
	    }
	}
	catch(Exception e) {
	    e.printStackTrace();
	}
    }

    //write the candidates out to a file (for access who can't support the query to insert them b/c of the unions...)
    public static void writeCandsFile(Vector rule, DataSet set1, DataSet set2, String path) {
	if(path == null) { path = ""; }
	String fileName = path+set1.getTableName()+"_finalCands.txt";
	try {
	    BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
	    String q = "";
	    for(int i = 0; i < rule.size(); i++) {
		String cRule = (String)rule.get(i);
		System.out.println("CURRENT RULE: "+cRule+"..."+new Time(System.currentTimeMillis()));
		String[] conjs = cRule.split("&");
		//build up the select and the from
		for(int j = 0; j < conjs.length; j++) {
		    String conjElement = conjs[j];
		    String[] parts = conjElement.split("\\|");
		    String tbl = parts[0]+"_"+parts[1]+"_CANDS";
		    if(j == 0) {
			q += "SELECT DISTINCT "+tbl+"."+set1.getTableId()+" as id1, "+tbl+"."+set2.getTableId()+" as id2 FROM "+tbl+" ";
		    }
		    else {
			q += ","+tbl;
		    }
		}		
		if(conjs.length > 1) {
		    q += " WHERE ";
		    //build up the where clause, but only if we have more than one conj
		    for(int x = 0; x < conjs.length-1; x++) {
			String prevConjElement = conjs[x];
			String[] prevParts = prevConjElement.split("\\|");
			String prevTbl = prevParts[0]+"_"+prevParts[1]+"_CANDS";
			
			String nextConjElement = conjs[x+1];
			String[] nextParts = nextConjElement.split("\\|");
			String nextTbl = nextParts[0]+"_"+nextParts[1]+"_CANDS";
			
			q += prevTbl+"."+set1.getTableId()+"="+nextTbl+"."+set1.getTableId()+" AND ";
			q += prevTbl+"."+set2.getTableId()+"="+nextTbl+"."+set2.getTableId();
			if(x+1 < conjs.length-1) {
			    q += " AND ";
			}
		    }
		}
		if(i < rule.size()-1 && rule.size() > 1) {
		    q += " UNION ";
		}
	    }
	    //System.out.println(q);
	    set1.openConnection();
	    ResultSet rs = (set1.getStatement()).executeQuery(q);
	    while(rs.next()) {
		String id1 = rs.getString("id1");
		String id2 = rs.getString("id2");
		out.write(id1+"|"+id2);
		out.newLine();
	    }
	    out.close();
	    set1.closeConnection();
	}
	catch(Exception e) {
	    e.printStackTrace();
	}
    }

    //write the candidates out to a table
   private static void writeCandsTable(Vector rule, DataSet set1, DataSet set2) {
       //first, delete the old cands that might be around
       String d = "DELETE FROM "+set1.getTableName()+"_finalCANDS";
       try {
	    set1.openConnection();
	    (set1.getStatement()).executeUpdate(d);
	    set1.closeConnection();
       }
       catch(Exception e) {
	   e.printStackTrace();
	   System.out.println("FAILED ON: "+d);
       }

	String q = "";
	q = "INSERT INTO "+set1.getTableName()+"_finalCANDS ";
	for(int i = 0; i < rule.size(); i++) {
	    String cRule = (String)rule.get(i);
	    String[] conjs = cRule.split("&");
	    //build up the select and the from
	    for(int j = 0; j < conjs.length; j++) {
		String conjElement = conjs[j];
		String[] parts = conjElement.split("\\|");
		String tbl = parts[0]+"_"+parts[1]+"_CANDS";
		if(j == 0) {
		    //we should probably just make this a count, b/c we just need the number in this query
		    //q += "SELECT COUNT(DISTINCT "+tbl+"."+set1.getTableId()+", "+tbl+"."+set2.getTableId()+") as cnt FROM "+tbl+" ";
		    q += "SELECT DISTINCT "+tbl+"."+set1.getTableId()+", "+tbl+"."+set2.getTableId()+" FROM "+tbl+" ";
		}
		else {
		    q += ","+tbl;
		}
	    }
	    
	    if(conjs.length > 1) {
		q += " WHERE ";
		//build up the where clause, but only if we have more than one conj
		for(int x = 0; x < conjs.length-1; x++) {
		    //  make_firstn_1_cands.record_id=model_token_cands.record_id AND
		    //  make_firstn_1_cands.refid=model_token_cands.refid
		    String prevConjElement = conjs[x];
		    String[] prevParts = prevConjElement.split("\\|");
		    String prevTbl = prevParts[0]+"_"+prevParts[1]+"_CANDS";
		    
		    String nextConjElement = conjs[x+1];
		    String[] nextParts = nextConjElement.split("\\|");
		    String nextTbl = nextParts[0]+"_"+nextParts[1]+"_CANDS";
		    
		    q += prevTbl+"."+set1.getTableId()+"="+nextTbl+"."+set1.getTableId()+" AND ";
		    q += prevTbl+"."+set2.getTableId()+"="+nextTbl+"."+set2.getTableId();
		    if(x+1 < conjs.length-1) {
			q += " AND ";
		    }
		}
	    }
	    if(i < rule.size()-1 && rule.size() > 1) {
		q += " UNION ";
	    }
	}
	//System.out.println(q);	
	try {
	    set1.openConnection();
	    (set1.getStatement()).executeUpdate(q);
	    set1.closeConnection();
	}
	catch(Exception e) {
	    e.printStackTrace();
	    System.out.println("FAILED ON: "+q);
	}
    }

    private void clearCachedRule(DataSet set) {
	String query = "";
	try {
	    set.openConnection();
	    DatabaseMetaData dmd = (set.getConnection()).getMetaData();
	    String dbType = dmd.getDatabaseProductName();
	    if(dbType.equalsIgnoreCase("access")) {
		query = "DROP INDEX rctIndex ON ruleCacheTable";
	    }
	    if(dbType.equals("Microsoft SQL Server")) {
		query = "DROP INDEX ruleCacheTable.rctIndex";
	    }
	    if(dbType.equalsIgnoreCase("mysql")) {
		query = "ALTER TABLE ruleCacheTable DROP INDEX rctIndex";
	    }	    
	    (set.getStatement()).executeUpdate(query);
	    query = "DELETE from ruleCacheTable";
	    (set.getStatement()).executeUpdate(query);
	    set.closeConnection();
	}
	catch(Exception e) {
	    e.printStackTrace();
	    System.out.println("FAILED ON: "+query);
	}
    }

    private void cacheRule(String rule, DataSet set1, DataSet set2) {
	//first, if the current cached rule is NoRuleSet, then we need to make the table
	//easy way to do this..make a DataSet object and let TableMaker make it for us
	if(cachedRule.equals("NoRuleSet")) {
	    //System.out.println("CHANGE FROM NoRuleSet");
	    DataSet cacheTable = new DataSet();
	    cacheTable.setURL(set1.getURL());
	    cacheTable.setLogin(set1.getLogin());
	    cacheTable.setPass(set1.getPass());
	    cacheTable.setTableName("ruleCacheTable");
	    cacheTable.setTableId("notUsed");
	    cacheTable.addAttribute(set1.getTableId());
	    cacheTable.addAttribute(set2.getTableId());
	    TableMaker tm = new TableMaker();
	    tm.createTable(cacheTable);
	}


	//now, delete the records from the cache
	if(!cachedRule.equals("NoRuleSet")) { //don't need to delete the first time...
	    clearCachedRule(set1);
	}

	//now we fill up the table with records...
	//note that it's really just a table in the same db as set1, so we can use set1's connections...
	String[] conjs = rule.split("&");
	String q = "INSERT INTO ruleCacheTable ("+set1.getTableId()+","+set2.getTableId()+") ";
	//build up the select and the from
	for(int j = 0; j < conjs.length; j++) {
	    String conjElement = conjs[j];
	    String[] parts = conjElement.split("\\|");
	    String tbl = parts[0]+"_"+parts[1]+"_CANDS";
	    if(j == 0) {
		q += "SELECT DISTINCT "+tbl+"."+set1.getTableId()+" as id1, "+tbl+"."+set2.getTableId()+" as id2 FROM "+tbl+" ";
	    }
	    else {
		q += ","+tbl;
	    }
	}		
	if(conjs.length > 1) {
	    q += " WHERE ";
	    //build up the where clause, but only if we have more than one conj
	    for(int x = 0; x < conjs.length-1; x++) {
		String prevConjElement = conjs[x];
		String[] prevParts = prevConjElement.split("\\|");
		String prevTbl = prevParts[0]+"_"+prevParts[1]+"_CANDS";
		
		String nextConjElement = conjs[x+1];
		String[] nextParts = nextConjElement.split("\\|");
		String nextTbl = nextParts[0]+"_"+nextParts[1]+"_CANDS";
		
		q += prevTbl+"."+set1.getTableId()+"="+nextTbl+"."+set1.getTableId()+" AND ";
		q += prevTbl+"."+set2.getTableId()+"="+nextTbl+"."+set2.getTableId();
		if(x+1 < conjs.length-1) {
		    q += " AND ";
		}
	    }
	}
	//System.out.println("UPDATE CACHE QUERY: "+q);
	try {
	    set1.openConnection();
	    (set1.getStatement()).executeUpdate(q);
	
	    //now we add an index for the table
	    DatabaseMetaData dmd = (set1.getConnection()).getMetaData();
	    String dbType = dmd.getDatabaseProductName();
	    if(dbType.equalsIgnoreCase("access") || dbType.equals("Microsoft SQL Server")) {
		//if(dbType.equalsIgnoreCase("access")) {
		q = "CREATE INDEX rctIndex ON ruleCacheTable ("+set1.getTableId()+","+set2.getTableId()+")";
	    }
	    if(dbType.equalsIgnoreCase("mysql")) {
		q = "CREATE INDEX rctIndex ON ruleCacheTable ("+set1.getTableId()+"(50),"+set2.getTableId()+"(50))";
	    }	    
	    (set1.getStatement()).executeUpdate(q);
	    set1.closeConnection();
	}
	catch(Exception e) {
	    e.printStackTrace();
	    System.out.println("FAILED ON: "+q);
	}
    }

    public void dropCacheTable() {
	if(cachedRule.equals("NoRuleSet")) {
	    return;
	}
	String q = "";
	try {
	    forMatchSet.openConnection();
	    q = "DROP TABLE ruleCacheTable";
	    (forMatchSet.getStatement()).executeUpdate(q);
	    forMatchSet.closeConnection();
	}
	catch(Exception e) {
	    e.printStackTrace();
	    System.out.println("FAILED ON: "+q);
	}
    }
}