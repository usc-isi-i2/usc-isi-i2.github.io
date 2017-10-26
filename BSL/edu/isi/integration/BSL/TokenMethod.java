package edu.isi.integration.BSL;

import java.util.Vector;

public class TokenMethod {    
    public static Vector applyMethod(String method, String attrVal) {
	Vector ret = new Vector();
	if(attrVal == null) {
	    return new Vector();
	}
	attrVal = attrVal.replaceAll("\\s+"," "); //first, remove all extra spaces, so it's easy to tokenize

	//these methods just work on the attribute value, not each token of the attribute value
	if(method.indexOf("firstN") > -1) {
	    String sp[] = method.split("_");
	    int n = (new Integer(sp[1]).intValue());
	    if(attrVal.length() >= n) {
		String sub = attrVal.substring(0,n);
		ret.add((String)sub.toLowerCase());
	    }
	}

	//these methods require that you check all of the tokens of the attribute value
	String inToks[] = attrVal.split(" "); //now we have each of the tokens of the attribute
	for(int x = 0; x < inToks.length; x++) {
	    if(method.equals("token")) {
		ret.add((String)inToks[x].toLowerCase());
	    }
	    if(method.indexOf("ngram") > -1) {
		String sp[] = method.split("_");
		int n = (new Integer(sp[1]).intValue());
		Vector usedNGrams = getNGrams(inToks[x], n);
		for(int z = 0; z < usedNGrams.size(); z++) {
		    String s = (String)usedNGrams.get(z);
		    ret.add((String)s.toLowerCase());
		}
	    }
	}
	return ret;
    }
    
    private static Vector getNGrams(String input, int ngramsize) {
	Vector usedNGrams = new Vector();
	if(input.length() < ngramsize && input.length() > 1) {
	    if(!usedNGrams.contains((String)input)) {
		usedNGrams.add((String)input);
	    }
	}
	else {
	    int endLen = input.length() - (ngramsize - 1);
	    for(int i = 0; i < endLen; i++) {
		String s = input.substring(i,i+ngramsize);
		if(!usedNGrams.contains((String)s)) {
		    usedNGrams.add((String)s);
		}
	    }
	}
	return usedNGrams;
    }
}