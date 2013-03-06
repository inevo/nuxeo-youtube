package pt.inevo.nuxeo.youtube.publisher;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.impl.core.CoreFolderPublicationNode;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.publisher.api.AbstractBasePublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;
import org.nuxeo.ecm.webengine.oauth2.WEOAuthConstants;
import org.nuxeo.runtime.api.Framework;

import com.google.api.services.youtube.model.Channel;

import pt.inevo.nuxeo.youtube.YouTubeClient;
import pt.inevo.nuxeo.youtube.YouTubeService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class YouTubePublicationTree extends AbstractBasePublicationTree implements
PublicationTree {

    private static final long serialVersionUID = 1L;

    protected static final String CAN_ASK_FOR_PUBLISHING = "CanAskForPublishing";

    protected static final String DEFAULT_ROOT_PATH = "/default-domain/youtube";

    protected YouTubeClient youtube;

    protected DocumentModel treeRoot;
    protected String sessionId;

    @Override
    public void initTree(String sid, CoreSession coreSession,
            Map<String, String> parameters, PublishedDocumentFactory factory,
            String configName, String title) throws ClientException {
        super.initTree(sid, coreSession, parameters, factory, configName, title);

        DocumentRef ref = new PathRef(rootPath);

        if (coreSession.hasPermission(ref, SecurityConstants.READ)) {
            treeRoot = coreSession.getDocument(new PathRef(rootPath));
        } else {
            sessionId = coreSession.getSessionId();
        }

        //
    }

    @Override
    public PublicationNode wrapToPublicationNode(DocumentModel documentModel)
            throws ClientException {
        if (!isPublicationNode(documentModel)) {
            throw new ClientException("Document "
                    + documentModel.getPathAsString()
                    + " is not a valid publication node.");
        }
        return new CoreFolderPublicationNode(documentModel, getConfigName(),
                sid, factory);
    }

    @Override
    public List<PublishedDocument> getExistingPublishedDocument(
            DocumentLocation docLoc) throws ClientException {

        List<PublishedDocument> allPubDocs = new ArrayList<PublishedDocument>();

        List<DocumentModel> possibleDocsToCheck = new ArrayList<DocumentModel>();

        DocumentModel livedoc = coreSession.getDocument(docLoc.getDocRef());
        if (!livedoc.isVersion()) {
            possibleDocsToCheck = coreSession.getVersions(docLoc.getDocRef());
        }

        possibleDocsToCheck.add(0, livedoc);

        for (DocumentModel doc : possibleDocsToCheck) {
            YouTubeDocumentLocation yLoc = YouTubeDocumentLocation.extractFromDoc(doc);
            if (yLoc != null) {
                allPubDocs.add(factory.wrapDocumentModel(doc));
            }
        }

        return allPubDocs;
    }

    protected CoreSession getCoreSession() {
        String coreSessionId = treeRoot == null ? sessionId
                : treeRoot.getSessionId();
        return CoreInstance.getInstance().getSession(coreSessionId);
    }

    @Override
    public PublishedDocument publish(DocumentModel doc,
        PublicationNode targetNode) throws ClientException {
       YouTubePublishedDocument publishedDocument = (YouTubePublishedDocument) factory.publishDocument(
            doc, targetNode);
       return publishedDocument;
    }

    @Override
    public PublishedDocument publish(DocumentModel doc,
        PublicationNode targetNode, Map<String, String> params)
        throws ClientException {
        YouTubePublishedDocument publishedDocument = (YouTubePublishedDocument) factory.publishDocument(
            doc, targetNode, params);
        return publishedDocument;
    }

    @Override
    public void unpublish(PublishedDocument publishedDocument)
            throws ClientException {
        if (publishedDocument instanceof YouTubePublishedDocument) {
            ((YouTubePublishedDocumentFactory) factory).unpublishDocument((YouTubePublishedDocument)publishedDocument);
        }
    }

    @Override
    public void unpublish(DocumentModel doc, PublicationNode targetNode)
        throws ClientException {
        PublicationNode realPublciationNode = getNodeByPath(targetNode.getPath());
        List<PublishedDocument> publishedDocuments = getPublishedDocumentInNode(realPublciationNode);
        String source = (String) doc.getProperty("dublincore", "source");
        for (PublishedDocument publishedDocument : publishedDocuments) {
            String publishedDocumentSource = publishedDocument.getSourceRepositoryName()
                    + "@"
                    + publishedDocument.getSourceServer()
                    + ":"
                    + publishedDocument.getSourceDocumentRef();
            if (source.equals(publishedDocumentSource)) {
                getCoreSession().removeDocument(
                        new PathRef(publishedDocument.getPath()));
                break;
            }
        }
    }

    @Override
    public PublicationNode getNodeByPath(String path) throws ClientException {
        DocumentRef docRef = new PathRef(path);
        if (path.equals(getPath()) && coreSession.hasPermission(docRef, SecurityConstants.READ)) {
            return this;
        } else {
            String id = new Path(path).lastSegment();

            Channel channel = new Channel();
            channel.setId(id);

            return new YouTubeChannelNode(coreSession.getSessionId(), this, factory, youtube, channel);
        }
    }

    @Override
    public void release() {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean canPublishTo(PublicationNode publicationNode)
            throws ClientException {
        if (publicationNode == null || publicationNode.getParent() == null) {
            // we can't publish in the root node
            return false;
        }
        // Check top level permissions only since the Node does not exist
        DocumentRef docRef = new PathRef(getPath());
        return coreSession.hasPermission(docRef, CAN_ASK_FOR_PUBLISHING);
    }

    @Override
    public boolean canUnpublish(PublishedDocument publishedDocument)
            throws ClientException {
        if (!accept(publishedDocument)) {
            return false;
        }
        // Check top level permissions only since the Node does not exist
        DocumentRef docRef = new PathRef(getPath());
        return coreSession.hasPermission(docRef, SecurityConstants.WRITE);
    }

    @Override
    protected String getDefaultRootPath() {
        return DEFAULT_ROOT_PATH;
    }

    @Override
    protected PublishedDocumentFactory getDefaultFactory() {
        return new YouTubePublishedDocumentFactory();
    }

    @Override
    protected boolean accept(PublishedDocument publishedDocument) {
        return publishedDocument instanceof YouTubePublishedDocument;
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
        return treeTitle;
    }

    @Override
    public String getName() throws ClientException {
        return treeTitle;
    }

    @Override
    public PublicationNode getParent() {
        return null;
    }

    @Override
    public String getPath() {
        return rootPath;
    }

    @Override
    public String getNodeType() {
        return this.getClass().getSimpleName();
    }

    protected YouTubeClient getYouTubeClient() throws Exception {
        if (youtube == null) {
            YouTubeService youTubeService = Framework.getLocalService(YouTubeService.class);
            youtube = youTubeService.getYouTubeClient(WEOAuthConstants.INSTALLED_APP_USER_ID);
        }
        return youtube;
    }
}
