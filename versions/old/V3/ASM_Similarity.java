import java.util.*;
import java.io.*;
import java.nio.*;
import java.security.*;

class ASM_Similarity
{
	public static void main(String [] args)
	{
		// Get command line args
		File samplesFolder = new File(args[0]);
		int gramSize = Integer.parseInt(args[1]);

		// Create array of files in folder
		ArrayList<File> fileList = new ArrayList<File>(Arrays.asList(samplesFolder.listFiles()));

		// Create array of normalized asm files
		ArrayList<String> normalizedAsmList = new ArrayList<String>();

		// Create a new process to run objdump
		Process p;

		// Create new asm files
		for(File i : fileList)
		{
			try 
			{
				// Outfile name
				File outFile = new File(i.getName() + ".ASM_TEMP");

				// Call objdump to get asm files
				ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", "objdump " + "-d " + "--no-show-raw-insn " + i + " | perl -p -e 's/^\\s+(\\S+):\\t//;'");
				builder.redirectOutput(outFile);
				builder.redirectError(new File("error.ASM_TEMP"));

				// Start process
				p = builder.start();

				// Wait until thread has terminated
	        	p.waitFor();

	        	// Normalize the asm file and add it to the normalized list
	        	normalizedAsmList.add(fileNormalize(outFile));
			}
			
			// Print exception if unable to objdump
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}

		for(String i : normalizedAsmList)
		{
			for(String j : normalizedAsmList)
			{
				BitSet hash1 = gramHash(i, gramSize);
				BitSet hash2 = gramHash(j, gramSize);

				// Generate percentage similarity
				double percentage = compareHash(hash1, hash2);

				System.out.println("Similarity of " + i + " and " + j + " : " + percentage + "%");
			}
		}

		File dir = new File(System.getProperty("user.dir"));

		for(File file: dir.listFiles()) 
		{
			if (file.getName().endsWith("ASM_TEMP"))
				file.delete();
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

	// Get the hash table of a file
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

		// To store to byte array
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

	public static String fileNormalize(File fileName)
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

		return outfile;
	}

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