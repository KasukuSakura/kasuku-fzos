package io.github.karlatemp.kasukufzos.image.reader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class KFzUrlStreamHandler extends URLStreamHandler {
    final KFzReaderImpl reader;

    public KFzUrlStreamHandler(KFzReader reader) {
        this.reader = (KFzReaderImpl) reader;
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        KFzReaderImpl.Node node = reader.rsNode(u.getPath());
        if (node == null) throw new FileNotFoundException(u.getPath());
        return new URLConnection(u) {
            @Override
            public void connect() throws IOException {
            }

            @Override
            public InputStream getInputStream() throws IOException {
                try {
                    return reader.resource(node);
                } catch (IOException ioe) {
                    throw ioe;
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }
        };
    }

    @Override
    protected void parseURL(URL u, String spec, int start, int limit) {
        super.parseURL(u, spec, start, limit);
        String path = u.getPath();

        while (!path.isEmpty() && path.charAt(0) == '/') path = path.substring(1);

        if (path.isEmpty()) path = "/";

        setURL(
                u,
                u.getProtocol(),
                null,
                0,
                null,
                null,
                path,
                null,
                null
        );
    }

    @Override
    protected String toExternalForm(URL u) {

        return u.getProtocol() + "://" + u.getPath();
    }
}
