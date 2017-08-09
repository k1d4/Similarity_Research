import java.util.*;
import java.io.*;
import java.nio.*;
import java.security.*;

class FilterData
{
	BitSet filter;
	String filename;

	FilterData(BitSet x, String y)
	{
		filter = x;
		filename = y;
	}
}

class ASM_Similarity
{
	public static void main(String [] args)
	{
		// Get command line args
		File samplesFolder = new File(args[0]);
		int gramSize = Integer.parseInt(args[1]);

		// Create array of files in folder
		ArrayList<File> fileList = new ArrayList<File>(Arrays.asList(samplesFolder.listFiles()));

		// Create array of Bloom filters
		ArrayList<FilterData> filterList = new ArrayList<FilterData>();

		// Create a new process to run objdump
		Process p;

		// Create new asm files
		for(File i : fileList)
		{
			try 
			{
				// Outfile name
				File outFile = new File(i.getName() + ".TEMP");

				// Call objdump to get asm files
				ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", "objdump " + "-d " + "--no-show-raw-insn " + i + " | perl -p -e 's/^\\s+(\\S+):\\t//;'");
				builder.redirectOutput(outFile);
				builder.redirectError(new File("error.txt"));

				// Start process
				p = builder.start();

				// Wait until thread has terminated
	        	p.waitFor();

	        	// Hash n-grams into object with Bloom filter and original file name
	        	FilterData filter = new FilterData(gramHash(outFile.getName(), gramSize), i.getName());

	        	// Add object to the filterlist
	        	filterList.add(filter);

	        	// File is no longer needed
	        	outFile.delete();

			}
			
			// Print exception if unable to objdump
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}

		try
		{
			// Create output csv for similarity index
			PrintWriter indexOut = new PrintWriter("SimilarityIndex.csv", "UTF-8");

			// Strings to write out to csv
			StringBuilder sb = new StringBuilder();

			// Print out header
			for(FilterData i : filterList)
			{
				sb.append("," + i.filename);
			}

			// Print out line
			indexOut.println(sb);

			// Clear sb
			sb.setLength(0);

			// Create array of files already done
			ArrayList<FilterData> doneList = new ArrayList<FilterData>();

			// Do comparisons of the normalized files, output to CSV
			for(FilterData i : filterList)
			{
				// Append file name to line
				sb.append(i.filename + ",");

				doneList.add(i);

				for(FilterData j : filterList)
				{

					if(doneList.contains(j))
					{
						sb.append(",");
					}

					else
					{
						// Generate percentage similarity
						double percentage = compareHash(i.filter, j.filter);

						// Append to line
						sb.append(percentage + ",");

						System.out.println("Similarity of " + i.filename + " and " + j.filename + " : " + percentage + "%");
					}
				}

				// Print to csv
				indexOut.println(sb);

				// Clear sb
				sb.setLength(0);
			}

			// Close out file
			indexOut.close();
		}

		// Exception occured
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	// Method for comparing two hashes
	public static double compareHash(BitSet hash1, BitSet hash2)
	{
		// The two variables needed for Jaccard Index
		double union = 0;
		double intersection = 0;

		// Do a comparison of each value in the hash table
		for(int i = 0; i < hash1.size(); i++)
		{
			if(hash1.get(i) == true || hash2.get(i) == true)
			{
				if(hash1.get(i) == hash2.get(i))
				{
					intersection++;
				}

				union++;
			}
		}

		// Return the percentage
		double percentage = intersection / union * 100.0;
		return percentage;
	}

	// Get the Bloom filter of a file
	public static BitSet gramHash(String fileName, int gramSize)
	{
		// Input file
		FileReader asmInput;
		Scanner asmScan = null;

		// Initialize input
		try 
		{
			asmInput = new FileReader(fileName);
			asmScan = new Scanner(new BufferedReader(asmInput));
		}

		// Some IO exception occurred, close files
		catch(Exception e)
		{
			System.out.println(e);
			System.exit(-1);
		}

		// Array to contain the strings as we scan
		ArrayList<String> lines = new ArrayList<String>();

		// Add all of the strings in the doc to an array
		while(asmScan.hasNext()) 
		{
			// Get next line
			String nextLine = asmScan.nextLine();

			// Add the line to the array
			lines.add(nextLine);
		}

		// This will contain the n-grams from the doc
		ArrayList<String> nGrams = new ArrayList<String>();

		// Create and store the n-gram strings
		for(int i = 0; i < lines.size(); i++)
		{
			// Initialize n-gram as empty string
			String nGram = "";

			for(int k = 0; k < gramSize; k++)
			{
				if(i + gramSize < lines.size())
				{
					// Concatenate the number of lines in the n-gram
					nGram += lines.get(i + k);
				}
			}

			nGrams.add(nGram);
		}

		// Initialize a binary array, size of 2^28
		BitSet hashArray = new BitSet((int) Math.pow(2, 28));

		// Store to byte array
		byte[] bytesOfMessage;
		MessageDigest md;
		byte[] thedigest = null;

		for(String i : nGrams)
		{
			try
			{
				bytesOfMessage = i.getBytes("UTF-8");
				md = MessageDigest.getInstance("MD5");
				thedigest = md.digest(bytesOfMessage);

			}

			catch(Exception e)
			{
				System.out.println(e);
				System.exit(-1);
			}

			byte [] trunc = Arrays.copyOfRange(thedigest, 11, 15);

			int index = ByteBuffer.wrap(trunc).getInt();

			index = index & 0x0FFFFFFF;

			hashArray.set(index);
		}

		return hashArray;
	}
}