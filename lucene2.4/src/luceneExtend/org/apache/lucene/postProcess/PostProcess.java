/**
 * 
 */
package org.apache.lucene.postProcess;

import java.io.IOException;

import org.apache.lucene.search.RBooleanQuery;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TopDocCollector;

/**
 * @author yezheng
 * 
 * this interface define the contract that is used to refine the retrieved results.
 */
public interface PostProcess {
	
	TopDocCollector postProcess(RBooleanQuery query, TopDocCollector topDoc, Searcher seacher) throws IOException;
	public String getInfo();
}
