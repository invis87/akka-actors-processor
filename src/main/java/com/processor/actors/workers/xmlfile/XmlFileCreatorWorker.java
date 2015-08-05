package com.processor.actors.workers.xmlfile;

import akka.actor.Props;
import akka.japi.Creator;
import akka.japi.Procedure;
import com.processor.actors.HashMapAbstractActor;
import com.processor.actors.workers.IProcessFileWorker;
import com.processor.messages.IMessage;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class XmlFileCreatorWorker extends HashMapAbstractActor implements IProcessFileWorker {
    public static Props props() {
        return Props.create(new Creator<XmlFileCreatorWorker>() {
            @Override
            public XmlFileCreatorWorker create() {
                return new XmlFileCreatorWorker();
            }
        }).withDispatcher("io-dispatcher");
    }

    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    private static final int contentStringLenght = 1024;
    private static final char[] characters = "йцукенгшщзхъёэждлорпавыфячсмитьбю1234567890qwertyuioplkjhgfdsazxcvbnm".toCharArray();

    @Override protected void fillMessageProcessors() {

        messageHandlers.put(ProcessFileMessage.class, ProcessFileMessageHandler());
    }

    private void generateXmlFile(String fileAbsolutePath) throws IOException {
        Random rnd = new Random();

        BufferedWriter bufWriter = new BufferedWriter((new FileWriter(fileAbsolutePath)));
        bufWriter.write("<Entry>");
        bufWriter.newLine();

        bufWriter.write("<content>");
        bufWriter.write(randomString(rnd));
        bufWriter.write("</content>");
        bufWriter.newLine();

        bufWriter.write("<creationDate>");
        bufWriter.write(dateFormatter.format(randomDate(rnd)));
        bufWriter.write("</creationDate>");
        bufWriter.newLine();

        bufWriter.write("</Entry>");
        bufWriter.flush();
        bufWriter.close();
    }

    private String randomString(Random rnd) {

        char[] text = new char[contentStringLenght];
        for (int i = 0; i < contentStringLenght; i++)
        {
            text[i] = characters[rnd.nextInt(characters.length)];
        }
        return new String(text);
    }

    private Date randomDate(Random rnd) {
        return new Date(rnd.nextInt());
    }


    //          ### HANDLERS ###

    private Procedure<IMessage> ProcessFileMessageHandler() {
        return new Procedure<IMessage>() {
            public void apply(IMessage message) {

                try {
                    ProcessFileMessage msg = (ProcessFileMessage) message;

                    log.info("Start creating file {}", msg.getFileAbsolutePath());

                    generateXmlFile(msg.getFileAbsolutePath());
                    getSender().tell(new SuccessMessage(), getSelf());
                } catch (IOException e) {
                    getSender().tell(new FailureMessage("io exception on file creation", e), getSelf());
                }
            }
        };
    }

}
