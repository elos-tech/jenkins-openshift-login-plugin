package org.openshift.jenkins.plugins.openshiftlogin;

import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.userdetails.User;

public class OpenShiftUserDetail extends User {

    /**
     *
     */
    private static final long serialVersionUID = 384393241873129246L;

    public OpenShiftUserDetail(String username, String password, boolean enabled, boolean accountNonExpired,
            boolean credentialsNonExpired, boolean accountNonLocked, GrantedAuthority[] authorities)
            throws IllegalArgumentException {
        super(username, password != null ? password : "PASSWORD", enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
    }
    

    
    
}