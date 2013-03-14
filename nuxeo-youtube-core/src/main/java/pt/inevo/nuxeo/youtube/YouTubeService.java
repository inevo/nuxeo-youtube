package pt.inevo.nuxeo.youtube;

import java.io.IOException;
import java.util.Arrays;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.oauth2.providers.NuxeoOAuth2ServiceProvider;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProviderRegistry;
import org.nuxeo.ecm.webengine.oauth2.WEOAuthConstants;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.youtube.YouTubeScopes;

public class YouTubeService extends DefaultComponent {

    private static final Log log = LogFactory.getLog(YouTubeService.class);

    public static final String CONFIGURATION_EP = "configuration";

    /** Global instance of the HTTP transport. */
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();

    private String providerName;

    private String clientId;

    private String clientSecret;

    private String accountEmail;

    private NuxeoOAuth2ServiceProvider oauth2Provider;

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
                    throws Exception {
        if (CONFIGURATION_EP.equals(extensionPoint)) {
            ConfigurationDescriptor config = (ConfigurationDescriptor) contribution;
            providerName = config.getProvider();
            clientId = config.getClientId();
            clientSecret = config.getClientSecret();
            accountEmail = config.getAccountEmail();
        }
    }

    protected OAuth2ServiceProviderRegistry getOAuth2ServiceProviderRegistry() {
        return Framework.getLocalService(OAuth2ServiceProviderRegistry.class);
    }

    public String getAuthorizationURL() {
        AuthorizationCodeFlow flow = oauth2Provider.getAuthorizationCodeFlow(HTTP_TRANSPORT, JSON_FACTORY);

        String redirectUrl = "http://localhost:8080" + WEOAuthConstants.getInstalledAppCallbackURL(oauth2Provider.getServiceName());

        // redirect to the authorization flow
        AuthorizationCodeRequestUrl authorizationUrl = flow.newAuthorizationUrl();
        authorizationUrl.setRedirectUri(redirectUrl);

        return authorizationUrl.build();
    }

    protected NuxeoOAuth2ServiceProvider getOAuth2ServiceProvider() throws ClientException {
     // Register the system wide OAuth2 provider
        if (oauth2Provider == null) {
            OAuth2ServiceProviderRegistry oauth2ProviderRegistry = getOAuth2ServiceProviderRegistry();

            if (oauth2ProviderRegistry != null) {

                oauth2Provider = oauth2ProviderRegistry.getProvider(providerName);

                if (oauth2Provider == null) {
                    try {
                        oauth2Provider = oauth2ProviderRegistry.addProvider(
                                providerName,
                                GoogleOAuthConstants.TOKEN_SERVER_URL,
                                GoogleOAuthConstants.AUTHORIZATION_SERVER_URL,
                                clientId, clientSecret,
                                Arrays.asList(YouTubeScopes.YOUTUBE));
                    } catch (Exception e) {
                       throw new ClientException(e.getMessage());
                    }
                } else {
                    log.warn("Provider "
                            + providerName
                            + " is already in the Database, XML contribution  won't overwrite it");
                }
            }

            log.info("Please got to " + getAuthorizationURL() + "to start the authorization flow");

        }
        return oauth2Provider;
    }

    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        getOAuth2ServiceProvider();
    }

    public YouTubeClient getYouTubeClient(String userId) throws ClientException {
        YouTubeClient youTubeClient = null;
        Credential credential = null;

        // Use system wide OAuth2 provider
        if (getOAuth2ServiceProvider() != null) {
            AuthorizationCodeFlow flow = getOAuth2ServiceProvider().getAuthorizationCodeFlow(HTTP_TRANSPORT, JSON_FACTORY);
            try {
                credential = flow.loadCredential(userId);
            } catch (IOException e) {
                throw new ClientException(e.getMessage());
            }
        }

        if (credential != null && credential.getAccessToken() != null) {
            youTubeClient = new YouTubeClient(credential);
        } else {
            throw new ClientException("Failed to get YouTube credentials");
        }

        return youTubeClient;
    }


}
