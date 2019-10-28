/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2019 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2019 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.smoketest.utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class SyslogUtils {

    /**
     * Event UEI that is expected to be generated by Syslog messages sent using the {@link #sendMessage(InetSocketAddress, String, int)} call.
     */
    public static final String SYSLOG_MESSAGE_UEI = "uei.opennms.org/vendor/cisco/syslog/SEC-6-IPACCESSLOGP/aclDeniedIPTraffic";

    private static final AtomicInteger ORDINAL = new AtomicInteger();

    /**
     * Use a {@link DatagramChannel} to send a number of syslog messages to the Minion host.
     *
     * @param host Hostname to inject into the syslog message
     * @param eventCount Number of messages to send
     */
    public static void sendMessage(InetSocketAddress syslogAddr, final String host, final int eventCount) {
        List<Integer> randomNumbers = new ArrayList<>();

        for (int i = 0; i < eventCount; i++) {
            int eventNum = Double.valueOf(Math.random() * 10000).intValue();
            randomNumbers.add(eventNum);
        }

        String message = "<190>Mar 11 08:35:17 " + host + " 30128311: Mar 11 08:35:16.844 CST: %SEC-6-IPACCESSLOGP: list in110 denied tcp 192.168.10.100(63923) -> 192.168.11.128(1521), " + ORDINAL.getAndIncrement() + " packet\n";

        Set<Integer> sendSizes = new HashSet<>();

        // Test by sending over an IPv4 NIO channel
        try (final DatagramChannel channel = DatagramChannel.open(StandardProtocolFamily.INET)) {

            // Set the socket send buffer to the maximum value allowed by the kernel
            channel.setOption(StandardSocketOptions.SO_SNDBUF, Integer.MAX_VALUE);
            channel.connect(syslogAddr);

            final ByteBuffer buffer = ByteBuffer.allocate(4096);
            buffer.clear();

            for (int i = 0; i < eventCount; i++) {
                buffer.put(message.getBytes());
                buffer.flip();
                final int sent = channel.send(buffer, syslogAddr);
                sendSizes.add(sent);
                buffer.clear();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}