package com.processor.sql;

import com.processor.entity.XMLEntity;

public class XmlSqlQueries {

    private static final String InsertXmlEntity = "INSERT INTO xmlentity (id, content, creationdate) VALUES ";

    private static final String SelectXmlEntity = "select * from xmlentity where id = ";

    public static String InsertXmlEntityQuery(XMLEntity entity) {
        String values = "(" + entity.hashCode() + ", '" + entity.getContent() + "', '" + entity.getDate() + "')";

        return InsertXmlEntity + values;
    }

    public static String SelectXmlEntityQuery(int id) {
        return SelectXmlEntity + id;
    }

    public static String SelectXmlEntityQuery(XMLEntity entity) {
        return SelectXmlEntityQuery(entity.hashCode());
    }
}
