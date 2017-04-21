package net.goldolphin.maria.api.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import net.goldolphin.maria.api.ApiHandler;
import net.goldolphin.maria.api.ApiServerCodec;
import net.goldolphin.maria.common.ExceptionUtils;

/**
 * Created by caofuxiang on 2017/4/21.
 */
public class CliEvaluator {
    private final ApiHandler<Object, Object> handler;
    private final ApiServerCodec<Object, Object, String[], String> codec;
    private final CliCommandHandler commandHandler;
    private final String description;

    public <REQUEST, RESPONSE> CliEvaluator(ApiHandler<REQUEST, RESPONSE> handler,
            ApiServerCodec<REQUEST, RESPONSE, String[], String> codec,
            CliCommandHandler commandHandler, String description) {
        this.handler = (ApiHandler<Object, Object>) handler;
        this.codec = (ApiServerCodec<Object, Object, String[], String>) codec;
        this.commandHandler = commandHandler;
        this.description = description;
    }

    public void evaluate(Reader input, Writer output, Writer error) throws IOException {
        welcome(error);
        BufferedReader reader = input instanceof BufferedReader ? (BufferedReader) input : new BufferedReader(input);
        while (true) {
            error.write(">>> ");
            output.flush();
            error.flush();
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            try {
                if (!process(line, output, error)) {
                    break;
                }
            } catch (Throwable e) {
                error.write(ExceptionUtils.getRootCause(e) + "\n");
            }
        }
    }

    private boolean process(String line, Writer output, Writer error) throws IOException {
        String[] splits = line.split("\\s+", 2);
        if (splits.length == 0 || splits[0].length() == 0) {
            return true;
        }
        if (splits[0].equals("/help")) {
            if (splits.length == 1) {
                help(error);
            } else {
                help(error, splits[1]);
            }
        } else if (splits[0].equals("/quit")) {
            return false;
        } else if (splits[0].startsWith("/")) {
            error.write("Unsupported command: " + splits[0] + "\n");
        } else if (commandHandler.help(splits[0]) == null) {
            error.write("Unsupported method: " + splits[0] + "\n");
        } else {
            Object request = codec.decodeRequest(splits);
            Object response = handler.call(request);
            output.write(codec.encodeResponse(response) + "\n");
        }
        return true;
    }

    private void welcome(Writer error) throws IOException {
        error.write(description + "\nType /help for more information.\nType /quit to quit.\n");
    }

    private void help(Writer error) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("Type \"method\" \"args\" to invoke specified method. Supported methods:\n");
        for (String method: commandHandler.list()) {
            builder.append(method).append("\n");
        }
        builder.append("\nType /help \"method\" for help for methods.\nType /quit to quit.\n");
        error.write(builder.toString());
    }

    private void help(Writer error, String method) throws IOException {
        String info = commandHandler.help(method);
        if (info == null) {
            error.write("Help on " + method + " not found.\n");
        } else {
            error.write("Help on " + method + ":\n\n" + commandHandler.help(method) + "\n");
        }
    }
}
