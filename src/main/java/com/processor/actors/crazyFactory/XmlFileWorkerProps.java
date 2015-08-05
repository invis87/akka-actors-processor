package com.processor.actors.crazyFactory;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.processor.actors.workers.xmlfile.XmlFileWorker;

public class XmlFileWorkerProps extends AbstractFileWorkerProps {

    private final ActorRef saverActor;
    private final Props readerProps;
    private final Props moverProps;

    public XmlFileWorkerProps(ActorRef saver, Props readerProps, final Props moverProps) {
        this.saverActor = saver;
        this.readerProps = readerProps;
        this.moverProps = moverProps;
    }

    private Props getXmlProcessFileWorkerProps(ActorRef filesWorkerActor) {
        return XmlFileWorker.props(filesWorkerActor, saverActor, readerProps, moverProps);
    }

    @Override public Props props(ActorRef filesWorkerActor) {
        return getXmlProcessFileWorkerProps(filesWorkerActor);
    }
}
