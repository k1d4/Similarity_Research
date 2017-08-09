import java.util.*;
import java.io.*;
import java.nio.*;
import java.security.*;

// ADD HTML FILE CHECKING

// Stores the copy data and the file names
class CopyData
{
	ArrayList<String> copy;
	String fileNameOne;
	String fileNameTwo;

	CopyData(ArrayList<String> copy, String fileNameOne, String fileNameTwo)
	{
		this.copy = copy;
		this.fileNameOne = fileNameOne;
		this.fileNameTwo = fileNameTwo;
	}
}

class PlagiarismChecker
{
	// Minimum size of copy that will get written
	static final int THRESHOLD = 10;

	public static void main(String [] args)
	{
		// Get command line args
		File samplesFolder = new File(args[0]);

		// Create array of files in folder
		ArrayList<File> folderList = new ArrayList<File>(Arrays.asList(samplesFolder.listFiles()));

		// Create array for the selected file problems
		ArrayList<File> problemList = new ArrayList<File>();

		// Find the problem files in the directory
		for(File i : folderList)
		{
			ArrayList<File> fileList = null;

			// Get files within each directory
			try
			{
				fileList = new ArrayList<File>(Arrays.asList(i.listFiles()));

				// Add each problem to the list
				problemList.addAll(fileList);
			}
			
			catch (Exception e)
			{
				System.out.println(e);
			}
		}

		try
		{
			// Create an output file
			PrintWriter indexOut = new PrintWriter(samplesFolder.getName() + "_plagiarism" + ".txt", "UTF-8");

			// Done List
			ArrayList<File> doneList = new ArrayList<File>();

			// Total CopyData list
			ArrayList<CopyData> totalCopyList = new ArrayList<CopyData>();

			for(File i : problemList)
			{
				// Add the file to the done list
				doneList.add(i);

				// Just to watch the program execute
				System.out.println(i.toString());

				for(File j : problemList)
				{
					if(doneList.contains(j))
					{
						continue;
					}

					if(!(i.getName().equals(j.getName())))
					{
						continue;
					}

					// Copylist of matching copyss
					ArrayList<CopyData> copyList = new ArrayList<CopyData>();

					// Create FileReader for both files
					FileReader inputOne = new FileReader(i.toString());
					FileReader inputTwo = new FileReader(j.toString());

					// Create Scanner for both files
					Scanner fileScanOne = new Scanner(new BufferedReader(inputOne));
					Scanner fileScanTwo = new Scanner(new BufferedReader(inputTwo));

					// String that will be checked
					ArrayList<String> copyOne = new ArrayList<String>();
					ArrayList<String> copyTwo = new ArrayList<String>();

					// Strings to be compared
					String sOne = null;
					String sTwo = null;

					// Index to check if a sequence is found
					int index = -1; 

					// Load the comparison set into copyTwo
					while(fileScanTwo.hasNext())
					{
						// Add line to copyTwo
						copyTwo.add(fileScanTwo.nextLine());
					}

					if(fileScanOne.hasNext())
					{
						sOne = fileScanOne.nextLine();
					}
				

					while(fileScanOne.hasNext())
					{
						// Make sure copyTwo is not empty
						if(index >= copyTwo.size())
						{
							if (!copyOne.isEmpty())
							{
								copyList.add(new CopyData(copyOne, i.toString(), j.toString()));
							}
							break;
						}

						// No previous match has been found
						if(index == -1)
						{
							for(int k = 0; k < copyTwo.size(); k++)
							{
								sTwo = copyTwo.get(k);

								if(sTwo.equals(sOne))
								{
									copyOne.add(sOne);
									index = k + 1;
									break;
								}
								index = -1;
							}
						}

						else
						{
							sTwo = copyTwo.get(index);

							if(sTwo.equals(sOne))
							{
								copyOne.add(sOne);
								index++;
							}

							else
							{
								copyList.add(new CopyData((ArrayList<String>) copyOne.clone(), i.toString(), j.toString()));
								index = -1;
								copyOne.clear();
								continue;
							}
						}

						sOne = fileScanOne.nextLine();
					}

					// Check last line
					for(int k = 0; k < copyTwo.size(); k++)
					{
						sTwo = copyTwo.get(k);

						if(sTwo.equals(sOne))
						{
							copyOne.add(sOne);
							break;
						}
					}

					if (!copyOne.isEmpty())
					{
						copyList.add(new CopyData(copyOne, i.toString(), j.toString()));
					}

					for(CopyData x : copyList)
					{
						if(x.copy.size() >= THRESHOLD)
						{
							System.out.println(x.copy.size());
							if(totalCopyList.size() != 0)
							{
								for(int l = 0; l < totalCopyList.size(); l++)
								{
									if(x.copy.size() >= totalCopyList.get(l).copy.size())
									{
										totalCopyList.add(l, x);
										break;
									}

									if(l == totalCopyList.size() - 1)
									{
										totalCopyList.add(x);
									}
								}
							}

							else
							{
								totalCopyList.add(x);
							}
						}
					}
				}
			}

			// Write out copys to list
			for(CopyData x : totalCopyList)
				{
					indexOut.println(x.fileNameOne + " -> " + x.fileNameTwo);
					indexOut.println("Size of copied section: " + x.copy.size());
					indexOut.println();

					for(String k : x.copy)
					{
						indexOut.println("\t" + k);
					}

					indexOut.println();
				}

				indexOut.close();
		}

		// Exception occured
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}