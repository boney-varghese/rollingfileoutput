package test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.jupiter.api.Test;

import com.output.RollingFileOutput.RollingFileOutputStream;

public class RollingFileTest {

	/*
	 * @BeforeEach public void deleteAllFiles() { clearLogFiles(); }
	 * 
	 * private static void clearLogFiles() { File folder = new
	 * File(System.getProperty("user.dir")); if (!folder.isDirectory()) { throw new
	 * IllegalStateException("not a directory"); }
	 * 
	 * File[] files = folder.listFiles(new FilenameFilter() {
	 * 
	 * @Override public boolean accept(File dir, String name) { return
	 * name.startsWith("testfile"); } }); for (final File file : files) { if
	 * (!file.delete()) { System.err.println("Can't remove " +
	 * file.getAbsolutePath()); } }
	 * 
	 * }
	 * 
	 * @AfterAll public static void tearDown() { clearLogFiles(); }
	 */
	
	@Test
	public void testGZipCompression() throws IOException {

		byte[] buffer = new byte[120];

		OutputStream fileOutputStream = new RollingFileOutputStream("Test_File.txt", 20L);
		FileInputStream fileInputStream = new FileInputStream("inputfile.txt");
		int bytes_read;


		while ((bytes_read = fileInputStream.read(buffer)) > 0) {
			fileOutputStream.write(buffer, 0, bytes_read);
		}

		fileInputStream.close();
		fileOutputStream.close();
	}

}
