package pt.inevo.nuxeo.youtube.publisher;

import org.nuxeo.common.utils.Path;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.helper.VersioningHelper;

public class YouTubePublishedDocument implements PublishedDocument {

    private static final long serialVersionUID = 1L;

    protected String sourceServer;

    protected String repositoryName;

    protected DocumentRef ref;

    protected String versionLabel;

    protected String path;

    protected String parentPath;

    protected boolean isPending;

    public YouTubePublishedDocument(DocumentModel doc)
            throws ClientException {
        YouTubeDocumentLocation yLoc = YouTubeDocumentLocation.extractFromDoc(doc);
        sourceServer = yLoc.getOriginalServer();
        repositoryName = yLoc.getServerName();
        ref = yLoc.getDocRef();
        versionLabel = VersioningHelper.getVersionLabelFor(doc);
        Path p = doc.getPath();
        path = p.toString();
        parentPath = p.removeLastSegments(1).toString();
    }

    @Override
    public DocumentRef getSourceDocumentRef() {
        return ref;
    }

    @Override
    public String getSourceRepositoryName() {
        return repositoryName;
    }

    @Override
    public String getSourceServer() {
        return sourceServer;
    }

    @Override
    public String getSourceVersionLabel() {
        return versionLabel;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getParentPath() {
        return "youtube.com";
    }

    @Override
    public boolean isPending() {
        return isPending;
    }

    @Override
    public Type getType() {
        return Type.REMOTE;
    }

}

