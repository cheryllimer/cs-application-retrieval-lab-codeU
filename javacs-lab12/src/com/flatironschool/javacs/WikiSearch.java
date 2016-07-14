package com.flatironschool.javacs;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.ArrayList;

import redis.clients.jedis.Jedis;


/**
 * Represents the results of a search query.
 *
 */
public class WikiSearch {

	// map from URLs that contain the term(s) to relevance score
	private Map<String, Integer> map;

	/**
	 * Constructor.
	 *
	 * @param map
	 */
	public WikiSearch(Map<String, Integer> map) {
		this.map = map;
	}

	/**
	 * Looks up the relevance of a given URL.
	 *
	 * @param url
	 * @return
	 */
	public Integer getRelevance(String url) {
		Integer relevance = map.get(url);
		return relevance==null ? 0: relevance;
	}

	/**
	 * Prints the contents in order of term frequency.
	 *
	 * @param map
	 */
	private  void print() {
		List<Entry<String, Integer>> entries = sort();
		for (Entry<String, Integer> entry: entries) {
			System.out.println(entry);
		}
	}

	/**
	 * Computes the union of two search results.
	 *
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch or(WikiSearch that)
	{
        Map<String, Integer> union = new HashMap<String, Integer>();
        Set<String> keys = map.keySet();
        for(String current: keys)
        {
        	union.put(current, totalRelevance(getRelevance(current), that.getRelevance(current)));
        }

        keys = that.map.keySet();
        for(String current: keys)
        {
        	if(!union.containsKey(current))
        		union.put(current, that.getRelevance(current));
        }

        return new WikiSearch(union);
	}

	/**
	 * Computes the intersection of two search results.
	 *
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch and(WikiSearch that)
	{
		Map<String, Integer> inter = new HashMap<String, Integer>();
        Set<String> keys = map.keySet();
        for(String current: keys)
        {
        	if(that.map.containsKey(current))
        		inter.put(current, totalRelevance(getRelevance(current), that.getRelevance(current)));
        }

        return new WikiSearch(inter);
	}

	/**
	 * Computes the intersection of two search results.
	 *
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch minus(WikiSearch that)
	{
		Map<String, Integer> inter = new HashMap<String, Integer>();
        Set<String> keys = map.keySet();
        for(String current: keys)
        {
        	if(!(that.map.containsKey(current)))
        		inter.put(current, totalRelevance(getRelevance(current), that.getRelevance(current)));
        }

        return new WikiSearch(inter);
	}

	/**
	 * Computes the relevance of a search with multiple terms.
	 *
	 * @param rel1: relevance score for the first search
	 * @param rel2: relevance score for the second search
	 * @return
	 */
	protected int totalRelevance(Integer rel1, Integer rel2) {
		// simple starting place: relevance is the sum of the term frequencies.
		return rel1 + rel2;
	}

	/**
	 * Sort the results by relevance.
	 *
	 * @return List of entries with URL and relevance.
	 */
	public List<Entry<String, Integer>> sort()
	{
		List<Entry<String, Integer>> list = new ArrayList<Entry<String, Integer>>();
		for(Entry<String, Integer> entry : map.entrySet())
		{
			list.add(entry);
		}

		Comparator<Entry<String, Integer>> compare = new Comparator<Entry<String, Integer>>() {
	            @Override
	            public int compare(Entry<String, Integer> first, Entry<String, Integer> second)
	            {
	            	if (first.getValue() < second.getValue())
	            	{
	                    return -1;
	                }
	                if (first.getValue() > second.getValue())
	                {
	                    return 1;
	                }
	                return 0;
	            }
	        };

	        Collections.sort(list, compare);
        return list;
	}

	/**
	 * Performs a search and makes a WikiSearch object.
	 *
	 * @param term
	 * @param index
	 * @return
	 */
	public static WikiSearch search(String term, JedisIndex index) {
		Map<String, Integer> map = index.getCounts(term);
		return new WikiSearch(map);
	}

	public static void main(String[] args) throws IOException {

		// make a JedisIndex
		Jedis jedis = JedisMaker.make();
		JedisIndex index = new JedisIndex(jedis);

		// search for the first term
		String term1 = "java";
		System.out.println("Query: " + term1);
		WikiSearch search1 = search(term1, index);
		search1.print();

		// search for the second term
		String term2 = "programming";
		System.out.println("Query: " + term2);
		WikiSearch search2 = search(term2, index);
		search2.print();

		// compute the intersection of the searches
		System.out.println("Query: " + term1 + " AND " + term2);
		WikiSearch intersection = search1.and(search2);
		intersection.print();
	}
}
