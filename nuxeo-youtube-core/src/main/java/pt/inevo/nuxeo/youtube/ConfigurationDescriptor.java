package pt.inevo.nuxeo.youtube;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("configuration")
public class ConfigurationDescriptor implements Serializable {
    private static final long serialVersionUID = 1L;

    @XNode("provider")
    private String provider;

    @XNode("accountEmail")
    private String accountEmail;

    @XNode("privateKey")
    private String privateKey;

    @XNode("clientId")
    private String clientId;

    @XNode("clientSecrent")
    private String clientSecret;

	public String getAccountEmail() {
		return accountEmail;
	}

	public String getPrivateKey() {
		return privateKey;
	}

	public String getProvider() {
		return provider;
	}

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }


}
