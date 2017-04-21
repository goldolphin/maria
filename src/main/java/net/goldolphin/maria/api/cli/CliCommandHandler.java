package net.goldolphin.maria.api.cli;

import java.util.List;

/**
 * Created by caofuxiang on 2017/4/21.
 */
public interface CliCommandHandler {
    List<String> list();
    String help(String method);
}
