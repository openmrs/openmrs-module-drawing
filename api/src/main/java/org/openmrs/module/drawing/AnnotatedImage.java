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

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.drawing.DrawingUtil.ErrorStatus;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 *
 */
public class AnnotatedImage {
	
	private Log log = LogFactory.getLog(AnnotatedImage.class);
	//Rendering on the server will require something like apache batik, 
	//which appeared to have some issue around xalan/xerces in previous trials
	//private BufferedImage renderedImage;
	
	//whether reading as an XML or using batik, a w3c Document will provide
	//the needed functionality
	private Document svgDoc;
	
	private ErrorStatus status = ErrorStatus.NONE;
	
	public AnnotatedImage() {
		createFromDocument(createNewDocument());
	}
	
	public AnnotatedImage(String markup) {
		try {
			createFromDocument(DrawingUtil.convertStringToDocument(markup));
		}
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			status = ErrorStatus.FORMAT_ERROR;
		}
		catch (ParserConfigurationException e) {
			e.printStackTrace();
			status = ErrorStatus.INTERNAL_ERROR;
		}
		catch (SAXException e) {
			e.printStackTrace();
			status = ErrorStatus.FORMAT_ERROR;
		}
		catch (IOException e) {
			e.printStackTrace();
			status = ErrorStatus.FILE_ERROR;
		}
	}
	
	public AnnotatedImage(File svgFile) {
		/*String parser = XMLResourceDescriptor.getXMLParserClassName();
		SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
		*/
		Document svgDocFromFile = null;
		
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			svgDocFromFile = builder.parse(svgFile);
		}
		catch (IOException e) {
			log.error("Trying to read file: " + svgFile.getAbsolutePath(), e);
			status = ErrorStatus.FILE_ERROR;
		}
		catch (ParserConfigurationException e) {
			e.printStackTrace();
			status = ErrorStatus.INTERNAL_ERROR;
		}
		catch (SAXException e) {
			e.printStackTrace();
			status = ErrorStatus.FORMAT_ERROR;
		}
		
		createFromDocument(svgDocFromFile);
	}
	
	private Document createNewDocument() {
		//DOMImplementation impl = SVGDOMImplementation.getDOMImplementation();
		//String svgNS = SVGDOMImplementation.SVG_NAMESPACE_URI;
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		DOMImplementation domImpl = null;
		Document newSvgDoc = null;
		
		try {
			builder = factory.newDocumentBuilder();
			domImpl = builder.getDOMImplementation();
			newSvgDoc = domImpl.createDocument(null, null, null);
			
			Element rootSvg = newSvgDoc.createElement("svg");
			
			newSvgDoc.appendChild(rootSvg);
			
		}
		catch (ParserConfigurationException e) {
			e.printStackTrace();
			status = ErrorStatus.INTERNAL_ERROR;
		}
		
		return newSvgDoc;//impl.createDocument(svgNS, "svg", null);
	}
	
	private void createFromDocument(Document image) {
		
		if (image == null) {
			status = ErrorStatus.NULL_DOC;
			image = createNewDocument();
			
		}
		
		setImageDocument(image);
		
	}
	
	/**
	 * @return error if any error was encountered when loading the svg
	 */
	
	public ErrorStatus getParsingError() {
		return status;
	}
	
	/**
	 * @param svgImage the SVG org.w3c.Document to set, parse, render et c.
	 */
	public void setImageDocument(Document image) {
		
		//immediately store the new document
		svgDoc = image;
		
		//create a transcoding pipeline from SVG document to PNG 
		//(which is a lossless encoding scheme) 
		/*PNGTranscoder t = new PNGTranscoder();
		TranscoderInput docInput = new TranscoderInput(image);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		TranscoderOutput raster = new TranscoderOutput(out);
		
		//transcode to PNG
		try {
			t.transcode(docInput, raster);
		} catch (TranscoderException e1) {
			
			e1.printStackTrace();
		}*/
		
		//should the full image be left in memory? 
		//it could be scaled and stored as a thumbnail, is it needed otherwise?
		//use ImageIO to decode the PNG data into an uncompressed BufferedImage
		/*try {
			
			//renderedImage = ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
		
		} catch (IOException e) {
			
			e.printStackTrace();
		}*/
	}
	
	/**
	 * @return svgImage the SVG org.w3c.Document that is set
	 */
	public Document getImageDocument() {
		return svgDoc;
	}
	
	/**
	 * @param svgImage A string representing the SVG, should be parsable to an org.w3c.Document
	 */
	public void setImageDocument(String svgImage) {
		
		Document svgDoc = null;
		
		try {
			svgDoc = DrawingUtil.convertStringToDocument(svgImage);
		}
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			status = ErrorStatus.FORMAT_ERROR;
		}
		catch (ParserConfigurationException e) {
			e.printStackTrace();
			status = ErrorStatus.INTERNAL_ERROR;
		}
		catch (SAXException e) {
			e.printStackTrace();
			status = ErrorStatus.FORMAT_ERROR;
		}
		catch (IOException e) {
			e.printStackTrace();
			status = ErrorStatus.FILE_ERROR;
		}
		
		setImageDocument(svgDoc);
	}
	
	/**
	 * @param parent The complex observation that text annotations should belong to
	 * @param userUuid The uuid of the user creating, changing, voiding et c.
	 * @param locUuid The uuid of the location current actions are being done at
	 * @param encUuid The encounter uuid being referenced (used for creating new obs)
	 * @return the annotations
	 */
	public void storeAnnotations(DrawingUtil.IHandleTextAnnObsStorage storage) {
		
		//get all text elements
		org.w3c.dom.NodeList textElements = svgDoc.getElementsByTagName("text");
		
		//create ImageAnnotation objs from element details
		List<Node> annotationList = new ArrayList<Node>(textElements.getLength());
		
		//NodeList is not an iterable
		for (Integer i = 0; i < textElements.getLength(); i++) {
			Node elem = textElements.item(i);
			NamedNodeMap attr = elem.getAttributes();
			
			Node obsIdAttr = attr.getNamedItem("data-obsid");
			
			Node statusAttr = attr.getNamedItem("data-status");
			
			String text = elem.getTextContent();
			
			String status = "new";
			if (statusAttr != null) {
				statusAttr.getNodeValue();
			}
			
			//if there is a data-obsid attr, i.e. it's not a new annotation
			String obsId = null;
			
			if (obsIdAttr != null) {
				
				obsId = obsIdAttr.getNodeValue();
				
				switch (status) {
					case "changed":
						storage.updateObs(obsId, text);
						break;
					case "voided":
						Node reasonAttr = attr.getNamedItem("voidReason");
						
						String reason = "Reason not provided";
						if (reasonAttr != null) {
							reason = reasonAttr.getNodeValue();
						}
						
						storage.voidObs(obsId, reason);
						break;
					default:
						break;
				}
				
			} else {
				String obsUuid = storage.newObs(text);
				((Element) elem).setAttribute("data-obsid", obsUuid);
			}
			
		}
		
	}
	
	/**
	 * @param annotationList the annotations from the db, mapping obsId:annotationText
	 */
	public void populateAnnotations(Map<String, String> annotationMap) {
		//this.annotations = annotations;
		
		//get all text elements
		org.w3c.dom.NodeList textElements = svgDoc.getElementsByTagName("text");
		
		for (Integer i = 0; i < textElements.getLength(); i++) {
			Node elem = textElements.item(i);
			NamedNodeMap attr = elem.getAttributes();
			
			Node obsIdAttr = attr.getNamedItem("data-obsid");
			
			//if there is a data-obsid attr, i.e. it's not a new annotation
			String obsId = null;
			
			if (obsIdAttr != null) {
				obsId = obsIdAttr.getNodeValue();
				
				String text = annotationMap.get(obsId);
				
				//TODO handle tspans
				
				elem.setTextContent(text);
				
			}
		}
		
	}
	
}
