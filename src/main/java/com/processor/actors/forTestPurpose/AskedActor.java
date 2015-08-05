package com.processor.actors.forTestPurpose;

import akka.actor.Props;
import akka.japi.Creator;
import akka.japi.Procedure;
import com.processor.actors.HashMapAbstractActor;
import com.processor.messages.IMessage;

import java.io.IOException;

public class AskedActor extends HashMapAbstractActor {
    public static Props props() {
        return Props.create(new Creator<AskedActor>() {
            @Override
            public AskedActor create() throws Exception {
                return new AskedActor("VALUE");
            }
        });
    }

    private final String value;

    public AskedActor(String value) {
        this.value = value;
    }

    @Override protected void fillMessageProcessors() {

        messageHandlers.put(Ask.class, AskMessageHandler());
    }

    @Override public void preRestart(Throwable reason, scala.Option<Object> message) {
        log.debug("preRestart on AskedActor");
        log.debug("current value is {}", value);
        getSelf().tell(message.get(), getSelf());
    }

    @Override public void postRestart(Throwable reason) {
        log.debug("postRestart on AskedActor");
        log.debug("current value is {}", value);
    }

    private Procedure<IMessage> AskMessageHandler() {
        return new Procedure<IMessage>() {
            public void apply(IMessage message) throws IOException {

                log.debug("Get Asked!");
                try {Thread.sleep(2000);} catch(Exception e){}
                log.debug("Asked after timeout!");
                getSender().tell(new AskerActor.Answer(), getSelf());
            }
        };
    }

    public static class Ask implements IMessage {}
}
