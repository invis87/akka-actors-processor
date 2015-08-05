package com.processor.actors.workers.xmlfile;

import akka.actor.*;
import akka.japi.Creator;
import akka.japi.Function;
import akka.japi.Procedure;
import com.processor.actors.HashMapAbstractActor;
import com.processor.actors.config.TIMEOUTS;
import com.processor.actors.workers.FileMoverWorker;
import com.processor.actors.workers.IProcessFileWorker;
import com.processor.actors.workers.ISaveEntityWorker;
import com.processor.messages.IMessage;
import scala.concurrent.duration.Duration;

import java.io.IOException;

public class XmlFileWorker extends HashMapAbstractActor implements IProcessFileWorker {
    public static Props props(final ActorRef filesWorkerActor, final ActorRef saver, final Props readerProps, final Props moverProps) {
        return Props.create(new Creator<XmlFileWorker>() {
            @Override
            public XmlFileWorker create() {
                return new XmlFileWorker(filesWorkerActor, saver, readerProps, moverProps);
            }
        });
    }

    private final SupervisorStrategy strategy = new OneForOneStrategy(
            10,
            Duration.create("1 minute"),
            new Function<Throwable, SupervisorStrategy.Directive>() {
                @Override
                public SupervisorStrategy.Directive apply(Throwable t) {

                    if (t instanceof ActorInitializationException){

                        return SupervisorStrategy.stop();
                    }
                    if (t instanceof IOException) {

                        log.debug("IOException on child. Restart it.");
                        filesWorkerActor.tell(new FailureMessage("IO exception", t), getSelf());
                        return SupervisorStrategy.restart();
                    } else {

                        return SupervisorStrategy.escalate();
                    }
                }
            });

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    private final Props readerProps;
    private final Props moverProps;

    //need it because parent of this actor is Router
    private final ActorRef filesWorkerActor;
    private ActorRef readerActor;
    private ActorRef saverActor;
    private ActorRef moverActor;

    public XmlFileWorker(ActorRef filesWorkerActor, ActorRef saver, Props readerProps, Props moverProps) {
        this.filesWorkerActor = filesWorkerActor;
        this.saverActor = saver;
        this.readerProps = readerProps;
        this.moverProps = moverProps;
    }

    @Override public void preStart() {
        readerActor = getContext().actorOf(readerProps, "reader");
        moverActor = getContext().actorOf(moverProps, "mover");

        getContext().watch(readerActor);
        getContext().watch(saverActor);
        getContext().watch(moverActor);
    }

    @Override protected void fillMessageProcessors() {

        messageHandlers.put(ProcessFileMessage.class, ProcessFileMessageHandler());

        messageHandlers.put(ReadXmlFileWorker.FileReadedMessage.class, FileReadedMessageHandler());

        messageHandlers.put(FileMoverWorker.FileMovedMessage.class, FileMovedMessageHandler());

        messageHandlers.put(MoveFileMessage.class, MoveFileMessageHandler());

        messageHandlers.put(FailureMessage.class, FailureMessageHandler());

        messageHandlers.put(WrongFormatMessage.class, WrongFormatMessageHandler());

        messageHandlers.put(ISaveEntityWorker.EntityProcessedMessage.class, EntityProcessedMessageHandler());
    }


    //    ### HANDLERS ###

    private Procedure<IMessage> ProcessFileMessageHandler() {
        return new Procedure<IMessage>() {
            public void apply(IMessage message) {

                ProcessFileMessage msg = (ProcessFileMessage) message;

                AskOnFailure(
                        readerActor,
                        new ReadXmlFileWorker.ReadFileMessage(msg.getFileAbsolutePath()),
                        TIMEOUTS.ReaderTimeout,
                        new Procedure<Throwable>() {
                            @Override public void apply(Throwable failure) {
                                filesWorkerActor.tell(new FailureMessage("reader timeout exception", failure), getSelf());
                            }
                        }
                );
            }
        };
    }

    private Procedure<IMessage> FileReadedMessageHandler() {
        return new Procedure<IMessage>() {
            public void apply(IMessage message) {

                final ReadXmlFileWorker.FileReadedMessage msg = (ReadXmlFileWorker.FileReadedMessage) message;
                ISaveEntityWorker.SaveEntityMessage saveEntityMessage = new ISaveEntityWorker.SaveEntityMessage(msg.getEntity());


                saverActor.tell(saveEntityMessage, getSelf());
            }
        };
    }

    private Procedure<IMessage> MoveFileMessageHandler() {
        return new Procedure<IMessage>() {
            public void apply(IMessage message) {

                MoveFileMessage msg = (MoveFileMessage) message;
                AskOnFailure(
                        moverActor,
                        new FileMoverWorker.MoveFileMessage(msg.getFilePath()),
                        TIMEOUTS.MoverTimeout,
                        new Procedure<Throwable>() {
                            @Override public void apply(Throwable failure) {

                                filesWorkerActor.tell(new FailureMessage("Move file timeout exception", failure), getSelf());
                            }
                        }
                );
            }
        };
    }

    private Procedure<IMessage> FileMovedMessageHandler() {
        return new Procedure<IMessage>() {
            public void apply(IMessage message) {

                log.info("File moved successfully");
                filesWorkerActor.tell(new SuccessMessage(), getSelf());
            }
        };
    }

    private Procedure<IMessage> FailureMessageHandler() {
        return new Procedure<IMessage>() {
            public void apply(IMessage message) {

                filesWorkerActor.tell(message, getSelf());
            }
        };
    }

    private Procedure<IMessage> WrongFormatMessageHandler() {
        return new Procedure<IMessage>() {
            public void apply(IMessage message) {

                filesWorkerActor.tell(message, getSelf());
            }
        };
    }

    private Procedure<IMessage> EntityProcessedMessageHandler() {
        return new Procedure<IMessage>() {
            public void apply(IMessage message) {

                ISaveEntityWorker.EntityProcessedMessage msg = (ISaveEntityWorker.EntityProcessedMessage) message;
                if(msg.isSaved()){

                    log.info("File saved successfully");
                    getSelf().tell(new MoveFileMessage(msg. getEntitySourcePath()), getSelf());
                } else {
                    filesWorkerActor.tell(new FailureMessage("Fail on save entity", msg.getFailure()), getSelf());
                }
            }
        };
    }


//    ### MESSAGES ###


}
