import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable; // Nécessaire pour les parties commentées
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

public class GroupBy {
    private static final String INPUT_PATH = "input-groupBy/";
    private static final String OUTPUT_PATH = "output/groupBy-";
    private static final Logger LOG = Logger.getLogger(GroupBy.class.getName());

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%5$s%n%6$s");
        try {
            FileHandler fh = new FileHandler("out.log");
            fh.setFormatter(new SimpleFormatter());
            LOG.addHandler(fh);
        } catch (SecurityException | IOException e) {
            System.exit(1);
        }
    }

    // =========================================================
    //  EXERCICE : TOTAL DES ACHATS PAR CLIENT
    // =========================================================

    public static class MapCustomerSales extends Mapper<LongWritable, Text, Text, DoubleWritable> {

        // Index des colonnes (6=Customer Name, 17=Sales)
        private int colCustomerName = 6;
        private int colSales = 17;

        private Text customerName = new Text();
        private DoubleWritable sales = new DoubleWritable();

        @Override
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();

            // Ignorer les lignes vides
            if (line.isEmpty()) return;

            // Split sur virgule (ou point-virgule au cas où)
            // Note: Pour un vrai CSV avec virgules dans les champs, il faudrait un parser plus robuste (ex: OpenCSV)
            // Ici on garde la logique simple du TP.
            String[] cols = line.split("[,;]");

            if (cols.length > colSales) {
                try {
                    // Nettoyage
                    String name = cols[colCustomerName].trim().replaceAll("\"", "");
                    String salesStr = cols[colSales].trim().replaceAll("\"", "");

                    // Tentative de conversion en nombre
                    double salesValue = Double.parseDouble(salesStr);

                    customerName.set(name);
                    sales.set(salesValue);

                    context.write(customerName, sales);

                } catch (NumberFormatException e) {
                    // Ignore la ligne d'en-tête ou lignes mal formées
                } catch (Exception e) {
                    System.err.println("Erreur ligne : " + line);
                }
            }
        }
    }

    public static class ReduceCustomerSales extends Reducer<Text, DoubleWritable, Text, DoubleWritable> {

        @Override
        public void reduce(Text key, Iterable<DoubleWritable> values, Context context)
                throws IOException, InterruptedException {

            double totalSales = 0.0;

            for (DoubleWritable val : values) {
                totalSales += val.get();
            }

            context.write(key, new DoubleWritable(totalSales));
        }
    }

    //  MAIN

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = new Job(conf, "GroupBy - Total Sales per Customer");

        job.setJarByClass(GroupBy.class); // Important pour le cluster

        job.setMapperClass(MapCustomerSales.class);
        job.setReducerClass(ReduceCustomerSales.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(DoubleWritable.class);

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        FileInputFormat.addInputPath(job, new Path(INPUT_PATH));
        FileOutputFormat.setOutputPath(job, new Path(OUTPUT_PATH + Instant.now().getEpochSecond()));

        job.waitForCompletion(true);
    }
}