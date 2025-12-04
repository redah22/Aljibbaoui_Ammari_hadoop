import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Job MapReduce pour une jointure de type "Reduce-Side Join".
 * Joint les clients et leurs commandes par ID client pour produire des paires (nom_client, commentaire_commande).
 */
public class Join {

    private static final String CUST_TAG = "CUST#";
    private static final String ORDER_TAG = "ORDER#";
    private static final String CUSTOMERS_INPUT_PATH = "input-join/customers.tbl";
    private static final String ORDERS_INPUT_PATH = "input-join/orders.tbl";
    private static final String OUTPUT_PATH = "output/Join-";

    /**
     * Mapper pour le fichier des clients (customers.tbl).
     */
    public static class CustomerMapper extends Mapper<Object, Text, Text, Text> {
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();
            String[] parts = line.split("\\|", -1);

            if (parts.length >= 2) {
                try {
                    Integer.parseInt(parts[0].trim());
                    String customerId = parts[0].trim();
                    String customerName = parts[1].trim();
                    context.write(new Text(customerId), new Text(CUST_TAG + customerName));
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    /**
     * Mapper pour le fichier des commandes (orders.tbl).
     * Correction importante : reconstruction complète du commentaire (colonne finale), même si le texte contient des '|'.
     */
    public static class OrderMapper extends Mapper<Object, Text, Text, Text> {
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {

            String line = value.toString();
            String[] parts = line.split("\\|", -1);

            // On doit avoir au moins 9 colonnes
            if (parts.length >= 9) {
                try {
                    Integer.parseInt(parts[0].trim()); // orderId
                    String customerId = parts[1].trim(); // customerId

                    // Reconstruction du commentaire : toutes les colonnes >= 8
                    StringBuilder commentBuilder = new StringBuilder();
                    for (int i = 8; i < parts.length; i++) {
                        if (i > 8) commentBuilder.append("|");
                        commentBuilder.append(parts[i]);
                    }

                    String orderComment = commentBuilder.toString().trim();

                    context.write(new Text(customerId), new Text(ORDER_TAG + orderComment));

                } catch (NumberFormatException ignored) {}
            }
        }
    }

    /**
     * Le Reducer effectue le produit cartésien entre les noms client et les commentaires.
     */
    public static class JoinReducer extends Reducer<Text, Text, Text, Text> {

        private final Text outKey = new Text();
        private final Text outValue = new Text();

        public void reduce(Text key, Iterable<Text> values, Context context)
                throws IOException, InterruptedException {

            List<String> customerNames = new ArrayList<>();
            List<String> orderComments = new ArrayList<>();

            for (Text val : values) {
                String valueStr = val.toString();
                if (valueStr.startsWith(CUST_TAG)) {
                    customerNames.add(valueStr.substring(CUST_TAG.length()));
                } else if (valueStr.startsWith(ORDER_TAG)) {
                    orderComments.add(valueStr.substring(ORDER_TAG.length()));
                }
            }

            // Jointure
            if (!customerNames.isEmpty() && !orderComments.isEmpty()) {
                for (String name : customerNames) {
                    for (String comment : orderComments) {
                        outKey.set(name);
                        outValue.set(comment);
                        context.write(outKey, outValue);
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "Join Customers and Orders");
        job.setJarByClass(Join.class);

        MultipleInputs.addInputPath(job, new Path(CUSTOMERS_INPUT_PATH),
                TextInputFormat.class, CustomerMapper.class);

        MultipleInputs.addInputPath(job, new Path(ORDERS_INPUT_PATH),
                TextInputFormat.class, OrderMapper.class);

        job.setReducerClass(JoinReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        FileOutputFormat.setOutputPath(
                job,
                new Path(OUTPUT_PATH + Instant.now().getEpochSecond())
        );

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
