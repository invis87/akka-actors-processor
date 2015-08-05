package com.processor.actors.workers;

import akka.actor.Props;
import akka.japi.Creator;
import akka.japi.Procedure;
import com.processor.actors.HashMapAbstractActor;
import com.processor.messages.*;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class DirChecker extends HashMapAbstractActor {
    public static Props props(final String pathToCheck) {
        return Props.create(new Creator<DirChecker>() {
            @Override
            public DirChecker create() {
                return new DirChecker(pathToCheck);
            }
        });
    }

    private final FilenameFilter fileFilter = new FilenameFilter() {
        @Override public boolean accept(File dir, String name) {
            return !filesToIgnore.contains(name);
        }
    };

    private static final int MAX_FILES_TO_PROCESS = 10000;
    private final HashSet<String> filesToIgnore;
    private final String pathToCheck;

    public DirChecker(String pathToCheck) {
        this.filesToIgnore = new HashSet<>();
        this.pathToCheck =  pathToCheck;
    }

    @Override public void preRestart(Throwable reason, scala.Option<Object> message) {
        filesToIgnore.clear();
    }

    protected void fillMessageProcessors() {
        messageHandlers.put(CheckDirMessage.class, CheckDirMessageHandler());

        messageHandlers.put(AddFileNameToIgnoreMessage.class, AddFileNameToIgnoreMessageHandler());
    }

    private void addFilePathToIgnore(String absolutePath) {
        filesToIgnore.add(absolutePath);
    }


    //          ### HANDLERS ###

    private Procedure<IMessage> CheckDirMessageHandler() {
        return new Procedure<IMessage>() {
            public void apply(IMessage message) throws IOException {

                log.debug("Start checking dir {}", pathToCheck);

                File dirToProcess = new File(pathToCheck);
                String[] fileNames = dirToProcess.list(fileFilter);


                if(fileNames == null) {
                    throw new IOException("IOException while checking directory.");
                }

                if(fileNames.length > 0) {
                    List<String> filesToProcess;
                    if(fileNames.length > MAX_FILES_TO_PROCESS){
                        filesToProcess = new ArrayList<>(MAX_FILES_TO_PROCESS);
                        for(int i = 0; i < MAX_FILES_TO_PROCESS; i++) {
                            filesToProcess.add(fileNames[i]);
                        }
                    }else {
                        filesToProcess = Arrays.asList(fileNames);
                    }

                    getSender().tell(new DirCheckedMessage(pathToCheck, filesToProcess), getSelf());
                    return;
                }

                getSender().tell(new DirIsFineMessage(), getSelf());
            }
        };
    }

    private Procedure<IMessage> AddFileNameToIgnoreMessageHandler() {
        return new Procedure<IMessage>() {
            public void apply(IMessage message) {

                AddFileNameToIgnoreMessage msg = (AddFileNameToIgnoreMessage) message;
                log.debug("adding {} to ignore list", msg.getFileName());
                addFilePathToIgnore(msg.getFileName());
            }
        };
    }


    //          ### MESSAGES ###

    public static class CheckDirMessage implements IMessage, Serializable {}

    public static class DirIsFineMessage implements IMessage, Serializable {}

    public static class AddFileNameToIgnoreMessage implements IMessage, Serializable {
        private final String fileName;

        public AddFileNameToIgnoreMessage(String fileName){
            this.fileName = fileName;
        }

        public String getFileName() {
            return fileName;
        }
    }

    public static class DirCheckedMessage implements IMessage, Serializable {
        private final String dirAbsolutePath;
        private final HashSet<String> fileNamesToProcess;

        public DirCheckedMessage(String dirAbsolutePath, List<String> fileNamesToProcess) {
            this.dirAbsolutePath = dirAbsolutePath;
            this.fileNamesToProcess = new HashSet<>(fileNamesToProcess);
        }

        public String getDirAbsolutePath() {
            return dirAbsolutePath;
        }

        public Set<String> getFileNamesToProcess() {
            return Collections.unmodifiableSet(fileNamesToProcess);
        }
    }
}
