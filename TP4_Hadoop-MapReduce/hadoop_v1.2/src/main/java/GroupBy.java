import java.io.IOException;
import java.time.Instant;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
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

    public static class Map extends Mapper<LongWritable, Text, Text, DoubleWritable> {


        private static final int COL_IDX_CUSTOMER_ID = 5;
        private static final int COL_IDX_PROFIT = 18;
        // -------------------------------

        private Text customerId = new Text();
        private DoubleWritable profit = new DoubleWritable();

        @Override
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();

            // Ignore la ligne si elle est vide
            if (line.isEmpty()) return;

            // 1. Découpage du CSV
            // Note: Si votre CSV utilise des points-virgules, remplacez "," par ";"
            String[] columns = line.split(",");

            // Vérification de sécurité pour éviter les "ArrayIndexOutOfBoundsException"
            if (columns.length > COL_IDX_PROFIT && columns.length > COL_IDX_CUSTOMER_ID) {
                try {
                    // 2. Extraction des données
                    String strId = columns[COL_IDX_CUSTOMER_ID].trim();
                    String strProfit = columns[COL_IDX_PROFIT].trim();

                    // Nettoyage éventuel (ex: enlever les guillemets si présents)
                    strId = strId.replaceAll("\"", "");

                    // Gestion des nombres (ex: convertir "100" en 100.0)
                    double valProfit = Double.parseDouble(strProfit);

                    // 3. Écriture (Clé = CustomerID, Valeur = Profit)
                    customerId.set(strId);
                    profit.set(valProfit);
                    context.write(customerId, profit);

                } catch (NumberFormatException e) {
                    // Ignore la ligne si le profit n'est pas un nombre (ex: ligne d'en-tête)
                }
            }
        }
    }

    public static class Reduce extends Reducer<Text, DoubleWritable, Text, DoubleWritable> {

        private DoubleWritable totalProfit = new DoubleWritable();

        @Override
        public void reduce(Text key, Iterable<DoubleWritable> values, Context context)
                throws IOException, InterruptedException {

            double sum = 0.0;

            // 1. Somme des profits pour ce client (clé)
            for (DoubleWritable val : values) {
                sum += val.get();
            }

            // 2. Écriture du résultat final
            totalProfit.set(sum);
            context.write(key, totalProfit);
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();

        Job job = new Job(conf, "GroupBy");

        // Définition des classes Map et Reduce
        job.setMapperClass(Map.class);
        job.setReducerClass(Reduce.class);

        // Définition des types de sortie (Clé = Text, Valeur = DoubleWritable)
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(DoubleWritable.class);

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        FileInputFormat.addInputPath(job, new Path(INPUT_PATH));
        FileOutputFormat.setOutputPath(job, new Path(OUTPUT_PATH + Instant.now().getEpochSecond()));

        job.waitForCompletion(true);
    }
}