package com.processor.actors.workers.xmlfile;

import akka.actor.Props;
import akka.japi.Creator;
import akka.japi.Option;
import akka.japi.Procedure;
import com.processor.actors.HashMapAbstractActor;
import com.processor.actors.workers.IProcessFileWorker;
import com.processor.entity.IEntity;
import com.processor.entity.XMLEntity;
import com.processor.messages.IMessage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ReadXmlFileWorker extends HashMapAbstractActor {
    public static Props props(final DocumentBuilderFactory dbFactory) {
        return Props.create(new Creator<ReadXmlFileWorker>() {
            @Override
            public ReadXmlFileWorker create() throws ParserConfigurationException {
                return new ReadXmlFileWorker(dbFactory);
            }
        }).withDispatcher("io-dispatcher");
    }

    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    private final DocumentBuilder dBuilder;

    public ReadXmlFileWorker(DocumentBuilderFactory dbFactory) throws ParserConfigurationException {
        this.dBuilder = dbFactory.newDocumentBuilder();
    }

    @Override protected void fillMessageProcessors() {
        messageHandlers.put(ReadFileMessage.class, ProcessFileMessageHandler());
    }

    private Option<XMLEntity> parseEntity(File file) throws IOException, SAXException {

        Document doc;
        try {
            doc = dBuilder.parse(file);
        } catch (FileNotFoundException e) {
            log.debug("FileNotFound exception on {}", file);
            return Option.none();
        }

        doc.getDocumentElement().normalize();

        Element rootNode = doc.getDocumentElement();

        if (rootNode.getNodeName().equals("Entry") || rootNode.getChildNodes().getLength() == 2) {
            NodeList contentElements = rootNode.getElementsByTagName("content");
            NodeList dateElements = rootNode.getElementsByTagName("creationDate");

            if (contentElements.getLength() != 1 || dateElements.getLength() != 1) {
                return Option.none();
            }

            String content = contentElements.item(0).getTextContent();
            Date date;
            try {
                date = dateFormatter.parse(dateElements.item(0).getTextContent());
            } catch (ParseException e) {
                return Option.none();
            }

            return Option.some(new XMLEntity(content, date, file.getAbsolutePath()));
        }

        return Option.none();
    }


    //    ### HANDLERS ###

    private Procedure<IMessage> ProcessFileMessageHandler() {
        return new Procedure<IMessage>() {
            public void apply(IMessage message) throws IOException {

                ReadFileMessage msg = (ReadFileMessage) message;
                String filePath = msg.getFileAbsolutePath();

                File file = new File(filePath);
                log.info("Start reading file {}", file.getName());

                Option<XMLEntity> entityOption;
                try {
                    entityOption = parseEntity(file);

                } catch (SAXException e) {

                    log.debug("catch SAXException");
                    getSender().tell(new IProcessFileWorker.WrongFormatMessage(file.getName()), getSelf());
                    return;
                }

                if (entityOption.isEmpty()) {

                    getSender().tell(new IProcessFileWorker.WrongFormatMessage(file.getName()), getSelf());
                } else {

                    XMLEntity entity = entityOption.get();
                    getSender().tell(new FileReadedMessage(entity, filePath), getSelf());
                }
            }
        };
    }


    //    ### MESSAGES ###

    public static class ReadFileMessage implements IMessage, Serializable {
        private final String fileAbsolutePath;

        public ReadFileMessage(String fileAbsolutePath) {
            this.fileAbsolutePath = fileAbsolutePath;
        }

        public String getFileAbsolutePath() {
            return fileAbsolutePath;
        }
    }

    public static class FileReadedMessage implements IMessage, Serializable {
        private final IEntity entity;
        private final String absoluteFilePath;

        public FileReadedMessage(IEntity entity, String filePath) {
            this.entity = entity;
            this.absoluteFilePath = filePath;
        }

        public IEntity getEntity() {
            return entity;
        }

        public String getAbsoluteFilePath() {
            return absoluteFilePath;
        }
    }
}
