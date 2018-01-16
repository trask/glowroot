/**
<<<<<<< HEAD
 * Copyright 2016-2019 the original author or authors.
=======
 * Copyright 2016-2018 the original author or authors.
>>>>>>> aaf05f926... Support Play 2.6
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
package org.glowroot.testing;

import static org.glowroot.testing.JavaVersion.JAVA6;
import static org.glowroot.testing.JavaVersion.JAVA7;
import static org.glowroot.testing.JavaVersion.JAVA8;

public class Play {

    private static final String MODULE_PATH = "agent/plugins/play-plugin";

    public static void main(String[] args) throws Exception {
        play1x();
        play2x();
    }

    private static void play1x() throws Exception {
        runPlay1x("1.1", "play-1.x");
        runPlay1x("1.1.1", "play-1.x");
        runPlay1x("1.1.2", "play-1.x");
        runPlay1x("1.2", "play-1.x");
        runPlay1x("1.2.1", "play-1.x");
        runPlay1x("1.2.1.1", "play-1.x");
        runPlay1x("1.2.2", "play-1.x");
        runPlay1x("1.2.3", "play-1.x");
        runPlay1x("1.2.4", "play-1.x");
        runPlay1x("1.2.5", "play-1.x");
        runPlay1x("1.2.5.1", "play-1.x");
        runPlay1x("1.2.5.2", "play-1.x");
        runPlay1x("1.2.5.3", "play-1.x");
        runPlay1x("1.2.5.5", "play-1.x");
        runPlay1x("1.2.5.6", "play-1.x");
        runPlay1x("1.2.6", "play-1.x");
        runPlay1x("1.2.6.1", "play-1.x");
        runPlay1x("1.2.6.2", "play-1.x");
        runPlay1x("1.2.7", "play-1.x");
        runPlay1x("1.2.7.2", "play-1.x");
        runPlay1x("1.3.0", "play-1.x");
        runPlay1x("1.3.1", "play-1.x");
        runPlay1x("1.3.2", "play-1.x");
        runPlay1x("1.3.3", "play-1.x");
        runPlay1x("1.3.4", "play-1.x");
        runPlay1x("1.4.0", "play-1.x");
        runPlay1x("1.4.1", "play-1.x");
        runPlay1x("1.4.2", "play-1.x");
    }

    private static void play2x() throws Exception {
        runPlay2x("2.1.0", "2.10.3", "2.2.2");
        runPlay2x("2.1.1", "2.10.3", "2.2.2");
        runPlay2x("2.1.2", "2.10.3", "2.2.2");
        runPlay2x("2.1.3", "2.10.3", "2.2.2");
        runPlay2x("2.1.4", "2.10.3", "2.2.2");
        runPlay2x("2.1.5", "2.10.3", "2.2.2");

        runPlay2x("2.2.0", "2.10.3", "2.2.2");
        runPlay2x("2.2.1", "2.10.3", "2.2.2");
        runPlay2x("2.2.2", "2.10.3", "2.2.2");
        runPlay2x("2.2.3", "2.10.3", "2.2.2");
        runPlay2x("2.2.4", "2.10.3", "2.2.2");
        runPlay2x("2.2.5", "2.10.3", "2.2.2");
        runPlay2x("2.2.6", "2.10.3", "2.2.2");

        runPlay2x("2.3.0", "2.11.8", "2.3.2");
        runPlay2x("2.3.1", "2.11.8", "2.3.2");
        runPlay2x("2.3.2", "2.11.8", "2.3.2");
        runPlay2x("2.3.3", "2.11.8", "2.3.2");
        runPlay2x("2.3.4", "2.11.8", "2.3.2");
        runPlay2x("2.3.5", "2.11.8", "2.3.2");
        runPlay2x("2.3.6", "2.11.8", "2.3.2");
        runPlay2x("2.3.7", "2.11.8", "2.3.2");
        runPlay2x("2.3.8", "2.11.8", "2.3.2");
        runPlay2x("2.3.9", "2.11.8", "2.3.2");
        runPlay2x("2.3.10", "2.11.8", "2.3.2");

        runPlay2x("2.4.0", "2.11.8", "2.5.3");
        runPlay2x("2.4.1", "2.11.8", "2.5.4");
        runPlay2x("2.4.2", "2.11.8", "2.5.4");
        runPlay2x("2.4.3", "2.11.8", "2.5.4");
        runPlay2x("2.4.4", "2.11.8", "2.5.4");
        runPlay2x("2.4.5", "2.11.8", "2.5.4");
        runPlay2x("2.4.6", "2.11.8", "2.5.4");
        runPlay2x("2.4.7", "2.11.8", "2.5.4");
        runPlay2x("2.4.8", "2.11.8", "2.5.4");

        runPlay2x("2.5.0", "2.11.8", "2.7.1");
        runPlay2x("2.5.1", "2.11.8", "2.7.1");
        runPlay2x("2.5.2", "2.11.8", "2.7.1");
        runPlay2x("2.5.3", "2.11.8", "2.7.1");
        runPlay2x("2.5.4", "2.11.8", "2.7.1");
        runPlay2x("2.5.5", "2.11.8", "2.7.6");
        runPlay2x("2.5.6", "2.11.8", "2.7.6");
        runPlay2x("2.5.7", "2.11.8", "2.7.6");
        runPlay2x("2.5.8", "2.11.8", "2.7.6");
        runPlay2x("2.5.9", "2.11.8", "2.7.6");
        runPlay2x("2.5.10", "2.11.8", "2.7.8");
        runPlay2x("2.5.11", "2.11.7", "2.7.8");
        runPlay2x("2.5.12", "2.11.7", "2.7.8");
        runPlay2x("2.5.13", "2.11.7", "2.7.8");
        runPlay2x("2.5.14", "2.11.7", "2.7.8");
        runPlay2x("2.5.15", "2.11.7", "2.7.8");
        runPlay2x("2.5.16", "2.11.7", "2.7.8");
        runPlay2x("2.5.17", "2.11.7", "2.7.8");
        runPlay2x("2.5.18", "2.11.7", "2.7.8");

        runPlay2x("2.6.0", "2.11.11", "2.8.8");
        runPlay2x("2.6.1", "2.11.11", "2.8.9");
        runPlay2x("2.6.2", "2.11.11", "2.8.9");
        runPlay2x("2.6.3", "2.11.11", "2.8.9");
        runPlay2x("2.6.4", "2.11.11", "2.8.9");
        runPlay2x("2.6.5", "2.11.11", "2.8.9");
        runPlay2x("2.6.6", "2.11.11", "2.8.9");
        runPlay2x("2.6.7", "2.11.11", "2.8.9");
        runPlay2x("2.6.9", "2.11.11", "2.8.9");
        runPlay2x("2.6.10", "2.11.11", "2.8.9");
        runPlay2x("2.6.11", "2.11.11", "2.8.9");
    }

    private static void runPlay2x(String playVersion, String scalaVersion, String jacksonVersion)
            throws Exception {
        String testAppVersion;
        if (playVersion.equals("2.1.0")) {
            // there are some incompatibilities between 2.1.0 and other 2.1.x
            testAppVersion = "2.1.0";
        } else {
            testAppVersion = playVersion.substring(0, playVersion.lastIndexOf('.')) + ".x";
        }
        String scalaMajorVersion = scalaVersion.substring(0, scalaVersion.lastIndexOf('.'));
        String profile;
        JavaVersion[] javaVersions;
        boolean alsoRunWithNetty = false;
        if (playVersion.startsWith("2.1")) {
            profile = "play-2.1.x";
            javaVersions = new JavaVersion[] {JAVA6, JAVA7, JAVA8};
        } else if (playVersion.startsWith("2.2.") || playVersion.startsWith("2.3.")) {
            profile = "play-2.2.x";
            javaVersions = new JavaVersion[] {JAVA6, JAVA7, JAVA8};
        } else if (playVersion.startsWith("2.4.") || playVersion.startsWith("2.5.")) {
            profile = "play-2.4.x";
            javaVersions = new JavaVersion[] {JAVA8};
        } else {
            // play version is 2.6.0+
            profile = "play-2.4.x";
            javaVersions = new JavaVersion[] {JAVA8};
            alsoRunWithNetty = true;
        }
        Util.updateLibVersion(MODULE_PATH, "play.version", playVersion);
        Util.updateLibVersion(MODULE_PATH, "scala.major.version", scalaMajorVersion);
        Util.updateLibVersion(MODULE_PATH, "scala.version", scalaVersion);
        Util.updateLibVersion(MODULE_PATH, "jackson.version", jacksonVersion);
        Util.updateLibVersion(MODULE_PATH, "test.app.version", testAppVersion);
        Util.updateLibVersion(MODULE_PATH, "test.app.language", "scala");
        Util.runTests(MODULE_PATH, new String[] {"play-2.x", profile}, javaVersions);
        Util.updateLibVersion(MODULE_PATH, "test.app.language", "java");
        Util.runTests(MODULE_PATH, new String[] {"play-2.x", profile}, javaVersions);
        if (alsoRunWithNetty) {
            Util.log("now with netty instead of default akka http");
            Util.updateLibVersion(MODULE_PATH, "test.app.language", "scala");
            Util.runTests(MODULE_PATH, new String[] {"play-2.x", profile, "play-2.6.x-netty"},
                    javaVersions);
            Util.updateLibVersion(MODULE_PATH, "test.app.language", "java");
            Util.runTests(MODULE_PATH, new String[] {"play-2.x", profile, "play-2.6.x-netty"},
                    javaVersions);
        }
    }

    private static void runPlay1x(String version, String profile) throws Exception {
        Util.updateLibVersion(MODULE_PATH, "play.version", version);
        Util.runTests(MODULE_PATH, profile, JAVA8, JAVA7, JAVA6);
    }
}
