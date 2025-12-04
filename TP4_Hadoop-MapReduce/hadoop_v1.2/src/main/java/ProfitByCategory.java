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

/**
 * Calcule le profit total pour chaque catégorie de produits.
 * Datamart 1, Requête 1.
 */
public class ProfitByCategory {

    private static final String INPUT_PATH = "input-groupBy/superstore.csv";
    private static final String OUTPUT_PATH = "output/ProfitByCategory-";

    public static class ProfitMapper extends Mapper<Object, Text, Text, DoubleWritable> {
        // Indices des colonnes dans superstore.csv
        private static final int CATEGORY_INDEX = 14;
        private static final int PROFIT_INDEX = 20;

        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String[] columns = value.toString().split("[,;]");
            // Ignore l'en-tête et les lignes mal formées
            if (columns.length > PROFIT_INDEX && !columns[0].equals("Row ID")) {
                try {
                    String category = columns[CATEGORY_INDEX];
                    double profit = Double.parseDouble(columns[PROFIT_INDEX]);
                    context.write(new Text(category), new DoubleWritable(profit));
                } catch (NumberFormatException e) {
                    // Ignore les lignes où le profit n'est pas un nombre valide
                }
            }
        }
    }

    public static class ProfitReducer extends Reducer<Text, DoubleWritable, Text, DoubleWritable> {
        public void reduce(Text key, Iterable<DoubleWritable> values, Context context) throws IOException, InterruptedException {
            double sum = 0;
            for (DoubleWritable val : values) {
                sum += val.get();
            }
            // Arrondir à 2 décimales pour un affichage propre
            double roundedSum = Math.round(sum * 100.0) / 100.0;
            context.write(key, new DoubleWritable(roundedSum));
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "Profit by Category");

        job.setJarByClass(ProfitByCategory.class);
        job.setMapperClass(ProfitMapper.class);
        job.setCombinerClass(ProfitReducer.class); // Optimisation: pré-agrégation côté Map
        job.setReducerClass(ProfitReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(DoubleWritable.class);

        FileInputFormat.addInputPath(job, new Path(INPUT_PATH));
        FileOutputFormat.setOutputPath(job, new Path(OUTPUT_PATH + Instant.now().getEpochSecond()));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}