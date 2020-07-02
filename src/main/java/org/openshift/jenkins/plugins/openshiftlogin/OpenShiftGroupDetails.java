package org.openshift.jenkins.plugins.openshiftlogin;

import java.util.Set;

import hudson.security.GroupDetails;

public class OpenShiftGroupDetails extends GroupDetails {
    
	private String name;
	
	private final Set<String> members;
    
    public OpenShiftGroupDetails(String name) {
		this(name, null);
	}
	
	public OpenShiftGroupDetails(String name, Set<String> members) {
		this.name = name;
		this.members = members;
	}
    
    @Override
	public String getName() {
		return this.name;
	}
	
	@Override
	public Set<String> getMembers() {
		return members;
	}

}