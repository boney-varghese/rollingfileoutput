package com.output.RollingFileOutput;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Comparator;
import java.util.zip.GZIPOutputStream;

public class RollingFileOutputStream extends FileOutputStream{
	
	private final long maxBytes;
	private final String fileName;
	private String extension;
	private String fileNameWithoutExtension;
    
	public RollingFileOutputStream( String fileName, long maxBytes ) throws FileNotFoundException {
        super( new File( fileName ), true );
        this.fileName = fileName;
        this.maxBytes = maxBytes;
        this.extension = fileName.contains( "." ) ? fileName.substring ( fileName.lastIndexOf(".") ) : "";
		this.fileNameWithoutExtension = fileName.contains( "." ) ? fileName.substring( 0, fileName.lastIndexOf(".") ) : fileName;
    }

	@Override
	public void write( byte[] data ) throws IOException {
		
		FileChannel fileChannel = this.getChannel();
		
		//If max_allowed == file_size : Close current and open a new file
		if(this.maxBytes == fileChannel.size()) {
			this.close();
			
			//Rename current file with number suffix
			renameFile();
			
			//Write to new file with the initial filename
			writeToNewFile(data);
		}
		
		//If some space available in the current file, fill it and the remaining data to new file
		else if(this.maxBytes <= fileChannel.size() + data.length){
			int dataSizeToWrite = (int)(this.maxBytes - fileChannel.size());
			super.write(data, 0, dataSizeToWrite);
			
			//Current file closed and opened a new file with the remaining data
			if(dataSizeToWrite != data.length) {
				this.close();
				
				//Rename current file with number suffix
				renameFile();
				
				byte[] subArray = Arrays.copyOfRange(data, dataSizeToWrite, data.length);
				writeToNewFile(subArray);
			}
		}
		
		//enough space in the file for the data
		else {
			super.write(data);
		}
	}
	
	@Override
	public void write(byte[] data, int off, int len) throws IOException {
		//create a subarray of data and pass to write(data) method
		byte[] subArray = Arrays.copyOfRange(data, off, len);
		write(subArray);
	}

	@Override
	public void write(int b) throws IOException {
		super.write(b);
	}
    
	//Find the latest file name
	private static String findFileName(String fileName) {
		File dir = new File(System.getProperty("user.dir"));
		
        if(!dir.isDirectory()) {
        	throw new IllegalStateException("not a directory");
        }
        
        File[] dirList = dir.listFiles(new CustomFilenameFilter(fileName));
        Arrays.sort(dirList, new FileSuffixComparator());   
        
		return dirList.length==0?fileName:dirList[0].getName();
	}
	
	//write to a new file when the limit is exceeded
	private void writeToNewFile(byte[] data) throws IOException {
		OutputStream os = new RollingFileOutputStream(fileName, this.maxBytes);
		os.write(data);
		os.close();
	}
	
	//Rename the current file with the number suffix
	private void renameFile() throws IOException {
		File current = new File(fileName);
		
		String latestFileName = findFileName(fileNameWithoutExtension);
		int newSuffix = RollingFileUtil.getSuffix(latestFileName) + 1;
		
		File newFile = new File(fileNameWithoutExtension + "." + newSuffix + extension);
		current.renameTo(newFile);
		
		// compress current
		InputStream is = new FileInputStream(newFile);
		GZIPOutputStream os = new GZIPOutputStream(new FileOutputStream(fileNameWithoutExtension + "." + newSuffix + extension + ".gz"));
		copyStream(is, os,-1);
		
		is.close();
		os.flush();
		os.close();
		
		newFile.delete();
	}

	public static int copyStream(InputStream src, OutputStream dst, int bufferSize) throws IOException {
		int totalCopied = 0;

		if (!(src instanceof BufferedInputStream)) {
			if (bufferSize > 0)
				src = new BufferedInputStream(src, bufferSize);
			else
				src = new BufferedInputStream(src);
		}

		if (!(dst instanceof BufferedOutputStream)) {
			if (bufferSize > 0)
				dst = new BufferedOutputStream(dst, bufferSize);
			else
				dst = new BufferedOutputStream(dst);
		}

		int b;

		while ((b = src.read()) != -1) {
			dst.write(b);
			totalCopied++;
		}

		dst.flush();

		return totalCopied;
	}

}

//Class to filter the files based on the fileName
class CustomFilenameFilter implements FilenameFilter {
	private String filterName;
	public CustomFilenameFilter(String filterName) {
		this.filterName = filterName;
	}
	public boolean accept(File dir, String name) {
		String lowercaseName = name.toLowerCase();
		if (lowercaseName.startsWith(filterName)) {
			return true;
		} else {
			return false;
		}
	}
}

//Comparator to sort files based on fileName and suffix
class FileSuffixComparator implements Comparator<File> {
	@Override
	public int compare(File o1, File o2) {
		int n1 = RollingFileUtil.getSuffix(o1.getName());
		int n2 = RollingFileUtil.getSuffix(o2.getName());
		return n2 - n1;
	}
}

class RollingFileUtil{
	public static int getSuffix(String fileName) {
		int suffix;
		if (fileName.lastIndexOf(".") == -1) {
			suffix = 0;
		} else {
			try {
				int firstIndex = fileName.lastIndexOf(".");
				String first = fileName.substring(0, firstIndex);
				int secondIndex = first.lastIndexOf(".");
				String second = first.substring(0, secondIndex);
				int thirdIndex = second.lastIndexOf(".");
				String number = second.substring(thirdIndex + 1);
				suffix = Integer.parseInt(number);
			} catch (Exception e) {
				suffix = 0;
			}
		}
		return suffix;
	}
}