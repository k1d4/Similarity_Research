import java.util.*;
import java.io.*;
import java.nio.*;
import java.security.*;

class ASM_Similarity
{
	public static void main(String [] args)
	{
		// Get command line args
		// flag = 0 for just hash comparison, flag = 1 for objdump, flag = 2 for normalize, flag = 3 for both
		int flag = Integer.parseInt(args[0]);
		String file1 = args[1];
		String file2 = args[2];
		int gramSize = Integer.parseInt(args[3]);

		// Run ObjDump to get output from executable
		if(flag == 1 || flag == 3)
		{
			// Create a new process to run objdump
			Process p;
			String objFile1 = "OBJ." + file1 + ".asm";
			String objFile2 = "OBJ." + file2 + ".asm";

			// Try to exec objdump
			try 
			{
				File outFile1 = new File(objFile1);
				File outFile2 = new File(objFile2);

				ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", "objdump " + "-d " + "--no-show-raw-insn " + file1 + " | perl -p -e 's/^\\s+(\\S+):\\t//;'");
				builder.redirectOutput(outFile1);
				builder.redirectError(new File("error.txt"));
				p = builder.start();

	        	p.waitFor();

	        	builder = new ProcessBuilder("/bin/sh", "-c", "objdump " + "-d " + "--no-show-raw-insn " + file2 + " | perl -p -e 's/^\\s+(\\S+):\\t//;'");
				builder.redirectOutput(outFile2);
				builder.redirectError(new File("error.txt"));
				p = builder.start();

	        	p.waitFor();
			}
			
			catch(Exception e)
			{
				e.printStackTrace();
			}

			// Set file string to objdump files
			file1 = objFile1;
			file2 = objFile2;
		}

		if(flag == 2 || flag == 3)
		{
			// Normalize both of the assembly files
			String norm1 = fileNormalize(file1);
			String norm2 = fileNormalize(file2);

			file1 = norm1;
			file2 = norm2;
		}

		// Create a hash table of the gramSize grams in the files
		boolean [] hash1 = gramHash(file1, gramSize);
		boolean [] hash2 = gramHash(file2, gramSize);

		// Generate percentage similarity
		double percentage = compareHash(hash1, hash2);

		System.out.println("Similarity: " + percentage + "%");
	}

	// Method for comparing two hashes
	public static double compareHash(boolean [] hash1, boolean [] hash2)
	{
		// The two variables needed for Jaccard Index
		double union = 0;
		double intersection = 0;

		// Do a comparison of each value in the hash table
		for(int i = 0; i < hash1.length; i++)
		{
			if(hash1[i] == true || hash2[i] == true)
			{
				if(hash1[i] == hash2[i])
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
	public static boolean [] gramHash(String fileName, int gramSize)
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
		boolean [] hashArray = new boolean [268435456];

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

			hashArray[index] = true;
		}

		return hashArray;
	}

	public static String fileNormalize(String fileName)
	{
		// Input file
		FileReader asmInput = null;
		Scanner asmScan = null;

		// Output file
		PrintWriter asmOut = null;
		String outfile = "NORM." + fileName;

		// Initialize IO
		try 
		{
			asmInput = new FileReader(fileName);
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

			// Remove comments from Assembly
			nextLine = removeComm(nextLine);

			// Normalize registers to "REG", this is me failing to write a good regex. NEED SUGESTION.
			nextLine = removeReg(nextLine);

			// Normalize labels, for example "00000000004008d0 <fputs@plt>:" to "LABEL:"
			nextLine = removeLab(nextLine);

			// Remove Constants 
			nextLine = removeConst(nextLine);

			// Remove locations following jumps
			nextLine = removeLoc(nextLine);

			// Remove locations vars
			nextLine = removeVar(nextLine);

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

	public static ArrayList<String> regGen()
	{
		// Create an ArrayList
		ArrayList<String> registers = new ArrayList<String>();

		// Inititialize partial Strings
		String [] midData = {"a", "b", "c", "d"};
		String [] upper = {"r", "e", ""};
		String [] lowerData = {"h", "l"};
		String [] lowerPointer = {"sp", "bp", "si", "di", "ip"};

		// Concatenate Strings for pointer registers
		for (String i : upper)
		{
			for (String j: lowerPointer)
			{
				registers.add(i + j);
			}
		}

		// 64 - 16 bit data registers
		for (String i : upper)
		{
			for (String j: midData)
			{
				registers.add(i + j + "x");
			}
		}

		// 8 bit data registers
		for(String i : midData)
		{
			for(String j: lowerData)
			{
				registers.add(i + j);
			}
		}

		// Return the register list
		return registers;
	}

	// Used to remove comments from string
	public static String removeComm(String input)
	{
		int comment = input.indexOf("#");

		if (comment != -1)
		{
			input = input.substring(0, comment);
		}

		return input;
	}

	// Used to normalize registers
	public static String removeReg(String input)
	{
		// Generate list of x86 registers
		ArrayList<String> registers = regGen();

		for(String i : registers)
		{
			input = input.replaceAll("%" + i, "%REG");
		}

		return input;
	}

	// Used to normalize labels
	public static String removeLab(String input)
	{
		// Generate list of x86 registers
		if (input.endsWith(":"))
		{
			if (input.contains("."))
			{
				return "GLOBAL-LABEL:";
			}

			return "LOCAL-LABEL:";
		}

		return input;
	}

	// Used to normalize constants
	public static String removeConst(String input)
	{
		return input.replaceAll("\\$0x[0-9a-f]*", "CONST");
	}

	// Used to remove location 
	public static String removeLoc(String input)
	{
		return input.replaceAll("[0-9a-f]*\\s<.*>", "LOC");
	}

	// Used to remove variables
	public static String removeVar(String input)
	{
		return input.replaceAll("-*0x[0-9a-f]*", "VAR");
	}
}