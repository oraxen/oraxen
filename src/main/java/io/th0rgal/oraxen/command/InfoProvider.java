package io.th0rgal.oraxen.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class InfoProvider {

    private final ArrayList<CommandInfo> infos = new ArrayList<>();

    private int size = 10;

    /*
     * 
     */

    public InfoProvider setPageSize(int size) {
        this.size = size;
        return this;
    }

    public int getPageSize() {
        return size;
    }

    public int getPageCount() {
        return (int) Math.ceil((double) infos.size() / 10);
    }

    /*
     * 
     */

    public InfoProvider clear() {
        infos.clear();
        return this;
    }

    public InfoProvider add(CommandInfo info) {
        if (!infos.contains(info))
            write(info);
        return this;
    }

    public InfoProvider remove(CommandInfo info) {
        delete(info);
        return this;
    }

    /*
     * 
     */

    public InfoProvider addAll(CommandInfo... infos) {
        for (CommandInfo info : infos)
            add(info);
        return this;
    }

    public InfoProvider addAll(Collection<CommandInfo> infos) {
        if (infos != null && !infos.isEmpty())
            for (CommandInfo info : infos)
                add(info);
        return this;
    }

    public InfoProvider removeAll(CommandInfo... infos) {
        for (CommandInfo info : infos)
            remove(info);
        return this;
    }

    public InfoProvider removeAll(Collection<CommandInfo> infos) {
        if (infos != null && !infos.isEmpty())
            for (CommandInfo info : infos)
                remove(info);
        return this;
    }

    /*
     * 
     */

    private void write(CommandInfo info) {
        infos.add(info);
    }

    private void delete(CommandInfo info) {
        infos.remove(info);
    }

    /*
     * 
     */

    public CommandInfo getInfo(String name) {
        return null;
    }

    public List<CommandInfo> getInfos(int page) {
        ArrayList<CommandInfo> list = new ArrayList<>();

        int end = page * size;
        int start = end - size;

        int size = infos.size();

        for (int index = start; index < end; index++) {
            if (index == size)
                break;
            list.add(infos.get(index));
        }

        return list;
    }

    @SuppressWarnings("unchecked")
    public List<CommandInfo> getInfos() {
        return (List<CommandInfo>) infos.clone();
    }

}
