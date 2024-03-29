package eu.iv4xr.ux.pxtesting.mbt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;


import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import com.google.common.primitives.Ints;

import eu.fbk.iv4xr.mbt.testcase.AbstractTestSequence;
import eu.fbk.iv4xr.mbt.utils.TestSerializationUtils;
import nl.uu.cs.aplib.utils.Pair;

public class AbstractTestUtils {
	
	/**
	 * Save a test-sequence in a file as a serialized object (.ser file),
	 * and also save its visualization as a .dot file, and as a text
	 * version (.txt file).
	 * @throws IOException 
	 */
	public static void save(String dir, 
			String basefileName, 
			AbstractTestSequence seq) throws IOException {

		String base = basefileName ;
		if (dir != null) {
			base = Paths.get(dir,basefileName).toString() ;
		}
		FileUtils.writeStringToFile(new File(base + ".dot"), seq.toDot(), Charset.defaultCharset());
		FileUtils.writeStringToFile(new File(base + ".txt"), seq.toString(), Charset.defaultCharset());
		TestSerializationUtils.saveTestSequence(seq, base + ".ser");
	}
	
	public static void save(String dir, 
			String basefileName, 
			List<AbstractTestSequence> testSuite) throws IOException {
		
		int k = 0 ;
		for (var seq : testSuite) {
			save(dir, basefileName + "_" + k, seq) ;
			k++ ;
		}
	}
	
	/**
	 * Read a serialized test-case from a file (.ser file).
	 */
	public static AbstractTestSequence parseAbstractTestSeq(String dir, String fname) throws FileNotFoundException {
		if (dir != null) {
			fname = Paths.get(dir,fname).toString() ;
		}
		AbstractTestSequence seq = TestSerializationUtils.loadTestSequence(fname);
		return seq ;
	}
	
	/**
	 * Parse a bunch of serialized test-cases from a directory.
	 * @throws IOException 
	 */
	public static List<Pair<String,AbstractTestSequence>> parseAbstrastTestSuite(String dir) throws IOException {
		List<Pair<String,AbstractTestSequence>> suite = new LinkedList<>() ;
		List<File> files = org.apache.maven.shared.utils.io.FileUtils.getFiles(new File(dir), "*.ser", "");
		for (File file : files) {
			
			String fname = file.getName() ;
			if (fname.startsWith("EFSM"))
				continue ;
			// dropping the extension .ser
			fname = fname.substring(0, fname.length() - 4) ;
			
			AbstractTestSequence test = TestSerializationUtils.loadTestSequence(file.getAbsolutePath());
			suite.add(new Pair<>(fname,test)) ;
		}
		return suite;
	}
	
	
	public static void printStats(List<AbstractTestSequence>  suite) {
		
		int[] tcLens = Ints.toArray(suite.stream().map(seq -> seq.getLength()).collect(Collectors.toList())) ;
					
		var stats = new SummaryStatistics() ;
		for (var seq : suite) stats.addValue(seq.getLength());
		
		var stats2 = (new Distance("jaro-winkler")).distances(suite) ;
		
		System.out.println("** Test suite, #= " + stats.getN()) ;
		System.out.println("**   len-min    = " + stats.getMin()) ;
		System.out.println("**   len-max    = " + stats.getMax()) ;
		System.out.println("**   len-avrg   = " + stats.getMean()) ;
		System.out.println("**   len-stdev  = " + stats.getStandardDeviation()) ;
		System.out.println("**   tcs-distance-avrg  = " + stats2.getMean()) ;
		System.out.println("**   tcs-distance-stdev = " + stats2.getStandardDeviation()) ;
		
		
	}

}
