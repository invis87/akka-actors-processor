package com.processor.actors.workers.db;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;

import akka.actor.*;
import akka.japi.Creator;
import akka.japi.Function;
import akka.japi.Procedure;
import akka.routing.RoundRobinPool;
import com.processor.actors.HashMapAbstractActor;
import com.processor.actors.workers.ISaveEntityWorker;
import com.processor.messages.IMessage;
import org.apache.commons.dbcp2.*;
import scala.Option;
import scala.concurrent.duration.Duration;

public class DBPoolActor extends HashMapAbstractActor implements ISaveEntityWorker {
    public static Props props() {
        return Props.create(new Creator<DBPoolActor>() {
            @Override
            public DBPoolActor create() throws IOException {
                return new DBPoolActor();
            }
        });
    }

    private static final String PROPERTIES_FILE = "/DBConnection.properties";
    private final Properties props;
    private BasicDataSource dataSource;
    private final ActorRef dbWorkersRouter;

    public DBPoolActor() throws IOException {
        props = new Properties();

        InputStream inputStream = DBPoolActor.class.getResourceAsStream(PROPERTIES_FILE);

        if (inputStream != null) {
            props.load(inputStream);
        } else {
            throw new FileNotFoundException("Exception during DBPoolActor creation. Property file '" + PROPERTIES_FILE + "' not found in the classpath");
        }

        SupervisorStrategy strategy = new OneForOneStrategy(
                10,
                Duration.create("1 minute"),
                new Function<Throwable, SupervisorStrategy.Directive>() {
                    @Override
                    public SupervisorStrategy.Directive apply(Throwable t) {

                        if (t instanceof SQLException) {

                            log.error(t, "SQLException on child {}. ", getSender().path().name());
                            return SupervisorStrategy.restart();
                        } else if (t instanceof ActorInitializationException) {

                            return SupervisorStrategy.restart();
                        } else {
                            return SupervisorStrategy.escalate();
                        }

                    }
                });

        this.dataSource = InitConnectionPool(props);
        this.dbWorkersRouter = getContext().actorOf(
                new RoundRobinPool(12).
                        withSupervisorStrategy(strategy).
                        props(DBSaverWorker.props(getSelf(), dataSource)).
                        withDispatcher("db-dispatcher"),
                "dbWorkersRouter");

        getContext().watch(dbWorkersRouter);
    }

    private BasicDataSource InitConnectionPool(Properties props) {
        String host = getProperty("host", props);
        String port = getProperty("port", props);
        String database = getProperty("database", props);
        String urlStartsWith = getProperty("dbConnectionUrlStartsWith", props);


        BasicDataSource connectionPool = new BasicDataSource();

        connectionPool.setDriverClassName(getProperty("driverClassname", props));
        connectionPool.setUrl(urlStartsWith + host + ":" + port + "/" + database);

        connectionPool.setUsername(getProperty("username", props));
        connectionPool.setPassword(getProperty("password", props));

        connectionPool.setInitialSize(getIntProperty("poolSize", props));
        connectionPool.setMaxConnLifetimeMillis(getIntProperty("connLifetimeMillis", props));
        connectionPool.setMaxTotal(getIntProperty("maxTotal", props));
        connectionPool.setMaxIdle(getIntProperty("maxIdle", props));
        connectionPool.setMaxWaitMillis(getIntProperty("maxWaitMillis", props));

        connectionPool.setRemoveAbandonedOnMaintenance(getBoolProperty("removeAbandoned", props));
        connectionPool.setRemoveAbandonedTimeout(getIntProperty("removeAbandonedTimeout", props));
        connectionPool.setLogAbandoned(getBoolProperty("logAbandoned", props));

        return connectionPool;
    }

    private static String getProperty(String property, Properties props) {
        return props.getProperty(property);
    }

    private static int getIntProperty(String property, Properties props) {
        return Integer.parseInt(getProperty(property, props));
    }

    private static boolean getBoolProperty(String property, Properties props) {
        return Boolean.parseBoolean(getProperty(property, props));
    }

    @Override protected void fillMessageProcessors() {
        messageHandlers.put(SaveEntityMessage.class, SaveEntityMessageHandler());

        messageHandlers.put(DBSaverWorker.EntitySavedMessage.class, EntitySavedMessageHandler());

        messageHandlers.put(DBSaverWorker.FailToSaveEntityMessage.class, FailToSaveEntityMessageHandler());
    }

    @Override public void preStart() throws Exception {
        dataSource = InitConnectionPool(props);
    }

    @Override public void preRestart(Throwable reason, Option<Object> message) throws Exception {
        super.preRestart(reason, message);

        dataSource.close();
    }

    @Override public void postStop() throws Exception {
        super.postStop();

        dataSource.close();
    }


    //    ### HANDLERS ###

    private Procedure<IMessage> SaveEntityMessageHandler() {
        return new Procedure<IMessage>() {
            public void apply(IMessage message) {

                SaveEntityMessage msg = (SaveEntityMessage) message;
                final DBSaverWorker.SaveEntityMessage saveEntityMessage = new DBSaverWorker.SaveEntityMessage(msg.getEntity(), getSender());

                dbWorkersRouter.tell(saveEntityMessage, getSelf());
            }
        };
    }

    private Procedure<IMessage> EntitySavedMessageHandler() {
        return new Procedure<IMessage>() {
            public void apply(IMessage message) {

                DBSaverWorker.EntitySavedMessage msg = (DBSaverWorker.EntitySavedMessage) message;
                msg.getRequester().tell(new EntityProcessedMessage(msg.getSourcePath()), getSelf());
            }
        };
    }

    private Procedure<IMessage> FailToSaveEntityMessageHandler() {
        return new Procedure<IMessage>() {
            public void apply(IMessage message) {

                DBSaverWorker.FailToSaveEntityMessage msg = (DBSaverWorker.FailToSaveEntityMessage) message;
                msg.getRequester().tell(new EntityProcessedMessage(msg.getFailure()), getSelf());
            }
        };
    }


    //    ### MESSAGES ###
}
