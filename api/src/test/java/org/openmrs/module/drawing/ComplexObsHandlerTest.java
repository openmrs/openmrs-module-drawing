package org.openmrs.module.drawing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.Obs;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.drawing.obs.handler.DrawingHandler;
import org.openmrs.obs.ComplexData;
import org.openmrs.obs.ComplexObsHandler;
import org.openmrs.obs.handler.AbstractHandler;
import org.openmrs.util.OpenmrsUtil;
import org.openmrs.web.WebConstants;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

//based on https://github.com/openmrs/openmrs-core/blob/master/api/src/test/java/org/openmrs/obs/ImageHandlerTest.java
@RunWith(PowerMockRunner.class)
@PrepareForTest({ AbstractHandler.class, OpenmrsUtil.class, Context.class })
public class ComplexObsHandlerTest {
	
	@Mock
	private AdministrationService administrationService;
	
	@Rule
	public TemporaryFolder complexObsTestFolder = new TemporaryFolder();
	
	private static final String mimetype = "image/svg+xml";
	
	private static String complexObsPath = null;
	
	private ComplexData createComplexData() throws FileNotFoundException {
		String filename = "TestingComplexObsSaving.svg";
		String sourceFilePath = "src" + File.separator + "test" + File.separator + "resources" + File.separator
		        + "ComplexObsTestImage.svg";
		
		byte[] svgData = DrawingUtil.loadResourceServerside(sourceFilePath).getBytes();
		
		return new ComplexData(filename, svgData);
	}
	
	@Before
	public void setup() throws Exception {
		// Mocked methods
		mockStatic(Context.class);
		when(Context.getAdministrationService()).thenReturn(administrationService);
		
		complexObsPath = complexObsTestFolder.newFolder().getAbsolutePath();
		
		when(administrationService.getGlobalProperty(any())).thenReturn(complexObsPath);
	}
	
	@Test
	public void supportsView_shouldSupportRawView() throws Exception {
		//Setup
		DrawingHandler handler = new DrawingHandler();
		
		//Replay
		boolean supported = handler.supportsView(ComplexObsHandler.RAW_VIEW);
		
		//Verify
		assertTrue("DrawingHandler should always support RAW_VIEW", supported);
	}
	
	@Test
	public void saveObs_shouldCreateComplexObs() throws Exception {
		//Setup
		DrawingHandler handler = new DrawingHandler();
		
		ComplexData cd = createComplexData();
		
		Obs tmp = new Obs();
		tmp.setComplexData(cd);
		
		//Replay
		handler.saveObs(tmp);
		
		//Verify
		File obsFile = AbstractHandler.getComplexDataFile(tmp);
		
		assertTrue("Obs file should be created", obsFile.exists());
	}
	
	@Test
	public void saveObs_shouldStoreData() throws Exception {
		//Setup
		DrawingHandler handler = new DrawingHandler();
		
		ComplexData cd = createComplexData();
		
		Obs tmp = new Obs();
		tmp.setComplexData(cd);
		
		//Replay
		handler.saveObs(tmp);
		
		//Verify
		File obsFile = AbstractHandler.getComplexDataFile(tmp);
		
		assertTrue("Obs file should be created", obsFile.exists());
		assertNotEquals("Data should be written when saving", 0, obsFile.length());
	}
	
	@Test
	public void saveObs_shouldNotThrowExceptionWhenComplexDataIsNull() throws Exception {
		//Setup
		DrawingHandler handler = new DrawingHandler();
		
		Obs tmp = new Obs();
		
		//Replay
		handler.saveObs(tmp);
		
		//Verify
	}
	
	@Test
	public void saveObs_shouldNotThrowExceptionWhenComplexDataIsNotByteArray() throws Exception {
		//Setup
		DrawingHandler handler = new DrawingHandler();
		
		Obs tmp = new Obs();
		
		ComplexData cd = new ComplexData("filename", "text data as a string");
		tmp.setComplexData(cd);
		
		//Replay
		handler.saveObs(tmp);
		
		//Verify
	}
	
	@Test
	public void getObs_shouldReturnByteArrayForRawView() throws Exception {
		//Setup
		DrawingHandler handler = new DrawingHandler();
		
		ComplexData cd = createComplexData();
		
		Obs tmp = new Obs();
		tmp.setComplexData(cd);
		
		handler.saveObs(tmp);
		
		//Replay
		handler.getObs(tmp, ComplexObsHandler.RAW_VIEW);
		
		//Verify
		assertEquals("RAW_VIEW should return byte []", byte[].class, tmp.getComplexData().getData().getClass());
	}
	
	@Test
	public void supportsView_shouldSupportTextView() throws Exception {
		//Setup
		DrawingHandler handler = new DrawingHandler();
		
		//Replay
		boolean supported = handler.supportsView(ComplexObsHandler.TEXT_VIEW);
		
		//Verify
		assertTrue("DrawingHandler should always support TEXT_VIEW", supported);
	}
	
	@Test
	public void getObs_shouldReturnStringForTextView() throws Exception {
		//Setup
		DrawingHandler handler = new DrawingHandler();
		
		ComplexData cd = createComplexData();
		
		Obs tmp = new Obs();
		tmp.setComplexData(cd);
		
		handler.saveObs(tmp);
		
		//Replay
		handler.getObs(tmp, ComplexObsHandler.TEXT_VIEW);
		
		//Verify
		assertEquals("TEXT_VIEW should return String", String.class, tmp.getComplexData().getData().getClass());
	}
	
	@Test
	public void supportsView_shouldSupportUriView() throws Exception {
		//Setup
		DrawingHandler handler = new DrawingHandler();
		
		//Replay
		boolean supported = handler.supportsView(ComplexObsHandler.URI_VIEW);
		
		//Verify
		assertTrue("DrawingHandler should support URI_VIEW", supported);
	}
	
	@Test
	public void getObs_shouldReturnURIForUriView() throws Exception {
		//Setup
		DrawingHandler handler = new DrawingHandler();
		
		ComplexData cd = createComplexData();
		
		Obs tmp = new Obs();
		tmp.setComplexData(cd);
		
		handler.saveObs(tmp);
		
		//Replay
		handler.getObs(tmp, ComplexObsHandler.URI_VIEW);
		
		//Verify
		assertEquals("URI_VIEW should return String", String.class, tmp.getComplexData().getData().getClass());
		
		String url = "/" + WebConstants.WEBAPP_NAME + "/module/drawing/manage.form?obsId=" + tmp.getId();
		
		assertTrue("URI_VIEW should be URI to editor loading this Obs",
		    url.equalsIgnoreCase((String) tmp.getComplexData().getData()));
	}
	
	@Test
	public void supportsView_shouldSupportHtmlView() throws Exception {
		//Setup
		DrawingHandler handler = new DrawingHandler();
		
		//Replay
		boolean supported = handler.supportsView(ComplexObsHandler.HTML_VIEW);
		
		//Verify
		assertTrue("DrawingHandler should support HTML_VIEW", supported);
	}
	
	@Test
	public void getObs_shouldReturnLinkMarkupForHtmlView() throws Exception {
		//Setup
		DrawingHandler handler = new DrawingHandler();
		
		ComplexData cd = createComplexData();
		
		Obs tmp = new Obs();
		tmp.setComplexData(cd);
		
		handler.saveObs(tmp);
		
		//Replay
		handler.getObs(tmp, ComplexObsHandler.HTML_VIEW);
		
		//Verify
		Object data = tmp.getComplexData().getData();
		
		assertEquals("HTML_VIEW should return String", String.class, data.getClass());
		
		String dataString = (String) data;
		String url = "/" + WebConstants.WEBAPP_NAME + "/module/drawing/manage.form?obsId=" + tmp.getId();
		
		assertTrue("HTML_VIEW should provide html markup link", dataString.toLowerCase().contains("<a"));
		
		assertTrue("HTML_VIEW should link to drawing editor", dataString.contains(url));
		
	}
	
	@Test
	public void getObs_shouldRetrieveCorrectMimetype() throws Exception {
		//Setup	
		ComplexData complexData = createComplexData();
		
		// Construct 2 Obs to also cover the case where the filename exists already
		Obs obs1 = new Obs();
		obs1.setComplexData(complexData);
		
		Obs obs2 = new Obs();
		obs2.setComplexData(complexData);
		
		DrawingHandler handler = new DrawingHandler();
		
		// Execute save
		handler.saveObs(obs1);
		handler.saveObs(obs2);
		
		//Replay
		// Get observation
		Obs complexObs1 = handler.getObs(obs1, "RAW_VIEW");
		Obs complexObs2 = handler.getObs(obs2, "RAW_VIEW");
		
		//Verify
		assertEquals(complexObs1.getComplexData().getMimeType(), mimetype);
		assertEquals(complexObs2.getComplexData().getMimeType(), mimetype);
	}
	
}
