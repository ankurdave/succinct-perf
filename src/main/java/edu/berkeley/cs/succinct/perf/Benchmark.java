package edu.berkeley.cs.succinct.perf;

import edu.berkeley.cs.succinct.StorageMode;
import edu.berkeley.cs.succinct.perf.buffers.SuccinctBufferBench;
import edu.berkeley.cs.succinct.perf.buffers.SuccinctFileBufferBench;
import edu.berkeley.cs.succinct.perf.streams.SuccinctFileStreamBench;
import edu.berkeley.cs.succinct.perf.streams.SuccinctStreamBench;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;

public class Benchmark {

    // Main class
    public static void main(String[] args) {
        CommandLineParser parser = new DefaultParser();

        Options options = new Options();
        options.addOption("b", true, "The benchmark to run; the specification format is <class-name>.<method-name>." +
            " If only class name is specified, then all benchmarks for that class name will be run." +
            " To run all benchmarks for all classes, specify \"all\"");
        options.addOption("r", true, "Path where the results will be stored.");
        options.addOption("q", true, "Path to query file that contains query strings" +
            " (Required for search/count benchmarks).");
        options.addOption("s", true, "Storage mode for SuccinctBuffer benchmarks."
                + " Can be MEMORY_ONLY or MEMORY_MAPPED.");
        options.addOption("d", true, "Path to serialized Succinct data. (REQUIRED)");

        HelpFormatter formatter = new HelpFormatter();

        try {
            // Parse the command line options
            CommandLine line = parser.parse(options, args);

            String resPath = line.getOptionValue("r");
            String queryFile = line.getOptionValue("q");
            String storageModeString = line.getOptionValue("s");
            StorageMode storageMode = (storageModeString == null || storageModeString == "MEMORY_ONLY") ?
                    StorageMode.MEMORY_ONLY : StorageMode.MEMORY_MAPPED;
            String benchType = line.getOptionValue("b");
            String dataPath = line.getOptionValue("d");

            if(dataPath == null) {
                System.out.println("Data path must be specified.");
                formatter.printHelp("succinct-perf", options);
                System.exit(0);
            }

            if(resPath == null) {
                System.out.println("Result path not specified; results will be stored in results/");
                resPath = "results/";
                File resDir = new File(resPath);
                if(!resDir.exists())
                    resDir.mkdir();
                resPath += "res";
            }

            if(benchType != null) {
                String[] benchParams = benchType.split("\\.");
                System.out.println("Benchmark Parameters (" + benchParams.length +  "): ");
                for(String param : benchParams) {
                    System.out.print(" \"" + param + "\" ");
                }
                System.out.println();
                if(benchParams.length == 1) {
                    if (benchParams[0] == "all") {
                        System.out.println("Benchmarking all classes and methods...");
                        new SuccinctBufferBench(dataPath, storageMode).benchAll(resPath);
                        new SuccinctFileBufferBench(dataPath, storageMode).benchAll(queryFile, resPath);
                        new SuccinctStreamBench(dataPath).benchAll(resPath);
                        new SuccinctFileStreamBench(dataPath).benchAll(queryFile, resPath);
                    } else if(benchParams[0] == "SuccinctBuffer") {
                        System.out.println("Benchmarking all methods for SuccinctBuffer...");
                        new SuccinctBufferBench(dataPath, storageMode).benchAll(resPath);
                    } else if(benchParams[0] == "SuccinctFileBuffer") {
                        System.out.println("Benchmarking all methods for SuccinctFileBuffer...");
                        new SuccinctFileBufferBench(dataPath, storageMode).benchAll(queryFile, resPath);
                    } else if(benchParams[0] == "SuccinctStream") {
                        System.out.println("Benchmarking all methods for SuccinctStream...");
                        new SuccinctStreamBench(dataPath).benchAll(resPath);
                    } else if(benchParams[0] == "SuccinctFileStream") {
                        System.out.println("Benchmarking all methods for SuccinctFileStream...");
                        new SuccinctFileStreamBench(dataPath).benchAll(queryFile, resPath);
                    } else {
                        System.out.println("Invalid benchmark specification.");
                        System.out.println("Test class must be one of SuccinctBuffer, SuccinctFileBuffer, SuccinctStream, SuccinctFileStream or all");
                        formatter.printHelp("succinct-perf", options);
                        System.exit(0);
                    }
                } else if(benchParams.length == 2) {
                    if (benchParams[0] == "SuccinctBuffer") {
                        if(benchParams[1] == "lookupNPA") {
                            System.out.println("Benchmarking SuccinctBuffer.lookupNPA...");
                            new SuccinctBufferBench(dataPath, storageMode).benchLookupNPA(resPath);
                        } else if(benchParams[1] == "lookupSA") {
                            System.out.println("Benchmarking SuccinctBuffer.lookupSA...");
                            new SuccinctBufferBench(dataPath, storageMode).benchLookupSA(resPath);
                        } else if(benchParams[2] == "lookupISA") {
                            System.out.println("Benchmarking SuccinctBuffer.lookupISA...");
                            new SuccinctBufferBench(dataPath, storageMode).benchLookupISA(resPath);
                        } else {
                            System.out.println("Invalid benchmark specification.");
                            formatter.printHelp("succinct-perf", options);
                            System.exit(0);
                        }
                    } else if(benchParams[0] == "SuccinctFileBuffer") {
                        if(benchParams[1] == "count") {
                            System.out.println("Benchmarking SuccinctFileBuffer.count...");
                            new SuccinctFileBufferBench(dataPath, storageMode).benchCount(queryFile, resPath);
                        } else if(benchParams[1] == "search") {
                            System.out.println("Benchmarking SuccinctFileBuffer.search...");
                            new SuccinctFileBufferBench(dataPath, storageMode).benchSearch(queryFile, resPath);
                        } else if(benchParams[1] == "extract") {
                            System.out.println("Benchmarking SuccinctFileBuffer.extract...");
                            new SuccinctFileBufferBench(dataPath, storageMode).benchExtract(resPath);
                        } else {
                            System.out.println("Invalid benchmark specification.");
                            formatter.printHelp("succinct-perf", options);
                            System.exit(0);
                        }
                    } else if(benchParams[0] == "SuccinctStream") {
                        if(benchParams[1] == "lookupNPA") {
                            System.out.println("Benchmarking SuccinctStream.lookupNPA...");
                            new SuccinctStreamBench(dataPath).benchLookupNPA(resPath);
                        } else if(benchParams[1] == "lookupSA") {
                            System.out.println("Benchmarking SuccinctStream.lookupSA...");
                            new SuccinctStreamBench(dataPath).benchLookupSA(resPath);
                        } else if(benchParams[2] == "lookupISA") {
                            System.out.println("Benchmarking SuccinctStream.lookupISA...");
                            new SuccinctStreamBench(dataPath).benchLookupISA(resPath);
                        } else {
                            System.out.println("Invalid benchmark specification.");
                            formatter.printHelp("succinct-perf", options);
                            System.exit(0);
                        }
                    } else if(benchParams[0] == "SuccinctFileStream") {
                        if(benchParams[1] == "count") {
                            System.out.println("Benchmarking SuccinctFileStream.count...");
                            new SuccinctFileStreamBench(dataPath).benchCount(queryFile, resPath);
                        } else if(benchParams[1] == "search") {
                            System.out.println("Benchmarking SuccinctFileStream.search...");
                            new SuccinctFileStreamBench(dataPath).benchSearch(queryFile, resPath);
                        } else if(benchParams[1] == "extract") {
                            System.out.println("Benchmarking SuccinctFileStream.extract...");
                            new SuccinctFileStreamBench(dataPath).benchExtract(resPath);
                        } else {
                            System.out.println("Invalid benchmark specification.");
                            System.out.println("Test method must be one of count, search or extract");
                            formatter.printHelp("succinct-perf", options);
                            System.exit(0);
                        }
                    } else {
                        System.out.println("Invalid benchmark specification.");
                        System.out.println("Test class must be one of SuccinctBuffer, SuccinctFileBuffer, SuccinctStream or SuccinctFileStream");
                        formatter.printHelp("succinct-perf", options);
                        System.exit(0);
                    }
                } else {
                    System.out.println("Invalid benchmark specification; benchParams.length = " + benchParams.length);
                    formatter.printHelp("succinct-perf", options);
                    System.exit(0);
                }
            }
        } catch (ParseException exception) {
            System.out.println("Could not parse command line options: " + exception.getMessage());
            formatter.printHelp("succinct-perf", options);
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }
}
