package com.processor.entity;

import java.util.Date;

public class XMLEntity implements IEntity{
    private final String content;
    private final Date date;
    private final String sourcePath;

    public XMLEntity(String content, Date date, String sourcePath) {
        this.content = content;
        this.date = date;
        this.sourcePath = sourcePath;
    }

    public String getContent() {
        return content;
    }

    public Date getDate() {
        return date;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof XMLEntity)) return false;

        XMLEntity entity = (XMLEntity) o;

        if (!content.equals(entity.content)) return false;
        return date.equals(entity.date);

    }

    @Override
    public int hashCode() {
        int result = content.hashCode();
        result = 31 * result + date.hashCode();
        return result;
    }

    @Override public String toString() {
        return "id: " + hashCode() + "; content: " + getContent() + "; creationDate: " + getDate();
    }

    public String getSourcePath() {
        return sourcePath;
    }
}
