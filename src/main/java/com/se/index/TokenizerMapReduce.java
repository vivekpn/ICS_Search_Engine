package com.se.index;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import com.google.gson.Gson;
import com.se.data.Posting;
import com.se.data.WordEntry;
import com.se.db.DatabaseUtil;

public class TokenizerMapReduce {
	public static class TokenizerMapper extends
			Mapper<Object, Text, Text, Text> {

		private static final String PATH = "Project/webpages_raw/";

		public void map(Object key, Text value, Context context)
				throws IOException, InterruptedException {
			StringTokenizer itr = new StringTokenizer(value.toString());
			if (itr.countTokens() < 2) {
				return;
			}
			String fileName = itr.nextToken();
			String url = itr.nextToken();
			String[] parts = fileName.split("/");
			int docID = 500 * Integer.valueOf(parts[0])
					+ Integer.valueOf(parts[1]);
			File file = new File(PATH + fileName);
			Map<String, Posting> postingsMap = Tokenizer.tokenize(file, docID);

			Gson gson = new Gson();
			for (Entry<String, Posting> entry : postingsMap.entrySet()) {
				context.write(new Text(entry.getKey()), new Text(gson.toJson(entry.getValue())));
			}
		}
	}

	public static class PostingsReducer extends
			Reducer<Text, Text, Text, Text> {

		public void reduce(Text term, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			Set<WordEntry> wordEntries = new HashSet<WordEntry>();
			WordEntry wordEntry = new WordEntry();
			wordEntry.setTerm(term.toString());
			List<Posting> postings = new ArrayList<Posting>();
			Gson gson = new Gson();
			for (Text value : values) {
				postings.add(gson.fromJson(value.toString(), Posting.class));
			}
			wordEntry.setPostings(postings);
			wordEntries.add(wordEntry);

			DatabaseUtil db = new DatabaseUtil();
			db.insert(wordEntries);
		}
	}

	public static void main(String[] args) throws IOException,
			ClassNotFoundException, InterruptedException {
		Configuration conf = new Configuration();
		String location = "/media/magic/Windows 7/Users/Vivek/Google Drive/Quarter 2/IR/Project/webpages_raw/bookkeeping.tsv";
		Job job = Job.getInstance(conf, "Postings Creator");
		job.setJarByClass(TokenizerMapReduce.class);
		job.setMapperClass(TokenizerMapper.class);
		job.setReducerClass(PostingsReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		FileInputFormat.addInputPath(job, new Path(location));
		FileOutputFormat.setOutputPath(job, new Path(
				"src/test/resources/output5"));
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}

}