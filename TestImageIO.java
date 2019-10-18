import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.stream.ImageInputStream;

/**
 * This program allows to reproduce an error that surfaces when reading
 * pictures with an associated color profile.  
 * <p>
 * Downloaded from: {@code https://github.com/gallardo/testImageIO}
 *
 * @author alberto
 */
public class TestImageIO {
	private final static String TEST_FILENAME = "testImage_sRGB.jpg";
	private final static int EXPECTED_RGB = 0xff010103;

	private static class TestIt implements Callable<Boolean> {

		private final int id;
		private final int threadNumber;
		final String filepath;
		final Object syncObj;

		TestIt(int id, int threadNumber, boolean sync) {
			this.id = id;
			this.threadNumber = threadNumber;
			this.filepath = TEST_FILENAME;
			this.syncObj = sync?TestImageIO.class:this;
		}

		@Override
		public Boolean call() {

			boolean successImageIO = false;
			try {
				BufferedImage bi;
				synchronized (syncObj) {
					final Iterator<ImageReader> imageReaders = ImageIO.getImageReadersByFormatName("jpg");
					ImageReader ir = imageReaders.next();
					{
						final IIOReadWarningListener iioReadWarningListener = (source, warning) -> System.err.println(warning);
						ir.addIIOReadWarningListener(iioReadWarningListener);
					}
					ImageInputStream iis = ImageIO.createImageInputStream(new File(filepath));
					ir.setInput(iis);
					bi = ir.read(0);
					ir.dispose();
					iis.close();

					// Option 2:
//					bi = ImageIO.read(new File(filepath));

					// Uncomment to see the result
//					ImageIO.write(bi, "jpg", new File(filepath + ".processed_" + threadNumber + ".jpg"));
				}
				successImageIO = bi.getRGB(0, 0) == EXPECTED_RGB;
				if (!successImageIO) {
					synchronized (TestImageIO.class) {
						//printf is not thread safe
						System.err.printf("    (%3d,%3d) !!!bi.getRGB(0,0) = %h. Expected: %h\n", id, threadNumber, bi.getRGB(0, 0), EXPECTED_RGB);
					}
				}
			} catch (Exception e) {
				synchronized (TestImageIO.class) {
					//printf is not thread safe
					System.err.printf("    (%3d,%3d) !!!Error reading '%s': %s\n", id, threadNumber, filepath, e.getLocalizedMessage());
				}
			}
			return successImageIO;
		}
	}

	private static void printHelpAndExit() {
		System.out.println("SYNOPSIS\n" +
				"\tjava " + TestImageIO.class.getSimpleName() + " nThreads nIterations serialize\n" +
				"\n" +
				"DESCRIPTION\n" +
				"\tThis class tests the loading of the test image '" + TEST_FILENAME + "' using ImageIO from a given\n" +
				"\tnumber of threads in parallel several iterations. Each iteration submits one task per thread\n" +
				"\tthat reads and tests the image. Afterwards, the file is read again several times serially, to\n" +
				"\tfind out if it recovers from a corrupt state.\n" +
				"\n" +
				"OPTIONS\n" +
				"\tnThreads\n" +
				"\t\tnumber of parallel threads\n" +
				"\n" +
				"\tnIterations\n" +
				"\t\tnumber of iterations of parallel reading of the test image\n" +
				"\n" +
				"\tserialize\n" +
				"\t\tshould the iterations invoke ImageIO in a sync bloc (serializing effectively the test)\n" +
				"\n" +
				"EXAMPLE\n" +
				"\tLaunch a test that iterates 10 times reading the test image with 5 parallel threads\n" +
				"\n" +
				"\t\t$ java " + TestImageIO.class.getSimpleName() + " 5 10 false\n" +
				"\n" +
				"\tLaunch a test that iterates 10 times reading the test image with 5 parallel threads, but\n" +
				"\tinvoking the ImageIO API synchronized\n" +
				"\n" +
				"\t\t$ java " + TestImageIO.class.getSimpleName() + " 5 10 true\n" +
				"\n");
	}

	public static void main(String[] args) {
		int nThreads = 0;
		int nIterations = 0;
		boolean sync = false;
		boolean failed = false;
		try {
			nThreads = Integer.parseInt(args[0]);
			nIterations = Integer.parseInt(args[1]);
			sync = Boolean.parseBoolean(args[2]);
		} catch (Exception e) {
			System.err.println("Wrong invokation:");
			e.printStackTrace(System.err);
			System.err.println();
			printHelpAndExit();
			System.exit(-1);
		}
		ExecutorService es = Executors.newFixedThreadPool(nThreads);

		System.out.println("Starting");

		for (int i=0; i < nIterations; i++) {
			Future<Boolean>[] futures = new Future[nThreads];
			System.out.println("Loop " + i);
			for (int j=0; j < nThreads; j++) {
				futures[j]=es.submit(new TestIt(i, j, sync));
			}
			System.out.println("Callables submitted. Collecting results");
			for (int j=0; j < nThreads; j++) {
				try {
					if (!futures[j].get()) {
						failed = true;
					}
					System.out.println("\tsuccess (" + i + ", " + j + ") -> " + futures[j].get());
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace(System.err);
				}
			}
		}
		System.out.println("Parallel reading ended. Now a serial test with 10 iterations:");
		for (int i=0; i<10; i++) {
			System.out.println("\tsuccess (" + i + "): " + new TestIt(i,0, false).call());
		}
		es.shutdown();
		if (failed) {
			System.exit(1);
		}
	}
}
