package edu.isi.integration.BSL;

import java.util.*;
import java.io.*;
import edu.isi.integration.Commons.DataSet;
 
public class SeqCoveringAlg {
    private double member_bestPerformance; //if we keep this around, we don't have to rerun the rule when we select the bst
    private double MIN_PC_THRESH; //this is the minimum coverage (PC) a conjunction must have to be considered
    private DataSet forMatch; //this is the set we using to match from. So we take these records and find their cands in the other set
    private DataSet toMatch;//these are the records we are matching to. So we take the records from the other set (forMatch) and match them to these records
    private DataSet matchTable;//this is the set of our labeled matches
    Vector attributes; //attributes we are using to learn from
    Vector methods; //methods are using to learn from. we learn attr/meth pairs from these...

    public SeqCoveringAlg(DataSet fm, DataSet tm, DataSet mt, Vector a, Vector m) {
	toMatch = tm;
	forMatch = fm;
	matchTable = mt;
	attributes = a;
	methods = m;
	MIN_PC_THRESH = 0.5;
    }

    public void setMinPCThreshold(double d) {
	MIN_PC_THRESH = d;
    }

    public void removeCoveredTPs(String rule) {
	BlockScheme bs = new BlockScheme(attributes, methods, matchTable, forMatch, toMatch);
	bs.removeCoveredTPs(rule,forMatch,toMatch,matchTable);
    }

    private Vector sortRules(Vector learnedRules) {//NOT DONE..but might be unnecessary for our problem mapping
	//without running them again
	Vector sortedRules = new Vector();
	return sortedRules;
    }

    public double performance(String rule, BlockScheme bs) {
	//should be private, but public for debug purposes
	System.out.println("testing rule: "+rule);
	Vector ruleVect = new Vector();
	ruleVect.add((String)rule);

	double reductionRatio = bs.getReductionRatio(ruleVect,forMatch,toMatch,matchTable);
	double pairsCompletness = bs.getPairsCompleteness(ruleVect,forMatch,toMatch,matchTable);

	System.out.println("RR: "+reductionRatio);
	System.out.println("PC: "+pairsCompletness);

	if(pairsCompletness < MIN_PC_THRESH) { //not enough pc to cover decent portion MIN_PC_THRESH
	    //System.out.println("NOT ENOUGH PC...");
	    return -999.0;
	}
	return reductionRatio;
    }

    public Vector cleanUp(Vector hyps, Vector testedHyps) {
	//now remove any new candidate hypotheses that: duplicates, inconsistent or not max. specific
	Vector cleanedHyps = new Vector();
	for(int i = 0; i < hyps.size(); i++) {
	    String s = (String)hyps.get(i);
	    String[] toks = s.split("&");
	    Vector cleaned = new Vector();//we can't just do indexOf w/ a string b/c might be a substring
	    //so we use a vector and keep whole thing
	    //note that this is order perserving in that first apperaence of tokens stay in relative order, which we want
	    for(int j = 0; j < toks.length; j++) {
		if(!cleaned.contains((String)toks[j])) {
		    cleaned.add((String)toks[j]);
		}
	    }
	    String cleanedS = "";
	    for(int x = 0; x < cleaned.size(); x++) {
		if(x == 0) {
		    cleanedS = (String)cleaned.get(x);
		}
		else {
		    cleanedS += "&"+(String)cleaned.get(x);
		}
	    }

	    //now its clean, but we need to see if we've already ran it before
	    cleanedS = cleanedS.trim();
	    if(!testedHyps.contains((String)cleanedS)) {
		//now, we need to check if the conjunction already exists in another order...
		//that is, we might have seen a&b, so if we now get b&a, we can ignore it
		if(!alreadySubsumed(cleanedS,testedHyps)) {
		    cleanedHyps.add((String)cleanedS);
		}
	    }
	    testedHyps.add((String)cleanedS);
	}
	return cleanedHyps;
    }

    public boolean alreadySubsumed(String s, Vector tested) {
	String sToks[] = s.split("&");
	Vector stoks = new Vector();
	for(int j = 0; j < sToks.length; j++) {
	    stoks.add((String)sToks[j]);
	}
	for(int i = 0; i < tested.size(); i++) {
	    String t  = (String)tested.get(i);
	    String tToks[] = t.split("&");
	    Vector ttoks = new Vector();
	    for(int z = 0; z < tToks.length; z++) {
		ttoks.add((String)tToks[z]);
	    }
	    if(ttoks.containsAll(stoks)) {
		return true;
	    }
	}
	return false;
    }

    public Vector updateKBest(String newVal, Vector oldVals, int K) {
	Vector newVals = new Vector();
	for(int i = 0; i < oldVals.size(); i++) {
	    if(i == 0) {
		newVals.add((String)newVal);
	    }
	    else {
		newVals.add((String)oldVals.get(i-1));
	    }
	}
	return newVals;
    }

    private Vector getAttributeMethodPairs() {
	Vector attrMethodPairs = new Vector();

	for(int i = 0; i < attributes.size(); i++) {
	    for(int j = 0; j < methods.size(); j++) {
		String s = (String)attributes.get(i)+"|"+(String)methods.get(j);
		attrMethodPairs.add((String)s);
	    }
	}
	return attrMethodPairs;
    }

    public Vector sequentialCovering() {
	Vector learnedRules = new Vector();
	BlockScheme bs = new BlockScheme(attributes, methods, matchTable, forMatch, toMatch);
	int totMatches = bs.getTotalMatches(forMatch, toMatch, matchTable);
	//System.out.println("START WITH: "+totMatches+" POS. EXAMPLES");
	String newRule = learnOneRule();
	//System.out.println("LEARNED RULE: "+newRule+" WITH PERFORMANCE: "+member_bestormance);
	learnedRules.add((String)newRule);
	//System.out.println("now iterate....");
	removeCoveredTPs(newRule);
	totMatches = bs.getTotalMatches(forMatch, toMatch, matchTable);
	System.out.println(" NUM OF EXAMPLES LEFT: "+totMatches);
	while(totMatches > 0) {//Learn to cover as many examples as possible, i.e. maximizePC
	    if(totMatches == 0) {
		//we have no records left to cover
		break;
	    }
	    newRule = learnOneRule();
	    if(newRule.trim().length() == 0) { 
		//we didn't learn any new rule
		break; 
	    } 
	    System.out.println("LEARNED RULE: "+newRule);
	    learnedRules = subsume(learnedRules, newRule);
	    removeCoveredTPs(newRule);
	    totMatches = bs.getTotalMatches(forMatch, toMatch, matchTable);
	    System.out.println(" NUM OF EXAMPLES LEFT: "+totMatches);
	}

	System.out.println("FINAL RULE: ");
	for(int z = 0; z < learnedRules.size(); z++) {
	    if(z == 0) {
		System.out.print("("+(String)learnedRules.get(z)+")");
	    }
	    else {
		System.out.print(" OR ("+(String)learnedRules.get(z)+")");
	    }
	}
	System.out.println();	
	return learnedRules;
    }

    private String learnOneRule() {
	String bestHyp = "";
	Vector candidateHypotheses = new Vector();
	Vector testedHypotheses = new Vector();//keep track of ones we already ran so don't run them again
	BlockScheme bs = new BlockScheme(attributes, methods, matchTable, forMatch, toMatch);

	//for keeping k best information
	int K = 3; //we can change this
	Vector k_best = new Vector();
	for(int x = 0; x < K+1; x++) { //the best hypothesis is k_best(0), the rest of the vector holds the other K
	    k_best.add((String)"");
	}
	double bestPerformance = -1.0;

	int iteration = 0;
	while(iteration == 0 || candidateHypotheses.size() > 0) {
	    //add to this, each of the (attribute,method) pairs
	    Vector allConstraints = getAttributeMethodPairs();
	    Vector newCandidateHypotheses = new Vector();

	    if(iteration == 0) {
		//means very first time, so we are using null hypothesis
		for(int j = 0; j < allConstraints.size(); j++) {	
		    newCandidateHypotheses.add((String)allConstraints.get(j));
		} 
	    }
	    else {
		for(int i = 0; i < candidateHypotheses.size(); i++) {
		    for(int j = 0; j < allConstraints.size(); j++) {	
			if(((String)candidateHypotheses.get(i)).trim().length() > 0) {
			    newCandidateHypotheses.add((String)candidateHypotheses.get(i)+"&"
						       +(String)allConstraints.get(j));
			}
			else {
			    newCandidateHypotheses.add((String)allConstraints.get(j));
			}
		    }
		}	 
	    }
	    //now remove any new candidate hypotheses that: duplicates, inconsistent or not max. specific
	    Vector newCandHypotheses = cleanUp(newCandidateHypotheses, testedHypotheses);
	    //System.out.println("CANDS: "+candidateHypotheses);
	    //System.out.println("TESTED: "+testedHypotheses);
	    //System.out.println("NCH: "+newCandHypotheses);

	    //now, get the best hypothesis
	    //also, keep the last k ones..in a vector, and shift them down as you update the best
	    for(int a = 0; a < newCandHypotheses.size(); a++) {
		double currPerformance = performance((String)newCandHypotheses.get(a), bs);
		if(currPerformance > bestPerformance) {
		    //update the k_best vector, set index = 0 to best, and shift the rest down, knocking off the last
		    k_best = updateKBest((String)newCandHypotheses.get(a), k_best, K);
		    bestHyp = (String)k_best.get(0);
		    System.out.println("UPDATE K BEST: "+k_best);
		    bestPerformance = currPerformance;
		    member_bestPerformance = currPerformance;
		}

		//add it to the tested hypotheses vector
		if(!testedHypotheses.contains((String)newCandHypotheses.get(a))) {
		    testedHypotheses.add((String)newCandHypotheses.get(a));
		}
	    }

	    //Update Candidate_hypotheses
	    // Candidate_hypotheses <-- the k best members of New_candidate_hypotheses, according to 
	    //the PERFORMANCE measure
	    candidateHypotheses.clear();
	    for(int z = 0; z < k_best.size(); z++) {
		String kb = (String)k_best.get(z);
		if(kb.trim().length() > 0) {
		    candidateHypotheses.add((String)k_best.get(z));
		}
		if(testedHypotheses.contains((String)k_best.get(z))) {
		    int idx = k_best.indexOf((String)k_best.get(z));
		    k_best.set(idx,"");
		}
	    }
	    iteration++;
	}
	bs.dropCacheTable();
	return bestHyp;//this is the best hypothesis
    }

    public static Vector subsume(Vector rules, String newRule) {
	for(int i = 0; i < rules.size(); i++) {
	    String[] currR = ((String)rules.get(i)).split("&");
	    String[] newR = newRule.split("&");
	    int cnt = 0;
	    for(int x = 0; x < newR.length; x++) {
		for(int y = 0; y < currR.length; y++) {
		    if(newR[x].equalsIgnoreCase(currR[y])) {
			cnt++;
		    }
		}
	    }
	    if(cnt == newR.length) {
		//System.out.println(newRule+" SUBSUMES "+(String)rules.get(i));
		rules.removeElementAt(i);
		i--;
	    }
	}
	rules.add((String)newRule);
	return rules;
    }

    /*
      See Mitchell pages 276-278
      //////////////////////////////////////
      SEQUENTIAL-COVERING(Target_attribute,Attributes,Examples,Threshold)
      //Returns a disjunctive set of rules, where the rules are conjunctions (or singletons)
      * Learned_rules <-- {}
      * Rule <-- LEARN-ONE-RULE(Target_attribute,Attributes,Examples)
      * while PERFORMANCE(Rule, Exmaples) > Threshold, do
             * Learned_rules <-- Learned_rules + Rule
	     * Examples <-- Examples - {examples correctly classified by Rule}
	     * Rule <-- LEARN-ONE-RULE(Target_attribute,Attributes,Examples)
      * Learned_rules <-- sort Learned_rules according to PERFORMANCE over Examples
      * return Learned_rules

      /////////////////////////////////////
      LEARN-ONE-RULE(Target_attribute,Attributes,Examples,k)
      //Returns a signle rule that covers some of the Examples. Conducts a general_to_specific
      //greedy beam search for the best rule, guided by the PERFORMANCE metric.

      * Initialize Best_hypothesis to the most general hypothesis NULL
      * Initialize Candidate_hypotheses to the set {Best_hypothesis}
      * While Candidate_hypotheses is no empty, Do
              1) Generate the next more specific candidate_hypotheses
	                * All_constraints <-- the set of all constraints of the form (a = v), where a is a member of 
			  Attributes and v is a value of a that occurs in the current set of Examples
			* New_candidate_hypotheses <-- 
			      for each h in Candidate_hypotheses,
			           for each c in All_constriants
				      * create a specialization of h by adding the constraint c
			* Remove from New_candidate_hypothesis any hypotheses that are duplicates, inconsistent, 
			  or not maximally specific
	      2) Update Best_hypothesis
	                * For all h in New_candidate_hypotheses do
			      * If (PERFORMANCE(h,Examples,Target_attribute)) >
			           (PERFORMANCE(Best_hypothesis,Examples,Target_attribute))
				Then Best_hypothesis <-- h
	      3) Update Candidate_hypotheses
	                * Candidate_hypotheses <-- the k best members of New_candidate_hypotheses, according to 
			  the PERFORMANCE measure
      * Return a rule of the form
        "IF Best_hypothesis THEN prediction"
	where prediction is the most frequent value of Target_attribute among those Examples that match Best_hypothesis


     //////////////////////////////////////
     PERFORMANCE(h, Examples, Target_attribute)
     * h_examples <-- the subset of Examples that match h
     * return - Entropy(h_examples), where entropy is with respect to Target_attribute
     

     ////////////////////////////////////
     Our modifications: Target_attribute should be a true positive (match/cand)
     Can we use RR for PERFORMANCE instead of entropy? This way we get the ones that cover the most PC while having best RR

    */

}