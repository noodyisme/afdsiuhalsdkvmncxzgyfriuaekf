/**
 * 
 */
package com.capitalone.identity.identitybuilder.policycore.model;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Describes business logic rules that were applied to a policy
 * and their results.
 * <p>
 * By convention, policies add these entries to a list in the policy state.
 * When the policy step completes, the list is returned in the result
 * (out of band of the policy result) and also appears in the event logs.
 * 
 * @author oqu271
 */
@ApiModel(value = "Results of rules applied.")
@XmlRootElement(name = "RuleApplicaton")
@XmlAccessorType(XmlAccessType.FIELD)
public class RuleApplication implements Serializable {
	private static final long serialVersionUID = -8263533935173944463L;
	
	public enum Result {
		PASSED("Passed"),
		FAILED("Failed");
		
		private final String displayName;
		private Result(String displayName) {
			this.displayName = displayName;
		}
		
		@Override public String toString() {
			return displayName;
		}
	}
	
	@ApiModelProperty(value = "The rule that was applied.")
	private String ruleName;

	@ApiModelProperty(value = "The result of the rule (Passed or Failed).")
	private Result result;

	@ApiModelProperty(value = "An optional reason for the result.")
	private String reason;

	public RuleApplication() {
		// Required for use as a bean
	}
	
	public RuleApplication(String ruleName, Result result, String reason) {
		this.ruleName = ruleName;
		this.result = result;
		this.reason = reason;
	}

	public String getRuleName() {
		return ruleName;
	}

	public void setRuleName(String ruleName) {
		this.ruleName = ruleName;
	}

	public Result getResult() {
		return result;
	}

	public void setResult(Result result) {
		this.result = result;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
	
	@Override
	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}
	
	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this, false);
	}
}
/*
 * Copyright 2020 Capital One Financial Corporation All Rights Reserved.
 *
 * This software contains valuable trade secrets and proprietary information of
 * Capital One and is protected by law. It may not be copied or distributed in
 * any form or medium, disclosed to third parties, reverse engineered or used in
 * any manner without prior written authorization from Capital One.
 */