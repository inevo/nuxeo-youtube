package pt.inevo.nuxeo.youtube.publisher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.publisher.api.AbstractPublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;
import org.nuxeo.ecm.platform.publisher.impl.core.CoreFolderPublicationNode;
import org.nuxeo.ecm.platform.publisher.impl.core.VirtualCoreFolderPublicationNode;
import org.nuxeo.ecm.webengine.oauth2.WEOAuthConstants;
import org.nuxeo.runtime.api.Framework;

import pt.inevo.nuxeo.youtube.YouTubeClient;
import pt.inevo.nuxeo.youtube.YouTubeService;

import com.google.api.services.youtube.model.Channel;

public class YouTubePublicationNode extends AbstractPublicationNode {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(CoreFolderPublicationNode.class);

    private static final String DEFAULT_SORT_PROP_NAME = "dc:title";

    protected DocumentModel folder;

    protected PublicationNode parent;

    protected String treeConfigName;

    protected PublishedDocumentFactory factory;

    protected String sid;

    YouTubeClient youtube;

    public YouTubePublicationNode(DocumentModel doc, PublicationTree tree,
            PublishedDocumentFactory factory) throws ClientException {
        folder = doc;
        treeConfigName = tree.getConfigName();
        this.factory = factory;
        sid = tree.getSessionId();
    }

    protected YouTubeClient getYouTubeClient() throws Exception {
        if (youtube == null) {
            YouTubeService youTubeService = Framework.getLocalService(YouTubeService.class);
            youtube = youTubeService.getYouTubeClient(WEOAuthConstants.INSTALLED_APP_USER_ID);
        }
        return youtube;
    }

    protected CoreSession getCoreSession() {
        return folder.getCoreSession();
    }

    @Override
    public List<PublicationNode> getChildrenNodes() throws ClientException {
        List<PublicationNode> childrenNodes = new ArrayList<PublicationNode>();
        try {
            // Update the channel list
            List<Channel> channels = getYouTubeClient().getChannels();
            for (Channel channel : channels) {
                YouTubeChannelNode youtubeChannelNode = new YouTubeChannelNode(sid, this, factory, youtube, channel);
                childrenNodes.add(youtubeChannelNode);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return childrenNodes;
    }

    @Override
    public List<PublishedDocument> getChildrenDocuments() throws ClientException {
        List<PublishedDocument> childrenDocs = new ArrayList<PublishedDocument>();
        return childrenDocs;
    }

    @Override
    public String getTitle() throws ClientException {
        return folder.getTitle();
    }

    @Override
    public String getName() throws ClientException {
        return folder.getName();
    }

    @Override
    public PublicationNode getParent() {
        if (parent == null) {
            DocumentRef docRef = folder.getParentRef();
            try {
                if (getCoreSession().hasPermission(docRef,
                        SecurityConstants.READ)) {
                    parent = new CoreFolderPublicationNode(
                            getCoreSession().getDocument(folder.getParentRef()),
                            treeConfigName, sid, factory);
                } else {
                    parent = new VirtualCoreFolderPublicationNode(
                            getCoreSession().getSessionId(), docRef.toString(),
                            treeConfigName, sid, factory);
                }
            } catch (Exception e) {
                log.error("Error while retrieving parent: ", e);
            }
        }
        return parent;
    }

    @Override
    public String getPath() {
        return folder.getPathAsString();
    }

    @Override
    public String getTreeConfigName() {
        return treeConfigName;
    }

    public DocumentRef getTargetDocumentRef() {
        return folder.getRef();
    }

    public DocumentModel getTargetDocumentModel() {
        return folder;
    }

    @Override
    public String getSessionId() {
        return sid;
    }
}
