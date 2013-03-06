package pt.inevo.nuxeo.youtube.publisher;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.publisher.api.AbstractPublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.PlaylistItem;

import pt.inevo.nuxeo.youtube.YouTubeClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nelsonsilva
 */
public class YouTubeChannelNode extends AbstractPublicationNode {

    private static final long serialVersionUID = 1L;

    YouTubeClient youtube;

    private Channel channel;

    private PublicationNode parent;

    private String sid;


	public YouTubeChannelNode(String sid, PublicationNode parent,
            PublishedDocumentFactory factory, YouTubeClient youtube, Channel channel) throws ClientException {
		this.youtube = youtube;
		this.channel = channel;
		this.parent = parent;
		this.sid = sid;
	}

    @Override
    public List<PublicationNode> getChildrenNodes() throws ClientException {
        List<PublicationNode> childrenNodes = new ArrayList<PublicationNode>();
		return childrenNodes;
	}

    @Override
    public List<PublishedDocument> getChildrenDocuments() throws ClientException {
        List<PublishedDocument> childrenDocs = new ArrayList<PublishedDocument>();
        try {
            List<PlaylistItem> videos = youtube.getVideos(channel);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
		return childrenDocs;
	}

    @Override
    public String getTitle() throws ClientException {
        return channel.getSnippet().getTitle();
    }

    @Override
    public String getName() throws ClientException {
        return channel.getSnippet().getTitle();
    }

    @Override
    public PublicationNode getParent() {
        return parent;
    }

    @Override
    public String getPath() {
        String path = channel.getId();

        return getParent().getPath() + "/" + path;
    }

    @Override
    public String getSessionId() {
        return sid;
    }


}
