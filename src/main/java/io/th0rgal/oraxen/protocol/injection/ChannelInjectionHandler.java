package io.th0rgal.oraxen.protocol.injection;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPromise;
import java.util.List;
import java.util.function.BiConsumer;

import org.jetbrains.annotations.NotNull;

@ChannelHandler.Sharable
public class ChannelInjectionHandler extends ChannelInboundHandlerAdapter {

    public enum Operation {
        INJECT(ChannelInjector::inject),
        UNINJECT(ChannelInjector::uninject);

        final BiConsumer<ChannelInjector, Channel> consumer;

        Operation(BiConsumer<ChannelInjector, Channel> consumer) {
            this.consumer = consumer;
        }

        void accept(ChannelInjector injector, Channel channel) {
            consumer.accept(injector, channel);
        }
    }

    private final List<ChannelInjector> injectors;
    private final Operation operation;

    public ChannelInjectionHandler(List<ChannelInjector> injectors, Operation operation) {
        this.injectors = injectors;
        this.operation = operation;
    }

    @Override
    public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {
        Channel channel = (Channel) msg;

        channel.pipeline().addLast(new ChannelInitializer<>() {

            @Override
            protected void initChannel(@NotNull Channel ch) {
                channel.pipeline().addLast(new ChannelDuplexHandler() {

                    @Override
                    public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {
                        ctx.pipeline().remove(this);
                        accept(ctx.channel());

                        super.channelRead(ctx, msg);
                    }

                    @Override
                    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                        ctx.pipeline().remove(this);
                        accept(ctx.channel());

                        super.write(ctx, msg, promise);
                    }
                });
            }
        });

        super.channelRead(ctx, msg);
    }

    private void accept(Channel channel) {
        injectors.forEach(injector -> operation.accept(injector, channel));
    }
}