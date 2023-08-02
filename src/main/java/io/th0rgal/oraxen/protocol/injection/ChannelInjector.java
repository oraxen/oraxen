package io.th0rgal.oraxen.protocol.injection;

import io.netty.channel.Channel;

public interface ChannelInjector {

    void inject(Channel channel);

    void uninject(Channel channel);
}