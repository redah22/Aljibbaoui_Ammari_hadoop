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
 * Filtre les commandes qui contiennent plus de 3 produits différents.
 * Datamart 2, Requête 2.
 */
public class OrdersWithMultipleProducts {

    private static final String INPUT_PATH = "input-groupBy/superstore.csv";
    private static final String OUTPUT_PATH = "output/OrdersWithMultipleProducts-";
    private static final int PRODUCT_THRESHOLD = 3;

    public static class OrderProductMapper extends Mapper<Object, Text, Text, Text> {
        private static final int ORDER_ID_INDEX = 1;
        private static final int PRODUCT_ID_INDEX = 13;

        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String[] columns = value.toString().split("[,;]");
            if (columns.length > PRODUCT_ID_INDEX && !columns[0].equals("Row ID")) {
                String orderId = columns[ORDER_ID_INDEX];
                String productId = columns[PRODUCT_ID_INDEX];
                context.write(new Text(orderId), new Text(productId));
            }
        }
    }

    public static class FilterReducer extends Reducer<Text, Text, Text, IntWritable> {
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            Set<String> uniqueProducts = new HashSet<>();
            for (Text val : values) {
                uniqueProducts.add(val.toString());
            }

            if (uniqueProducts.size() > PRODUCT_THRESHOLD) {
                context.write(key, new IntWritable(uniqueProducts.size()));
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "Orders With More Than " + PRODUCT_THRESHOLD + " Products");

        job.setJarByClass(OrdersWithMultipleProducts.class);
        job.setMapperClass(OrderProductMapper.class);
        job.setReducerClass(FilterReducer.class);

        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        FileInputFormat.addInputPath(job, new Path(INPUT_PATH));
        FileOutputFormat.setOutputPath(job, new Path(OUTPUT_PATH + Instant.now().getEpochSecond()));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}