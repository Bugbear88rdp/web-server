package server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class FormPart {
    private final String name;
    private final String filename;
    private final String contentType;
    private final byte[] content;

    public FormPart(String name, String filename, String contentType, byte[] content) {
        this.name = name;
        this.filename = filename;
        this.contentType = contentType;
        this.content = content;
    }

    public String getName() {
        return name;
    }
    public String getFilename() {
        return filename;
    }

    public boolean isFile() {
        return filename != null && !filename.isEmpty();
    }

    public byte[] getContent() {
        return content;
    }

    public String getString() {
        return new String(content, StandardCharsets.UTF_8);
    }

    public long getSize() {
        return content.length;
    }

    @Override
    public String toString() {
        return "FormPart{" +
                "name='" + name + '\'' +
                ", filename'" + filename + '\'' +
                ", contentType='" + contentType + '\'' +
                ", size=" + content.length +
                '}';
    }

}
