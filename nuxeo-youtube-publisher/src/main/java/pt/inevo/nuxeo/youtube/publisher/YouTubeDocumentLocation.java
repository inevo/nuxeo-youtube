package pt.inevo.nuxeo.youtube.publisher;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;

public class YouTubeDocumentLocation extends DocumentLocationImpl {

    private static final long serialVersionUID = 1L;

    private static final String YOUTUBE = "youtube.com";

    protected String videoId;

    public YouTubeDocumentLocation(String videoId, DocumentModel doc) {
        super(doc);
        this.videoId = videoId;
    }

    public YouTubeDocumentLocation(String videoId, String serverName, DocumentRef ref) {
        super(serverName, ref);
        this.videoId = videoId;
    }


    @Override
    public String toString() {
        return getServerName() + ":" + getDocRef().toString() + "@" + YOUTUBE + ":" + videoId;
    }

    public static YouTubeDocumentLocation parseString(String source) {
        String[] refParts = source.split("@");

        String serverName = refParts[0].split(":")[0];
        DocumentRef ref = new IdRef(refParts[0].split(":")[1]);

        String videoId = refParts[1].split(":")[1];

        return new YouTubeDocumentLocation(videoId, serverName, ref);
    }

    public static YouTubeDocumentLocation extractFromDoc(DocumentModel doc)
            throws ClientException {
        if (doc.hasSchema("dublincore")) {
            String source = (String) doc.getProperty("dublincore", "source");

            if (source != null) {
                return parseString(source);
            }
        }
        return null;
    }

    public String getOriginalServer() {
        return YOUTUBE;
    }

}
