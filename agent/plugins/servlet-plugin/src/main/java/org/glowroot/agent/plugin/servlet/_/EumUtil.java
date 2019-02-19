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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.glowroot.agent.plugin.api.Logger;
import org.glowroot.agent.plugin.servlet.ServletAspect;

class EumUtil {

    private static final Logger logger = Logger.getLogger(EumUtil.class);

    private static final char[] hexDigits = "0123456789abcdef".toCharArray();

    private EumUtil() {}

    static byte[] readEumJs(String resourceName) {
        InputStream in = ServletAspect.class.getResourceAsStream("resources/" + resourceName);
        if (in == null) {
            return new byte[0];
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            byte[] bytes = new byte[1024];
            int len;
            while ((len = in.read(bytes)) != -1) {
                out.write(bytes, 0, len);
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return new byte[0];
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return out.toByteArray();
    }

    static String revisionHash(byte[] bytes) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
        md.update(bytes);
        return toShortHex(md.digest());
    }

    private static String toShortHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 4; i++) {
            byte b = bytes[i];
            // this logic copied from com.google.common.hash.HashCode.toString()
            sb.append(hexDigits[(b >> 4) & 0xf]).append(hexDigits[b & 0xf]);
        }
        return sb.toString();
    }
}
