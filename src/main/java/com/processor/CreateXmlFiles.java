package com.processor;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.processor.actors.crazyFactory.CreateXmlFilesWorkerProps;
import com.processor.actors.workers.ProcessFilesWorker;

import java.io.File;
import java.util.HashSet;

public class CreateXmlFiles {

    public static void main(String[] args) {

        if (args.length != 3) {
            System.out.println("Please specify directory where to create .xml files, filesCount and startIndex.");
            System.exit(0);
        }

        File filesPath = new File(args[0]);
        if(!filesPath.exists() || !filesPath.isDirectory()) {
            System.out.println("Directory to store .xml files not exists or not a directory.");
            System.exit(0);
        }

        try {
            int filesCount = Integer.parseInt(args[1]);
            int startIndex = Integer.parseInt(args[2]);

            CreateXmlFiles(filesPath.getAbsolutePath(), filesCount, startIndex);

        } catch (NumberFormatException e){

            System.out.println("second and third parameters must be integers!");
            System.exit(0);
        }
    }

    private static void CreateXmlFiles(String filePath, int filesCount, int startIndex) {
        HashSet<String> fileNames = new HashSet<>(filesCount);

        for(int i = startIndex; i < filesCount + startIndex; i++){
            String fileName = "good" + i + ".xml";
            fileNames.add(fileName);
        }

        CreateXmlFilesWorkerProps xmlFileCreatorWorker = new CreateXmlFilesWorkerProps();

        ActorSystem system = ActorSystem.create("system");
        ActorRef terminator = system.actorOf(Props.create(Terminator.class, ProcessFilesWorker.props(xmlFileCreatorWorker, ActorRef.noSender()).withDispatcher("main-dispatcher")), "terminator");

        terminator .tell(new ProcessFilesWorker.StartProcessMessage(filePath, fileNames), ActorRef.noSender());
    }

    public static class Terminator extends UntypedActor {

        private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

        private final ActorRef processFilesWorker;

        public Terminator(Props props) {

            processFilesWorker = getContext().actorOf(props, "processFilesWorker");
            getContext().watch(processFilesWorker);
        }

        @Override
        public void onReceive(Object msg) {
            if(msg instanceof ProcessFilesWorker.StartProcessMessage) {
                processFilesWorker.tell(msg, getSelf());
            } else

            if(msg instanceof ProcessFilesWorker.AllFilesProcessedMessage) {
                log.info("All files processed, shutting down system");
                getContext().system().shutdown();
            }else
            if (msg instanceof Terminated) {
                log.info("ProcessFilesWorker has terminated, shutting down system");
                getContext().system().shutdown();
            } else {
                unhandled(msg);
            }
        }
    }
}
