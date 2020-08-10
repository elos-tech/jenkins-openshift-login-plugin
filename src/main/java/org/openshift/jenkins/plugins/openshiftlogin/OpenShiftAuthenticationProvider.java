package org.openshift.jenkins.plugins.openshiftlogin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpTransport;

import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.GrantedAuthorityImpl;
import org.acegisecurity.userdetails.UserDetails;
import org.acegisecurity.userdetails.UserDetailsService;
import org.acegisecurity.userdetails.UsernameNotFoundException;
import org.springframework.dao.DataAccessException;

import hudson.security.GroupDetails;
import hudson.security.SecurityRealm;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import java.util.logging.Level;
import java.util.logging.Logger;


public class OpenShiftAuthenticationProvider implements UserDetailsService {

    
    static final Logger LOGGER = Logger.getLogger(OpenShiftAuthenticationProvider.class.getName());

    private static final String USER_URI = "/apis/user.openshift.io/v1/users/~";
    private static final String GROUPS_URI = "/apis/user.openshift.io/v1/groups";

    private HttpTransport transport;
    private String token;
    private String serverUrl;

    public OpenShiftAuthenticationProvider(String token, HttpTransport transport, String serverUrl) {
        this.token = token;
        this.transport = transport;
        this.serverUrl = serverUrl;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException {
        
        final Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod())
                .setAccessToken(token);
        
        OpenShiftUserInfo usrInfo = null;
        OpenShiftGroupList groups = getOpenShiftGroups(this.token, this.transport, this.serverUrl);

        HttpRequestFactory requestFactory = transport.createRequestFactory(new CredentialHttpRequestInitializer(credential));
        GenericUrl url = new GenericUrl(serverUrl + USER_URI.substring(0, USER_URI.length()-1) +"/"+ username);

        try {
            HttpRequest request = requestFactory.buildGetRequest(url);
            usrInfo = request.execute().parseAs(OpenShiftUserInfo.class);
            LOGGER.log(Level.FINE, "Loaded OCP user: " + usrInfo.getName());
        } catch (IOException e) {
            LOGGER.log(Level.INFO, "Failed to get OCP user: " + username, e);
        }
        // }
        // create a groups list for given user
        
        List<GrantedAuthority> userGroups = new ArrayList<>();
        List<OpenShiftGroupInfo> grplist = groups.getGroups();
        Iterator<OpenShiftGroupInfo> it = grplist.iterator();
        OpenShiftGroupInfo info = null;
        userGroups.add(SecurityRealm.AUTHENTICATED_AUTHORITY);
        
        while (it.hasNext()) {
            info = it.next();
            if(info.users.contains(username)) {
                userGroups.add(new GrantedAuthorityImpl(info.getName()));
                LOGGER.log(Level.FINE, "Added OCP user authority: " + info.getName() + " for user: " + username);
            }
        }
        LOGGER.fine("Loaded groups: " + userGroups.toString());
        return (UserDetails) new OpenShiftUserDetail(username, "", true, true, true, true,
                userGroups.toArray(new GrantedAuthority[userGroups.size()]));
    }

    private OpenShiftGroupList getOpenShiftGroups(String token, HttpTransport transport, String serverUrl) {
        
        OpenShiftGroupList groups = null;

        final Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod())
                    .setAccessToken(token);
        HttpRequestFactory requestFactory = transport.createRequestFactory(new CredentialHttpRequestInitializer(credential));
        GenericUrl url = new GenericUrl(serverUrl + GROUPS_URI);

        try {
            HttpRequest request = requestFactory.buildGetRequest(url);
            groups = request.execute().parseAs(OpenShiftGroupList.class);
        } catch (IOException e) {
            LOGGER.log(Level.INFO, "Failed to get OCP groups: ", e);
        }
        return groups;
    }

    public GroupDetails loadGroupByGroupname(String groupname, boolean fetchMembers) throws UsernameNotFoundException, DataAccessException {
        
        LOGGER.fine("ELOS: calling loadGroupByName: " + groupname);
        OpenShiftGroupDetails groupDetails = null;
        OpenShiftGroupInfo groupinfo = null;

        final Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod())
                .setAccessToken(this.token);
        
        HttpRequestFactory requestFactory = transport.createRequestFactory(new CredentialHttpRequestInitializer(credential));
        GenericUrl url = new GenericUrl(this.serverUrl + GROUPS_URI + "/" + groupname);
        HttpRequest request;
        
        
        if(fetchMembers) {
            try {
                request = requestFactory.buildGetRequest(url);
                groupinfo = request.execute().parseAs(OpenShiftGroupInfo.class);
            } catch (IOException e) {
                LOGGER.fine("ELOS: problem calling loadGroupByName: " + e);
            }
            groupDetails = new OpenShiftGroupDetails(groupinfo.getName());
            LOGGER.fine("ELOS: found groups: " + groupinfo.getUsers().toString());
            Set<String> members = new HashSet<String>(groupinfo.getUsers());
            groupDetails = new OpenShiftGroupDetails(groupDetails.getName(), members);
        }
        
        LOGGER.fine("ELOS: found loadGroupByName: " + groupDetails.getName() + " " + groupDetails.getMembers().toString());
        if (groupDetails == null) {
            throw new UsernameNotFoundException("ELOS: Group not found or not accessible");
        } else
            return groupDetails;
    }
    
}