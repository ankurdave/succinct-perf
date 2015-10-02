package edu.berkeley.cs.succinct.perf;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

public class BenchmarkUtils {
    public static long[] generateRandoms(int numQueries, int limit) {
        long[] randoms = new long[numQueries];
        Random rand = new Random();
        for(int i = 0; i < numQueries; i++) {
            randoms[i] = rand.nextInt(limit);
        }
        return randoms;
    }

    public static String[] readQueryFile(String queryFile, int numQueries) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(queryFile));
        String[] queries = new String[numQueries];
        int i = 0;
        String query;
        while((query = bufferedReader.readLine()) != null && i < numQueries) {
            queries[i] = query;
            i++;
        }
        if(i < numQueries) {
            System.err.println("[WARNING] Number of queries is less then " + numQueries);
        }
        bufferedReader.close();
        return queries;
    }

    public static Configuration getConf() {
        Configuration conf = new Configuration();
        String confDir = System.getenv("HADOOP_CONF_DIR");
        if(confDir != null) {
            conf.addResource(new Path(confDir + "/core-site.xml"));
            conf.addResource(new Path(confDir + "/hdfs-site.xml"));
        }
        return conf;
    }
}
