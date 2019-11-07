/**
 * 
 */
package org.openmrs.module.drawing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
//import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptComplex;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptDescription;
import org.openmrs.ConceptName;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.Module;
import org.openmrs.module.ModuleFactory;
import org.openmrs.module.drawing.handlers.DrawingTagHandler;
import org.openmrs.module.drawing.obs.handler.DrawingHandler;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.htmlformentry.RegressionTestHelper;
import org.openmrs.obs.ComplexData;
import org.openmrs.obs.handler.AbstractHandler;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.util.DatabaseUtil;
import org.openmrs.util.OpenmrsClassLoader;
import org.openmrs.util.OpenmrsConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author long27km
 */

//https://wiki.openmrs.org/display/docs/Unit+Tests#UnitTests-Settinguptheexpecteddatabase
//BaseContextSensitiveTest classes will automatically load standardTestDataset.xml,
//and you may use any object defined there for your tests. 
//standardTestDataset.xml includes

//https://github.com/openmrs/openmrs-core/blob/master/api/src/test/resources/org/openmrs/include/standardTestDataset.xml
//<concept_datatype concept_datatype_id="3" name="Text" hl7_abbreviation="ST" description="Free text" creator="1" date_created="2004-02-02 00:00:00.0" retired="false" uuid="8d4a4ab4-c2cc-11de-8d13-0010c6dffd0f"/>
//<concept_datatype concept_datatype_id="4" name="N/A" hl7_abbreviation="ZZ" description="Not associated with a datatype (e.g., term answers, sets)" creator="1" date_created="2004-02-02 00:00:00.0" retired="false" uuid="8d4a4c94-c2cc-11de-8d13-0010c6dffd0f"/>
//concept_datatype concept_datatype_id="13" name="Complex" hl7_abbreviation="ED" description="Complex value.  Analogous to HL7 Embedded Datatype" creator="1" date_created="2008-06-11 13:22:00.0" retired="false" uuid="8d4a6242-c2cc-11de-8d13-0010c6dffd0f"/>
public class DrawingTagHandlerTest extends BaseModuleContextSensitiveTest {
	
	@Autowired
	ConceptService conceptService;
	
	private abstract class DrawingTestHelper extends RegressionTestHelper {
		
		final protected Date date = new Date();
		
		@Override
		protected String getXmlDatasetPath() {
			//the xml files in test/resources will be copied into the root
			//while the classes from java will be in the 
			//standard java classpath dir structure "org/openmrs/module/drawing...."
			return "";
		}
		
		@Override
		public String[] widgetLabels() {
			return new String[] { "Date:", "Location:", "Provider:", "Drawing:" };
		}
		
		@Override
		public String[] widgetLabelsForEdit() {
			return widgetLabels();
		}
		
	}
	
	final private String SIMPLE_UUID = "17d7503a-e705-49af-b0f6-b0b567716860";
	
	final private String COMPLEX_UUID = "2956c045-3154-40b3-bf30-65b3c49d9f59";
	
	final private String UNDEFINED_CONCEPT_UUID = "b85ad74e-dff4-4a7d-8d93-0e0c235f59f6";
	
	private Concept simpleConcept;
	
	private ConceptComplex complexConcept;
	
	private int complexConceptId;
	
	private Map<String, String[]> parameters;
	//	private Map<String, String> results;
	
	public void showInfo() {
		List<ConceptClass> allConceptClasses = conceptService.getAllConceptClasses();
		System.out.println("===Loaded Concept Classes===");
		for (ConceptClass cc : allConceptClasses) {
			System.out.println(cc.getName());
		}
		
		List<ConceptDatatype> allConceptDataTypes = conceptService.getAllConceptDatatypes();
		System.out.println("===Loaded Concept Datatypes===");
		for (ConceptDatatype cd : allConceptDataTypes) {
			System.out.println(cd.getName());
		}
	}
	
	private void compareBuiltExceptionMsg(String expectedExceptionMsg, String curMsg, String matchTill) {
		//check that this is actually the argument that was invalid
		//a little naive in the way of impl. 
		//but more complicated could be more error prone and less obvious
		
		expectedExceptionMsg = expectedExceptionMsg.toLowerCase();
		curMsg = curMsg.toLowerCase();
		
		matchTill = matchTill.toLowerCase();
		
		if (expectedExceptionMsg == null) {
			fail("Bad Test, expected exception message is null");
		}
		
		if (curMsg == null) {
			fail("Bad Test or Exception, " + "target exception message is null."
			        + "All drawing exceptions should have a message.");
		}
		
		int expectIndex = expectedExceptionMsg.indexOf(matchTill);
		int targetIndex = curMsg.indexOf(matchTill);
		
		if (expectIndex == -1) {
			fail("Bad Test, expected exception message did not conatin the specified match key phrase");
		}
		
		if (targetIndex == -1) {
			fail("Bad Test or exception, target exception did not conatin the specified match key phrase");
		}
		
		assertEquals("Exception should be for the right reason", expectedExceptionMsg.substring(0, expectIndex),
		    curMsg.subSequence(0, targetIndex));
		
	}
	
	private void checkObsFilename(String obsUuid, Obs tmp) {
		ComplexData cd = tmp.getComplexData();
		
		String[] expectedFilenameComponents = { DrawingConstants.BASE_COMPLEX_OBS_FILENAME.substring(0,
		    DrawingConstants.BASE_COMPLEX_OBS_FILENAME.length() - 4), obsUuid + ".svg" };
		
		String[] actualFilenameComponents = cd.getTitle().split("_");
		
		assertEquals("There should be no extra info in the complex filename", expectedFilenameComponents.length,
		    actualFilenameComponents.length);
		
		for (int i = 0; i < expectedFilenameComponents.length; i++) {
			assertTrue("Filename parts should match",
			    expectedFilenameComponents[i].equalsIgnoreCase(actualFilenameComponents[i]));
			
		}
	}
	
	@Before
	public void setup() {
		
		URL baseTestingPath = OpenmrsClassLoader.getInstance().getResource("");
		
		String testingComplexObsDir = baseTestingPath.getPath() + "complex_obs";
		
		Context.getAdministrationService().setGlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_COMPLEX_OBS_DIR,
		    testingComplexObsDir);
		
		//would it be possible to create a new activator instance and call the start method?
		Context.getService(HtmlFormEntryService.class).addHandler(DrawingConstants.DRAWING_TAG, new DrawingTagHandler());
		
		//this prevents the ModuleFactory.getModuleClassLoader("drawing") call from crashing
		Module fakeDrawingModule = new Module("", "drawing", "", "", "", "1.2.0-SNAPSHOT");
		ModuleFactory.getStartedModulesMap().put("drawing", fakeDrawingModule);
		
		ConceptClass qConceptClass = conceptService.getConceptClassByName("Question");
		
		ConceptDatatype textDataType = conceptService.getConceptDatatypeByUuid(ConceptDatatype.TEXT_UUID);
		
		ConceptDatatype complexDataType = conceptService.getConceptDatatypeByUuid(ConceptDatatype.COMPLEX_UUID);
		
		//create simple test concept
		{
			simpleConcept = new Concept();
			simpleConcept.setUuid(SIMPLE_UUID);
			ConceptName conceptName = new ConceptName("Simple Concept", Locale.ENGLISH);
			simpleConcept.setFullySpecifiedName(conceptName);
			simpleConcept.setPreferredName(conceptName);
			simpleConcept.setConceptClass(qConceptClass);
			simpleConcept.setDatatype(textDataType);
			simpleConcept.addDescription(new ConceptDescription("Simple test concept", Locale.ENGLISH));
			
			conceptService.saveConcept(simpleConcept);
		}
		
		//create complex test concept
		{
			complexConcept = new ConceptComplex();
			complexConcept.setUuid(COMPLEX_UUID);
			//this should be setting the complex concept handler
			complexConcept.setHandler(DrawingHandler.class.getSimpleName());
			ConceptName conceptName = new ConceptName("Complex Concept", Locale.ENGLISH);
			complexConcept.setFullySpecifiedName(conceptName);
			complexConcept.setPreferredName(conceptName);
			complexConcept.setConceptClass(qConceptClass);
			complexConcept.setDatatype(complexDataType);
			complexConcept.addDescription(new ConceptDescription("Complex test concept", Locale.ENGLISH));
			
			conceptService.saveConcept(complexConcept);
			
			complexConceptId = complexConcept.getId();
		}
	}
	
	@Test
	public void NoConceptIdThrowsException() throws Exception {
		
		try {
			new DrawingTestHelper() {
				
				//Setup
				@Override
				public String getFormName() {
					return "noConceptIdForm";
				}
				
				//Replay
			}.run();
			
			//Verify
			
			//if this doesn't throw an exception, fail this test 
			fail();
			
			//if an exception other than IllegalArgumentException is thrown
			//it's probably also a failure
		}
		catch (RuntimeException e) {
			
			//check that this is actually the argument that was invalid
			//a little naive in the way of impl. 
			//but more complicated could be more error prone and less obvious
			assertEquals("RuntimeException should be for the right reason", DrawingConstants.CONCEPT_ID_MISSING_MSG,
			    e.getMessage());
		}
		
	}
	
	@Test
	public void UndefinedConceptIdThrowsException() throws Exception {
		
		try {
			new DrawingTestHelper() {
				
				//Setup
				@Override
				public String getFormName() {
					return "undefinedConceptIdForm";
				}
				//Replay		
			}.run();
			
			//Verify
			//if this doesn't throw an exception, fail this test 
			fail();
			
			//if an exception other than IllegalArgumentException is thrown
			//it's probably also a failure
		}
		catch (IllegalArgumentException e) {
			
			//			//check that this is actually the argument that was invalid
			//			//a little naive in the way of impl. 
			//			//but more complicated could be more error prone and less obvious
			String expectedExceptionMsg = String.format(DrawingConstants.CONCEPT_NOT_FOUND_MSG, UNDEFINED_CONCEPT_UUID,
			    parameters);
			
			compareBuiltExceptionMsg(expectedExceptionMsg, e.getMessage(), "Parameters");
		}
		
	}
	
	@Test
	public void SimpleConceptIdThrowsException() throws Exception {
		
		try {
			new DrawingTestHelper() {
				
				//Setup		
				@Override
				public String getFormName() {
					return "simpleConceptIdForm";
				}
				//Replay						
			}.run();
			
			//Verify	
			//if this doesn't throw an exception, fail this test 
			fail();
			
			//if an exception other than IllegalArgumentException is thrown
			//it's probably also a failure
		}
		catch (IllegalArgumentException e) {
			//check that this is actually the argument that was invalid
			
			assertEquals("Exception should be for the right reason", DrawingConstants.CONCEPT_SIMPLE_MSG, e.getMessage());
		}
		
	}
	
	@Test
	public void ComplexConceptIdDoesNotThrowException() throws Exception {
		
		try {
			new DrawingTestHelper() {
				
				//Setup		
				@Override
				public String getFormName() {
					return "complexConceptIdForm";
				}
				//Replay					
			}.run();
			
			//Verify				
			//if an exception other than IllegalArgumentException is thrown
			//it's probably also a failure
		}
		catch (IllegalArgumentException e) {
			//check that this is actually the argument that was invalid
			
			//if this does throw an exception, and it's about the type of concept 
			//fail this test 
			
			String exceptionMsg = e.getMessage();
			
			if (exceptionMsg != null) {
				assertFalse("Complex Concept Id should not throw concept exception",
				    exceptionMsg.toLowerCase().contains("concept"));
			} else {
				fail("All drawing exceptions should have a message");
			}
		}
		
	}
	
	@Test
	public void MissingDisplayModeThrowsException() throws Exception {
		
		try {
			new DrawingTestHelper() {
				
				//Setup		
				@Override
				public String getFormName() {
					return "missingDisplayModeForm";
				}
				//Replay						
			}.run();
			//Verify				
			//if an exception other than IllegalArgumentException is thrown
			//it's probably also a failure
		}
		catch (IllegalArgumentException e) {
			//check that this is actually the argument that was invalid
			//if this does throw an exception, fail this test 
			
			String expectedExceptionMsg = String.format(DrawingConstants.DISPLAY_MODE_UNK_MSG, "", "");
			
			compareBuiltExceptionMsg(expectedExceptionMsg, e.getMessage(), "Known modes");
			
		}
		
	}
	
	@Test
	public void InvalidDisplayModeThrowsException() throws Exception {
		try {
			
			new DrawingTestHelper() {
				
				//Setup
				@Override
				public String getFormName() {
					return "invalidDisplayModeForm";
				}
				//Replay	
			}.run();
			
			//Verify	
			fail("Invalid/Unknown displayMode should throw exception");
			//if an exception other than IllegalArgumentException is thrown
			//it's probably also a failure
		}
		catch (IllegalArgumentException e) {
			//check that this is actually the argument that was invalid
			//if this does throw an exception, fail this test 
			String expectedExceptionMsg = String.format(DrawingConstants.DISPLAY_MODE_UNK_MSG, "badDisplayMode", "");
			
			compareBuiltExceptionMsg(expectedExceptionMsg, e.getMessage(), "Known modes");
			
		}
		
	}
	
	@Test
	public void SignatureDisplayModeValid() throws Exception {
		try {
			new DrawingTestHelper() {
				
				//Setup		
				@Override
				public String getFormName() {
					return "sigDisplayModeForm";
				}
				//Replay						
			}.run();
			
			//Verify
			
			//if an exception other than IllegalArgumentException is thrown
			//it's probably also a failure
		}
		catch (IllegalArgumentException e) {
			//check that this is actually the argument that was invalid
			//if this does throw an exception, fail this test 
			fail("Signature display mode is valid. An exception should not be thrown: " + e.getMessage());
			
		}
		
	}
	
	@Test
	public void AnnotationDisplayModeValid() throws Exception {
		try {
			new DrawingTestHelper() {
				
				//Setup	
				@Override
				public String getFormName() {
					return "annDisplayModeForm";
				}
				//Replay					
			}.run();
			
			//Verify				
			//if an exception other than IllegalArgumentException is thrown
			//it's probably also a failure
		}
		catch (IllegalArgumentException e) {
			//check that this is actually the argument that was invalid
			//if this does throw an exception, fail this test 
			fail("Annotation display mode is valid. An exception should not be thrown: " + e.getMessage());
			
		}
		
	}
	
	@Test
	public void MissingHeightDimUnitsThrowsException() throws Exception {
		try {
			new DrawingTestHelper() {
				
				//Setup		
				@Override
				public String getFormName() {
					return "missingHeightUnitsForm";
				}
				//Replay					
			}.run();
			//Verify	
			fail("An exception should be thrown: " + DrawingConstants.DIM_MISSING_UNITS_MSG);
			
			//if an exception other than IllegalArgumentException is thrown
			//it's probably also a failure
		}
		catch (IllegalArgumentException e) {
			//check that this is actually the argument that was invalid
			//if this does throw an exception, fail this test 
			
			String expectedExceptionMsg = String.format(DrawingConstants.DIM_MISSING_UNITS_MSG, "height", "height", "");
			
			compareBuiltExceptionMsg(expectedExceptionMsg, e.getMessage(), "height=");
			
		}
	}
	
	@Test
	public void MissingWidthtDimUnitsThrowsException() throws Exception {
		try {
			new DrawingTestHelper() {
				
				//Setup		
				@Override
				public String getFormName() {
					return "missingWidthUnitsForm";
				}
				//Replay					
			}.run();
			
			//Verify	
			fail("An exception should be thrown: " + DrawingConstants.DIM_MISSING_UNITS_MSG);
			
			//if an exception other than IllegalArgumentException is thrown
			//it's probably also a failure
		}
		catch (IllegalArgumentException e) {
			//check that this is actually the argument that was invalid
			//if this does throw an exception, fail this test 
			
			String expectedExceptionMsg = String.format(DrawingConstants.DIM_MISSING_UNITS_MSG, "width", "width", "");
			
			compareBuiltExceptionMsg(expectedExceptionMsg, e.getMessage(), "width=");
			
		}
		
	}
	
	@Test
	public void SaveButtonHiddenDisabled() throws Exception {
		
		try {
			new DrawingTestHelper() {
				
				//Setup		
				@Override
				public String getFormName() {
					return "preloadImageForm";
				}
				
				//Verify		
				@Override
				public void testBlankFormHtml(String html) {
					Document form = null;
					try {
						form = DrawingUtil.convertStringToDocument(html);
					}
					catch (ParserConfigurationException | SAXException | IOException e) {
						e.printStackTrace();
					}
					
					Element saveImageButton = DrawingUtil.getElementById("save-image", form);
					String classes = saveImageButton.getAttribute("class");
					assertTrue("Save SVG button should always have hidden class in HFE forms", classes.contains("hidden"));
					assertTrue("Save SVG button should always have disabled class in HFE forms",
					    classes.contains("disabled"));
				}
				
				//Replay
			}.run();
			
			//if an exception other than IllegalArgumentException is thrown
			//it's probably also a failure
		}
		catch (Exception e) {
			fail("Bad test or other error, Exception should not be thrown");
		}
		
	}
	
	@Test
	public void ExcludeButtonsHiddenDisabled() throws Exception {
		
		try {
			new DrawingTestHelper() {
				
				//Setup
				@Override
				public String getFormName() {
					return "excludeButtonsForm";
				}
				
				//Verify	
				@Override
				public void testBlankFormHtml(String html) {
					Document form = null;
					try {
						form = DrawingUtil.convertStringToDocument(html);
					}
					catch (ParserConfigurationException | SAXException | IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					String[] ids = { "text-tool", "template-button", "load-image-button", "path-tool", "line-tool",
					        "circle-tool", "clear-all", "layer-info-vis-toggle", "edit-tool", "move-tool",
					        "send-to-front-tool", "move-forward-tool", "move-backward-tool", "send-to-back-tool",
					        "delete-tool" };
					
					for (String id : ids) {
						
						Element button = DrawingUtil.getElementById(id, form);
						String classes = button.getAttribute("class");
						
						assertTrue(id + " should have hidden class", classes.contains("hidden"));
						
						assertTrue(id + " should have disabled class", classes.contains("disabled"));
					}
				}
				//Replay					
			}.run();
			
			//if an exception other than IllegalArgumentException is thrown
			//it's probably also a failure
		}
		catch (Exception e) {
			fail("Bad test or other error, Exception should not be thrown");
		}
		
	}
	
	@Test
	public void ToolButtonsNotHiddenDisabledByDefault() throws Exception {
		
		try {
			new DrawingTestHelper() {
				
				//Setup		
				@Override
				public String getFormName() {
					return "preloadImageForm";
				}
				
				//Verify		
				@Override
				public void testBlankFormHtml(String html) {
					Document form = null;
					try {
						form = DrawingUtil.convertStringToDocument(html);
					}
					catch (ParserConfigurationException | SAXException | IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					String[] ids = { "text-tool", "template-button", "load-image-button", "path-tool", "line-tool",
					        "circle-tool", "clear-all", "layer-info-vis-toggle" };
					
					for (String id : ids) {
						
						Element button = DrawingUtil.getElementById(id, form);
						String classes = button.getAttribute("class");
						
						assertFalse(id + " should not have hidden class by default", classes.contains("hidden"));
						
						assertFalse(id + " should not have disabled class by default", classes.contains("disabled"));
					}
				}
				//Replay						
			}.run();
			
			//if an exception other than IllegalArgumentException is thrown
			//it's probably also a failure
		}
		catch (Exception e) {
			fail("Bad test or other error, Exception should not be thrown");
		}
		
	}
	
	@Test
	public void PreloadImageInSVGAddsImageTagAndContent() throws Exception {
		
		try {
			new DrawingTestHelper() {
				
				//Setup		
				@Override
				public String getFormName() {
					return "preloadImageForm";
				}
				
				//Verify		
				@Override
				public void testBlankFormHtml(String html) {
					Document form = null;
					try {
						form = DrawingUtil.convertStringToDocument(html);
					}
					catch (ParserConfigurationException | SAXException | IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
					Element rootSvg = DrawingUtil.getElementById("root-svg", form);
					NodeList children = rootSvg.getChildNodes();
					
					boolean containsImageTag = false;
					boolean hasCorrectBase64Image = false;
					
					for (int i = 0; i < children.getLength(); i++) {
						Node child = children.item(i);
						
						String nodeName = child.getNodeName();
						
						if (nodeName.equals("image")) {
							containsImageTag = true;
							
							NamedNodeMap attr = child.getAttributes();
							
							Node srcAttr = attr.getNamedItem("href");
							
							URL res = OpenmrsClassLoader.getInstance().getResource("web/module/resources/red-dot.png");
							
							File resFile = new File(res.getPath());
							
							try {
								String base64Image = srcAttr.getNodeValue();
								
								if (base64Image.equals(DrawingUtil.imageToBase64(resFile))) {
									hasCorrectBase64Image = true;
								}
							}
							catch (IOException e) {
								e.printStackTrace();
								fail(e.getMessage());
							}
						}
					}
					
					assertTrue("Preloaded image tag should be present in blank form", containsImageTag);
					assertTrue("Preloaded image tag href should have the correct base64 data", hasCorrectBase64Image);
				}
				//Replay						
			}.run();
			
			//if an exception other than IllegalArgumentException is thrown
			//it's probably also a failure
		}
		catch (Exception e) {
			fail("Bad test or other error, Exception should not be thrown");
		}
		
	}
	
	@Test
	public void PreloadImageInSVGStoresContent() throws Exception {
		
		new DrawingTestHelper() {
			
			//Setup
			private Element rootSvg;
			
			private String svgDom;
			
			@Override
			public String getFormName() {
				return "preloadImageForm";
			}
			
			@Override
			public void testBlankFormHtml(String html) {
				Document form = null;
				try {
					form = DrawingUtil.convertStringToDocument(html);
				}
				catch (ParserConfigurationException | SAXException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				rootSvg = DrawingUtil.getElementById("root-svg", form);
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateTodayAsString());
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "1");
				
				svgDom = DrawingUtil.elementToString(rootSvg);
				request.addParameter("svgDOM", svgDom);
			}
			
			//Verify
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(1);
				results.assertLocation(2);
				
				//TODO insure complex Obs value is as expected? (should this be a separate test?)
				results.assertObsCreated(complexConceptId, null);
				Set<Obs> allObs = results.getEncounterCreated().getAllObs();
				
				for (Obs curObs : allObs) {
					
					//this seems unnecessary, but it is the expected procedure
					//based on the comments for getComplexData()
					Obs obs = Context.getObsService().getObs(curObs.getId());
					
					obs.getComplexData().getData().equals(svgDom);
				}
			}
			//Replay		
		}.run();
		
	}
	
	@Test
	public void PreloadImageInSVGStoresContentRetrivableFromDB() throws Exception {
		
		new DrawingTestHelper() {
			
			private Element rootSvg;
			
			private String svgDom;
			
			@Override
			public String getFormName() {
				return "preloadImageForm";
			}
			
			@Override
			public void testBlankFormHtml(String html) {
				Document form = null;
				try {
					form = DrawingUtil.convertStringToDocument(html);
				}
				catch (ParserConfigurationException | SAXException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				rootSvg = DrawingUtil.getElementById("root-svg", form);
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateTodayAsString());
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "1");
				
				svgDom = DrawingUtil.elementToString(rootSvg);
				request.addParameter("svgDOM", svgDom);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(1);
				results.assertLocation(2);
				
				//TODO insure complex Obs value is as expected? (should this be a separate test?)
				results.assertObsCreated(complexConceptId, null);
				Set<Obs> allObs = results.getEncounterCreated().getAllObs();
				
				for (Obs curObs : allObs) {
					
					//this seems unnecessary, but it is the expected procedure
					//based on the comments for getComplexData()
					Obs obs = Context.getObsService().getObs(curObs.getId());
					
					obs.getComplexData().getData().equals(svgDom);
				}
				
				//verifies the data is actually stored in the db
				List<List<Object>> sqlResults = DatabaseUtil.executeSQL(getConnection(),
				    "SELECT value_complex FROM OBS WHERE concept_id=" + complexConcept.getId() + ";", true);
				
				for (List<Object> resultList : sqlResults) {
					for (Object sqlRow : resultList) {
						if (sqlRow != null) {
							
							Obs tmp = new Obs();
							
							tmp.setValueComplex(sqlRow.toString());
							File file = AbstractHandler.getComplexDataFile(tmp);
							
							assertTrue("File should actually exist in filesystem", file.exists());
							
							String fileContent = "";
							try {
								
								fileContent = DrawingUtil.loadResourceServerside(file.getPath());
								
							}
							catch (FileNotFoundException e) {
								e.printStackTrace();
								fail("file path should be valid to load file from file system");
							}
							
							assertTrue("File should not be empty", fileContent.length() > 0);
							
						} else {
							fail("All created complex concepts should have value_complex set in the db");
							
						}
					}
				}
			}
			
		}.run();
		
	}
	
	//	//TODO definition of "Blank" needs to be established, e.g. is 1 pixel a signature?
	//	//Should a given blur and/or histogram be applied to establish "signed"? 
	//	@Test	
	//	public void requiredDetectsBlankImages() throws Exception {	        
	//	        
	//		//try {
	//			new DrawingTestHelper() {
	//				
	//				@Override
	//				public String getFormName() {
	//					return "requiredForm";
	//				}
	//				
	//				@Override
	//				public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
	//					request.addParameter(widgets.get("Date:"), dateAsString(date));
	//					request.addParameter(widgets.get("Location:"), "2");
	//					request.addParameter(widgets.get("Provider:"), "1");
	//				}
	//
	//				@Override
	//				public void testResults(SubmissionResults results) {
	//					List<FormSubmissionError> validationErrors =
	//							results.getValidationErrors();
	//					
	//					final String missingError = "Blank signatures that are required should create a validation error";
	//					
	//					if(validationErrors!=null) {
	//						assertEquals(missingError, 1, validationErrors.size());
	//						//TODO assert this one error is from the drawing handler
	//						//TODO assert this one error is the validation error we expect it to be
	//					} else {
	//						fail(missingError);
	//					}
	//					
	//					results.assertNoEncounterCreated();
	//				}
	//				
	//			}.run();
	//						
	//			//if an exception other than IllegalArgumentException is thrown
	//			//it's probably also a failure
	//		//} catch (Exception e) {
	//		//	fail("Bad test or other error, Exception should not be thrown: "+e.getMessage());
	//		//}
	//		
	//	}
	
	@Test
	public void ViewLoadsPreviousObs() throws Exception {
		
		new DrawingTestHelper() {
			
			private Element rootSvg;
			
			@Override
			public String getFormName() {
				return "preloadImageForm";
			}
			
			@Override
			public boolean doViewEncounter() {
				return true;
			}
			
			@Override
			public void testBlankFormHtml(String html) {
				Document form = null;
				try {
					form = DrawingUtil.convertStringToDocument(html);
				}
				catch (ParserConfigurationException | SAXException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				rootSvg = DrawingUtil.getElementById("root-svg", form);
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateTodayAsString());
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "1");
				
				String svgDom = DrawingUtil.elementToString(rootSvg);
				request.addParameter("svgDOM", svgDom);
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				Document form = null;
				try {
					form = DrawingUtil.convertStringToDocument(html);
				}
				catch (ParserConfigurationException | SAXException | IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				Element rootSvg = DrawingUtil.getElementById("root-svg", form);
				NodeList children = rootSvg.getChildNodes();
				
				boolean containsImageTag = false;
				boolean hasCorrectBase64Image = false;
				
				for (int i = 0; i < children.getLength(); i++) {
					Node child = children.item(i);
					
					String nodeName = child.getNodeName();
					
					if (nodeName.equals("image")) {
						containsImageTag = true;
						
						NamedNodeMap attr = child.getAttributes();
						
						Node srcAttr = attr.getNamedItem("href");
						
						URL res = OpenmrsClassLoader.getInstance().getResource("web/module/resources/red-dot.png");
						
						File resFile = new File(res.getPath());
						
						try {
							String base64Image = srcAttr.getNodeValue();
							
							if (base64Image.equals(DrawingUtil.imageToBase64(resFile))) {
								hasCorrectBase64Image = true;
							}
						}
						catch (IOException e) {
							e.printStackTrace();
							fail(e.getMessage());
						}
					}
				}
				
				assertTrue("View should still contain image loaded and saved in ENTER mode", containsImageTag);
				assertTrue("View should still contain same preloaded image", hasCorrectBase64Image);
				
			}
			
		}.run();
		
	}
	
	@Test
	public void EditLoadsPreviousObs() throws Exception {
		
		new DrawingTestHelper() {
			
			//store the rootSvg that's supplied by the tag handler in HFE ENTER mode
			private Element enterRootSvg;
			
			@Override
			public String getFormName() {
				return "preloadImageForm";
			}
			
			//setup for edit tests, store the supplied svg to use as a param in the submission step for ENTER
			@Override
			public void testBlankFormHtml(String html) {
				Document form = null;
				try {
					form = DrawingUtil.convertStringToDocument(html);
				}
				catch (ParserConfigurationException | SAXException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				enterRootSvg = DrawingUtil.getElementById("root-svg", form);
			}
			
			//submit the provided SVG
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateTodayAsString());
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "1");
				
				String svgDom = DrawingUtil.elementToString(enterRootSvg);
				request.addParameter("svgDOM", svgDom);
			}
			
			//Really testing the HFE EDIT behavior
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			//the html created by the tag handler/submission element for EDIT mode
			@Override
			public void testEditFormHtml(String html) {
				Document form = null;
				try {
					form = DrawingUtil.convertStringToDocument(html);
				}
				catch (ParserConfigurationException | SAXException | IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				Element editRootSvg = DrawingUtil.getElementById("root-svg", form);
				NodeList children = editRootSvg.getChildNodes();
				
				boolean containsImageTag = false;
				boolean hasCorrectBase64Image = false;
				
				for (int i = 0; i < children.getLength(); i++) {
					Node child = children.item(i);
					
					String nodeName = child.getNodeName();
					
					if (nodeName.equals("image")) {
						containsImageTag = true;
						
						NamedNodeMap attr = child.getAttributes();
						
						Node srcAttr = attr.getNamedItem("href");
						
						URL res = OpenmrsClassLoader.getInstance().getResource("web/module/resources/red-dot.png");
						
						File resFile = new File(res.getPath());
						
						try {
							String base64Image = srcAttr.getNodeValue();
							
							if (base64Image.equals(DrawingUtil.imageToBase64(resFile))) {
								//this should not load the image twice!
								assertFalse(
								    "The VIEW/EDIT codepath should not add any preload images, the existing file should already contain it",
								    hasCorrectBase64Image);
								
								hasCorrectBase64Image = true;
							}
						}
						catch (IOException e) {
							e.printStackTrace();
							fail(e.getMessage());
						}
					}
				}
				
				assertTrue("View should still contain image loaded and saved in ENTER mode", containsImageTag);
				assertTrue("View should still contain same preloaded image", hasCorrectBase64Image);
				
			}
			
		}.run();
	}
	
	@Test
	public void EditChangesPreviousObs() throws Exception {
		
		new DrawingTestHelper() {
			
			private Element enterRootSvg;
			
			private Element editedRootSvg;
			
			private String prevBase64Image;
			
			private String replacementBase64Image;
			
			@Override
			public String getFormName() {
				return "preloadImageForm";
			}
			
			//setup for edit tests, store the supplied svg to use as a param in the submission step for ENTER
			@Override
			public void testBlankFormHtml(String html) {
				Document form = null;
				try {
					form = DrawingUtil.convertStringToDocument(html);
				}
				catch (ParserConfigurationException | SAXException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				enterRootSvg = DrawingUtil.getElementById("root-svg", form);
			}
			
			//submit the provided SVG
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				
				request.addParameter(widgets.get("Date:"), dateTodayAsString());
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "1");
				
				String svgDom = DrawingUtil.elementToString(enterRootSvg);
				request.addParameter("svgDOM", svgDom);
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public void testEditFormHtml(String html) {
				Document form = null;
				try {
					form = DrawingUtil.convertStringToDocument(html);
				}
				catch (ParserConfigurationException | SAXException | IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				Element editRootSvg = DrawingUtil.getElementById("root-svg", form);
				NodeList children = editRootSvg.getChildNodes();
				
				boolean containsImageTag = false;
				boolean hasCorrectBase64Image = false;
				
				for (int i = 0; i < children.getLength(); i++) {
					Node child = children.item(i);
					
					String nodeName = child.getNodeName();
					
					if (nodeName.equals("image")) {
						containsImageTag = true;
						
						NamedNodeMap attr = child.getAttributes();
						
						Node srcAttr = attr.getNamedItem("href");
						
						URL res = OpenmrsClassLoader.getInstance().getResource("web/module/resources/red-dot.png");
						
						File resFile = new File(res.getPath());
						
						try {
							
							prevBase64Image = DrawingUtil.imageToBase64(resFile);
							
							String base64Image = srcAttr.getNodeValue();
							
							if (base64Image.equals(prevBase64Image)) {
								hasCorrectBase64Image = true;
							}
						}
						catch (IOException e) {
							e.printStackTrace();
							fail(e.getMessage());
						}
					}
				}
				
				assertTrue("View should still contain image loaded and saved in ENTER mode", containsImageTag);
				assertTrue("View should still contain same preloaded image", hasCorrectBase64Image);
				
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				//set minimum HFE required parameters
				request.addParameter(widgets.get("Date:"), dateTodayAsString());
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "1");
				
				//load a new replacement image
				URL res = OpenmrsClassLoader.getInstance().getResource("web/module/resources/blue-dot.png");
				
				File resFile = new File(res.getPath());
				
				replacementBase64Image = null;
				try {
					replacementBase64Image = DrawingUtil.imageToBase64(resFile);
				}
				catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				//modify the DOM with a new image submit the new DOM
				editedRootSvg = (Element) enterRootSvg.cloneNode(true);
				NodeList images = editedRootSvg.getElementsByTagName("image");
				Element image = (Element) images.item(0);
				
				image.setAttribute("href", replacementBase64Image);
				
				String elemStr = DrawingUtil.elementToString(editedRootSvg);
				
				//update svgDOM instead of adding a duplicate
				request.setParameter("svgDOM", elemStr);
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				Encounter editEncounter = results.getEncounterCreated();
				
				if (editEncounter == null) {
					fail("There were validation errors: " + results.getValidationErrors().toString());
				}
				
				//get all obs including voided obs to validate there are new
				//obs and the previous voided obs still exist
				Set<Obs> allObsWithVoided = editEncounter.getAllObs(true);
				
				Map<Obs, Obs> voidedObsMap = new HashMap<Obs, Obs>();
				Map<Obs, Obs> newObsMap = new HashMap<Obs, Obs>();
				
				for (Obs obs : allObsWithVoided) {
					
					Obs tmp = Context.getObsService().getObs(obs.getId());
					
					checkObsFilename(obs.getUuid(), tmp);
					
					if (obs.getVoided()) {
						//if the voided obs has already been seen 
						if (!voidedObsMap.containsKey(obs)) {
							voidedObsMap.put(obs, null);
						}
					} else {
						Obs voidedObs = obs.getPreviousVersion();
						assertNotEquals("New observations should store previous observations as previous version", voidedObs,
						    (Obs) null);
						newObsMap.put(obs, voidedObs);
						voidedObsMap.put(voidedObs, obs);
					}
				}
				
				for (Obs voidedObs : voidedObsMap.keySet()) {
					assertTrue("All voided obs should be prevObs in encounter", newObsMap.values().contains(voidedObs));
				}
				//the previousVersion is one way...
				//assertEquals("All new obs should be in encounter", voidedObsMap.values(), newObsMap.keySet());
				
				byte[] prevImage = prevBase64Image.getBytes();
				for (Obs voidedObs : voidedObsMap.keySet()) {
					byte[] obsData = (byte[]) voidedObs.getComplexData().getData();
					
					Document svgDoc = null;
					try {
						svgDoc = DrawingUtil.convertStringToDocument(new String(obsData));
					}
					catch (ParserConfigurationException | SAXException | IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					NodeList images = svgDoc.getElementsByTagName("image");
					
					Element image = (Element) images.item(0);
					byte[] storedPrevImage = image.getAttribute("href").getBytes();
					
					boolean arraysDataEqual = Arrays.equals(prevImage, storedPrevImage);
					assertTrue("Previous obs complex data should have old image", arraysDataEqual);
				}
				
				byte[] newImage = replacementBase64Image.getBytes();
				
				for (Obs curObs : newObsMap.keySet()) {
					//this seems unnecessary, but according to the comments
					//for getComplexData(), unless an obs is "gotten" this way
					//complexData may be null
					Obs newObs = Context.getObsService().getObs(curObs.getObsId());
					
					byte[] obsData = (byte[]) newObs.getComplexData().getData();
					
					Document svgDoc = null;
					try {
						svgDoc = DrawingUtil.convertStringToDocument(new String(obsData));
					}
					catch (ParserConfigurationException | SAXException | IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					NodeList images = svgDoc.getElementsByTagName("image");
					
					Element image = (Element) images.item(0);
					byte[] storedNewImage = image.getAttribute("href").getBytes();
					
					boolean arraysDataEqual = Arrays.equals(newImage, storedNewImage);
					assertTrue("New obs complex data should have new image", arraysDataEqual);
				}
				
			}
			
		}.run();
	}
	
	@Test
	public void EditWithNoChangesDoesNotModifyObs() throws Exception {
		
		new DrawingTestHelper() {
			
			private Element enterRootSvg;
			
			private String prevBase64Image;
			
			private String svgDom;
			
			@Override
			public String getFormName() {
				return "preloadImageForm";
			}
			
			//setup for edit tests, store the supplied svg to use as a param in the submission step for ENTER
			@Override
			public void testBlankFormHtml(String html) {
				Document form = null;
				try {
					form = DrawingUtil.convertStringToDocument(html);
				}
				catch (ParserConfigurationException | SAXException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				enterRootSvg = DrawingUtil.getElementById("root-svg", form);
				svgDom = DrawingUtil.elementToString(enterRootSvg).trim();
			}
			
			//submit the provided SVG
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				
				request.addParameter(widgets.get("Date:"), dateTodayAsString());
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "1");
				
				request.addParameter("svgDOM", svgDom);
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				
				//verify the file was created
				results.assertEncounterCreated();
				results.assertObsCreatedCount(1);
				Encounter enc = results.getEncounterCreated();
				
				Set<Obs> allObs = enc.getAllObs();
				
				for (Obs obs : allObs) {
					assertTrue("Obs should be saved with the test complex concept",
					    obs.getConcept().getId() == complexConceptId);
					
					Obs tmp = Context.getObsService().getObs(obs.getId());
					
					byte[] storedData = (byte[]) tmp.getComplexData().getData();
					byte[] svgData = svgDom.getBytes();
					assertTrue("Data should match the submitted form value", Arrays.equals(svgData, storedData));
				}
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public void testEditFormHtml(String html) {
				Document form = null;
				try {
					form = DrawingUtil.convertStringToDocument(html);
				}
				catch (ParserConfigurationException | SAXException | IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				Element editRootSvg = DrawingUtil.getElementById("root-svg", form);
				NodeList children = editRootSvg.getChildNodes();
				
				boolean containsImageTag = false;
				boolean hasCorrectBase64Image = false;
				
				for (int i = 0; i < children.getLength(); i++) {
					Node child = children.item(i);
					
					String nodeName = child.getNodeName();
					
					if (nodeName.equals("image")) {
						containsImageTag = true;
						
						NamedNodeMap attr = child.getAttributes();
						
						Node srcAttr = attr.getNamedItem("href");
						
						URL res = OpenmrsClassLoader.getInstance().getResource("web/module/resources/red-dot.png");
						
						File resFile = new File(res.getPath());
						
						try {
							
							prevBase64Image = DrawingUtil.imageToBase64(resFile);
							
							String base64Image = srcAttr.getNodeValue();
							
							if (base64Image.equals(prevBase64Image)) {
								hasCorrectBase64Image = true;
							}
						}
						catch (IOException e) {
							e.printStackTrace();
							fail(e.getMessage());
						}
					}
				}
				
				assertTrue("View should still contain image loaded and saved in ENTER mode", containsImageTag);
				assertTrue("View should still contain same preloaded image", hasCorrectBase64Image);
				
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				//set minimum HFE required paramters 
				request.addParameter(widgets.get("Date:"), dateTodayAsString());
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "1");
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				Encounter editEncounter = results.getEncounterCreated();
				
				if (editEncounter == null) {
					fail("There were validation errors: " + results.getValidationErrors().toString());
				}
				
				results.assertObsCreatedCount(1);
				
				for (Obs obs : editEncounter.getAllObs()) {
					
					Obs tmp = Context.getObsService().getObs(obs.getId());
					
					checkObsFilename(obs.getUuid(), tmp);
					
					ComplexData cd = tmp.getComplexData();
					
					String data = new String((byte[]) cd.getData());
					
					//this isn't really what this test is intended to check, but refer to
					//the code following it for explanation of why it doesn't currently work
					assertTrue("Complex Data should be identical", data.equals(svgDom));
					
					//List<List<Object>> sqlResults = DatabaseUtil.executeSQL(getConnection(), "SELECT obs_id FROM obs WHERE concept_id='"+complexConceptId+"';", true);
					
					//because of the dirty flag the file is rewritten, can't check fs file time
					//when a handler handles a getObs()
					//it calls setComplexData() which sets the dirty flag
					
					//System.out.println(sqlResults);
					//assertEquals("There should be only one matching observation in the db but there were "+ sqlResults.toString(),
					//		1, sqlResults.size());
					
				}
			}
			
		}.run();
	}
}
