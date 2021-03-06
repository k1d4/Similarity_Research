import java.util.*;
import java.io.*;
import java.nio.*;
import java.security.*;

// Stores the bloom filter and the original file name
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
	static int BITSET_SIZE = (int) Math.pow(2, 28);

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

				// Start process
				p = builder.start();

				// Wait until thread has terminated
	        	p.waitFor();

	        	// Normalize file
	        	File outFileTwo = fileNormalize(outFile);

	        	// Hash n-grams into object with Bloom filter and original file name
	        	FilterData filter = new FilterData(gramHash(outFile.getName(), gramSize), i.getName());

	        	// Add object to the filterlist
	        	filterList.add(filter);

	        	// File is no longer needed
	        	outFile.delete();
	        	outFileTwo.delete();
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

		// Create new Bitset for intersection
		BitSet intersection = (BitSet) hash1.clone();

		// Size of the smallest set
		double denominator = (hash1.cardinality() < hash2.cardinality()) ? hash1.cardinality():hash2.cardinality();

		// Get the intersection
		intersection.and(hash2);

		// Get the size of the intersection
		double numerator = intersection.cardinality();

		// Return the percentage
		double percentage = (denominator != 0) ? (numerator / denominator * 100.0) : 0;

		// Checking for 100% which is invalid because of objdump error
		percentage = (percentage == 100) ? 0 : percentage;
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
		BitSet hashArray = new BitSet(BITSET_SIZE);

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

	public static File fileNormalize(File fileName)
	{
		// Input file
		FileReader asmInput = null;
		Scanner asmScan = null;

		// Output file
		PrintWriter asmOut = null;
		String outfile = "NORM." + fileName.getName();

		// Initialize IO
		try 
		{
			asmInput = new FileReader(fileName.getName());
			asmScan = new Scanner(new BufferedReader(asmInput));
			asmOut = new PrintWriter(outfile, "UTF-8");
		}

		// Some IO exception occurred, close files
		catch(Exception e)
		{
			System.out.println(e);
			System.exit(-1);
		}

		// Normalize input and write to output
		while(asmScan.hasNext()) 
		{
			// Get next line
			String nextLine = asmScan.nextLine();

			// clean line, should return only the assembly instructions
			nextLine = cleanLine(nextLine);

			// Print to output file
			if(nextLine.length() != 0)
			{
				asmOut.println(nextLine);
			}
		}

		// Close files
		asmOut.close();
		asmScan.close();

		// Reopen the file
		File f = new File(outfile);

		return f;
	}

	// Clean each line
	public static String cleanLine(String input)
	{
		try
		{	
			return input.substring(0, input.indexOf("\t"));
		}

		catch (Exception e)
		{
			return input;
		}
	}
}