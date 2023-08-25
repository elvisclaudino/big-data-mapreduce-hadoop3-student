package advanced.customwritable;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.log4j.BasicConfigurator;

import java.io.IOException;

public class AverageTemperature {

    public static void main(String args[]) throws IOException,
            ClassNotFoundException,
            InterruptedException {
        BasicConfigurator.configure();

        Configuration c = new Configuration();
        String[] files = new GenericOptionsParser(c, args).getRemainingArgs();
        // arquivo de entrada
        Path input = new Path("in/forestfireinput.csv");

        // arquivo de saida
    Path output = new Path("output/media.txt");

        // criacao do job e seu nome
        Job j = new Job(c, "media");

        // Registros de classes
        j.setJarByClass(AverageTemperature.class);
        j.setMapperClass(MapForAverage.class);
        j.setReducerClass(ReduceForAverage.class);

        // Tipos de saida
        j.setMapOutputKeyClass(Text.class);
        j.setMapOutputValueClass(FireAvgTempWritable.class);
        j.setOutputKeyClass(Text.class);
        j.setOutputValueClass(FloatWritable.class);

        // Definição de arquivos de entrada e saída
        FileInputFormat.addInputPath(j, input);
        FileOutputFormat.setOutputPath(j, output);

        // rodar :)
        j.waitForCompletion(false);

    }


    public static class MapForAverage extends Mapper<LongWritable, Text, Text, FireAvgTempWritable> {
        public void map(LongWritable key, Text value, Context con)
                throws IOException, InterruptedException {
            String linha = value.toString();
            float temperatura = Float.parseFloat(linha.split(",")[8]);
            int n = 1;

            String mes = linha.split(",")[2];

            // Gerar (chave = 'key', valor = (soma=temperatura, n=1))
            con.write(new Text(mes), new FireAvgTempWritable(temperatura, n));
            con.write(new Text("key"), new FireAvgTempWritable(temperatura, n));

        }
    }

    public static class CombineForAverage extends Reducer<Text, FireAvgTempWritable, Text, FireAvgTempWritable>{
        public void reduce(Text key, Iterable<FireAvgTempWritable> values, Context con)
                throws IOException, InterruptedException {
        }
    }


    public static class ReduceForAverage extends Reducer<Text, FireAvgTempWritable, Text, FloatWritable> {
        public void reduce(Text key, Iterable<FireAvgTempWritable> values, Context con)
                throws IOException, InterruptedException {
            // SOmando as somas e os Ns (quantidades)
            float somaTemperaturas = 0.0f;
            int somaNs = 0;
            for(FireAvgTempWritable o: values) {
                somaTemperaturas += o.getSoma();
                somaNs += o.getN();
            }

            // Calculando a média
            float media = somaTemperaturas / somaNs;

            // salvando o resultado
            con.write(key, new FloatWritable(media));
        }
    }

}
