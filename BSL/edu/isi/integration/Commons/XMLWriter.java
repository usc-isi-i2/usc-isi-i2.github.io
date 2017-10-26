package edu.isi.integration.Commons;

import java.util.*;
import java.sql.*;
import java.io.*;
import org.w3c.dom.*;

/***
This class is used for creating XML output of the results from running phoebus.
It is not part of Phoebus so that we could use it elsewhere and also so that
iterative runs of Phoebus (say for multiple ref. sets), could be done and then we 
produce just 1 XML output...
***/
public class XMLWriter {
    public XMLWriter() {
    }

    //You need to consider that you might have multiple reference sets you used, so you need the cleaned attributes
    //From all of them...
    //Since you might have multiple reference sets, you also might have multiple matchTables for them. So, you need to make
    //sure that the order of your reference sets matches the order of your match tables, otherwise you will get 
    //funky results.
    //Lastly, all of these tables should be in the same database, for now. This makes it faster than having to 
    //do some sort of crazy bookkeeping across multiple databases...
    /*
      public void writeXMLOutputOfPosts(DataSet refSet[], DataSet postSet, DataSet matchTable[], String filename, String origXMLFile) {
      if(refSet.length != matchTable.length) {
      //something's weird here. We need a set of matches for each reference set
      System.err.println("MISMATCH BETWEEN REFERENCE SETS AND MATCH TABLES...");
      return;
      }
      
      //first, we must find the attributes that are not in the reference sets such as the 
      //common scores and the "post" attribute
      Vector refSetAttrs[] = new Vector[refSet.length];
      for(int i = 0; i < refSetAttrs.length; i++) {
      refSetAttrs[i] = refSet[i].getAttributes();
      }
      //System.out.println("PSA: "+postSet.getAttributes());
      Vector nonRefAttrs = getNonCoveredAttributes(refSetAttrs ,postSet.getAttributes());
      
      String query = "SELECT DISTINCT "+postSet.getTableName()+".post,";
      query += postSet.getTableName()+"."+postSet.getTableId()+" AS id,";
      //add the post only attributes (which are not standard values from teh reference set)
      for(int j = 0; j < nonRefAttrs.size(); j++) {
      String currAtt = (String)nonRefAttrs.get(j);
      query += postSet.getTableName()+"."+currAtt+",";
      }
      
      //add the reference set attributes
      for(int x = 0; x < refSet.length; x++) {
      Vector rAttrs = (Vector)refSet[x].getAttributes();
      for(int y = 0; y < rAttrs.size(); y++) {
      String currRefAttr = (String)rAttrs.get(y);
      query += refSet[x].getTableName()+"."+currRefAttr+",";
      }
      }
      
      query += "FROM ";
      query = query.replaceAll(",FROM ", " FROM ");
      query += postSet.getTableName()+",";
      //add the reference sets
      for(int z = 0; z < refSet.length; z++) {
      query += refSet[z].getTableName()+",";
      }
      //add the match tables
      for(int zz = 0; zz < matchTable.length; zz++) {
      query += matchTable[zz].getTableName()+",";
      }
      //ADD JOINS...
      query += " WHERE ";
      query = query.replaceAll(", WHERE ", " WHERE ");
      for(int aa = 0; aa < refSet.length; aa++) {
      query += postSet.getTableName()+"."+postSet.getTableId()+"=";
      query += matchTable[aa].getTableName()+"."+postSet.getTableId();
      query += " AND "+refSet[aa].getTableName()+"."+refSet[aa].getTableId()+"=";
      query += matchTable[aa].getTableName()+"."+refSet[aa].getTableId()+" AND ";
      }
      int lastIndx = query.lastIndexOf(" AND");
      query = query.substring(0, lastIndx);
      //System.out.println(query);
      
      //now, open up the connections
      try {
      BufferedWriter outFile = new BufferedWriter(new FileWriter(filename));
      
      //find row that corresponds to this post...just find row with id that is the same
      //Node currentNode = xr.getSpecificRow(XMLFile, id);
      //if(currentNode != null) { 
      
      outFile.write("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
      outFile.newLine();
      //remove last Row tag and insert the new data...
      //outFile.write(prevXMLData...
      //outFile.write("<a>AAAA</a>");
      outFile.newLine();
      outFile.write("<Data>");
      outFile.newLine();
      postSet.openConnection();
      ResultSet rs = (postSet.getStatement()).executeQuery(query);
      String currAtt = "";
      XMLReader xr = new XMLReader();
      while(rs.next()) {
      String currId = rs.getString("id");		
      Node currentNode = xr.getSpecificRow(origXMLFile, id);
      if(currentNode != null) {
      outFile.write("\t<Row>");
      //outFile.write("\t\t<POST>"+rs.getString("post")+"</POST>");
      for(int r = 0; r < nonRefAttrs.size(); r++) {
      currAtt = (String)nonRefAttrs.get(r);
      outFile.write("\t\t<"+currAtt.toUpperCase()+">");
      outFile.write(rs.getString(currAtt));
      outFile.write("</"+currAtt.toUpperCase()+">");
      outFile.newLine();
      }
      for(int t = 0; t < refSet.length; t++) {
      Vector currRefSetAttrs = refSet[t].getAttributes();
      for(int s = 0; s < currRefSetAttrs.size(); s++) {
      currAtt = (String)currRefSetAttrs.get(s);
      outFile.write("\t\t<"+currAtt.toUpperCase()+">");
      outFile.write(rs.getString(currAtt));
      outFile.write("</"+currAtt.toUpperCase()+">");
      outFile.newLine();
      }
      }
      outFile.write("\t</Row>");
      outFile.newLine();
      }
      }
      outFile.write("</Data>");
      outFile.newLine();
      outFile.close();
      postSet.closeConnection();
      }
      catch(Exception e) {
      e.printStackTrace();
      System.out.println("FAILED: "+query);
      }
      }
    */
    //same method as above but returns an output string rather than writing a file. This is so we cna use it as output for a web service.
    public String writeXMLOutputOfPosts(DataSet refSet[], DataSet postSet, DataSet matchTable[],String origXMLFile) {
	String xmlOutput = "";
	if(refSet.length != matchTable.length) {
	    //something's weird here. We need a set of matches for each reference set
	    System.err.println("<err>MISMATCH BETWEEN REFERENCE SETS AND MATCH TABLES...</err>");
	    return "";
	}

	//first, we must find the attributes that are not in the reference sets such as the 
	//common scores and the "post" attribute
	Vector refSetAttrs[] = new Vector[refSet.length];
	for(int i = 0; i < refSetAttrs.length; i++) {
	    refSetAttrs[i] = refSet[i].getAttributes();
	}
	Vector nonRefAttrs = getNonCoveredAttributes(refSetAttrs ,postSet.getAttributes());
	String query = "SELECT DISTINCT "+postSet.getTableName()+"."+postSet.getTableId()+" as id,";
	//add the post only attributes (which are not standard values from teh reference set)
	for(int j = 0; j < nonRefAttrs.size(); j++) {
	    String currAtt = (String)nonRefAttrs.get(j);
	    query += postSet.getTableName()+"."+currAtt+" AS "+currAtt+",";
	}

	//add the reference set attributes
	for(int x = 0; x < refSet.length; x++) {
	    Vector rAttrs = (Vector)refSet[x].getAttributes();
	    for(int y = 0; y < rAttrs.size(); y++) {
		String currRefAttr = (String)rAttrs.get(y);
		query += refSet[x].getTableName()+"."+currRefAttr+" AS "+currRefAttr+",";
	    }
	}

	query += "FROM ";
	query = query.replaceAll(",FROM ", " FROM ");
	query += postSet.getTableName()+",";
	//add the reference sets
	for(int z = 0; z < refSet.length; z++) {
	    query += refSet[z].getTableName()+",";
	}
	//add the match tables
	for(int zz = 0; zz < matchTable.length; zz++) {
	    query += matchTable[zz].getTableName()+",";
	}
	//ADD JOINS...
	query += " WHERE ";
	query = query.replaceAll(", WHERE ", " WHERE ");
	for(int aa = 0; aa < refSet.length; aa++) {
	    query += postSet.getTableName()+"."+postSet.getTableId()+"=";
	    query += matchTable[aa].getTableName()+"."+postSet.getTableId();
	    query += " AND "+refSet[aa].getTableName()+"."+refSet[aa].getTableId()+"=";
	    query += matchTable[aa].getTableName()+"."+refSet[aa].getTableId()+" AND ";
	}
	int lastIndx = query.lastIndexOf(" AND");
	query = query.substring(0, lastIndx);
	//System.out.println(query);
	//xmlOutput += "<q>"+query+"</q>\n";
	//now, open up the connections
	try {
	    XMLReader xr = new XMLReader();
	    Document dom =  xr.getDocument(origXMLFile);
	    Vector idSet = xr.getRowIds(origXMLFile);
	    //System.out.println(idSet);
	    xmlOutput += "<Data>\n";
	    postSet.openConnection();
	    ResultSet rs = (postSet.getStatement()).executeQuery(query);
	    String currAtt = "";
	    while(rs.next()) {
		String currId = rs.getString("id");	
		idSet.remove((String)currId);
		Node currentNode = xr.getSpecificRow(origXMLFile, currId);
		String currNodeName = currentNode.getNodeName();
		if(currentNode != null) {
		    //add all of these new child elements to the current node...
		    //first teh non-ref attrs
		    xmlOutput += printNode(currentNode);
		    int lastRowEnd = xmlOutput.lastIndexOf("</"+currNodeName+">");
		    xmlOutput = xmlOutput.substring(0,lastRowEnd-1);
		    for(int r = 0; r < nonRefAttrs.size(); r++) {			
			currAtt = (String)nonRefAttrs.get(r);
			xmlOutput += "\t\t<"+currAtt.toUpperCase()+">";
			String s = rs.getString(currAtt);
			if(s == null) { s="";}
			//xmlOutput += rs.getString(currAtt);
			xmlOutput += s;
			xmlOutput += "</"+currAtt.toUpperCase()+">\n";
		    }
		    //now the ref attrs...
		    for(int t = 0; t < refSet.length; t++) {
			Vector currRefSetAttrs = refSet[t].getAttributes();
			for(int s = 0; s < currRefSetAttrs.size(); s++) {
			    currAtt = (String)currRefSetAttrs.get(s);
			    currAtt = (String)currRefSetAttrs.get(s);
			    xmlOutput += "\t\t<"+currAtt.toUpperCase()+">";
			    xmlOutput += rs.getString(currAtt);
			    xmlOutput +="</"+currAtt.toUpperCase()+">\n";
			}
		    }
		}
		xmlOutput += "</"+currNodeName+">";	    
	    }

	    //now for the records that didn't get matched or anything, we need to pass their XML along
	    for(int j = 0; j < idSet.size(); j++) {
		String cId = (String)idSet.get(j);
		Node currentNode = xr.getSpecificRow(origXMLFile, cId);
		xmlOutput += printNode(currentNode);
	    }

	    xmlOutput += "</Data>";
	    postSet.closeConnection();
	}
	catch(Exception e) {
	    e.printStackTrace();
	    System.out.println("FAILED: "+query);
	}
	return xmlOutput;
    }

    public static Vector getNonCoveredAttributes(Vector set[], Vector singleSet) {
	//get the attributes in singleSet that are not covered by the vectors in set
	Vector notCovered = new Vector();
	boolean hit = false;
	for(int a = 0; a < singleSet.size(); a++) {
	    String currElem = (String)singleSet.get(a);
	    for(int b = 0; b < set.length; b++) {
		Vector currV = (Vector)set[b];
		if(currV.contains((String)currElem)) {
		    hit = true;
		    continue;
		}
	    }
	    if(hit == false) {
		notCovered.add((String)currElem);
	    }
	    hit = false;
	}
	//System.out.println("NCA: "+notCovered);
	return notCovered;
    }

    public String printNode(Node n) {
	String ret = "";
	String nodeName = n.getNodeName();
	if(nodeName != "#text") {
	    ret += "<";
	    ret += n.getNodeName();
	    NamedNodeMap attrs = n.getAttributes();
	    if(attrs != null) {
		for (int i = 0; i < attrs.getLength(); i++) {
		    Node attr = attrs.item(i);
		    ret += " ";
		    ret += attr.getNodeName();
		    ret += "=\"";
		    //ret += n.normalize(attr.getNodeValue());   
		    //ret += attr.getNodeValue();   
		    ret += attr.getTextContent();   
		    ret += "\"";
		}
	    }
	    ret += ">\n";
	}
	//ret += n.getTextContent();
	NodeList children = n.getChildNodes();
	if (children != null) {
	    int len = children.getLength();
	    for (int i = 0; i < len; i++) {
		//ret += "I'M a CHILD OF "+((children.item(i)).getParentNode()).getNodeName();
		ret += printNode(children.item(i));
	    }
	}
	String val = n.getNodeValue();
	if(val != null) {
	    ret += val;
	}
	if(nodeName != "#text") {
	    ret += "</"+n.getNodeName()+">\n";
	}
	return ret;
    }
}