package edu.isi.integration.Commons;

//Note: part of this code was taken from http://totheriver.com/learn/xml/code/DomParserExample.java 
//(mostly the stuff to parse the DOM

//Note: Only one method uses XPath right now, it would be great to get all the XML parsing methods to use the XQuery to get their data...DEFINITELY SHOULD DO THIS FOR REFACTORING AND B/C ITS BETTER
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Vector;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.xpath.*;

public class XMLReader {
    Document dom;

    public Document getDocument() { return dom; }
    public Document getDocument(String file) {
	parseXmlFile(file);
	return dom;
    }

    //need a method to parse and read in posts...
    //for now, take the posts and stick them in a database to run phoebus
    //later, figure out how to do it on the fly
    public HashMap retrievePosts(String pFile) {
	HashMap hm = new HashMap();
	//Vector v = new Vector();
	parseXmlFile(pFile);
	
	Element docEle = dom.getDocumentElement();
	
	//get a nodelist of <Item> elements
	NodeList rows = docEle.getElementsByTagName("Row");
	if(rows != null && rows.getLength() > 0) {
	    for(int i = 0 ; i < rows.getLength();i++) {		
		//get each post and id from a row
		Element el = (Element)rows.item(i);
		NamedNodeMap attrs = el.getAttributes();
		Node idN = attrs.getNamedItem("id");
		String id = idN.getNodeValue();
		String post = getTextValue(el, "postText");
		hm.put((String)id, (String)post);
		//v.add((String)post);
	    }
	} 
	return hm;
    }

    public Vector getRowIds(String pFile) {
	Vector ids = new Vector();
	//this gets the set of Ids from the file
	//it's a treeset for fast searching later...
	XPath xpath = XPathFactory.newInstance().newXPath();
	String expression = "//Row/@id";
	InputSource inputSource = new InputSource(pFile);
	try {
	    NodeList nodes = (NodeList) xpath.evaluate(expression, inputSource, XPathConstants.NODESET);
	    if(nodes != null && nodes.getLength() > 0) {
		//iterate trhough them
		for(int i = 0 ; i < nodes.getLength();i++) {		
		    //get the Attribute element
		    Node curr = (Node)nodes.item(i);
		    String id = curr.getTextContent();
		    ids.add((String)id);
		}
	    }
	}
	catch(Exception e) {
	    e.printStackTrace();
	}
	return ids;
    }

    //since we keep the previous informaiton, we need to be able to look up a row by id in the XML file
    public Node getSpecificRow(String pFile, String id) {
	XPath xpath = XPathFactory.newInstance().newXPath();
	String expression = "//Row[@id='"+id+"']";
	InputSource inputSource = new InputSource(pFile);
	try {
	    NodeList nodes = (NodeList) xpath.evaluate(expression, inputSource, XPathConstants.NODESET);
	    //now parse the nodes, which should only be of size 1, so just check it's not null and then get
	    //all the information...
	    if(nodes != null && nodes.getLength() == 1) {
		return (Node)nodes.item(0);
	    }
	}
	catch(Exception e) {
	    e.printStackTrace();
	}
	return null;
    }

    //method to parse and read in the chosen common scores by a user
    public HashMap retrieveCommonScores(String csFile, String chosenScores[]) {
	HashMap commScores = new HashMap();
 	parseXmlFile(csFile);

	for(int i = 0; i < chosenScores.length;i++) {
	    String currAttr = chosenScores[i];
	    String patt = getPatternFromDom(currAttr);
	    if(patt.trim().length() != 0) {
		//System.out.println("PUT: "+currAttr+","+patt);
		commScores.put((String)currAttr, (String)patt);
	    }
	}
	return commScores;
    }

    private String getPatternFromDom(String currAttr){
	//get the root elememt
	Element docEle = dom.getDocumentElement();
	
	//get a nodelist of <ReferenceSet> elements
	NodeList attrs = docEle.getElementsByTagName("Attribute");
	if(attrs != null && attrs.getLength() > 0) {
	    for(int i = 0 ; i < attrs.getLength();i++) {		
		//get the Attribute element
		Element el = (Element)attrs.item(i);
		//System.out.println(getTextValue(el,"Name")+":"+getTextValue(el,"Pattern"));
		if(getTextValue(el,"Name").equalsIgnoreCase(currAttr)) {
		    return getTextValue(el,"Pattern");
		}
	    }
	} 
	return "";
    }

    //method to parse and read in reference sets...
    //ASSUMPTION: You have JDBC connections that correspond in the file
    public void retreiveReferenceSet(String referenceSetFile, String referenceSetName, DataSet refSet) {
	parseXmlFile(referenceSetFile);
	getReferenceSetsFromDom(referenceSetName, refSet);
    }

    //note that we can actually use the methods in retreiveReferenceSet to retreive any database
    //we have above method to distinguish between refSet adn posts when we are using Phoebus
    //but for BSL, both data sets are full databases, so we use the following method (just for clarity)
    public void retrieveDataSet(String dataSetFile, String dataSetName, DataSet set) {
	retreiveReferenceSet(dataSetFile, dataSetName, set);
    }

    //method to parse and read in a blocking scheme for a given reference set
    public void retreiveBlockingScheme(String referenceSetFile, String referenceSetName, Vector blockingScheme) {
	parseXmlFile(referenceSetFile);
	getBlockingSchemeFromDom(referenceSetName, blockingScheme);
    }

    public String retrieveFilePath(String configFile) {
	parseXmlFile(configFile);
	return getFilePathFromDom();
    }

    private void parseXmlFile(String xmlFile){
	//get the factory
	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();	
	try {	    
	    //Using factory get an instance of document builder
	    DocumentBuilder db = dbf.newDocumentBuilder();	    
	    //parse using builder to get DOM representation of the XML file
	    dom = null;
	    //System.out.println(xmlFile);
	    dom = db.parse(xmlFile);	    	    
	}catch(ParserConfigurationException pce) {
	    pce.printStackTrace();
	}catch(SAXException se) {
	    se.printStackTrace();
	}catch(IOException ioe) {
	    ioe.printStackTrace();
	}
    }
    
    private String getFilePathFromDom() {
	String path = "";
	//get the root element
	Element docEle = dom.getDocumentElement();

	//get a nodelist of <Database> elements
	NodeList nl = docEle.getElementsByTagName("Database");
	if(nl != null && nl.getLength() > 0) {
	    //get the Database element, there should only be 1
	    Element el = (Element)nl.item(0);
	    NodeList fileNodeL = el.getElementsByTagName("FilePath");
	    if(fileNodeL != null && fileNodeL.getLength() > 0) {
		Element fNode = (Element)fileNodeL.item(0); //should be just 1
		path = fNode.getFirstChild().getNodeValue();		
	    }
	} 
	return path;
    }

    private void getBlockingSchemeFromDom(String refSetName, Vector blockingScheme) {
	//get the root element
	Element docEle = dom.getDocumentElement();
	
	//get a nodelist of <ReferenceSet> elements
	NodeList nl = docEle.getElementsByTagName("ReferenceSet");
	if(nl != null && nl.getLength() > 0) {
	    for(int i = 0 ; i < nl.getLength();i++) {		
		//get the ReferenceSet element
		Element el = (Element)nl.item(i);
		
		//if it's the name of the ref set we are looking for
		//parse out the information we want and return...
		if(getTextValue(el,"Name").equalsIgnoreCase(refSetName)) {
		    getBlockingScheme(el, blockingScheme);
		    return;
		}
	    }
	} 
    }

    private void getReferenceSetsFromDom(String refSetName, DataSet refSet){
	//get the root elememt
	Element docEle = dom.getDocumentElement();
	
	//get a nodelist of <ReferenceSet> elements
	NodeList nl = docEle.getElementsByTagName("ReferenceSet");
	if(nl != null && nl.getLength() > 0) {
	    for(int i = 0 ; i < nl.getLength();i++) {		
		//get the ReferenceSet element
		Element el = (Element)nl.item(i);
		
		//if it's the name of the ref set we are looking for
		//parse out the information we want and return...
		if(getTextValue(el,"Name").equalsIgnoreCase(refSetName)) {
		    getReferenceSet(el, refSet);
		    return;
		}
	    }
	} 
    }

    private void getBlockingScheme(Element bsEl, Vector blockingScheme) {
	NodeList conjs = bsEl.getElementsByTagName("Conjunction");
	if(conjs != null && conjs.getLength() > 0) {
	    for(int i = 0 ; i < conjs.getLength();i++) {
		Element el = (Element)conjs.item(i);
		String textVal = el.getFirstChild().getNodeValue();
		blockingScheme.add((String)textVal);
	    }
	}
    }

    private void getReferenceSet(Element rsEl, DataSet refSet) {
	String attributes = "";	
	String url = "";
	String login = "";
	String pass = "";
	String refIDAttr = "";
	String tableName = "";
	String name = getTextValue(rsEl,"Name");

	//set the dataset object's name
	refSet.setName(name);
	//System.out.println("NAME: "+name);
	
	//get the schema
	NodeList attrs = rsEl.getElementsByTagName("Attr");
	if(attrs != null && attrs.getLength() > 0) {
	    for(int i = 0 ; i < attrs.getLength();i++) {
		Element el = (Element)attrs.item(i);
		String textVal = el.getFirstChild().getNodeValue();
		if(attributes.trim().length() == 0) {
		    attributes = textVal;
		}
		else {
		    attributes += "|"+textVal;
		}	
	    }
	}
 	refSet.setAllAttrs(attributes);
	//System.out.println("ATTRS: "+attributes);

	//get all of the database information
	NodeList dbs = rsEl.getElementsByTagName("Database");
	getDatabaseInformation(dbs, refSet);
     }
    
    public void retrieveDatabaseInformation(String configFile, DataSet ds) {
	parseXmlFile(configFile);
	Element docEle = dom.getDocumentElement();	
	NodeList dbs = docEle.getElementsByTagName("Database");
	getDatabaseInformation(dbs, ds);
    }

    private void getDatabaseInformation(NodeList dbs, DataSet ds) {
	String url="",login="",pass="",refIDAttr="",tableName="";
	if(dbs != null && dbs.getLength() > 0) {
	    Element db = (Element)dbs.item(0);
	    url = getTextValue(db,"URL");
	    login = getTextValue(db,"Login");
	    pass = getTextValue(db,"Password");
	    refIDAttr = getTextValue(db,"ID_Attr");
	    tableName = getTextValue(db,"TableName");
	}

	ds.setURL(url);
	ds.setLogin(login);
	ds.setPass(pass);
	ds.setTableName(tableName);
	ds.setTableId(refIDAttr);
    }

    /**
     * I take a xml element and the tag name, look for the tag and get
     * the text content 
     * i.e for <employee><name>John</name></employee> xml snippet if
     * the Element points to employee node and tagName is name I will return John  
     * @param ele
     * @param tagName
     * @return
     */
    private String getTextValue(Element ele, String tagName) {
	String textVal = null;
	NodeList nl = ele.getElementsByTagName(tagName);
	if(nl != null && nl.getLength() > 0) {
	    Element el = (Element)nl.item(0);
	    textVal = el.getFirstChild().getNodeValue();
	}
	
	return textVal;
    }

    public void loadAttributesAndMethods(String attrMethodFile, Vector attributes, Vector methods) {
	XPath xpath = XPathFactory.newInstance().newXPath();
	InputSource inputSource = new InputSource(attrMethodFile);
	try {
	    //first get the attributes
	    String expression = "//Attribute";	    
	    NodeList nodes = (NodeList) xpath.evaluate(expression, inputSource, XPathConstants.NODESET);
	    if(nodes != null && nodes.getLength() > 0) {
		//iterate trhough them
		for(int i = 0 ; i < nodes.getLength();i++) {		
		    //get the Attribute element
		    Node curr = (Node)nodes.item(i);
		    String attr = curr.getTextContent();
		    attributes.add((String)attr);
		}
	    }

	    //now get the methods
	    expression = "//Method";	    
	    nodes = (NodeList) xpath.evaluate(expression, inputSource, XPathConstants.NODESET);
	    if(nodes != null && nodes.getLength() > 0) {
		//iterate trhough them
		for(int i = 0 ; i < nodes.getLength();i++) {		
		    //get the Attribute element
		    Node curr = (Node)nodes.item(i);
		    String meth = curr.getTextContent();
		    methods.add((String)meth);
		}
	    }	    
	}
	catch(Exception e) {
	    e.printStackTrace();
	}
    }   
}