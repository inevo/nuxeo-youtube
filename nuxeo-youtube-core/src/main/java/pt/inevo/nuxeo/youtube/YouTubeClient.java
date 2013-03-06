package pt.inevo.nuxeo.youtube;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProviderRegistry;
import org.nuxeo.runtime.api.Framework;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTube.Videos.Insert;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.Video;

public class YouTubeClient {

    private static final Log log = LogFactory.getLog(YouTubeClient.class);

    public static String YOUTUBE_PROVIDER = "YouTubePublisher";

    /** Encoded URL of Google's end-user authorization server. */
    public static final String AUTHORIZATION_SERVER_URL = "https://accounts.google.com/o/oauth2/auth";

    /** Encoded URL of Google's token server. */
    public static final String TOKEN_SERVER_URL = "https://accounts.google.com/o/oauth2/token";

    /** Global instance of the HTTP transport. */
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();

    YouTube youtube;

    AuthorizationCodeFlow flow;

    Credential credential;

    public YouTubeClient(Credential credential) throws Exception {
        this.credential = credential;
    }

    protected OAuth2ServiceProviderRegistry getOAuth2ServiceProviderRegistry() {
        return Framework.getLocalService(OAuth2ServiceProviderRegistry.class);
    }

    public boolean isAuthorized() {
        return (credential != null && credential.getAccessToken() != null);
    }

    public YouTube getYouTube() throws IOException {

        // if credential found with an access token, invoke the user code
        if (youtube == null) {
            youtube = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY,
                    credential).setApplicationName("EnerGeo/1.0").build();
        }
        return youtube;
    }

    public List<Video> getVideos() throws IOException {
        return getYouTube().videos().list(null, "snippet").execute().getItems();
    }

    public List<Channel> getChannels() throws IOException {
        return getYouTube().channels().list("snippet").setMine(true).execute().getItems();
    }

    public List<PlaylistItem> getVideos(Channel channel) throws IOException {
        String id = channel.getId();
        return getYouTube().playlistItems().list("snippet").setPlaylistId(id).execute().getItems();
    }

    public Video upload(Video video, InputStream stream, String type,
            long length) throws IOException {
        YouTubeUploaderProgressListener progressListener = new YouTubeUploaderProgressListener() {

            @Override
            public void onStart() {
                log.info("Upload starting...");
            }

            @Override
            public void onProgress(double progress) {
                log.info("Upload percentage: " + progress);
            }

            @Override
            public void onComplete() {
                // TODO Auto-generated method stub

            }

            @Override
            public void onError() {

            }

        };
        return upload(video, stream, type, length, progressListener);
    }

    public Video upload(Video video, InputStream stream, String type,
            long length, final YouTubeUploaderProgressListener uploadListener)
            throws IOException {
        InputStreamContent mediaContent = new InputStreamContent(type, stream);
        mediaContent.setLength(length);

        Insert insert = getYouTube().videos().insert(
                "snippet,statistics,status", video, mediaContent);

        // Set the upload type and add event listener.
        MediaHttpUploader uploader = insert.getMediaHttpUploader();

        /*
         * Sets whether direct media upload is enabled or disabled. True = whole
         * media content is uploaded in a single request. False (default) =
         * resumable media upload protocol to upload in data chunks.
         */
        uploader.setDirectUploadEnabled(false);

        MediaHttpUploaderProgressListener progressListener = new MediaHttpUploaderProgressListener() {
            @Override
            public void progressChanged(MediaHttpUploader uploader)
                    throws IOException {
                switch (uploader.getUploadState()) {
                case INITIATION_STARTED:
                case INITIATION_COMPLETE:
                    uploadListener.onStart();
                    break;
                case MEDIA_IN_PROGRESS:
                    uploadListener.onProgress(uploader.getProgress());
                    break;
                case MEDIA_COMPLETE:
                    uploadListener.onComplete();
                    break;
                case NOT_STARTED:
                    System.out.println("Upload Not Started!");
                    break;
                }
            }
        };
        uploader.setProgressListener(progressListener);

        // Execute upload.
        Video returnedVideo = insert.execute();
        // Print out returned results.
        log.info("\n================== Returned Video ==================\n");
        log.info("  - Id: " + returnedVideo.getId());
        log.info("  - Title: " + returnedVideo.getSnippet().getTitle());
        log.info("  - Tags: " + returnedVideo.getSnippet().getTags());
        log.info("  - Privacy Status: "
                + returnedVideo.getStatus().getPrivacyStatus());
        log.info("  - Video Count: "
                + returnedVideo.getStatistics().getViewCount());
        return returnedVideo;

    }

}
