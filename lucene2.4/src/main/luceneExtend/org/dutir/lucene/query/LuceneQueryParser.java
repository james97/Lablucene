package org.dutir.lucene.query;

import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.RMultiFieldQueryParser;
import org.apache.lucene.search.RBooleanQuery;
import org.dutir.lucene.util.ApplicationSetup;

import com.google.common.collect.Lists;

public abstract class LuceneQueryParser {
	/** Encoding to be used to open all files. */
	public String desiredEncoding = ApplicationSetup.getProperty(
			"trec.encoding", "utf8");
	/**
	 * use a part of the queries, specified the queries by the startid and endid
	 */
	public boolean partial = Boolean.parseBoolean(ApplicationSetup.getProperty(
			"trec.query.partial", "false"));
	public int startid = Integer.parseInt(ApplicationSetup.getProperty(
			"trec.query.startid", "0"));
	public int endid = Integer.parseInt(ApplicationSetup.getProperty(
			"trec.query.endid", "10000"));
	public String idranges = ApplicationSetup.getProperty(
            "trec.query.range", "0-10000");

	/**
	 * use the queries with odd, or even numbers to retrieve. 0: even number, 1:
	 * odd number 2: default, use all the queries
	 */
	public int parityid = Integer.parseInt(ApplicationSetup.getProperty(
			"trec.query.parity", "2"));

	// public boolean odd = Boolean.parseBoolean(ApplicationSetup.getProperty(
	// "trec.query.odd", "false"));
	// public boolean even = Boolean.parseBoolean(ApplicationSetup.getProperty(
	// "trec.query.even", "false"));

	public String getInfo() {
		String rValue = "";
		if (partial) {
		    if (startid != 0)
		        rValue += "Q" + startid + "-" + endid;
		    else if (idranges.compareTo("0-10000") != 0)
		        rValue += "Q" + idranges.replace(",", "").trim();
		    else
		        throw new IllegalArgumentException("When parital is true, you"
		                + "should input either startid/endid pairs or using"
		                + "the trec.query.range argument");
		}
		if (parityid == 0) {
			rValue += "QEven";
		} else if (parityid == 1) {
			rValue += "QOdd";
		}
		boolean proximity = Boolean.parseBoolean(ApplicationSetup.getProperty(
				"proximity.enable", "false"));
		if (proximity) {
			float pweight = Float.parseFloat(ApplicationSetup.getProperty(
					"proximity.weight", "1.0"));
			int slop = Integer.parseInt(ApplicationSetup.getProperty(
					"proximity.slop", "1"));
			String pModel = ApplicationSetup.getProperty("proximity.model", "");
			String pType = ApplicationSetup.getProperty("proximity.type", "SD");
			rValue += "P" + pweight + "S" + slop + pModel + "_" + pType + "_";
		}
		return rValue;
	}

	public abstract int getNumberOfQueries();

	public abstract String[] getTopicFilenames();

	public abstract String getQuery(String queryNo);

	public abstract boolean hasMoreQueries();

	public abstract String nextQuery();

	public abstract String getQueryId();

	public abstract void reset();

	private List<Integer> convertRangetoList(String idranges){
	    List<Integer> ids = Lists.newArrayList();
	    String[] ranges = idranges.trim().split(",");
	    for (String range : ranges){
	        if (!range.contains("-"))
	            ids.add(Integer.parseInt(range.trim()));
	        else{
	            int start = Integer.parseInt(range.split("-")[0]);
	            int end = Integer.parseInt(range.split("-")[1]);
	            if (start > end){
	                int tmp = end;
	                end = start;
	                start = tmp;
	            }
	            for (int i = start; i <= end; i++)
	                ids.add(i);
	        }
	    }
	    
	    return ids;
	}
	
	public RBooleanQuery getNextQuery(String fields[], Analyzer analyzer) {
		if (partial) {
		    if (startid > 0)
		        while (hasMoreQueries()) {
		            RBooleanQuery query = getNextLuceneQuery(fields, analyzer);
		            int id = Integer.parseInt(query.getTopicId());
		            if (id >= startid && id <= endid) {
		                return query;
		            }
		        }
		    else if(idranges.compareTo("0-10000") != 0){
		        List ids = this.convertRangetoList(idranges);
		        while (hasMoreQueries()) {
                    RBooleanQuery query = getNextLuceneQuery(fields, analyzer);
                    int id = Integer.parseInt(query.getTopicId());
                    if (ids.contains(id))
                        return query;                  
		        }
		    }
		} else if (parityid == 0) {
			while (hasMoreQueries()) {
				RBooleanQuery query = getNextLuceneQuery(fields, analyzer);
				int id = Integer.parseInt(query.getTopicId());
				if (id%2 ==0) {
					return query;
				}
			}
		} else if (parityid == 1) {
			while (hasMoreQueries()) {
				RBooleanQuery query = getNextLuceneQuery(fields, analyzer);
				int id = Integer.parseInt(query.getTopicId());
				if (id%2 == 1) {
					return query;
				}
			}
		} else {
			return getNextLuceneQuery(fields, analyzer);
		}
		return null;
	}

	// public abstract RBooleanQuery getNextLuceneQuery(String Fields[],
	// Analyzer analyzer);

	public RBooleanQuery getNextLuceneQuery(String fields[], Analyzer analyzer) {
		try {
			if (!hasMoreQueries()) {
				return null;
			}
			String squery = this.nextQuery();
			String queryId = this.getQueryId();
			RBooleanQuery query;
			// System.out.println(queryId + ": " + squery);
			query = RMultiFieldQueryParser.parse(squery, fields, analyzer);
			query.setID(queryId);
			return query;
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	

}
