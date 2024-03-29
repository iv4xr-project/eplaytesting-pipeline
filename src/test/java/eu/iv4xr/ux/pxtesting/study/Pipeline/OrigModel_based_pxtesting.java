package eu.iv4xr.ux.pxtesting.study.Pipeline;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.evosuite.shaded.org.apache.commons.lang3.SerializationUtils;
//import org.apache.maven.shared.utils.io.FileUtils;
//import org.junit.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import agents.EventsProducer;
import agents.LabRecruitsTestAgent;
import environments.LabRecruitsConfig;
import environments.LabRecruitsEnvironment;
import eu.fbk.iv4xr.mbt.MBTProperties;
import eu.fbk.iv4xr.mbt.MBTProperties.LR_random_mode;
import eu.fbk.iv4xr.mbt.efsm.EFSM;
import eu.fbk.iv4xr.mbt.efsm.EFSMFactory;
import eu.fbk.iv4xr.mbt.efsm.EFSMOperation;
import eu.fbk.iv4xr.mbt.efsm.EFSMState;
import eu.fbk.iv4xr.mbt.efsm.EFSMTransition;
import eu.fbk.iv4xr.mbt.efsm.exp.Assign;
import eu.fbk.iv4xr.mbt.efsm.exp.Var;
import eu.fbk.iv4xr.mbt.efsm.exp.integer.IntSum;
import eu.fbk.iv4xr.mbt.efsm.exp.Const;
import eu.fbk.iv4xr.mbt.strategy.GenerationStrategy;
//import eu.fbk.iv4xr.mbt.strategy.RandomTestStrategy;
import eu.fbk.iv4xr.mbt.strategy.SearchBasedStrategy;
import eu.fbk.iv4xr.mbt.testcase.AbstractTestSequence;
import eu.fbk.iv4xr.mbt.testcase.MBTChromosome;
import eu.fbk.iv4xr.mbt.testsuite.SuiteChromosome;
import eu.fbk.iv4xr.mbt.testcase.Path;
import eu.fbk.iv4xr.mbt.utils.TestSerializationUtils;
import eu.iv4xr.framework.extensions.occ.Emotion;
import eu.iv4xr.framework.extensions.occ.EmotionAppraisalSystem;
import eu.iv4xr.framework.extensions.occ.Event;
import eu.iv4xr.framework.extensions.occ.Emotion.EmotionType;
import eu.iv4xr.framework.extensions.occ.Event.Tick;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.TestDataCollector;
import eu.iv4xr.framework.mainConcepts.ObservationEvent.TimeStampedObservationEvent;
import eu.iv4xr.framework.spatial.Vec3;
import eu.iv4xr.ux.pxtesting.study.labrecruits.CSVlevelImport;
import eu.iv4xr.ux.pxtesting.study.labrecruits.LREvent;
import eu.iv4xr.ux.pxtesting.study.labrecruits.PlayerOneCharacterization;
import eu.iv4xr.ux.pxtesting.study.labrecruits.levelsize;
import eu.iv4xr.ux.pxtesting.study.labrecruits.PlayerOneCharacterization.EmotionBeliefBase;
import game.LabRecruitsTestServer;
import game.Platform;
import eu.fbk.iv4xr.mbt.execution.labrecruits.LabRecruitsTestSuiteExecutor;

import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.mainConcepts.GoalStructure.PrimitiveGoal;
import nl.uu.cs.aplib.multiAgentSupport.Message;

import static eu.iv4xr.ux.pxtesting.study.labrecruits.CSVExport.exportToCSV;
import static eu.iv4xr.ux.pxtesting.study.labrecruits.CSVlevelImport.ImportFromCSV;
import static eu.iv4xr.ux.pxtesting.study.labrecruits.PlayerOneCharacterization.gotAsMuchPointsAsPossible;
import static eu.iv4xr.ux.pxtesting.study.labrecruits.PlayerOneCharacterization.questIsCompleted;
import static nl.uu.cs.aplib.AplibEDSL.* ;
import world.BeliefState;
import eu.fbk.iv4xr.mbt.Main;



/**
 * MBT and SearchBasedStrategy give us a bunch of generated tests to cover all transitions and states as much as possible. 
 * Using this along with the appraisal system we could record emotional states of different numbers of generated test cases.  
 * @author sansari
 * 
 */
// Original version when all MBT_model_test_generation, EmotionData and TestOperation are in the same class.
public class OrigModel_based_pxtesting {

	// use a logger to save output execution information
	protected static final Logger logger = LoggerFactory.getLogger(Main.class);
	protected List <EFSMState> desired_states_tocover = new ArrayList<EFSMState>();
	protected EFSM efsm_copy;
    // context variables
	public Var<Integer> hope = new Var<Integer>("Hope", 0);
	public Var<Integer> fear = new Var<Integer>("Fear", 0);
	public Var<Integer> joy = new Var<Integer>("Joy", 0);
	public Var<Integer> distress = new Var<Integer>("Distress", 0);
	public Var<Integer> satisfaction = new Var<Integer>("Satisfaction", 0);
	public Var<Integer> disappointment = new Var<Integer>("Disappointment", 0);
	public HashMap<Long,HashSet<Emotion>> map=new HashMap<Long,HashSet<Emotion>>();
	
	public void setPropertiesMBT() {
		// to control level creation, there are few parameters
		// seed for random number generation
		MBTProperties.LR_seed = 7652;

		// MBT random level generation has few mode that can be selected with
		// LR_random_mode
		// This mode allows to specify the number of rooms and the mean number of
		// buttons in a room
		MBTProperties.LR_generation_mode = LR_random_mode.N_BUTTONS_DEPENDENT;
		
		// Expected number of buttons in a room follow a Poisson distribution with
		// parameter
		MBTProperties.LR_mean_buttons = 0.5;

		// Number of room to generate
		MBTProperties.LR_n_rooms = 5;

		// Number of doors (corridors) to connect rooms
		MBTProperties.LR_n_doors = 5;

		// Time budget for test generation
		MBTProperties.SEARCH_BUDGET = 60;
		
		// MBT has a model factory controlled by the SUT_EFSM property
		// "labrecruits.random_default" generate a lab recruits level with parameters
		// specified above
		MBTProperties.SUT_EFSM = "labrecruits.random_simple";
		// there are some predefined configuration to pass to MBTProperties.SUT_EFSM
		// "labrecruits.random_simple", "labrecruits.random_medium",
		// "labrecruits.random_large"

	}

	/**
	 * Check if LabRecruits level exists
	 */
	public boolean existsLabRecruitLevel() {
		String efsmString = EFSMFactory.getInstance().getEFSM().getEFSMString();
		boolean out = !efsmString.equalsIgnoreCase("");
	     efsm_copy = EFSMFactory.getInstance().getEFSM();
		return out;
	}
	
	/**
	 * Run test generation
	 */
	public SuiteChromosome runTestGeneration() {

		// initialize the test generator to use search based strategy
		GenerationStrategy generationStrategy = new SearchBasedStrategy<MBTChromosome>();
		// random stategy could also be used
		// GenerationStrategy generationStrategy = new
		// RandomTestStrategy<MBTChromosome>();

		// run test generation  
		SuiteChromosome solution = generationStrategy.generateTests();
		// run test generation to cover selected states
		//SuiteChromosome solution = generationStrategy.generateTests(desired_states_tocover);

		return solution;
	}

	/**
	 * Save generated test
	 * 
	 * @param solution   solution generated
	 * @param testFolder folder where to save the tests (make sure tests folder
	 *                   exists)
	 */
	private void writeTests(SuiteChromosome solution, String testFolder) {
		// make sure tests folder exists
		File testsFolder = new File(testFolder);
		testsFolder.mkdirs();
		int count = 1;
		for (MBTChromosome testCase : solution.getTestChromosomes()) {
			String dotFileName = testFolder + File.separator + "test_" + count + ".dot";
			String txtFileName = testFolder + File.separator + "test_" + count + ".txt";
			String serFileName = testFolder + File.separator + "test_" + count + ".ser";
			File dotFile = new File(dotFileName);
			File txtFile = new File(txtFileName);
			try {
				FileUtils.writeStringToFile(dotFile, ((AbstractTestSequence) testCase.getTestcase()).toDot(),
						Charset.defaultCharset());
				FileUtils.writeStringToFile(txtFile, testCase.getTestcase().toString(), Charset.defaultCharset());
				TestSerializationUtils.saveTestSequence((AbstractTestSequence) testCase.getTestcase(), serFileName);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			count++;
		}

	}

	/**
	 * Save EFSM model
	 * 
	 * @param modelFolderName folder where to save the model
	 */
	public void writeModel(String modelFolderName) {
		File modelFolder = new File(modelFolderName);
		modelFolder.mkdirs();

		String modelFileName = modelFolderName + File.separator + "EFSM_model.ser";
		String levelFileName = modelFolderName + File.separator + "LabRecruits_level.csv";
		String modelDotFileName = modelFolderName + File.separator + "EFSM_model.dot";

		File csvFile = new File(levelFileName);
		File dotFile = new File(modelDotFileName);

		EFSM efsm = EFSMFactory.getInstance().getEFSM();
		try {

			FileUtils.writeByteArrayToFile(new File(modelFileName), 
					// old MBT 1.1.0b code, upgrading it; it should return a byte-code
					// serialization of the current EFSM:
					// EFSMFactory.getInstance().getOriginalEFSM()
					SerializationUtils.serialize(EFSMFactory.getInstance().getEFSM())
					);
			FileUtils.writeStringToFile(dotFile, efsm.getDotString(), Charset.defaultCharset());
			// if csv is available
			if (efsm.getEFSMString() != "") {
				FileUtils.writeStringToFile(csvFile, efsm.getEFSMString(), Charset.defaultCharset());
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void loadModel(String modelFolderName) {
		File modelFolder = new File(modelFolderName);
		modelFolder.mkdirs();
		String modelFileName = modelFolderName + File.separator + "EFSM_model.ser";
		try {
			efsm_copy= TestSerializationUtils.loadEFSM(modelFileName);
		} catch (FileNotFoundException e) {
			
			e.printStackTrace();
		}
	}
	
	/**
	 * Load serialized tests into a SuiteChromosome object
	 * 
	 * @param testsDir
	 * @return
	 */
	private SuiteChromosome parseTests(String testsDir) {
		SuiteChromosome suite = new SuiteChromosome();
		try {
			List<File> files = org.apache.maven.shared.utils.io.FileUtils.getFiles(new File(testsDir), "*.ser", "");
			for (File file : files) {
				AbstractTestSequence test = TestSerializationUtils.loadTestSequence(file.getAbsolutePath());
				MBTChromosome chromosome = new MBTChromosome();
				chromosome.setTestcase(test);
				suite.addTest(chromosome);

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return suite;
	}

	//@Test
	public void runGenerationTest() {
		// set the parameters for the generation
		setPropertiesMBT();

		// check if csv can be generated
		if (!existsLabRecruitLevel()) {
			fail();
		}
		
		// run test generation
		SuiteChromosome solution = runTestGeneration();

		// output folders
		String rootFolder = new File(System.getProperty("user.dir")).getParent();
		String testFolder = rootFolder + File.separator + "MBTtest";
		String modelFolder = testFolder + File.separator + "Model";
		
		// save generated tests
		File testFolderFile = new File(testFolder);
		if (!testFolderFile.exists()) {
			testFolderFile.mkdirs();
		}
		File modelFolderFile = new File(modelFolder);
		if (!modelFolderFile.exists()) {
			modelFolderFile.mkdirs();
		}

		writeTests(solution, testFolder);
		writeModel(modelFolder);
	}


	@Test
    public void runGeneratedTests() throws IOException {

	    String rootFolder = new File(System.getProperty("user.dir")).getParent();
        String testFolder = rootFolder + File.separator + "MBTtest";
        String modelFolder = testFolder + File.separator + "Model";
        String labRecruitesExeRootDir = rootFolder + File.separator + "iv4xrDemo";
        loadModel(modelFolder);

        // load tests from file
        SuiteChromosome loadedSolution = parseTests(testFolder);

        // before converting it is needed a LR test agent

        // open the server
        LabRecruitsTestServer testServer = new LabRecruitsTestServer(false,
                Platform.PathToLabRecruitsExecutable(labRecruitesExeRootDir));

        // set the configuration of the server 
        // level file name is hard coded in writeModel but can be changed
        LabRecruitsConfig lrCfg = new LabRecruitsConfig("LabRecruits_level", modelFolder);
        levelsize lrsize= new levelsize(CSVlevelImport.ImportFromCSV("LabRecruits_level", modelFolder));
        
        // convert test cases in loadedSolution to goal structure
        
        // use the test case executor to convert
        LabRecruitsTestSuiteExecutor lrExecutor = new LabRecruitsTestSuiteExecutor(rootFolder, testFolder, modelFolder, null);
        
        // iterate over test case and convert to goals
        String results = "" ;
        for (int i = 0; i < loadedSolution.size(); i++) {
            AbstractTestSequence testcase = (AbstractTestSequence) loadedSolution.getTestChromosome(i).getTestcase();
            
            
            // the system returns a list of goals that can be sequentialized with SEQ
            LabRecruitsEnvironment labRecruitsEnvironment = new LabRecruitsEnvironment(lrCfg);
            var dataCollector = new TestDataCollector();
            // run the testing task:
            LabRecruitsTestAgent testAgent = new LabRecruitsTestAgent("Agent1") ; 
            testAgent.attachState(new BeliefState())
                     .setTestDataCollector(dataCollector)
                     .attachEnvironment(labRecruitsEnvironment)
                     .attachSyntheticEventsProducer(new EventsProducer());
            		 

            List<GoalStructure> goals = lrExecutor.convertTestCaseToGoalStructure(testAgent, testcase);
            // Add instrumentation here:
            goals = instrumentTestCase(testAgent,testcase,goals) ;
            GoalStructure g =SEQ(goals.toArray(new GoalStructure[goals.size()]));
            
            System.out.println(">> Testing task: " + i + ", #=" + goals.size()) ;
            System.out.println(">> " + testcase ) ;
            
            testAgent.setGoal(g) ;
            
            // add an event-producer to the test agent so that it produce events for
            // emotion appraisals:
            	
            // Create an emotion appraiser, and hook it to the agent:
            EmotionAppraisalSystem eas = new EmotionAppraisalSystem(testAgent.getId());
            eas.attachEmotionBeliefBase(new EmotionBeliefBase().attachFunctionalState(testAgent.getState()))
                    .withUserModel(new PlayerOneCharacterization()).addGoal(questIsCompleted, 50)
                    .addGoal(gotAsMuchPointsAsPossible, 50).addInitialEmotions();
            
         // some lists for collecting experiment data:
            List<String[]> csvData_goalQuestIsCompleted = new LinkedList<>();
            String[] csvRow = { "t", "x", "y", "hope", "joy", "satisfaction", "fear" };
            csvData_goalQuestIsCompleted.add(csvRow);
            List<String[]> csvData_goalGetMuchPoints = new LinkedList<>();
            csvData_goalGetMuchPoints.add(csvRow);
            Function<Emotion, Float> normalizeIntensity = e -> e != null ? (float) e.intensity / 800f : 0f;
            labRecruitsEnvironment.startSimulation();
            
            // goal not achieved yet
            assertFalse(testAgent.success());
            int t=0 ;
            while (g.getStatus().inProgress()) {
            	 Vec3 position = testAgent.getState().worldmodel.position;
                 System.out.println("*** " + t + ", " + testAgent.getState().id + " @" + position);
                 
                 if (testAgent.getSyntheticEventsProducer().currentEvents.isEmpty()) {
                     eas.update(new Tick(), t);
                 }
                 else {
                     for (Message m :testAgent.getSyntheticEventsProducer().currentEvents) {
                         eas.update( new LREvent(m.getMsgName()), t);
                         if(!eas.newEmotions.isEmpty())
                         {
                         map.put(testAgent.getState().worldmodel.timestamp, eas.newEmotions);
                         }
                     }
                 }

                 if (position != null) {
                     Vec3 p_ = position.copy();
                     Float score = (float) testAgent.getState().worldmodel().score;
                     System.out.println("*** score=" + score);

                     float hope_completingQuest = normalizeIntensity
                             .apply(eas.getEmotion(questIsCompleted.name, EmotionType.Hope));
                     float joy_completingQuest = normalizeIntensity
                             .apply(eas.getEmotion(questIsCompleted.name, EmotionType.Joy));
                     float satisfaction_completingQuest = normalizeIntensity
                             .apply(eas.getEmotion(questIsCompleted.name, EmotionType.Satisfaction));
                     float fear_completingQuest = normalizeIntensity
                             .apply(eas.getEmotion(questIsCompleted.name, EmotionType.Fear));
                     
                     float hope_getMuchPoints = normalizeIntensity
                             .apply(eas.getEmotion(gotAsMuchPointsAsPossible.name, EmotionType.Hope));
                     float joy_getMuchPoints = normalizeIntensity
                             .apply(eas.getEmotion(gotAsMuchPointsAsPossible.name, EmotionType.Joy));
                     float satisfaction_getMuchPoints = normalizeIntensity
                             .apply(eas.getEmotion(gotAsMuchPointsAsPossible.name, EmotionType.Satisfaction));
                     float fear_getMuchPoints = normalizeIntensity
                             .apply(eas.getEmotion(gotAsMuchPointsAsPossible.name, EmotionType.Fear));

                     String[] csvRow1 = { "" + t, "" + p_.x, "" + p_.z, "" + hope_completingQuest,
                             "" + joy_completingQuest, "" + satisfaction_completingQuest, "" + fear_completingQuest };

                     String[] csvRow2 = { "" + t, "" + p_.x, "" + p_.z, "" + hope_getMuchPoints, "" + joy_getMuchPoints,
                             "" + satisfaction_getMuchPoints, "" + fear_getMuchPoints };

                     csvData_goalQuestIsCompleted.add(csvRow1);
                     csvData_goalGetMuchPoints.add(csvRow2);
                 }
                if (t>10000) {
                    break ;
                }
                try {
                    Thread.sleep(200);
                    testAgent.update();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                t++ ;
             }
            
            
            update_EFSMmodel(map,testAgent);
            
            exportToCSV(csvData_goalQuestIsCompleted, "data_goalQuestCompleted_"+i+".csv");
            exportToCSV(csvData_goalGetMuchPoints, "data_goalGetMuchPoints_"+i+".csv");
            
            // run the python script called "mkgraph.py" for drawing graphs according to the saved .csv  
            String path=new File(new File(System.getProperty("user.dir")).getAbsolutePath(),"mkgraph.py").getAbsolutePath();
			ProcessBuilder builder = new ProcessBuilder(); 
			ProcessBuilder pb = new ProcessBuilder();
			
			//sending the csvfile number, width and heights of the level as parameters.
			builder.command("python", path,""+i,""+lrsize.getheight(),""+lrsize.getwidth());
			Process p=builder.start();
			BufferedReader bfr = new BufferedReader(new InputStreamReader(p.getInputStream()));
	
			System.out.println(".........start   visualization process.........");  
		    String line = "";     
		    while ((line = bfr.readLine()) != null){
			      System.out.println("Python Output: " + line);
		    }
            
            g.printGoalStructureStatus();
            labRecruitsEnvironment.close() ;           
            results += ">> tc-" + i + ", Duration: "+ t + ", goalstatus: " + g.getStatus() 
               + ", #fail: " + testAgent.getTestDataCollector().getNumberOfFailVerdictsSeen()
               + ", #success: " + testAgent.getTestDataCollector().getNumberOfPassVerdictsSeen() + "\n" ;
          
        }
        System.out.println("" + results) ;
        testServer.close();
        
	}

	GoalStructure transitionMarker(TestAgent agent, EFSMTransition tr, String marker) {
		String tr_id = "" + tr.getId() + "_" + tr.toString() ;
		return goal("Start of MBT transition " + tr_id)
				.toSolve(doesNotMatter -> true) // will be immediately solved
				.withTactic(action("bla")
					.do1((BeliefState S) -> {
					   agent.getTestDataCollector().registerEvent(
							   agent.getId(), 
							   new TimeStampedObservationEvent(
									   "MBT-transition-" + marker,
									   tr_id + ":" + S.worldmodel.timestamp 
									   )) ;
					   
					   return true ;
				      })
					.lift())
				.lift() ;
	}
	
	List<GoalStructure> instrumentTestCase(TestAgent agent, AbstractTestSequence tc, List<GoalStructure> tcgoals) {
		Path path = tc.getPath();
		List<EFSMTransition> transitions = path.getTransitions();
		List<GoalStructure> intrumentedTcGoals = new LinkedList<>() ;
		int k = 0 ;
		for(var tr : transitions) {
			var G = tcgoals.get(k) ;
			String trId = tr.getId() ;
			intrumentedTcGoals.add(SEQ(
					transitionMarker(agent,tr,"START"),
					G,
					transitionMarker(agent,tr,"END")
					)) ;
			k++ ;
		}
		return intrumentedTcGoals ;
	}
	
	  
	private void update_EFSMmodel(HashMap<Long, HashSet<Emotion>> activated_emotions, TestAgent agent) throws IOException {

		HashMap<String,Long[]> tr_timestamp=new HashMap<String,Long[]>();
		for(var e : agent.getTestDataCollector().getTestAgentTrace(agent.getId()))
		{
			
			if(e.getFamilyName().startsWith("MBT-transition") &&e.getFamilyName().contains("START"))
			{
				ArrayList<String> info = 
						new  ArrayList<String>(Arrays.asList(e.getInfo().split(":")));
				
				//find the the time that the same transition ends and return the timestamp.
				String t_end=agent.getTestDataCollector().getTestAgentTrace(agent.getId()).stream()
															.filter(t->(t.getInfo().contains(info.get(0))&&t.getFamilyName().contains("END")))
															.map(t-> t.getInfo().substring(t.getInfo().lastIndexOf(":")+1))
															.findFirst().orElse(null);
																										
				// add to have all transition names (tr_id+ t.string) with their start & ending time.
				if(t_end!=null)
				{
					tr_timestamp.put(info.get(0), new Long[] {Long.parseLong(info.get(1)),Long.parseLong(t_end)});	
				}
			}
		}
		
		Assign<Boolean> assign=null;	
		for (Long key : activated_emotions.keySet()) {		
		    String transition_name=null;
			HashSet<Emotion> value = activated_emotions.get(key);
			
		    //check the time the emotion is triggered is in range for any transition 
		    transition_name=tr_timestamp.entrySet().stream().filter(p->p.getValue()[0]<=key&& key<=p.getValue()[1])
		    												.map(k->k.getKey())
		    												.findFirst().orElse(null);		    
		    if(transition_name!=null)
		    {
			    EFSMTransition tr=null;
		    	boolean hope_match =value.stream().anyMatch(x ->x.etype==EmotionType.Hope);
			    if(hope_match)
			    {
			    	assign = new Assign(hope , new IntSum(hope,new Const(1)));
			    	tr=efsm_copy.getTransition(transition_name.substring(0, transition_name.lastIndexOf("_")));
			    	if(tr.getOp()==null)
			    	{
			    		EFSMOperation x  = new EFSMOperation(assign);
			    		tr.setOp(x);
			    	}else {
			    		tr.getOp().getAssignments().put(assign);
		            }
			    	
			    }
			    boolean fear_match=value.stream().anyMatch(x ->x.etype==EmotionType.Fear);
			    if(fear_match)
			    {
			    	assign = new Assign(fear , new IntSum(fear,new Const(1)));
			    	tr=efsm_copy.getTransition(transition_name.substring(0, transition_name.lastIndexOf("_")));
			    	if(tr.getOp()==null)
			    	{
			    		EFSMOperation x  = new EFSMOperation(assign);
			    		tr.setOp(x);
			    	}else {
			    		tr.getOp().getAssignments().put(assign);
		            }
			    }
			    boolean joy_match=value.stream().anyMatch(x ->x.etype==EmotionType.Joy);
			    if(joy_match)
			    {
			    	assign = new Assign(joy , new IntSum(joy,new Const(1)));
			    	tr=efsm_copy.getTransition(transition_name.substring(0, transition_name.lastIndexOf("_")));
			    	if(tr.getOp()==null)
			    	{
			    		EFSMOperation x  = new EFSMOperation(assign);
			    		tr.setOp(x);
			    	}else {
			    		tr.getOp().getAssignments().put(assign);
		            }
			    }
			    boolean distress_match=value.stream().anyMatch(x ->x.etype==EmotionType.Distress);
			    if(distress_match)
			    {
			    	assign = new Assign(distress , new IntSum(distress,new Const(1)));
			    	tr=efsm_copy.getTransition(transition_name.substring(0, transition_name.lastIndexOf("_")));
			    	if(tr.getOp()==null)
			    	{
			    		EFSMOperation x  = new EFSMOperation(assign);
			    		tr.setOp(x);
			    	}else {
			    		tr.getOp().getAssignments().put(assign);
		            }
			    }
			    boolean satf_match=value.stream().anyMatch(x ->x.etype==EmotionType.Satisfaction);
			    if(satf_match)
			    {
			    	assign = new Assign(satisfaction , new IntSum(satisfaction,new Const(1)));
			    	tr=efsm_copy.getTransition(transition_name.substring(0, transition_name.lastIndexOf("_")));
			    	if(tr.getOp()==null)
			    	{
			    		EFSMOperation x  = new EFSMOperation(assign);
			    		tr.setOp(x);
			    	}else {
			    		tr.getOp().getAssignments().put(assign);
		            }
			    }
			    boolean disap_match=value.stream().anyMatch(x ->x.etype==EmotionType.Disappointment);
			    if(disap_match)
			    {
			    	assign = new Assign(disappointment , new IntSum(disappointment,new Const(1)));
			    	tr=efsm_copy.getTransition(transition_name.substring(0, transition_name.lastIndexOf("_")));
			    	if(tr.getOp()==null)
			    	{
			    		EFSMOperation x  = new EFSMOperation(assign);
			    		tr.setOp(x);
			    	}else {
			    		tr.getOp().getAssignments().put(assign);
		            }
			    }

		    }
	    
		}
	}
}
