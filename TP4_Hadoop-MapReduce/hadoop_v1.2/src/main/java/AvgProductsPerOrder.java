import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.time.Instant;

/**
 * Calcule le nombre moyen de produits par commande.
 * Datamart 2, Requête 1.
 * Utilise un seul Reducer pour calculer la moyenne globale.
 */
public class AvgProductsPerOrder {

    private static final String INPUT_PATH = "input-groupBy/superstore.csv";
    private static final String OUTPUT_PATH = "output/AvgProductsPerOrder-";

    public static class OrderMapper extends Mapper<Object, Text, Text, IntWritable> {
        private static final int ORDER_ID_INDEX = 1;
        private static final IntWritable ONE = new IntWritable(1);

        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String[] columns = value.toString().split("[,;]");
            if (columns.length > ORDER_ID_INDEX && !columns[0].equals("Row ID")) {
                String orderId = columns[ORDER_ID_INDEX];
                context.write(new Text(orderId), ONE);
            }
        }
    }

    public static class AvgReducer extends Reducer<Text, IntWritable, Text, DoubleWritable> {
        private long totalProducts = 0;
        private long orderCount = 0;

        public void reduce(Text key, Iterable<IntWritable> values, Context context) {
            orderCount++;
            int productsInOrder = 0;
            for (IntWritable val : values) {
                productsInOrder += val.get();
            }
            totalProducts += productsInOrder;
        }

        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {
            double average = (double) totalProducts / orderCount;
            context.write(new Text("Nombre moyen de produits par commande"), new DoubleWritable(average));
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "Average Products per Order");

        job.setJarByClass(AvgProductsPerOrder.class);
        job.setMapperClass(OrderMapper.class);
        job.setCombinerClass(Reducer.class); // On peut sommer les produits par commande côté Map
        job.setReducerClass(AvgReducer.class);

        job.setNumReduceTasks(1); // Un seul reducer pour la moyenne globale

        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(IntWritable.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(DoubleWritable.class);

        FileInputFormat.addInputPath(job, new Path(INPUT_PATH));
        FileOutputFormat.setOutputPath(job, new Path(OUTPUT_PATH + Instant.now().getEpochSecond()));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}