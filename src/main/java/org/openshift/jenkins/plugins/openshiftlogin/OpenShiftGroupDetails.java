package org.openshift.jenkins.plugins.openshiftlogin;

import hudson.security.GroupDetails;

public class OpenShiftGroupDetails extends GroupDetails {
    
    private String name;
    
    public OpenShiftGroupDetails(String name) {
		super();
		this.name = name;
    }
    
    @Override
	public String getName() {
		return this.name;
	}

}