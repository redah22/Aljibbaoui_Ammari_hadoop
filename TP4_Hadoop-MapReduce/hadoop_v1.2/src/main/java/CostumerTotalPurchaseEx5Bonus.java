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
 * Exercice 5 - GroupBy + Join
 * Pour le fichier superstore.csv, calculer le montant total des achats faits par chaque client.
 * Restitue des couples (CUSTOMERS.name, SUM(totalprice)).
 *
 * Interprétation de "Join" pour un seul fichier: toutes les informations nécessaires sont dans
 * superstore.csv. Le "join" est conceptuel, reliant les données client et vente dans chaque enregistrement.
 */
public class CostumerTotalPurchaseEx5Bonus {

    private static final String INPUT_PATH = "input-groupBy/superstore.csv";
    private static final String OUTPUT_PATH = "output/CostumerTotalPurchaseEx5Bonus-";

    // Mapper pour superstore.csv
    public static class CustomerSalesMapper extends Mapper<Object, Text, Text, DoubleWritable> {
        // Index 6 correspond généralement au Nom du Client (Customer Name) dans Superstore.
        private static final int CUSTOMER_NAME_INDEX = 6;
        // Index 17 correspond généralement aux Ventes (Sales) dans Superstore.
        private static final int SALES_INDEX = 17;

        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String[] columns = value.toString().split("[,;]");
            // Vérifie l'en-tête et la longueur suffisante pour lire le nom et les ventes
            if (columns.length > SALES_INDEX && !columns[0].equals("Row ID")) {
                try {
                    String customerName = columns[CUSTOMER_NAME_INDEX];
                    // Nettoyage potentiel du montant (ex: suppression de symboles monétaires)
                    String salesStr = columns[SALES_INDEX].replaceAll("[^\\d.-]", "");
                    
                    if (!salesStr.isEmpty()) {
                        double sales = Double.parseDouble(salesStr);
                        context.write(new Text(customerName), new DoubleWritable(sales));
                    }
                } catch (Exception e) {
                    // Ignorer les lignes malformées
                }
            }
        }
    }

    // Reducer pour sommer les ventes par client
    public static class SumSalesReducer extends Reducer<Text, DoubleWritable, Text, DoubleWritable> {
        public void reduce(Text key, Iterable<DoubleWritable> values, Context context) throws IOException, InterruptedException {
            double totalSales = 0.0;
            for (DoubleWritable val : values) {
                totalSales += val.get();
            }

            // Arrondi à 2 décimales pour un affichage propre
            double roundedSum = Math.round(totalSales * 100.0) / 100.0;
            context.write(key, new DoubleWritable(roundedSum));
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "Costumer Total Purchase (GroupBy)");

        job.setJarByClass(CostumerTotalPurchaseEx5Bonus.class);

        job.setMapperClass(CustomerSalesMapper.class);
        job.setCombinerClass(SumSalesReducer.class); // Optimisation: pré-agrégation côté Map
        job.setReducerClass(SumSalesReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(DoubleWritable.class);

        FileInputFormat.addInputPath(job, new Path(INPUT_PATH));
        FileOutputFormat.setOutputPath(job, new Path(OUTPUT_PATH + Instant.now().getEpochSecond()));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}