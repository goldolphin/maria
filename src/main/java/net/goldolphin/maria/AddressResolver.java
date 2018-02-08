package net.goldolphin.maria;

import java.net.InetSocketAddress;

/**
 * Created by caofuxiang on 2018/02/07.
 */
public interface AddressResolver {
    AddressResolver SYSTEM_DEFAULT = InetSocketAddress::new;

    InetSocketAddress resolve(String host, int port);
}
