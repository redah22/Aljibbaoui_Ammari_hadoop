import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.time.Instant;
import java.util.TreeMap;

/**
 * Trouve le top 5 des clients les plus rentables.
 * Datamart 1, Requête 3.
 * Utilise un seul Reducer pour avoir un classement global.
 */
public class Top5ProfitableCustomers {

    private static final String INPUT_PATH = "input-groupBy/superstore.csv";
    private static final String OUTPUT_PATH = "output/Top5Customers-";

    public static class CustomerProfitMapper extends Mapper<Object, Text, Text, DoubleWritable> {
        private static final int CUSTOMER_ID_INDEX = 5;
        private static final int PROFIT_INDEX = 20;

        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String[] columns = value.toString().split("[,;]");
            if (columns.length > PROFIT_INDEX && !columns[0].equals("Row ID")) {
                try {
                    String customerId = columns[CUSTOMER_ID_INDEX];
                    double profit = Double.parseDouble(columns[PROFIT_INDEX]);
                    context.write(new Text(customerId), new DoubleWritable(profit));
                } catch (NumberFormatException e) {
                    // Ignoré
                }
            }
        }
    }

    public static class CustomerProfitReducer extends Reducer<Text, DoubleWritable, Text, DoubleWritable> {
        public void reduce(Text key, Iterable<DoubleWritable> values, Context context) throws IOException, InterruptedException {
            double sum = 0;
            for (DoubleWritable val : values) {
                sum += val.get();
            }
            context.write(key, new DoubleWritable(sum));
        }
    }

    public static class Top5Reducer extends Reducer<Text, DoubleWritable, DoubleWritable, Text> {
        private final TreeMap<Double, String> topCustomers = new TreeMap<>();
        private final int K = 5;

        public void reduce(Text key, Iterable<DoubleWritable> values, Context context) {
            double totalProfit = 0;
            for (DoubleWritable val : values) {
                totalProfit += val.get();
            }

            topCustomers.put(totalProfit, key.toString());

            // Si la map dépasse K éléments, on retire le plus petit
            if (topCustomers.size() > K) {
                topCustomers.remove(topCustomers.firstKey());
            }
        }

        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {
            // Écrit le top K final
            for (java.util.Map.Entry<Double, String> entry : topCustomers.descendingMap().entrySet()) {
                context.write(new DoubleWritable(entry.getKey()), new Text(entry.getValue()));
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "Top 5 Profitable Customers");

        job.setJarByClass(Top5ProfitableCustomers.class);
        job.setMapperClass(CustomerProfitMapper.class);
        job.setCombinerClass(CustomerProfitReducer.class); // Premier niveau d'agrégation
        job.setReducerClass(Top5Reducer.class); // Deuxième Reducer pour le Top-K

        job.setNumReduceTasks(1); // Important: un seul reducer pour le classement final

        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(DoubleWritable.class);
        job.setOutputKeyClass(DoubleWritable.class);
        job.setOutputValueClass(Text.class);

        FileInputFormat.addInputPath(job, new Path(INPUT_PATH));
        FileOutputFormat.setOutputPath(job, new Path(OUTPUT_PATH + Instant.now().getEpochSecond()));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}