package org.amit.model;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class Twitter {

	private String twitterId;
	private String trendingKeyword;
	private List<String> topHashTags;
	private Map<String, Integer> topHashTagsWithCount;
	private String updatedOn;

	public Twitter()
	{
		this.updatedOn = new Date().toString();
	}
	
	public String getUpdatedOn() {
		return updatedOn;
	}

	public void setUpdatedOn(String updatedOn) {
		this.updatedOn = updatedOn;
	}

	public String getTwitterId() {
		return twitterId;
	}

	public void setTwitterId(String twitterId) {
		this.twitterId = twitterId;
	}

	public String getTrendingKeyword() {
		return trendingKeyword;
	}

	public void setTrendingKeyword(String trendingKeyword) {
		this.trendingKeyword = trendingKeyword;
	}

	public List<String> getTopHashTags() {
		return topHashTags;
	}

	public void setTopHashTags(List<String> topHashTags) {
		this.topHashTags = topHashTags;
	}

	public Map<String, Integer> getTopHashTagsWithCount() {
		return topHashTagsWithCount;
	}

	public void setTopHashTagsWithCount(Map<String, Integer> topHashTagsWithCount) {
		this.topHashTagsWithCount = topHashTagsWithCount;
	}

}
