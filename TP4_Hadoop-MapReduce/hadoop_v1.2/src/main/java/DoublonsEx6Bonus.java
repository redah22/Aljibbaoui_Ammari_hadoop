import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.time.Instant;

public class DoublonsEx6Bonus {

    // Chemins demandés par l'exercice
    private static final String INPUT_PATH = "input-groupBy/superstore.csv";
    private static final String OUTPUT_PATH = "output/exo6-distinctClients-";

    public static class DistinctCustomerMapper extends Mapper<Object, Text, Text, NullWritable> {
        private Text customerName = new Text();

        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();

            // o,n ignore l'en-tête du fichier CSV
            if (line.startsWith("Row ID")) return;

            // découpage  (gère les virgules dans les guillemets)
            String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

            // extraction du nom (Index 6)
            if (parts.length > 6) {
                String name = parts[6].trim().replace("\"", ""); // On nettoie les guillemets autour du nom
                customerName.set(name);

                // On envoie (Nom, Null). La valeur ne nous intéresse pas pour un DISTINCT.
                context.write(customerName, NullWritable.get());
            }
        }
    }

    public static class DistinctCustomerReducer extends Reducer<Text, NullWritable, Text, NullWritable> {
        public void reduce(Text key, Iterable<NullWritable> values, Context context) throws IOException, InterruptedException {
            // Le framework a déjà supprimé les doublons en regroupant par Clé.
            // On écrit simplement la clé.
            context.write(key, NullWritable.get());
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "Exo 6 - Distinct Customers");

        job.setJarByClass(DoublonsEx6Bonus.class);
        job.setMapperClass(DistinctCustomerMapper.class);
        job.setCombinerClass(DistinctCustomerReducer.class); // Optimisation réseau
        job.setReducerClass(DistinctCustomerReducer.class);

        // Sortie : Clé = Texte (Nom), Valeur = Rien (Null)
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(NullWritable.class);

        // Configuration des fichiers
        Path inputPath = new Path(INPUT_PATH);
        Path outputPath = new Path(OUTPUT_PATH + Instant.now().getEpochSecond());

        FileInputFormat.addInputPath(job, inputPath);
        FileOutputFormat.setOutputPath(job, outputPath);

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}