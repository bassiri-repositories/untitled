import java.net.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.*;

public class MainClass {
    Set<String> inputs;
    Set<String> addresses;
    Set<String> retryRequests = ConcurrentHashMap.newKeySet();
    Map<String, String> inputData = new HashMap<>();
    public static void main(String[] args) throws IOException {
        MainClass mainClass = new MainClass(args);
        mainClass.retryInvokeService();
    }

    public MainClass(String[] args) throws IOException {

        for (String arg : args) {
            String[] splitArg = arg.split("=");
            inputData.put(splitArg[0].substring(1), splitArg[1]);
        }
        inputs = readFromFile(inputData.get("inputFile"));
        addresses = readFromFile(inputData.get("addressFile"));
    }

    public void retryInvokeService() {
        asyncInvokeServiceAndSave(inputs);
        while(!retryRequests.isEmpty()) {
            System.out.println("Executing retry:");
            asyncInvokeServiceAndSave(retryRequests);
        }
    }

    public void asyncInvokeServiceAndSave(Collection<String> inputsToProcess) {
        inputsToProcess.parallelStream().forEach(throwingConsumerWrapper(inputRequest -> {
            URL proxy = new URL(String.format("http://localhost:8080/api/data?input=%s", inputRequest));
            HttpURLConnection httpResponse = (HttpURLConnection) proxy.openConnection();
            if (httpResponse.getResponseCode() == 200) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(httpResponse.getInputStream()))) {
                    processAndSaveProxyResult(in, inputRequest);
                    retryRequests.remove(inputRequest);
                }
            }
            else if(httpResponse.getResponseCode() == 503)  {
                retryRequests.add(inputRequest);
                System.out.println(String.format("Request %s responded with a 503 status code. Adding it to the retry list", inputRequest));
            }
            else {
                System.exit(-1);
            }
        }));
    }

    private void processAndSaveProxyResult(BufferedReader in, String inputRequest) throws IOException {
        StringBuilder sb = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null)
            sb.append(inputLine);
        JSONObject obj = new JSONObject(sb.toString());
        writeResultToFile(String.format("%s %s", inputRequest, obj.getString("information")));
    }
    static <T> Consumer<T> throwingConsumerWrapper(
            ThrowingConsumer<T, Exception> throwingConsumer) {

        return i -> {
            try {
                throwingConsumer.accept(i);
            } catch (Exception ex) {
                System.out.println("Unrecoverable error. Exiting.");
                System.exit(-1);
            }
        };
    }
    static Set<String> readFromFile(String path) throws IOException {
        Set<String> result;
        try (Stream<String> lines = Files.lines(Paths.get(path))) {
            result = lines.collect(Collectors.toSet());
        }
        return result;
    }

    private void writeResultToFile(String result) throws IOException {
        FileWriter fw = new FileWriter(inputData.get("outputfile"), true);
        try(BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(result + "\n");
        }
    }
}
