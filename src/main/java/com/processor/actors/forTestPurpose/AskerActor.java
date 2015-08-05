package com.processor.actors.forTestPurpose;

import akka.actor.*;
import akka.japi.Function;
import akka.japi.Procedure;
import akka.pattern.AskTimeoutException;
import akka.util.Timeout;
import com.processor.actors.HashMapAbstractActor;
import com.processor.messages.IMessage;
import com.processor.messages.IntMessage;
import scala.concurrent.duration.Duration;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AskerActor extends HashMapAbstractActor {

    private static final SupervisorStrategy strategy = new OneForOneStrategy(5,
            Duration.create("10 minute"),
            new Function<Throwable, SupervisorStrategy.Directive>() {
                @Override
                public SupervisorStrategy.Directive apply(Throwable t) {
                    if( t instanceof IOException){
                        System.out.println("in supervisor strategy");
                        return SupervisorStrategy.restart();
                    }

                    if(t instanceof ActorKilledException){
                        return SupervisorStrategy.escalate();
                    }

                    if (t instanceof IllegalArgumentException) {
                        return SupervisorStrategy.restart();
                    } else if (t instanceof AskTimeoutException) {
                        System.out.println("exception in supervisor by timeout");
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

    @Override protected void fillMessageProcessors() {

        messageHandlers.put(Answer.class, AnswerMessageHandler());

        messageHandlers.put(IntMessage.class, IntMessageHandler());
    }


    private Procedure<IMessage> AnswerMessageHandler() {
        return new Procedure<IMessage>() {
            public void apply(IMessage message) {
                log.debug("get Answer message.");
            }
        };
    }

    private Procedure<IMessage> IntMessageHandler() {
        return new Procedure<IMessage>() {
            public void apply(IMessage message) {

                final ActorRef askedActor = getContext().actorOf(AskedActor.props(), "asked");
                getContext().watch(askedActor);

                AskOnFailure(askedActor, new AskedActor.Ask(), new Timeout(1, TimeUnit.SECONDS), new Procedure<Throwable>() {
                    @Override public void apply(Throwable failure) throws Exception {

                        if(failure instanceof TimeoutException) {
                            log.debug("11111");
//                            getContext().system().shutdown();
                        } else if(failure instanceof IOException){
                            log.debug("333");
                        }
                        else {

                            log.debug("2222");
                        }

//                        System.out.println("WTF!!!");
//                        log.debug("on failure in Ask Future!!!");
                    }
                });
            }
        };
    }

    public static class Answer implements IMessage {}
}
