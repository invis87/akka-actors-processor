package com.processor.actors.workers;

import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.japi.Creator;
import akka.japi.Function;
import akka.japi.Procedure;
import akka.routing.RoundRobinPool;
import com.processor.actors.HashMapAbstractActor;
import com.processor.actors.crazyFactory.AbstractFileWorkerProps;
import com.processor.messages.IMessage;
import scala.concurrent.duration.Duration;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

public class ProcessFilesWorker extends HashMapAbstractActor {
    public static Props props(final AbstractFileWorkerProps processFileWorkerCreator, final ActorRef dirChecker) {
        return Props.create(new Creator<ProcessFilesWorker>() {
            @Override
            public ProcessFilesWorker create() {
                return new ProcessFilesWorker(processFileWorkerCreator, dirChecker);
            }
        });
    }

    private final SupervisorStrategy strategy = new OneForOneStrategy(
            -1,
            Duration.Inf(),
            new Function<Throwable, SupervisorStrategy.Directive>() {
                @Override
                public SupervisorStrategy.Directive apply(Throwable t) {

                    if (t instanceof Exception) {
                        log.error(t, "Exception on child {}. ", getSender().path().name());
                        return SupervisorStrategy.restart();}

                    else { return SupervisorStrategy.escalate();}
                }
            });

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }


    private final ActorRef dirChecker;
    private final ActorRef fileWorkersRouter;
    private int filesToProcess;

    public ProcessFilesWorker(AbstractFileWorkerProps fileWorkerProps, ActorRef dirChecker) {
        this.dirChecker = dirChecker;

        this.fileWorkersRouter = getContext().actorOf(
                new RoundRobinPool(1000).
                        props(fileWorkerProps.props(getSelf())),
                "fileWorkersRouter");

        getContext().watch(fileWorkersRouter);
    }

    @Override protected void fillMessageProcessors() {
        messageHandlers.put(StartProcessMessage.class, StartProcessMessageHandler());

        messageHandlers.put(IProcessFileWorker.WrongFormatMessage.class, WrongFormatMessageHandler());

        messageHandlers.put(IProcessFileWorker.SuccessMessage.class, SuccessMessageHandler());

        messageHandlers.put(IProcessFileWorker.FailureMessage.class, FailureMessageHandler());

        messageHandlers.put(FileProcessedMessage.class, FileProcessedMessageHandler());
    }


    //          ### HANDLERS ###

    private Procedure<IMessage> StartProcessMessageHandler() {
        return new Procedure<IMessage>() {
            public void apply(IMessage message) {

                getSender().tell(new ProcessingStartedMessage(), getSelf());

                StartProcessMessage msg = (StartProcessMessage) message;

                filesToProcess = msg.getFileNamesToProcess().size();
                log.debug("Start processing {} files in dir {}", filesToProcess, msg.getAbsoluteDirPath());

                for (String fileName : msg.getFileNamesToProcess()) {
                    IProcessFileWorker.ProcessFileMessage processMessage = new IProcessFileWorker.ProcessFileMessage(new File(msg.getAbsoluteDirPath(), fileName).getAbsolutePath());
                    fileWorkersRouter.tell(processMessage, getSelf());
                }
            }
        };
    }

    private Procedure<IMessage> WrongFormatMessageHandler() {
        return new Procedure<IMessage>() {
            public void apply(IMessage message) {

                IProcessFileWorker.WrongFormatMessage msg = (IProcessFileWorker.WrongFormatMessage) message;
                log.info("Wrong format file: {}", msg.getFileName());

                dirChecker.tell(new DirChecker.AddFileNameToIgnoreMessage(msg.getFileName()), getSelf());
                getSelf().tell(new FileProcessedMessage(), getSelf());
            }
        };
    }

    private Procedure<IMessage> SuccessMessageHandler() {
        return new Procedure<IMessage>() {
            public void apply(IMessage message) {

                getSelf().tell(new FileProcessedMessage(), getSelf());
            }
        };
    }

    private Procedure<IMessage> FailureMessageHandler() {
        return new Procedure<IMessage>() {
            public void apply(IMessage message) {

                IProcessFileWorker.FailureMessage msg = (IProcessFileWorker.FailureMessage) message;
                log.debug("Failed to process file because {}", msg.getReason());
                getSelf().tell(new FileProcessedMessage(), getSelf());
            }
        };
    }

    private Procedure<IMessage> FileProcessedMessageHandler() {
        return new Procedure<IMessage>() {
            public void apply(IMessage message) {

                filesToProcess--;
                if(filesToProcess == 0){
                    log.debug("processed finish");
                    getContext().parent().tell(new AllFilesProcessedMessage(), getSelf());
                } else if(filesToProcess % 100 == 0){
                    log.debug("remain {} files", filesToProcess);
                }
            }
        };
    }


    //          ### MESSAGES ###

    public static class AllFilesProcessedMessage implements IMessage, Serializable {}

    public static class FileProcessedMessage implements IMessage, Serializable {}

    public static class StartProcessMessage implements IMessage, Serializable {
        private final String absoluteDirPath;
        private final Set<String> fileNamesToProcess;

        public StartProcessMessage(String absoluteDirPath, Set<String> fileNamesToProcess) {
            this.absoluteDirPath = absoluteDirPath;
            this.fileNamesToProcess = fileNamesToProcess;
        }

        public String getAbsoluteDirPath() {
            return absoluteDirPath;
        }

        public Set<String> getFileNamesToProcess() {
            return Collections.unmodifiableSet(fileNamesToProcess);
        }
    }

    public static class ProcessingStartedMessage implements IMessage, Serializable {}
}
