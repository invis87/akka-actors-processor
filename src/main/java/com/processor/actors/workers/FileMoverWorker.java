package com.processor.actors.workers;

import akka.actor.Props;
import akka.japi.Creator;
import akka.japi.Procedure;
import com.processor.actors.HashMapAbstractActor;
import com.processor.messages.IMessage;

import java.io.*;

public class FileMoverWorker extends HashMapAbstractActor {
    public static Props props(final String moveToDirPath) {
        return Props.create(new Creator<FileMoverWorker>() {
            @Override
            public FileMoverWorker create() {
                return new FileMoverWorker(moveToDirPath);
            }
        }).withDispatcher("io-dispatcher");
    }

    private final String moveToDirPath;

    public FileMoverWorker(String moveToDirPath) {
        this.moveToDirPath = moveToDirPath;
    }

    @Override protected void fillMessageProcessors() {
        messageHandlers.put(MoveFileMessage.class, MoveFileMessageHandler());
    }

    private void moveFile(File fileToMove, File moveToDir) throws IOException {
        InputStream inStream;
        OutputStream outStream;

            File newFile = new File(moveToDir, fileToMove.getName());

            inStream = new FileInputStream(fileToMove);
            outStream = new FileOutputStream(newFile);

            byte[] buffer = new byte[1024];

            int length;
            //copy the file content in bytes
            while ((length = inStream.read(buffer)) > 0){
                outStream.write(buffer, 0, length);
            }

            inStream.close();
            outStream.close();

            //delete the original file
            fileToMove.delete();
    }


    //          ### HANDLERS ###

    private Procedure<IMessage> MoveFileMessageHandler() {
        return new Procedure<IMessage>() {
            public void apply(IMessage message) throws IOException {

                MoveFileMessage msg = (MoveFileMessage) message;
                log.info("Start move file {} to {}", msg.getFilePath(), moveToDirPath);

                File fileToMove = new File(msg.getFilePath());
                File moveToDir = new File(moveToDirPath);

                moveFile(fileToMove, moveToDir);
                getSender().tell(new FileMovedMessage(), getSelf());
            }
        };
    }


//    ### MESSAGES ###

    public static class MoveFileMessage implements IMessage, Serializable {
        private final String filePath;

        public MoveFileMessage(String filePath) {
            this.filePath = filePath;
        }

        public String getFilePath() {
            return filePath;
        }
    }

    public static class FileMovedMessage implements IMessage, Serializable {}
}
