package pt.inevo.nuxeo.youtube.publisher;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.publisher.api.AbstractBasePublishedDocumentFactory;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;
import org.nuxeo.ecm.platform.publisher.impl.core.SimpleCorePublishedDocument;
import org.nuxeo.ecm.platform.video.TranscodedVideo;
import org.nuxeo.ecm.platform.video.VideoDocument;
import org.nuxeo.ecm.webengine.oauth2.WEOAuthConstants;
import org.nuxeo.runtime.api.Framework;

import pt.inevo.nuxeo.youtube.YouTubeClient;
import pt.inevo.nuxeo.youtube.YouTubeService;

import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;

public class YouTubePublishedDocumentFactory extends AbstractBasePublishedDocumentFactory implements
		PublishedDocumentFactory {

    protected YouTubeClient youtube;

	@Override
    public PublishedDocument publishDocument(DocumentModel doc,
			PublicationNode targetNode, Map<String, String> params)
			throws ClientException {

        // We don't want to erase the current version
        //final ScopedMap ctxData = doc.getContextData();
        //ctxData.putScopedValue(ScopeType.REQUEST,VersioningService.SKIP_VERSIONING, true);


	    Video video = uploadToYouTube(doc, targetNode, params);
	    String videoId = video.getId();

	    String source = new YouTubeDocumentLocation(videoId, doc).toString();

	    doc.setProperty("dublincore", "source", source);
	    coreSession.saveDocument(doc);
        coreSession.save();

	    return new YouTubePublishedDocument(doc);
	}

    public void unpublishDocument(YouTubePublishedDocument doc)
            throws ClientException {


    }

	@Override
    public PublishedDocument wrapDocumentModel(DocumentModel doc)
            throws ClientException {
        if (doc.isProxy()) {
            return new SimpleCorePublishedDocument(doc);
        }
        return new YouTubePublishedDocument(doc);
    }

	 protected YouTubeClient getYouTubeClient() throws Exception {
	     if (youtube == null) {
	         YouTubeService youTubeService = Framework.getLocalService(YouTubeService.class);
	         youtube = youTubeService.getYouTubeClient(WEOAuthConstants.INSTALLED_APP_USER_ID);
	     }
	     return youtube;
	 }

	 protected Video uploadToYouTube(DocumentModel doc,
	            PublicationNode targetNode, Map<String, String> params)
	            throws ClientException {

	        VideoDocument videoDocument = doc.getAdapter(VideoDocument.class);

	        Video youtubeVideo = new Video();

	        //VideoContentDetails contentDetails = new VideoContentDetails();
	        //youtubeVideo.setContentDetails(contentDetails);

	        // set as unlisted for now
	        VideoStatus status = new VideoStatus();
	        status.setPrivacyStatus("unlisted");
	        youtubeVideo.setStatus(status);

	        VideoSnippet snippet = new VideoSnippet();
	        snippet.setTitle(doc.getTitle());
	        // TODO - add description
	        snippet.setDescription(doc.getTitle());

	        List<String> tags = new ArrayList<String>();
	        snippet.setTags(tags);

	        youtubeVideo.setSnippet(snippet);

	        TranscodedVideo transcoded = videoDocument.getTranscodedVideo("MP4 480p");

	        try {
	            Blob blob = transcoded.getBlob();

	            String mimeType = blob.getMimeType();
	            long length = blob.getLength();

	            // Insert a file
	            java.io.File file = java.io.File.createTempFile("nuxeo-youtube-uploader-", ".tmp");
	            blob.transferTo(file);
	            InputStream stream = new BufferedInputStream(new FileInputStream(file));

	            youtubeVideo = getYouTubeClient().upload(youtubeVideo, stream, mimeType, length);
	        } catch (Exception e) {
	            youtubeVideo = null;
            }

	        return youtubeVideo;

	    }
}
