import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

public class TestMainClass {
    @Test
    public void testMainClass() throws IOException {
        String inputFileArg = "-inputFile=src/main/resources/inputfile.txt";
        String addressFileArg = "-addressFile=src/main/resources/addressfile.txt";
        String outputFileArg = "-outputfile=src/main/resources/outputfile.txt";
        String[] args = {inputFileArg, addressFileArg, outputFileArg};
        MainClass.main(args);
        performAssertions();
    }

    private void performAssertions() throws IOException {
        Set<String> inputFileData = MainClass.readFromFile("src/main/resources/inputfile.txt");
        Set<String> outputFileData = MainClass.readFromFile("src/main/resources/outputfile.txt");
        Set<String> expectedOutputFileData = MainClass.readFromFile("src/main/resources/outputfile.txt");

        Set<String> outputRequestInfo = outputFileData.stream().map(s-> s.substring(0, s.indexOf(' ')))
                .collect(Collectors.toSet());
        Assert.assertEquals(inputFileData, outputRequestInfo);
        Assert.assertEquals(outputFileData, expectedOutputFileData);
    }
}
