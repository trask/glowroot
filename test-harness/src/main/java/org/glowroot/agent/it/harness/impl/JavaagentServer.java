/*
 * Copyright 2015-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glowroot.agent.it.harness.impl;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.glowroot.agent.it.harness.AppUnderTest;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

class JavaagentServer {

    private static final Logger logger = LoggerFactory.getLogger(JavaagentServer.class);

    private final int port;

    JavaagentServer(int port) {
        this.port = port;
    }

    void start() throws Exception {
        ServerSocket serverSocket = new ServerSocket(port);
        Socket socket = serverSocket.accept();
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        while (true) {
            Command command = (Command) in.readObject();
            switch (command) {
                case PING:
                    out.writeObject("ok");
                    break;
                case EXECUTE_APP:
                    try {
                        String appClassName = (String) in.readObject();
                        executeApp(appClassName);
                        out.writeObject("ok");
                    } catch (Throwable t) {
                        logger.error(t.getMessage(), t);
                        out.writeObject(t);
                    }
                    break;
                case RESET_CONFIG:
                    // TODO
                    break;
                case KILL:
                    kill();
                    out.writeObject("ok");
                default:
                    throw new IllegalStateException("Unexpected command: " + command);
            }
        }
    }

    private void executeApp(String appClassName)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchMethodException, Exception {
        Class<?> appClass =
                Class.forName(appClassName, true,
                        ClassLoader.getSystemClassLoader());
        AppUnderTest app = (AppUnderTest) appClass.getConstructor().newInstance();
        app.executeApp();
    }

    private void kill() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // wait a few millis for response to be returned successfully
                    MILLISECONDS.sleep(10);
                } catch (InterruptedException e) {
                    // ignore, exiting below anyways
                }
                System.exit(0);
            }
        });
    }

    enum Command {
        PING, EXECUTE_APP, RESET_CONFIG, KILL
    }
}
