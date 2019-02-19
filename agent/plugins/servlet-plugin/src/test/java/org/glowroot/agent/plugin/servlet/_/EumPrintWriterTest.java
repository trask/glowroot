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
package org.glowroot.agent.plugin.servlet._;

import java.io.StringWriter;
import java.io.Writer;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EumPrintWriterTest {

    @Test
    public void shouldInjectViaPrintString() {
        // given
        StringWriter sw = new StringWriter();
        EumPrintWriter writer = new EumTestPrintWriter(sw, "xyz");

        // when
        writer.print("<head></head>");
        writer.close();

        // then
        assertThat(sw.toString()).isEqualTo("<head>xyz</head>");
    }

    @Test
    public void shouldInjectWriteString() {
        // given
        StringWriter sw = new StringWriter();
        EumPrintWriter writer = new EumTestPrintWriter(sw, "xyz");

        // when
        writer.write("<head></head>");
        writer.close();

        // then
        assertThat(sw.toString()).isEqualTo("<head>xyz</head>");
    }

    @Test
    public void shouldInjectWriteCharArray() {
        // given
        StringWriter sw = new StringWriter();
        EumPrintWriter writer = new EumTestPrintWriter(sw, "xyz");

        // when
        writer.write("<head></head>".toCharArray());
        writer.close();

        // then
        assertThat(sw.toString()).isEqualTo("<head>xyz</head>");
    }

    @Test
    public void shouldInjectWriteChar() {
        // given
        StringWriter sw = new StringWriter();
        EumPrintWriter writer = new EumTestPrintWriter(sw, "xyz");

        // when
        writer.write('<');
        writer.write('h');
        writer.write('e');
        writer.write('a');
        writer.write('d');
        writer.write('>');
        writer.write('<');
        writer.write('/');
        writer.write('h');
        writer.write('e');
        writer.write('a');
        writer.write('d');
        writer.write('>');
        writer.close();

        // then
        assertThat(sw.toString()).isEqualTo("<head>xyz</head>");
    }

    @Test
    public void shouldInjectWriteStringCharByChar() {
        // given
        StringWriter sw = new StringWriter();
        EumPrintWriter writer = new EumTestPrintWriter(sw, "xyz");

        // when
        writer.write("<");
        writer.write("h");
        writer.write("e");
        writer.write("a");
        writer.write("d");
        writer.write(">");
        writer.write("<");
        writer.write("/");
        writer.write("h");
        writer.write("e");
        writer.write("a");
        writer.write("d");
        writer.write(">");
        writer.close();

        // then
        assertThat(sw.toString()).isEqualTo("<head>xyz</head>");
    }

    @Test
    public void shouldInjectWriteCharArrayCharByChar() {
        // given
        StringWriter sw = new StringWriter();
        EumPrintWriter writer = new EumTestPrintWriter(sw, "xyz");

        // when
        writer.write("<".toCharArray());
        writer.write("h".toCharArray());
        writer.write("e".toCharArray());
        writer.write("a".toCharArray());
        writer.write("d".toCharArray());
        writer.write(">".toCharArray());
        writer.write("<".toCharArray());
        writer.write("/".toCharArray());
        writer.write("h".toCharArray());
        writer.write("e".toCharArray());
        writer.write("a".toCharArray());
        writer.write("d".toCharArray());
        writer.write(">".toCharArray());
        writer.close();

        // then
        assertThat(sw.toString()).isEqualTo("<head>xyz</head>");
    }

    @Test
    public void shouldInjectWriteStringWithOverlap() {
        String content = "<head></head>";
        for (int i = 0; i < content.length(); i++) {
            shouldInjectWriteStringWithOverlap(content, i, "<head>xyz</head>");
        }
    }

    @Test
    public void shouldInjectWriteCharArrayWithOverlap() {
        String content = "<head></head>";
        for (int i = 0; i < content.length(); i++) {
            shouldInjectWriteCharArrayWithOverlap(content, i, "<head>xyz</head>");
        }
    }

    @Test
    public void shouldInjectWriteStringWithOverlapAndSpaces() {
        String content = "<head></head    >";
        for (int i = 0; i < content.length(); i++) {
            shouldInjectWriteStringWithOverlap(content, i, "<head>xyz</head    >");
        }
    }

    @Test
    public void shouldInjectWriteCharArrayWithOverlapAndSpaces() {
        String content = "<head></head    >";
        for (int i = 0; i < content.length(); i++) {
            shouldInjectWriteCharArrayWithOverlap(content, i, "<head>xyz</head    >");
        }
    }

    @Test
    public void shouldInjectWriteStringWithOverlap2() {
        String content = "<head></head><body></body>";
        for (int i = 0; i < content.length(); i++) {
            shouldInjectWriteStringWithOverlap(content, i, "<head>xyz</head><body></body>");
        }
    }

    @Test
    public void shouldInjectWriteCharArrayWithOverlap2() {
        String content = "<head></head><body></body>";
        for (int i = 0; i < content.length(); i++) {
            shouldInjectWriteCharArrayWithOverlap(content, i, "<head>xyz</head><body></body>");
        }
    }

    @Test
    public void shouldInjectWriteStringWithOverlapAndSpaces2() {
        String content = "<head></head    ><body></body>";
        for (int i = 0; i < content.length(); i++) {
            shouldInjectWriteStringWithOverlap(content, i, "<head>xyz</head    ><body></body>");
        }
    }

    @Test
    public void shouldInjectWriteCharArrayWithOverlapAndSpaces2() {
        String content = "<head></head    ><body></body>";
        for (int i = 0; i < content.length(); i++) {
            shouldInjectWriteCharArrayWithOverlap(content, i, "<head>xyz</head    ><body></body>");
        }
    }

    private void shouldInjectWriteStringWithOverlap(String content, int splitAtIndex,
            String expected) {
        // given
        StringWriter sw = new StringWriter();
        EumPrintWriter writer = new EumTestPrintWriter(sw, "xyz");

        // when
        writer.write(content.substring(0, splitAtIndex));
        writer.write(content.substring(splitAtIndex));
        writer.close();

        // then
        assertThat(sw.toString()).isEqualTo(expected);
    }

    private void shouldInjectWriteCharArrayWithOverlap(String content, int splitAtIndex,
            String expected) {
        // given
        StringWriter sw = new StringWriter();
        EumPrintWriter writer = new EumTestPrintWriter(sw, "xyz");

        // when
        writer.write(content.substring(0, splitAtIndex).toCharArray());
        writer.write(content.substring(splitAtIndex).toCharArray());
        writer.close();

        // then
        assertThat(sw.toString()).isEqualTo(expected);
    }

    private static class EumTestPrintWriter extends EumPrintWriter {

        private final String script;

        private EumTestPrintWriter(Writer delegate, String script) {
            super(delegate);
            this.script = script;
        }

        @Override
        protected void writeScript() {
            superWrite(script, 0, script.length());
        }
    }
}
