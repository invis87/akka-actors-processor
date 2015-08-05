package com.processor.actors.config;

import akka.util.Timeout;

import java.util.concurrent.TimeUnit;

public class TIMEOUTS {

    public static Timeout ReaderTimeout = new Timeout(5, TimeUnit.SECONDS);

    public static Timeout MoverTimeout = new Timeout(5, TimeUnit.SECONDS);
}
