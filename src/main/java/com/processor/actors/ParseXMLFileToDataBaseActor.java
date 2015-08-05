package com.processor.actors;

import akka.actor.*;
import akka.japi.Creator;
import akka.japi.Function;
import akka.japi.Procedure;
import akka.pattern.AskTimeoutException;
import akka.util.Timeout;
import com.processor.actors.crazyFactory.XmlFileWorkerProps;
import com.processor.actors.workers.db.DBPoolActor;
import com.processor.actors.workers.xmlfile.ReadXmlFileWorker;
import com.processor.messages.*;
import com.processor.actors.workers.*;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public class ParseXMLFileToDataBaseActor extends HashMapAbstractActor {
    public static Props props(final String incomingDirPath, final String outgoingDirPath) {
        return Props.create(new Creator<ParseXMLFileToDataBaseActor>() {
            @Override
            public ParseXMLFileToDataBaseActor create() {
                return new ParseXMLFileToDataBaseActor(incomingDirPath, outgoingDirPath);
            }
        });
    }

    private final SupervisorStrategy strategy = new OneForOneStrategy(
            10,
            Duration.create("10 minute"),
            new Function<Throwable, SupervisorStrategy.Directive>() {
                @Override
                public SupervisorStrategy.Directive apply(Throwable t) {

                    if (t instanceof ActorInitializationException) { return SupervisorStrategy.stop(); }

                    if (t instanceof IOException) { return SupervisorStrategy.resume();}
                    if (t instanceof AskTimeoutException) { return SupervisorStrategy.restart(); }
                    if (t instanceof Exception) {
                        getSelf().tell(new StartMessage(), getSelf());
                        return SupervisorStrategy.restart();}
                    else {
                        return SupervisorStrategy.escalate();}
                }
            });

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }


    private final FiniteDuration checkDelay = Duration.create(10, TimeUnit.SECONDS);

    private ActorRef dirChecker;
    private ActorRef filesProcessor;

    public ParseXMLFileToDataBaseActor(String incomingDirPath, String outgoingDirPath) {

        Props reader = ReadXmlFileWorker.props(DocumentBuilderFactory.newInstance());
        Props mover = FileMoverWorker.props(outgoingDirPath);

        ActorRef dbSaver = getContext().actorOf(DBPoolActor.props(), "DBPoolActor");

        getContext().watch(dbSaver);

        dirChecker = getContext().actorOf(DirChecker.props(incomingDirPath), "dirChecker");

        XmlFileWorkerProps processFileWorkerCreator = new XmlFileWorkerProps(dbSaver, reader, mover);
        filesProcessor = getContext().actorOf(ProcessFilesWorker.props(processFileWorkerCreator, dirChecker), "processFilesWorker");

        getContext().watch(dirChecker);
        getContext().watch(filesProcessor);
    }

    @Override protected void fillMessageProcessors() {
        messageHandlers.put(StartMessage.class, StartMessageHandler());

        messageHandlers.put(DirChecker.DirCheckedMessage.class, DirCheckedMessageHandler());

        messageHandlers.put(DirChecker.DirIsFineMessage.class, DirIsFineMessageHandler());

        messageHandlers.put(ProcessFilesWorker.AllFilesProcessedMessage.class, AllFilesProcessedMessagesHandler());

        messageHandlers.put(ProcessFilesWorker.ProcessingStartedMessage.class, ProcessingStartedMessageHandler());
    }

    private void AskDirChecker() {
        AskOnFailure(
                dirChecker,
                new DirChecker.CheckDirMessage(),
                new Timeout(1, TimeUnit.MINUTES),
                new Procedure<Throwable>() {
                    @Override public void apply(Throwable failure) throws Exception {

                        log.error("Can't get list of files in directory to check.");
                        getSelf().tell(PoisonPill.getInstance(), getSelf());
                    }
                });
    }


    //          ### HANDLERS ###

    private Procedure<IMessage> StartMessageHandler() {
        return new Procedure<IMessage>() {
            public void apply(IMessage message){

                AskDirChecker();
            }
        };
    }

    private Procedure<IMessage> DirCheckedMessageHandler() {
        return new Procedure<IMessage>() {
            public void apply(IMessage message) {
                log.debug("Get DirCheckedMessageHandler. Start filesProcessor.");

                DirChecker.DirCheckedMessage msg = (DirChecker.DirCheckedMessage) message;

                AskOnFailure(
                        filesProcessor,
                        new ProcessFilesWorker.StartProcessMessage(msg.getDirAbsolutePath(), msg.getFileNamesToProcess()),
                        new Timeout(10, TimeUnit.SECONDS),
                        new Procedure<Throwable>() {
                            @Override public void apply(Throwable failure) throws Exception {

                                log.error("FilesProcessor didn't answer. That should not happens!");
                                getSelf().tell(PoisonPill.getInstance(), getSelf());
                            }
                        });
            }
        };
    }

    private Procedure<IMessage> DirIsFineMessageHandler() {
        return new Procedure<IMessage>() {
            public void apply(IMessage message){
                log.debug("Got DirChecker.DirIsFine message. Send check message after {} milliseconds.", checkDelay.toMillis());

                getContext().system().scheduler().scheduleOnce(checkDelay, new Runnable() {
                    @Override public void run() {
                        AskDirChecker();
                    }
                }, getContext().dispatcher());
            }
        };
    }

    private Procedure<IMessage> AllFilesProcessedMessagesHandler() {
        return new Procedure<IMessage>() {
            public void apply(IMessage message){

                log.debug("All files processed! Send dir check.");
                AskDirChecker();
            }
        };
    }

    private Procedure<IMessage> ProcessingStartedMessageHandler() {
        return new Procedure<IMessage>() {
            public void apply(IMessage message){
            }
        };
    }


    //          ### MESSAGES ###

    public static class StartMessage implements IMessage, Serializable {}

}
