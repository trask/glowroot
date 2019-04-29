/*
 * Copyright 2019 the original author or authors.
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
package org.glowroot.xyzzy.test.harness.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import org.glowroot.xyzzy.test.harness.impl.JavaagentServer.Command;

class JavaagentClient {

    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    JavaagentClient(int port) throws IOException {
        socket = new Socket("localhost", port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    public void ping() throws Exception {
        out.writeObject(Command.PING);
        in.readObject();
    }

    public void executeApp(String name) throws Exception {
        out.writeObject(Command.EXECUTE_APP);
        out.writeObject(name);
        Object response = in.readObject();
        if (response instanceof Throwable) {
            throw new Exception((Throwable) response);
        }
    }

    public void resetConfig() throws Exception {
        out.writeObject(Command.RESET_CONFIG);
        in.readObject();
    }

    public void kill() throws Exception {
        out.writeObject(Command.KILL);
        in.readObject();
        socket.close();
    }
}
