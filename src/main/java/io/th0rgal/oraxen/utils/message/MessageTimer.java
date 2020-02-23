package io.th0rgal.oraxen.utils.message;

import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.stream.Collectors;

public class MessageTimer extends Thread {

    private static MessageTimer defaultTimer;

    public static MessageTimer getDefaultTimer() {
        if(defaultTimer != null)
            return defaultTimer;
        defaultTimer = new MessageTimer(OraxenPlugin.get());
        defaultTimer.start();
        return defaultTimer;
    }

    private final HashMap<CommandSender, List<Message>> queue = new HashMap<>();
    private final BossbarManager manager;

    public MessageTimer(Plugin plugin) {
        manager = new BossbarManager(plugin);
    }

    public final BossbarManager getBossbarManager() {
        return manager;
    }

    @Override
    public void run() {
        ArrayList<CommandSender> remove = new ArrayList<>();
        while(true) {
            try {
                manager.freeUnused();
                if(!queue.isEmpty()) {
                    for(Map.Entry<CommandSender, List<Message>> entry : queue.entrySet().stream().filter(checkEntry -> checkEntry.getValue().isEmpty()).collect(Collectors.toList())) {
                        List<Message> messages = entry.getValue();
                        int sent = 0;
                        for(Message message : messages) {
                            if (!message.delay())
                                break;
                            message.sendTo(entry.getKey());
                            sent++;
                        }
                        for(; sent != 0; sent--)
                            messages.remove(0);
                        if(messages.isEmpty())
                            remove.add(entry.getKey());
                    }
                    if(!remove.isEmpty()) {
                        for (CommandSender sender : remove)
                            queue.remove(sender);
                        remove.clear();
                    }
                }
                Thread.sleep(1000);
            } catch(Throwable throwable) {
                // Just check if timer is interrupted
                if(Thread.interrupted())
                    return;
            }
        }
    }

    public int queuedMessageCount(CommandSender sender) {
        if(!queue.containsKey(sender))
            return 0;
        return queue.get(sender).size();
    }

    public int totalQueuedMessageCount() {
        if(queue.isEmpty())
            return 0;
        int count = 0;
        for(List<Message> messages : queue.values())
            count += messages.size();
        return count;
    }

    public void queue(CommandSender sender, Message... messages) {
        if(queue.containsKey(sender))
            queue.get(sender).addAll(Arrays.asList(messages));
        else
            queue.put(sender, new ArrayList<>(Arrays.asList(messages)));
    }

    public void queue(CommandSender sender, Collection<Message> messages) {
        if(queue.containsKey(sender))
            queue.get(sender).addAll(messages);
        else
            queue.put(sender, new ArrayList<>(messages));
    }

    public void queue(CommandSender sender, Message message) {
        if(queue.containsKey(sender))
            queue.get(sender).add(message);
        else {
            ArrayList<Message> queued = new ArrayList<>();
            queued.add(message);
            queue.put(sender, queued);
        }
    }

    public void cancel(CommandSender sender) {
        queue.remove(sender);
    }
}
