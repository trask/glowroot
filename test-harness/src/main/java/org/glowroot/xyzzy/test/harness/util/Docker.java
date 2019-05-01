/**
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
package org.glowroot.xyzzy.test.harness.util;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.assertj.core.util.Lists;

public class Docker {

    public static String start(String image, String... portMappings) throws IOException {
        List<String> command = Lists.newArrayList();
        command.add("docker");
        command.add("run");
        command.add("--name");
        String name = uniqueName();
        command.add(name);
        command.add("--rm");
        for (String portMapping : portMappings) {
            command.add("-p");
            command.add(portMapping);
        }
        command.add(image);
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        ConsoleOutputPipe consoleOutputPipe =
                new ConsoleOutputPipe(process.getInputStream(), System.out);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(consoleOutputPipe);
        return name;
    }

    public static void stop(String name) throws IOException {
        List<String> command = Lists.newArrayList();
        command.add("docker");
        command.add("stop");
        command.add(name);
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        ConsoleOutputPipe consoleOutputPipe =
                new ConsoleOutputPipe(process.getInputStream(), System.out);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(consoleOutputPipe);
    }

    private static String uniqueName() {
        return "xyzzy-test-harness-" + Math.abs(new Random().nextLong());
    }
}
