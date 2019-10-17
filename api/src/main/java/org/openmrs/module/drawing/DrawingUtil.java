/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.drawing;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.ModuleClassLoader;
import org.openmrs.module.ModuleFactory;
import org.openmrs.util.OpenmrsClassLoader;
import org.openmrs.util.OpenmrsUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Drawing utility methods
 */
public class DrawingUtil {
	
	private static MessageSourceService mss = Context.getMessageSourceService();
	
	private static Log log = LogFactory.getLog(DrawingUtil.class);
	
	public enum ErrorStatus {
		NONE,
		INTERNAL_ERROR,
		FILE_ERROR,
		FORMAT_ERROR,
		NULL_DOC
	};
	
	/**
	 * @param an image
	 * @return String containing Base64 encoded image/null if there is a problem in encoding
	 * @throws IOException
	 */
	public static String imageToBase64(BufferedImage img) throws IOException {
		
		return imageToBase64(img, null);
	}
	
	public static String loadResourceServerside(String path) throws FileNotFoundException {
		File doc = new File(path);
		Scanner reader;
		String markup = "";
		
		reader = new Scanner(doc);
		
		//read the entire file
		reader.useDelimiter("\\Z");
		markup = reader.next();
		
		reader.close();
		
		return markup;
	}
	
	public static Element getElementById(String id, Document doc) {
		XPath path = XPathFactory.newInstance().newXPath();
		
		Element elem = null;
		//should only be one with given id
		try {
			elem = (Element) path.evaluate("//*[@id='" + id + "']", doc, XPathConstants.NODE);
		}
		catch (XPathExpressionException e) {
			e.printStackTrace();
		}
		
		return elem;
	}
	
	public static Document loadEditorMarkup(String path) throws ParserConfigurationException, SAXException, IOException {
		
		String markup = loadResourceServerside(path);
		
		//if the resource isnt found the markup string is ""
		if (markup.equals("")) {
			return null;
		}
		
		//wrap in a single root tag for builder parsing
		markup = "<body>" + markup + "</body>";
		
		//provide an interface builder.parse can read from
		InputStream markupIS = null;
		
		markupIS = new ByteArrayInputStream(markup.getBytes("UTF-8"));
		
		Document parsedDoc = null;
		
		if (markupIS != null) {
			
			DocumentBuilder builder = null;
			
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			builder = factory.newDocumentBuilder();
			parsedDoc = builder.parse(markupIS);
		}
		
		return parsedDoc;
	}
	
	public static String bodyChildrenToString(Document xmldoc) {
		
		StringWriter sw = new StringWriter();
		NodeList children = xmldoc.getFirstChild().getChildNodes();
		
		Transformer transformer = null;
		
		try {
			transformer = TransformerFactory.newInstance().newTransformer();
			
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF8");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.METHOD, "html");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		}
		catch (TransformerConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		catch (TransformerFactoryConfigurationError e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		if (transformer != null) {
			for (int i = 0; i < children.getLength(); i++) {
				DOMSource childDOM = new DOMSource(children.item(i));
				
				try {
					transformer.transform(childDOM, new StreamResult(sw));
				}
				catch (TransformerException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}
		return sw.getBuffer().toString();
	}
	
	public static String documentToString(Document xmldoc) {
		
		StringWriter sw = new StringWriter();
		
		Transformer transformer;
		
		try {
			transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF8");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.transform(new DOMSource(xmldoc), new StreamResult(sw));
			
		}
		catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (TransformerFactoryConfigurationError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return sw.getBuffer().toString();
		
	}
	
	public static String elementToString(Element elem) {
		
		StringWriter sw = new StringWriter();
		
		Transformer transformer;
		
		try {
			transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF8");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.transform(new DOMSource(elem), new StreamResult(sw));
			
		}
		catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (TransformerFactoryConfigurationError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return sw.getBuffer().toString();
		
	}
	
	public static Document convertStringToDocument(String markup)
	        throws UnsupportedEncodingException, ParserConfigurationException, SAXException, IOException {
		
		/*String parser = XMLResourceDescriptor.getXMLParserClassName();
		SAXSVGDocumentFactory docFactory = new SAXSVGDocumentFactory(parser);*/
		
		Document parsedSvgDoc = null;
		
		//StringReader svgReader = new StringReader(markup);
		InputStream svgMarkupIS = null;
		
		svgMarkupIS = new ByteArrayInputStream(markup.getBytes("UTF-8"));
		
		//try to create an XML/SVG document from the markup string 
		//SVGDoc = docFactory.createDocument("SVG", SVGReader);
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		//factory.set
		DocumentBuilder builder = factory.newDocumentBuilder();
		parsedSvgDoc = builder.parse(svgMarkupIS);
		
		return parsedSvgDoc;
	}
	
	/**
	 * @param an image
	 * @param String containing the extension of image
	 * @return String containing Base64 encoded image/null if there is a problem in encoding
	 * @throws IOException
	 */
	public static String imageToBase64(BufferedImage img, String extension) throws IOException {
		String encodedImage = null;
		if (StringUtils.isBlank(extension))
			extension = "png";
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ImageIO.write(img, extension, baos);
			encodedImage = "data:image/" + extension.toLowerCase() + ";base64,"
			        + Base64.encodeBase64String(baos.toByteArray());
		}
		catch (Exception e) {
			log.error("cannot write image", e);
		}
		finally {
			baos.close();
		}
		return encodedImage;
	}
	
	/**
	 * @param an image
	 * @param String containing the extension of image
	 * @return String containing Base64 encoded image/null if there is a problem in encoding
	 * @throws IOException
	 */
	public static String imageToBase64(File file) throws IOException {
		
		String extension = getExtension(file.getName());
		if (isImage(file.getName()) && !extension.equals("raw")) {
			BufferedImage bi = ImageIO.read(file);
			return imageToBase64(bi, extension);
		}
		
		return null;
		
	}
	
	/**
	 * Converts a Base64 String to image
	 * 
	 * @param base64Data
	 * @return image
	 * @throws IOException
	 */
	
	public static BufferedImage base64ToImage(String base64Data) throws IOException {
		
		if (StringUtils.isBlank(base64Data) || !base64Data.contains(",") || !(base64Data.split(",")[1].length() % 4 == 0)) {
			log.error("String is not Base64 encoded");
			return null;
		}
		byte[] raw = Base64.decodeBase64(base64Data.split(",")[1]);
		ByteArrayInputStream bs = new ByteArrayInputStream(raw);
		BufferedImage img = null;
		try {
			img = ImageIO.read(bs);
		}
		catch (IOException e) {
			log.error("Unable to convert Base64 String to image", e);
		}
		finally {
			bs.close();
		}
		
		return img;
		
	}
	/**
	 * @param request
	 * @param id
	 * @return array of annotations
	 */
	
	//TODO support loading legacy 1.1 image and XML and resaving as SVG
	/*public static ImageAnnotation[] getAnnotations(HttpServletRequest request, String id) {
		if (id == null)
			id = "";
		ArrayList<ImageAnnotation> annotations = new ArrayList<ImageAnnotation>();
		if (StringUtils.isNotBlank(request.getParameter("annotationCounter" + id))) {
			int annotationsCount = Integer.parseInt(request.getParameter("annotationCounter" + id));
			for (int i = 0; i < annotationsCount; i++) {
				String s = request.getParameter("annotation" + id + i);
				String[] data = s.split("\\|");
				if (data == null || data.length < 5) {
					log.error("incorrect annotation format");
					continue;
				}
				annotations.add(new ImageAnnotation(Integer.parseInt(data[0]), new Position(Integer.parseInt(data[1]),
				        Integer.parseInt(data[2])), data[3], new Date(), Context.getAuthenticatedUser(), data[4]));
			}
		}
		return annotations.toArray(new ImageAnnotation[0]);
	}*/
	
	/**
	 * @return the directory specified in {@link DrawingConstants#DRAWINGDIRECTORY}
	 */
	public static File getDrawingDirectory() {
		File file = OpenmrsUtil.getDirectoryInApplicationDataDirectory(DrawingConstants.DRAWINGDIRECTORY);
		if (!file.exists() || file.list(new ExtensionFilter()).length == 0) {
			loadDefaultTemplates(file);
			return file;
		} else
			return file;
	}
	
	private static void loadDefaultTemplates(File file) {
		try {
			String s = OpenmrsClassLoader.getInstance().getResource(DrawingConstants.DRAWINGDIRECTORY).getPath();
			File f = new File(s);
			FileUtils.copyDirectory(f, file);
		}
		catch (Exception e) {
			log.error("Unable to copy templates from resources to app directory", e);
		}
		
	}
	
	public static String getServerResourcesPath(String resourceName) {
		String resourcePath = null;
		
		try {
			
			//this works when running, it should be tested when testing, but falls back
			//to the secondary OpenmrsClassLoader method
			ModuleClassLoader drawingModule = ModuleFactory.getModuleClassLoader("drawing");
			
			resourcePath = drawingModule.getResource(resourceName).getPath();
			
		}
		catch (NullPointerException e) {
			
			try {
				//this method loads resources from other modules
				//and works during unit testing
				resourcePath = OpenmrsClassLoader.getInstance().getResource(resourceName).getPath();
			}
			catch (Exception e2) {
				RuntimeException moreInfo = new RuntimeException("Unable to find resource: " + resourceName);
				
				moreInfo.addSuppressed(e2);
				
				throw moreInfo;
				
			}
		}
		
		return resourcePath;
	}
	
	/**
	 * encodes the of the contents of file using Base64 encoder
	 * 
	 * @param filename
	 * @return String containing Base64 encoding of file. if file does not exist ,null
	 */
	public static String getTemplateAsBase64ByName(String name) throws IOException {
		File file = new File(getDrawingDirectory(), name);
		if (!file.exists()) {
			log.error("File does not exist");
			return null;
		} else
			return imageToBase64(file);
		
	}
	
	/**
	 * Checks if the file has one of the extensions defined in
	 * {@link DrawingConstants#ACCEPTDEXTENSIONS}
	 * 
	 * @param filename
	 * @return boolean
	 */
	public static boolean isImage(String fileName) {
		String extension = getExtension(fileName);
		for (String s : DrawingConstants.ACCEPTDEXTENSIONS)
			if (extension.toUpperCase().equals(s))
				return true;
			
		return false;
	}
	
	/**
	 * @param filename
	 * @return the extension of file otherwise "raw"
	 */
	public static String getExtension(String filename) {
		String[] filenameParts = filename.split("\\.");
		
		log.debug("titles length: " + filenameParts.length);
		
		String extension = (filenameParts.length < 2) ? filenameParts[0] : filenameParts[filenameParts.length - 1];
		extension = (null != extension && !"".equals(extension)) ? extension : "raw";
		
		return extension;
	}
	
	/**
	 * @return Names off all the files with extensions defined in
	 *         {@link DrawingConstants#ACCEPTDEXTENSIONS}
	 */
	public static String[] getAllTemplateNames() {
		File dir = getDrawingDirectory();
		return dir.list(new ExtensionFilter());
	}
	
	/**
	 * @param name of the file
	 * @param Image to be saved
	 * @return true if file is saved other wise false
	 */
	public static Boolean saveFile(String name, BufferedImage bi) {
		File f = new File(getDrawingDirectory(), name);
		Boolean saved = false;
		try {
			saved = ImageIO.write(bi, getExtension(name), f);
		}
		catch (IOException e) {
			log.error("Unable to Save File", e);
		}
		return saved;
	}
	
	/**
	 * @param name of the file to be deleted
	 * @return returns true if file is deleted else false
	 */
	public static Boolean deleteTemplate(String name) {
		File f = new File(getDrawingDirectory(), name);
		Boolean deleted = f.exists() ? f.delete() : true;
		return deleted;
	}
	
	public static String translateLanguageKey(String key) {
		return mss.getMessage(key);
	}
	
}
