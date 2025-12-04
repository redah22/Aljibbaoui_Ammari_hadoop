import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Compte le nombre de clients uniques pour chaque région.
 * Datamart 1, Requête 2.
 */
public class UniqueCustomersByRegion {

    private static final String INPUT_PATH = "input-groupBy/superstore.csv";
    private static final String OUTPUT_PATH = "output/UniqueCustomersByRegion-";

    public static class RegionMapper extends Mapper<Object, Text, Text, Text> {
        // Indices des colonnes
        private static final int CUSTOMER_ID_INDEX = 5;
        private static final int REGION_INDEX = 12;

        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String[] columns = value.toString().split("[,;]");
            if (columns.length > REGION_INDEX && !columns[0].equals("Row ID")) {
                String region = columns[REGION_INDEX];
                String customerId = columns[CUSTOMER_ID_INDEX];
                context.write(new Text(region), new Text(customerId));
            }
        }
    }

    public static class CountReducer extends Reducer<Text, Text, Text, IntWritable> {
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            Set<String> uniqueCustomers = new HashSet<>();
            for (Text val : values) {
                uniqueCustomers.add(val.toString());
            }
            context.write(key, new IntWritable(uniqueCustomers.size()));
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "Unique Customers by Region");

        job.setJarByClass(UniqueCustomersByRegion.class);
        job.setMapperClass(RegionMapper.class);
        // Pas de Combiner ici car on a besoin de tous les ID pour dé-dupliquer
        job.setReducerClass(CountReducer.class);

        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        FileInputFormat.addInputPath(job, new Path(INPUT_PATH));
        FileOutputFormat.setOutputPath(job, new Path(OUTPUT_PATH + Instant.now().getEpochSecond()));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}