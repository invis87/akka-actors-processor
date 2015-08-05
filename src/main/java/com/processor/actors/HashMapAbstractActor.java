package com.processor.actors;

import akka.actor.ActorRef;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.dispatch.OnFailure;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.processor.messages.IMessage;
import scala.concurrent.Future;

import java.util.HashMap;

public abstract class HashMapAbstractActor extends UntypedActor {
    protected LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    protected final HashMap<Class, Procedure<IMessage>> messageHandlers;

    private final Procedure<Object> AskDefaultOnSuccess = new Procedure<Object>() {
        @Override public void apply(Object result) throws Exception {
            Procedure<IMessage> handler = messageHandlers.get(result.getClass());

            if(handler == null){
                log.error("Actor {} don't have handler for message {}", getSelf().path(), result.getClass());
            } else {
                handler.apply((IMessage) result);
            }
        }
    };

    private final Procedure<Throwable> AskDefaultOnFailure = new Procedure<Throwable>() {
        @Override public void apply(Throwable failure) throws Exception {
            throw new Exception(failure);
        }
    };

    public HashMapAbstractActor() {
        messageHandlers = new HashMap<>();
        fillMessageProcessors();
    }

    protected abstract void fillMessageProcessors();

    @Override public void onReceive(Object message) throws Exception {

        if(message instanceof Terminated) {
            getContext().stop(getSelf());
            return;
        }

        Procedure<IMessage> handler = messageHandlers.get(message.getClass());

        if(handler == null) {
            log.error("Actor {} don't have handler for message {}", getSelf().path(), message.getClass());
            unhandled(message);
        } else {
            IMessage msg = (IMessage) message;
            handler.apply(msg);
        }
    }

    //be care to use this. If timeout occurs - AskTimeoutException will be thrown by This actor
    public void Ask(final ActorRef actor, final Object message, final Timeout timeout) {

        Ask(actor, message, timeout, AskDefaultOnSuccess, AskDefaultOnFailure);
    }

    public void AskOnSuccess(final ActorRef actor, final Object message, final Timeout timeout, final Procedure<Object> onSuccess) {
        Ask(actor, message, timeout, onSuccess, AskDefaultOnFailure);
    }

    public void AskOnFailure(final ActorRef actor, final Object message, final Timeout timeout, final Procedure<Throwable> onFailure) {
        Ask(actor, message, timeout, AskDefaultOnSuccess, onFailure);
    }

    public void Ask(ActorRef actor, Object message, Timeout timeout, final Procedure<Object> onSuccess, final Procedure<Throwable> onFailure){
        Future<Object> askResult = Patterns.ask(actor, message, timeout);
        askResult.onSuccess(new OnSuccess<Object>() {
            @Override public void onSuccess(Object result) throws Throwable {

                onSuccess.apply(result);
            }
        }, getContext().dispatcher());

        askResult.onFailure(new OnFailure() {
            @Override public void onFailure(Throwable failure) throws Throwable {

                onFailure.apply(failure);
            }
        }, getContext().dispatcher());
    }
}
