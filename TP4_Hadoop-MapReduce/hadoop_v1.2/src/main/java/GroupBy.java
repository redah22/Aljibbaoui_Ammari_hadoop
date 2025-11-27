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
    //  EXERCICE 3 : ANALYSE PAR COMMANDE (ACTIF)
    // =========================================================

    public static class MapExo3 extends Mapper<LongWritable, Text, Text, Text> {

        // Index des colonnes (1=Order ID, 13=Product ID, 18=Quantity)
        private int colOrder = 1;
        private int colProduct = 13;
        private int colQty = 18;

        private Text orderId = new Text();
        private Text infos = new Text();

        @Override
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();

            // Ignorer les lignes vides
            if (line.isEmpty()) return;

            // CORRECTION : Split sur virgule OU point-virgule pour éviter le fichier vide
            String[] cols = line.split("[,;]");

            if (cols.length > colQty) {
                try {
                    // Nettoyage (.trim et .replaceAll) pour virer les guillemets et espaces
                    String id = cols[colOrder].trim().replaceAll("\"", "");
                    String product = cols[colProduct].trim().replaceAll("\"", "");
                    String qtyStr = cols[colQty].trim().replaceAll("\"", "");

                    // On vérifie si c'est un nombre (exclut l'en-tête "Quantity")
                    int testNum = Integer.parseInt(qtyStr);

                    orderId.set(id);
                    // On combine ProductID et Quantité
                    infos.set(product + ";" + testNum);

                    context.write(orderId, infos);

                } catch (NumberFormatException e) {
                    // Ignore la ligne d'en-tête
                } catch (Exception e) {
                    System.err.println("Erreur ligne : " + line);
                }
            }
        }
    }

    public static class ReduceExo3 extends Reducer<Text, Text, Text, Text> {

        @Override
        public void reduce(Text key, Iterable<Text> values, Context context)
                throws IOException, InterruptedException {

            Set<String> produitsUniques = new HashSet<>();
            int totalExemplaires = 0;

            for (Text val : values) {
                String[] data = val.toString().split(";");

                if (data.length == 2) {
                    produitsUniques.add(data[0]); // Le Set gère les doublons tout seul
                    totalExemplaires += Integer.parseInt(data[1]);
                }
            }

            String res = " -> Nb Produits distincts: " + produitsUniques.size() +
                    " | Total Exemplaires: " + totalExemplaires;

            context.write(key, new Text(res));
        }
    }


    //  ANCIENS EXOS (VENTES PAR DATE/STATE)

    /*
    public static class MapDateState extends Mapper<LongWritable, Text, Text, DoubleWritable> {
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();
            if (line.isEmpty()) return;

            String[] cols = line.split("[,;]"); // Split robuste

            // Index supposés : Date=2, State=10, Sales=17
            if (cols.length > 17) {
                try {
                    String date = cols[2].trim().replaceAll("\"", "");
                    String state = cols[10].trim().replaceAll("\"", "");
                    String salesStr = cols[17].trim().replaceAll("\"", "").replace(",", "."); // Gestion décimale

                    String maCle = date + " - " + state;
                    double profit = Double.parseDouble(salesStr);

                    context.write(new Text(maCle), new DoubleWritable(profit));
                } catch (Exception e) {}
            }
        }
    }

    public static class ReduceSum extends Reducer<Text, DoubleWritable, Text, DoubleWritable> {
        public void reduce(Text key, Iterable<DoubleWritable> values, Context context)
                throws IOException, InterruptedException {
            double somme = 0;
            for (DoubleWritable val : values) {
                somme += val.get();
            }
            context.write(key, new DoubleWritable(somme));
        }
    }
    */


    //  MAIN

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = new Job(conf, "GroupBy - Exo 3");

        //CONFIG ACTIVE (EXO 3)
        job.setMapperClass(MapExo3.class);
        job.setReducerClass(ReduceExo3.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class); // Sortie finale = Texte

        // ONFIG ANCIENNE
        /*
        job.setMapperClass(MapDateState.class);
        job.setReducerClass(ReduceSum.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(DoubleWritable.class); // Sortie finale = Nombre
        */

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        FileInputFormat.addInputPath(job, new Path(INPUT_PATH));
        FileOutputFormat.setOutputPath(job, new Path(OUTPUT_PATH + Instant.now().getEpochSecond()));

        job.waitForCompletion(true);
    }
}